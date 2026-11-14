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
 * The find-the-weight marker (ROADMAP N309, schema 25, B431).
 *
 * One column on a plan's exercise. Every plan already on disk was never marked, because the app had no way
 * to ask for a weightless plan before this; a migration that marked one would make the next session start
 * blank for a reason nobody chose.
 *
 * The old shape is spelled out, not made by dropping a column (B431): `ALTER TABLE … DROP COLUMN` needs
 * SQLite 3.35 (API 31+) while the app supports API 26, and the raw child row needs the foreign keys off
 * because the parent template is not what this test is about. This is the [MeasurementMigrationTest]
 * pattern — the old table as the schema-24 JSON gives it, real rows, then the migration.
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
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL("DROP TABLE template_exercises")
        db.execSQL(
            """
            CREATE TABLE template_exercises (
                id TEXT NOT NULL,
                templateId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                position INTEGER NOT NULL,
                restSeconds INTEGER,
                techniqueNote TEXT,
                targetRpeHalves INTEGER,
                supersetGroup INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                PRIMARY KEY(id),
                FOREIGN KEY(templateId) REFERENCES templates(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt, updatedAt) " +
                "VALUES ('te1', 't1', 'back-squat', 0, 1, 1)",
        )

        MIGRATION_24_25.migrate(db)

        db.query("SELECT calibrationPending FROM template_exercises WHERE id = 'te1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("a plan written before the marker is never marked", 0, cursor.getInt(0))
        }
    }
}
