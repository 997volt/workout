package com.example.androidapp.ui.statistics.chart

import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.levelFor
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.ChartPoint
import com.example.androidapp.ui.components.ChartSegment
import com.example.androidapp.ui.statistics.MetricKey
import com.example.androidapp.ui.statistics.MetricReading
import com.example.androidapp.ui.statistics.MetricSeries
import java.time.Instant

/**
 * Places a series horizontally by elapsed time (ROADMAP N37).
 *
 * The distortion this removes is real: spreading points by index drew two workouts a day apart and two a
 * month apart the same distance apart, so a month off looked like a day off and a hard week looked like a
 * hard quarter. Time is what the x-axis means.
 *
 * A moment that recorded nothing stays in the sequence with a null value: it is a place on the axis and a
 * break in the line, and dropping it would silently close the gap the chart exists to show.
 *
 * A single reading, or several that share an instant, sit at the middle — there is no span to place them
 * across, and putting the only point at the left edge would read as "the start of something".
 */
fun projectByTime(readings: List<MetricReading>): List<ChartPoint> {
    if (readings.isEmpty()) return emptyList()

    val first = readings.first().at
    val last = readings.last().at
    val span = last.toEpochMilli() - first.toEpochMilli()

    return readings.map { reading ->
        ChartPoint(
            x = if (span <= 0L) MIDDLE else fractionOf(reading.at, first, span),
            value = reading.value,
        )
    }
}

internal fun fractionOf(at: Instant, first: Instant, span: Long): Float =
    ((at.toEpochMilli() - first.toEpochMilli()).toDouble() / span).toFloat().coerceIn(0f, 1f)

/**
 * Each period's target as a segment of the chart (ROADMAP N137).
 *
 * The window is the series', so a period is clipped to it — and a period entirely outside it draws nothing
 * rather than a line pinned to an edge. The x mapping is [fractionOf], the same one the readings are placed by,
 * because two mappings of the same instants would be two chances to disagree (N37).
 *
 * A **period that states no weight** draws no line: a direction-only goal has no level to draw, and inventing
 * one from the band would be manufacturing the end weight N135 refuses to manufacture.
 */
internal fun goalSegments(series: MetricSeries, periods: List<GoalPeriod>): List<ChartSegment> {
    if (series.readings.size < 2) return emptyList()
    val first = series.readings.first().at
    val last = series.readings.last().at
    val span = last.toEpochMilli() - first.toEpochMilli()
    return if (span <= 0L) {
        emptyList()
    } else {
        periods.mapIndexedNotNull { index, period ->
            segmentOf(period, periods.getOrNull(index + 1), first, last, span)
        }
    }
}

/**
 * The plans' targets as stretches of the chart (ROADMAP N39).
 *
 * Across the **whole window**, unlike a goal's line: a prescription has no period — it is what the plan asks
 * for now — where a goal is bounded by the periods it applied to (N137).
 *
 * Empty for any series a plan target does not speak to, which is [levelFor]'s question: a rep range has no
 * level, and a total is a sum of a prescription rather than a line in it.
 */
internal fun planSegments(series: MetricSeries, plans: List<PlanTarget>, key: MetricKey): List<ChartSegment> {
    val metric = (key as? MetricKey.Exercise)?.metric
    return if (metric == null || series.readings.size < 2) {
        emptyList()
    } else {
        plans.mapNotNull { plan ->
            plan.levelFor(metric)?.let { level ->
                ChartSegment(fromX = 0f, toX = 1f, value = level, planName = plan.templateName)
            }
        }
    }
}

/** One period's segment, or null when it states no level or falls outside the window entirely. */
private fun segmentOf(
    period: GoalPeriod,
    next: GoalPeriod?,
    first: Instant,
    last: Instant,
    span: Long,
): ChartSegment? {
    val level = period.value ?: return null
    val from = period.startedAt ?: first
    val to = next?.startedAt ?: last
    return if (to.isBefore(first) || from.isAfter(last)) {
        null
    } else {
        ChartSegment(
            fromX = fractionOf(maxOf(from, first), first, span),
            toX = fractionOf(minOf(to, last), first, span),
            value = level,
        )
    }
}

/** The middle of an axis with no span: one reading, or several at the same instant. */
private const val MIDDLE = 0.5f
