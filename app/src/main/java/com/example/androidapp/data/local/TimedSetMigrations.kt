package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A set can be a hold, and a movement can say it is timed (ROADMAP N220).
 *
 * Two columns and no backfill, for the reason [MIGRATION_9_10] gives: both mean what was already true. A
 * movement nobody marked timed is counted, and a set that recorded no duration was counted too — which is
 * every set on disk before this, because there was nowhere to put a hold.
 */
internal val MIGRATION_10_11 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_IS_TIMED)
        db.execSQL(ADD_DURATION_SECONDS)
    }
}

private const val FROM_VERSION = 10
private const val TO_VERSION = 11

/** Not null with a SQL default, because Room needs a value for rows that already exist. */
private const val ADD_IS_TIMED = "ALTER TABLE exercises ADD COLUMN isTimed INTEGER NOT NULL DEFAULT 0"

/** Nullable with no default, so a set written before this reads as a counted set. */
private const val ADD_DURATION_SECONDS = "ALTER TABLE set_entries ADD COLUMN durationSeconds INTEGER"
