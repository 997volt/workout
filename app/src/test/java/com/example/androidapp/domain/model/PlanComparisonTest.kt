package com.example.androidapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan versus actual (ROADMAP N20).
 *
 * The roadmap's own example is the first case: *prescribed 6×2 at 92.2, performed 6×2 at
 * 92.2, top single 2.5 over plan.*
 */
class PlanComparisonTest {

    // The id is what matches the two sides (B20), so these fixtures give each name an id and
    // the collapse tests below give two rows the *same* name with different ids.
    private fun planned(name: String, vararg sets: PlannedSetSpec) =
        ExercisePlan(exerciseId = name.lowercase().replace(" ", "-"), name = name, sets = sets.toList())

    private fun performed(name: String, vararg sets: PerformedSetSpec) =
        ExerciseActual(exerciseId = name.lowercase().replace(" ", "-"), name = name, sets = sets.toList())

    private fun working(weightGrams: Long?, reps: Int) =
        PlannedSetSpec(role = SetType.NORMAL, weightGrams = weightGrams, maxReps = reps)

    private fun did(
        weightGrams: Long,
        reps: Int,
        role: SetType = SetType.NORMAL,
        rpeHalves: Int? = null,
        durationSeconds: Int? = null,
    ) = PerformedSetSpec(
        role = role,
        weightGrams = weightGrams,
        reps = reps,
        rpeHalves = rpeHalves,
        durationSeconds = durationSeconds,
    )

    /** A plan that names the one effort N59 gives an exercise, with no load to confuse the question. */
    private fun plannedAt(name: String, targetRpeHalves: Int?, vararg sets: PlannedSetSpec) =
        ExercisePlan(
            exerciseId = name.lowercase().replace(" ", "-"),
            name = name,
            sets = sets.toList(),
            targetRpeHalves = targetRpeHalves,
        )

    @Test
    fun theEffort_isTheWorkingSetsMean_notTheHeaviestSetsRating() {
        // ROADMAP N131. Prescribed 8; the sets were 8, 8 and 9 — a mean of 8.33, rounded to the
        // half-point the scale can express, 8.5. The heaviest set's own rating would have been 9,
        // which answers a different question: what the top set felt like, not whether the work
        // landed where the plan put it.
        val comparison = comparePlanToActual(
            planned = listOf(
                plannedAt("Squat", targetRpeHalves = 16, working(100_000L, 5)),
            ),
            performed = listOf(
                performed(
                    "Squat",
                    did(100_000L, 5, rpeHalves = 16),
                    did(95_000L, 5, rpeHalves = 16),
                    did(90_000L, 5, rpeHalves = 18),
                ),
            ),
        ).single()

        assertEquals(16, comparison.prescribedRpeHalves)
        assertEquals("(16 + 16 + 18) / 3 = 16.67 halves, rounded to 17", 17, comparison.performedAverageRpeHalves)
        assertEquals(1, comparison.rpeDeltaHalves)
        assertFalse(comparison.matchedEffort)
    }

    @Test
    fun anUnratedSet_isNotAZeroInTheMean() {
        // RPE is skippable (N6): a set nobody rated must not pull the average down as if it had been
        // "no effort". Prescribed 8, two rated sets at 8 and 9 — 8.5, not (8 + 9 + 0) / 3.
        val comparison = comparePlanToActual(
            planned = listOf(plannedAt("Squat", targetRpeHalves = 16, working(100_000L, 5))),
            performed = listOf(
                performed(
                    "Squat",
                    did(100_000L, 5, rpeHalves = 16),
                    did(100_000L, 5, rpeHalves = 18),
                    did(100_000L, 5),
                ),
            ),
        ).single()

        assertEquals(17, comparison.performedAverageRpeHalves)
        assertEquals(1, comparison.rpeDeltaHalves)
    }

    @Test
    fun aWarmUpsRating_isNotTheWorksEffort() {
        // N67: a warm-up records no effort, so one that somehow carries a rating is ignored here for
        // the same reason it is excluded from the rep and weight sums.
        val comparison = comparePlanToActual(
            planned = listOf(plannedAt("Squat", targetRpeHalves = 16, working(100_000L, 5))),
            performed = listOf(
                performed(
                    "Squat",
                    did(60_000L, 5, role = SetType.WARMUP, rpeHalves = 20),
                    did(100_000L, 5, rpeHalves = 16),
                ),
            ),
        ).single()

        assertEquals("only the working set counts", 16, comparison.performedAverageRpeHalves)
        assertTrue(comparison.matchedEffort)
    }

