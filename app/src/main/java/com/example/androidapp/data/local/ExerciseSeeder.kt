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
                    // A seeded row starts top level and is a lift. The families N95 ships are linked by
                    // the migration that creates them, not by this top-up, which must never re-file a row
                    // the lifter has moved — the seeder's own rule is that it undoes nothing.
                    null, // parentId
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
