package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The two numbers an activity may carry (ROADMAP N293).
 *
 * Both nullable: a marker-only swim remains a valid record, and a restored backup from before this column
 * reads as "not stated" rather than zero. Duration is seconds and distance metres, the same exact units the
 * rest of the schema uses.
 */
internal val MIGRATION_21_22 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_ACTIVITY_DURATION)
        db.execSQL(ADD_ACTIVITY_DISTANCE)
    }
}

private const val FROM_VERSION = 21
private const val TO_VERSION = 22
private const val ADD_ACTIVITY_DURATION =
    "ALTER TABLE workout_sessions ADD COLUMN activityDurationSeconds INTEGER"
private const val ADD_ACTIVITY_DISTANCE =
    "ALTER TABLE workout_sessions ADD COLUMN activityDistanceMeters INTEGER"
