package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.headsOthers
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.RangeKind
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.DatePrompt
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.chart.SeriesChart
import com.example.androidapp.ui.theme.EyebrowStyle
import java.time.LocalDate
import java.time.ZoneId

/** The Statistics tab (ROADMAP N35). */
@Composable
fun StatisticsRoute(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
    onOpenMeasurements: () -> Unit = {},
    onOpenAdherence: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        state = state,
        onSelectRange = viewModel::onSelectRange,
        onSelectMetric = viewModel::onSelectMetric,
        modifier = modifier,
        onSelectExercise = viewModel::onSelectExercise,
        onSetGoal = viewModel::onSetGoal,
        onOpenMeasurements = onOpenMeasurements,
        onOpenAdherence = onOpenAdherence,
    )
}

/**
 * One chart with a picker over every series, the range it covers, and three numbers about it.
 *
 * A tab root, so it has no back arrow: the shell's bar is the way out, and a root that offered "back" would
 * be offering to go somewhere the user did not come from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onSelectRange: (StatisticsRange) -> Unit,
    onSelectMetric: (MetricKey) -> Unit,
    modifier: Modifier = Modifier,
    onSelectExercise: (String) -> Unit = {},
    /** Sets or clears the chosen metric's target (ROADMAP N39). */
    onSetGoal: (Double?) -> Unit = {},
    /**
     * Opens body measurements (ROADMAP N35).
     *
     * Their charts are picker entries now, but **recording** an entry is a different job from reading a
     * trend, so the screen stays a pushed destination — and this action is what keeps it reachable, which
     * it stopped being the moment the trends screen that used to link to it was deleted.
     */
    onOpenMeasurements: (() -> Unit)? = null,
    /**
     * Opens adherence (ROADMAP P3.5).
     *
     * The aggregate over the session-level plan-versus-actual: how often the days a program
     * scheduled actually happened, and a month of days trained. A pushed destination for the same
     * reason measurements is one — it answers a question this tab raises, and it is not a tab.
     */
    onOpenAdherence: (() -> Unit)? = null,
) {
    var choosingDates by remember { mutableStateOf(false) }
    if (choosingDates) {
        CustomRangeDialog(
            initial = state.range,
            onDismiss = { choosingDates = false },
            onConfirm = { chosen ->
                choosingDates = false
                onSelectRange(chosen)
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            StatisticsTopBar(
                onOpenMeasurements = onOpenMeasurements,
                onOpenAdherence = onOpenAdherence,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RangeControl(
                range = state.range,
                onSelectRange = onSelectRange,
                onChooseDates = { choosingDates = true },
            )
            RecentSeries(state = state, onSelectMetric = onSelectMetric)
            MetricPicker(selected = state.selection.metric, onSelectMetric = onSelectMetric)

            // The overview and the chart both read the series, so neither is drawn until it exists: a frame
            // that printed "0 workouts · 0 kg" while loading was making a claim about data it had not read
            // yet, and the same zeros appeared behind a failed read with no message at all.
            when {
                state.isLoading -> CenteredMessage(
                    text = stringResource(R.string.statistics_loading),
                    showSpinner = true,
                )

                state.error != null -> CenteredMessage(text = dataErrorMessage(state.error))

                else -> StatisticsBody(
                    state = state,
                    series = state.series,
                    onSelectExercise = onSelectExercise,
                    onSetGoal = onSetGoal,
                )
            }
        }
    }
}

/**
 * What the screen shows once the series has been read.
 *
 * Split out so the loading and error frames above are a decision about the *state* rather than a set of
 * conditions threaded through the body.
 */
@Composable
private fun StatisticsBody(
    state: StatisticsUiState,
    series: MetricSeries?,
    onSelectExercise: (String) -> Unit,
    onSetGoal: (Double?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // The picker is drawn whenever the metric is about a lift, not only when nothing is chosen yet.
        // Gating it on "no lift selected" made it unreachable: every entry point arrives with one selected,
        // so the lift could never be changed, and the picker's own selected-name branch was dead code.
        if (state.metricNeedsExercise) {
            if (state.needsExercise) {
                Text(
                    text = stringResource(R.string.statistics_choose_lift),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TestTags.Statistics.CHOOSE_LIFT),
                )
            }
            LiftPicker(
                lifts = state.lifts,
                selectedId = state.selection.exerciseId,
                acceptsHead = state.acceptsHead,
                onSelectExercise = onSelectExercise,
            )
        }

        series?.let { series ->
            val entry = MetricRegistry.entryFor(series.key)
            SeriesChart(
                series = series,
                metric = entry,
                goalPeriods = state.goalPeriods,
                planTargets = state.planTargets,
            )
            // Under the chart, and the literal answer to "see everything" — which is also the accessible
            // counterpart to a canvas this app blanks out for screen readers (ROADMAP N36).
            ReadingsSection(series = series, metric = entry)
            // A weight goal is set on the measurements screen now, where the body is recorded (a request in
            // use; no roadmap id covers the move) — the chart still draws it from the goal's periods.
            if (entry.key != MetricKey.Body(BodyMetric.WEIGHT)) {
                GoalRow(goal = state.goal, metric = entry, onSetGoal = onSetGoal)
            }
            // Under the goal, because the rate is now *derived* from it rather than typed beside it
            // (N135): it is the same fact read at the speed the goal asks for.
            EnergyAdjustmentSection(
                // The statement is a verdict, not a drawing: it reads the whole series so the chart's range
                // (seven days by default) cannot silence it below N98's 21-day floor (B141).
                series = MetricSeries(key = series.key, readings = state.goalReadings),
                metric = entry,
                weightGoal = state.weightGoal,
                rateTarget = state.rateTarget,
                rateBand = state.rateBand,
                targetDate = state.targetDate,
                targetReached = state.targetReached,
            )
        }
        // The three numbers sit after the figure rather than above it (N208): they summarise the window, and the
        // window is what the chart is for — a card between the metric and the chart is a row the figure pays for.
        Overview(overview = state.overview)
    }
}

/**
 * The two or three series read lately, one tap from the chart (N212).
 *
 * Hidden while there are fewer than two: one chip repeating the picker's own label is not a shortcut, and a
 * row that appears before there is anything in it is a control that promises a memory it does not have. The
 * current series is one of the chips and reads as selected, so the row also says which one is on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSeries(state: StatisticsUiState, onSelectMetric: (MetricKey) -> Unit) {
    if (state.recentSeries.size < 2) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.recentSeries.forEach { key ->
            AppFilterChip(
                selected = key == state.selection.metric,
                onClick = { onSelectMetric(key) },
                label = MetricRegistry.entryFor(key).label(),
                testTag = TestTags.Statistics.recentSeries(key.id),
            )
        }
    }
}

/**
 * The ranges, as one row and one menu (N197).
 *
 * Seven chips wrapped three to a row spent two rows of the screen on a control that is used once a visit, and
 * every one of them was a button of equal weight. The three a thumb reaches for stay out; the rest — six months,
 * a year, all, custom — sit behind one *More* control, which takes the chosen range's own name while it is
 * active, so the screen still says which window is on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeControl(
    range: StatisticsRange,
    onSelectRange: (StatisticsRange) -> Unit,
    onChooseDates: () -> Unit,
) {
    val overflow = RangeKind.entries.filterNot { it in COMMON_RANGES }
    val selectedOverflow = range.kind.takeIf { it in overflow }
    var open by remember { mutableStateOf(false) }

    // A wrapping row rather than a plain one: four controls fit one row on a phone, and a device that cannot is
    // better with a second row than with a clipped control.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        COMMON_RANGES.forEach { kind ->
            RangeChip(kind = kind, selected = range.kind == kind, onSelectRange = onSelectRange)
        }
        Box {
            AppFilterChip(
                selected = selectedOverflow != null,
                onClick = { open = true },
                label = stringResource(selectedOverflow?.labelRes ?: R.string.range_more),
                testTag = TestTags.Statistics.RANGE_MORE,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                overflow.forEach { kind ->
                    DropdownMenuItem(
                        text = { Text(stringResource(kind.labelRes)) },
                        onClick = {
                            open = false
                            // Custom asks its dates before it applies: an unbounded window would make the
                            // chart look broken rather than empty.
                            if (kind == RangeKind.CUSTOM) {
                                onChooseDates()
                            } else {
                                onSelectRange(StatisticsRange(kind))
                            }
                        },
                        modifier = Modifier.testTag(TestTags.Statistics.range(kind.name)),
                    )
                }
            }
        }
    }
}

/** The ranges a thumb reaches for; the rest sit behind one control (N197). */
private val COMMON_RANGES = listOf(
    RangeKind.LAST_7_DAYS,
    RangeKind.LAST_MONTH,
    RangeKind.LAST_3_MONTHS,
)

@Composable
private fun RangeChip(kind: RangeKind, selected: Boolean, onSelectRange: (StatisticsRange) -> Unit) {
    AppFilterChip(
        selected = selected,
        onClick = { onSelectRange(StatisticsRange(kind)) },
        // The short name: "Last" is understood from the control, and dropping it is what keeps four controls on
        // one row. The menu behind *More* keeps the full names, where there is room for them.
        label = stringResource(
            when (kind) {
                RangeKind.LAST_7_DAYS -> R.string.range_7_days_short
                RangeKind.LAST_MONTH -> R.string.range_month_short
                RangeKind.LAST_3_MONTHS -> R.string.range_3_months_short
                else -> kind.labelRes
            },
        ),
        testTag = TestTags.Statistics.range(kind.name),
    )
}

/** The three numbers, and no more (ROADMAP N35). */
@Composable
private fun Overview(overview: StatisticsOverview) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Number(
                labelRes = R.string.statistics_overview_workouts,
                value = overview.workouts.toString(),
                testTag = TestTags.Statistics.OVERVIEW_WORKOUTS,
            )
            Number(
                labelRes = R.string.statistics_overview_volume,
                value = Weight.kilograms(overview.volumeGrams),
                testTag = TestTags.Statistics.OVERVIEW_VOLUME,
            )
            Number(
                labelRes = R.string.statistics_overview_records,
                // A dash rather than a zero: "not counted yet" and "you set none" are different statements
                // (see StatisticsOverview.personalRecords).
                value = overview.personalRecords?.toString() ?: stringResource(R.string.statistics_not_counted),
                testTag = TestTags.Statistics.OVERVIEW_RECORDS,
            )
        }
    }
}

