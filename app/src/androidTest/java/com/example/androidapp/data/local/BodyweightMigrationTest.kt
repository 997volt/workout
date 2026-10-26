package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A bodyweight set and a bodyweight movement, carried by [MIGRATION_9_10] (ROADMAP N219).
 *
 * Two plain ALTERs, so this is the direct shape [ActivityMigrationTest] uses: the two tables are rewound to
 * their schema-9 shape, a row written into each, and the migration run against them. What it proves is that
 * both columns mean **exactly what was already true** — an ordinary movement and a set loaded at its added
 * weight — and that each row survives the ALTER with its own values.
 */
@RunWith(AndroidJUnit4::class)
class BodyweightMigrationTest {

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
    fun anExerciseWrittenBeforeThis_isAnOrdinaryMovement() = runTest {
        val db = v9Store()

        MIGRATION_9_10.migrate(db)

        db.query("SELECT isBodyweight, name FROM exercises WHERE id = 'pull-up'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("nobody had marked a movement bodyweight yet", 0, cursor.getInt(0))
            assertEquals("and the row keeps its own values", "Pull-up", cursor.getString(1))
        }
    }

    @Test
    fun aSetWrittenBeforeThis_carriesNoBodyweight() = runTest {
        val db = v9Store()

        MIGRATION_9_10.migrate(db)

        db.query("SELECT bodyweightGrams, reps FROM set_entries WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertTrue("null is the added weight, which is what the set was", cursor.isNull(0))
            assertEquals("and the set keeps its own values", 8, cursor.getInt(1))
        }
    }

    @Test
    fun aBodyweightSnapshot_writtenAfterTheMigration_survives() = runTest {
        val db = v9Store()
        MIGRATION_9_10.migrate(db)

        db.execSQL("UPDATE set_entries SET bodyweightGrams = 75000 WHERE id = 's1'")

        db.query("SELECT bodyweightGrams FROM set_entries WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(75_000L, cursor.getLong(0))
        }
    }

    /**
     * A reduced v9-shaped store, not `9.json`: enough columns for the ALTERs and the assertions, and no more.
     *
     * Only the columns the migration and the assertions need are declared, so this proves the ALTERs and what
     * they leave behind — **not** that the result matches `10.json`, which only Room's own validation could say
     * (B255). The user's rows go with the drops,
     * and this migration touches nothing else.
     */
    private fun v9Store() = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE set_entries")
        execSQL("DROP TABLE exercises")
        execSQL(V9_EXERCISES)
        execSQL(V9_SET_ENTRIES)
        execSQL("INSERT INTO exercises (id, name) VALUES ('pull-up', 'Pull-up')")
        execSQL(
            "INSERT INTO set_entries (id, sessionExerciseId, setIndex, reps, weightGrams, setType) " +
                "VALUES ('s1', 'se1', 0, 8, 0, 'NORMAL')",
        )
    }

    private companion object {
        /** `9.json`'s shape for `exercises`, minus the column this migration adds. */
        const val V9_EXERCISES = """
            CREATE TABLE IF NOT EXISTS `exercises` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """

        /** `9.json`'s shape for `set_entries`, minus the column this migration adds. */
        const val V9_SET_ENTRIES = """
            CREATE TABLE IF NOT EXISTS `set_entries` (
                `id` TEXT NOT NULL,
                `sessionExerciseId` TEXT NOT NULL,
                `setIndex` INTEGER NOT NULL,
                `reps` INTEGER NOT NULL,
                `weightGrams` INTEGER NOT NULL,
                `setType` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """
    }
}
