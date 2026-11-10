package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A program can adjust its own plans (ROADMAP N299).
 *
 * Three changes that are one feature: the switch on the program, the steps the app applied (with the
 * whole target before and after, because a load step also resets the range's place), and the target
 * each set held when the switch went on, which is what *reset* restores.
 *
 * Every existing program is `false`: the app was a manual log before this column, and a migration that
 * switched a program on would start writing to a plan nobody asked it to adjust. The two tables start
 * empty for the same reason; there is no history to invent.
 */
internal val MIGRATION_23_24 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_AUTO_PROGRESSION)
        db.execSQL(CREATE_WRITES)
        db.execSQL(CREATE_WRITES_PROGRAM_INDEX)
        db.execSQL(CREATE_WRITES_SET_INDEX)
        db.execSQL(CREATE_WRITES_EXERCISE_INDEX)
        db.execSQL(CREATE_WRITES_APPLIED_INDEX)
        db.execSQL(CREATE_BASELINES)
        db.execSQL(CREATE_BASELINES_PROGRAM_INDEX)
    }
}

private const val FROM_VERSION = 23
private const val TO_VERSION = 24

/** Not null with a default, so every row already on disk is manual and a raw insert stays valid. */
private const val ADD_AUTO_PROGRESSION =
    "ALTER TABLE programs ADD COLUMN autoProgression INTEGER NOT NULL DEFAULT 0"

private const val CREATE_WRITES =
    """
    CREATE TABLE IF NOT EXISTS `progression_writes` (
        `id` TEXT NOT NULL,
        `programId` TEXT,
        `templateSetId` TEXT NOT NULL,
        `sessionExerciseId` TEXT NOT NULL,
        `direction` TEXT,
        `beforeWeightGrams` INTEGER,
        `beforeRepsCurrent` INTEGER,
        `beforeDurationSeconds` INTEGER,
        `afterWeightGrams` INTEGER,
        `afterRepsCurrent` INTEGER,
        `afterDurationSeconds` INTEGER,
        `appliedAt` INTEGER NOT NULL,
        `revertedAt` INTEGER,
        PRIMARY KEY(`id`)
    )
    """

private const val CREATE_WRITES_PROGRAM_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_progression_writes_programId` " +
        "ON `progression_writes` (`programId`)"

private const val CREATE_WRITES_SET_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_progression_writes_templateSetId` " +
        "ON `progression_writes` (`templateSetId`)"

private const val CREATE_WRITES_EXERCISE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_progression_writes_sessionExerciseId` " +
        "ON `progression_writes` (`sessionExerciseId`)"

private const val CREATE_WRITES_APPLIED_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_progression_writes_appliedAt` " +
        "ON `progression_writes` (`appliedAt`)"

private const val CREATE_BASELINES =
    """
    CREATE TABLE IF NOT EXISTS `progression_baselines` (
        `programId` TEXT NOT NULL,
        `templateSetId` TEXT NOT NULL,
        `weightGrams` INTEGER,
        `repsCurrent` INTEGER,
        `durationSeconds` INTEGER,
        `capturedAt` INTEGER NOT NULL,
        PRIMARY KEY(`programId`, `templateSetId`)
    )
    """

private const val CREATE_BASELINES_PROGRAM_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_progression_baselines_programId` " +
        "ON `progression_baselines` (`programId`)"
