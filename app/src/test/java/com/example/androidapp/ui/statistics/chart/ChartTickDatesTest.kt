package com.example.androidapp.ui.statistics.chart

import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.BodyMetric
import com.example.androidapp.ui.statistics.MetricKey
import com.example.androidapp.ui.statistics.MetricReading
import com.example.androidapp.ui.statistics.MetricSeries
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * The time axis's three labels (ROADMAP B282).
 *
 * They were built from the first and last reading that *recorded* something while the plot projected every
 * reading, so a series ending in a gap named a day that was not where the axis ended. One span now.
 */
class ChartTickDatesTest {

    @Test
    fun theLastLabel_isWhereTheAxisEnds_evenWhenThatReadingIsAGap() {
        val first = Instant.parse("2026-01-01T12:00:00Z")
        val recorded = Instant.parse("2026-01-11T12:00:00Z")
        val gap = Instant.parse("2026-01-21T12:00:00Z")
        val series = MetricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            readings = listOf(
                MetricReading(first, 80_000.0),
                MetricReading(recorded, 79_000.0),
                MetricReading(gap, null),
            ),
        )

        val dates = tickDates(series)

        assertThat(dates).hasSize(3)
        assertThat(dates.last()).isEqualTo(HistoryFormat.date(gap, zone = ZoneId.systemDefault()))
        assertThat(dates.last()).isNotEqualTo(HistoryFormat.date(recorded, zone = ZoneId.systemDefault()))
    }
}
