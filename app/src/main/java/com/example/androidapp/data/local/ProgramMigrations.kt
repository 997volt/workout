package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A program says whether it repeats, and when a block begins (ROADMAP N174).
 *
 * Two columns and nothing moved. `kind` lands on **WEEKLY** for every row already on disk, which is
 * what every one of them is: the choice did not exist, and the rule they were written under is the one
 * the column now names (P3.3's recurring weekday). `startDate` is null there too, meaning "the day it
 * was created" — the reading a weekly program keeps, because its weeks are the calendar's.
 *
 * Both are read by the code added beside them: the schedule and the editor are the only readers, and a
 * sequence is the only thing that asks either a question.
 */
internal val MIGRATION_7_8 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_KIND)
        db.execSQL(ADD_START_DATE)
    }
}

private const val FROM_VERSION = 7
private const val TO_VERSION = 8

/** A plain ALTER: the column is NOT NULL with a default, which is the value of every row already here. */
private const val ADD_KIND =
    "ALTER TABLE programs ADD COLUMN kind TEXT NOT NULL DEFAULT 'WEEKLY'"

/** Nullable, so no default is needed and "no authored start" is the absent value. */
private const val ADD_START_DATE =
    "ALTER TABLE programs ADD COLUMN startDate INTEGER"
