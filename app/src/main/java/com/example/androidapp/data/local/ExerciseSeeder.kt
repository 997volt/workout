package com.example.androidapp.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.SeedExercises
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
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
        // The families first (ROADMAP N95). A category is a row of the library like any other, so it is
        // seeded by the same statement with the same conflict rule — which is what keeps a lifter's rename
        // or re-file from being undone on the next start.
        SeedExercises.categories.forEach { category ->
            db.execSQL(
                INSERT_EXERCISE,
                arrayOf<Any?>(
                    category.id,
                    category.name,
                    // The placeholders a head carries: it is never offered and never logged, so its muscles and
                    // its equipment say nothing rather than something untrue — a family spans equipment, and
                    // every child sets its own. **Its pattern is the exception** (N96): that is the fact the
                    // category owns, because whether a family presses or hinges is the family's to decide, and
                    // declaring it once is what stops a movement and its variations disagreeing.
                    converters.fromMuscleGroup(MuscleGroup.OTHER),
                    converters.fromMuscleGroups(emptyList()),
                    converters.fromEquipment(Equipment.OTHER),
                    converters.fromMovementPattern(category.pattern),
                    0, // isCustom: seed, like every row this function writes
                    null, // parentId: a head sits at the top level
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
                    converters.fromMuscleGroup(exercise.primaryMuscle),
                    converters.fromMuscleGroups(exercise.secondaryMuscles),
                    converters.fromEquipment(exercise.equipment),
                    converters.fromMovementPattern(exercise.movementPattern),
                    0, // isCustom
                    // The family this movement arrives filed under (ROADMAP N95), from the seed's own map so
                    // the seeder and the migration cannot disagree about it. `INSERT OR IGNORE` is what
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

/** `deletedAt` is omitted so it keeps its NULL default. */
internal const val INSERT_EXERCISE =
    "INSERT OR IGNORE INTO exercises " +
        "(id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, " +
        "isCustom, parentId, rowKind, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
