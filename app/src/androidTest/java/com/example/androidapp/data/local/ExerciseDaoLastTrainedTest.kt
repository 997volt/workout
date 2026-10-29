package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The library's last-trained read (ROADMAP N230).
 *
 * Three conditions in one query — finished, not deleted, grouped per movement — and each is a different
 * answer: an abandoned session would claim a lift was trained, a session that was removed would claim it was
 * trained *then*, and a set with no session would claim nothing at all. The newest finished session wins,
 * because the question is "when did I last do this" rather than which sessions exist.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseDaoLastTrainedTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theNewestFinishedSession_wins() = runTest {
        movement("squat")
        trained("squat", sessionId = "s1", startedAt = 1_000L)
        trained("squat", sessionId = "s2", startedAt = 5_000L)

        val last = dao.observeLastTrained().first().single()

        assertEquals("library-squat", last.exerciseId)
        assertEquals(5_000L, last.trainedAt)
    }

    @Test
    fun aSessionThatWasNeverFinished_doesNotCount() = runTest {
        // P3.5's rule for the same question: an abandoned session is not training, so it is not a date.
        movement("squat")
        trained("squat", sessionId = "open", startedAt = 9_000L, finishedAt = null)

        assertEquals(emptyList<LastTrainedRow>(), dao.observeLastTrained().first())
    }

    @Test
    fun aRemovedSession_andARemovedExercise_doNotCount() = runTest {
        movement("squat")
        movement("bench")
        trained("squat", sessionId = "gone", startedAt = 9_000L, deleted = true)
        trained("bench", sessionId = "live", startedAt = 4_000L, exerciseDeleted = true)

        assertEquals(emptyList<LastTrainedRow>(), dao.observeLastTrained().first())
    }

    @Test
    fun eachMovementGetsItsOwnDate() = runTest {
        movement("squat")
        movement("bench")
        trained("squat", sessionId = "s1", startedAt = 1_000L)
        trained("bench", sessionId = "s2", startedAt = 2_000L)

        val trained = dao.observeLastTrained().first().associate { it.exerciseId to it.trainedAt }

        assertEquals(mapOf("library-squat" to 1_000L, "library-bench" to 2_000L), trained)
    }

    /** A library row, because the join resolves through the session's own reference to it. */
    private suspend fun movement(id: String) {
        dao.insertAll(
            listOf(
                ExerciseEntity(
                    id = "library-$id",
                    name = id.replaceFirstChar { it.uppercase() },
                    primaryMuscle = MuscleGroup.QUADS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.SQUAT,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
            ),
        )
    }

    /** One movement in one session, with a set so the session is more than a shell. */
    private suspend fun trained(
        id: String,
        sessionId: String,
        startedAt: Long,
        finishedAt: Long? = startedAt + 1,
        deleted: Boolean = false,
        exerciseDeleted: Boolean = false,
    ) {
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = sessionId,
                startedAt = startedAt,
                finishedAt = finishedAt,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                createdAt = startedAt,
                updatedAt = startedAt,
                deletedAt = if (deleted) startedAt else null,
            ),
        )
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "se-$sessionId-$id",
                sessionId = sessionId,
                exerciseId = "library-$id",
                position = 0,
                finishedAt = null,
                muscleFeel = null,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = if (exerciseDeleted) 1L else null,
            ),
        )
    }
}
