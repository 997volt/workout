package com.example.androidapp.ui.statistics

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import java.time.Instant
import org.junit.Test

/**
 * The energy adjustment a weight trend implies (ROADMAP N98).
 *
 * The arithmetic is the entry's — `(target − observed) × 7700 ÷ 7`, in kilograms — and the two things worth
 * pinning beside it are the *floor* below which nothing is said at all, and the band, which is the fit's own
 * uncertainty rather than a decoration.
 */
class EnergyAdjustmentTest {

    private val start = Instant.parse("2026-09-01T08:00:00Z")

    /** Readings given as `day offset to grams`, so a weekly series and a daily one read the same way. */
    private fun series(vararg pairs: Pair<Long, Double?>) = MetricSeries(
        key = MetricKey.Body(BodyMetric.WEIGHT),
        readings = pairs.map { (day, value) -> MetricReading(start.plus(Duration.ofDays(day)), value) },
    )

    /** Eight weekly weigh-ins falling [perWeek] grams a week from 82 kg — the floor's own minimum. */
    private fun weekly(perWeek: Double) = series(
        *(0L..7L).map { week ->
            (week * 7) to (82_000.0 + perWeek * week)
        }.toTypedArray(),
    )

    @Test
    fun aQuarterKiloAWeekBehindTarget_isAboutTwoHundredAndSeventyFiveKcalADay() {
        // Losing 250 g a week against a target of 500 is 250 g a week short, and 0.25 kg × 7700 ÷ 7 = 275.
        val adjustment = weekly(perWeek = -250.0).energyAdjustment(targetPerWeekGrams = -500.0)!!

        assertThat(adjustment.lowKcal).isEqualTo(-275)
        assertThat(adjustment.highKcal).isEqualTo(-275)
        assertWithMessage("a shortfall means eating less").that(adjustment.eatsMore).isFalse()
        assertThat(adjustment.isOnTarget).isFalse()
    }

    @Test
    fun aTrendFasterThanTheTarget_asksForMoreRatherThanLess() {
        // Losing 500 g a week while only 100 was asked for is 400 g a week too fast: 440 kcal a day more.
        val adjustment = weekly(perWeek = -500.0).energyAdjustment(targetPerWeekGrams = -100.0)!!

        assertThat(adjustment.lowKcal).isEqualTo(440)
        assertThat(adjustment.eatsMore).isTrue()
    }

    @Test
    fun aTrendAtTheTarget_asksForNoChange() {
        val adjustment = weekly(perWeek = -500.0).energyAdjustment(targetPerWeekGrams = -500.0)!!

        assertThat(adjustment.isOnTarget).isTrue()
    }

    @Test
    fun sevenWeighIns_areNotEnoughToSayAnything() {
        // The floor is the entry's, and null is the answer rather than a figure drawn from noise.
        val seven = series(
            *(
                0L..6L
                ).map { week -> (week * 7) to (82_000.0 - week * 250) }.toTypedArray(),
        )

        assertThat(seven.readings).hasSize(7)
        assertThat(seven.energyAdjustment(targetPerWeekGrams = -500.0)).isNull()
    }

    @Test
    fun eightWeighInsInsideThreeWeeks_areStillNotEnough() {
        // The count is met and the *window* is not: a rate read off eight daily readings is a week of water.
        val daily = series(*(0L..7L).map { day -> day to (82_000.0 - day * 40) }.toTypedArray())

        assertThat(daily.readings).hasSize(8)
        assertThat(daily.energyAdjustment(targetPerWeekGrams = -500.0)).isNull()
    }

    @Test
    fun aScatteredSeries_reportsARangeRatherThanAPoint() {
        // The band is the fit's own residual, so a series that wobbles around its line must widen the answer.
        // A point estimate here would be the *plausible rather than true* that P2.8 is parked on.
        val scattered = series(
            *listOf(0L, 7L, 14L, 21L, 28L, 35L, 42L, 49L).mapIndexed { index, day ->
                val wobble = if (index % 2 == 0) 300.0 else -300.0
                day to (82_000.0 - index * 250.0 + wobble)
            }.toTypedArray(),
        )

        val adjustment = scattered.energyAdjustment(targetPerWeekGrams = -500.0)!!

        assertWithMessage("a band, not a point").that(adjustment.highKcal).isGreaterThan(adjustment.lowKcal)
        assertWithMessage("and the arithmetic's own figure is inside it")
            .that(adjustment.lowKcal <= -275 && adjustment.highKcal >= -275).isTrue()
    }

    @Test
    fun anUnweighedRow_doesNotCountTowardsTheFloor() {
        // A tape measurement with no weight is a row, not a reading: the floor counts what there is to fit.
        val rows = series(
            *listOf(0L to 82_000.0, 7L to null, 14L to 81_500.0).toTypedArray(),
        )

        assertThat(rows.energyAdjustment(targetPerWeekGrams = -500.0)).isNull()
    }

    @Test
    fun twoReadings_cannotReportHowFarOutTheyAre() {
        // The line through two points passes through both, so every residual is zero — a certainty the data
        // does not support, which is why the error is null rather than nought.
        val trend = series(0L to 82_000.0, 7L to 81_000.0).trend()!!

        assertThat(trend.perWeekStandardError).isNull()
    }
}
