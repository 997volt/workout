package com.example.androidapp.ui.measurements

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.model.kind
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.domain.repository.GoalRepository
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.ui.statistics.WEIGHT_METRIC_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the measurements screen's ViewModel owns: passing an entry on, and reporting what came back. */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementsViewModelTest {

    private val clock = TimeSource { Instant.parse("2026-10-21T10:00:00Z") }

    /** The day the ViewModel calls today, read the way it reads it, so these tests hold in any zone. */
    private val today: LocalDate = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()

    private fun viewModelOf(repository: MeasurementRepository, goals: GoalRepository) =
        MeasurementsViewModel(repository, goals, clock)

    private val dispatcher = StandardTestDispatcher()
    private val entries = MutableStateFlow<List<BodyMeasurement>>(emptyList())

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun settingADirectionGoal_opensAPeriodWithTheKind_andNoWeight() = runTest(dispatcher) {
        // N135's write, moved here with the row: the goal is set where the body is recorded.
        val goals = FakeGoalRepository()
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Direction(gain = true))
        advanceUntilIdle()

        val open = goals.observePeriods(WEIGHT_METRIC_ID).first().last()
        assertEquals(WeightGoalKind.GAIN.name, open.kind)
        assertNull(open.value)
    }

    @Test
    fun settingATargetGoal_opensOnePeriodWithBothTheWeight_andItsKind() = runTest(dispatcher) {
        // One row rather than two writes (N137), so a failure between the value and the kind cannot arise.
        val goals = FakeGoalRepository()
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Target(75_000L))
        advanceUntilIdle()

        val periods = goals.observePeriods(WEIGHT_METRIC_ID).first()
        assertEquals(1, periods.size)
        assertEquals(75_000.0, periods.last().value)
        assertEquals(WeightGoalKind.TARGET.name, periods.last().kind)
    }

    @Test
    fun theGoalAndItsHistory_areInTheStateTheRowReads() = runTest(dispatcher) {
        // B215: the row moved from statistics to this screen, and the three-flow combine that feeds it had no
        // test — only the write was checked, not that the goal and its periods reach the state the row draws.
        val goals = FakeGoalRepository()
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Target(75_000L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WeightGoal.Target(75_000L), state.weightGoal)
        assertEquals(1, state.goalHistory.size)
    }

    @Test
    fun editingAPeriod_writesTheRowTheEditorNamed() = runTest(dispatcher) {
        // N215: the editor names the period, and the write is that row rather than a new boundary.
        val goals = FakeGoalRepository(
            current = mapOf(
                WEIGHT_ID to GoalPeriod(id = "p1", startedAt = FIRST_DAY, kind = "LOSE", value = null),
            ),
        )
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSaveGoalPeriod(
            id = "p1",
            start = today.minusDays(30),
            end = null,
            goal = WeightGoal.Target(80_000L),
        )
        advanceUntilIdle()

        assertEquals(listOf("p1" to WeightGoal.Target(80_000L)), goals.saves)
        assertEquals("p1", goals.observePeriods(WEIGHT_METRIC_ID).first().single().id)
    }

    @Test
    fun removingAPeriod_offersItBack_andTheUndoPutsItBack() = runTest(dispatcher) {
        val goals = FakeGoalRepository(
            current = mapOf(
                WEIGHT_ID to GoalPeriod(id = "p1", startedAt = FIRST_DAY, kind = "TARGET", value = 80_000.0),
            ),
        )
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRemoveGoalPeriod("p1")
        advanceUntilIdle()

        assertEquals(listOf("p1"), goals.removed)
        assertEquals("p1", viewModel.pendingGoalUndo.value?.id)

        viewModel.onUndoGoalPeriodRemoval()
        advanceUntilIdle()

        assertEquals(listOf("p1"), goals.restored)
        assertNull("the offer is taken by the undo", viewModel.pendingGoalUndo.value)
    }

    @Test
    fun dismissingTheGoalUndo_takesTheOfferAway() = runTest(dispatcher) {
        val goals = FakeGoalRepository(
            current = mapOf(
                WEIGHT_ID to GoalPeriod(id = "p1", startedAt = FIRST_DAY, kind = "TARGET", value = 80_000.0),
            ),
        )
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRemoveGoalPeriod("p1")
        advanceUntilIdle()
        viewModel.onDismissGoalUndo()

        assertNull(viewModel.pendingGoalUndo.value)
        assertTrue("nothing is restored when the offer is dismissed", goals.restored.isEmpty())
    }

    @Test
    fun savingAGoal_takesTheRemovalOfferAway() = runTest(dispatcher) {
        val goals = FakeGoalRepository(
            current = mapOf(
                WEIGHT_ID to GoalPeriod(id = "p1", startedAt = FIRST_DAY, kind = "TARGET", value = 80_000.0),
            ),
        )
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRemoveGoalPeriod("p1")
        advanceUntilIdle()
        assertEquals("the removal offered its undo", "p1", viewModel.pendingGoalUndo.value?.id)

        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Target(79_000L))
        advanceUntilIdle()

        assertNull("a newer statement ends the offer", viewModel.pendingGoalUndo.value)
    }

    @Test
    fun aGoalSave_reportsItsResult_andCanBeDismissed() = runTest(dispatcher) {
        // B234: the editor closes on true and stays on false; the one-shot is taken away by the screen.
        val goals = FakeGoalRepository()
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), goals)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Target(80_000L))
        advanceUntilIdle()
        assertEquals(true, viewModel.goalSave.value)

        viewModel.onDismissGoalSave()
        assertNull(viewModel.goalSave.value)

        goals.failSaves = true
        viewModel.onSaveGoalPeriod(id = null, start = today, end = null, goal = WeightGoal.Target(81_000L))
        advanceUntilIdle()
        assertEquals(false, viewModel.goalSave.value)
    }

    @Test
    fun itShowsWhatTheRepositoryHolds_newestFirst() = runTest(dispatcher) {
        entries.value = listOf(entry("m2", weight = 82_000L), entry("m1", weight = 83_000L))
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(
            listOf("m2", "m1"),
            viewModel.uiState.value.entries.map { it.id },
        )
    }

    @Test
    fun todaysDailyEntry_isTheOneTheShortcutEdits() = runTest(dispatcher) {
        // B182: the home row opens this screen wanting today's entry, so the screen has to know which row
        // that is. A tape row on the same day is not it (N113), and neither is yesterday's daily record.
        val today = Instant.parse("2026-10-21T10:00:00Z")
        entries.value = listOf(
            BodyMeasurement(
                id = "today",
                measuredAt = today,
                kind = MeasurementKind.DAILY,
                weightGrams = 82_000L,
            ),
            BodyMeasurement(
                id = "yesterday",
                measuredAt = today.minus(Duration.ofDays(1)),
                kind = MeasurementKind.DAILY,
                weightGrams = 83_000L,
            ),
            BodyMeasurement(
                id = "tape",
                measuredAt = today,
                kind = MeasurementKind.TAPE,
                tape = mapOf(TapeSite.WAIST to 900L),
            ),
        )
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("today", viewModel.uiState.value.todayEntry?.id)
    }

    @Test
    fun saving_passesTheEntryThrough_andClearsTheError() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(entries)
        repository.failNextSave = true
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertTrue(
            "a failure is surfaced",
            viewModel.error.value is DataError.Storage,
        )

        repository.failNextSave = false
        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertNull("and cleared by the next save that works", viewModel.error.value)
    }

    @Test
    fun dismissingTheError_clearsIt() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(entries)
        repository.failNextSave = true
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertTrue(viewModel.error.value is DataError.Storage)

        viewModel.onDismissError()

        assertNull("dismissing clears it, so the snackbar is not left owed", viewModel.error.value)
    }

    @Test
    fun deleting_offersTheEntryBack() = runTest(dispatcher) {
        // N133: what the snackbar's Undo puts back is the entry itself, read before the write — after it
        // the row is not in the list, and the message has to name what went.
        entries.value = listOf(entry("m1", weight = 82_000L))
        val viewModel = viewModelOf(FakeMeasurementRepository(entries), FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()

        assertEquals("m1", viewModel.pendingUndo.value?.id)
    }

    @Test
    fun undo_restoresTheEntry_itOffered() = runTest(dispatcher) {
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onUndoDelete()
        advanceUntilIdle()

        assertEquals("the undo reached the row the delete took", "m1", repository.restored)
        assertNull("and the offer is taken rather than left standing", viewModel.pendingUndo.value)
    }

    @Test
    fun aRefusedUndo_saysWhy_ratherThanFailingSilently() = runTest(dispatcher) {
        // The collision N133 names: the day was re-entered, so the restore is refused with a sentence.
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        repository.failNextRestore = true
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onUndoDelete()
        advanceUntilIdle()

        assertEquals(
            "the refusal is the sentence, which the snackbar shows",
            DataError.Invalid("That day has a newer daily measurement."),
            viewModel.error.value,
        )
    }

    @Test
    fun dismissingTheUndo_dropsTheOffer_withoutWritingAnything() = runTest(dispatcher) {
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onDismissUndo()

        assertNull(viewModel.pendingUndo.value)
        assertNull("taking the message is not a restore", repository.restored)
    }

    @Test
    fun aWeighIn_answersWhetherTheTrendIsTheGoal() = runTest(dispatcher) {
        // N136: the answer lived under a chart in Statistics, so a lifter had to go and look for it. Here it
        // arrives with the weighing — and a flat trend against a lose goal is the case it exists to name.
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 82_000L, at = LAST_DAY))
        advanceUntilIdle()

        val verdict = viewModel.rateVerdict.value
        assertNotNull("a weigh-in with a goal and enough history says something", verdict)
        assertFalse("a flat trend against a lose goal is not on target", verdict!!.isOnTarget)
        assertFalse("and the move it names is to eat less", verdict.eatsMore)
    }

    @Test
    fun withNoGoal_aWeighIn_saysNothingAtAll() = runTest(dispatcher) {
        // The one prompt this app must never grow: a direction is the lifter's, and this one has none.
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        val viewModel = viewModelOf(repository, FakeGoalRepository())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 82_000L, at = LAST_DAY))
        advanceUntilIdle()

        assertNull("no goal, no verdict", viewModel.rateVerdict.value)
    }

    @Test
    fun belowTheFloor_aWeighIn_saysNothing() = runTest(dispatcher) {
        // Three readings are a number and not a trend, and a prompt that fires on water weight is worse than no
        // prompt — so N98's floor is kept rather than the reference's shorter window (N136).
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns(count = 3)))
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 82_000L, at = FIRST_DAY.plus(Duration.ofDays(6))))
        advanceUntilIdle()

        assertNull("too little to be sure, so nothing is said", viewModel.rateVerdict.value)
    }

    @Test
    fun aTapeDay_isNotAWeighIn() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(tapeEntry())
        advanceUntilIdle()

        assertNull("a tape day has no weight to answer about", viewModel.rateVerdict.value)
    }

    @Test
    fun aSorenessOnlyDay_isNotAWeighIn() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(
            BodyMeasurement(
                id = "s1",
                measuredAt = LAST_DAY,
                kind = MeasurementKind.DAILY,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            ),
        )
        advanceUntilIdle()

        assertNull("a morning with no weight on the scale answers nothing", viewModel.rateVerdict.value)
    }

    @Test
    fun aMaintenanceGoal_isJudgedAgainstItsCorridor() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns(baseGrams = 80_000L)))
        val viewModel = viewModelOf(
            repository,
            settingsFor(WeightGoalKind.MAINTAIN, grams = 80_000.0),
        )
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 80_200L, at = LAST_DAY))
        advanceUntilIdle()

        assertNotNull("a corridor answers too, rather than a rate of zero", viewModel.rateVerdict.value)
        assertTrue("and a weight inside it is on target", viewModel.rateVerdict.value!!.isOnTarget)
    }

    @Test
    fun aFailedSave_saysNothing_ratherThanAnsweringFromNothing() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        repository.failNextSave = true
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 82_000L, at = LAST_DAY))
        advanceUntilIdle()

        assertTrue(viewModel.error.value is DataError.Storage)
        assertNull("nothing was written, so there is nothing to say about it", viewModel.rateVerdict.value)
    }

    @Test
    fun dismissingTheVerdict_clearsIt() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(MutableStateFlow(weighIns()))
        val viewModel = viewModelOf(repository, settingsFor(WeightGoalKind.LOSE))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("m-new", weight = 82_000L, at = LAST_DAY))
        advanceUntilIdle()
        viewModel.onDismissRateVerdict()

        assertNull("the sentence is taken rather than left standing", viewModel.rateVerdict.value)
    }

    /** The period that is open: a weight goal of [kind], at [grams] where the kind states a weight (N135). */
    private fun settingsFor(kind: WeightGoalKind, grams: Double = 78_000.0) = FakeGoalRepository(
        current = mapOf(WEIGHT_ID to GoalPeriod(id = "p0", startedAt = null, kind = kind.name, value = grams)),
    )

    private fun entry(id: String, weight: Long, at: Instant = FIRST_DAY) = BodyMeasurement(
        id = id,
        measuredAt = at,
        kind = MeasurementKind.DAILY,
        weightGrams = weight,
    )

    /** A tape day: a record, but not a weigh-in (N113, N136). */
    private fun tapeEntry() = BodyMeasurement(
        id = "t1",
        measuredAt = FIRST_DAY,
        kind = MeasurementKind.TAPE,
        tape = mapOf(TapeSite.WAIST to 860L),
    )

    /**
     * The history N98's floor asks for: eight weigh-ins across three weeks, [perDayGrams] a day apart.
     *
     * Below it the prompt must stay silent, which is the floor N136 keeps rather than the reference's ten days.
     */
    private fun weighIns(perDayGrams: Long = 0L, baseGrams: Long = 82_000L, count: Int = 8) =
        (0 until count).map { index ->
            val day = index * (FLOOR_DAYS / (count - 1))
            entry("m$index", weight = baseGrams + perDayGrams * day, at = FIRST_DAY.plus(Duration.ofDays(day)))
        }

    private companion object {
        val FIRST_DAY: Instant = Instant.parse("2026-10-01T08:00:00Z")
        val LAST_DAY: Instant = FIRST_DAY.plus(Duration.ofDays(FLOOR_DAYS))

        /** N98's floor, in days: the span eight readings must cover before the app will speak. */
        const val FLOOR_DAYS = 21L

        /** The key the weight goal is stored under (ROADMAP N135). */
        const val WEIGHT_ID = "BODY:WEIGHT"
    }
}

