package com.example.androidapp.ui.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ExerciseTrendMetric
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.RangeKind
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.model.TrendPoint
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.isTargetReached
import com.example.androidapp.domain.model.rateBandPerWeekGrams
import com.example.androidapp.domain.model.ratePerWeekGrams
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.domain.model.targetDateEstimate
import com.example.androidapp.domain.model.window
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.domain.repository.GoalRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.seriesSubjectIds
import com.example.androidapp.ui.navigation.Statistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * What the picker starts on (ROADMAP N35).
 *
 * Arriving with a lift — "how is my bench going", from the library or from the lift just performed —
 * selects that lift and a strength metric: an Exercise series means nothing until a lift is chosen, and
 * bodyweight would answer a question nobody asked. Estimated 1RM rather than the heaviest set, because it
 * accounts for the reps a heavy single and a hard set of five differ by.
 *
 * Without a lift, bodyweight: the series a person checks most often.
 */
fun initialSelection(exerciseId: String?): StatisticsSelection =
    if (exerciseId == null) {
        StatisticsSelection()
    } else {
        StatisticsSelection(
            metric = MetricKey.Exercise(ExerciseTrendMetric.ESTIMATED_1RM),
            exerciseId = exerciseId,
        )
    }

/** The metric the picker is on, and the lift when that metric needs one (ROADMAP N35). */
data class StatisticsSelection(
    /** Bodyweight, because it is the series a person checks most often. */
    val metric: MetricKey = MetricKey.Body(BodyMetric.WEIGHT),
    val exerciseId: String? = null,
)

/** The separator between a stored metric id and its lift: neither id contains it (N212, B226). */
internal const val RECENT_LIFT_SEPARATOR = "|"

/** The stable name of a recent selection, for its test tag and its stored entry (B226). */
internal fun StatisticsSelection.recentTag(): String =
    metric.id + (exerciseId?.let { RECENT_LIFT_SEPARATOR + it }.orEmpty())

/** What the Statistics screen shows (ROADMAP N35). */
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val range: StatisticsRange = StatisticsRange(),
    val selection: StatisticsSelection = StatisticsSelection(),
    /**
     * The series read most recently, newest first, at most three (N212).
     *
     * A selection, not a metric: an Exercise series is meaningless without its lift, and storing the id alone
     * made two lifts' volume one chip and restored whichever lift happened to be on screen (B226). The current
     * selection is in the list, so the row can show which one is on screen; the row is hidden while there is
     * fewer than two, because one chip repeating the picker's own label is not a shortcut.
     */
    val recentSeries: List<StatisticsSelection> = emptyList(),
    val overview: StatisticsOverview = StatisticsOverview(),
    val series: MetricSeries? = null,
    /** The read failed, shown in place of the chart (the same shape the trends screen uses). */
    val error: DataError? = null,
    /** A range write failed; shown as a snackbar rather than replacing the chart (B229). */
    val rangeError: DataError? = null,
    /** The library, for choosing the lift an Exercise metric is about. */
    val lifts: List<Exercise> = emptyList(),
    /** The target set for the chosen metric, in its own units, or null when there is none (ROADMAP N39). */
    val goal: Double? = null,
    /**
     * The chosen metric's goal, when it is the weight one (ROADMAP N135).
     *
     * The weight goal is the only one with a **kind** — a target to reach, a direction with no target, or a
     * weight to hold within a corridor — and the kind is what sets the rate below. Every other metric keeps
     * [goal] alone.
     */
    val weightGoal: WeightGoal? = null,
    /**
     * The chosen metric's goal **periods**, oldest first, so the chart can draw each and the history can say
     * what became of it (ROADMAP N137).
     */
    val goalPeriods: List<GoalPeriod> = emptyList(),
    /**
     * The **whole** series the goal history is judged from, unwindowed (ROADMAP N137, B141).
     *
     * The chart's own [series] is clipped to the range chips, and a period's verdict must not be: a target is
     * met when a reading **in the period** reached it, so judging from the chart's window made the verdict a
     * function of a display setting — and on the default seven-day range a rate could never clear N98's floor
     * at all. Empty for a metric that reads no lift; the history is only drawn for the weight goal.
     */
    val goalReadings: List<MetricReading> = emptyList(),
    /**
     * What the lifter's **plans** prescribe for the chosen lift, one entry per plan that names it (N39).
     *
     * Empty for a metric that reads no lift: a plan prescribes sets, not a bodyweight.
     */
    val planTargets: List<PlanTarget> = emptyList(),
    /**
     * The rate the goal asks for, in the metric's own units per week, or null (ROADMAP N98, N135).
     *
     * **Derived, never typed.** N135: the goal and the weight the lifter is now are what set it, so the
     * screen shows it rather than offering it for editing — two numbers that can disagree were the thing
     * being fixed.
     */
    val rateTarget: Double? = null,
    /**
     * The band the goal's fitted trend is judged against, signed for its direction, or null (N149).
     *
     * The target is the number the energy adjustment works from; this is the range that says whether the trend
     * is *on* it. A loss goal's band is negative at both ends, so it reads as one interval rather than a
     * magnitude the screen has to re-sign.
     */
    val rateBand: ClosedRange<Double>? = null,
    /**
     * The date a single Target goal's rate implies, or null (N151).
     *
     * A reached target has no date; a direction or maintenance goal has no level to reach, so both are null.
     */
    val targetDate: LocalDate? = null,
    /** True when a Target goal's weight is already within the goal history's tolerance (N151). */
    val targetReached: Boolean = false,
    /**
     * Whether the chosen metric reads a head as a family (ROADMAP N97, B98, N110).
     *
     * The picker draws its family label from this rather than from the row's shape alone: a **movement** is
     * offered under every metric and reads as the one lift it is, while a **category** is offered only where
     * the metric reads a family.
     */
    val acceptsHead: Boolean = false,
) {
    /**
     * True when the chosen metric needs a lift and none is chosen yet.
     *
     * The screen asks rather than drawing nothing: an empty chart and a question look different, and only
     * one of them is true.
     */
    val needsExercise: Boolean
        get() = metricNeedsExercise && selection.exerciseId == null

    /**
     * True when the chosen metric is about a lift at all.
     *
     * Separate from [needsExercise] because the two drive different things, and conflating them made the
     * picker unreachable: the screen gated the *picker* on "nothing is chosen yet", so the only way to change
     * lift was to arrive with none selected — and every entry point selects one. This is "the metric needs a
     * lift"; that is "and there is not one yet", which is about the prompt instead.
     */
    val metricNeedsExercise: Boolean
        get() = MetricRegistry.entryFor(selection.metric).needsExercise
}

