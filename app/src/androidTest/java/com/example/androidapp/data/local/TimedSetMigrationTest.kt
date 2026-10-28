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
 * A hold, and a movement whose sets are holds, carried by [MIGRATION_10_11] (ROADMAP N220).
 *
 * The same direct shape [BodyweightMigrationTest] uses: both tables are rewound to their schema-10 shape, a
 * row written into each, and the migration run against them. What it proves is that both columns mean what was
 * already true — a counted movement and a counted set — and that each row keeps its own values through the
 * ALTER.
 */
@RunWith(AndroidJUnit4::class)
class TimedSetMigrationTest {

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
    fun aMovementWrittenBeforeThis_isCounted() = runTest {
        val db = v10Store()

        MIGRATION_10_11.migrate(db)

        db.query("SELECT isTimed, name FROM exercises WHERE id = 'plank'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("nobody had marked a movement timed yet", 0, cursor.getInt(0))
            assertEquals("and the row keeps its own values", "Plank", cursor.getString(1))
        }
    }

    @Test
    fun aSetWrittenBeforeThis_hasNoHold() = runTest {
        val db = v10Store()

        MIGRATION_10_11.migrate(db)

        db.query("SELECT durationSeconds, reps FROM set_entries WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertTrue("null is a counted set, which is what it was", cursor.isNull(0))
            assertEquals("and the set keeps its own rep count", 8, cursor.getInt(1))
        }
    }

    @Test
    fun aHoldWrittenAfterTheMigration_survives() = runTest {
        val db = v10Store()
        MIGRATION_10_11.migrate(db)

        db.execSQL("UPDATE set_entries SET durationSeconds = 90, reps = 0 WHERE id = 's1'")

        db.query("SELECT durationSeconds, reps FROM set_entries WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(90, cursor.getInt(0))
            assertEquals("a hold states no reps", 0, cursor.getInt(1))
        }
    }

    /** A schema-10 store with both tables rewound, the same shape the bodyweight migration's test builds. */
    private fun v10Store() = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE set_entries")
        execSQL("DROP TABLE exercises")
        execSQL(V10_EXERCISES)
        execSQL(V10_SET_ENTRIES)
        execSQL("INSERT INTO exercises (id, name) VALUES ('plank', 'Plank')")
        execSQL(
            "INSERT INTO set_entries (id, sessionExerciseId, setIndex, reps, weightGrams, setType) " +
                "VALUES ('s1', 'se1', 0, 8, 0, 'NORMAL')",
        )
    }

    private companion object {
        /**
         * Enough of a schema-10 `exercises` for the two ALTERs and the assertions, and no more.
         *
         * The full schema-10 store is built from `10.json` by the unit suite's `MigrationSchemaTest`,
         * which is where Room validates the result; these reduced tables are for the row-level checks.
         */
        const val V10_EXERCISES = """
            CREATE TABLE IF NOT EXISTS `exercises` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """

        /** Enough of a schema-10 `set_entries` for the row-level checks, and no more (see above). */
        const val V10_SET_ENTRIES = """
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
