package com.example.androidapp.ui.statistics

import com.example.androidapp.ui.components.ChartPoint
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun theNearestRecordedPoint_isTheAnswer() {
        assertThat(nearestReadingIndex(projected, 0.05f)).isEqualTo(0)
        assertThat(nearestReadingIndex(projected, 0.9f)).isEqualTo(2)
    }

    @Test
    fun aGapIsNeverTheAnswer() {
        // The middle point is a reading with no value: the finger half way across is nearest to it, and what it
        // must read is the last thing that was recorded rather than "nothing was".
        assertThat(nearestReadingIndex(projected, 0.5f)).isIn(listOf(0, 2))
    }

    @Test
    fun aChartWithNothingRecorded_hasNoAnswer() {
        val empty = listOf(ChartPoint(x = 0.5f, value = null))
        assertThat(nearestReadingIndex(empty, 0.5f)).isNull()
    }
}
