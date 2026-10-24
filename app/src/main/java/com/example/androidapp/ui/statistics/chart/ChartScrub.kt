package com.example.androidapp.ui.statistics.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.androidapp.ui.components.ChartPoint
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.MetricEntry
import com.example.androidapp.ui.statistics.MetricReading
import java.time.Instant
import java.time.ZoneId

/**
 * The reading nearest a point across the chart, or null when the finger is past the last one (N200).
 *
 * A pure function rather than arithmetic inside the gesture so the answer can be asserted without a touch: the
 * points are the chart's own projection (a fraction of the width each), and the readings are the series' — the
 * two are index-aligned, gaps included, which is what lets the index name a reading.
 */
internal fun nearestReadingIndex(
    projected: List<ChartPoint>,
    fraction: Float,
): Int? {
    val recorded = projected.withIndex().filter { it.value.value != null }
    if (recorded.isEmpty()) return null
    return recorded.minByOrNull { (_, point) -> kotlin.math.abs(point.x - fraction) }?.index
}

/**
 * One reading a finger is over, with the value and the place it sits (B208).
 *
 * A reading rather than a bare index: the readout is drawn from this, so a series that changes under a held
 * finger cannot name a reading the figure no longer has, and a gap cannot be rendered as a zero. The index
 * survives only so the accessibility actions can step from where the reader is.
 */
internal data class ScrubPoint(val index: Int, val x: Float, val at: Instant, val value: Double)

/** The point [index] names, or null when the figure or the series has nothing there (B208). */
internal fun scrubPointAt(
    projected: List<ChartPoint>,
    readings: List<MetricReading>,
    index: Int,
): ScrubPoint? {
    val point = projected.getOrNull(index)
    val reading = readings.getOrNull(index)
    val value = reading?.value
    return if (point == null || reading == null || value == null) {
        null
    } else {
        ScrubPoint(index = index, x = point.x, at = reading.at, value = value)
    }
}

/** The point nearest [fraction] across the figure, or null when nothing recorded is there (B208). */
internal fun scrubPoint(
    projected: List<ChartPoint>,
    readings: List<MetricReading>,
    fraction: Float,
): ScrubPoint? = nearestReadingIndex(projected, fraction)?.let { scrubPointAt(projected, readings, it) }

/**
 * The recorded index [step] places from [current], or the first/last when nothing is being read (B213).
 *
 * Stops at the ends rather than wrapping: a reader on the last reading who asks for the next one stays where
 * they are. Null only when the figure has nothing recorded at all.
 */
internal fun recordedNeighbour(projected: List<ChartPoint>, current: Int?, step: Int): Int? {
    val recorded = projected.indices.filter { projected[it].value != null }
    return when {
        recorded.isEmpty() -> null
        current == null -> if (step > 0) recorded.first() else recorded.last()
        else -> (
            if (step > 0) {
                recorded.firstOrNull { it > current }
            } else {
                recorded.lastOrNull { it < current }
            }
            ) ?: current
    }
}

/**
 * The date and value a finger on the figure is over (N200, B208).
 *
 * A real line of text beside the figure rather than paint on the canvas: the canvas is blanked for a screen
 * reader, and this is the one part of touching it that can be read.
 */
@Composable
internal fun ScrubReadout(point: ScrubPoint, metric: MetricEntry) {
    Text(
        text = HistoryFormat.date(point.at, zone = ZoneId.systemDefault()) + " \u00b7 " +
            metric.unit.format(point.value),
        style = MaterialTheme.typography.labelMedium,
        // Announced as it changes (N206): a value that only exists for a sighted finger is a value half the
        // readers of this screen cannot have.
        modifier = Modifier
            .testTag(TestTags.Statistics.SCRUB_READOUT)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
