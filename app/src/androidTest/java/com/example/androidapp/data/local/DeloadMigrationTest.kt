package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A marked deload week can prescribe its own days, carried by [MIGRATION_6_7] (ROADMAP N147).
 *
 * One new table and its indices, so this is the direct shape [ActivityMigrationTest] uses: the table is
 * dropped from a store Room created at the current version, the migration runs against it, and a row is
 * written and read back. What it proves is the structure the app now relies on — the day's own weekday,
 * and the two foreign keys that make a day unable to outlive its week or its workout.
 */
@RunWith(AndroidJUnit4::class)
class DeloadMigrationTest {

    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aDayWrittenAfterTheMigration_keepsItsWeek_andItsWeekday() = runTest {
        val db = v6Store()
        MIGRATION_6_7.migrate(db)
        insertParents(db)

        db.execSQL(
            "INSERT INTO program_deload_days " +
                "(id, deloadId, templateId, position, weekday, createdAt, updatedAt, deletedAt) " +
                "VALUES ('day1', 'deload1', 'template1', 2, 'MONDAY', 1, 2, NULL)",
        )

        db.query(
            "SELECT weekday, position, deloadId, templateId FROM program_deload_days WHERE id = 'day1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("MONDAY", cursor.getString(0))
            assertEquals(2, cursor.getInt(1))
            assertEquals("deload1", cursor.getString(2))
            assertEquals("template1", cursor.getString(3))
        }
    }

    @Test
    fun aDay_cannotOutliveItsWeek() = runTest {
        val db = v6Store()
        MIGRATION_6_7.migrate(db)
        insertParents(db)
        db.execSQL(
            "INSERT INTO program_deload_days " +
                "(id, deloadId, templateId, position, weekday, createdAt, updatedAt, deletedAt) " +
                "VALUES ('day1', 'deload1', 'template1', 0, NULL, 1, 1, NULL)",
        )

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM program_deloads WHERE id = 'deload1'")

        db.query("SELECT COUNT(*) FROM program_deload_days").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the hard delete of the week takes its days", 0, cursor.getInt(0))
        }
    }

    /** The parents a day's foreign keys need: a program, its mark, and a template. */
    private fun insertParents(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p1', 'Upper/Lower', 0, 0, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO templates (id, name, versionOf, createdAt, updatedAt, deletedAt) " +
                "VALUES ('template1', 'Heavy lower', NULL, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO program_deloads (id, programId, weekStart, createdAt, updatedAt, deletedAt) " +
                "VALUES ('deload1', 'p1', 20305, 1, 1, NULL)",
        )
    }

    /**
     * The store with the new table dropped, which is the schema-6 shape this migration leaves.
     *
     * Room creates the file at version 7, so the table exists; dropping it makes the migration's
     * `CREATE TABLE` the thing being tested rather than a no-op against Room's own work.
     */
    private fun v6Store() = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE program_deload_days")
    }
}
