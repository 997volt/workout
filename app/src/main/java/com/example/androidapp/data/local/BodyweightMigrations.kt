package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A bodyweight set records the lifter's weight, and a movement can say it is loaded by one (ROADMAP N219).
 *
 * Two columns and no backfill, because both mean "this is what it already was": a movement that never said
 * it was a bodyweight movement is an ordinary one, and a set that recorded no bodyweight was loaded at its
 * added weight — the rule every figure in the app already followed. Writing a bodyweight onto old rows would
 * be inventing a measurement nobody took, which is the same objection the goal timeline makes to stamping a
 * date on a goal whose moment was never recorded.
 */
internal val MIGRATION_9_10 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_IS_BODYWEIGHT)
        db.execSQL(ADD_BODYWEIGHT_GRAMS)
    }
}

private const val FROM_VERSION = 9
private const val TO_VERSION = 10

/** Not null with a SQL default, because Room needs a value for rows that already exist. */
private const val ADD_IS_BODYWEIGHT =
    "ALTER TABLE exercises ADD COLUMN isBodyweight INTEGER NOT NULL DEFAULT 0"

/** Nullable with no default, so a set written before this reads as its added weight. */
private const val ADD_BODYWEIGHT_GRAMS = "ALTER TABLE set_entries ADD COLUMN bodyweightGrams INTEGER"
