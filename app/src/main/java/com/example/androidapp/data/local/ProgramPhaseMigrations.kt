package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A program's day can name the authored week it belongs to (ROADMAP N292).
 *
 * One nullable column and nothing moved. Every row already on disk keeps `phaseWeek` null, which is
 * exactly what it was: a weekly program whose single week repeats forever. A phased program numbers
 * its weeks from one, so the absent value stays free to mean "no phases" rather than week zero.
 *
 * The column is read by the code added beside it — the phase derivation, home, adherence, the run and
 * the editor — and a null there is the old behaviour rather than a missing value.
 */
internal val MIGRATION_22_23 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_PHASE_WEEK)
    }
}

private const val FROM_VERSION = 22
private const val TO_VERSION = 23

/** Nullable, so no default is needed and "no phases" is the absent value. */
private const val ADD_PHASE_WEEK =
    "ALTER TABLE program_slots ADD COLUMN phaseWeek INTEGER"
