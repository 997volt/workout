package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A marked deload week can prescribe its own days (ROADMAP N147).
 *
 * One new table and two indices, hanging off the `program_deloads` mark P3.10 already keeps. Nothing
 * already on disk changes: a week marked before this build has no days, which is exactly the "the mark
 * exempts the week and its slots still prescribe their workouts" shape P3.10 shipped.
 */
internal val MIGRATION_6_7 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_DELOAD_DAYS)
        db.execSQL(INDEX_DELOAD)
        db.execSQL(INDEX_TEMPLATE)
    }
}

private const val FROM_VERSION = 6
private const val TO_VERSION = 7

/**
 * The table Room expects from [ProgramDeloadDayEntity].
 *
 * Both foreign keys cascade: a hard delete of the week or the template takes the day with it, while the
 * app's own soft deletes leave the row for the export (P1.12). The weekday is nullable because an
 * order-only source slot has none, and the day stores its own rather than deriving it — the creator lets
 * the lifter move one (N147).
 */
private const val CREATE_DELOAD_DAYS = """
    CREATE TABLE IF NOT EXISTS `program_deload_days` (
        `id` TEXT NOT NULL,
        `deloadId` TEXT NOT NULL,
        `templateId` TEXT NOT NULL,
        `position` INTEGER NOT NULL,
        `weekday` TEXT,
        `createdAt` INTEGER NOT NULL,
        `updatedAt` INTEGER NOT NULL,
        `deletedAt` INTEGER,
        PRIMARY KEY(`id`),
        FOREIGN KEY(`deloadId`) REFERENCES `program_deloads`(`id`)
            ON UPDATE NO ACTION ON DELETE CASCADE,
        FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`)
            ON UPDATE NO ACTION ON DELETE CASCADE
    )
"""

private const val INDEX_DELOAD =
    "CREATE INDEX IF NOT EXISTS `index_program_deload_days_deloadId` " +
        "ON `program_deload_days` (`deloadId`)"

private const val INDEX_TEMPLATE =
    "CREATE INDEX IF NOT EXISTS `index_program_deload_days_templateId` " +
        "ON `program_deload_days` (`templateId`)"
