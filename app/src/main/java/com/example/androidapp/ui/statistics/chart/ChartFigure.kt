package com.example.androidapp.ui.statistics.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.ChartMarker
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TrendChartFrame
import com.example.androidapp.ui.statistics.MOVING_AVERAGE_PERIODS
import com.example.androidapp.ui.statistics.MetricEntry
import com.example.androidapp.ui.statistics.MetricSeries
import com.example.androidapp.ui.statistics.average
import com.example.androidapp.ui.statistics.label

/**
 * The figure and its scale, drawn once for both the card and the full-height view (N196, N200).
 *
 * One composable rather than two drawings: the scrub, the axis and the chart have to agree about the same points,
 * and a second copy is how they stop agreeing.
 */
@Composable
internal fun ChartFigure(
    overlay: ChartOverlay,
    series: MetricSeries,
    metric: MetricEntry,
    height: Dp,
    testTag: String,
    /** A pinch over the figure, reported rather than applied (N213). */
    onZoom: ((scale: Float, focus: Float) -> Unit)? = null,
) {
    // The reading the finger is over, or null (N200). Held here because the answer is a date and a value, which
    // is the screen's knowledge rather than the drawing's. A **reading** rather than an index (B208): keyed on
    // the series, so a range or metric that changes under a held finger throws the answer away instead of
    // naming whatever now sits at that place.
    var scrub by remember(series) { mutableStateOf<ScrubPoint?>(null) }

    // How a screen reader moves the readout (B213): the canvas is blanked, so the two steps live on the frame.
    val previousLabel = stringResource(R.string.statistics_chart_previous_reading)
    val nextLabel = stringResource(R.string.statistics_chart_next_reading)
    fun read(index: Int?) {
        scrub = index?.let { scrubPointAt(overlay.projected, series.readings, it) }
    }

    fun step(by: Int) {
        read(recordedNeighbour(overlay.projected, scrub?.index, by))
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        ChartAxis(height = height, axis = overlay.axis, format = { metric.unit.format(it) })
        TrendChartFrame(
            points = overlay.projected,
            goals = overlay.goals,
            minValue = overlay.axis.min,
            maxValue = overlay.axis.max,
            testTag = testTag,
            bars = metric.isBars,
            average = series.average(),
            // The line is evaluated across the same elapsed time the points are placed by (N37), so it leans the
            // way the readings do rather than the way the index would.
            movingAverage = overlay.mean,
            trend = overlay.fittedLine,
            height = height,
            modifier = Modifier.weight(1f),
            onZoom = onZoom,
            semanticsModifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(previousLabel) {
                        step(-1)
                        true
                    },
                    CustomAccessibilityAction(nextLabel) {
                        step(+1)
                        true
                    },
                )
            },
            // The nearest recorded reading under the finger (N200): the same points the line is drawn through, so
            // a readout cannot name a value the figure does not show.
            onScrub = { fraction ->
                scrub = fraction?.let { scrubPoint(overlay.projected, series.readings, it) }
            },
            // The same point the readout names, drawn on the figure: a line to the axis and a dot on the reading
            // (N203), so the value does not have to be matched to a place by eye.
            marker = scrub?.let { ChartMarker(x = it.x, value = it.value) },
        )
    }
    scrub?.let { ScrubReadout(point = it, metric = metric) }
}

