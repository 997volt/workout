package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.ChartMarker
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TrendChartFrame

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
) {
    // The reading the finger is over, or null (N200). Held here because the answer is a date and a value, which
    // is the screen's knowledge rather than the drawing's; the index names the same point in the projection and
    // in the series, which is what lets the marker and the readout agree (N203).
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    Row(modifier = Modifier.fillMaxWidth()) {
        ChartAxis(axis = overlay.axis, format = { metric.unit.format(it) })
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
            // The nearest recorded reading under the finger (N200): the same points the line is drawn through, so
            // a readout cannot name a value the figure does not show.
            onScrub = { fraction -> scrubbedIndex = fraction?.let { nearestReadingIndex(overlay.projected, it) } },
            // The same point the readout names, drawn on the figure: a line to the axis and a dot on the reading
            // (N203), so the value does not have to be matched to a place by eye.
            marker = scrubbedIndex?.let { index ->
                overlay.projected.getOrNull(index)?.value?.let { value ->
                    ChartMarker(x = overlay.projected[index].x, value = value)
                }
            },
        )
    }
    scrubbedIndex?.let { index -> ScrubReadout(reading = series.readings[index], metric = metric) }
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
    onDismiss: () -> Unit,
) {
    // The room the window has, not the preferences' idea of it: LocalConfiguration is a display setting that
    // can disagree with the window a dialog is actually drawn in.
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val height = with(density) { (windowHeight * EXPANDED_CHART_FRACTION).toDp() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.Statistics.CHART_EXPANDED),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // The way out sits above the figure, not under it: a dialog taller than the screen would put a
                // close button exactly where it cannot be reached.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    AppTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(TestTags.Statistics.CHART_COLLAPSE),
                    ) {
                        Text(stringResource(R.string.statistics_chart_collapse))
                    }
                }
                ChartFigure(
                    overlay = overlay,
                    series = series,
                    metric = metric,
                    height = height,
                    testTag = TestTags.Statistics.CHART_EXPANDED_FIGURE,
                )
            }
        }
    }
}
