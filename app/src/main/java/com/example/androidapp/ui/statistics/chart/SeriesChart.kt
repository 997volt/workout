package com.example.androidapp.ui.statistics.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.DAYS
import com.example.androidapp.ui.statistics.MetricEntry
import com.example.androidapp.ui.statistics.MetricSeries
import java.time.Instant
import java.time.ZoneId

/**
 * The series, drawn through the chart every other trend uses. N37 gives it a time axis.
 *
 * Two functions rather than one: the frame decides *when* and *how much*, and the body works out what to
 * draw inside it. Kept apart because the whole thing together was past the length a reader can hold, and the
 * seam is a real one — nothing in the body knows about the picker or the restart.
 */
@Composable
internal fun SeriesChart(
    series: MetricSeries,
    metric: MetricEntry,
    goalPeriods: List<GoalPeriod>,
    planTargets: List<PlanTarget>,
    onZoom: (scale: Float, focus: Float) -> Unit,
) {
    if (series.readings.count { it.value != null } < 2) return
    var period by rememberSaveable { mutableStateOf(DAYS) }
    val overlay = chartOverlay(series, metric, goalPeriods, planTargets, period)

    // The chart, the window it is averaged over and the axis in words are one card: they are one
    // reading of one series, and separating the picker from the line it changes would make the
    // control look like a setting rather than part of the figure.
    var expanded by remember { mutableStateOf(false) }

    // The room this window has, which is the figure's size (B261): a fixed height was a fifth of a phone's
    // screen and did not change when the card's other rows did.
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    AppCard {
        ChartFigure(
            overlay = overlay,
            series = series,
            metric = metric,
            height = inlineFigureHeight(windowHeight),
            testTag = TestTags.Statistics.CHART,
            onZoom = onZoom,
        )
        ChartExtras(
            dates = tickDates(series),
            planLines = planLines(overlay, metric),
            period = period,
            onSelectPeriod = { period = it },
            onExpand = { expanded = true },
        )
    }

    if (expanded) {
        ChartExpandedDialog(
            overlay = overlay,
            series = series,
            metric = metric,
            dates = tickDates(series),
            planLines = planLines(overlay, metric),
            period = period,
            onSelectPeriod = { period = it },
            onDismiss = { expanded = false },
            onZoom = onZoom,
        )
    }
}

/**
 * The first and last moments that recorded something, which is what the axis labels span.
 *
 * A gap means "not recorded", so an empty reading at either end is not the end of the window. One function
 * rather than two because the pair is only ever used together, and the file is at its function ceiling.
 */
private fun recordedSpan(series: MetricSeries): Pair<Instant, Instant> =
    series.readings.first { it.value != null }.at to series.readings.last { it.value != null }.at

/** Where the time axis puts its ticks: its two ends and the middle between them (N209). */
private const val AXIS_START = 0.0
private const val AXIS_MIDDLE = 0.5
private const val AXIS_END = 1.0
private val CHART_TICKS = listOf(AXIS_START, AXIS_MIDDLE, AXIS_END)

/** The three moments the time axis names: its ends and the middle between them (N209). */
private fun tickDates(series: MetricSeries): List<String> {
    val span = recordedSpan(series)
    val from = span.first.toEpochMilli()
    val length = span.second.toEpochMilli() - from
    return CHART_TICKS.map { fraction ->
        HistoryFormat.date(Instant.ofEpochMilli(from + (length * fraction).toLong()), zone = ZoneId.systemDefault())
    }
}

/** The plans whose targets the figure draws, named rather than left to a dash pattern (N39). */
@Composable
private fun planLines(overlay: ChartOverlay, metric: MetricEntry): List<String> {
    if (overlay.planLevels.isEmpty()) return emptyList()
    val unit = stringResource(metric.unit.labelRes)
    return overlay.planLevels.map { (plan, level) ->
        stringResource(R.string.statistics_plan_target, plan, metric.unit.format(level), unit)
    }
}