@Composable
private fun Number(labelRes: Int, value: String, testTag: String) {
    Column {
        // The label is tracked out small caps and the value is large: the pair reads as a
        // figure with a caption rather than two lines of the same sentence, which is what makes
        // three numbers side by side scannable instead of merely present.
        Text(
            text = stringResource(labelRes).uppercase(LocalConfiguration.current.locales[0]),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 2.dp)
                .testTag(testTag),
        )
    }
}

/**
 * The readings, newest first, with the average on top (ROADMAP N36).
 *
 * Collapsed by default: the chart is what the screen is for, and a year of readings is a wall of numbers to
 * anyone who has not asked for one. Nothing at all when there is nothing recorded — a disclosure control
 * that opens onto emptiness is worse than no control.
 */
@Composable
private fun ReadingsSection(series: MetricSeries, metric: MetricEntry) {
    val readings = series.asReadings()
    if (readings.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        AppTextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag(TestTags.Statistics.READINGS_TOGGLE),
        ) {
            Text(stringResource(R.string.statistics_readings, readings.size))
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                // The label beside it already says what this is; a description would make TalkBack repeat it.
                contentDescription = null,
            )
        }

        if (expanded) {
            series.average()?.let { average ->
                ReadingRow(
                    label = stringResource(R.string.statistics_average),
                    value = metric.unit.format(average),
                    testTag = TestTags.Statistics.READINGS_AVERAGE,
                )
            }
            // The Trend row the roadmap puts beside the average (N36, N39). It arrives with the fitted line
            // rather than before it: a slope is a reading of the series, and there was nothing to read.
            series.trend()?.let { trend ->
                ReadingRow(
                    label = stringResource(R.string.statistics_trend),
                    value = stringResource(
                        R.string.statistics_per_week,
                        slopeText(trend.perWeek) { metric.unit.formatRate(it) },
                        stringResource(metric.unit.labelRes),
                    ),
                    testTag = TestTags.Statistics.READINGS_TREND,
                )
            }
            readings.forEachIndexed { index, reading ->
                ReadingRow(
                    label = HistoryFormat.date(reading.at, zone = ZoneId.systemDefault()),
                    value = metric.unit.format(reading.value),
                    testTag = TestTags.Statistics.reading(index),
                )
            }
        }
    }
}

