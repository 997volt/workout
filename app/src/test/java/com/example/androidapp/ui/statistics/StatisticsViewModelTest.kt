package com.example.androidapp.ui.statistics

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import androidx.lifecycle.SavedStateHandle
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.DataError
import kotlinx.coroutines.launch
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ExerciseTrendMetric
import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.RangeKind
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.TrendMetric
import com.example.androidapp.domain.model.TrendPoint
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.repository.StatisticsRepository
import com.example.androidapp.domain.repository.TrendsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What the Statistics screen reads (ROADMAP N35).
 *
 * The interesting assertions are the wiring: which source each metric comes from, that the range reaches
 * both the totals and the series, and that a metric needing a lift says so rather than drawing nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-02T09:00:00Z")
    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = now.atZone(zone).toLocalDate()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun daysAgo(days: Long): Instant = today.minusDays(days).atTime(12, 0).atZone(zone).toInstant()

    @Test
    fun theRangeComesFromTheStore_andBodyweightIsTheDefaultMetric() = runTest(dispatcher) {
        val viewModel = viewModel(range = StatisticsRange(RangeKind.LAST_MONTH))
        observe(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.range.kind).isEqualTo(RangeKind.LAST_MONTH)
        assertThat(viewModel.uiState.value.selection.metric).isEqualTo(MetricKey.Body(BodyMetric.WEIGHT))
    }

    @Test
    fun theOverview_countsTheWorkoutsInTheRange() = runTest(dispatcher) {
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.LAST_7_DAYS),
            sessions = listOf(
                summary(daysAgo(1), volume = 10_000L),
                summary(daysAgo(3), volume = 5_000L),
                // Two months old: outside the window, so it must not be counted.
                summary(daysAgo(60), volume = 999_000L),
            ),
            records = 3,
        )
        observe(viewModel)
        advanceUntilIdle()

        val overview = viewModel.uiState.value.overview
        assertThat(overview.workouts).isEqualTo(2)
        assertThat(overview.volumeGrams).isEqualTo(15_000L)
        assertWithMessage("the record count comes from its own query").that(overview.personalRecords).isEqualTo(3)
    }

    @Test
    fun theSeries_isTheSelectedMetric_filteredToTheRange() = runTest(dispatcher) {
        // Two weigh-ins inside the window and one long before it.
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.LAST_7_DAYS),
            body = listOf(
                measurement(daysAgo(1), weight = 82_000L),
                measurement(daysAgo(4), weight = 83_000L),
                measurement(daysAgo(90), weight = 90_000L),
            ),
        )
        observe(viewModel)
        advanceUntilIdle()

        val series = viewModel.uiState.value.series
        assertWithMessage("only the readings inside the window").that(series?.readings?.size).isEqualTo(2)
        assertThat(series?.readings?.first()?.value).isEqualTo(83_000.0)
    }

    @Test
    fun aWorkoutMetric_readsTheWorkoutsOwnPoints() = runTest(dispatcher) {
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.ALL),
            points = listOf(TrendPoint(startedAt = daysAgo(1), averageRpe = 8.0)),
        )
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onSelectMetric(MetricKey.Workout(TrendMetric.RPE))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.series?.readings?.single()?.value).isEqualTo(8.0)
    }

    @Test
    fun anExerciseMetric_readsTheChosenLiftsPoints() = runTest(dispatcher) {
        val trends = FakeTrendsRepository(
            exercisePoints = listOf(
                ExerciseTrendPoint(startedAt = daysAgo(2), volumeGrams = 4_000_000L),
            ),
        )
        val viewModel = viewModel(range = StatisticsRange(RangeKind.ALL), trends = trends)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onSelectMetric(MetricKey.Exercise(ExerciseTrendMetric.VOLUME))
        advanceUntilIdle()
        assertWithMessage("with no lift chosen yet, the screen says so").that(viewModel.uiState.value.needsExercise)
            .isTrue()

        viewModel.onSelectExercise("back-squat")
        advanceUntilIdle()

        assertThat(trends.askedFor).containsExactly("back-squat")
        assertThat(viewModel.uiState.value.series?.readings?.single()?.value).isEqualTo(4_000_000.0)
        assertThat(viewModel.uiState.value.needsExercise).isEqualTo(false)
    }

    @Test
    fun choosingARange_storesIt() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = viewModel(range = StatisticsRange(), settings = settings)
        observe(viewModel)
        advanceUntilIdle()

        val custom = StatisticsRange(RangeKind.CUSTOM, today.minusDays(30), today)
        viewModel.onSelectRange(custom)
        advanceUntilIdle()

        assertThat(settings.stored.value).isEqualTo(custom)
    }

    /** A measurement entry, with only the fields these tests care about. */
    private fun measurement(at: Instant, weight: Long) = BodyMeasurement(
        id = "m-$at",
        measuredAt = at,
        weightGrams = weight,
        tape = emptyMap<TapeSite, Long>(),
    )

    @Test
    fun arrivingWithALift_preselectsIt_andAStrengthMetric() {
        // What "how is my bench going" arrives with, from the library or from the lift just performed: an
        // Exercise series means nothing until a lift is chosen, so the screen must not land on bodyweight.
        val selection = initialSelection("bench-press")

        assertThat(selection.exerciseId).isEqualTo("bench-press")
        assertThat(selection.metric is MetricKey.Exercise).isTrue()
    }

    @Test
    fun arrivingWithNoLift_startsOnBodyweight() {
        assertThat(initialSelection(null)).isEqualTo(StatisticsSelection())
    }

    @Test
    fun theLibrary_reachesTheState_forTheLiftPicker() = runTest(dispatcher) {
        // The picker offers the library's names and nothing else: a second way to name a lift would be a
        // second thing to keep in step with it.
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.ALL),
            lifts = listOf(lift("back-squat", "Back Squat"), lift("bench-press", "Barbell Bench Press")),
        )
        observe(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.lifts.map { it.name }).isEqualTo(listOf("Back Squat", "Barbell Bench Press"))
    }

    @Test
    fun theLiftPicker_offersAHead_onlyWhereTheMetricReadsOne() = runTest(dispatcher) {
        // ROADMAP N97, B98: this is the one list where a head *is* offered — N95's "never offered" is about
        // logging, and a head is still never named by a set. But it is offered only where the metric reads a
        // family: volume is a sum over one, while a heaviest set merged across a speed day and a competition
        // single would read as a decline that never happened — and a category is never named by a set, so
        // offering it there drew an empty chart under a label promising the family.
        val trends = FakeTrendsRepository()
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.ALL),
            lifts = listOf(
                lift("back-squat", "Back Squat"),
                lift("cat-bench", "Bench Press", rowKind = RowKind.CATEGORY),
                // A category a lifter has made and filed nothing under: it is not a family, so offering it
                // would promise one and draw nothing.
                lift("cat-empty", "Empty Family", rowKind = RowKind.CATEGORY),
                lift("barbell-bench-press", "Barbell Bench Press", parentId = "cat-bench"),
                lift("paused-bench", "Paused Bench", parentId = "barbell-bench-press"),
            ),
            trends = trends,
        )
        observe(viewModel)
        advanceUntilIdle()

        assertWithMessage("the default metric reads one id, so no head is offered")
            .that(viewModel.uiState.value.lifts.map { it.name })
            .containsExactly("Back Squat", "Barbell Bench Press", "Paused Bench")

        viewModel.onSelectMetric(MetricKey.Exercise(ExerciseTrendMetric.VOLUME))
        advanceUntilIdle()
        assertWithMessage("volume offers the heads that head something")
            .that(viewModel.uiState.value.lifts.map { it.name })
            .containsExactly("Back Squat", "Bench Press", "Barbell Bench Press", "Paused Bench")

        viewModel.onSelectExercise("cat-bench")
        advanceUntilIdle()
        assertWithMessage("volume reads the whole family")
            .that(trends.askedFor)
            .containsExactly("cat-bench", "barbell-bench-press", "paused-bench")

        viewModel.onSelectMetric(MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET))
        advanceUntilIdle()
        assertWithMessage("a heaviest set offers movements only")
            .that(viewModel.uiState.value.lifts.map { it.name })
            .containsExactly("Back Squat", "Barbell Bench Press", "Paused Bench")
        assertWithMessage("and the head it can no longer offer is dropped rather than left hidden")
            .that(viewModel.uiState.value.selection.exerciseId)
            .isNull()

        viewModel.onSelectExercise("barbell-bench-press")
        advanceUntilIdle()
        assertWithMessage("a heaviest set reads the row that was chosen")
            .that(trends.askedFor)
            .containsExactly("barbell-bench-press")
    }

    private fun lift(
        id: String,
        name: String,
        rowKind: RowKind = RowKind.MOVEMENT,
        parentId: String? = null,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        isCustom = false,
        parentId = parentId,
        rowKind = rowKind,
    )

    private fun summary(at: Instant, volume: Long) = WorkoutSummary(
        id = "s-$at",
        startedAt = at,
        finishedAt = at.plusSeconds(3_600),
        exerciseCount = 1,
        setCount = 3,
        volumeGrams = volume,
        zoneOffsetMinutes = 0,
        repeatableExerciseCount = 1,
    )

    private fun viewModel(
        range: StatisticsRange,
        sessions: List<WorkoutSummary> = emptyList(),
        points: List<TrendPoint> = emptyList(),
        body: List<BodyMeasurement> = emptyList(),
        records: Int? = null,
        lifts: List<Exercise> = emptyList(),
        goals: Map<String, Double> = emptyMap(),
        // Defaults may reference earlier parameters, which is what keeps `points` from being a parameter
        // nobody reads — the bug this test found in its own fixture.
        trends: FakeTrendsRepository = FakeTrendsRepository(points = points),
        settings: FakeSettingsRepository = FakeSettingsRepository(range, goals),
        statistics: FakeStatisticsRepository = FakeStatisticsRepository(sessions, records),
    ) = StatisticsViewModel(
        settings = settings,
        repositories = StatisticsRepositories(
            statistics = statistics,
            trends = trends,
            measurements = FakeMeasurementRepository(body),
            exercises = FakeExerciseRepository(lifts),
        ),
        timeSource = TimeSource { now },
        savedStateHandle = SavedStateHandle(),
    )

    /**
     * A collect never returns, so it goes in the background scope: `runTest` cancels that at the end, and a
     * plain `launch` here would leave the test waiting a minute for a coroutine that is doing its job.
     */
    private fun TestScope.observe(viewModel: StatisticsViewModel) {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
    }


    @Test
    fun aGoalForTheChosenMetric_reachesTheState() = runTest(dispatcher) {
        // The chart draws it and the row shows it, so this is where the screen's copy comes from.
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.ALL),
            goals = mapOf("BODY:WEIGHT" to 80_000.0),
        )
        observe(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.goal).isEqualTo(80_000.0)
    }

    @Test
    fun aGoalForAnotherMetric_isNotShown() = runTest(dispatcher) {
        // Otherwise a target set for bodyweight would draw a line across a chart of volume.
        val viewModel = viewModel(
            range = StatisticsRange(RangeKind.ALL),
            goals = mapOf("BODY:WAIST" to 800.0),
        )
        observe(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.goal).isNull()
    }

    @Test
    fun theAllRange_stillCountsRecords() = runTest(dispatcher) {
        // "All" has no window, and that used to end the flow early — so the tile showed a permanent dash on
        // the one range where a lifetime count means most. Unbounded is a range the query can express.
        val statistics = FakeStatisticsRepository(sessions = emptyList(), records = 7)
        val viewModel = viewModel(range = StatisticsRange(RangeKind.ALL), statistics = statistics)
        observe(viewModel)
        advanceUntilIdle()

        assertThat(statistics.asked).isTrue()
        assertThat(viewModel.uiState.value.overview.personalRecords).isEqualTo(7)
    }

    @Test
    fun aWindowedRange_countsRecordsToo() = runTest(dispatcher) {
        // The ordinary case, kept honest beside the new one.
        val statistics = FakeStatisticsRepository(sessions = emptyList(), records = 3)
        val viewModel = viewModel(range = StatisticsRange(RangeKind.LAST_7_DAYS), statistics = statistics)
        observe(viewModel)
        advanceUntilIdle()

        assertThat(statistics.asked).isTrue()
        assertThat(viewModel.uiState.value.overview.personalRecords).isEqualTo(3)
    }
}

