package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A session can be an activity rather than a workout (ROADMAP N141).
 *
 * One nullable column, and that is the whole of the structure: an activity is a session that holds no sets, so
 * the day, the history, the calendar and the export all read it through the paths they already have. The
 * migration therefore adds the column and nothing else — every row already on disk is a workout, which is
 * exactly what a null activity means.
 */
internal val MIGRATION_5_6 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_ACTIVITY)
    }
}

private const val FROM_VERSION = 5
private const val TO_VERSION = 6

/** Nullable with no default, so a session written before this is a workout. */
private const val ADD_ACTIVITY = "ALTER TABLE workout_sessions ADD COLUMN activity TEXT"
