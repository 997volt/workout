package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * The weight goal and the rate it sets (ROADMAP N135).
 *
 * The rule this file holds: the rate is **derived** from what the lifter stated and the weight they are now,
 * so there is no second number to disagree with the first.
 */
class WeightGoalTest {

    @Test
    fun aTarget_takesItsDirectionFromWhereTheLifterIsNow() {
        val losing = WeightGoal.Target(weightGrams = 75_000L).ratePerWeekGrams(currentWeightGrams = 80_000L)
        val gaining = WeightGoal.Target(weightGrams = 85_000L).ratePerWeekGrams(currentWeightGrams = 80_000L)

        assertThat(losing).isLessThan(0.0)
        assertThat(gaining).isGreaterThan(0.0)
        // N149: gaining is the slower direction, so the two magnitudes are deliberately not equal.
        assertThat(abs(gaining!!)).isLessThan(abs(losing!!))
    }

    @Test
    fun theRate_isAShareOfBodyweight_insideItsOwnDirectionBand() {
        // 80 kg: losing is held to 0.5–0.8% of the body a week and asks for 0.65%, its band's centre.
        val rate = WeightGoal.Direction(gain = false).ratePerWeekGrams(currentWeightGrams = 80_000L)!!

        val band = weightRateBandPerWeekGrams(80_000L, gain = false)
        assertThat(-rate).isWithin(0.001).of((band.start + band.endInclusive) / 2)
        assertThat(-rate).isWithin(0.001).of(520.0)
        assertThat(-rate).isAtLeast(band.start)
        assertThat(-rate).isAtMost(band.endInclusive)
    }

    @Test
    fun aGainGoal_hasItsOwnBandAndTarget() {
        // 80 kg: gaining is held to 0.25–0.5% a week and asks for 0.35%.
        val rate = WeightGoal.Direction(gain = true).ratePerWeekGrams(currentWeightGrams = 80_000L)!!

        val band = weightRateBandPerWeekGrams(80_000L, gain = true)
        assertThat(rate).isWithin(0.001).of(280.0)
        assertThat(rate).isAtLeast(band.start)
        assertThat(rate).isAtMost(band.endInclusive)
    }

    @Test
    fun aTarget_usesTheRateForTheSideItIsOn() {
        val below = WeightGoal.Target(75_000L).ratePerWeekGrams(80_000L)!!
        val above = WeightGoal.Target(85_000L).ratePerWeekGrams(80_000L)!!

        assertThat(below).isWithin(0.001).of(-520.0)
        assertThat(above).isWithin(0.001).of(280.0)
    }

    @Test
    fun theSameGoal_scalesWithTheBodyItBelongsTo() {
        // The shape the reference states: a share, not a fixed number of kilograms.
        val small = WeightGoal.Direction(gain = false).ratePerWeekGrams(50_000L)!!
        val large = WeightGoal.Direction(gain = false).ratePerWeekGrams(100_000L)!!

        assertThat(abs(large)).isEqualTo(abs(small) * 2)
    }

    @Test
    fun aTargetAlreadyMet_asksForNoMovement() {
        assertThat(WeightGoal.Target(80_000L).ratePerWeekGrams(80_000L)).isEqualTo(0.0)
    }

    @Test
    fun maintenance_asksForNoRate_atAll() {
        // N135: a corridor is what a maintenance goal is; a literal 0.0 kg a week is broken by the first
        // glass of water, so it is not a rate the app will state.
        assertThat(WeightGoal.Maintain(80_000L).ratePerWeekGrams(80_000L)).isNull()
    }

    @Test
    fun withNoWeightToScaleBy_thereIsNoRate() {
        // A share of nothing is not a rate, and zero would be a number the app made up.
        assertThat(WeightGoal.Direction(gain = false).ratePerWeekGrams(0L)).isNull()
    }

    @Test
    fun aDirectionOnlyGoal_statesNoWeight() {
        assertThat(WeightGoal.Direction(gain = true).statedWeightGrams()).isNull()
        assertThat(WeightGoal.Target(80_000L).statedWeightGrams()).isEqualTo(80_000L)
        assertThat(WeightGoal.Maintain(80_000L).statedWeightGrams()).isEqualTo(80_000L)
    }

    @Test
    fun aStoredValueWithNoKind_isATarget() {
        // Every goal written before kinds existed was one number, which is exactly a target.
        assertThat(weightGoalOf(kind = null, valueGrams = 80_000.0)).isEqualTo(WeightGoal.Target(80_000L))
    }

    @Test
    fun theKind_roundTripsThroughItsName() {
        val goals = listOf(
            WeightGoal.Target(80_000L),
            WeightGoal.Direction(gain = false),
            WeightGoal.Direction(gain = true),
            WeightGoal.Maintain(80_000L),
        )

        goals.forEach { goal ->
            val stored = goal.statedWeightGrams()?.toDouble()
            assertThat(weightGoalOf(kind = goal.kind().name, valueGrams = stored)).isEqualTo(goal)
        }
    }

    @Test
    fun aKindThisBuildDoesNotKnow_isNoGoal_ratherThanAGuess() {
        // A build that cannot honour a kind must not invent a target from it (the enum-by-name rule).
        assertThat(weightGoalOf(kind = "RECOMP", valueGrams = 80_000.0)).isNull()
    }

    @Test
    fun aValueKindWithNoValue_statesNothing() {
        assertThat(weightGoalOf(kind = WeightGoalKind.TARGET.name, valueGrams = null)).isNull()
        assertThat(weightGoalOf(kind = WeightGoalKind.MAINTAIN.name, valueGrams = null)).isNull()
    }
}
