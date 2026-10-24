package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.components.TestTags

/**
 * The height the statistics figure is drawn at (N196).
 *
 * A presentation constant rather than a metric's: the same figure draws a month of weight and a year of
 * volume, and the height is the room the screen gives the figure, not something the series decides.
 */
internal val CHART_FIGURE_HEIGHT = 180.dp

/**
 * How much of the screen the expanded figure takes (N201).
 *
 * A fraction rather than a height: the point of expanding is to use the room the device has, and a fixed number
 * would be a second, larger sparkline on a small phone and a small figure on a tablet.
 */
internal const val EXPANDED_CHART_FRACTION = 0.55f

/** The axis gutter's width, so three numbers do not push the figure off its own card on a small phone. */
private val CHART_AXIS_WIDTH = 52.dp

/**
 * The figure's scale, written against its own top, middle and foot (N196).
 *
 * A column rather than labels inside the canvas: the canvas is blanked for a screen reader, and three numbers
 * in the metric's own unit are the part of a chart that is a sentence rather than a picture. The **middle** is
 * new with the gutter — min and max sat in a row under the chart, where nothing said which end of the line
 * they were.
 */
@Composable
internal fun ChartAxis(axis: AxisBounds, format: (Double) -> String) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
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
