package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A program's day gains its authored phase week, carried by [MIGRATION_22_23] (ROADMAP N292).
 *
 * The thing worth proving is what every row already on disk becomes: **no phase week**, which is
 * the single week that repeats and the behaviour every one of them had. The v22 shape is made by
 * dropping the column from a store Room created at the current version, the direct shape
 * [ProgramMigrationTest] uses for the columns it owns.
 */
@RunWith(AndroidJUnit4::class)
class ProgramPhaseMigrationTest {

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
    fun aProgramDayAlreadyOnDisk_hasNoAuthoredWeek() = runTest {
        val db = v22Store()
        db.execSQL(
            "INSERT INTO templates (id, name, versionOf, createdAt, updatedAt, deletedAt) " +
                "VALUES ('t1', 'Heavy lower', NULL, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p1', 'Upper/Lower', 1, 0, 'WEEKLY', NULL, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO program_slots " +
                "(id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt) " +
                "VALUES ('s1', 'p1', 't1', 0, 'MONDAY', 1, 1, NULL)",
        )

        MIGRATION_22_23.migrate(db)

        val slot = database.programDao().findSlot("s1")
        assertNull("the old single week is the absent value, not week zero", slot?.phaseWeek)
        assertEquals("and the day itself is untouched", 0, slot?.position)
    }

    @Test
    fun aWeekWrittenAfterTheMigration_isReadBack() = runTest {
        val db = v22Store()

        MIGRATION_22_23.migrate(db)
        db.execSQL(
            "INSERT INTO templates (id, name, versionOf, createdAt, updatedAt, deletedAt) " +
                "VALUES ('t2', 'Week 2 push', NULL, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p2', 'Phased', 0, 1, 'WEEKLY', 20731, 1, 1, NULL)",
        )
        db.execSQL(
            "INSERT INTO program_slots " +
                "(id, programId, templateId, position, weekday, phaseWeek, createdAt, updatedAt, deletedAt) " +
                "VALUES ('s2', 'p2', 't2', 0, 'WEDNESDAY', 2, 1, 1, NULL)",
        )

        assertEquals(2, database.programDao().findSlot("s2")?.phaseWeek)
    }

    /** The v22 shape: the column the migration adds, dropped. */
    private fun v22Store() = database.openHelper.writableDatabase.apply {
        execSQL("ALTER TABLE program_slots DROP COLUMN phaseWeek")
    }
}
