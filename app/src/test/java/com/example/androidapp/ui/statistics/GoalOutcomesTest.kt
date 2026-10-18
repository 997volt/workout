package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.repository.GoalPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * What became of each period of a goal (ROADMAP N137).
 *
 * The rules the entry settles, one test each: a rate is judged on the trend sitting in its band, a target on a
 * reading reaching it, maintenance on the weight staying in its corridor — and a period nobody weighed in during
 * is **unjudged** rather than missed.
 */
class GoalOutcomesTest {

    private val start = Instant.parse("2026-01-01T08:00:00Z")

    private fun readings(vararg pairs: Pair<Long, Double?>) = pairs.map { (day, value) ->
        MetricReading(start.plus(Duration.ofDays(day)), value)
    }

    private fun period(day: Long?, kind: String?, value: Double?) = GoalPeriod(
        startedAt = day?.let { start.plus(Duration.ofDays(it)) },
        kind = kind,
        value = value,
    )

    /** Eight weekly weigh-ins from 82 kg, falling [perWeek] grams a week — N98's floor, covered. */
    private fun weekly(perWeek: Double) = readings(
        *((0L..7L).map { week -> (week * 7) to (82_000.0 + perWeek * week) }).toTypedArray(),
    )

    @Test
    fun aTarget_isMet_whenAReadingReachedIt_withinTolerance() {
        val outcomes = goalOutcomes(
            listOf(period(null, "TARGET", 80_000.0)),
            readings(0L to 82_000.0, 7L to 81_500.0, 14L to 80_400.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Met)
    }

    @Test
    fun aTarget_thatWasNeverReached_isMissed() {
        val outcomes = goalOutcomes(
            listOf(period(null, "TARGET", 75_000.0)),
            readings(0L to 82_000.0, 7L to 81_500.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aLoseGoal_isMet_whenTheTrendSatInItsBand() {
        // 82 kg falling 533 g a week is the loss band's centre (0.65% of the body), so the rate is met.
        val outcomes = goalOutcomes(listOf(period(null, "LOSE", null)), weekly(perWeek = -533.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Met)
    }

    @Test
    fun aGainGoal_isMet_whenTheTrendSatInItsOwnBand() {
        // 82 kg rising 287 g a week is the gain target (0.35% of the body), inside 0.25–0.5%.
        val outcomes = goalOutcomes(listOf(period(null, "GAIN", null)), weekly(perWeek = +287.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Met)
    }

    @Test
    fun aLoseGoal_isMissed_whenTheTrendWentTheOtherWay() {
        // Inside the loss band as a magnitude and the opposite of what was asked — the half a magnitude-only
        // check would call met.
        val outcomes = goalOutcomes(listOf(period(null, "LOSE", null)), weekly(perWeek = +533.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aGainGoal_isMissed_whenTheTrendWentTheOtherWay() {
        val outcomes = goalOutcomes(listOf(period(null, "GAIN", null)), weekly(perWeek = -287.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aLoseGoal_isMissed_whenTheRateWasTooFast() {
        // Above the loss band's top: 0.8% of 82 kg is 656 g a week.
        val outcomes = goalOutcomes(listOf(period(null, "LOSE", null)), weekly(perWeek = -900.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aGainGoal_isMissed_whenTheRateWasTooFast() {
        // Above the gain band's top: 0.5% of 82 kg is 410 g a week.
        val outcomes = goalOutcomes(listOf(period(null, "GAIN", null)), weekly(perWeek = +500.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aRateGoal_withTooFewWeighIns_isUnjudged_ratherThanMissed() {
        // N98's floor, kept: three readings are a number and not a trend, and a verdict about the past must not
        // be noise either.
        val outcomes = goalOutcomes(
            listOf(period(null, "LOSE", null)),
            readings(0L to 82_000.0, 7L to 81_500.0, 14L to 81_000.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Unjudged)
    }

    @Test
    fun maintenance_isMet_whenTheWeightStayedInItsCorridor() {
        val outcomes = goalOutcomes(
            listOf(period(null, "MAINTAIN", 80_000.0)),
            readings(0L to 80_400.0, 7L to 80_600.0, 14L to 80_500.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Met)
    }

    @Test
    fun maintenance_isMissed_whenTheWeightLeftIt() {
        val outcomes = goalOutcomes(
            listOf(period(null, "MAINTAIN", 80_000.0)),
            readings(0L to 80_400.0, 7L to 82_500.0, 14L to 83_000.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Missed)
    }

    @Test
    fun aPeriodOfNoGoal_isNotAMiss() {
        // The row a *Clear* writes: a stretch of time the lifter was not asking for anything.
        val outcomes = goalOutcomes(listOf(period(null, null, null)), weekly(perWeek = 0.0))

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.NoGoal)
    }

    @Test
    fun aPeriodNobodyWeighedIn_isUnjudged() {
        val outcomes = goalOutcomes(
            listOf(period(20L, "TARGET", 80_000.0)),
            readings(0L to 82_000.0, 7L to 81_000.0),
        )

        assertThat(outcomes.single().outcome).isEqualTo(GoalOutcome.Unjudged)
    }

    @Test
    fun eachPeriod_isJudgedOnItsOwnStretch_ratherThanTheWholeSeries() {
        // The point of periods: March's goal is judged on March, and a target reached in April does not answer
        // for it.
        val outcomes = goalOutcomes(
            listOf(
                period(null, "TARGET", 75_000.0),
                period(28L, "TARGET", 80_000.0),
            ),
            readings(0L to 82_000.0, 7L to 81_000.0, 28L to 80_400.0, 35L to 80_000.0),
        )

        assertThat(outcomes.map { it.outcome })
            .containsExactly(GoalOutcome.Missed, GoalOutcome.Met)
            .inOrder()
    }
}
