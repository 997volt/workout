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
    fun aSeededMovement_arrivesWithTheFlagsItStates() = runTest {
        // B293: the insert wrote ten columns and the two that say how a movement is loaded were not among
        // them, so every seeded row arrived as an ordinary counted lift. This is the raw column list proving
        // the seed, the entity and the statement still agree.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()

            assertEquals(true, dao.findById("pull-up")?.isBodyweight)
            // Assisted work is body-loaded too: the machine takes weight off rather than adding it (N15).
            assertEquals(true, dao.findById("assisted-pull-up")?.isBodyweight)
            assertEquals(true, dao.findById("plank")?.isTimed)
            assertEquals(false, dao.findById("barbell-bench-press")?.isBodyweight)
            assertEquals(false, dao.findById("barbell-bench-press")?.isTimed)
        } finally {
            database.close()
        }
    }

    @Test
    fun aDeviceFromBeforeTheFlags_isRaised_andALiftersRaiseIsKept() = runTest {
        // The upgrade path (B293). A row already on the device says "not bodyweight" because no seeder ever
        // wrote the column, and it still carries the never-edited marker, so the repair raises it to what the
        // seed declares — and it only ever raises, so a movement the lifter marked themselves keeps it.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            raw.execSQL("UPDATE exercises SET isBodyweight = 0 WHERE id = 'pull-up'")
            raw.execSQL("UPDATE exercises SET isBodyweight = 1 WHERE id = 'barbell-bench-press'")
            assertEquals(false, dao.findById("pull-up")?.isBodyweight)

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals("the seed's own truth is restored", true, dao.findById("pull-up")?.isBodyweight)
            assertEquals(
                "and a raise the lifter made is never lowered",
                true,
                dao.findById("barbell-bench-press")?.isBodyweight,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun aFlagTheLifterTurnedOff_isNotRaisedAgain() = runTest {
        // B299: an edit bumps updatedAt, the row's own record that the seed is no longer its only writer. The
        // repair used to re-assert the seed on every open, so a movement the lifter unmarked came back.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase
            raw.execSQL(
                "UPDATE exercises SET isBodyweight = 0, updatedAt = createdAt + 1 WHERE id = 'pull-up'",
            )

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals(
                "the lifter's answer is not the seed's to overwrite",
                false,
                dao.findById("pull-up")?.isBodyweight,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun aMovementTheLifterUnfiled_isNotFiledAgain() = runTest {
        // B299: the filing repair has the same shape. A row the lifter put at the top level stays there.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase
            raw.execSQL(
                "UPDATE exercises SET parentId = NULL, updatedAt = createdAt + 1 " +
                    "WHERE id = 'reverse-hyperextension'",
            )

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertNull(
                "the lifter's unfile is not the seed's to undo",
                dao.findById("reverse-hyperextension")?.parentId,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun theRetiredDeadlift_isInTheMap_evenOnAStoreThatNeverHeldIt() = runTest {
        // B300: the map is what a program document exported before the retirement resolves through. The seed
        // records its static retirement on every open, so a fresh install — where the merge is a no-op — has
        // the line too, and a second open does not duplicate it.
        val database = databaseWithSeeding()
        try {
            val raw = database.openHelper.writableDatabase
            val first = database.movementMergeDao().retiredMovements().single()
            assertEquals("deadlift", first.id)
            assertEquals("conventional-deadlift", first.survivorId)

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals(
                "a second open does not duplicate it",
                1,
                database.movementMergeDao().retiredMovements().size,
            )
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
    fun aDatabaseHoldingTheCollidedCategory_freesTheIdAndKeepsTheFamilysChildren() = runTest {
        // The upgrade path (B292, replacing B148's repair). A 2.6 file holds the *category* under
        // `back-extension`, and `INSERT OR IGNORE` would keep it there for ever — so the seeder resolves the
        // impostor before it tops up, and the movement the corrected seed declares can land.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            // The movement has to go first: 2.6 could not hold both rows under one id, because that is the
            // primary key the collision was resolved against — the movement is the one `INSERT OR IGNORE`
            // discarded. Only then can the category take the freed id, and it is planted with no parent,
            // which is the shape B148's guard asked about and never matched.
            raw.execSQL("DELETE FROM exercises WHERE id = 'back-extension' AND rowKind = 'MOVEMENT'")
            raw.execSQL(
                "UPDATE exercises SET id = 'back-extension', parentId = NULL " +
                    "WHERE id = 'back-extension-family'",
            )
            // The children are the other half of the 2.6 shape: they point at the category whose id the
            // movement now claims. Planting them on the seed's family instead is what made this test pass
            // before the repair ran (B296).
            raw.execSQL(
                "UPDATE exercises SET parentId = 'back-extension' WHERE id = 'reverse-hyperextension'",
            )
            assertEquals(
                "the children start under the id the movement claims",
                "back-extension",
                dao.findById("reverse-hyperextension")?.parentId,
            )
            assertNull("the broken shape is the starting point", dao.findById("back-extension-family"))

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            val movement = dao.findById("back-extension")
            assertEquals("the id now names the movement", RowKind.MOVEMENT, movement?.rowKind)
            assertEquals("and it is filed under its family", "back-extension-family", movement?.parentId)
            assertEquals(
                "while the family is the one the seed declares",
                RowKind.CATEGORY,
                dao.findById("back-extension-family")?.rowKind,
            )
            assertEquals(
                "the family's child is re-filed onto it rather than lost with the impostor",
                "back-extension-family",
                dao.findById("reverse-hyperextension")?.parentId,
            )
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

    @Test
    fun aFamilyWhoseMusclesWereCorrected_isRewritten_onlyWhereTheSeedLeftIt() = runTest {
        // N240: a correction to a family is a write behind the insert, because the row is already there. The
        // guard is the value the seed shipped before, so a lifter's own list survives.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            // A device from before the audit: the row family names upper back and biceps only.
            raw.execSQL("UPDATE exercises SET secondaryMuscles = 'BICEPS' WHERE id = 'row'")
            // And a lifter's own list on the pulldown family, which the repair must not touch.
            raw.execSQL("UPDATE exercises SET secondaryMuscles = 'FOREARMS' WHERE id = 'lat-pulldown-family'")

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals(
                listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
                dao.findById("row")?.secondaryMuscles,
            )
            assertEquals(
                "a list the lifter set is theirs",
                listOf(MuscleGroup.FOREARMS),
                dao.findById("lat-pulldown-family")?.secondaryMuscles,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun anUnfiledMovement_isFiledByTheNextTopUp() = runTest {
        // N229: the seed left eight movements loose, and a row already on a device was never updated by the
        // filing — `INSERT OR IGNORE` keeps what is there. The repair moves the family and nothing else.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            raw.execSQL("UPDATE exercises SET parentId = NULL WHERE id = 'face-pull'")
            assertNull(dao.findById("face-pull")?.parentId)

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals("rear-delt", dao.findById("face-pull")?.parentId)
        } finally {
            database.close()
        }
    }

    @Test
    fun aMovementALifterFiledOrRenamed_isNotMovedByTheRepair() = runTest {
        // The guard: the repair runs on every open, so only a row that still looks like the seed's is filed.
        val database = databaseWithSeeding()
        try {
            val dao = database.exerciseDao()
            val raw = database.openHelper.writableDatabase

            raw.execSQL("UPDATE exercises SET parentId = 'core' WHERE id = 'face-pull'")
            raw.execSQL("UPDATE exercises SET parentId = NULL, name = 'Rope Face Pull' WHERE id = 'dip'")

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertEquals("a filing the lifter changed stays", "core", dao.findById("face-pull")?.parentId)
            assertNull("and a renamed row is theirs", dao.findById("dip")?.parentId)
        } finally {
            database.close()
        }
    }

    @Test
    fun theRetiredDeadlift_isMergedIntoTheConventionalOne_keepingItsHistory() = runTest {
        // N229: the seed shipped a plain *Deadlift* beside the family of the same name, and the bar lift a
        // lifter means is the conventional one. A device that logged the retired row keeps every set, plan
        // target and program slot — they follow the survivor rather than being stranded on a row that goes.
        val database = databaseWithSeeding()
        try {
            val raw = database.openHelper.writableDatabase
            val dao = database.exerciseDao()

            // A device from before the merge: the retired row, one session naming it, and one plan.
            raw.execSQL(
                "INSERT INTO exercises (id, name, secondaryMuscles, movementPattern, rowKind, createdAt, updatedAt) " +
                    "VALUES ('deadlift', 'Deadlift', '', 'HINGE', 'MOVEMENT', 1, 1)",
            )
            raw.execSQL(
                "INSERT INTO workout_sessions (id, startedAt, createdAt, updatedAt) VALUES ('s1', 1, 1, 1)",
            )
            raw.execSQL(
                "INSERT INTO session_exercises (id, sessionId, exerciseId, position, createdAt, updatedAt) " +
                    "VALUES ('se1', 's1', 'deadlift', 0, 1, 1)",
            )
            raw.execSQL(
                "INSERT INTO templates (id, name, createdAt, updatedAt) VALUES ('t1', 'Lower', 1, 1)",
            )
            raw.execSQL(
                "INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt, updatedAt) " +
                    "VALUES ('te1', 't1', 'deadlift', 0, 1, 1)",
            )

            seedMissingExercises(raw, SEEDED_AT + 1_000L)

            assertNull("the retired row is gone", dao.findById("deadlift"))
            assertEquals(
                "the logged set follows the movement that means it",
                "conventional-deadlift",
                raw.singleString("SELECT exerciseId FROM session_exercises WHERE id = 'se1'"),
            )
            assertEquals(
                "and so does the plan",
                "conventional-deadlift",
                raw.singleString("SELECT exerciseId FROM template_exercises WHERE id = 'te1'"),
            )
        } finally {
            database.close()
        }
    }

    /** One value out of one row, for the assertions that read a table the DAOs do not offer. */
    private fun SupportSQLiteDatabase.singleString(sql: String): String? =
        query(sql).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private companion object {
        const val SEEDED_AT = 1_700_000_000_000L
    }
}
