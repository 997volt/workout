package com.example.androidapp.ui.statistics.chart

import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.ChartLine
import com.example.androidapp.ui.components.ChartPoint
import com.example.androidapp.ui.components.ChartSegment
import com.example.androidapp.ui.statistics.MetricEntry
import com.example.androidapp.ui.statistics.MetricSeries
import com.example.androidapp.ui.statistics.movingAverage
import com.example.androidapp.ui.statistics.trend

/**
 * Everything the chart draws, worked out away from the canvas (ROADMAP N37, N39, N40).
 *
 * A pure function rather than locals in the composable so the arithmetic can be asserted without rendering:
 * the axis depending on the fitted line is the kind of thing that is wrong invisibly, since a clamped line
 * still draws.
 */
internal data class ChartOverlay(
    val projected: List<ChartPoint>,
    val mean: List<ChartPoint>,
    val fittedLine: ChartLine?,
    /** One line per goal period, and one per plan that prescribes the lift (ROADMAP N137, N39). */
    val goals: List<ChartSegment>,
    /** The plans drawn, named, so the line under the chart can say which is which (ROADMAP N39). */
    val planLevels: List<Pair<String, Double>>,
    val axis: AxisBounds,
)

internal fun chartOverlay(
    series: MetricSeries,
    metric: MetricEntry,
    goalPeriods: List<GoalPeriod>,
    planTargets: List<PlanTarget>,
    period: Int,
): ChartOverlay {
    val recorded = series.readings.mapNotNull { it.value }
    val firstAt = series.readings.first().at.toEpochMilli().toDouble()
    val timeSpan = series.readings.last().at.toEpochMilli().toDouble() - firstAt
    val trend = series.trend()
    val projected = projectByTime(series.readings)

    // The fitted line can leave the range of the readings — a series rising fast ends above its own last
    // point — so its ends are part of the axis. Built from the readings alone, the line was clamped flat along
    // the top edge for its last stretch and appeared to level off where it rose fastest.
    fun endAt(fraction: Double) = trend?.valueAt(fraction, first = firstAt, span = timeSpan)
    val fittedLine = trend?.let {
        ChartLine(start = endAt(0.0)!!, end = endAt(1.0)!!)
    }

    // The mean is a subset of the same readings, so it shares their places on the axis rather than being
    // projected again — two projections of the same instants would be two chances to disagree.
    val xByInstant = series.readings.mapIndexed { index, reading -> reading.at to projected[index].x }.toMap()
    val mean = series.movingAverage(period).mapNotNull { reading ->
        xByInstant[reading.at]?.let { x -> ChartPoint(x = x, value = reading.value) }
    }

    // The lifter's own goal and the plans' targets in one list: the chart draws reference lines, and the axis
    // has to hold all of them for any of them to be visible.
    val planSegments = planSegments(series, planTargets, metric.key)
    val goals = goalSegments(series, goalPeriods) + planSegments

    return ChartOverlay(
        projected = projected,
        mean = mean,
        fittedLine = fittedLine,
        goals = goals,
        planLevels = planSegments.mapNotNull { segment -> segment.planName?.let { it to segment.value } },
        axis = axisBounds(
            recorded + listOfNotNull(endAt(0.0), endAt(1.0)),
            fromZero = metric.fromZero,
            goals = goals.map { it.value },
            fixedRange = metric.fixedRange,
        ),
    )
}
