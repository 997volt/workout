package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneSource
import com.example.androidapp.domain.model.AdherenceReport
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.DeloadDay
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.MonthlyRatio
import com.example.androidapp.domain.model.Streak
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.repository.AdherenceRepository
import com.example.androidapp.ui.testing.FakeDeloadRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The recovery screen's wiring (ROADMAP N300, N301).
 *
 * The week arithmetic is pure and tested as a rule ([deloadSuggestion]); the screen renders the state it
 * is handed. What sat between them is this: the two marks and two plans read for the right program, the
 * exemption written through the port, and — since B404 — the cadence suggestion following a mark that
 * appears while the screen is open, including the one the creator writes for the week it was sent to build.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramWeeksViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val programs = FakeProgramRepository()
    private val deloads = FakeDeloadRepository()
    private val settings = FakeSettingsRepository()
    private val adherence = FakeAdherence()

    /** A Wednesday, so the week under the screen is the one that started on Monday. */
    private var now: Instant = Instant.parse("2026-11-11T09:00:00Z")
    private val thisWeek = LocalDate.of(2026, 11, 9)
    private val nextWeek = thisWeek.plusWeeks(1)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        programs.program.value = WorkoutProgram(
            id = PROGRAM_ID,
            name = "Upper/Lower",
            startDate = thisWeek.minusWeeks(20),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProgramWeeksViewModel(
        programs = programs,
        adherence = adherence,
        deloads = deloads,
        settings = settings,
        savedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
        timeSource = TimeSource { now },
        zoneSource = ZoneSource { ZoneOffset.UTC },
    )

    /** The ViewModel with its state subscribed, as the screen would, and the first reads settled. */
    private fun TestScope.state(): ProgramWeeksViewModel = viewModel().also { viewModel ->
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun aDueCadence_isSuggested_withTheCountFromTheStart() = runTest(dispatcher) {
        // N301: twenty whole weeks without a recovery week, at a six-week cadence.
        settings.deloadSuggestion.value = 6
        val viewModel = state()

        val suggestion = viewModel.uiState.value.suggestion
        assertThat(suggestion?.due).isTrue()
        assertThat(suggestion?.weeksSince).isEqualTo(20)
        assertThat(suggestion?.sinceLastDeload).isFalse()
    }

    @Test
    fun aMarkThatAppears_clearsTheSuggestion_whileTheScreenIsOpen() = runTest(dispatcher) {
        // B404: building a week marks it (the creator reuses a mark it finds), and the suggestion used to
        // stay on screen saying a recovery week may be due because it was read once when the screen opened.
        settings.deloadSuggestion.value = 6
        val viewModel = state()
        assertThat(viewModel.uiState.value.suggestion?.due).isTrue()

        // What the creator's write does to the store: a mark for this week.
        deloads.lastMark = thisWeek
        deloads.marksByWeek.value = mapOf(thisWeek to setOf(PROGRAM_ID))
        advanceUntilIdle()

        val suggestion = viewModel.uiState.value.suggestion
        assertThat(suggestion?.due).isFalse()
        assertThat(suggestion?.sinceLastDeload).isTrue()
        assertThat(suggestion?.weeksSince).isEqualTo(0)
    }

    @Test
    fun exemptingAWeek_writesTheMarkThroughThePort() = runTest(dispatcher) {
        val viewModel = state()

        viewModel.onSetExempt(nextWeek, exempt = true)
        advanceUntilIdle()

        assertThat(adherence.marks).containsExactly(Triple(PROGRAM_ID, nextWeek, true))
        assertThat(viewModel.uiState.value.writing).isFalse()
    }

    @Test
    fun aFailedMark_isStated() = runTest(dispatcher) {
        adherence.result = DataResult.Failure(DataError.Storage(IllegalStateException("disk full")))
        val viewModel = state()

        viewModel.onSetExempt(thisWeek, exempt = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test
    fun theTwoWeeks_readTheirOwnMarkAndTheirOwnPlan() = runTest(dispatcher) {
        // The screen shows two weeks side by side, and a plan for another program is not this one's.
        deloads.marksByWeek.value = mapOf(thisWeek to setOf(PROGRAM_ID))
        deloads.weeks.value = listOf(
            DeloadWeek(
                programId = PROGRAM_ID,
                weekStart = thisWeek,
                days = listOf(day(id = "d1", programId = PROGRAM_ID, templateId = "t1")),
            ),
            DeloadWeek(
                programId = "other",
                weekStart = nextWeek,
                days = listOf(day(id = "d2", programId = "other", templateId = "t2")),
            ),
        )
        val viewModel = state()

        val drawn = viewModel.uiState.value
        assertThat(drawn.thisWeekExempt).isTrue()
        assertThat(drawn.thisWeekHasPlan).isTrue()
        assertThat(drawn.nextWeekExempt).isFalse()
        assertThat(drawn.nextWeekHasPlan).isFalse()
    }

    @Test
    fun aScreenKeptAcrossTheWeekBoundary_followsTheNewWeek() = runTest(dispatcher) {
        // B415: a screen left open over Sunday night used to label last week "This week" — and exempt it.
        val viewModel = state()
        assertThat(viewModel.uiState.value.thisWeek).isEqualTo(thisWeek)

        now = Instant.parse("2026-11-18T09:00:00Z")
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.thisWeek).isEqualTo(thisWeek.plusWeeks(1))
        assertThat(viewModel.uiState.value.nextWeek).isEqualTo(thisWeek.plusWeeks(2))
    }

    @Test
    fun aDeletedProgram_saysSo_ratherThanShowingAWeek() = runTest(dispatcher) {
        programs.program.value = null
        val viewModel = state()

        assertThat(viewModel.uiState.value.notFound).isTrue()
        assertThat(viewModel.uiState.value.suggestion).isNull()
    }

    /** One day of a built plan, with only the fields the week's read uses. */
    private fun day(id: String, programId: String, templateId: String) = DeloadDay(
        id = id,
        programId = programId,
        templateId = templateId,
        templateName = "Deload $id",
    )

    /** The adherence port, holding what the screen writes and the one refusal it can be handed. */
    private class FakeAdherence : AdherenceRepository {
        var result: DataResult<Unit> = DataResult.Success(Unit)
        val marks = mutableListOf<Triple<String, LocalDate, Boolean>>()

        override fun observeBlockProgress(): Flow<List<BlockProgress>> = flowOf(emptyList())

        override suspend fun monthAdherence(
            month: YearMonth,
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<AdherenceReport> = notUsed()

        override suspend fun setDeloadWeek(
            programId: String,
            weekStart: LocalDate,
            marked: Boolean,
        ): DataResult<Unit> {
            marks += Triple(programId, weekStart, marked)
            return result
        }

        override suspend fun occurrencesOn(
            date: LocalDate,
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<List<DayOccurrence>> = notUsed()

        override suspend fun streak(today: LocalDate, zone: ZoneId): DataResult<Streak?> = notUsed()

        override suspend fun ratioHistory(
            months: Int,
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<List<MonthlyRatio>> = notUsed()

        override suspend fun setOccurrenceSkipped(
            slotId: String,
            weekStart: LocalDate,
            skipped: Boolean,
        ): DataResult<Unit> = notUsed()
    }

    private companion object {
        const val PROGRAM_ID = "p1"

        fun notUsed(): Nothing = error("the recovery screen does not read this")
    }
}
