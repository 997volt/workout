package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The body's record split in two (ROADMAP N113).
 *
 * Two things happen, and the first is why this is a table rebuild rather than two `ALTER`s: SQLite
 * cannot relax a `NOT NULL`, so `measurements` is recreated with the same columns, a `kind`, and a
 * weight that may be absent.
 *
 * **No measurement is dropped.** Every row the old shape held was a whole reading of the body, so each
 * becomes that day's **daily** entry — it is the one with a weight, which was the field the old shape
 * required — and a row that took any tape site *also* becomes that day's **tape** entry under an id of
 * its own. A day holding one of each kind is exactly the shape the split introduces, so a legacy entry
 * that measured both lands as the two records it always was.
 *
 * A session's own sore list is deliberately **left alone**: those rows are what a workout opened with,
 * they still read in history, and nothing new is written to them (N113).
 *
 * The statements are file-level values rather than lines in [Migration.migrate] so the function stays
 * the shape this project asks for: what happens, in order, with the SQL out of the way.
 */
internal val MIGRATION_2_3 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_MEASUREMENTS)
        db.execSQL(COPY_EACH_ROW_AS_DAILY)
        db.execSQL(COPY_EACH_MEASURED_ROW_AS_TAPE)
        db.execSQL("DROP TABLE measurements")
        db.execSQL("ALTER TABLE measurements_new RENAME TO measurements")
        db.execSQL(CREATE_SORE_MUSCLES)
        db.execSQL(CREATE_SORE_MUSCLES_INDEX)
    }
}

private const val FROM_VERSION = 2
private const val TO_VERSION = 3

/** The old table's columns, with `kind` stated and the weight allowed to be absent. */
private val CREATE_MEASUREMENTS = """
    CREATE TABLE measurements_new (
        id TEXT NOT NULL PRIMARY KEY,
        measuredAt INTEGER NOT NULL,
        kind TEXT NOT NULL,
        weightGrams INTEGER,
        bodyFatTenths INTEGER,
        muscleTenths INTEGER,
        neckMm INTEGER,
        chestMm INTEGER,
        waistMm INTEGER,
        hipsMm INTEGER,
        upperArmMm INTEGER,
        thighMm INTEGER,
        calfMm INTEGER,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        deletedAt INTEGER
    )
""".trimIndent()

/** Every row the old shape held was a whole reading of the body: that is the day's daily entry. */
private val COPY_EACH_ROW_AS_DAILY = """
    INSERT INTO measurements_new (
        id, measuredAt, kind, weightGrams, bodyFatTenths, muscleTenths,
        neckMm, chestMm, waistMm, hipsMm, upperArmMm, thighMm, calfMm,
        createdAt, updatedAt, deletedAt
    )
    SELECT id, measuredAt, 'DAILY', weightGrams, bodyFatTenths, muscleTenths,
           NULL, NULL, NULL, NULL, NULL, NULL, NULL,
           createdAt, updatedAt, deletedAt
    FROM measurements
""".trimIndent()

/** And one that took any site becomes that day's tape entry, under an id of its own. */
private val COPY_EACH_MEASURED_ROW_AS_TAPE = """
    INSERT INTO measurements_new (
        id, measuredAt, kind, weightGrams, bodyFatTenths, muscleTenths,
        neckMm, chestMm, waistMm, hipsMm, upperArmMm, thighMm, calfMm,
        createdAt, updatedAt, deletedAt
    )
    SELECT id || '-tape', measuredAt, 'TAPE', NULL, NULL, NULL,
           neckMm, chestMm, waistMm, hipsMm, upperArmMm, thighMm, calfMm,
           createdAt, updatedAt, deletedAt
    FROM measurements
    WHERE neckMm IS NOT NULL OR chestMm IS NOT NULL OR waistMm IS NOT NULL
       OR hipsMm IS NOT NULL OR upperArmMm IS NOT NULL OR thighMm IS NOT NULL
       OR calfMm IS NOT NULL
""".trimIndent()

/** The sore list the daily record now owns, keyed to the entry it describes. */
private val CREATE_SORE_MUSCLES = """
    CREATE TABLE IF NOT EXISTS measurement_sore_muscles (
        id TEXT NOT NULL PRIMARY KEY,
        measurementId TEXT NOT NULL,
        muscle TEXT NOT NULL,
        score INTEGER NOT NULL,
        position INTEGER NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        deletedAt INTEGER,
        FOREIGN KEY(measurementId) REFERENCES measurements(id)
            ON UPDATE NO ACTION ON DELETE CASCADE
    )
""".trimIndent()

private const val CREATE_SORE_MUSCLES_INDEX =
    "CREATE INDEX IF NOT EXISTS index_measurement_sore_muscles_measurementId " +
        "ON measurement_sore_muscles (measurementId)"
