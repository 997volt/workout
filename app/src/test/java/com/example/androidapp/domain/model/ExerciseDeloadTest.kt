package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The per-lift deload's arithmetic (ROADMAP N307).
 *
 * The trim is shared with the replace (N308), and both surfaces read this function, so the tests pin the
 * rule rather than a screen: the survivor counts, the three-step load, the assistance direction, the ranges,
 * and what is deliberately left alone (warm-ups, rungs, holds, bodyweight).
 */
class ExerciseDeloadTest {

    private fun set(
        id: String,
        index: Int,
        role: SetType = SetType.NORMAL,
        weight: Long? = 100_000L,
        assistance: Long? = null,
        repsMin: Int? = 5,
        repsMax: Int? = 8,
        repsCurrent: Int? = null,
        duration: Int? = null,
        dropValue: Long? = null,
    ) = TemplateSet(
        id = id,
        templateExerciseId = "te1",
        setIndex = index,
        role = role,
        targetWeightGrams = weight,
        targetAssistanceGrams = assistance,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetRepsCurrent = repsCurrent,
        dropValueGrams = dropValue,
        targetDurationSeconds = duration,
    )

    @Test
    fun theSurvivorCounts_areTheRuleForOneToTwelve() {
        // N − round(0.3 N), floored at two; the roadmap's 1, 2, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8.
        val expected = listOf(1, 2, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8)
        assertThat((1..12).map { keepWorkingSets(it) }).isEqualTo(expected)
        assertThat(keepWorkingSets(0)).isEqualTo(0)
    }

    @Test
    fun theTrimKeepsTheFirstWorkingSets_andDropsTheLaterAnchors() {
        val sets = (0 until 5).map { set("s$it", it) }
        val plan = deloadOf(sets, stepGrams = 2_500L)

        assertThat(plan.kept.map { it.setId }).containsExactly("s0", "s1", "s2").inOrder()
        assertThat(plan.removedSetIds).containsExactly("s3", "s4").inOrder()
    }

    @Test
    fun warmUpsAndRungs_areNeitherCountedNorRemoved() {
        val sets = listOf(
            set("w", 0, role = SetType.WARMUP, weight = 40_000L),
            set("s0", 1),
            set("s1", 2),
            set("s2", 3),
            set("s3", 4),
            set("r", 5, role = SetType.DROP, weight = null, dropValue = 10_000L),
        )
        val plan = deloadOf(sets, stepGrams = 2_500L)

        // The warm-up is not counted (N is four), the rung is not an anchor, and neither is a target.
        assertThat(plan.kept.map { it.setId }).containsExactly("s0", "s1", "s2").inOrder()
        assertThat(plan.removedSetIds).containsExactly("s3")
    }

    @Test
    fun aKeptSetLosesThreeLoadSteps() {
        val plan = deloadOf(listOf(set("s0", 0, weight = 100_000L)), stepGrams = 2_500L)

        assertThat(plan.kept.single().weightGrams).isEqualTo(92_500L)
        assertThat(plan.kept.single().assistanceGrams).isNull()
    }

    @Test
    fun aWeight_neverGoesBelowZero() {
        val plan = deloadOf(listOf(set("s0", 0, weight = 5_000L)), stepGrams = 2_500L)

        assertThat(plan.kept.single().weightGrams).isEqualTo(0L)
    }

    @Test
    fun anAssistedSet_gainsThreeStepsOfAssistance() {
        // More help is lighter: the assistance grows, the weight column does not move.
        val plan = deloadOf(
            listOf(set("s0", 0, weight = 0L, assistance = 20_000L)),
            stepGrams = 2_500L,
        )

        assertThat(plan.kept.single().weightGrams).isEqualTo(0L)
        assertThat(plan.kept.single().assistanceGrams).isEqualTo(27_500L)
    }

    @Test
    fun aSetThatStatesBoth_movesItsAssistance_andLeavesTheWeight() {
        val plan = deloadOf(
            listOf(set("s0", 0, weight = 60_000L, assistance = 20_000L)),
            stepGrams = 2_500L,
        )

        assertThat(plan.kept.single().weightGrams).isEqualTo(60_000L)
        assertThat(plan.kept.single().assistanceGrams).isEqualTo(27_500L)
    }

    @Test
    fun aBodyweightSet_andAWeightlessOne_areUnchanged() {
        val plan = deloadOf(
            listOf(
                set("body", 0, weight = 0L),
                set("empty", 1, weight = null, repsMin = null, repsMax = null),
            ),
            stepGrams = 2_500L,
        )

        assertThat(plan.kept[0].weightGrams).isEqualTo(0L)
        assertThat(plan.kept[0].assistanceGrams).isNull()
        assertThat(plan.kept[1].weightGrams).isNull()
        assertThat(plan.kept[1].assistanceGrams).isNull()
    }

    @Test
    fun aRangedPlan_movesToTheFloor_andAFixedOneDoesNot() {
        val plan = deloadOf(
            listOf(
                set("range", 0, repsMin = 5, repsMax = 8, repsCurrent = 7),
                set("fixed", 1, repsMin = 5, repsMax = 5, repsCurrent = null),
            ),
            stepGrams = 2_500L,
        )

        assertThat(plan.kept[0].repsCurrent).isEqualTo(5)
        assertThat(plan.kept[1].repsCurrent).isNull()
    }

    @Test
    fun aHold_keepsItsDurationTarget_andItsRangeIsNotARepRange() {
        val plan = deloadOf(
            listOf(set("hold", 0, repsMin = null, repsMax = null, duration = 45, weight = 40_000L)),
            stepGrams = 2_500L,
        )

        assertThat(plan.kept.single().repsCurrent).isNull()
        // The load still moves: the hold's seconds are untouched by the rule, but its load is a load.
        assertThat(plan.kept.single().weightGrams).isEqualTo(32_500L)
    }
}