/**
 * The same figure at the room a screen can give it (N201).
 *
 * A dialog rather than a route: the chart is one figure with no navigation of its own, and leaving the screen to
 * see it would lose the range and metric the reader had just chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartExpandedDialog(
    overlay: ChartOverlay,
    series: MetricSeries,
    metric: MetricEntry,
    dates: List<String>,
    planLines: List<String>,
    period: Int,
    onSelectPeriod: (Int) -> Unit,
    onDismiss: () -> Unit,
    onZoom: ((scale: Float, focus: Float) -> Unit)? = null,
) {
    // The room the window has, not the preferences' idea of it: LocalConfiguration is a display setting that
    // can disagree with the window a dialog is actually drawn in.
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val height = with(density) { expandedFigureHeight(windowHeight.toDp()) }
    // The dialog may use most of the window, and its **body** scrolls rather than clipping (B219): the figure is
    // still a fraction of the window, but a short one would push the extras off the bottom of a window that has
    // nowhere to go. The header stays put so the way out is never scrolled away.
    val maxHeight = with(density) { (windowHeight * EXPANDED_DIALOG_MAX_FRACTION).toDp() }
    // The dialog's name, said twice on purpose: once as a pane title for a screen reader (B213), once as the
    // heading a sighted reader sees.
    val title = stringResource(R.string.statistics_chart_figure, metric.label())
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.Statistics.CHART_EXPANDED)
                .semantics { paneTitle = title },
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.heightIn(max = maxHeight).padding(16.dp)) {
                // The way out sits above the figure, not under it: a dialog taller than the screen would put a
                // close button exactly where it cannot be reached.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A dialog needs a name to be announced with (N206): a figure with nothing but a close button
                    // is a window a screen reader calls "dialog" and nothing else.
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag(TestTags.Statistics.CHART_EXPANDED_TITLE),
                    )
                    AppTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(TestTags.Statistics.CHART_COLLAPSE),
                    ) {
                        Text(stringResource(R.string.statistics_chart_collapse))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .testTag(TestTags.Statistics.CHART_EXPANDED_BODY),
                ) {
                    ChartFigure(
                        overlay = overlay,
                        series = series,
                        metric = metric,
                        height = height,
                        testTag = TestTags.Statistics.CHART_EXPANDED_FIGURE,
                        onZoom = onZoom,
                    )
                    // The same extras the card carries (N204): the expanded view is the same reading of the same
                    // series, so it cannot be a figure with less said about it.
                    ChartExtras(
                        dates = dates,
                        planLines = planLines,
                        period = period,
                        onSelectPeriod = onSelectPeriod,
                    )
                }
            }
        }
    }
}

/**
 * What the figure says around itself: the window it averages over, the time it spans and the plans it draws
 * (N204).
 *
 * One composable for the card and the expanded view, because the two are the same reading: a dialog that dropped
 * the dates or the plans would be a second, thinner answer to the same question.
 *
 * **The window and the way into the full-height view share a row** (N222). They are the card's two ways of
 * changing how much of the figure is read, and each on a line of its own spent a row of a small phone on nothing
 * between the dates and the actions.
 */
@Composable
internal fun ChartExtras(
    dates: List<String>,
    planLines: List<String>,
    period: Int,
    onSelectPeriod: (Int) -> Unit,
    onExpand: (() -> Unit)? = null,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dates.forEachIndexed { index, date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag(
                        when (index) {
                            0 -> TestTags.Statistics.CHART_FIRST_DATE
                            1 -> TestTags.Statistics.CHART_MIDDLE_DATE
                            else -> TestTags.Statistics.CHART_LAST_DATE
                        },
                    ),
                )
            }
        }
        if (planLines.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_plan_targets, planLines.joinToString(" · ")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.Statistics.PLAN_TARGETS),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MovingAveragePicker(period = period, onSelect = onSelectPeriod)
            if (onExpand != null) {
                AppTextButton(
                    onClick = onExpand,
                    modifier = Modifier.testTag(TestTags.Statistics.CHART_EXPAND),
                ) {
                    Text(stringResource(R.string.statistics_chart_expand))
                }
            }
        }
    }
}

/**
 * The window the trailing average is taken over (N40), behind one control (N211).
 *
 * Three chips for a setting most people pick once were a fourth row inside the figure's card; the control says
 * which window is on screen and opens the three, the shape the range's *More* already uses.
 */
@Composable
private fun MovingAveragePicker(period: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.Statistics.MOVING_AVERAGE),
        ) {
            Text(stringResource(R.string.statistics_moving_average) + ": " + period)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MOVING_AVERAGE_PERIODS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    modifier = Modifier.testTag(TestTags.Statistics.movingAveragePeriod(option)),
                )
            }
        }
    }
}
