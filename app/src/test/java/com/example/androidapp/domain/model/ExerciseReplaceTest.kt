package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The replacement plan's pure half (ROADMAP N308): the shared trim, the seed by place, the Empty
 * choice, and what happens to warm-ups, rungs, holds and a missing counterpart.
 */
class ExerciseReplaceTest {

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
        note: String? = null,
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
        note = note,
    )

    private fun seed(weight: Long = 80_000L, reps: Int = 6, duration: Int? = null) =
        ReplacementSeed(weightGrams = weight, assistanceGrams = 0L, reps = reps, durationSeconds = duration)

    @Test
    fun theTrim_keepsTheFirstWorkSets_andDropsTheLaterAnchorsWithTheirRungs() {
        val old = listOf(
            set("w", 0, role = SetType.WARMUP, weight = 40_000L),
            set("s0", 1),
            set("s1", 2),
            set("s2", 3),
            set("s3", 4),
            set("r3", 5, role = SetType.DROP, weight = null, repsMin = null, repsMax = null, dropValue = 10_000L),
        )

        val plan = replacementOf(old, listOf(seed(), seed(), seed(), seed()), empty = false)

        // Five work sets keep three; the warm-up is cleared, the dropped anchor's rung is not copied.
        assertThat(plan.sets.map { it.role })
            .containsExactly(SetType.WARMUP, SetType.NORMAL, SetType.NORMAL, SetType.NORMAL).inOrder()
        assertThat(plan.sets.first().weightGrams).isNull()
    }

    @Test
    fun theSeed_isByPlace_andTheOldRangeIsNotCarried() {
        val old = listOf(
            set("s0", 0, repsMin = 3, repsMax = 5, repsCurrent = 4),
            set("s1", 1, repsMin = 8, repsMax = 10),
            set("s2", 2),
        )

        val plan = replacementOf(
            old,
            listOf(seed(weight = 70_000L, reps = 6), null, seed(weight = 90_000L, reps = 2)),
            empty = false,
        )

        assertThat(plan.sets[0].weightGrams).isEqualTo(70_000L)
        assertThat(plan.sets[0].repsMin).isEqualTo(6)
        assertThat(plan.sets[0].repsMax).isEqualTo(6)
        assertThat(plan.sets[0].repsCurrent).isNull()
        assertThat(plan.sets[1].weightGrams).isNull()
    }

    @Test
    fun aMissingCounterpart_clearsTheTargetRatherThanGuessing() {
        val plan = replacementOf(listOf(set("s0", 0, weight = 100_000L)), listOf(null), empty = false)

        assertThat(plan.sets.single().weightGrams).isNull()
        assertThat(plan.sets.single().repsMin).isNull()
        assertThat(plan.sets.single().repsMax).isNull()
    }

    @Test
    fun empty_keepsThePlansRepsAndClearsTheLoads() {
        val old = listOf(set("s0", 0, weight = 100_000L, repsMin = 5, repsMax = 8, repsCurrent = 7))

        val plan = replacementOf(old, emptyList(), empty = true)

        assertThat(plan.sets.single().weightGrams).isNull()
        assertThat(plan.sets.single().repsMin).isEqualTo(5)
        assertThat(plan.sets.single().repsMax).isEqualTo(8)
        assertThat(plan.sets.single().repsCurrent).isEqualTo(7)
    }

    @Test
    fun aSeededHold_takesItsSeconds_andNoReps() {
        val old = listOf(set("h", 0, repsMin = null, repsMax = null, duration = 30))

        val plan = replacementOf(old, listOf(seed(duration = 45, reps = 0)), empty = false)

        assertThat(plan.sets.single().targetDurationSeconds).isEqualTo(45)
        assertThat(plan.sets.single().repsMin).isNull()
        assertThat(plan.sets.single().repsMax).isNull()
    }

    @Test
    fun aKeptRung_keepsItsRoleAndDropValue() {
        val old = listOf(
            set("s0", 0),
            set("s1", 1),
            set("r1", 2, role = SetType.DROP, weight = null, repsMin = null, repsMax = null, dropValue = 12_500L),
        )

        val plan = replacementOf(old, listOf(seed(), seed()), empty = false)

        assertThat(plan.sets.last().role).isEqualTo(SetType.DROP)
        assertThat(plan.sets.last().dropValueGrams).isEqualTo(12_500L)
    }

    @Test
    fun anUnkeptRung_isDroppedWithItsAnchor() {
        val old = listOf(
            set("s0", 0),
            set("s1", 1),
            set("s2", 2),
            set("s3", 3),
            set("r3", 4, role = SetType.CLUSTER, weight = null, repsMin = null, repsMax = null),
        )

        val plan = replacementOf(old, listOf(seed(), seed(), seed(), seed()), empty = false)

        assertThat(plan.sets).hasSize(3)
        assertThat(plan.sets.none { it.role == SetType.CLUSTER }).isTrue()
    }
}
