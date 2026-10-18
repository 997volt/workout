package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.DeloadDay
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.repository.DeloadCreation
import com.example.androidapp.domain.repository.DeloadDayInput
import com.example.androidapp.domain.repository.DeloadRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * The creator's state machine (ROADMAP N147, B164).
 *
 * The arithmetic and the screen are covered elsewhere; this is the wiring between them — a failed read is
 * a failure rather than an empty program, the controls re-derive the draft, and a create writes exactly what
 * the screen showed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProgramDeloadViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now: Instant = Instant.parse("2026-10-07T12:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aFailedRead_isAFailure_notAnEmptyProgram() = runTest(dispatcher) {
        // B164: the screen has an error channel; a storage read must land in it, not in "no workouts".
        val reader = FakeReader().apply { fail = IOException("disk") }
        val viewModel = viewModel(reader = reader)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.loadFailed).isTrue()
        assertThat(viewModel.uiState.value.error).isInstanceOf(DataError.Storage::class.java)
        assertThat(viewModel.uiState.value.hasNothingToBuild).isFalse()
        assertThat(viewModel.uiState.value.canCreate).isFalse()
    }

    @Test
    fun aGoneProgram_isNotFound() = runTest(dispatcher) {
        val viewModel = viewModel(reader = FakeReader(result = null))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.error).isEqualTo(DataError.NotFound)
        assertThat(viewModel.uiState.value.loadFailed).isFalse()
    }

    @Test
    fun aSnapshot_becomesTheDraft_andTheControlsReDeriveIt() = runTest(dispatcher) {
        val viewModel = viewModel(reader = FakeReader(result = snapshot()))
        advanceUntilIdle()

        val ready = viewModel.uiState.value
        assertThat(ready.programName).isEqualTo("Upper/Lower")
        assertThat(ready.dayCount).isEqualTo(1)
        assertThat(ready.maxDays).isEqualTo(1)
        assertThat(ready.selectedExerciseCount).isEqualTo(1)
        assertThat(ready.canCreate).isTrue()
        assertThat(ready.days.single().exercises.single().sets.first().weightGrams).isEqualTo(60_000L)

        viewModel.onLoadPercentChange(50)
        assertThat(viewModel.uiState.value.days.single().exercises.single().sets.first().weightGrams)
            .isEqualTo(50_000L)

        viewModel.onWeekdayChange("s1", null)
        assertThat(viewModel.uiState.value.days.single().weekday).isNull()

        viewModel.onToggleExercise(0, "back-squat")
        assertThat(viewModel.uiState.value.selectedExerciseCount).isEqualTo(0)
        assertThat(viewModel.uiState.value.canCreate).isFalse()
    }

    @Test
    fun aLastPlan_isOffered_andItsExercisesAndDayCountPrefillTheDraft() = runTest(dispatcher) {
        // N150: the last plan's exercise ids are matched to the current program, and its number of days is
        // the draft's; the numbers are re-derived by the same rule, not copied from the old templates.
        val last = DeloadWeek(
            programId = "p1",
            weekStart = now.atZone(java.time.ZoneId.of("UTC")).toLocalDate().minusWeeks(2),
            days = listOf(
                DeloadDay(
                    id = "old-day",
                    programId = "p1",
                    templateId = "old-template",
                    templateName = "Heavy lower — deload",
                    weekday = DayOfWeek.MONDAY,
                    exerciseIds = listOf("back-squat"),
                ),
            ),
        )
        val viewModel = viewModel(reader = FakeReader(result = snapshot().copy(lastPlan = last)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.lastDeloadWeek).isEqualTo(last.weekStart)

        viewModel.onUseLastDeload()

        assertThat(viewModel.uiState.value.selectedExerciseCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.dayCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.days.single().exercises.single().sets.first().weightGrams)
            .isEqualTo(60_000L)
    }

    @Test
    fun create_writesTheDraft_andThenSaysDone() = runTest(dispatcher) {
        val repository = FakeDeloadRepository()
        val viewModel = viewModel(reader = FakeReader(result = snapshot()), repository = repository)
        advanceUntilIdle()

        viewModel.onCreate()
        advanceUntilIdle()

        assertThat(repository.writes).hasSize(1)
        assertThat(repository.writes.single().single().name).isEqualTo("Deload A")
        assertThat(viewModel.uiState.value.created).isNotNull()
    }

    @Test
    fun aFailedCreate_isReported() = runTest(dispatcher) {
        val repository = FakeDeloadRepository().apply {
            failure = DataError.Invalid("Keep at least one exercise.")
        }
        val viewModel = viewModel(reader = FakeReader(result = snapshot()), repository = repository)
        advanceUntilIdle()

        viewModel.onCreate()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error)
            .isEqualTo(DataError.Invalid("Keep at least one exercise."))
        assertThat(viewModel.uiState.value.created).isNull()
    }

    private fun snapshot() = DeloadSourceSnapshot(
        programName = "Upper/Lower",
        days = listOf(
            DeloadSourceDay(
                sourceSlotId = "s1",
                name = "Heavy lower",
                weekday = DayOfWeek.MONDAY,
                exercises = listOf(
                    DeloadSourceExercise(
                        templateExerciseId = "te1",
                        exerciseId = "back-squat",
                        name = "Back squat",
                        restSeconds = 180,
                        techniqueNote = null,
                        supersetGroup = null,
                        sets = List(2) {
                            TemplateSet(
                                id = "ts$it",
                                templateExerciseId = "te1",
                                setIndex = it,
                                role = SetType.NORMAL,
                                targetWeightGrams = 100_000L,
                                targetAssistanceGrams = null,
                                targetRepsMin = 5,
                                targetRepsMax = 5,
                                targetRepsCurrent = null,
                                note = null,
                            )
                        },
                        weightUnit = WeightUnit.KILOGRAMS,
                        stepGrams = 2_500L,
                        previous = PreviousPerformance(emptyList()),
                    ),
                ),
            ),
        ),
        hasExistingPlan = false,
    )

    private fun viewModel(
        reader: DeloadSourceReader,
        repository: DeloadRepository = FakeDeloadRepository(),
    ) = ProgramDeloadViewModel(
        deloadRepository = repository,
        loader = reader,
        timeSource = TimeSource { now },
        savedStateHandle = SavedStateHandle(mapOf("programId" to "p1")),
    )

    private class FakeReader(
        var result: DeloadSourceSnapshot? = null,
        var fail: Throwable? = null,
    ) : DeloadSourceReader {
        override suspend fun load(programId: String, weekStart: LocalDate): DeloadSourceSnapshot? {
            fail?.let { throw it }
            return result
        }
    }

    private class FakeDeloadRepository : DeloadRepository {
        var failure: DataError? = null
        val writes = mutableListOf<List<DeloadDayInput>>()

        override fun observeWeekPlans(weekStart: LocalDate): Flow<List<DeloadWeek>> = flowOf(emptyList())

        override suspend fun findLastPlan(programId: String, beforeWeek: LocalDate): DeloadWeek? = null

        override suspend fun createDeload(
            programId: String,
            weekStart: LocalDate,
            days: List<DeloadDayInput>,
        ): DataResult<DeloadCreation> {
            writes += days
            return failure?.let { DataResult.Failure(it) }
                ?: DataResult.Success(DeloadCreation(weekStart, days.map { "t${it.name}" }))
        }
    }
}
