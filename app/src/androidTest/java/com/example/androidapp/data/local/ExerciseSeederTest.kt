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
import com.example.androidapp.domain.model.RowKind
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

    @Test
    fun theBackExtensionMovement_isSeededAndFiledUnderItsFamily() = runTest {
        // B148, on the shape the fix produces: the movement is a movement (so every picker offers it and a
        // logged set may name it) and it is filed under the family, which is where its muscles come from.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()

            val movement = dao.findById("back-extension")
            assertNotNull("the movement the id collision evicted", movement)
            assertEquals(RowKind.MOVEMENT, movement?.rowKind)
            assertEquals("back-extension-family", movement?.parentId)

            val family = dao.findById("back-extension-family")
            assertNotNull("the family the movement is filed under", family)
            assertEquals(RowKind.CATEGORY, family?.rowKind)
            assertEquals(MuscleGroup.LOWER_BACK, family?.primaryMuscle)
        } finally {
            database.close()
        }
    }

    @Test
    fun aDatabaseHoldingTheCollidedCategory_movesItAsideAndKeepsItsChildren() = runTest {
        // The upgrade path. A 2.6 file holds the *category* under `back-extension` with a self-parent, and
        // `INSERT OR IGNORE` would keep it there for ever — so the seeder moves the id aside before it tops
        // up, and the movement the corrected seed declares can land. The self-parent is planted with raw SQL
        // on purpose: no write path can produce one (`requireValidLibraryShape` refuses it), and this is the
        // only faithful reproduction of what 2.6 wrote.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            // The movement has to go first: 2.6 could not hold both rows under one id, because that is the
            // primary key the collision was resolved against — the movement is the one `INSERT OR IGNORE`
            // discarded. Only then can the category take the freed id.
            raw.execSQL("DELETE FROM exercises WHERE id = 'back-extension' AND rowKind = 'MOVEMENT'")
            raw.execSQL(
                "UPDATE exercises SET id = 'back-extension', parentId = 'back-extension' " +
                    "WHERE id = 'back-extension-family'",
            )
            assertNull("the broken shape is the starting point", dao.findById("back-extension-family"))

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            val repaired = dao.findById("back-extension-family")
            assertNotNull("the family kept the row, under the id the seed now declares", repaired)
            assertEquals(RowKind.CATEGORY, repaired?.rowKind)
            assertNull("a category sits at the top level", repaired?.parentId)

            val movement = dao.findById("back-extension")
            assertEquals("and the movement is the row that id now names", RowKind.MOVEMENT, movement?.rowKind)
            assertNotNull("the family's other child is untouched", dao.findById("reverse-hyperextension"))
        } finally {
            database.close()
        }
    }

    @Test
    fun aCategoryALifterHasRenamed_isNeverMoved() = runTest {
        // The repair's guard. A rename is exactly what `INSERT OR IGNORE` exists to protect (N95's rule), and
        // the repair runs on **every** open — so the id may only be moved while the row still looks like the
        // seed's. The self-parent alone is not enough to identify it: this plants the same broken link under a
        // name the lifter chose, which is the state a guard reading `parentId` alone would clobber.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            raw.execSQL("DELETE FROM exercises WHERE id = 'back-extension' AND rowKind = 'MOVEMENT'")
            raw.execSQL(
                "UPDATE exercises SET id = 'back-extension', parentId = 'back-extension', " +
                    "name = 'Lower Back Machine' WHERE id = 'back-extension-family'",
            )

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            val renamed = dao.findById("back-extension")
            assertEquals("the row stays where the lifter left it", "Lower Back Machine", renamed?.name)
            assertEquals(RowKind.CATEGORY, renamed?.rowKind)
            assertNotNull(
                "and the corrected family arrives beside it rather than on top of it",
                dao.findById("back-extension-family"),
            )
        } finally {
            database.close()
        }
    }

    private companion object {
        const val SEEDED_AT = 1_700_000_000_000L
    }
}
