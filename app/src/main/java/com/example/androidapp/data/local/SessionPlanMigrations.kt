package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The plan each session exercise started under (ROADMAP B471).
 *
 * A snapshot table, empty on every existing install: the sessions already on disk were trained
 * against plans the app can no longer reconstruct, so the rules fall back to the living template for
 * them rather than inventing a prescription. New sessions write their snapshot as they are seeded.
 */
internal val MIGRATION_25_26 = object : Migration(FROM_25, TO_26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SESSION_PLAN_SETS)
        db.execSQL(CREATE_SESSION_PLAN_SETS_INDEX)
    }
}

private const val FROM_25 = 25
private const val TO_26 = 26

private const val CREATE_SESSION_PLAN_SETS =
    """
    CREATE TABLE IF NOT EXISTS `session_plan_sets` (
        `sessionExerciseId` TEXT NOT NULL,
        `setIndex` INTEGER NOT NULL,
        `role` TEXT NOT NULL,
        `targetWeightGrams` INTEGER,
        `targetAssistanceGrams` INTEGER,
        `targetRepsMin` INTEGER,
        `targetRepsMax` INTEGER,
        `targetRepsCurrent` INTEGER,
        `targetDurationSeconds` INTEGER,
        `targetRpeHalves` INTEGER,
        PRIMARY KEY(`sessionExerciseId`, `setIndex`),
        FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`)
            ON UPDATE NO ACTION ON DELETE CASCADE
    )
    """

private const val CREATE_SESSION_PLAN_SETS_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_session_plan_sets_sessionExerciseId` " +
        "ON `session_plan_sets` (`sessionExerciseId`)"
