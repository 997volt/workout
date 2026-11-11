package com.example.androidapp.ui.home

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneSource
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.ui.testing.FakeDeloadRepository
import com.example.androidapp.ui.testing.FakeMeasurementRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home's destructive handlers (ROADMAP N89, N195, P3.11).
 *
 * The plan rule and the screens have their own tests; what had none is what these three writes are keyed
 * to. Two of them — a skipped block session and a substitution — are dated by the **screen's** captured
 * day rather than a live clock reading (N286), and getting that wrong writes a skip or a stand-in for the
 * wrong occurrence, which is not a visible error until the next session is due.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutsHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val workouts = FakeWorkoutRepository()
    private val programs = FakeProgramRepository()

    /** A Wednesday, so its week starts on the Monday this test asserts against. */
    private var now: Instant = Instant.parse("2026-10-07T12:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(deloads: FakeDeloadRepository = FakeDeloadRepository()) = WorkoutsHomeViewModel(
        workoutRepository = workouts,
        templateRepository = FakeTemplateRepository(),
        programRepository = programs,
        deloadRepository = deloads,
        measurementRepository = FakeMeasurementRepository(),
        timeSource = TimeSource { now },
        zoneSource = ZoneSource { ZoneId.of("UTC") },
    )

    @Test
    fun discardingTheOpenWorkout_deletesThatSession_andReturnsTheWritesOutcome() = runTest(dispatcher) {
        workouts.activeSession.value = WorkoutSession(
            id = "open-1",
            startedAt = Instant.parse("2026-10-07T07:00:00Z"),
        )
        val viewModel = viewModel()

        // A refusal is a value, not a silent no-op: the notice Home shows reads exactly this (F7).
        val refused = DataResult.Failure(DataError.Storage(IOException("locked")))
        workouts.deleteResult = refused
        assertThat(viewModel.discardActiveWorkout()).isEqualTo(refused)

        workouts.deleteResult = DataResult.Success(Unit)
        assertThat(viewModel.discardActiveWorkout()).isEqualTo(DataResult.Success(Unit))
        assertThat(workouts.deletedSessions).containsExactly("open-1", "open-1").inOrder()
    }

    @Test
    fun skippingABlockSession_isDatedTheScreensOwnDay_untilAResumeReReadsIt() = runTest(dispatcher) {
        val viewModel = viewModel()

        // The clock moves across the weekend, but this screen was opened on the Wednesday: the occurrence
        // the lifter is looking at is that day's (N286).
        now = Instant.parse("2026-10-12T06:00:00Z")
        assertThat(viewModel.skipBlockSession("slot-1")).isEqualTo(DataResult.Success(Unit))
        assertThat(programs.blockSkips).containsExactly("slot-1" to LocalDate.of(2026, 10, 7))

        viewModel.onResume()
        assertThat(viewModel.skipBlockSession("slot-2")).isEqualTo(DataResult.Success(Unit))
        assertThat(programs.blockSkips.last()).isEqualTo("slot-2" to LocalDate.of(2026, 10, 12))
    }

    @Test
    fun aSubstitution_isKeyedToTheCapturedWeek_andNullRestoresTheSlotsOwnWorkout() = runTest(dispatcher) {
        val viewModel = viewModel()

        // Wednesday of the week of 5 October. The clock is moved into the next week without a resume, so a
        // live read here would record the stand-in against a week nobody is looking at (P3.11).
        now = Instant.parse("2026-10-14T06:00:00Z")
        assertThat(viewModel.setSubstitution("slot-1", "template-alt")).isEqualTo(DataResult.Success(Unit))
        assertThat(programs.substitutions)
            .containsExactly(Triple("slot-1", LocalDate.of(2026, 10, 5), "template-alt"))

        assertThat(viewModel.setSubstitution("slot-1", null)).isEqualTo(DataResult.Success(Unit))
        assertThat(programs.substitutions.last())
            .isEqualTo(Triple("slot-1", LocalDate.of(2026, 10, 5), null))
    }

    @Test
    fun theExemptBanner_isRaisedOnlyWhenEveryWeeklyProgramIsExempt() = runTest(dispatcher) {
        // B406: the banner speaks for the whole screen, so one exempt program beside a scored one must not
        // say "this week is not scored" — and a block never raises it, because a block has no scored week.
        val week = LocalDate.of(2026, 10, 5)
        programs.activePrograms.value = listOf(
            weekly(id = "a"),
            weekly(id = "b"),
            WorkoutProgram(id = "block", name = "Base", isActive = true, kind = ProgramKind.SEQUENCE),
        )
        val deloads = FakeDeloadRepository()
        deloads.marksByWeek.value = mapOf(week to setOf("a", "block"))
        val viewModel = viewModel(deloads)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isExemptWeek).isFalse()

        deloads.marksByWeek.value = mapOf(week to setOf("a", "b"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isExemptWeek).isTrue()
    }

    @Test
    fun aBlocksMark_neverRaisesTheExemptBanner() = runTest(dispatcher) {
        val week = LocalDate.of(2026, 10, 5)
        programs.activePrograms.value = listOf(
            WorkoutProgram(id = "block", name = "Base", isActive = true, kind = ProgramKind.SEQUENCE),
        )
        val deloads = FakeDeloadRepository()
        deloads.marksByWeek.value = mapOf(week to setOf("block"))
        val viewModel = viewModel(deloads)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isExemptWeek).isFalse()
    }

    private fun weekly(id: String) = WorkoutProgram(id = id, name = id.uppercase(), isActive = true)
}
