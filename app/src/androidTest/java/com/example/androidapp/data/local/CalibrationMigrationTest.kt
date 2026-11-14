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
 * The find-the-weight marker (ROADMAP N309, schema 25).
 *
 * One column on a plan's exercise. Every plan already on disk was never marked, because the app had no way
 * to ask for a weightless plan before this; a migration that marked one would make the next session start
 * blank for a reason nobody chose.
 */
@RunWith(AndroidJUnit4::class)
class CalibrationMigrationTest {

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
    fun aPlanThatExistedBeforeTheMarker_isNotMarked() = runTest {
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE template_exercises DROP COLUMN calibrationPending")
        db.execSQL(
            "INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('te1', 't1', 'back-squat', 0, 1, 1, NULL)",
        )

        MIGRATION_24_25.migrate(db)

        db.query("SELECT calibrationPending FROM template_exercises WHERE id = 'te1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("a plan written before the marker is never marked", 0, cursor.getInt(0))
        }
    }
}
