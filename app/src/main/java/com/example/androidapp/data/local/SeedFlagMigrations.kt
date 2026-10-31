package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The flags' provenance (ROADMAP B327).
 *
 * One nullable column: when the seed last wrote `isBodyweight`/`isTimed`, null while it has not. Every row
 * that predates it is null, which is the honest answer — the past cannot say whether a 0 was the old seeder's
 * or the lifter's. The next open's one-time repair raises those rows to the seed's truth and stamps them, and
 * `MAX` keeps it from ever lowering a flag the lifter set. A new seed row is stamped at insert, and the
 * editor stamps a lifter's own flag change, so the repair is one-time per row rather than a per-open
 * re-assertion.
 */
internal val MIGRATION_17_18 = object : Migration(FROM_17, TO_18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SEED_FLAGS_AT)
    }
}

private const val FROM_17 = 17
private const val TO_18 = 18
private const val ADD_SEED_FLAGS_AT = "ALTER TABLE exercises ADD COLUMN seedFlagsAt INTEGER"
