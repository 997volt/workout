package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Where a removed planned set should come back (ROADMAP B364).
 *
 * The survivors are renumbered when a set is removed, so their indexes no longer say where the gap was,
 * and a later ramp addition moves every index again. The row above the removed block is a stable anchor:
 * it keeps its identity while the numbers move. Null on every row that existed before this column, and on
 * every row the program-document edit removes, which has no plan undo; restore falls back to the old count
 * for those.
 */
internal val MIGRATION_18_19 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_RESTORE_AFTER_ID)
    }
}

private const val FROM_VERSION = 18
private const val TO_VERSION = 19

private const val ADD_RESTORE_AFTER_ID = "ALTER TABLE template_sets ADD COLUMN restoreAfterId TEXT"
