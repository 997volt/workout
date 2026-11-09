package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneSource
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ExerciseWorkingSets
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RecordEvent
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.TrainedWindow
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.StatisticsRepository
import com.example.androidapp.ui.testing.FakeExerciseRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
import java.time.ZoneOffset

/**
 * The balance read's wiring (ROADMAP N119, N289).
 *
 * `muscleBalance`, `trainedMuscleBalance` and the windows are pure and tested as rules; the balance screen
 * renders the state it is handed. What sat untested between them is the read: every slot's template, all of
 * them flattened, a schedule edit replacing the read rather than adding to it, and — since N289 — the log's
 * counts arriving for the selected window and the toggle changing which one is asked for.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramBalanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val programs = FakeProgramRepository()
    private val templates = FakeTemplateRepository()
    private val exercises = FakeExerciseRepository()
    private val statistics = FakeStatisticsRepository()
    private val zone = ZoneOffset.UTC

    /** The clock is a var so a resume can move the day the window ends on without rebuilding the screen (N286). */
    private var now: Instant = Instant.parse("2026-10-02T09:00:00Z")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        programs.program.value = WorkoutProgram(id = PROGRAM_ID, name = "Upper/Lower")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProgramBalanceViewModel(
        programRepository = programs,
        templateRepository = templates,
        exerciseRepository = exercises,
        statisticsRepository = statistics,
        timeSource = TimeSource { now },
        zoneSource = ZoneSource { zone },
        savedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
    )

    private fun startOf(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    @Test
    fun everySlotsTemplate_isCounted_andTheHeaderCountsWorkingSets() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0), slot("s2", "t2", position = 1))
        templates.exercises.value = mapOf(
            "t1" to listOf(planned("e1", workingSets = 2)),
            "t2" to listOf(planned("e2", workingSets = 3)),
        )
        exercises.library.value = DataResult.Success(
            listOf(
                Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS),
                Exercise(id = "e2", name = "Bench", primaryMuscle = MuscleGroup.CHEST),
            ),
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.programName).isEqualTo("Upper/Lower")
        // The plan's own size, not the sum of the credits: a primary set is two halves (N112, N122).
        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(5)
        assertThat(viewModel.uiState.value.balance)
            .containsExactly(
                MuscleSets(MuscleGroup.CHEST, 6),
                MuscleSets(MuscleGroup.QUADS, 4),
            )
            .inOrder()
    }

    @Test
    fun addingASlot_replacesTheRead_ratherThanKeepingTheFirstTemplatesWork() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("e1", workingSets = 1)))
        exercises.library.value = DataResult.Success(
            listOf(Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS)),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(1)

        programs.slots.value = listOf(slot("s1", "t1", position = 0), slot("s2", "t2", position = 1))
        templates.exercises.value = templates.exercises.value +
            ("t2" to listOf(planned("e2", workingSets = 2)))
        exercises.library.value = DataResult.Success(
            listOf(
                Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS),
                Exercise(id = "e2", name = "Bench", primaryMuscle = MuscleGroup.CHEST),
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(3)
        assertThat(viewModel.uiState.value.balance.map { it.muscle })
            .containsExactly(MuscleGroup.CHEST, MuscleGroup.QUADS)
            .inOrder()
    }

    @Test
    fun theLogsCounts_areCreditedByThePlansOwnRule_andTheWindowIsTheLastSevenDays() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("squat", workingSets = 2)))
        exercises.library.value = DataResult.Success(
            listOf(
                Exercise(id = "squat", name = "Squat", primaryMuscle = MuscleGroup.QUADS),
                Exercise(
                    id = "row",
                    name = "Barbell Row",
                    primaryMuscle = MuscleGroup.LATS,
                    secondaryMuscles = listOf(MuscleGroup.BICEPS),
                ),
            ),
        )
        // What the DAO returned: three logged squat sets and two rows, already counted per movement.
        statistics.workingSets.value = DataResult.Success(
            listOf(
                ExerciseWorkingSets(exerciseId = "squat", workingSets = 3),
                ExerciseWorkingSets(exerciseId = "row", workingSets = 2),
            ),
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // The trained side is a count of work, so the halves are the secondary's share (N122): lats 4 and
        // biceps 2 from two row sets, quads 6 from three squat sets.
        assertThat(viewModel.uiState.value.trained).containsExactly(
            MuscleSets(MuscleGroup.QUADS, 6),
            MuscleSets(MuscleGroup.LATS, 4),
            MuscleSets(MuscleGroup.BICEPS, 2),
        ).inOrder()
        assertThat(viewModel.uiState.value.trainedSets).isEqualTo(5)
        assertThat(viewModel.uiState.value.view).isEqualTo(BalanceView.PLAN)
        assertThat(viewModel.uiState.value.window).isEqualTo(TrainedWindow.LAST_7_DAYS)
        // Seven dates including today, half-open at tomorrow's midnight (N289).
        assertThat(statistics.windows.single()).isEqualTo(
            startOf(LocalDate.of(2026, 9, 26)) to startOf(LocalDate.of(2026, 10, 3)),
        )
    }

    @Test
    fun selectingTrained_andTheTwentyEightDayWindow_readsTheWiderDates() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("squat", workingSets = 1)))
        exercises.library.value = DataResult.Success(
            listOf(Exercise(id = "squat", name = "Squat", primaryMuscle = MuscleGroup.QUADS)),
        )
        statistics.workingSets.value = DataResult.Success(
            listOf(ExerciseWorkingSets(exerciseId = "squat", workingSets = 4)),
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSelectView(BalanceView.TRAINED)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.view).isEqualTo(BalanceView.TRAINED)
        assertThat(viewModel.uiState.value.trained).containsExactly(MuscleSets(MuscleGroup.QUADS, 8))

        viewModel.onSelectWindow(TrainedWindow.LAST_28_DAYS)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.window).isEqualTo(TrainedWindow.LAST_28_DAYS)
        // The screen still shows the same count, but it was asked for over the wider window.
        assertThat(statistics.windows.last()).isEqualTo(
            startOf(LocalDate.of(2026, 9, 5)) to startOf(LocalDate.of(2026, 10, 3)),
        )
        assertThat(viewModel.uiState.value.trainedSets).isEqualTo(4)

        viewModel.onSelectView(BalanceView.PLAN)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.view).isEqualTo(BalanceView.PLAN)
        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(1)
    }

    @Test
    fun aFailedLogRead_isAFailure_ratherThanAnEmptyWindow() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("squat", workingSets = 1)))
        exercises.library.value = DataResult.Success(
            listOf(Exercise(id = "squat", name = "Squat", primaryMuscle = MuscleGroup.QUADS)),
        )
        statistics.workingSets.value = DataResult.Failure(DataError.Storage(IllegalStateException("disk")))

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trainedReadFailed).isTrue()
        assertThat(viewModel.uiState.value.trained).isEmpty()
        assertThat(viewModel.uiState.value.trainedSets).isEqualTo(0)
    }

    @Test
    fun aResumeAfterMidnight_movesTheWindowsEnd() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("squat", workingSets = 1)))
        exercises.library.value = DataResult.Success(
            listOf(Exercise(id = "squat", name = "Squat", primaryMuscle = MuscleGroup.QUADS)),
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        now = Instant.parse("2026-10-03T09:00:00Z")
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(statistics.windows.last()).isEqualTo(
            startOf(LocalDate.of(2026, 9, 27)) to startOf(LocalDate.of(2026, 10, 4)),
        )
    }

    private fun slot(id: String, templateId: String, position: Int) = ProgramSlot(
        id = id,
        programId = PROGRAM_ID,
        templateId = templateId,
        position = position,
    )

    /** One planned exercise whose working sets are all ordinary, so each counts as work (N112). */
    private fun planned(exerciseId: String, workingSets: Int) = TemplateExercise(
        id = "te-$exerciseId",
        templateId = "t",
        exerciseId = exerciseId,
        position = 0,
        exerciseName = exerciseId,
        primaryMuscle = null,
        equipment = null,
        sets = (0 until workingSets).map { index ->
            TemplateSet(
                id = "ts-$exerciseId-$index",
                templateExerciseId = "te-$exerciseId",
                setIndex = index,
            )
        },
    )

    private companion object {
        const val PROGRAM_ID = "p1"
    }
}

