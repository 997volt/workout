package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A movement can answer *Done* its own way (ROADMAP N235).
 *
 * One nullable column, added by a plain ALTER: every row that exists answers "follow the app setting", which
 * is exactly what it did before a movement could have an opinion. The value is stored by **name** rather than
 * by ordinal, so a build that does not know a name reads it as unset instead of as a different answer.
 *
 * There is no backfill because there is nothing to backfill: the setting is still the answer for a row whose
 * column is null, and a null is not a missing opinion but the absence of one.
 */
internal val MIGRATION_12_13 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_PROGRESSION_MODE)
    }
}

private const val FROM_VERSION = 12
private const val TO_VERSION = 13

private const val ADD_PROGRESSION_MODE = "ALTER TABLE exercises ADD COLUMN progressionMode TEXT"
