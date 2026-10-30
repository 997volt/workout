package com.example.androidapp.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.MergeCandidate
import com.example.androidapp.data.SeedExercises
import com.example.androidapp.data.mergedMovementPairs

/**
 * Merges the library's duplicates, once per open (ROADMAP N238).
 *
 * Runs inside the seeder's transaction, after the seed's own rows are written — so every seeded survivor is
 * there to merge into — and before anything reads the library. **Idempotent**: a store with no duplicates
 * produces no pairs, so the second run of the same version writes nothing.
 *
 * The rule is a pure function ([mergedMovementPairs]); this is only the four statements it takes, in the order
 * that keeps the library valid: a session's and a plan's rows are re-pointed first, so the delete cannot leave a
 * reference behind.
 */
internal fun mergeDuplicateMovements(db: SupportSQLiteDatabase, at: Long) {
    val library = db.query(LIVE_LIBRARY).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    MergeCandidate(
                        id = cursor.getString(ID_COLUMN),
                        name = cursor.getString(NAME_COLUMN),
                        seeded = cursor.getString(ID_COLUMN) in seededIds,
                        // The stored shape an import leaves: every fact unstated, and no family.
                        isBare = TAXONOMY_COLUMNS.all { cursor.isNull(it) || cursor.getString(it).isEmpty() },
                    ),
                )
            }
        }
    }
    val pairs = mergedMovementPairs(library)
    if (pairs.isEmpty()) return

    pairs.forEach { merge ->
        db.execSQL(REPOINT_SESSION_EXERCISES, arrayOf<Any?>(merge.survivorId, at, merge.sourceId))
        db.execSQL(REPOINT_TEMPLATE_EXERCISES, arrayOf<Any?>(merge.survivorId, at, merge.sourceId))
        // What only the source stated is the lifter's own (B298): the survivor keeps every value it has and
        // gains whatever it left unset. A bare import may still carry a cue or a rest a document brought whole,
        // and after the delete this is the only row left to hold it.
        carryLifterFields(db, from = merge.sourceId, to = merge.survivorId)
        rememberRetiredMovement(db, from = merge.sourceId, to = merge.survivorId)
        db.execSQL(DELETE_MOVEMENT, arrayOf<Any?>(merge.sourceId))
    }
}

/**
 * Records that [from] now means [to] (ROADMAP N238, B300).
 *
 * The program import resolves a document's movement ids through this map; without a row, a file exported
 * before the retirement mints the twin again. Idempotent, so the seeder can assert its static retirement on
 * every open — a fresh install gets the mapping even though its merge is a no-op — and the duplicate repair
 * can record its own pair.
 */
internal fun rememberRetiredMovement(db: SupportSQLiteDatabase, from: String, to: String) {
    db.execSQL(RETIRE, arrayOf<Any?>(from, to))
}

/**
 * Moves the lifter-authored columns a merge would otherwise delete onto the survivor (B298).
 *
 * The five are the row's own settings rather than its taxonomy: rest, cue, display unit, weight step and
 * movement-level progression answer. The survivor's value always wins — a merge fills a gap, it does not
 * overwrite — and the columns travel before the source row is deleted.
 */
internal fun carryLifterFields(db: SupportSQLiteDatabase, from: String, to: String) {
    db.execSQL(CARRY_LIFTER_FIELDS, arrayOf<Any?>(from, from, from, from, from, to))
}

/** Each column keeps the survivor's own value and takes the source's only where the survivor states none. */
private const val CARRY_LIFTER_FIELDS = """
    UPDATE exercises SET
        restSeconds = COALESCE(restSeconds, (SELECT restSeconds FROM exercises WHERE id = ?)),
        techniqueNote = COALESCE(techniqueNote, (SELECT techniqueNote FROM exercises WHERE id = ?)),
        weightUnit = COALESCE(weightUnit, (SELECT weightUnit FROM exercises WHERE id = ?)),
        stepGrams = COALESCE(stepGrams, (SELECT stepGrams FROM exercises WHERE id = ?)),
        progressionMode = COALESCE(progressionMode, (SELECT progressionMode FROM exercises WHERE id = ?))
    WHERE id = ?
"""

/** The ids the seed ships, which are never sources of a merge. */
private val seededIds: Set<String> by lazy {
    SeedExercises.all.map { it.id }.toSet() + SeedExercises.categories.map { it.id }
}

private const val ID_COLUMN = 0
private const val NAME_COLUMN = 1

/**
 * The columns "bare" is read from: no muscle, no secondary list, no equipment, no pattern and no family.
 *
 * One list rather than five `getString` calls, so the question the rule asks — *does this row state anything?* —
 * is asked once and a column added to the rule is a change in one place.
 */
private val TAXONOMY_COLUMNS: List<Int> = buildList {
    var column = NAME_COLUMN + 1
    repeat(TAXONOMY_COLUMN_COUNT) {
        add(column)
        column += 1
    }
}

/** Five facts decide *bare*: the two muscle columns, equipment, the pattern and the family. */
private const val TAXONOMY_COLUMN_COUNT = 5

private const val LIVE_LIBRARY = """
    SELECT id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, parentId
    FROM exercises
    WHERE deletedAt IS NULL AND rowKind = 'MOVEMENT'
"""

private const val REPOINT_SESSION_EXERCISES =
    "UPDATE session_exercises SET exerciseId = ?, updatedAt = ? WHERE exerciseId = ?"

private const val REPOINT_TEMPLATE_EXERCISES =
    "UPDATE template_exercises SET exerciseId = ?, updatedAt = ? WHERE exerciseId = ?"

private const val RETIRE =
    "INSERT OR IGNORE INTO retired_movements (id, survivorId) VALUES (?, ?)"

private const val DELETE_MOVEMENT = "DELETE FROM exercises WHERE id = ?"
