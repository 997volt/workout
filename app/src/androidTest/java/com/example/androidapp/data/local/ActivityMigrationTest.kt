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
 * A session can be an activity rather than a workout, carried by [MIGRATION_5_6] (ROADMAP N141).
 *
 * One nullable column, so this is the direct shape [TemplateMigrationTest] uses: `workout_sessions` is rewound
 * to its schema-5 shape, a row written into it, and the migration run against that. What it proves is the one
 * thing the column means — a session written before activities existed is **a workout**, which is what a null
 * activity is — and that the row survives the ALTER with its own values.
 */
@RunWith(AndroidJUnit4::class)
class ActivityMigrationTest {

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
    fun aSessionWrittenBeforeActivities_isAWorkout() = runTest {
        val db = v5Store(
            "INSERT INTO workout_sessions " +
                "(id, startedAt, readinessNote, createdAt, updatedAt) " +
                "VALUES ('s1', 1000, 'Slept badly', 1, 2)",
        )

        MIGRATION_5_6.migrate(db)

        db.query("SELECT activity, readinessNote FROM workout_sessions WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the start is unknown rather than invented", 0, cursor.getColumnIndex("activity"))
            assertNull("a session written before this column is a workout", cursor.getString(0))
            assertEquals("and the row keeps its own values", "Slept badly", cursor.getString(1))
        }
    }

    @Test
    fun anActivity_writtenAfterTheMigration_isStoredByName() = runTest {
        val db = v5Store()
        MIGRATION_5_6.migrate(db)

        db.execSQL(
            "INSERT INTO workout_sessions (id, startedAt, activity, createdAt, updatedAt) " +
                "VALUES ('s2', 2000, 'SWIMMING', 3, 4)",
        )

        db.query("SELECT activity FROM workout_sessions WHERE id = 's2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("SWIMMING", cursor.getString(0))
        }
    }

    /**
     * A schema-5 store with `workout_sessions` rewound to its old shape.
     *
     * Room creates the file at version 6, so the test drops the table and recreates it as `5.json` spells it.
     * The tables that reference a session carry foreign keys, so their rows go with the drop — this migration
     * touches nothing else, and nothing here reads them.
     */
    private fun v5Store(vararg inserts: String) = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE workout_sessions")
        execSQL(V5_SESSIONS)
        inserts.forEach { execSQL(it) }
    }

    private companion object {
        /** `5.json`'s `createSql` for `workout_sessions`, with the table's real name in place. */
        const val V5_SESSIONS = """
            CREATE TABLE IF NOT EXISTS `workout_sessions` (
                `id` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `finishedAt` INTEGER,
                `notes` TEXT,
                `restEndsAt` INTEGER,
                `readinessNote` TEXT,
                `zoneOffsetMinutes` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `templateId` TEXT,
                PRIMARY KEY(`id`)
            )
        """
    }
}
