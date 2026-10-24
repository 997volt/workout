package com.example.androidapp.ui.statistics

import androidx.compose.ui.geometry.Offset
import com.example.androidapp.ui.components.ChartPoint
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * Which reading a finger on the figure is over (N200).
 *
 * A pure function rather than arithmetic inside the gesture, for the reason every other chart calculation is
 * pure here: a nearest point is wrong invisibly, and a touch test can only say that *something* was read.
 */
class ChartScrubTest {

    private val projected = listOf(
        ChartPoint(x = 0f, value = 83_000.0),
        ChartPoint(x = 0.5f, value = null),
        ChartPoint(x = 1f, value = 82_000.0),
    )

    private val readings = listOf(
        MetricReading(Instant.parse("2026-10-01T08:00:00Z"), 83_000.0),
        MetricReading(Instant.parse("2026-10-02T08:00:00Z"), null),
        MetricReading(Instant.parse("2026-10-03T08:00:00Z"), 82_000.0),
    )

    @Test
    fun theNearestRecordedPoint_isTheAnswer() {
        assertThat(nearestReadingIndex(projected, 0.05f)).isEqualTo(0)
        assertThat(nearestReadingIndex(projected, 0.9f)).isEqualTo(2)
    }

    @Test
    fun aGapIsNeverTheAnswer() {
        // The middle point is a reading with no value: the finger half way across is nearest to it, and what it
        // must read is the last thing that was recorded rather than "nothing was". Equidistant from both ends,
        // the earlier reading wins — the tie is pinned here rather than accepted either way (B216).
        assertThat(nearestReadingIndex(projected, 0.5f)).isEqualTo(0)
    }

    @Test
    fun aChartWithNothingRecorded_hasNoAnswer() {
        val empty = listOf(ChartPoint(x = 0.5f, value = null))
        assertThat(nearestReadingIndex(empty, 0.5f)).isNull()
    }

    @Test
    fun thePointCarriesTheReadingItsPlaceNames() {
        // B208: the state is the reading, not the index, so what the readout says and what the marker draws
        // come from the same value.
        val point = scrubPoint(projected, readings, 0.9f)

        assertThat(point?.index).isEqualTo(2)
        assertThat(point?.x).isEqualTo(1f)
        assertThat(point?.at).isEqualTo(Instant.parse("2026-10-03T08:00:00Z"))
        assertThat(point?.value).isEqualTo(82_000.0)
    }

    @Test
    fun aSeriesShorterThanTheFigure_hasNoPointPastItsEnd() {
        // B208: a range or metric can change under a held finger. The old read read the new series at the old
        // index; there is no point when the two do not line up.
        assertThat(scrubPoint(projected, readings.take(1), 0.9f)).isNull()
    }

    @Test
    fun aDragInsideTheSlop_isNotYetEitherGesture() {
        // B211: nothing is consumed while the direction is undecided, so the screen can still take the drag.
        assertThat(scrubDirection(Offset(2f, 2f), slop = 8f)).isNull()
    }

    @Test
    fun aVerticalDragBelongsToTheScreen_andAHorizontalOneToTheFigure() {
        assertThat(scrubDirection(Offset(2f, 40f), slop = 8f)).isFalse()
        assertThat(scrubDirection(Offset(40f, 2f), slop = 8f)).isTrue()
    }

    @Test
    fun aDiagonalDragTakesTheAxisItMovedFurther() {
        assertThat(scrubDirection(Offset(40f, 30f), slop = 8f)).isTrue()
        assertThat(scrubDirection(Offset(30f, 40f), slop = 8f)).isFalse()
    }

    @Test
    fun steppingMovesBetweenRecordedReadings_andStopsAtTheEnds() {
        // B213: the two actions a screen reader uses instead of a finger. The gap is stepped over, and the
        // ends hold rather than wrapping.
        assertThat(recordedNeighbour(projected, current = null, step = 1)).isEqualTo(0)
        assertThat(recordedNeighbour(projected, current = 0, step = 1)).isEqualTo(2)
        assertThat(recordedNeighbour(projected, current = 2, step = 1)).isEqualTo(2)
        assertThat(recordedNeighbour(projected, current = 2, step = -1)).isEqualTo(0)
        assertThat(recordedNeighbour(projected, current = null, step = -1)).isEqualTo(2)
    }
}
