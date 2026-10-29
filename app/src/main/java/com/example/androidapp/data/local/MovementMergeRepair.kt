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
        db.execSQL(RETIRE, arrayOf<Any?>(merge.sourceId, merge.survivorId, at))
        db.execSQL(DELETE_MOVEMENT, arrayOf<Any?>(merge.sourceId))
    }
}

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
    "INSERT OR REPLACE INTO retired_movements (id, survivorId, at) VALUES (?, ?, ?)"

private const val DELETE_MOVEMENT = "DELETE FROM exercises WHERE id = ?"
