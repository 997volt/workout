package com.example.androidapp.ui.statistics

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneId
import com.example.androidapp.ui.components.ChartPoint

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
 * Reports where a finger is across the chart, and null when it lifts (N200).
 *
 * A tap and a drag are the same gesture here — the finger lands and the nearest reading is named — so this
 * awaits the pointer's own stream rather than composing two detectors that would disagree about a zero-distance
 * drag.
 */
internal fun Modifier.scrubbable(onScrub: (Float?) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onScrub(down.fraction(size.width))
        down.consume()
        var pressed = true
        while (pressed) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            pressed = change?.pressed == true
            if (change != null && pressed) {
                onScrub(change.fraction(size.width))
                change.consume()
            }
        }
        onScrub(null)
    }
}

/** How far across the chart the pointer is, as the fraction the projection uses. */
private fun PointerInputChange.fraction(width: Int): Float =
    (position.x / width.toFloat()).coerceIn(0f, 1f)

/**
 * The date and value a finger on the figure is over (N200).
 *
 * A real line of text beside the figure rather than paint on the canvas: the canvas is blanked for a screen
 * reader, and this is the one part of touching it that can be read.
 */
@Composable
internal fun ScrubReadout(reading: MetricReading, metric: MetricEntry) {
    Text(
        text = HistoryFormat.date(reading.at, zone = ZoneId.systemDefault()) + " \u00b7 " +
            metric.unit.format(reading.value ?: 0.0),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.testTag(TestTags.Statistics.SCRUB_READOUT),
    )
}
