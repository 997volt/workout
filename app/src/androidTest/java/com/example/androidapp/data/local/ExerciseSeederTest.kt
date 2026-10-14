package com.example.androidapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for seed delivery (ROADMAP F15).
 *
 * The seeding itself is exercised, rather than the Hilt module that wires it, so
 * the raw SQL's column list is checked against the real entity by a real SQLite.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseSeederTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Mirrors what `DatabaseModule` does on every open. */
    private fun databaseWithSeeding(): WorkoutDatabase =
        Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedMissingExercises(db, SEEDED_AT)
                    }
                },
            )
            .build()

    @Test
    fun theLibraryIsPopulatedBeforeTheFirstReadReturns() = runTest {
        // This is the race F15 fixed. The insert used to be dispatched to an
        // application coroutine, so the first collect emitted an empty library and
        // the UI cannot tell that apart from "your search matched nothing".
        val database = databaseWithSeeding()
        try {
            assertTrue(
                "the seed must land synchronously, in onOpen",
                database.exerciseDao().count() >= 30,
            )
            assertNotNull(database.exerciseDao().findById("back-squat"))
        } finally {
            database.close()
        }
    }

    @Test
    fun aSecondTopUp_neitherDuplicatesNorClobbersASoftDelete() = runTest {
        // The trap the review found: OnConflictStrategy.REPLACE is DELETE + INSERT,
        // and session_exercises.exerciseId is ON DELETE RESTRICT once history
        // exists — so replacing a seeded row would throw. INSERT OR IGNORE cannot.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val seededCount = dao.count()

            // A user removes a seeded exercise.
            dao.softDelete("back-squat", deletedAt = 5L)

            // The next app open tops up again.
            seedMissingExercises(database.openHelper.writableDatabase, SEEDED_AT + 1_000L)

            assertEquals("a top-up must not duplicate rows", seededCount, dao.count())
            assertNull(
                "a user's soft-delete must survive a top-up",
                dao.findById("back-squat"),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun aSeededCategory_statesTheFactsItsMovementsRead() = runTest {
        // ROADMAP N110: a family is the home of the pattern, the primary muscle and the secondary list, so
        // the seed has to write all three — and the seeder's own raw column list is what proves the seed, the
        // entity and the statement still agree.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()

            val bench = dao.findById("bench-press")
            assertNotNull(bench)
            assertEquals(MuscleGroup.CHEST, bench?.primaryMuscle)
            assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), bench?.secondaryMuscles)
            assertEquals(MovementPattern.PRESS, bench?.movementPattern)

            // And the movement under it states its equipment alone, reading the rest from the family.
            val bar = dao.findById("barbell-bench-press")
            assertNull("the movement states no muscle of its own", bar?.primaryMuscle)
            assertEquals(Equipment.BARBELL, bar?.equipment)

            // The same link carries the movement N123 added: a movement the seed writes is filed under the
            // family its `parentOf` entry names, which is where its muscles then come from.
            val curl = dao.findById("behind-the-back-cable-curl")
            assertNotNull(curl)
            assertEquals(Equipment.CABLE, curl?.equipment)
            assertNull("it states no muscle of its own either", curl?.primaryMuscle)
            assertEquals("curl", curl?.parentId)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val SEEDED_AT = 1_700_000_000_000L
    }
}
