package com.example.androidapp.ui.weekly

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BlockProgress
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
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
    private var now = Instant.parse("2026-10-28T12:00:00Z")
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
    fun theFirstResume_doesNotStartASecondLoad() = runTest(dispatcher) {
        // B392: init's load and the route's first ON_RESUME used to race; the later one won unpredictably.
        val fake = FakeWeekly(history = emptyList(), occurrences = emptyMap())
        val viewModel = WeeklyReviewViewModel(weekly = fake, timeSource = TimeSource { now })
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(fake.historyReads).isEqualTo(1)
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun onResume_reloadsTheWeekTheClockNowNames() = runTest(dispatcher) {
        // N286: a week boundary must not wait for the screen to be recreated.
        val viewModel = WeeklyReviewViewModel(
            weekly = FakeWeekly(history = emptyList(), occurrences = emptyMap()),
            timeSource = TimeSource { now },
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.weekStart).isEqualTo(monday)

        now = now.plusSeconds(7 * 86_400)
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(viewModel.state.value.weekStart).isEqualTo(monday.plusWeeks(1))
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
            blocks = listOf(BlockProgress(name = "Push Pull Legs", position = 3, length = 9, complete = false)),
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
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.weekStart).isEqualTo(monday)
        assertThat(state.weekEnd).isEqualTo(monday.plusDays(6))
        assertThat(state.thisWeek.sessions).isEqualTo(1)
        assertThat(state.thisWeek.sets).isEqualTo(6)
        assertThat(state.lastWeek.sessions).isEqualTo(1)
        assertThat(state.lastWeek.sets).isEqualTo(4)
        assertThat(state.plan.planned).isEqualTo(2)
        assertThat(state.plan.done).isEqualTo(1)
        assertThat(state.days).hasSize(7)
        assertThat(state.stall?.exerciseName).isEqualTo("Back Squat")
        assertThat(state.stallProgramId).isEqualTo("p1")
        assertWithMessage("the block is part of the week")
            .that(state.blocks.map { it.name })
            .containsExactly("Push Pull Legs")
        assertWithMessage("an active program is followed even without weekdays")
            .that(state.hasActivePlan)
            .isTrue()
        // The row itself, not "something was returned": the gain is the one read this test cannot reach
        // through a database, so its shape is pinned here (N260).
        assertThat(state.movements).containsExactly(
            MovementGain(
                exerciseId = "back-squat",
                name = "Back Squat",
                topGrams = 120_000,
                previousGrams = 115_000,
                firstTime = false,
            ),
        )
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

        assertWithMessage("a failed read must be said rather than drawn as an empty week")
            .that(viewModel.state.value.error)
            .isNotNull()
        assertThat(viewModel.state.value.weekStart).isNull()

        fake.failDate = null
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.weekStart).isEqualTo(monday)
        assertThat(state.days).hasSize(7)
    }

    @Test
    fun aFailedProgramRead_isAnError_ratherThanNoProgramFollowed() = runTest(dispatcher) {
        // N255: `hasActivePlan` feeds the sentence "No program is being followed", so a failed read cannot
        // answer it with false.
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = mapOf(monday to listOf(occurrence(monday, OccurrenceState.DONE))),
            failPrograms = true,
        )
        val viewModel = WeeklyReviewViewModel(weekly = fake, timeSource = TimeSource { now })
        advanceUntilIdle()

        val state = viewModel.state.value
        assertWithMessage("a failed read must not be drawn as a statement about the plan")
            .that(state.error)
            .isNotNull()
        assertThat(state.weekStart).isNull()
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
        private val blocks: List<BlockProgress> = emptyList(),
        private val stall: PlanStall? = null,
        var failDate: LocalDate? = null,
        var failPrograms: Boolean = false,
        var historyReads: Int = 0,
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

        override suspend fun history(): DataResult<List<WorkoutSummary>> {
            historyReads++
            return DataResult.Success(history)
        }

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
            if (failPrograms) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the programs could not be read")))
            } else {
                DataResult.Success(programs)
            }

        override suspend fun firstStall(programId: String): DataResult<PlanStall?> =
            DataResult.Success(stall)

        override suspend fun blocks(): DataResult<List<BlockProgress>> = DataResult.Success(blocks)
    }
}