private class FakeMeasurementRepository(
    private val entries: MutableStateFlow<List<BodyMeasurement>>,
) : MeasurementRepository {

    var failNextSave = false
    var failNextRestore = false

    /** What the repository was asked to delete, so a test can read the write back. */
    var deleted: String? = null

    /** What the repository was asked to restore (ROADMAP N133). */
    var restored: String? = null

    override fun observeAll(): Flow<List<BodyMeasurement>> = entries

    override suspend fun save(measurement: BodyMeasurement): DataResult<Unit> {
        if (failNextSave) return DataResult.Failure(DataError.Storage(IllegalStateException()))
        // Placed by its id, as the real repository places it: a save both adds and corrects, which is what the
        // weigh-in verdict reads the series back from (N136).
        entries.value = entries.value.filterNot { it.id == measurement.id && measurement.id.isNotEmpty() } +
            measurement
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted = id
        entries.value = entries.value.filterNot { it.id == id }
        return DataResult.Success(Unit)
    }

    override suspend fun restore(id: String): DataResult<Unit> {
        restored = id
        if (failNextRestore) {
            return DataResult.Failure(DataError.Invalid("That day has a newer daily measurement."))
        }
        return DataResult.Success(Unit)
    }
}

/**
 * The goal history, with only what a weigh-in reads: the period that is open for the weight metric (N137).
 *
 * The writes record what they were asked for, because nothing on this screen sets a goal — the statistics
 * screen does.
 */
