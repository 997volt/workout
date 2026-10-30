package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.ProgressionDirection
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A step offered and not taken survives the upgrade (ROADMAP N236, schema 14).
 *
 * The column is nullable with no default, so the migration is a plain ALTER and every row that exists
 * declined nothing — the only honest answer, because a decline is a thing the app did not observe before the
 * column existed. The other half is the codec: the directions travel as names, so a value this build does not
 * know is skipped rather than read as a different direction.
 */
@RunWith(AndroidJUnit4::class)
class DeclinedStepMigrationTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var dao: WorkoutDao
    private lateinit var exercises: SessionExerciseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        dao = database.workoutDao()
        exercises = database.sessionExerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theColumnTheMigrationAdds_isNullForEveryRowThatExisted() = runTest {
        // A row written *before* the ALTER, which is the row the migration has to leave alone (B318).
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE session_exercises DROP COLUMN declinedDirections")
        library()
        dao.insertSession(session())
        db.execSQL(
            "INSERT INTO session_exercises (id, sessionId, exerciseId, position, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(ROW_ID, SESSION_ID, "back-squat", 0, 1L, 1L),
        )

        MIGRATION_13_14.migrate(db)

        assertNull(exercises.findSessionExercise(ROW_ID)?.declinedDirections)
    }

    @Test
    fun aDecline_isReadBack_inTheEnumsOwnOrder() = runTest {
        // Through the DAO and the column, not only the codec (B318): the write, the storage and the read are
        // the round trip the review and History depend on.
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE session_exercises DROP COLUMN declinedDirections")
        library()
        dao.insertSession(session())
        db.execSQL(
            "INSERT INTO session_exercises (id, sessionId, exerciseId, position, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(ROW_ID, SESSION_ID, "back-squat", 0, 1L, 1L),
        )
        MIGRATION_13_14.migrate(db)

        exercises.setDeclinedDirections(ROW_ID, "REPS,LOAD", 2L)

        val stored = exercises.findSessionExercise(ROW_ID)?.declinedDirections
        assertEquals("REPS,LOAD", stored)
        assertEquals(
            "reading it back is the enum's own order",
            listOf(ProgressionDirection.LOAD, ProgressionDirection.REPS),
            declaredDirections(stored).toList(),
        )
    }

    /** The movement the session row points at, because the foreign key is real. */
    private suspend fun library() {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = null,
                    secondaryMuscles = emptyList(),
                    equipment = null,
                    movementPattern = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    deletedAt = null,
                ),
            ),
        )
    }

    private fun session(id: String = SESSION_ID, startedAt: Long = 1L) = WorkoutSessionEntity(
        id = id,
        startedAt = startedAt,
        finishedAt = null,
        notes = null,
        restEndsAt = null,
        readinessNote = null,
        createdAt = startedAt,
        updatedAt = startedAt,
        deletedAt = null,
    )

    private companion object {
        const val SESSION_ID = "s1"
        const val ROW_ID = "se1"
    }
}
