package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A plan can say "find the weight" (ROADMAP N309).
 *
 * One one-shot marker on a plan's exercise: the next session starts with the weight blank, and the finish's
 * prompt either adopts what was done or leaves the plan weightless. Every plan on disk before this was simply
 * never marked, which is what the default says; the column is not a prescription, so it never moves a target.
 */
internal val MIGRATION_24_25 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_CALIBRATION)
    }
}

private const val FROM_VERSION = 24
private const val TO_VERSION = 25

/** Not null with a default, so every plan already on disk is "never marked" and a raw insert stays valid. */
private const val ADD_CALIBRATION =
    "ALTER TABLE template_exercises ADD COLUMN calibrationPending INTEGER NOT NULL DEFAULT 0"
