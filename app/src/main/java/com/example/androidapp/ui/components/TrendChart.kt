package com.example.androidapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A line over time, hand-drawn (ROADMAP N13, N37).
 *
 * **Points arrive already placed horizontally**, as a fraction of the width, because where a reading sits is
 * a question about time and this component draws pictures (N37). Spreading them by index is what made two
 * workouts a day apart and two a month apart the same distance apart — a real distortion rather than a
 * detail, and one the chart could not fix from the inside because it never saw the dates.
 *
 * Two things it deliberately does not do. It does not interpolate across a missing
 * value: a gap is "not recorded", and drawing through it would invent a measurement. That matters more on a
 * time axis, not less: the gap is now visible as *distance*, and a straight segment drawn across three weeks
 * would be a claim about weeks nobody measured. And it does not choose its own axis — the caller passes the
 * bounds, and the bounds decide how much a change *looks* like: a load starts at zero because a kilogram is a
 * quantity, while a rating works in a narrow band where a fixed axis is what stops a 0.2 change reading as a
 * cliff. That policy belongs to the metric, so it lives in the registry beside the metric rather than here.
 *
 * Marked as decorative: the labels and the caption carry the numbers, so a screen reader hears the sentence
 * rather than a description of a picture it cannot see. The frame above it is where a caller puts the figure's
 * own actions, outside this cleared node (B213).
 */
@Composable
fun TrendChart(
    points: List<ChartPoint>,
    minValue: Double,
    maxValue: Double,
    modifier: Modifier = Modifier,
    /** Bars for a count-like series, a line for a continuous one (ROADMAP N38). */
    bars: Boolean = false,
    /** A horizontal line at the average, and the fitted trend (ROADMAP N39). */
    average: Double? = null,
    trend: ChartLine? = null,
    /** The trailing mean, drawn heavier and without dots (ROADMAP N40). */
    movingAverage: List<ChartPoint> = emptyList(),
    /**
     * This metric's targets, drawn dotted so they are not the average (ROADMAP N39, N137).
     *
     * A list rather than one value: a goal that changed draws one line per period, each across the stretch it
     * applied to, so the chart never claims a target applied to a range it did not.
     */
    goals: List<ChartSegment> = emptyList(),
    /** A point the reader is pointing at, drawn as a line up the figure and a dot on it (N203). */
    marker: ChartMarker? = null,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    // A neutral annotation rather than a tinted one: the theme's tertiary is close enough to the surface
    // that a reference line drew but could not be seen on a device.
    val referenceColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Stronger than the goal's muted grey: a plan's target is a prescription rather than a note, and the two
    // lines must be tellable apart at a glance as well as by the dash (N39).
    val planColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val axis = TrendAxis(minValue, maxValue)

        fun y(value: Double): Float = size.height * (1f - axis.fractionOf(value).toFloat())

        drawAxisGrid(gridColor = gridColor, minValue = minValue, maxValue = maxValue, y = ::y)

        // The marker under the readings: a line the eye can follow down to the axis, then the reading itself
        // (N203). Drawn before the series so the dot is never hidden by the line it belongs to.
        marker?.let { drawMarker(it, line = referenceColor, dot = dotColor, y = ::y) }

        drawReferenceLines(
            referenceColor = referenceColor,
            planColor = planColor,
            lines = ReferenceLines(
                average = average,
                trend = trend,
                movingAverage = movingAverage,
                goals = goals,
            ),
            y = ::y,
        )

        if (bars) {
            // The mean first, under the readings: it summarises them, and the data is what is being read.
            movingAverage.zipWithNext { from, to ->
                val fromValue = from.value ?: return@zipWithNext
                val toValue = to.value ?: return@zipWithNext
                drawLine(
                    color = referenceColor,
                    start = Offset(from.x * size.width, y(fromValue)),
                    end = Offset(to.x * size.width, y(toValue)),
                    strokeWidth = MEAN_STROKE_PX,
                )
            }

            drawBars(points = points, y = ::y, color = dotColor)
            return@Canvas
        }

        points.forEachIndexed { index, point ->
            val value = point.value ?: return@forEachIndexed
            val x = point.x * size.width
            // A segment needs both ends recorded, so a gap is a break rather than a straight line through
            // whatever nobody measured.
            val next = points.getOrNull(index + 1)?.value
            if (next != null) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, y(value)),
                    end = Offset(points[index + 1].x * size.width, y(next)),
                    strokeWidth = LINE_STROKE_PX,
                )
            }
            drawCircle(
                color = dotColor,
                radius = DOT_RADIUS_PX,
                center = Offset(x, y(value)),
            )
        }
    }
}

/** One point the reader is pointing at: where it is across the figure, and what it says (N203). */
data class ChartMarker(val x: Float, val value: Double)