/** One line of the list: what it is on the left, what it reads on the right. */
@Composable
private fun ReadingRow(label: String, value: String, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // The label is the quiet half: a date or "Average" names the row, and the reading is
        // what someone opened this for.
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The custom range, with its From and To (ROADMAP N35).
 *
 * Each date is chosen in a calendar of its own, and applying is refused until both are: a window with one
 * end is the same as no window, and the type's tolerance for a missing end exists so an older stored value
 * cannot break a screen, not as a state to offer.
 *
 * The dates are read back in UTC, which is the zone the picker reports in — converting them in the device's
 * zone would shift the day for anyone west of Greenwich and make "From 1 August" mean 31 July.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    initial: StatisticsRange,
    onDismiss: () -> Unit,
    onConfirm: (StatisticsRange) -> Unit,
) {
    var from by remember { mutableStateOf(initial.from) }
    var to by remember { mutableStateOf(initial.to) }
    var picking by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.range_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRow(
                    labelRes = R.string.statistics_custom_from,
                    date = from,
                    testTag = TestTags.Statistics.CUSTOM_FROM,
                    onClick = { picking = true },
                )
                DateRow(
                    labelRes = R.string.statistics_custom_to,
                    date = to,
                    testTag = TestTags.Statistics.CUSTOM_TO,
                    onClick = { picking = false },
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { onConfirm(StatisticsRange(RangeKind.CUSTOM, from = from, to = to)) },
                enabled = from != null && to != null,
                modifier = Modifier.testTag(TestTags.Statistics.CUSTOM_APPLY),
            ) {
                Text(stringResource(R.string.statistics_custom_apply))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    picking?.let { choosing ->
        // The calendar is shared with the measurement day field (B217); this end of a range may be any day.
        DatePrompt(
            initial = if (choosing) from else to,
            onDismiss = { picking = null },
            onPick = { date ->
                if (choosing) from = date else to = date
                picking = null
            },
        )
    }
}

/** One end of a custom range: what it is, what it currently says, and the way to change it. */
@Composable
private fun DateRow(labelRes: Int, date: LocalDate?, testTag: String, onClick: () -> Unit) {
    AppTextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Text(stringResource(labelRes) + ": " + (date?.toString() ?: stringResource(R.string.statistics_custom_no_date)))
    }
}