private class FakeGoalRepository(
    current: Map<String, GoalPeriod> = emptyMap(),
) : GoalRepository {
    private val periods = MutableStateFlow(current.mapValues { (_, period) -> listOf(period) })

    /** Every period write, as the id asked for (null for a new one) and the goal it stated (N215). */
    val saves = mutableListOf<Pair<String?, WeightGoal?>>()

    /** A goal write can be refused; only a test asks for that (B234). */
    var failSaves = false
    val removed = mutableListOf<String>()
    val restored = mutableListOf<String>()

    override fun observeCurrent(): Flow<Map<String, GoalPeriod>> =
        periods.map { all -> all.mapNotNull { (id, rows) -> rows.lastOrNull()?.let { id to it } }.toMap() }

    override fun observePeriods(metricId: String): Flow<List<GoalPeriod>> =
        periods.map { it[metricId].orEmpty() }

    override suspend fun savePeriod(
        metricId: String,
        id: String?,
        start: LocalDate?,
        end: LocalDate?,
        goal: WeightGoal?,
    ): DataResult<Unit> {
        if (failSaves) {
            return DataResult.Failure(DataError.Storage(IllegalStateException("the goal was not stored")))
        }
        saves += id to goal
        val rows = periods.value[metricId].orEmpty()
        val startedAt = start?.atStartOfDay(ZoneId.systemDefault())?.toInstant()
        val kind = goal?.kind()?.name
        val value = goal?.statedWeightGrams()?.toDouble()
        periods.value = periods.value + (
            metricId to if (id != null) {
                rows.map {
                    if (it.id == id) it.copy(startedAt = startedAt ?: it.startedAt, kind = kind, value = value) else it
                }
            } else {
                rows + GoalPeriod(id = "p${rows.size + 1}", startedAt = startedAt, kind = kind, value = value)
            }
            )
        return DataResult.Success(Unit)
    }

    override suspend fun setLevelGoal(metricId: String, value: Double?): DataResult<Unit> {
        return DataResult.Success(Unit)
    }

    override suspend fun removePeriod(id: String): DataResult<Unit> {
        removed += id
        periods.value = periods.value.mapValues { (_, rows) -> rows.filterNot { it.id == id } }
        return DataResult.Success(Unit)
    }

    override suspend fun restorePeriod(metricId: String, period: GoalPeriod): DataResult<Unit> {
        restored += period.id
        periods.value = periods.value + (metricId to periods.value[metricId].orEmpty() + period)
        return DataResult.Success(Unit)
    }
}