/**
 * The chart's own frame: a height the caller chooses, and the width it is given (N196).
 *
 * The height used to be fixed at the sparkline's, which made a screen whose subject *was* the figure draw a
 * thumbnail: statistics asks for a figure's height now, and the adherence breakdown keeps the small one it was
 * designed around. The default is the small one on purpose — a caller that has not thought about it gets what
 * every existing screen already looked like.
 */
@Composable
fun TrendChartFrame(
    points: List<ChartPoint>,
    minValue: Double,
    maxValue: Double,
    testTag: String,
    modifier: Modifier = Modifier,
    /**
     * Semantics on the frame rather than on the canvas (B213).
     *
     * The canvas is blanked for a screen reader on purpose, so the figure's own actions — a reader stepping
     * through the readings — live on the frame that holds it, outside the cleared node.
     */
    semanticsModifier: Modifier = Modifier,
    bars: Boolean = false,
    average: Double? = null,
    trend: ChartLine? = null,
    movingAverage: List<ChartPoint> = emptyList(),
    goals: List<ChartSegment> = emptyList(),
    height: Dp = CHART_HEIGHT_DP.dp,
    /**
     * Where a finger is across the figure, and null when it lifts (N200), or null to leave the chart
     * untouchable.
     *
     * A fraction of the width rather than an index: the caller knows what its points mean, and this component
     * only knows where the finger landed.
     */
    onScrub: ((Float?) -> Unit)? = null,
    /** The point the scrub is naming, drawn on the figure as well (N203). */
    marker: ChartMarker? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(semanticsModifier)
            .testTag(testTag),
    ) {
        TrendChart(
            points = points,
            minValue = minValue,
            maxValue = maxValue,
            bars = bars,
            average = average,
            trend = trend,
            movingAverage = movingAverage,
            goals = goals,
            marker = marker,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .then(if (onScrub == null) Modifier else Modifier.scrubbable(onScrub)),
        )
    }
}

/** A trend line drawn across the whole width: its value at the first reading and at the last. */
data class ChartLine(val start: Double, val end: Double)

/**
 * One **period's** target, drawn across the stretch of the chart it applied to (ROADMAP N137).
 *
 * The line used to be one value across the whole width, which is a claim about ranges it never applied to:
 * changing a goal silently re-judged the past. A segment carries where it begins and ends as fractions of the
 * width, so the chart draws what was actually asked for when it was asked for.
 */
data class ChartSegment(
    val fromX: Float,
    val toX: Float,
    val value: Double,
    /**
     * The plan this line is the target of, or null when it is the lifter's own goal (ROADMAP N39).
     *
     * A chart can carry both, and they mean different things — "what you asked yourself for" and "what your
     * plan asks you for" — so the two are drawn differently and the plan is named under the chart.
     */
    val planName: String? = null,
)

/** Thinner than the data, because a fitted line is a summary rather than a measurement. */
private const val REFERENCE_STROKE_PX = 2f

/** The lines that are about the series rather than readings in it (ROADMAP N39, N40). */
data class ReferenceLines(
    val average: Double?,
    val trend: ChartLine?,
    val movingAverage: List<ChartPoint>,
    val goals: List<ChartSegment>,
)

/** Heavier than a fitted line: the mean is what a noisy daily reading is read through. */
private const val MEAN_STROKE_PX = 4f

/** A dot, then a gap four times its length: a dotted line rather than a dashed one. */
private const val GOAL_DOT_PX = 1f
private const val GOAL_GAP_PX = 4f

/** A bar is a share of the width, not a sliver: two fifths of the space each bar has. */
private const val BAR_WIDTH_FRACTION = 2.5f

/** Below this a bar is invisible on a phone, however many readings there are. */
private const val MIN_BAR_WIDTH_PX = 2f

/** The middle of a flat axis: a series whose values are all equal draws down the centre. */
private const val MID_AXIS = 0.5

/** Where a value sits on the axis, as a 0..1 fraction from the bottom. */
private class TrendAxis(private val min: Double, private val max: Double) {
    fun fractionOf(value: Double): Double =
        if (max - min == 0.0) MID_AXIS else ((value - min) / (max - min)).coerceIn(0.0, 1.0)
}

/** The 1–10 rating axis: fixed, so the three ratings read against each other. */

/** The chart's ink, in pixels: it is a fixed-size glyph, not a responsive layout. */
private const val GRID_STROKE_PX = 1f
private const val LINE_STROKE_PX = 3f
private const val DOT_RADIUS_PX = 4f
private const val DASH_ON_PX = 6f
private const val DASH_OFF_PX = 6f
private const val CHART_HEIGHT_DP = 72

/** The pointer's line is thinner than the data: it is a question, not a reading (N203). */
private const val MARKER_STROKE_PX = 1.5f

/** And its dot is larger than a reading's, so the one being pointed at is the one the eye finds (N203). */
private const val MARKER_RADIUS_PX = 6f