private class FakeSettingsRepository(
    initial: StatisticsRange = StatisticsRange(),
    initialGoals: Map<String, Double> = emptyMap(),
) : SettingsRepository {
    val stored = MutableStateFlow(initial)
    private val goals = MutableStateFlow(initialGoals)

    override fun observeGoals(): Flow<Map<String, Double>> = goals

    override suspend fun setGoal(metricId: String, value: Double?): DataResult<Unit> {
        goals.value = goals.value.toMutableMap().apply {
            if (value == null) remove(metricId) else put(metricId, value)
        }
        return DataResult.Success(Unit)
    }
    override fun observeDefaultRestSeconds(): Flow<Int> = flowOf(45)
    override suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeRestCueEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeKeepScreenOn(): Flow<Boolean> = flowOf(true)
    override suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeRestTimerEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit> =
        DataResult.Success(Unit)

    override fun observeWeightUnit(): Flow<WeightUnit> = flowOf(WeightUnit.KILOGRAMS)

    override suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeProgressionPromptEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setProgressionPromptEnabled(enabled: Boolean): DataResult<Unit> =
        DataResult.Success(Unit)
    override fun observeStatisticsRange(): Flow<StatisticsRange> = stored
    override suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit> {
        stored.value = range
        return DataResult.Success(Unit)
    }
}

