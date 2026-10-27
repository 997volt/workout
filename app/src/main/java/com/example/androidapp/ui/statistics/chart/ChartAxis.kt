package com.example.androidapp.ui.statistics.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.TestTags

/**
 * How much of the window the inline figure takes (B261, amending N196).
 *
 * The height is the room the screen gives the figure, not something the series decides — and for years that
 * room was a fixed 180 dp, chosen when the card still carried a row of window chips. The chips became one
 * control (N211) and that control moved onto *Expand*'s line (N222), so the figure was the one thing in the
 * card that never grew: a fifth of a phone's screen, for the screen whose subject it is.
 *
 * **The bounds are the point of the fraction.** Below them the card would squeeze the dates and the actions on
 * a short window; above them a tall one would turn the inline figure into a second expanded view and leave the
 * page underneath it with nowhere to be read. 30 % is N201's own reasoning applied to the small figure.
 */
internal const val INLINE_CHART_FRACTION = 0.30f

/** The shortest the inline figure may be drawn, whatever the window says. */
internal val INLINE_CHART_MIN_HEIGHT = 220.dp

/** The tallest, so a tablet does not get a second expanded view by default. */
internal val INLINE_CHART_MAX_HEIGHT = 340.dp

/**
 * How much of the screen the expanded figure takes (N201).
 *
 * A fraction rather than a height: the point of expanding is to use the room the device has, and a fixed number
 * would be a second, larger sparkline on a small phone and a small figure on a tablet.
 */
internal const val EXPANDED_CHART_FRACTION = 0.55f

/** The inline figure's height for a window of [windowHeight] (B261). */
internal fun inlineFigureHeight(windowHeight: Dp): Dp =
    (windowHeight * INLINE_CHART_FRACTION).coerceIn(INLINE_CHART_MIN_HEIGHT, INLINE_CHART_MAX_HEIGHT)

/**
 * The expanded figure's height for the same window.
 *
 * A function rather than the multiplication at the call site so the two sizes can be compared in one test: the
 * expanded view is the larger of the two on every window the app can be drawn in, which is what makes *Expand*
 * mean something.
 */
internal fun expandedFigureHeight(windowHeight: Dp): Dp = windowHeight * EXPANDED_CHART_FRACTION

/**
 * How much of the window the expanded dialog itself may take (B219).
 *
 * More than the figure's fraction, because the title and the extras are above and below it; the dialog's body
 * scrolls past this rather than clipping, so a short window loses nothing but a swipe.
 */
internal const val EXPANDED_DIALOG_MAX_FRACTION = 0.9f

/** The axis gutter's width, so three numbers do not push the figure off its own card on a small phone. */
private val CHART_AXIS_WIDTH = 52.dp

/**
 * The figure's scale, written against its own top, middle and foot (N196, B259).
 *
 * A column rather than labels inside the canvas: the canvas is blanked for a screen reader, and three numbers
 * in the metric's own unit are the part of a chart that is a sentence rather than a picture. The **middle** is
 * new with the gutter — min and max sat in a row under the chart, where nothing said which end of the line
 * they were.
 *
 * **The height is handed in rather than filled** (B259). Asking for `fillMaxHeight()` looked right and did
 * nothing on the screen this gutter lives on: the statistics page scrolls, so the constraint it filled was
 * infinite and the modifier resolved to wrap-content — the row then took its height from the canvas, and the
 * three numbers stacked in a line at the top where they named no height at all.
 */
@Composable
internal fun ChartAxis(height: Dp, axis: AxisBounds, format: (Double) -> String) {
    Column(
        modifier = Modifier
            .height(height)
            .width(CHART_AXIS_WIDTH)
            .padding(top = 12.dp, end = 8.dp)
            .testTag(TestTags.Statistics.CHART_AXIS),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = format(axis.max),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag(TestTags.Statistics.CHART_MAX),
        )
        Text(
            text = format((axis.max + axis.min) / 2),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = format(axis.min),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag(TestTags.Statistics.CHART_MIN),
        )
    }
}