/**
 * Everything the Statistics screen reads (ROADMAP N35).
 *
 * One screen over three sources, which is the point of the round: the registry says how to show each of the
 * twenty-one series, `metricSeries` says how to read one, and this says which of them to read and for when.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val goals: GoalRepository,
    private val repositories: StatisticsRepositories,
    private val timeSource: TimeSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** One recents write at a time, so two quick choices cannot both read the same list (N212). */
    private val recentWrites = Mutex()

    /**
     * The window a pinch is still changing (N213).
     *
     * A pinch reports a scale per motion event; writing the settings file on each one would put dozens of
     * commits on disk for one gesture. The pending window is what the screen draws, and it is written once,
     * shortly after the fingers stop — or dropped if a range chip is chosen while it waits.
     */
    private val pendingRange = MutableStateFlow<StatisticsRange?>(null)

    /** The last range write's failure, consumed by the screen's snackbar (B229). */
    private val rangeError = MutableStateFlow<DataError?>(null)
    private var pinchWrite: Job? = null

    private val selection = MutableStateFlow(
        initialSelection(savedStateHandle.toRoute<Statistics>().exerciseId),
    )

    /**
     * The range the screen is showing.
     *
     * From the store rather than from local state, so a range chosen here is the range it comes back to —
     * and so the screen and the store cannot disagree about which one is current.
     */
    private val range = combine(
        settings.observeStatisticsRange(),
        pendingRange,
    ) { stored, pending -> pending ?: stored }

    /** The four sources, gathered before they are combined: five flows is the typed combine's limit. */
    private data class Sources(
        val range: StatisticsRange,
        val selection: StatisticsSelection,
        val recentSeries: List<StatisticsSelection>,
        val rangeError: DataError?,
        val summaries: List<WorkoutSummary>,
        val workoutPoints: DataResult<List<TrendPoint>>,
        val measurements: List<BodyMeasurement>,
        val lifts: List<Exercise>,
        /** Whether the chosen metric reads a head as a family (ROADMAP N97, B98). */
        val acceptsHead: Boolean,
    )

    /**
     * The two reads that are not a series: the workouts the overview totals, and the library the lift picker
     * offers. Combined first because the typed `combine` stops at five flows and this is the pair that has
     * nothing to do with the selected metric.
     */
    private val library = combine(
        repositories.statistics.observeWorkoutSummaries(),
        repositories.exercises.observeExercises(),
    ) { summaries, result ->
        summaries to (result as? DataResult.Success)?.data.orEmpty()
    }

    /**
     * The series read recently, as selections (N212, B226).
     *
     * The store holds one entry per selection: the metric's id, then the lift's id after [RECENT_LIFT_SEPARATOR]
     * where the metric reads one. An entry this build's registry does not know is dropped rather than shown as a
     * chip that would select nothing, a lift-less Exercise entry (the old N212 format) is dropped for the same
     * reason, and duplicates or an over-long store are pruned here.
     */
    private val recents: Flow<List<StatisticsSelection>> = settings.observeRecentSeries().map { entries ->
        entries.mapNotNull(::recentSelection).distinct().take(RECENT_SERIES_MAX)
    }

    /** The selection, the recents and the last write's failure, so the five-flow combine keeps its five. */
    private val chosen = combine(selection, recents, rangeError) { selection, recent, failed ->
        Triple(selection, recent, failed)
    }

    private val sources = combine(
        range,
        chosen,
        library,
        repositories.trends.observeTrends(NO_LIMIT),
        repositories.measurements.observeAll(),
    ) { range, chosen, library, points, body ->
        val selection = chosen.first
        val acceptsHead = MetricRegistry.entryFor(selection.metric).acceptsHead
        val rows = library.second
        Sources(
            range = range,
            selection = selection,
            recentSeries = chosen.second,
            rangeError = chosen.third,
            summaries = library.first,
            // A head is offered only where the chosen metric **reads** one (ROADMAP N97, B98), and only where
            // it actually heads something: a category a lifter has made and not yet filed anything under is
            // not a family, and choosing it would draw an empty chart under a label promising one. For the
            // rest — heaviest set, estimated 1RM, assistance — the rule N95 set still stands, because a
            // category is never named by a set. This is the one list where a head may be chosen at all, a
            // read-view exception rather than a repeal: the pickers that log keep their filter.
            lifts = if (acceptsHead) {
                rows.filter { row -> row.rowKind.isLoggable || rows.any { child -> child.parentId == row.id } }
            } else {
                rows.filter { it.rowKind.isLoggable }
            },
            acceptsHead = acceptsHead,
            workoutPoints = points,
            measurements = body,
        )
    }

    /**
     * The chosen lift's series, or nothing when the metric does not need one.
     *
     * The library is combined in because the *subject* is a question about the library's shape rather than
     * about the selection: a head reads its own ids plus everything filed under it (ROADMAP N97).
     */
    private val exercisePoints = combine(selection, library) { chosen, lib -> chosen to lib.second }
        .flatMapLatest { (chosen, exercises) ->
            val metric = chosen.metric
            val exerciseId = chosen.exerciseId
            // Asked of the registry rather than of the key's type: a metric that needs a lift is exactly
            // the set whose series comes from the per-exercise read, and that now includes the per-site
            // joint series (N130) — a type test here silently left it drawing nothing.
            if (MetricRegistry.entryFor(metric).needsExercise && exerciseId != null) {
                repositories.trends.observeExerciseTrends(
                    subjectIds(metric, exerciseId, exercises),
                    NO_LIMIT,
                )
            } else {
                flowOf(DataResult.Success(emptyList()))
            }
        }

    /**
     * The ids one chosen lift's series reads (ROADMAP N97).
     *
     * A head reads its whole family only for a metric that takes one; every other metric — and every row
     * that heads nothing, which is most of them — reads the row that was chosen, exactly as it did before.
     */
    private fun subjectIds(
        metric: MetricKey,
        exerciseId: String,
        library: List<Exercise>,
    ): List<String> {
        val chosen = library.firstOrNull { it.id == exerciseId } ?: return listOf(exerciseId)
        return if (MetricRegistry.entryFor(metric).acceptsHead) {
            chosen.seriesSubjectIds(library)
        } else {
            listOf(exerciseId)
        }
    }

    /** How many records the window contains. Its own flow because it is a suspend read, not a stream. */
    private val records = settings.observeStatisticsRange().mapLatest { current ->
        // The settled range, not the pinch's pending one: a gesture must not issue a database count per
        // motion event, and the overview is a summary of the window that was chosen (B236).
        // "All" has no window, and the count is still a question worth answering — it is the range where a
        // lifetime record count means most. A null window means unbounded, which the query expresses as the
        // ends of the Long range rather than as a missing clause.
        val bounds = current.window(today(), currentZone())
        val from = bounds?.from ?: Instant.ofEpochMilli(Long.MIN_VALUE)
        val to = bounds?.toExclusive ?: Instant.ofEpochMilli(Long.MAX_VALUE)
        (repositories.statistics.countRecordsIn(from, to) as? DataResult.Success)?.data
    }

    /** The goal that is open and the periods behind it, as one source (ROADMAP N135, N137). */
    private val goalHistory: Flow<Pair<Map<String, GoalPeriod>, List<GoalPeriod>>> = combine(
        goals.observeCurrent(),
        selection.flatMapLatest { chosen -> goals.observePeriods(chosen.metric.id) },
    ) { current, periods -> current to periods }

    /**
     * What the plans prescribe for the chosen lift (ROADMAP N39).
     *
     * Read only where the metric reads a **lift**: a plan prescribes sets, so there is nothing for a bodyweight
     * or a tape site to answer. The ids are **the same ones the series reads** ([subjectIds], N97, B143): a
     * metric that accepts a head draws its whole family's points, so its plan lines have to be read over that
     * family too — a set never names a category, and querying the chosen id alone drew the family's readings
     * with no plan line at all. `distinctUntilChanged` on the list so switching between two metrics about the
     * same lift does not re-query the plan's tables.
     */
    private val planTargets: Flow<List<PlanTarget>> =
        combine(selection, library) { chosen, lib -> chosen to lib.second }
            .map { (chosen, exercises) ->
                val exerciseId = chosen.exerciseId
                if (exerciseId == null || !MetricRegistry.entryFor(chosen.metric).needsExercise) {
                    emptyList()
                } else {
                    subjectIds(chosen.metric, exerciseId, exercises)
                }
            }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyList()) else repositories.statistics.observePlanTargets(ids)
            }

    val uiState: StateFlow<StatisticsUiState> =
        combine(
            sources,
            exercisePoints,
            records,
            goalHistory,
            planTargets,
        ) { sources, points, records, goal, plans ->
            val (goalPeriods, periods) = goal
            val workoutPoints = sources.workoutPoints
            // The period that is **open** for this metric (N137), and the two readings the screen makes of it:
            // the weight goal, with the kind N135 gave it, and the level a chart can draw. A direction-only
            // goal deliberately states no weight, so the app never manufactures an end weight the lifter did
            // not state.
            val current = goalPeriods[sources.selection.metric.id]
            val weightGoal = current?.weightGoal
                ?.takeIf { sources.selection.metric == MetricKey.Body(BodyMetric.WEIGHT) }
            val levelGoal = if (sources.selection.metric == MetricKey.Body(BodyMetric.WEIGHT)) {
                weightGoal?.statedWeightGrams()?.toDouble()
            } else {
                current?.value
            }
            if (workoutPoints is DataResult.Failure) {
                StatisticsUiState(
                    isLoading = false,
                    range = sources.range,
                    selection = sources.selection,
                    recentSeries = sources.recentSeries,
                    error = workoutPoints.error,
                    rangeError = sources.rangeError,
                    lifts = sources.lifts,
                    goal = levelGoal,
                    weightGoal = weightGoal,
                    goalPeriods = periods,
                    planTargets = plans,
                    acceptsHead = sources.acceptsHead,
                )
            } else {
                val allReadings = metricSeries(
                    key = sources.selection.metric,
                    workouts = (workoutPoints as DataResult.Success).data,
                    exercises = (points as? DataResult.Success)?.data.orEmpty(),
                    measurements = sources.measurements,
                ).readings
                val body = sources.range.inWindow(
                    readings = allReadings,
                    today = today(),
                    zone = currentZone(),
                )
                // The current weight is the newest reading in the **whole** series rather than the windowed
                // one: the rate is a share of the body now, and a range that excluded the latest weigh-in
                // could not state it (N135).
                val currentWeightGrams = allReadings.mapNotNull { it.value }.lastOrNull()?.toLong()
                StatisticsUiState(
                    isLoading = false,
                    range = sources.range,
                    selection = sources.selection,
                    recentSeries = sources.recentSeries,
                    rangeError = sources.rangeError,
                    overview = statisticsOverview(
                        range = sources.range,
                        today = today(),
                        sessions = sources.summaries,
                        zone = currentZone(),
                        personalRecords = records,
                    ),
                    series = MetricSeries(key = sources.selection.metric, readings = body),
                    lifts = sources.lifts,
                    goal = levelGoal,
                    weightGoal = weightGoal,
                    goalPeriods = periods,
                    // Unwindowed, beside the windowed `series`: the history judges each period against the
                    // readings **in that period**, not against what the range chips happen to show (B141).
                    goalReadings = allReadings,
                    planTargets = plans,
                    rateTarget = weightGoal?.ratePerWeekGrams(currentWeightGrams ?: 0L),
                    rateBand = weightGoal?.rateBandPerWeekGrams(currentWeightGrams ?: 0L),
                    targetDate = weightGoal?.targetDateEstimate(currentWeightGrams ?: 0L, today()),
                    targetReached = weightGoal?.isTargetReached(currentWeightGrams ?: 0L) == true,
                    acceptsHead = sources.acceptsHead,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StatisticsUiState(),
        )

    /**
     * Sets or clears the chosen metric's target (ROADMAP N39).
     *
     * Written straight through rather than buffered: a target is one number the user typed and expects to be
     * remembered, so a failure has to be visible — the rule every other write in this app follows.
     */
    fun onSetGoal(value: Double?) {
        viewModelScope.launch { goals.setLevelGoal(selection.value.metric.id, value) }
    }

    /**
     * Chooses the lift an Exercise metric reads (N110, B226).
     *
     * It records the selection: choosing the metric alone is not enough to make a per-lift series, and a row
     * that remembered only the metric could not come back to this lift.
     */
    fun onSelectExercise(exerciseId: String?) {
        val next = selection.value.copy(exerciseId = exerciseId)
        selection.value = next
        rememberSelection(next)
    }

    /**
     * Chooses the metric, dropping a selection it can no longer offer (ROADMAP B98).
     *
     * A **category** is offered only under the metrics that read a head, so switching to one that reads a
     * single id would otherwise leave the category selected behind a picker that no longer lists it — the
     * picker reading "Choose a lift" while the empty chart behind it still read the hidden id. A movement
     * keeps its place under every metric (N110): it heads nothing, and the ones that read one id read it as
     * the single lift it is. The library is read from the state rather than re-queried, because the row's
     * kind is the whole question and it cannot change with the metric.
     *
     * An Exercise metric with no lift yet is not recorded: the lift picker is the next step, and it records the
     * series when it is answered (B226).
     */
    fun onSelectMetric(metric: MetricKey) {
        val chosen = uiState.value.lifts.firstOrNull { it.id == selection.value.exerciseId }
        val offered = MetricRegistry.entryFor(metric).acceptsHead || chosen?.rowKind != RowKind.CATEGORY
        val next = selection.value.copy(
            metric = metric,
            exerciseId = selection.value.exerciseId.takeIf { offered },
        )
        selection.value = next
        rememberSelection(next)
    }

    /**
     * Restores a series from the recents row (N212, B226).
     *
     * The whole selection travels, so an Exercise chip returns to its own lift. A lift this build no longer
     * holds is dropped rather than selected invisibly, and the category rule is the picker's: a metric that
     * reads one id does not keep a category head.
     */
    fun onSelectRecent(recent: StatisticsSelection) {
        val entry = MetricRegistry.entryFor(recent.metric)
        val chosen = uiState.value.lifts.firstOrNull { it.id == recent.exerciseId }
        val offered = entry.acceptsHead || chosen?.rowKind != RowKind.CATEGORY
        val next = recent.copy(
            exerciseId = recent.exerciseId.takeIf { entry.needsExercise && offered },
        )
        selection.value = next
        rememberSelection(next)
    }

    /**
     * Records a series as just read, newest first and at most [RECENT_SERIES_MAX] (N212, B226).
     *
     * Only an explicit choice records: opening the tab on its default series is not "using" it, and writing
     * that on every visit would push the three actually read out of the row. An Exercise metric without its
     * lift is not a series yet, so it waits for the picker. Writes are serialised because two quick choices
     * would otherwise both read the same list and the second would drop the first.
     */
    private fun rememberSelection(selection: StatisticsSelection) {
        val entry = MetricRegistry.entryFor(selection.metric)
        if (entry.needsExercise && selection.exerciseId == null) return
        val encoded = recentEntry(selection)
        viewModelScope.launch {
            recentWrites.withLock {
                val current = settings.observeRecentSeries().first()
                val next = (listOf(encoded) + current.filterNot { it == encoded })
                    .take(RECENT_SERIES_MAX)
                if (next != current) settings.setRecentSeries(next)
            }
        }
    }

    /** The stored form of one selection: the metric's id, and the lift's after the separator (B226). */
    private fun recentEntry(selection: StatisticsSelection): String {
        val needsExercise = MetricRegistry.entryFor(selection.metric).needsExercise
        return selection.metric.id +
            (selection.exerciseId?.takeIf { needsExercise }?.let { RECENT_LIFT_SEPARATOR + it }.orEmpty())
    }

    /** The selection an entry names, or null when this build cannot offer it or it has no lift where one is asked. */
    private fun recentSelection(entry: String): StatisticsSelection? {
        val parts = entry.split(RECENT_LIFT_SEPARATOR, limit = 2)
        val metric = MetricRegistry.entries.firstOrNull { it.key.id == parts[0] }?.key ?: return null
        val needsExercise = MetricRegistry.entryFor(metric).needsExercise
        val exerciseId = parts.getOrNull(1)?.takeIf { needsExercise && it.isNotBlank() }
        if (needsExercise && exerciseId == null) return null
        return StatisticsSelection(metric = metric, exerciseId = exerciseId)
    }

    /** Stored, not held: the range has to mean the same thing the next time the tab is opened. */
    fun onSelectRange(range: StatisticsRange) {
        // A chip overrules a pinch that is still settling; otherwise the pending write would land after it.
        pinchWrite?.cancel()
        pendingRange.value = null
        viewModelScope.launch {
            val result = settings.setStatisticsRange(range)
            if (result is DataResult.Failure) rangeError.value = result.error
        }
    }

    /** Takes the range-write failure away once its snackbar has been read (B229). */
    fun onDismissRangeError() {
        rangeError.value = null
    }

    /**
     * Pinches the window around [focus] (N213).
     *
     * The gesture reports a scale per motion event; the window is computed from the range on screen and held
     * pending until the fingers settle, so one gesture is one settings write rather than dozens.
     */
    fun onPinch(scale: Float, focus: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        val next = pinchedWindow(uiState.value, scale, focus) ?: return
        pendingRange.value = next
        pinchWrite?.cancel()
        pinchWrite = viewModelScope.launch {
            delay(PINCH_SETTLE_MILLIS)
            val settled = pendingRange.value ?: return@launch
            when (val result = settings.setStatisticsRange(settled)) {
                is DataResult.Success -> {
                    // Keep the pending window until the preferences listener reports it: clearing first lets
                    // the stored old range paint for a frame before the new one arrives (B229).
                    withTimeoutOrNull(RANGE_SETTLE_WAIT_MILLIS) {
                        settings.observeStatisticsRange().first { it == settled }
                    }
                    pendingRange.value = null
                }

                is DataResult.Failure -> {
                    pendingRange.value = null
                    rangeError.value = result.error
                }
            }
        }
    }

    /**
     * The window a pinch asks for, or null when it would change nothing (N213).
     *
     * Whole days around the pinched point, clamped inside the **recorded readings** and never narrower than
     * [MIN_PINCH_DAYS]. A pinch becomes a custom range, because a window the finger chose has no rolling name.
     */
    private fun pinchedWindow(state: StatisticsUiState, scale: Float, focus: Float): StatisticsRange? {
        val zone = currentZone()
        val today = today()
        // The window the gesture is building, read synchronously: a scale is reported since the previous
        // motion event, and `uiState` catches up only after three flow hops, so reading it lost every
        // increment delivered before the collector ran (B228).
        val base = pendingRange.value ?: state.range
        val plotted = base.inWindow(state.goalReadings, today, zone)
        if (plotted.count { it.value != null } < 2) return null

        val firstPlot = plotted.first().at
        val lastPlot = plotted.last().at
        val firstPlotDay = firstPlot.atZone(zone).toLocalDate()
        val lastPlotDay = lastPlot.atZone(zone).toLocalDate()
        val plottedSpan = (ChronoUnit.DAYS.between(firstPlotDay, lastPlotDay).toInt() + 1).coerceAtLeast(1)

        val days = state.goalReadings
            .mapNotNull { reading -> reading.value?.let { reading.at.atZone(zone).toLocalDate() } }
            .distinct()
        if (days.size < 2) return null
        val firstDay = days.min()
        val lastDay = days.max()
        val maxSpan = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1

        val bounds = base.window(today, zone)
        val baseFrom = bounds?.from?.atZone(zone)?.toLocalDate()
        val baseTo = bounds?.toExclusive?.minusMillis(1)?.atZone(zone)?.toLocalDate()
        val baseSpan = if (baseFrom != null && baseTo != null) {
            ChronoUnit.DAYS.between(baseFrom, baseTo).toInt() + 1
        } else {
            maxSpan
        }
        // The focus is a fraction of the **plotted** domain, because that is the axis the finger is on; the old
        // range-window mapping anchored a centre pinch two days away from the reading under it (B227). A
        // pinch-out scales the window it is opening (never below the window already shown); a pinch-in scales
        // the span the data actually fills, which is what the eye is being asked to magnify.
        val focusOnPlot = focus.coerceIn(0f, 1f)
        val focusMillis = firstPlot.toEpochMilli() +
            ((lastPlot.toEpochMilli() - firstPlot.toEpochMilli()) * focusOnPlot).toLong()
        val focusDay = Instant.ofEpochMilli(focusMillis).atZone(zone).toLocalDate()
        val domainSpan = if (scale < 1f) maxOf(plottedSpan, baseSpan) else plottedSpan
        val newSpan = (domainSpan / scale).roundToInt().coerceIn(MIN_PINCH_DAYS, maxSpan)
        val start = focusDay
            .minusDays((focusOnPlot * (newSpan - 1)).roundToInt().toLong())
            .coerceAtLeast(firstDay)
            .coerceAtMost(lastDay.minusDays(newSpan - 1L))
        val candidate = StatisticsRange(RangeKind.CUSTOM, from = start, to = start.plusDays(newSpan - 1L))
        // The window a finger chose still has to be one the figure can draw; a pinch that leaves a single
        // reading would empty the card and leave the range row saying Custom (B227).
        if (plotted.count { it.value != null } >= 2 &&
            candidate.inWindow(state.goalReadings, today, zone).count { it.value != null } < 2
        ) {
            return null
        }
        return candidate.takeUnless { start == baseFrom && candidate.to == baseTo }
    }

    private fun today(): LocalDate =
        Instant.ofEpochMilli(timeSource.nowEpochMillis()).atZone(currentZone()).toLocalDate()

    private companion object {
        /** How many series the recent row holds: enough for the habitual three, not a second menu (N212). */
        const val RECENT_SERIES_MAX = 3

        /** The narrowest window a pinch may leave: one day cannot show a change (N213). */
        const val MIN_PINCH_DAYS = 2

        /** How long after the last pinch motion the window is written, once (N213). */
        const val PINCH_SETTLE_MILLIS = 250L

        /** How long to wait for the stored range to report a settled pinch before clearing the pending one (B229). */
        const val RANGE_SETTLE_WAIT_MILLIS = 500L

        /**
         * How many sessions the training series are asked for: no limit.
         *
         * The trends queries window by session, not by time, so a range is honoured by filtering the readings
         * by date in memory. Asking for a *fixed number* and then filtering was silently wrong: with more
         * finished sessions than the cap, a long range lost the older sessions inside it — including from the
         * record count, which is a headline number. The cap was justified by what a chart can draw legibly,
         * but the chart draws a subset anyway and the readings list is meant to be read.
         *
         * `Int.MAX_VALUE` rather than removing the parameter: `LIMIT` is part of the query contract, and the
         * alternative is a second query shape to keep in step.
         */
        const val NO_LIMIT = Int.MAX_VALUE
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /**
         * The zone "today" is measured in, read when it is asked for.
         *
         * A function rather than a `val`: a companion-object `val` is evaluated once at class load, so a
         * process that outlives a timezone change would keep computing today's date in the old zone. See
         * DECISIONS.md — a range is a window in the current zone.
         */
        fun currentZone(): ZoneId = ZoneId.systemDefault()
    }
}
