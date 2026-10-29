package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A movement id the library merged away is remembered (ROADMAP N238).
 *
 * One table, empty on every store that existed: a merge writes a row only when it retires an id, and no store
 * has merged anything before this migration ran. Nothing is backfilled, because the ids this would have recorded
 * are ids the app never knew were duplicates.
 */
internal val MIGRATION_15_16 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_RETIRED_MOVEMENTS)
    }
}

private const val FROM_VERSION = 15
private const val TO_VERSION = 16

private const val CREATE_RETIRED_MOVEMENTS = """
    CREATE TABLE IF NOT EXISTS `retired_movements` (
        `id` TEXT NOT NULL,
        `survivorId` TEXT NOT NULL,
        `at` INTEGER NOT NULL,
        PRIMARY KEY(`id`)
    )
"""