/**
 * The statistics port, with the one read the balance view takes.
 *
 * Every window the ViewModel asks for is recorded, so a test can assert the dates rather than only the count;
 * the log itself is a flow the test sets, because the real read is a Room query.
 */
private class FakeStatisticsRepository : StatisticsRepository {

    val workingSets = MutableStateFlow<DataResult<List<ExerciseWorkingSets>>>(DataResult.Success(emptyList()))

    /** Each window requested, in order, so the toggle's effect is observable. */
    val windows = mutableListOf<Pair<Instant, Instant>>()

    override fun observeRecentRecords(limit: Int): Flow<List<RecordEvent>> =
        flowOf(emptyList())

    override fun observeWorkingSetsIn(
        from: Instant,
        toExclusive: Instant,
    ): Flow<DataResult<List<ExerciseWorkingSets>>> {
        windows += from to toExclusive
        return workingSets
    }

    /** The statistics screen's own reads; that screen is not under test, so they answer empty. */
    override fun observeWorkoutSummaries(): Flow<List<WorkoutSummary>> = flowOf(emptyList())

    override fun observePlanTargets(exerciseIds: List<String>): Flow<List<PlanTarget>> = flowOf(emptyList())

    override suspend fun countRecordsIn(from: Instant, to: Instant): DataResult<Int> = DataResult.Success(0)
}
