package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A step that was offered and not taken is recorded (ROADMAP N236).
 *
 * One nullable column on `session_exercises`, added by a plain ALTER: every row that exists declined nothing,
 * which is what it did. It lives on the session's row rather than on the plan because the **offer** is the
 * fact — it was computed from the plan as it stood, and a plan that has since moved on can no longer say what
 * was there to take.
 *
 * There is no backfill, and none is possible: a decline is a thing the app did not observe before this
 * column existed, so an upgraded store honestly says nothing about the past.
 */
internal val MIGRATION_13_14 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_DECLINED_DIRECTIONS)
    }
}

private const val FROM_VERSION = 13
private const val TO_VERSION = 14

private const val ADD_DECLINED_DIRECTIONS = "ALTER TABLE session_exercises ADD COLUMN declinedDirections TEXT"