/**
 * Which lift an Exercise metric is about (ROADMAP N35).
 *
 * Only shown when the metric needs one, and only the library's names: the same list the library screen
 * offers, because a second way to name a lift would be a second thing to keep in step.
 *
 * [acceptsHead] is the chosen metric's own answer (ROADMAP N97, B98), and it is what the family label is
 * gated on: a movement is offered under every metric and reads as the one lift it is, while a **category** —
 * the only row that heads others (N110) — is offered under the metrics that read a family, so under the rest
 * the same name must promise the one lift it actually draws. The list itself is already filtered by the
 * ViewModel.
 */
@Composable
private fun LiftPicker(
    lifts: List<Exercise>,
    selectedId: String?,
    acceptsHead: Boolean,
    onSelectExercise: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = lifts.firstOrNull { it.id == selectedId }

    Column {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.Statistics.LIFT),
        ) {
            // A category reads as a family rather than as another lift (ROADMAP N97, N110): choosing it is
            // choosing every movement filed under it, and a name alone would promise one lift's numbers.
            val chosenFamily = acceptsHead && selected?.headsOthers == true
            Text(
                text = when {
                    selected == null -> stringResource(R.string.statistics_choose_lift)
                    chosenFamily -> stringResource(R.string.statistics_lift_family, selected.name)
                    else -> selected.name
                },
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            lifts.forEach { lift ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(lift.name)
                            if (acceptsHead && lift.headsOthers) {
                                Text(
                                    text = stringResource(R.string.statistics_lift_all_in_family),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        open = false
                        onSelectExercise(lift.id)
                    },
                    modifier = Modifier.testTag(TestTags.Statistics.lift(lift.id)),
                )
            }
        }
    }
}

/** The screen's bar: a title, and the way to the two screens it pushes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsTopBar(
    onOpenMeasurements: (() -> Unit)?,
    onOpenAdherence: (() -> Unit)?,
) {
    // Icon actions rather than the two text buttons this used to have. A centred title and two
    // word-length actions want the same pixels, and on a phone the title loses: "STATISTICS"
    // ran into "Adherence" and overlapped it. The glyphs carry the meaning and their
    // descriptions carry it for a screen reader; the words were only ever doing the second job.
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = stringResource(R.string.tab_statistics)) },
        actions = {
            if (onOpenAdherence != null) {
                IconButton(
                    onClick = onOpenAdherence,
                    modifier = Modifier.testTag(TestTags.Statistics.ADHERENCE),
                ) {
                    Icon(
                        imageVector = Icons.Filled.EventAvailable,
                        contentDescription = stringResource(R.string.adherence_title),
                    )
                }
            }
            if (onOpenMeasurements != null) {
                IconButton(
                    onClick = onOpenMeasurements,
                    modifier = Modifier.testTag(TestTags.Statistics.MEASUREMENTS),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Straighten,
                        contentDescription = stringResource(R.string.measurements_title),
                    )
                }
            }
        },
    )
}