/**
 * One reading's place in the chart (ROADMAP N37).
 *
 * [x] is a fraction of the width and [value] is what to draw there, or null for a moment that recorded
 * nothing — which is a point on the axis and a break in the line. Placement is the caller's, because it is a
 * question about time and this component never sees a date.
 */
data class ChartPoint(val x: Float, val value: Double?)

/** The point a finger is over: a line down the figure and an emphasised dot on the reading (N203). */
private fun DrawScope.drawMarker(marker: ChartMarker, line: Color, dot: Color, y: (Double) -> Float) {
    val x = marker.x * size.width
    drawLine(
        color = line,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = MARKER_STROKE_PX,
    )
    drawCircle(color = dot, radius = MARKER_RADIUS_PX, center = Offset(x, y(marker.value)))
}

/**
 * Bars from the baseline up (ROADMAP N38).
 *
 * A separate function because it is the other way of drawing the same points, and because the chart was at its
 * length ceiling with both of them in it.
 */
private fun DrawScope.drawBars(points: List<ChartPoint>, y: (Double) -> Float, color: Color) {
    // An empty series would divide by zero, and `NaN.coerceAtLeast` stays `NaN` — which then becomes a
    // `Size(NaN, …)` silently rather than throwing, so it is guarded here rather than trusted.
    if (points.isEmpty()) return
    // A bar length is the quantity, so the bar has to start where the quantity starts — which is why a bars
    // metric's axis is anchored at zero by the registry.
    val width = (size.width / points.size).coerceAtLeast(MIN_BAR_WIDTH_PX) / BAR_WIDTH_FRACTION
    val baseline = y(0.0).coerceIn(0f, size.height)
    points.forEach { point ->
        val value = point.value ?: return@forEach
        drawRect(
            color = color,
            topLeft = Offset(point.x * size.width - width / 2f, minOf(y(value), baseline)),
            size = Size(width, kotlin.math.abs(baseline - y(value))),
        )
    }
}

/**
 * The average, the fitted trend and the trailing mean (ROADMAP N39, N40).
 *
 * Drawn before the readings, so the data sits over its own summaries, and together because they are the same
 * kind of thing: a line about the series rather than a reading in it.
 */
private fun DrawScope.drawReferenceLines(
    referenceColor: Color,
    planColor: Color,
    lines: ReferenceLines,
    y: (Double) -> Float,
) {
    val average = lines.average
    val trend = lines.trend
    val movingAverage = lines.movingAverage
    val goals = lines.goals
    val color = referenceColor
    // Dashed, because an average is a level rather than a measurement: it says where the series sits, and
    // every reading is above or below it.
    average?.let { value ->
        drawLine(
            color = color,
            start = Offset(0f, y(value)),
            end = Offset(size.width, y(value)),
            strokeWidth = REFERENCE_STROKE_PX,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_PX, DASH_OFF_PX)),
        )
    }
    trend?.let { line ->
        drawLine(
            color = color,
            start = Offset(0f, y(line.start)),
            end = Offset(size.width, y(line.end)),
            strokeWidth = REFERENCE_STROKE_PX,
        )
    }
    // Dotted where the average is dashed: both are levels, and reading the user's own target as the app's
    // summary of them would be the worst kind of quiet error. One line per period, each across its own stretch
    // (N137), so the chart never draws a target over a range it did not apply to.
    goals.forEach { segment ->
        // A plan's target is **dashed** where the lifter's own goal is **dotted** (N39): two lines that mean
        // different things must not be drawn the same, and the plan is named under the chart as well.
        val fromPlan = segment.planName != null
        drawLine(
            color = if (fromPlan) planColor else color,
            start = Offset(segment.fromX * size.width, y(segment.value)),
            end = Offset(segment.toX * size.width, y(segment.value)),
            strokeWidth = REFERENCE_STROKE_PX,
            pathEffect = PathEffect.dashPathEffect(
                if (fromPlan) floatArrayOf(DASH_ON_PX, DASH_OFF_PX) else floatArrayOf(GOAL_DOT_PX, GOAL_GAP_PX),
            ),
        )
    }
    movingAverage.zipWithNext { from, to ->
        val fromValue = from.value ?: return@zipWithNext
        val toValue = to.value ?: return@zipWithNext
        drawLine(
            color = color,
            start = Offset(from.x * size.width, y(fromValue)),
            end = Offset(to.x * size.width, y(toValue)),
            strokeWidth = MEAN_STROKE_PX,
        )
    }
}

/** The top and bottom of the axis, so an empty stretch still reads as a scale. */
private fun DrawScope.drawAxisGrid(
    gridColor: Color,
    minValue: Double,
    maxValue: Double,
    y: (Double) -> Float,
) {
    listOf(minValue, maxValue).forEach { value ->
        drawLine(
            color = gridColor,
            start = Offset(0f, y(value)),
            end = Offset(size.width, y(value)),
            strokeWidth = GRID_STROKE_PX,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_PX, DASH_OFF_PX)),
        )
    }
}
