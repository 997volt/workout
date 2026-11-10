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
 * Auto progression's three new objects (ROADMAP N299, schema 24).
 *
 * One column and two tables. Every program already on disk must come back **manual**, because the
 * app never wrote a target on its own before this column, and a migration that switched one on
 * would start writing to a plan nobody asked it to adjust. The tables start empty for the same
 * reason; there is no history to invent.
 */
@RunWith(AndroidJUnit4::class)
class ProgressionMigrationTest {

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
    fun aProgramThatExistedBeforeTheColumn_isManual_andItsRowsAreGone() = runTest {
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE programs DROP COLUMN autoProgression")
        db.execSQL("DROP TABLE progression_writes")
        db.execSQL("DROP TABLE progression_baselines")
        db.execSQL(
            "INSERT INTO programs (id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt) " +
                "VALUES ('p1', 'Upper/Lower', 0, 0, 'WEEKLY', NULL, 1, 1, NULL)",
        )

        MIGRATION_23_24.migrate(db)

        db.query("SELECT autoProgression FROM programs WHERE id = 'p1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("a program written before the switch is manual", 0, cursor.getInt(0))
        }
    }

    @Test
    fun aWriteAndABaseline_canBeStoredAfterTheMigration() = runTest {
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE programs DROP COLUMN autoProgression")
        db.execSQL("DROP TABLE progression_writes")
        db.execSQL("DROP TABLE progression_baselines")

        MIGRATION_23_24.migrate(db)

        db.execSQL(
            "INSERT INTO progression_writes " +
                "(id, programId, templateSetId, sessionExerciseId, direction, beforeWeightGrams, " +
                "beforeRepsCurrent, beforeDurationSeconds, afterWeightGrams, afterRepsCurrent, " +
                "afterDurationSeconds, appliedAt, revertedAt) " +
                "VALUES ('w1', 'p1', 'ts1', 'se1', 'LOAD', 100000, 8, NULL, 102500, 5, NULL, 5, NULL)",
        )
        db.execSQL(
            "INSERT INTO progression_baselines " +
                "(programId, templateSetId, weightGrams, repsCurrent, durationSeconds, capturedAt) " +
                "VALUES ('p1', 'ts1', 100000, 8, NULL, 5)",
        )

        db.query(
            "SELECT afterWeightGrams, afterRepsCurrent FROM progression_writes WHERE id = 'w1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(102_500L, cursor.getLong(0))
            assertEquals(5, cursor.getInt(1))
        }
        db.query(
            "SELECT weightGrams, repsCurrent FROM progression_baselines WHERE programId = 'p1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(100_000L, cursor.getLong(0))
            assertEquals(8, cursor.getInt(1))
        }
    }
}