private class FakeStatisticsRepository(
    private val sessions: List<WorkoutSummary>,
    private val records: Int?,
) : StatisticsRepository {
    /** Whether the count was asked for at all — the distinction the "All" range used to lose. */
    var asked = false
        private set

    override fun observeWorkoutSummaries(): Flow<List<WorkoutSummary>> = flowOf(sessions)

    override suspend fun countRecordsIn(from: Instant, to: Instant): DataResult<Int> {
        asked = true
        return DataResult.Success(records ?: 0)
    }
}

private class FakeTrendsRepository(
    private val points: List<TrendPoint> = emptyList(),
    private val exercisePoints: List<ExerciseTrendPoint> = emptyList(),
) : TrendsRepository {
    var askedFor: List<String>? = null
        private set

    override fun observeTrends(limit: Int): Flow<DataResult<List<TrendPoint>>> =
        flowOf(DataResult.Success(points))

    override fun observeExerciseTrends(
        exerciseIds: List<String>,
        limit: Int,
    ): Flow<DataResult<List<ExerciseTrendPoint>>> {
        askedFor = exerciseIds
        return flowOf(DataResult.Success(exercisePoints))
    }
}

private class FakeMeasurementRepository(
    private val entries: List<BodyMeasurement>,
) : MeasurementRepository {
    override fun observeAll(): Flow<List<BodyMeasurement>> = flowOf(entries)
    override suspend fun save(measurement: BodyMeasurement): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun delete(id: String): DataResult<Unit> = DataResult.Success(Unit)
}

private class FakeExerciseRepository(private val lifts: List<Exercise>) : ExerciseRepository {
    override fun observeExercises(): Flow<DataResult<List<Exercise>>> = flowOf(DataResult.Success(lifts))

    override suspend fun getExercise(id: String): DataResult<Exercise?> =
        DataResult.Success(lifts.firstOrNull { it.id == id })

    override suspend fun createCustomExercise(name: String): DataResult<Exercise> =
        DataResult.Failure(DataError.Invalid("not used here"))

    override suspend fun createCategory(name: String): DataResult<Exercise> =
        DataResult.Failure(DataError.Invalid("not used here"))

    override suspend fun createVariationOf(parent: Exercise): DataResult<Exercise> =
        DataResult.Failure(DataError.Invalid("not used here"))

    override suspend fun getAllIncludingDeleted(): DataResult<List<Exercise>> =
        DataResult.Success(lifts)

    override suspend fun updateExercise(exercise: Exercise): DataResult<Unit> = DataResult.Success(Unit)


}