    @Test
    fun effortIsNull_ratherThanZero_whenEitherSideSaysNothing() {
        // The rule every nullable field here follows: a difference from nothing is not a comparison,
        // and a zero would read as "matched".
        val noTarget = comparePlanToActual(
            planned = listOf(plannedAt("Squat", targetRpeHalves = null, working(100_000L, 5))),
            performed = listOf(performed("Squat", did(100_000L, 5, rpeHalves = 16))),
        ).single()
        assertNull("the plan named no effort", noTarget.rpeDeltaHalves)
        assertNull(noTarget.prescribedRpeHalves)
        assertEquals("but what was done is still reported", 16, noTarget.performedAverageRpeHalves)

        val noRating = comparePlanToActual(
            planned = listOf(plannedAt("Squat", targetRpeHalves = 16, working(100_000L, 5))),
            performed = listOf(performed("Squat", did(100_000L, 5))),
        ).single()
        assertNull("nothing was rated", noRating.rpeDeltaHalves)
        assertNull(noRating.performedAverageRpeHalves)
        assertEquals(16, noRating.prescribedRpeHalves)
    }

    @Test
    fun aHold_isCarriedAsAHold_ratherThanAsZeroReps() {
        // B243: reps are zero by definition, so the review read "3×0" for a timed movement. The comparison
        // keeps the plan's target as written and carries the hold the exercise actually performed.
        val comparison = comparePlanToActual(
            planned = listOf(planned("Carry", working(weightGrams = 100_000L, reps = 5))),
            performed = listOf(performed("Carry", did(100_000L, reps = 0, durationSeconds = 45))),
        ).single()

        assertEquals(45, comparison.performedHoldSeconds)
        assertEquals("the plan's target stays what the plan wrote", 5, comparison.prescribedReps)
        assertEquals(0, comparison.performedReps)
    }

    @Test
    fun anImprovisedExercise_hasNoEffortTargetToCompare() {
        // It was not in the plan, so the plan named no effort for it — the same reason its prescribed
        // line is absent.
        val comparison = comparePlanToActual(
            planned = emptyList(),
            performed = listOf(performed("Curl", did(30_000L, 10, rpeHalves = 16))),
        ).single()

        assertFalse(comparison.wasPlanned)
        assertNull(comparison.prescribedRpeHalves)
        assertNull(comparison.rpeDeltaHalves)
    }

    @Test
    fun aPlanAnsweredExactly_reportsNoDelta() {
        val comparison = comparePlanToActual(
            planned = listOf(planned("Bench Press", working(92_200L, 2), working(92_200L, 2))),
            performed = listOf(performed("Bench Press", did(92_200L, 2), did(92_200L, 2))),
        ).single()

        assertEquals(2, comparison.prescribedSets)
        assertEquals(2, comparison.performedSets)
        assertEquals(92_200L, comparison.prescribedTopWeightGrams)
        assertEquals(92_200L, comparison.performedTopWeightGrams)
        assertEquals(0L, comparison.topSetDeltaGrams)
        assertTrue(comparison.matchedPlan)
    }

    @Test
    fun goingHeavierThanThePlan_readsAsAPositiveDelta() {
        // "top single 2.5 over plan": the plan asked for 90, the lifter put up 92.5.
        val comparison = comparePlanToActual(
            planned = listOf(planned("Deadlift", working(90_000L, 1))),
            performed = listOf(performed("Deadlift", did(92_500L, 1))),
        ).single()

        assertEquals(2_500L, comparison.topSetDeltaGrams)
        assertFalse(comparison.matchedPlan)
    }

    @Test
    fun aWarmUpNeverStandsInForTheTopSet() {
        // The reason N17 excludes warm-ups applies here too: a 60 kg warm-up must not be
        // read as the day's top set, nor a planned warm-up as the prescription.
        val comparison = comparePlanToActual(
            planned = listOf(
                planned(
                    "Squat",
                    PlannedSetSpec(role = SetType.WARMUP, weightGrams = 60_000L, maxReps = 5),
                    working(120_000L, 3),
                ),
            ),
            performed = listOf(
                performed("Squat", did(60_000L, 5, SetType.WARMUP), did(122_500L, 3)),
            ),
        ).single()

        assertEquals(122_500L, comparison.performedTopWeightGrams)
        assertEquals(120_000L, comparison.prescribedTopWeightGrams)
        assertEquals(2_500L, comparison.topSetDeltaGrams)
        // B21 changed this: the counts exclude warm-ups, as the rep sums always did. Counting
        // them here while dropping them there is what made "prescribed 2×3" mean two sets, one a
        // warm-up, totalling three reps.
        assertEquals("only the working set counts", 1, comparison.performedSets)
        assertEquals("on both sides", 1, comparison.prescribedSets)
    }

