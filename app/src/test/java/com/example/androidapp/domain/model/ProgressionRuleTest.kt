package com.example.androidapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule both screens state about a plan's progression (P3.17, B181, N227).
 *
 * The rule is two-directional: a set below its range's ceiling earns a rep, and a set at the ceiling
 * earns the movement's weight step. The block used to print the weight step as though it were the only
 * one, and to call a plan with no weight unable to progress at all — both false for a 5–8 plan. These
 * cases pin the four shapes, the assisted set that has no added weight, and the warm-up that does not
 * decide the rule.
 *
 * It reads [progressionSets], which is the mapping both callers use — the plan editor maps its own sets and
 * the session already carries the plan's — so one translation and one predicate, rather than the duplicate
 * the template screen used to keep beside the offer's own.
 */
class ProgressionRuleTest {

    /** The rule as the plan editor asks it: the exercise's one effort, and the sets the rule reads. */
    private fun planned(exercise: TemplateExercise): ProgressionRule =
        progressionRuleFor(exercise.targetRpeHalves, exercise.progressionSets())

    private fun exercise(
        reps: Pair<Int?, Int?> = 5 to 5,
        weightGrams: Long? = 100_000L,
        rpeHalves: Int? = 16,
        role: SetType = SetType.NORMAL,
        assistanceGrams: Long? = null,
    ): TemplateExercise = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "back-squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = null,
        equipment = Equipment.BARBELL,
        targetRpeHalves = rpeHalves,
        sets = listOf(
            TemplateSet(
                id = "s1",
                templateExerciseId = "te1",
                setIndex = 1,
                role = role,
                targetWeightGrams = weightGrams,
                targetAssistanceGrams = assistanceGrams,
                targetRepsMin = reps.first,
                targetRepsMax = reps.second,
            ),
        ),
    )

    @Test
    fun withoutATargetRpe_theGateNeverOpens() {
        assertEquals(
            ProgressionRule.NO_TARGET_RPE,
            planned(exercise(reps = 5 to 8, rpeHalves = null)),
        )
    }

    @Test
    fun withoutReps_thereIsNothingToMeasureTheWorkAgainst() {
        assertEquals(
            ProgressionRule.NO_REPS,
            planned(exercise(reps = null to null)),
        )
    }

    @Test
    fun aRangeAndALoad_moveTheRepsFirst_thenTheWeight() {
        assertEquals(
            ProgressionRule.REPS_THEN_LOAD,
            planned(exercise(reps = 5 to 8)),
        )
    }

    @Test
    fun aRangeWithoutALoad_stopsAtTheCeiling() {
        assertEquals(
            ProgressionRule.REPS_ONLY,
            planned(exercise(reps = 5 to 8, weightGrams = null)),
        )
    }

    @Test
    fun aFixedRepTarget_leavesTheWeightAsTheOnlyStep() {
        assertEquals(
            ProgressionRule.LOAD_ONLY,
            planned(exercise(reps = 5 to 5)),
        )
    }

    @Test
    fun aFixedRepTargetAndNoLoad_hasNowhereToGo() {
        assertEquals(
            ProgressionRule.NOTHING_TO_MOVE,
            planned(exercise(reps = 5 to 5, weightGrams = null)),
        )
    }

    @Test
    fun anAssistedSet_statesNoLoadItCanRaise() {
        // -20 is 20 kg of help and no added weight (N15), so the weight step is not offered even
        // though the column is non-null (B181).
        assertEquals(
            ProgressionRule.NOTHING_TO_MOVE,
            planned(exercise(reps = 5 to 5, weightGrams = 0L, assistanceGrams = 20_000L)),
        )
    }

    @Test
    fun aWarmUpDoesNotDecideTheSentence() {
        // The ramp names a load and no reps, and a warm-up is not what progression measures (N17):
        // reading it would turn a bodyweight range plan into "reps, then the weight".
        val work = exercise(reps = 5 to 8, weightGrams = null)
        val withWarmUp = work.copy(
            sets = listOf(
                work.sets.single().copy(
                    id = "w1",
                    role = SetType.WARMUP,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = null,
                    targetRepsMax = null,
                ),
                work.sets.single().copy(id = "s1", role = SetType.NORMAL),
            ),
        )

        assertEquals(ProgressionRule.REPS_ONLY, planned(withWarmUp))
    }
}
