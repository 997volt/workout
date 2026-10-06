package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The frozen progression question and the picks made on it (ROADMAP N74).
 *
 * What matters here is that a pick is only a pick — nothing is written until the question is
 * confirmed — that a row cannot be given a direction it never offered, and that a step already
 * written stays written: a failure part-way through a confirm must not let the retry write it twice.
 */
class PendingProgressionTest {

    @Test
    fun of_freezesEverySet_withNothingChosen() {
        val question = pending(loadRow("ts-0"), repRow("ts-1"))

        assertThat(question.sets).hasSize(2)
        assertThat(question.sets.map { it.direction }).containsExactly(null, null)
        assertThat(question.chosen).isEmpty()
        assertThat(question.isSettled).isTrue()
    }

    @Test
    fun toggle_picksOneStep_andTakesItBackOnASecondTap() {
        val picked = pending(repRow("ts-0")).toggle("ts-0", ProgressionDirection.REPS)
        val cleared = picked.toggle("ts-0", ProgressionDirection.REPS)

        assertThat(picked.sets.single().direction).isEqualTo(ProgressionDirection.REPS)
        assertThat(picked.chosen.map { it.offer.set.setId }).containsExactly("ts-0")
        assertThat(cleared.sets.single().direction).isNull()
        assertThat(cleared.isSettled).isTrue()
    }

    @Test
    fun toggle_switchesDirection_ratherThanPickingBoth() {
        // The no-ceiling plan offers both, and a set cannot take both in one answer (N74).
        val question = pending(bothRow("ts-0"))
            .toggle("ts-0", ProgressionDirection.REPS)
            .toggle("ts-0", ProgressionDirection.LOAD)

        assertThat(question.sets.single().direction).isEqualTo(ProgressionDirection.LOAD)
        assertThat(question.chosen).hasSize(1)
    }

    @Test
    fun toggle_ignoresADirectionTheSetNeverOffered() {
        // A set below its ceiling has no weight step, and the dialog has no chip to tap: the pick is
        // refused here too rather than recording an impossible answer.
        val question = pending(repRow("ts-0")).toggle("ts-0", ProgressionDirection.LOAD)

        assertThat(question.sets.single().direction).isNull()
    }

    @Test
    fun chooseAll_picksOnlyTheRowsThatOfferThatDirection() {
        val question = pending(repRow("ts-0"), repRow("ts-1"), loadRow("ts-2"))
            .chooseAll(ProgressionDirection.REPS)

        assertThat(question.sets.map { it.direction })
            .containsExactly(ProgressionDirection.REPS, ProgressionDirection.REPS, null)
        assertThat(question.offering(ProgressionDirection.REPS)).isEqualTo(2)
        assertThat(question.offering(ProgressionDirection.LOAD)).isEqualTo(1)
    }

    @Test
    fun applied_spendsTheOffer_andKeepsTheAcceptedPlan() {
        val accepted = planSet("ts-0").copy(targetRepsCurrent = 6)
        val question = pending(repRow("ts-0"), repRow("ts-1"))
            .chooseAll(ProgressionDirection.REPS)
            .applied("ts-0", accepted)

        assertThat(question.sets[0].applied).isTrue()
        assertThat(question.sets[0].direction).isNull()
        assertThat(question.sets[0].prompt.offer).isNull()
        assertThat(question.sets[0].prompt.miss).isNull()
        assertThat(question.sets[0].prompt.planned?.targetRepsCurrent).isEqualTo(6)
        assertWithMessage("a written step is not offered again, so a retry cannot repeat it")
            .that(question.sets[0].offers(ProgressionDirection.REPS))
            .isFalse()
        assertWithMessage("the other pick is still waiting").that(question.chosen.map { it.offer.set.setId })
            .containsExactly("ts-1")
    }

    @Test
    fun aRowWithoutAPlanSet_isNotPicked() {
        // Logged work the plan does not name has no target to change, so it never offers a direction.
        val extra = ProgressionSetPrompt(
            planned = null,
            performed = ProgressionPerformance(reps = 5),
            miss = ProgressionMiss.NOT_IN_PLAN,
        )
        val question = pending(extra).toggle("ts-0", ProgressionDirection.REPS)

        assertThat(question.sets.single().offers(ProgressionDirection.REPS)).isFalse()
        assertThat(question.chosen).isEmpty()
    }

    private fun pending(vararg sets: ProgressionSetPrompt) = pendingProgressionFor(
        sessionExerciseId = "se1",
        exerciseName = "Back Squat",
        unit = WeightUnit.KILOGRAMS,
        prompt = ProgressionPrompt(sets.toList()),
    )

    private fun repRow(setId: String) = ProgressionSetPrompt(
        planned = planSet(setId),
        performed = ProgressionPerformance(reps = 5),
        offer = ProgressionOffer(set = planSet(setId), reps = ProgressionStep(5, 6)),
    )

    private fun loadRow(setId: String) = ProgressionSetPrompt(
        planned = planSet(setId),
        performed = ProgressionPerformance(reps = 8),
        offer = ProgressionOffer(set = planSet(setId), load = ProgressionStep(100_000L, 102_500L)),
    )

    private fun bothRow(setId: String) = ProgressionSetPrompt(
        planned = planSet(setId),
        performed = ProgressionPerformance(reps = 5),
        offer = ProgressionOffer(
            set = planSet(setId),
            reps = ProgressionStep(5, 6),
            load = ProgressionStep(100_000L, 102_500L),
        ),
    )

    private fun planSet(setId: String) = ProgressionPlanSet(
        setId = setId,
        setIndex = 0,
        targetWeightGrams = 100_000L,
        targetRepsMin = 5,
        targetRepsMax = 8,
        targetRpeHalves = 8,
    )
}
