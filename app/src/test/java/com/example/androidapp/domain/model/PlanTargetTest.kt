package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a plan prescribes for a lift, and what of it a chart can draw (ROADMAP N39).
 *
 * Two questions, and the second is the one that keeps the chart honest: a prescription is a set of numbers, and
 * only the level-shaped ones are a line.
 */
class PlanTargetTest {

    private fun planned(
        templateId: String = "t1",
        templateName: String = "Push day",
        weightGrams: Long? = null,
        assistanceGrams: Long? = null,
        reps: Int? = null,
        rpeHalves: Int? = null,
    ) = PlannedSetTarget(templateId, templateName, weightGrams, assistanceGrams, reps, rpeHalves)

    @Test
    fun theHeaviestSet_decidesTheWeight_andTheRepsThatGoWithIt() {
        // Two numbers from one row: a 1RM estimated from one set's weight and another's reps would be a lifter
        // the plan never described.
        val targets = planTargets(
            listOf(
                planned(weightGrams = 80_000L, reps = 8),
                planned(weightGrams = 100_000L, reps = 3),
                planned(weightGrams = 90_000L, reps = 5),
            ),
        )

        assertThat(targets.single().weightGrams).isEqualTo(100_000L)
        assertThat(targets.single().reps).isEqualTo(3)
    }

    @Test
    fun assistanceIsTheLeastPrescribed_becauseLessHelpIsHarderWork() {
        val targets = planTargets(
            listOf(planned(assistanceGrams = 30_000L), planned(assistanceGrams = 15_000L)),
        )

        assertThat(targets.single().assistanceGrams).isEqualTo(15_000L)
    }

    @Test
    fun theEffortIsTheHardestAskedFor() {
        val targets = planTargets(listOf(planned(rpeHalves = 14), planned(rpeHalves = 19)))

        assertThat(targets.single().rpeHalves).isEqualTo(19)
    }

    @Test
    fun oneEntryPerPlan_evenWhenTheLiftIsInTwo() {
        val targets = planTargets(
            listOf(
                planned(templateId = "t1", templateName = "Push day", weightGrams = 100_000L),
                planned(templateId = "t2", templateName = "Heavy day", weightGrams = 85_000L),
            ),
        )

        assertThat(targets.map { it.templateName }).containsExactly("Push day", "Heavy day")
    }

    @Test
    fun aPlanThatStatesNoWeight_stillPrescribesWhatItCan() {
        val targets = planTargets(listOf(planned(assistanceGrams = 20_000L, rpeHalves = 16)))

        assertThat(targets.single().weightGrams).isNull()
        assertThat(targets.single().rpeHalves).isEqualTo(16)
    }

    @Test
    fun aWeightTarget_drawsOnTheHeaviestSet() {
        val target = PlanTarget(
            "t1",
            "Push day",
            weightGrams = 100_000L,
            reps = 5,
            assistanceGrams = null,
            rpeHalves = null
        )

        assertThat(target.levelFor(ExerciseTrendMetric.HEAVIEST_SET)).isEqualTo(100_000.0)
    }

    @Test
    fun aWeightAndItsReps_drawAnEstimatedOneRepMax() {
        // 100 kg × 5 by Epley is 116.67 kg, rounded to the half-kilo the estimate is worth: 116.5.
        val target = PlanTarget(
            "t1",
            "Push day",
            weightGrams = 100_000L,
            reps = 5,
            assistanceGrams = null,
            rpeHalves = null
        )

        assertThat(target.levelFor(ExerciseTrendMetric.ESTIMATED_1RM)).isEqualTo(116_500.0)
    }

    @Test
    fun withoutReps_thereIsNoEstimatedMax_toDraw() {
        val target = PlanTarget(
            "t1",
            "Push day",
            weightGrams = 100_000L,
            reps = null,
            assistanceGrams = null,
            rpeHalves = null
        )

        assertThat(target.levelFor(ExerciseTrendMetric.ESTIMATED_1RM)).isNull()
        // The weight itself is still a level, even where the estimate is not.
        assertThat(target.levelFor(ExerciseTrendMetric.HEAVIEST_SET)).isEqualTo(100_000.0)
    }

    @Test
    fun anEffortInHalfPoints_drawsInTheUnitsTheSeriesCarries() {
        // The series averages RPE into points (ExerciseTrendPoint), so a line drawn at 19 would be double height.
        val target = PlanTarget(
            "t1",
            "Push day",
            weightGrams = null,
            reps = null,
            assistanceGrams = null,
            rpeHalves = 19
        )

        assertThat(target.levelFor(ExerciseTrendMetric.RPE)).isEqualTo(9.5)
    }

    @Test
    fun theSeriesWithNoLevel_drawNothing() {
        val target = PlanTarget("t1", "Push day", 100_000L, 5, 20_000L, 16)

        assertThat(target.levelFor(ExerciseTrendMetric.VOLUME)).isNull()
        assertThat(target.levelFor(ExerciseTrendMetric.TOTAL_REPS)).isNull()
        assertThat(target.levelFor(ExerciseTrendMetric.MUSCLE_FEEL)).isNull()
        assertThat(target.levelFor(ExerciseTrendMetric.JOINT_PAIN)).isNull()
    }
}
