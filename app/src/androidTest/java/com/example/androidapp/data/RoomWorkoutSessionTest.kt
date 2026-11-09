package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionSoreMuscleEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneOffsetSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.repository.StartedSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Session-level writes, end to end through the repository (ROADMAP N4).
 *
 * The readiness prompt is driven by [StartedSession.isNew], so that flag is
 * asserted exactly rather than assumed: getting it wrong would either nag on every
 * resume or never ask at all.
 */
@RunWith(AndroidJUnit4::class)
class RoomWorkoutSessionTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomWorkoutRepository

    private val clock = TimeSource { Instant.parse("2026-09-29T08:00:00Z") }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aFreshlyOpenedSession_isNew_andKeepsItsReadiness() = runTest {
        val started = start()
        assertTrue(started.isNew)

        assertTrue(
            repository.setReadiness(started.id, "Shoulders still sore") is DataResult.Success,
        )

        val stored = repository.observeSession(started.id).first()
        assertEquals("Shoulders still sore", stored?.readinessNote)
    }

    @Test
    fun resuming_doesNotReportANewSession() = runTest {
        val first = start()
        val second = start()

        assertEquals("the open session must be reused", first.id, second.id)
        // This is what stops the prompt re-appearing on every resume (N4).
        assertFalse(second.isNew)
    }

    @Test
    fun aBlankNote_isStoredAsNull_ratherThanAnEmptyString() = runTest {
        val started = start()
        repository.setReadiness(started.id, "Shoulders sore")
        assertEquals(
            "Shoulders sore",
            repository.observeSession(started.id).first()?.readinessNote,
        )

        repository.setReadiness(started.id, "   ")

        assertNull(
            "clearing the field must leave one representation of nothing",
            repository.observeSession(started.id).first()?.readinessNote,
        )
    }

    @Test
    fun setReadiness_onAGoneSession_isNotFound() = runTest {
        val failure = repository.setReadiness("no-such-session", "x") as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun deletingASession_hidesItsSoreMuscles_too() = runTest {
        // "everything in it" is the delete's contract (N62): a soft-deleted workout must not leave
        // its soreness live for the next read to find. A live session cannot record the list any more
        // (N113 moved it to the day), but the rows an older workout wrote are still on disk, so the row
        // is seeded through the DAO rather than through setReadiness — which is the only way this test
        // can assert anything at all.
        val started = start()
        database.sessionSoreMuscleDao().insertAll(
            listOf(
                SessionSoreMuscleEntity(
                    id = "sore-1",
                    sessionId = started.id,
                    muscle = MuscleGroup.CORE,
                    score = 4,
                    position = 0,
                    createdAt = 1L,
                    updatedAt = 1L,
                    deletedAt = null,
                ),
            ),
        )
        assertEquals(
            "the row has to be live before the delete, or this asserts nothing",
            1,
            database.sessionSoreMuscleDao().observeForSession(started.id).first().size,
        )

        repository.deleteSession(started.id)

        // The session read is null — that is what deleting the workout means — so the soreness is
        // unreachable through it, and the rows themselves are hidden rather than merely orphaned.
        assertNull(repository.observeSession(started.id).first())
        assertEquals(
            "the deleted session's soreness is hidden, not left live",
            0,
            database.sessionSoreMuscleDao().observeForSession(started.id).first().size,
        )
    }

    @Test
    fun substitutingAnExercise_keepsItsPosition_andRecordsThePlanRow() = runTest {
        // N291: a substituted row is still the plan's row, just performed with a different movement.
        seedTemplate("t1", listOf("back-squat", "bench-press"))
        val started = start("t1")
        val row = repository.observeSessionExercises(started.id).first().first()

        val result = repository.substituteExercise(row.id, "bench-press")

        assertTrue(result is DataResult.Success)
        val rows = repository.observeSessionExercises(started.id).first()
        assertEquals("bench-press", rows.first().exerciseId)
        assertEquals("back-squat", rows.first().substitutedFromExerciseId)
        assertEquals(0, rows.first().position)
        assertEquals("the other row is untouched", "bench-press", rows.last().exerciseId)
        assertNull(rows.last().substitutedFromExerciseId)
    }

    @Test
    fun anActivity_storesAndClearsItsOptionalNumbers() = runTest {
        // N293: duration and distance are the activity's own facts, not a workout's.
        val started = start()
        repository.setSessionActivity(started.id, com.example.androidapp.domain.model.Activity.SWIMMING)
        assertTrue(repository.setActivityMetrics(started.id, 1_800, 1_500) is DataResult.Success)

        val saved = repository.observeSession(started.id).first()!!
        assertEquals(1_800, saved.activityDurationSeconds)
        assertEquals(1_500L, saved.activityDistanceMeters)

        assertTrue(repository.setActivityMetrics(started.id, null, null) is DataResult.Success)
        val cleared = repository.observeSession(started.id).first()!!
        assertNull(cleared.activityDurationSeconds)
        assertNull(cleared.activityDistanceMeters)

        // Turning the activity back into a workout also clears the numbers, so returning to an
        // activity later cannot resurrect a stale duration/distance.
        repository.setActivityMetrics(started.id, 1_800, 1_500)
        repository.setSessionActivity(started.id, null)
        val workout = repository.observeSession(started.id).first()!!
        assertNull(workout.activityDurationSeconds)
        assertNull(workout.activityDistanceMeters)
    }

    @Test
    fun startingFromATemplate_opensTheWorkoutWithItsExercisesInOrder() = runTest {
        seedTemplate("t1", listOf("back-squat", "bench-press"))

        val started = start(templateId = "t1")

        assertEquals(
            listOf("Back Squat", "Bench Press"),
            repository.observeSessionExercises(started.id).first().map { it.exerciseName },
        )
    }

    @Test
    fun theSeededExercises_arePositionsZeroUpwards_likeThePickerWouldWrite() = runTest {
        seedTemplate("t1", listOf("back-squat", "bench-press"))

        val started = start(templateId = "t1")

        assertEquals(
            listOf(0, 1),
            repository.observeSessionExercises(started.id).first().map { it.position },
        )
    }

    @Test
    fun resumingWithATemplate_doesNotSeedItAgain() = runTest {
        seedTemplate("t1", listOf("back-squat"))
        val first = start(templateId = "t1")

        // A second start — the user tapped Start from template again, or the
        // screen was recreated — must not duplicate the exercises (N3).
        val second = start(templateId = "t1")

        assertEquals(first.id, second.id)
        assertEquals(
            1,
            repository.observeSessionExercises(first.id).first().size,
        )
    }

    @Test
    fun startingFromAnEmptyTemplate_opensAnEmptyWorkout() = runTest {
        seedTemplate("t1", emptyList())

        val started = start(templateId = "t1")

        assertTrue(repository.observeSessionExercises(started.id).first().isEmpty())
    }

    @Test
    fun startingEmpty_leavesTheTemplateAlone() = runTest {
        seedTemplate("t1", listOf("back-squat"))

        val started = start()

        assertTrue(repository.observeSessionExercises(started.id).first().isEmpty())
        // The template is a plan, not a log: starting a workout must not consume it.
        assertEquals(
            listOf("back-squat"),
            database.templateDao().findTemplateExercises("t1").map { it.exerciseId },
        )
    }

    /** A template is seeded once, so the copy must not be shared with the session. */
    @Test
    fun editingTheSession_doesNotTouchTheTemplate() = runTest {
        seedTemplate("t1", listOf("back-squat", "bench-press"))
        val started = start(templateId = "t1")
        val benchPress = repository.observeSessionExercises(started.id).first()[1]

        repository.removeExercise(benchPress.id)

        assertEquals(
            listOf("back-squat"),
            repository.observeSessionExercises(started.id).first().map { it.exerciseId },
        )
        assertEquals(
            listOf("back-squat", "bench-press"),
            database.templateDao().findTemplateExercises("t1").map { it.exerciseId },
        )
    }

    /** ROADMAP N54: the session's order is editable, and the template's is not the session's. */
    @Test
    fun movingAnExercise_reordersTheSession_andLeavesTheTemplateAlone() = runTest {
        seedTemplate("t1", listOf("back-squat", "bench-press", "deadlift"))
        val started = start(templateId = "t1")
        val benchPress = repository.observeSessionExercises(started.id).first()[1]

        repository.moveExercise(benchPress.id, delta = -1)

        assertEquals(
            listOf("bench-press", "back-squat", "deadlift"),
            repository.observeSessionExercises(started.id).first().map { it.exerciseId },
        )
        // The reason N54 is a change at all: a reason that belonged to one afternoon must not rewrite
        // every future run.
        assertEquals(
            listOf("back-squat", "bench-press", "deadlift"),
            database.templateDao().findTemplateExercises("t1").map { it.exerciseId },
        )
    }

    @Test
    fun movingPastEitherEnd_leavesTheOrderAndReportsSuccess() = runTest {
        seedTemplate("t1", listOf("back-squat", "bench-press"))
        val started = start(templateId = "t1")
        val first = repository.observeSessionExercises(started.id).first().first()

        val up = repository.moveExercise(first.id, delta = -1)

        assertTrue("a move with nowhere to go is not a failure", up is DataResult.Success)
        assertEquals(
            listOf("back-squat", "bench-press"),
            repository.observeSessionExercises(started.id).first().map { it.exerciseId },
        )
    }

    private suspend fun seedTemplate(
        templateId: String,
        exerciseIds: List<String>,
        restSeconds: Int? = null,
        techniqueNote: String? = null,
    ) {
        database.templateDao().insertTemplate(
            TemplateEntity(
                id = templateId,
                name = "Push day",
                createdAt = 1_000L,
                updatedAt = 1_000L,
                deletedAt = null,
            ),
        )
        exerciseIds.forEachIndexed { index, exerciseId ->
            database.exerciseDao().insertAll(
                listOf(
                    ExerciseEntity(
                        id = exerciseId,
                        name = exerciseId.split("-").joinToString(" ") { part ->
                            part.replaceFirstChar { it.uppercase() }
                        },
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
            database.templateDao().insertTemplateExercise(
                TemplateExerciseEntity(
                    id = "$templateId-$exerciseId",
                    templateId = templateId,
                    exerciseId = exerciseId,
                    position = index,
                    restSeconds = restSeconds,
                    techniqueNote = techniqueNote,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    deletedAt = null,
                ),
            )
        }
    }

    private suspend fun start(templateId: String? = null): StartedSession =
        (repository.startOrResumeSession(templateId) as DataResult.Success).data

    @Test
    fun startingFromAPlan_carriesItsRestAndCueOntoTheSession() = runTest {
        // ROADMAP N14: the plan's rest and cue exist to be used, and the fallback to
        // the library's is only observable if the plan's value wins.
        seedTemplate("t1", listOf("back-squat"), restSeconds = 180, techniqueNote = "Slow descent")

        val started = start("t1")
        val row = repository.observeSessionExercises(started.id).first().single()

        assertEquals("the plan's break, not the library's", 180, row.restSeconds)
        assertEquals("Slow descent", row.techniqueNote)
    }

    @Test
    fun aPlanThatPrescribesNoRest_leavesTheLibrarysShowing() = runTest {
        seedTemplate("t1", listOf("back-squat"))

        val started = start("t1")
        val row = repository.observeSessionExercises(started.id).first().single()

        // The exercise seeded by this test has no library rest of its own, so the
        // fallback shows as null rather than as an override with nothing (N5).
        assertNull(row.restSeconds)
        assertNull(row.techniqueNote)
    }

    @Test
    fun aFinishedSession_movesToAnotherDay_andBothInstantsTravelTogether() = runTest {
        // N266: a workout logged on the wrong day is corrected by the day. The start and the finish move by the
        // same offset, so the session keeps the length it had rather than stretching or inverting.
        val started = start()
        repository.finishSession(started.id)
        val before = repository.observeSession(started.id).first()!!

        assertTrue(repository.moveSessionToDay(started.id, LocalDate.of(2026, 9, 26)) is DataResult.Success)

        val after = repository.observeSession(started.id).first()!!
        assertEquals(
            "both instants moved by the same offset",
            Duration.between(before.startedAt, after.startedAt),
            Duration.between(before.finishedAt, after.finishedAt),
        )
        assertEquals(Instant.parse("2026-09-26T08:00:00Z"), after.startedAt)
    }

    @Test
    fun aDayThatHasNotHappened_isRefused_andWritesNothing() = runTest {
        // The clock is 29 September; a session moved to the 30th would end in the future, which is not a record
        // (B212's rule), and the sentence says that rather than claiming the day does not exist (B337).
        val started = start()
        repository.finishSession(started.id)

        val result = repository.moveSessionToDay(started.id, LocalDate.of(2026, 9, 30))

        val error = (result as DataResult.Failure).error
        assertEquals(
            "the refusal names the future, not the day",
            "That would end the workout in the future.",
            (error as DataError.Invalid).message,
        )
        assertEquals(
            Instant.parse("2026-09-29T08:00:00Z"),
            repository.observeSession(started.id).first()!!.startedAt,
        )
    }

    @Test
    fun aSessionStillRunning_isRefusedWithItsOwnSentence() = runTest {
        // B337: a session with no finish has no day to correct, and says so — the message is not the future one.
        val started = start()

        val result = repository.moveSessionToDay(started.id, LocalDate.of(2026, 9, 26))

        val error = (result as DataResult.Failure).error
        assertEquals(
            "the refusal names what is missing: the finish",
            "Finish the workout before changing its day.",
            (error as DataError.Invalid).message,
        )
        assertEquals(
            "and nothing was written",
            Instant.parse("2026-09-29T08:00:00Z"),
            repository.observeSession(started.id).first()!!.startedAt,
        )
    }
}
