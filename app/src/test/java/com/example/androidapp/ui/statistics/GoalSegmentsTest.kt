package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.statistics.chart.goalSegments
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Each period's target as a stretch of the chart (ROADMAP N137).
 *
 * The thing the segments exist to stop: one line drawn across a range it never applied to.
 */
class GoalSegmentsTest {

    private val start = Instant.parse("2026-01-01T08:00:00Z")

    /** Ten daily readings, which is the window everything below is placed in. */
    private val series = MetricSeries(
        key = MetricKey.Body(BodyMetric.WEIGHT),
        readings = (0L..9L).map { day ->
            MetricReading(start.plus(Duration.ofDays(day)), 80_000.0 + day)
        },
    )

    private fun period(day: Long?, value: Double?) = GoalPeriod(
        startedAt = day?.let { start.plus(Duration.ofDays(it)) },
        kind = "TARGET",
        value = value,
    )

    @Test
    fun aPeriodOpenToTheEnd_isDrawnFromItsStart_toTheRightEdge() {
        val segments = goalSegments(series, listOf(period(5L, 80_000.0)))

        assertThat(segments).hasSize(1)
        assertThat(segments.single().fromX).isWithin(0.001f).of(5f / 9f)
        assertThat(segments.single().toX).isEqualTo(1f)
        assertThat(segments.single().value).isEqualTo(80_000.0)
    }

    @Test
    fun eachPeriod_endsWhereTheNextBegins() {
        val segments = goalSegments(
            series,
            listOf(period(null, 78_000.0), period(5L, 80_000.0)),
        )

        assertThat(segments.map { it.fromX to it.toX }).isEqualTo(
            listOf(0f to 5f / 9f, 5f / 9f to 1f),
        )
    }

    @Test
    fun anUndatedPeriod_startsAtTheLeftEdge_ratherThanInventingADate() {
        val segments = goalSegments(series, listOf(period(null, 78_000.0)))

        assertThat(segments.single().fromX).isEqualTo(0f)
    }

    @Test
    fun aPeriodEntirelyOutsideTheWindow_drawsNothing() {
        // A goal from before the range on show is not a line pinned to the edge, which would claim it applied
        // to everything the chart shows.
        val segments = goalSegments(
            series,
            listOf(period(null, 78_000.0), period(40L, 80_000.0)),
        )

        assertThat(segments).hasSize(1)
    }

    @Test
    fun aDirectionGoal_drawsNoLine_atAll() {
        // It states no weight (N135), and a level invented from the band would be the end weight the app
        // refuses to manufacture.
        val direction = GoalPeriod(startedAt = null, kind = "LOSE", value = null)

        assertThat(goalSegments(series, listOf(direction))).isEmpty()
    }

    @Test
    fun aSeriesTooShortToHaveASpan_drawsNothing() {
        val single = MetricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            readings = listOf(MetricReading(start, 80_000.0)),
        )

        assertThat(goalSegments(single, listOf(period(null, 80_000.0)))).isEmpty()
    }
}
