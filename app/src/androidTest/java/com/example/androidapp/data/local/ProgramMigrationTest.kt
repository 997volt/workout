package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.ProgramKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A program's kind and its authored start date, carried by [MIGRATION_7_8] (ROADMAP N174).
 *
 * Two columns added to a table that already has rows, so the thing worth proving is what those rows
 * become: **WEEKLY**, which is the rule every one of them was written under, and an absent start,
 * which reads as "the day it was created". The v7 shape is made by dropping the two columns from a
 * store Room created at the current version, the same "make the migration the thing being tested"
 * shape [DeloadMigrationTest] uses.
 */
@RunWith(AndroidJUnit4::class)
class ProgramMigrationTest {

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
    fun aProgramAlreadyOnDisk_becomesWeekly_withNoAuthoredStart() = runTest {
        val db = v7Store()
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p1', 'Upper/Lower', 1, 0, 1, 1, NULL)",
        )

        MIGRATION_7_8.migrate(db)

        val program = database.programDao().findProgram("p1")
        assertEquals("the rule it was written under is the value it reads", ProgramKind.WEEKLY, program?.kind)
        assertNull("no authored start, so the creation date is the reading", program?.startDate)
    }

    @Test
    fun aSequenceRow_keepsItsKindAndItsStart() = runTest {
        val db = v7Store()

        MIGRATION_7_8.migrate(db)
        db.execSQL(
            "INSERT INTO programs " +
                "(id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p2', 'Peak', 0, 1, 'SEQUENCE', 20305, 1, 1, NULL)",
        )

        val program = database.programDao().findProgram("p2")
        assertEquals(ProgramKind.SEQUENCE, program?.kind)
        assertEquals(20305L, program?.startDate)
    }

    @Test
    fun aProgramWrittenAfterTheMigration_namesNoKindAndGetsOne() = runTest {
        // B193: the migrated column had a SQL default the exported schema did not, so this insert
        // worked on a migrated store and failed on a fresh one. A writer that does not know the column
        // is the shape that found it: DeloadMigrationTest's own parent insert.
        val db = v7Store()

        MIGRATION_7_8.migrate(db)
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p3', 'Weekly', 0, 2, 1, 1, NULL)",
        )

        assertEquals(ProgramKind.WEEKLY, database.programDao().findProgram("p3")?.kind)
    }

    /**
     * The v7 shape: the two columns the migration adds, dropped.
     *
     * SQLite's `DROP COLUMN` is in the versions every emulator this runs on has; dropping rather than
     * casting a hand-written table keeps Room's own indices and constraints out of the test.
     */
    private fun v7Store() = database.openHelper.writableDatabase.apply {
        execSQL("ALTER TABLE programs DROP COLUMN kind")
        execSQL("ALTER TABLE programs DROP COLUMN startDate")
    }
}
