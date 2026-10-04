package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.repository.StartedSession
import com.example.androidapp.domain.ZoneOffsetSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test

/**
 * Repeating one finished workout, against a real database (ROADMAP N29, N48).
 *
 * Two of this feature's edge cases are SQL rules — a library row deleted since is skipped, and the
 * same exercise twice stays twice — so a fake cannot hold them: it would agree with whatever the
 * query was supposed to do. These seed rows and read back what the app would show.
 *
 * N48 changed the address: the workout is named rather than "the last one", because History offers
 * the action on the row the user is looking at. The addressing gets its own case here.
 */
class RepeatSessionTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomWorkoutRepository

    private val clock = TimeSource { Instant.parse("2026-10-01T08:00:00Z") }

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 })
        database.exerciseDao().insertAll(
            listOf(exercise("back-squat"), exercise("bench-press"), exercise("row")),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun itCopiesTheExercises_inTheOrderTheyWerePerformed() = runTest {
        seedWorkout("past", "back-squat", "bench-press")

        val started = opened("past")
        assertEquals(
            listOf("back-squat", "bench-press"),
            repository.observeSessionExercises(started).first().map { it.exerciseId },
        )
    }

    @Test
    fun itRepeatsTheWorkoutItIsGiven_notTheNewestOne() = runTest {
        // ROADMAP N48: the action is addressed to the row the user is looking at, so a repeat of an
        // older workout must not silently copy the newest one instead.
        seedWorkout("past", "back-squat", finishedAt = 2_000L)
        seedWorkout("newer", "bench-press", finishedAt = 9_000L)

        val started = opened("past")

        assertEquals(
            listOf("back-squat"),
            repository.observeSessionExercises(started).first().map { it.exerciseId },
        )
    }

    @Test
    fun repeatingAWorkoutThatIsGone_reportsNotFound() = runTest {
        // A row can be deleted between the list being drawn and the tap. An empty session would be a
        // silent lie about a workout the user asked for, so the repository refuses instead.
        val result = repository.repeatSession("never-existed")

        assertTrue("a missing workout is reported: $result", result is DataResult.Failure)
        assertTrue(
            "and it is a NotFound",
            (result as DataResult.Failure).error is DataError.NotFound,
        )
    }

    @Test
    fun anExerciseDeletedFromTheLibrary_isSkipped_whileTheRestRepeat() = runTest {
        seedWorkout("past", "back-squat", "bench-press")
        // Deleted from the library since the workout was performed.
        database.exerciseDao().softDelete("bench-press", deletedAt = 2_000L)

        val started = opened("past")
        assertEquals(
            "the surviving exercise repeats, the deleted one is skipped",
            listOf("back-squat"),
            repository.observeSessionExercises(started).first().map { it.exerciseId },
        )
    }

    @Test
    fun anExercisePerformedTwice_repeatsTwice() = runTest {
        seedWorkout("past", "back-squat", "bench-press", "back-squat")

        val started = opened("past")
        assertEquals(
            "what was performed twice was performed twice",
            listOf("back-squat", "bench-press", "back-squat"),
            repository.observeSessionExercises(started).first().map { it.exerciseId },
        )
    }

    /** Opens a session by repeating [sessionId], and returns the new session's id. */
    private suspend fun opened(sessionId: String): String {
        val result = repository.repeatSession(sessionId)
        assertTrue("repeating opens a session: $result", result is DataResult.Success)
        return (result as DataResult.Success<StartedSession>).data.id
    }

    /**
     * A workout holding [exerciseIds] in order, at [id], finished unless [finishedAt] says otherwise.
     */
    private suspend fun seedWorkout(
        id: String,
        vararg exerciseIds: String,
        finishedAt: Long? = 2_000L,
    ) {
        val dao = database.workoutDao()
        dao.insertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = 1_000L,
                finishedAt = finishedAt,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                createdAt = 1_000L,
                updatedAt = 2_000L,
                deletedAt = null,
            ),
        )
        exerciseIds.forEachIndexed { index, exerciseId ->
            dao.insertSessionExercise(
                SessionExerciseEntity(
                    id = "$id-$index",
                    sessionId = id,
                    exerciseId = exerciseId,
                    position = index,
                    restSeconds = null,
                    techniqueNote = null,
                    finishedAt = null,
                    supersetGroup = null,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    deletedAt = null,
                ),
            )
        }
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        isCustom = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    @Test
    fun openingASession_recordsTheZoneItIsIn() = runTest {
        // ROADMAP N25: the capture, on a device. A session opened while the source says Tokyo (+540)
        // is stored as Tokyo, which is what every screen then reads back.
        val tokyo = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 540 })

        val started = (tokyo.startOrResumeSession() as DataResult.Success<StartedSession>).data

        assertEquals(540, database.workoutDao().findSession(started.id)?.zoneOffsetMinutes)
    }

    @Test
    fun resumingASession_keepsTheZoneItOpenedIn() = runTest {
        // The "captured once" rule, and the one a DST-spanning session depends on: opening in Tokyo
        // and resuming hours later from a device that has since moved must NOT rewrite the row. The
        // DAO returns early for an existing session, which is where this is enforced.
        val tokyo = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 540 })
        val started = (tokyo.startOrResumeSession() as DataResult.Success<StartedSession>).data

        val home = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 })
        home.startOrResumeSession()

        assertEquals(
            "the session still remembers Tokyo",
            540,
            database.workoutDao().findSession(started.id)?.zoneOffsetMinutes,
        )
    }

    @Test
    fun itCarriesTheRest_theNote_andTheGrouping() = runTest {
        // ROADMAP B41: only the movement used to be copied, so a repeated superset lost its grouping —
        // and with a null group the round logic short-circuits, degrading the pair into unrelated
        // exercises resting separately, which is what N24 exists to prevent.
        seedWorkout("past", "back-squat", "bench-press")
        // The past workout prescribed a 3-minute rest, a note, and performed the two as a superset.
        database.openHelper.writableDatabase.execSQL(
            """
            UPDATE session_exercises
            SET restSeconds = 180, techniqueNote = 'pause at the bottom', supersetGroup = 1
            WHERE sessionId = 'past'
            """.trimIndent(),
        )

        val started = opened("past")
        val copied = repository.observeSessionExercises(started).first()

        assertEquals(listOf(180, 180), copied.map { it.restSeconds })
        assertEquals(
            listOf("pause at the bottom", "pause at the bottom"),
            copied.map { it.techniqueNote },
        )
        assertEquals("still a superset", listOf(1, 1), copied.map { it.supersetGroup })
    }
    @Test
    fun aRepeatKeepsTheWorkoutsName() = runTest {
        // ROADMAP N58's edge, settled: a repeat starts a session with the *source's* template id, so a
        // repeated Push A is still a Push A in history rather than an unnamed session. The targets are
        // not copied with it — this is provenance (P3.3), not prescription.
        database.templateDao().insertTemplate(
            TemplateEntity(id = "t1", name = "Push A", createdAt = 0L, updatedAt = 0L, deletedAt = null),
        )
        val session = database.workoutDao()
            .findOrCreateActiveSession(id = "past", now = 1_000L, templateId = "t1").session
        database.workoutDao().markFinished(id = session.id, at = 2_000L)

        val repeated = opened("past")

        assertEquals("t1", database.workoutDao().findSession(repeated)?.templateId)
    }
}
