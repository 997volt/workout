package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.model.ExerciseTrendMetric
import com.example.androidapp.domain.model.PlanTarget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The plans' targets as stretches of the chart (ROADMAP N39). */
class PlanSegmentsTest {

    private val start = Instant.parse("2026-01-01T08:00:00Z")

    private val series = MetricSeries(
        key = MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
        readings = (0L..9L).map { day ->
            MetricReading(start.plus(Duration.ofDays(day)), 90_000.0 + day)
        },
    )

    private fun plan(name: String, weight: Long?) = PlanTarget(
        templateId = name,
        templateName = name,
        weightGrams = weight,
        reps = 5,
        assistanceGrams = null,
        rpeHalves = null,
    )

    @Test
    fun aPlansTarget_spansTheWholeWindow_becauseAPrescriptionHasNoPeriod() {
        val segments = planSegments(
            series,
            listOf(plan("Push day", 100_000L)),
            MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
        )

        assertThat(segments).hasSize(1)
        assertThat(segments.single().fromX).isEqualTo(0f)
        assertThat(segments.single().toX).isEqualTo(1f)
        assertThat(segments.single().value).isEqualTo(100_000.0)
        assertThat(segments.single().planName).isEqualTo("Push day")
    }

    @Test
    fun twoPlansDrawTwoLines_eachNamed() {
        val segments = planSegments(
            series,
            listOf(plan("Push day", 100_000L), plan("Heavy day", 85_000L)),
            MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
        )

        assertThat(segments.map { it.planName }).containsExactly("Push day", "Heavy day")
    }

    @Test
    fun aPlanThatStatesNoWeightOnThisSeries_drawsNothing() {
        val segments = planSegments(
            series,
            listOf(plan("Push day", null)),
            MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
        )

        assertThat(segments).isEmpty()
    }

    @Test
    fun aSeriesAPlanDoesNotSpeakTo_drawsNothing() {
        // A plan prescribes sets, not a bodyweight or a tape measurement.
        val body = MetricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            readings = listOf(
                MetricReading(start, 80_000.0),
                MetricReading(start.plus(Duration.ofDays(1)), 79_900.0),
            ),
        )

        assertThat(planSegments(body, listOf(plan("Push day", 100_000L)), body.key)).isEmpty()
    }
}
