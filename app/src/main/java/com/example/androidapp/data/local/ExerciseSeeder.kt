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
        // **The repair runs first, so the corrected seed can use the id it frees** (B148): a 2.6 database
        // holds the *category* under `back-extension`, and `INSERT OR IGNORE` would keep it there, so the
        // movement this releases is inserted by the loop below, in this same transaction.
        repairSeededCategoryCollision(db)

        // The families first (ROADMAP N95). A category is a row of the library like any other, so it is
        // seeded by the same statement with the same conflict rule — which is what keeps a lifter's rename
        // or re-file from being undone on the next start.
        SeedExercises.categories.forEach { category ->
            db.execSQL(
                INSERT_EXERCISE,
                arrayOf<Any?>(
                    category.id,
                    category.name,
                    // **The three facts a family holds** (ROADMAP N110): its pattern, its primary muscle and
                    // its secondary list, stated once here so no movement under it has to. Its **equipment
                    // stays unstated**: a family spans equipment — the lat pulldown family alone holds a
                    // cable, a bodyweight and a machine movement — so there is no one value to write.
                    converters.fromMuscleGroup(category.primaryMuscle),
                    converters.fromMuscleGroups(category.secondaryMuscles),
                    null,
                    converters.fromMovementPattern(category.pattern),
                    null, // parentId: a category sits at the top level
                    RowKind.CATEGORY.name,
                    seededAt,
                    seededAt,
                ),
            )
        }

        SeedExercises.all.forEach { exercise ->
            db.execSQL(
                INSERT_EXERCISE,
                arrayOf<Any?>(
                    exercise.id,
                    exercise.name,
                    // What this row **states**, and nothing else (ROADMAP N103, N110): a movement filed
                    // under a category usually states only its equipment, because the family is the home of
                    // the pattern and the muscles. Null means *read it from the category*.
                    exercise.primaryMuscle?.let(converters::fromMuscleGroup),
                    converters.fromMuscleGroups(exercise.secondaryMuscles),
                    exercise.equipment?.let(converters::fromEquipment),
                    exercise.movementPattern?.let(converters::fromMovementPattern),
                    // The family this movement arrives filed under (ROADMAP N95), from the seed's own map so
                    // the seeder and the record of it cannot disagree. `INSERT OR IGNORE` is what
                    // keeps this from undoing a re-file: the row that already exists is left alone, and
                    // only a movement a database does not have yet arrives with its parent set.
                    SeedExercises.parentOf[exercise.id],
                    // The enum's name rather than the column's SQL default, so the value keeps one home; a
                    // DEFAULT in SQL would be a second copy of a name that lives in the enum.
                    RowKind.MOVEMENT.name,
                    // One timestamp per batch, so seeded rows are recognisable as
                    // seed rather than as something the user created.
                    seededAt,
                    seededAt,
                ),
            )
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

/**
 * Moves the category a 2.6 database filed under a movement's own id (B148).
 *
 * The seed declared *Back Extension* twice — once as a movement, once as a family — and because the
 * families are seeded first and `INSERT OR IGNORE` keeps what exists, the row a database holds under
 * `back-extension` is the **category**, with a `parentId` pointing at itself. Nothing can be done about
 * the movement on that file by re-seeding: the id is taken, so the corrected seed still cannot insert it.
 * The id has to be freed, and the row holding it has to become the family it was meant to be.
 *
 * **Guarded, because the row may not be the seed's any more.** A lifter can rename a category and re-file
 * its children (N95's rule, which `INSERT OR IGNORE` exists to protect), and this runs on every open. The
 * id is only moved while the row still looks like the seed's: a category, still named *Back Extension*, and
 * still its own parent — the last being the one field no write path can produce, since
 * `requireValidLibraryShape` refuses a self-parent. A row a lifter has touched in any other way fails at
 * least one of those and is left alone.
 *
 * The secondary list and the muscles are the seed's own values, so the move does not rewrite them: only the
 * key and the link that key broke.
 *
 * The replacement family arrives from the corrected seed in the same [seedMissingExercises] transaction,
 * which is why this runs before the inserts rather than after them.
 */
internal fun repairSeededCategoryCollision(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        UPDATE exercises
        SET id = 'back-extension-family', parentId = NULL
        WHERE id = 'back-extension'
          AND name = 'Back Extension'
          AND rowKind = 'CATEGORY'
          AND parentId = 'back-extension'
        """.trimIndent(),
    )
}

/** `deletedAt` is omitted so it keeps its NULL default. */
internal const val INSERT_EXERCISE =
    "INSERT OR IGNORE INTO exercises " +
        "(id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, " +
        "parentId, rowKind, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