    @Test
    fun aPlanWithoutAWeight_leavesTheDeltaUnknown_ratherThanZero() {
        // Null is "nothing to compare", and a zero here would claim the lifter matched a
        // plan that never named a weight.
        val comparison = comparePlanToActual(
            planned = listOf(planned("Pull-Up", PlannedSetSpec(role = SetType.NORMAL, maxReps = 8))),
            performed = listOf(performed("Pull-Up", did(0L, 8))),
        ).single()

        assertNull(comparison.topSetDeltaGrams)
        assertNull(comparison.prescribedTopWeightGrams)
        assertFalse(comparison.matchedPlan)
    }

    @Test
    fun anExerciseAddedMidWorkout_isNotPresentedAsAPlan() {
        val comparisons = comparePlanToActual(
            planned = listOf(planned("Bench Press", working(80_000L, 5))),
            performed = listOf(
                performed("Bench Press", did(80_000L, 5)),
                performed("Face Pull", did(25_000L, 15)),
            ),
        )

        assertEquals(2, comparisons.size)
        assertEquals("the plan is answered first", "Bench Press", comparisons[0].name)
        val improvised = comparisons[1]
        assertEquals("Face Pull", improvised.name)
        assertFalse("nothing was prescribed for it", improvised.wasPlanned)
        assertEquals(0, improvised.prescribedSets)
        assertEquals(1, improvised.performedSets)
    }

    @Test
    fun aPlannedExerciseThatWasNeverTouched_stillAppears() {
        // The plan is the thing being answered, so an exercise skipped entirely is a fact
        // about the session and must not vanish from the review.
        val comparisons = comparePlanToActual(
            planned = listOf(planned("Squat", working(100_000L, 5))),
            performed = emptyList(),
        ).single()

        assertTrue(comparisons.wasPlanned)
        assertEquals(1, comparisons.prescribedSets)
        assertEquals(0, comparisons.performedSets)
        assertNull(comparisons.performedTopWeightGrams)
    }

    @Test
    fun aPlanWithNoRepCeiling_leavesTheRepsUnknown_ratherThanZero() {
        // ROADMAP B19: the guard tested the input list, so working sets naming no reps produced
        // an empty sum with a truthy guard — "prescribed 2×0" on screen, against this file's own
        // promise that a field with nothing to say is null.
        val comparison = comparePlanToActual(
            planned = listOf(
                planned(
                    "Back Squat",
                    PlannedSetSpec(role = SetType.NORMAL, weightGrams = 100_000L),
                    PlannedSetSpec(role = SetType.NORMAL, weightGrams = 100_000L),
                ),
            ),
            performed = listOf(performed("Back Squat", did(100_000L, 5))),
        ).single()

        assertNull("nothing was prescribed to count", comparison.prescribedReps)
    }

    @Test
    fun theSameExerciseTwice_keepsBothRowsApart() {
        // ROADMAP B20: keying by display name collapsed two rows of one movement — last one won —
        // so the earlier row's sets vanished from the review while the totals still counted them,
        // making the summary contradict itself.
        val comparisons = comparePlanToActual(
            planned = emptyList(),
            performed = listOf(
                ExerciseActual("back-squat", "Back Squat", listOf(did(100_000L, 5))),
                ExerciseActual("back-squat-top", "Back Squat", listOf(did(110_000L, 1))),
            ),
        )

        assertEquals("both are reported", 2, comparisons.size)
        assertEquals("with their own sets", 1, comparisons[1].performedSets)
        assertEquals(110_000L, comparisons[1].performedTopWeightGrams)
    }

    @Test
    fun theCounts_excludeWarmUps_asTheRepsDo() {
        // ROADMAP B21: "prescribed 2×3" meant two sets, one a warm-up, totalling three reps.
        val comparison = comparePlanToActual(
            planned = listOf(
                planned(
                    "Bench Press",
                    PlannedSetSpec(role = SetType.WARMUP, weightGrams = 40_000L, maxReps = 10),
                    PlannedSetSpec(role = SetType.NORMAL, weightGrams = 80_000L, maxReps = 3),
                ),
            ),
            performed = listOf(
                performed("Bench Press", did(40_000L, 10, SetType.WARMUP), did(80_000L, 3)),
            ),
        ).single()

        assertEquals("one working set each side", 1, comparison.prescribedSets)
        assertEquals(1, comparison.performedSets)
        assertEquals("and three reps, which now agrees", 3, comparison.prescribedReps)
        assertEquals(3, comparison.performedReps)
    }
}
