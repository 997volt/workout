package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A plan can prescribe a hold, and a hold can progress (ROADMAP N242).
 *
 * One nullable column on `template_sets`, added by a plain ALTER: every planned set that exists is counted,
 * which is exactly what it was. The column sits **beside** the rep target rather than replacing it, because a
 * plan is written either way and the write boundary — not the column — is where "one or the other" is enforced.
 */
internal val MIGRATION_14_15 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_TARGET_DURATION)
    }
}

private const val FROM_VERSION = 14
private const val TO_VERSION = 15

private const val ADD_TARGET_DURATION = "ALTER TABLE template_sets ADD COLUMN targetDurationSeconds INTEGER"
