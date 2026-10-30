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

/**
 * `retired_movements` loses the timestamp nothing read (B316).
 *
 * Schema 16 shipped the column with every retirement and no reader. It is dropped by recreating the table
 * rather than `ALTER TABLE … DROP COLUMN`, because the supported API levels carry an SQLite without that
 * statement — and a store already at 16 keeps its rows through the copy.
 */
internal val MIGRATION_16_17 = object : Migration(FROM_16, TO_17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_RETIRED_MOVEMENTS_WITHOUT_AT)
        db.execSQL(COPY_RETIRED_MOVEMENTS)
        db.execSQL(DROP_RETIRED_MOVEMENTS)
        db.execSQL(RENAME_RETIRED_MOVEMENTS)
    }
}

private const val FROM_16 = 16
private const val TO_17 = 17

private const val CREATE_RETIRED_MOVEMENTS_WITHOUT_AT = """
    CREATE TABLE IF NOT EXISTS `retired_movements_new` (
        `id` TEXT NOT NULL,
        `survivorId` TEXT NOT NULL,
        PRIMARY KEY(`id`)
    )
"""

private const val COPY_RETIRED_MOVEMENTS =
    "INSERT INTO `retired_movements_new` (id, survivorId) SELECT id, survivorId FROM `retired_movements`"

private const val DROP_RETIRED_MOVEMENTS = "DROP TABLE `retired_movements`"

private const val RENAME_RETIRED_MOVEMENTS =
    "ALTER TABLE `retired_movements_new` RENAME TO `retired_movements`"
