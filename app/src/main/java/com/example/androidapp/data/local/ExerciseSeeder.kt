package com.example.androidapp.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.SeedExercises
import com.example.androidapp.domain.model.RowKind

/**
 * Tops up the seed exercise library.
 *
 * Runs on a raw [SupportSQLiteDatabase] because it is called from
 * `RoomDatabase.Callback.onOpen`, where a Room DAO call would deadlock on the
 * connection currently being opened.
 *
 * `INSERT OR IGNORE` is the important part:
 *
 *  - `OnConflictStrategy.REPLACE` is DELETE + INSERT. Once any workout references
 *    an exercise, `session_exercises.exerciseId` is `ON DELETE RESTRICT`, so
 *    replacing a seeded row throws instead of updating it. No conflict strategy
 *    both updates a row and preserves foreign keys, so the seed simply never
 *    updates a row that exists.
 *  - Existing ids are left alone, which also means a user's soft-delete of a
 *    seeded exercise survives a top-up instead of being silently undone.
 *
 * [INSERT_EXERCISE]'s column list must match [ExerciseEntity];
 * `ExerciseSeederTest` asserts the library is populated the moment the database
 * opens, which is what catches drift.
 */
internal fun seedMissingExercises(db: SupportSQLiteDatabase, seededAt: Long) {
    // Reusing the converters keeps the storage format (enums by name, muscles as
    // CSV) in one place instead of re-implementing it in SQL.
    val converters = Converters()

    db.beginTransaction()
    try {
        // **The repair runs first, so the seed can use the ids it frees** (B148, B292): a database written by
        // an older seed can hold a row under an id the seed now claims for a *movement*, and `INSERT OR IGNORE`
        // would keep it there for ever — so the impostor is resolved before the inserts, in this transaction.
        releaseSeededMovementIds(db)
        insertCategories(db, converters, seededAt)
        insertMovements(db, converters, seededAt)
        writeBehindTheInsert(db, seededAt)
        // **The lifter's own movements gain the seed's taxonomy** (N239): their rows are already here under the
        // ids the seed now ships, and `INSERT OR IGNORE` never updates a row.
        fillSeededTaxonomy(db, seededAt)
        // **The duplicates are merged after the seed is whole** (N238): a seeded survivor has to be there to
        // merge into, and a document loaded an hour ago may have left a twin beside it.
        mergeDuplicateMovements(db, seededAt)
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

/**
 * The families, first (ROADMAP N95).
 *
 * A category is a row of the library like any other, so it is seeded by the same statement with the same
 * conflict rule — which is what keeps a lifter's rename or re-file from being undone on the next start.
 */
private fun insertCategories(db: SupportSQLiteDatabase, converters: Converters, seededAt: Long) {
    SeedExercises.categories.forEach { category ->
        db.execSQL(
            INSERT_EXERCISE,
            arrayOf<Any?>(
                category.id,
                category.name,
                // **The three facts a family holds** (ROADMAP N110): its pattern, its primary muscle and its
                // secondary list, stated once here so no movement under it has to. Its **equipment stays
                // unstated**: a family spans equipment — the lat pulldown family alone holds a cable, a
                // bodyweight and a machine movement — so there is no one value to write.
                converters.fromMuscleGroup(category.primaryMuscle),
                converters.fromMuscleGroups(category.secondaryMuscles),
                null,
                converters.fromMovementPattern(category.pattern),
                null, // parentId: a category sits at the top level
                RowKind.CATEGORY.name,
                // A family is never loaded by the body and is never held: the two flags belong to the
                // movements a set is logged against (B293).
                0,
                0,
                seededAt,
                seededAt,
            ),
        )
    }
}

/** The movements, after the families they read their taxonomy from (ROADMAP N110). */
private fun insertMovements(db: SupportSQLiteDatabase, converters: Converters, seededAt: Long) {
    SeedExercises.all.forEach { exercise ->
        db.execSQL(
            INSERT_EXERCISE,
            arrayOf<Any?>(
                exercise.id,
                exercise.name,
                // What this row **states**, and nothing else (ROADMAP N103, N110): a movement filed under a
                // category usually states only its equipment, because the family is the home of the pattern
                // and the muscles. Null means *read it from the category*.
                exercise.primaryMuscle?.let(converters::fromMuscleGroup),
                converters.fromMuscleGroups(exercise.secondaryMuscles),
                exercise.equipment?.let(converters::fromEquipment),
                exercise.movementPattern?.let(converters::fromMovementPattern),
                // The family this movement arrives filed under (ROADMAP N95), from the seed's own map so the
                // seeder and the record of it cannot disagree. `INSERT OR IGNORE` is what keeps this from
                // undoing a re-file: a row that already exists is left alone, and only a movement a database
                // does not have yet arrives with its parent set.
                SeedExercises.parentOf[exercise.id],
                // The enum's name rather than the column's SQL default, so the value keeps one home; a
                // DEFAULT in SQL would be a second copy of a name that lives in the enum.
                RowKind.MOVEMENT.name,
                // The two flags the seeder did not write, so every seeded movement arrived as an ordinary
                // loaded one (B293): a weighted pull-up read as its belt alone, a plank offered reps.
                if (exercise.isBodyweight) 1 else 0,
                if (exercise.isTimed) 1 else 0,
                // One timestamp per batch, so seeded rows are recognisable as
                // seed rather than as something the user created.
                seededAt,
                seededAt,
            ),
        )
    }
}

/**
 * The repairs that write **behind** the insert, because `INSERT OR IGNORE` never updates a row.
 *
 * A change the seed makes to how a movement already on a device is read has to be written there, and each is
 * guarded by what the seed shipped so a lifter's own edit is never undone. In order: a movement the seed now
 * files (N229), a family's muscles the audit corrected (N240), the two flags the seeder never wrote (B293),
 * and a movement the seed retired merged into the one that means it (N229) — last, because the row it merges
 * into has just been inserted.
 */
private fun writeBehindTheInsert(db: SupportSQLiteDatabase, seededAt: Long) {
    fileMovementsTheSeedNowFiles(db)
    correctSeededTaxonomy(db, seededAt)
    // Only ever raised, so a lifter's own answer survives.
    SeedExercises.all.forEach { exercise ->
        if (!exercise.isBodyweight && !exercise.isTimed) return@forEach
        val bodyweight = if (exercise.isBodyweight) 1 else 0
        val timed = if (exercise.isTimed) 1 else 0
        db.execSQL(
            UPDATE_SEEDED_FLAGS,
            arrayOf<Any?>(bodyweight, timed, exercise.id, exercise.name, RowKind.MOVEMENT.name, bodyweight, timed),
        )
    }
    mergeRetiredMovements(db, seededAt)
}

/**
 * Frees an id the seed claims for a movement when something else holds it (B292, replacing B148's repair).
 *
 * A database written by an older seed can hold the **category** under an id this seed uses for a movement —
 * `back-extension` is the surviving case, where 2.6 declared the lift twice and the families, inserted first,
 * kept the id. `INSERT OR IGNORE` cannot take a taken id, so the movement never arrives: a picker offers
 * movements, so the lift cannot be logged at all, and the stale family is drawn beside the corrected one.
 *
 * B148 tried to move that row aside, and its guard asked for one that was still its own parent — a shape the
 * row on disk does not have, so the repair never fired. **This one matches what is actually there and what no
 * write path can produce**: a row the seed claims for a movement, under that movement's id, of another kind,
 * still carrying that movement's name. A category made in the app is minted with a UUID, and a lifter's rename
 * breaks the name match, so neither is touched.
 *
 * What it does with the impostor: its children are re-filed onto the family the seed files that movement
 * under, which is what a family is for (N95), and the row is **deleted** rather than soft-deleted — a
 * soft-deleted row keeps its id and would block the movement exactly as it does now.
 */
internal fun releaseSeededMovementIds(db: SupportSQLiteDatabase) {
    val claimed = SeedExercises.all.associateBy { it.id }
    val impostors = mutableListOf<String>()
    db.query(
        "SELECT id, name FROM exercises WHERE rowKind <> ?",
        arrayOf<Any?>(RowKind.MOVEMENT.name),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val claimedByTheSeed = claimed[id] ?: continue
            // The name is the seed's own, so a lifted-before rename is a row the lifter owns.
            if (claimedByTheSeed.name == cursor.getString(1)) impostors += id
        }
    }
    impostors.forEach { id ->
        db.execSQL(
            // The impostor's children are the rows still filed under the id it stole. The guard that used to
            // ride here (`parentId <> ?`, bound to the same id) asked for a row that is its own parent, so it
            // matched nothing and the children were left under an id that then named the movement (B296).
            // Nothing here bumps `updatedAt`: a seed repair must not look like a lifter's edit (B299).
            "UPDATE exercises SET parentId = ? WHERE parentId = ?",
            arrayOf<Any?>(SeedExercises.parentOf[id], id),
        )
        db.execSQL(
            "DELETE FROM exercises WHERE id = ? AND rowKind <> ?",
            arrayOf<Any?>(id, RowKind.MOVEMENT.name),
        )
    }
}

/**
 * Files a movement the seed now files, on a row that predates the link (N229, B299).
 *
 * The `parentId IS NULL` and `name` guards make it safe to run on every open — a movement a lifter filed
 * themselves, or renamed, is not the seed's to move — and `createdAt = updatedAt` is the row's own record
 * that the seed is its only writer: an edit bumps `updatedAt`, so a movement a lifter **unfiled** is left
 * where they put it rather than re-filed on the next open. Nothing here bumps `updatedAt` either, or the
 * repair would consume the very marker it reads.
 */
internal const val UPDATE_SEEDED_FAMILY =
    "UPDATE exercises SET parentId = ? " +
        "WHERE id = ? AND name = ? AND rowKind = ? AND parentId IS NULL AND createdAt = updatedAt"

/**
 * Fills in the two flags an older seeder never wrote (B293, B299).
 *
 * The two `<` comparisons make the repair one-way: a row is only touched where its own value is below the
 * seed's, so a raise the lifter made is never lowered. `createdAt = updatedAt` keeps the other direction: an
 * edit bumps `updatedAt`, so a flag the lifter turned **off** is theirs and is not raised again on the next
 * open. Between the two guards only a row the seed itself last wrote is backfilled.
 */
internal const val UPDATE_SEEDED_FLAGS =
    "UPDATE exercises SET isBodyweight = ?, isTimed = ? " +
        "WHERE id = ? AND name = ? AND rowKind = ? AND (isBodyweight < ? OR isTimed < ?) " +
        "AND createdAt = updatedAt"

/**
 * The families whose taxonomy a later seed corrected, with the list it shipped before (N240).
 *
 * A correction is not a new row: the family is on the device already and `INSERT OR IGNORE` will not touch it,
 * so the value it shipped is the only thing that says the row is still the seed's rather than one a lifter
 * edited. Two entries, because the audit found two: the row family was missing the lats, and the pulldown
 * family the upper back.
 */
internal data class CorrectedFamily(val id: String, val wasSecondaryMuscles: String)

/** The audits' corrections, each with the value it replaces (N240). */
internal val CORRECTED_FAMILIES = listOf(
    CorrectedFamily(id = "row", wasSecondaryMuscles = "BICEPS"),
    CorrectedFamily(id = "lat-pulldown-family", wasSecondaryMuscles = "BICEPS"),
)

/**
 * Writes the audited taxonomy onto a family that still carries the value the seed shipped before (N240).
 *
 * **The whole family moves**, because a family's muscles are what its members read (N103, N110): every
 * movement under it that states nothing of its own now reports the corrected muscle, which is the point.
 * The repair is guarded on the old list rather than on the id alone, so a lifter who set their own list keeps
 * it, and it is a no-op after the first open.
 */
private fun correctSeededTaxonomy(db: SupportSQLiteDatabase, now: Long) {
    val converters = Converters()
    CORRECTED_FAMILIES.forEach { corrected ->
        val category = SeedExercises.categories.firstOrNull { it.id == corrected.id } ?: return@forEach
        db.execSQL(
            "UPDATE exercises SET secondaryMuscles = ?, updatedAt = ? " +
                "WHERE id = ? AND rowKind = ? AND secondaryMuscles = ?",
            arrayOf<Any?>(
                converters.fromMuscleGroups(category.secondaryMuscles),
                now,
                category.id,
                RowKind.CATEGORY.name,
                corrected.wasSecondaryMuscles,
            ),
        )
    }
}

/**
 * Writes the seed's taxonomy onto a seeded row that states none (ROADMAP N239).
 *
 * The movements the seed took over from one device's library are already **on** that device, under the ids the
 * seed now ships — which is the whole reason their history did not have to move. `INSERT OR IGNORE` never updates
 * a row, so without this they would stay bare for ever: not found by "barbell", not rolled into a family's muscle
 * total, not sorted among their siblings.
 *
 * **The guard is the shape, not the id alone**: a row whose facts a lifter has filled in is theirs, and the
 * repair only touches one that states nothing — the same rule N238's mechanical merge reads. It is therefore a
 * no-op after the first open, because the row it wrote now states its equipment.
 */
private fun fillSeededTaxonomy(db: SupportSQLiteDatabase, now: Long) {
    val converters = Converters()
    SeedExercises.all.forEach { exercise ->
        // Only the movements the seed took over from a lifter: the rows it has always shipped were written with
        // their taxonomy in the first place, so writing it again on every open would be churn.
        if (exercise.id !in TAXONOMY_TAKEN_OVER) return@forEach
        db.execSQL(
            FILL_SEEDED_TAXONOMY,
            arrayOf<Any?>(
                // A movement states its muscle and pattern only where its family's is wrong (N103), so null is
                // the ordinary answer and is written as null rather than as a value the family would not give.
                exercise.primaryMuscle?.let(converters::fromMuscleGroup),
                converters.fromMuscleGroups(exercise.secondaryMuscles),
                exercise.equipment?.let(converters::fromEquipment),
                exercise.movementPattern?.let(converters::fromMovementPattern),
                now,
                exercise.id,
                exercise.name,
                RowKind.MOVEMENT.name,
            ),
        )
    }
}

/**
 * The ids whose taxonomy the seed took over from one device's library (ROADMAP N239).
 *
 * Written out rather than derived: "this row was bare on one device" is not a fact the seed can recompute, and
 * a list is what makes the repair readable next to the movements it describes.
 */
private val TAXONOMY_TAKEN_OVER: Set<String> by lazy {
    SeedExercises.all.mapNotNull { exercise ->
        // The rows the seed has always shipped state their equipment in the builder; the ones N239 took over are
        // exactly those whose ids are one device's import. Their markers are the ids themselves.
        exercise.id.takeIf { it.startsWith("imp_") || it == "super-rom-dumbbell-lateral-raise" }
    }.toSet()
}

internal const val FILL_SEEDED_TAXONOMY =
    "UPDATE exercises SET primaryMuscle = ?, secondaryMuscles = ?, equipment = ?, movementPattern = ?, " +
        "updatedAt = ? WHERE id = ? AND name = ? AND rowKind = ? " +
        "AND primaryMuscle IS NULL AND equipment IS NULL AND movementPattern IS NULL " +
        "AND (secondaryMuscles IS NULL OR secondaryMuscles = '')"

/**
 * Files the movements the seed now files, on a device whose rows predate the filing (N229).
 *
 * `INSERT OR IGNORE` never updates a row, so a movement that arrived unfiled keeps its null family for ever:
 * the seed's own copy of it is the same row, and nothing wrote the link at the time. [UPDATE_SEEDED_FAMILY]
 * carries the guards; this is only the loop over what the seed files.
 */
private fun fileMovementsTheSeedNowFiles(db: SupportSQLiteDatabase) {
    SeedExercises.all.forEach { exercise ->
        val family = SeedExercises.parentOf[exercise.id] ?: return@forEach
        db.execSQL(UPDATE_SEEDED_FAMILY, arrayOf<Any?>(family, exercise.id, exercise.name, RowKind.MOVEMENT.name))
    }
}

/**
 * A movement a later seed merged into another, and the name the retired one shipped under (N229).
 *
 * The retired row is not deleted from a fresh install's list — it is never in it again — and the pair is the
 * whole record: what has to move is history, and the name is the guard that says the row is still the seed's.
 */
internal data class MergedMovement(val retired: String, val name: String, val survivor: String)

/** The movements the seed has merged away, in the order they were retired (N229). */
internal val MERGED_MOVEMENTS = listOf(
    MergedMovement(retired = "deadlift", name = "Deadlift", survivor = "conventional-deadlift"),
)

/**
 * Moves a retired movement's history onto the one that means it, then takes the row away (N229).
 *
 * Only a row that is still the seed's is merged — same id, same name, still a movement — so a lifter who
 * renamed theirs keeps it, and the merge is a no-op after the first run. **History is the point**: a set
 * names its exercise by id, so every logged set, plan target and program slot follows the survivor, and the
 * row that said the same thing under another name stops being a second movement in the list.
 *
 * A survivor this device does not hold stops the merge rather than orphaning the history: the insert that
 * puts it there runs earlier in this same transaction, and the guard is what keeps a refused statement from
 * rolling the whole seeding back.
 *
 * The **retirement itself** is recorded before the guard (B300): the map is what a program document exported
 * before this release resolves through, so a store that never held the row — every fresh install, where the
 * merge is a no-op — still needs the line. The write is idempotent, so the next open changes nothing.
 */
private fun mergeRetiredMovements(db: SupportSQLiteDatabase, now: Long) {
    MERGED_MOVEMENTS.forEach { merge ->
        rememberRetiredMovement(db, from = merge.retired, to = merge.survivor)
        val retired = db.holdsMovement(merge.retired, merge.name)
        val survivor = db.holdsMovement(merge.survivor)
        if (!retired || !survivor) return@forEach
        db.execSQL(
            "UPDATE session_exercises SET exerciseId = ? WHERE exerciseId = ?",
            arrayOf<Any?>(merge.survivor, merge.retired),
        )
        // A plan is a definition, so the row it changed moves its own timestamp: a program document exported
        // before this is refused as stale rather than merged into a plan that changed (N171).
        db.execSQL(
            "UPDATE template_exercises SET exerciseId = ?, updatedAt = ? WHERE exerciseId = ?",
            arrayOf<Any?>(merge.survivor, now, merge.retired),
        )
        // The row being retired is the seed's, but a lifter may have set its rest, cue or answer (B298), and
        // deleting it would be the only copy of those facts. The survivor fills only what it left unset.
        carryLifterFields(db, from = merge.retired, to = merge.survivor)
        db.execSQL(
            "DELETE FROM exercises WHERE id = ? AND name = ? AND rowKind = ?",
            arrayOf<Any?>(merge.retired, merge.name, RowKind.MOVEMENT.name),
        )
    }
}

/** True when this device holds [id] as a movement, under [name] when one is given. */
private fun SupportSQLiteDatabase.holdsMovement(id: String, name: String? = null): Boolean {
    val byId = arrayOf<Any?>(id, RowKind.MOVEMENT.name)
    val sql = "SELECT 1 FROM exercises WHERE id = ? AND rowKind = ?" + if (name == null) "" else " AND name = ?"
    val args = if (name == null) byId else byId + name
    return query(sql, args).use { cursor -> cursor.moveToFirst() }
}

/** `deletedAt` is omitted so it keeps its NULL default. */
internal const val INSERT_EXERCISE =
    "INSERT OR IGNORE INTO exercises " +
        "(id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, " +
        "parentId, rowKind, isBodyweight, isTimed, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
