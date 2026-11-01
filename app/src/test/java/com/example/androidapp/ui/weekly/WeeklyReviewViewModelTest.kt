package com.example.androidapp.ui.weekly

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.OccurrenceState
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgressionMiss
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.WeeklyReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week's assembly (ROADMAP N252): the loop over the seven days, the two totals, the plan and the stall.
 *
 * The domain math and the screen are tested; this is the glue between them — the part a wrong date would break
 * without any single piece looking wrong.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.systemDefault()
    private val now = Instant.parse("2026-10-28T12:00:00Z")
    private val today = now.atZone(zone).toLocalDate()
    private val monday = ProgramSchedule.weekStartOf(today)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theWeek_isAssembled_fromTheHistory_theDays_thePlan_theGains_andTheStall() = runTest(dispatcher) {
        val history = listOf(
            session("this", monday.atTime(10, 0), sets = 6, volume = 12_000),
            session("last", monday.minusWeeks(1).atTime(10, 0), sets = 4, volume = 8_000),
        )
        val fake = FakeWeekly(
            history = history,
            occurrences = mapOf(
                monday to listOf(occurrence(monday, OccurrenceState.DONE)),
                monday.plusDays(1) to listOf(occurrence(monday.plusDays(1), OccurrenceState.MISSED)),
            ),
            programs = listOf(program()),
            stall = PlanStall(
                exerciseName = "Back Squat",
                setNumber = 2,
                kind = ProgressionMiss.NOT_DONE,
                sessions = 3,
            ),
        )
        val viewModel = WeeklyReviewViewModel(weekly = fake, timeSource = TimeSource { now })

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertNull(state.error)
        assertEquals(monday, state.weekStart)
        assertEquals(monday.plusDays(6), state.weekEnd)
        assertEquals(1, state.thisWeek.sessions)
        assertEquals(6, state.thisWeek.sets)
        assertEquals(1, state.lastWeek.sessions)
        assertEquals(4, state.lastWeek.sets)
        assertEquals(2, state.plan.planned)
        assertEquals(1, state.plan.done)
        assertEquals(7, state.days.size)
        assertEquals("Back Squat", state.stall?.exerciseName)
        assertEquals("p1", state.stallProgramId)
        assertTrue("the gains are the week's", state.movements.isNotEmpty())
    }

    @Test
    fun aFailedDayRead_isAnError_andRetryLoadsTheWeek() = runTest(dispatcher) {
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = mapOf(monday to listOf(occurrence(monday, OccurrenceState.DONE))),
            failDate = monday,
        )
        val viewModel = WeeklyReviewViewModel(weekly = fake, timeSource = TimeSource { now })
        advanceUntilIdle()

        assertNotNull("a failed read must be said rather than drawn as an empty week", viewModel.state.value.error)
        assertTrue(viewModel.state.value.weekStart == null)

        fake.failDate = null
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(monday, state.weekStart)
        assertEquals(7, state.days.size)
    }

    private fun session(id: String, at: java.time.LocalDateTime, sets: Int, volume: Long) = WorkoutSummary(
        id = id,
        startedAt = at.atZone(zone).toInstant(),
        finishedAt = at.plusHours(1).atZone(zone).toInstant(),
        exerciseCount = 1,
        setCount = sets,
        volumeGrams = volume,
    )

    private fun occurrence(date: LocalDate, state: OccurrenceState) = DayOccurrence(
        slotId = "slot",
        templateId = "template",
        templateName = "Squat day",
        date = date,
        weekStart = monday,
        state = state,
        canCorrect = false,
    )

    private fun program() = WorkoutProgram(
        id = "p1",
        name = "Push Pull Legs",
        slotCount = 2,
        isActive = true,
        position = 0,
        kind = ProgramKind.WEEKLY,
    )

    private class FakeWeekly(
        private val history: List<WorkoutSummary>,
        private val occurrences: Map<LocalDate, List<DayOccurrence>>,
        private val programs: List<WorkoutProgram> = emptyList(),
        private val stall: PlanStall? = null,
        var failDate: LocalDate? = null,
    ) : WeeklyReviewRepository {
        override suspend fun movementGains(
            weekStart: LocalDate,
            zone: ZoneId,
        ): DataResult<List<MovementGain>> = DataResult.Success(
            listOf(
                MovementGain(
                    exerciseId = "back-squat",
                    name = "Back Squat",
                    topGrams = 120_000,
                    previousGrams = 115_000,
                    firstTime = false,
                ),
            ),
        )

        override suspend fun history(): DataResult<List<WorkoutSummary>> =
            DataResult.Success(history)

        override suspend fun occurrencesOn(
            date: LocalDate,
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<List<DayOccurrence>> =
            if (date == failDate) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the day could not be read")))
            } else {
                DataResult.Success(occurrences[date].orEmpty())
            }

        override suspend fun activePrograms(): DataResult<List<WorkoutProgram>> =
            DataResult.Success(programs)

        override suspend fun firstStall(programId: String): PlanStall? = stall
    }
}
