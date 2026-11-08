package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The two read paths that grow with history (ROADMAP N282).
 *
 * `workout_sessions.templateId` is the key a program's adherence and repeat reads use, and
 * `measurements.measuredAt` is the range and newest-entry path. Both are nullable columns; SQLite indexes
 * nulls like any other value, and the queries filter by the indexed column rather than scanning the table.
 */
internal val MIGRATION_19_20 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SESSION_TEMPLATE_INDEX)
        db.execSQL(CREATE_MEASUREMENT_DATE_INDEX)
    }
}

private const val FROM_VERSION = 19
private const val TO_VERSION = 20

private const val CREATE_SESSION_TEMPLATE_INDEX =
    "CREATE INDEX IF NOT EXISTS index_workout_sessions_templateId ON workout_sessions(templateId)"
private const val CREATE_MEASUREMENT_DATE_INDEX =
    "CREATE INDEX IF NOT EXISTS index_measurements_measuredAt ON measurements(measuredAt)"
