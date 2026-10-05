package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The next step an exercise earned (ROADMAP N50).
 *
 * The rule is where "earned" is decided, so what matters here is every way it can say no: a rep
 * short, an RPE at the target and one over it, no RPE recorded, no target RPE written for the
 * exercise, a plan of warm-ups only, and no plan at all. A wrong yes is the app telling a lifter to
 * add weight they have not earned, which is the failure the whole rule exists to avoid.
 */
class ProgressionOfferTest {

    @Test
    fun everyWorkingSet_isCheckedAgainstTheExercisesOneTargetRpe() {
        // ROADMAP N59, amended: the plan names one RPE for the exercise, and the caller carries it
        // onto every prescribed set. Meeting it on one set and exceeding it on the next is not the
        // plan answered, so nothing is offered.
        val offer = progressionOfferFor(
            planned = listOf(
                planSet(id = "ts-0", index = 0, rpe = 8),
                planSet(id = "ts-1", index = 1, rpe = 8),
            ),
            performed = listOf(done(rpe = 8), done(rpe = 9)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun anAnsweredPlan_offersBothDirections() {
        val offer = progressionOfferFor(
            planned = listOf(planSet()),
            performed = listOf(done()),
        )

        assertThat(offer).isNotNull()
        assertThat(offer!!.set.setId).isEqualTo("ts-0")
        assertThat(offer.reps).isEqualTo(ProgressionStep(5, 6))
        assertThat(offer.load).isEqualTo(ProgressionStep(100_000L, 102_500L))
    }

    @Test
    fun aRepShort_earnsNothing() {
        val offer = progressionOfferFor(
            planned = listOf(planSet(repsMax = 5)),
            performed = listOf(done(reps = 4)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun anRpeAtTheTarget_isMet() {
        // "at or under the target": the plan was answered exactly, and there is still room to move.
        val offer = progressionOfferFor(
            planned = listOf(planSet(rpe = 8)),
            performed = listOf(done(rpe = 8)),
        )

        assertThat(offer).isNotNull()
    }

    @Test
    fun anRpeOverTheTarget_earnsNothing() {
        val offer = progressionOfferFor(
            planned = listOf(planSet(rpe = 8)),
            performed = listOf(done(rpe = 9)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun anUnrecordedRpe_earnsNothing() {
        // The session did not say how hard it was, so nothing is known about the room in hand.
        val offer = progressionOfferFor(
            planned = listOf(planSet(rpe = 8)),
            performed = listOf(done(rpe = null)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun aPlanWithNoTargetRpe_earnsNothing() {
        val offer = progressionOfferFor(
            planned = listOf(planSet(rpe = null)),
            performed = listOf(done(rpe = 7)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun aPlanWithNoRepTarget_earnsNothing() {
        // Nothing to check the work against, and nothing to raise.
        val offer = progressionOfferFor(
            planned = listOf(planSet(repsMin = null, repsMax = null)),
            performed = listOf(done()),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun aPlanOfOnlyWarmUps_earnsNothing() {
        val offer = progressionOfferFor(
            planned = listOf(planSet(role = SetType.WARMUP)),
            performed = listOf(done(role = SetType.WARMUP)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun noPlan_earnsNothing() {
        val offer = progressionOfferFor(planned = emptyList(), performed = listOf(done()))

        assertThat(offer).isNull()
    }

    @Test
    fun noWorkLogged_earnsNothing() {
        val offer = progressionOfferFor(planned = listOf(planSet()), performed = emptyList())

        assertThat(offer).isNull()
    }

    @Test
    fun oneUnansweredSetAmongAnsweredOnes_earnsNothing() {
        // Every prescribed working set, not the last one: a plan answered twice and missed once was
        // not answered.
        val offer = progressionOfferFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1)),
            performed = listOf(done(reps = 5), done(reps = 3)),
        )

        assertThat(offer).isNull()
    }

    @Test
    fun anExtraSetLogged_doesNotStopTheOffer() {
        // More work than the plan asked for is not a reason to withhold the plan's next step.
        val offer = progressionOfferFor(
            planned = listOf(planSet()),
            performed = listOf(done(), done(), done()),
        )

        assertThat(offer).isNotNull()
    }

    @Test
    fun warmUpsAreExcludedFromBothSides() {
        // The plan's warm-up went unlogged, which shifts no index here: the work is paired by the
        // order it was performed in, with a warm-up on either side dropped first (N17, N20, N22).
        val offer = progressionOfferFor(
            planned = listOf(
                planSet(id = "warm", index = 0, role = SetType.WARMUP, weight = 40_000L, rpe = 5),
                planSet(id = "work", index = 1),
            ),
            performed = listOf(done()),
        )

        assertThat(offer).isNotNull()
        assertThat(offer!!.set.setId).isEqualTo("work")
    }

    @Test
    fun theLastWorkingSet_isTheOneChanged() {
        // It is the set the session built toward, so it is the plan's target in the sense a lifter
        // means it — and one set is what the write is for.
        val offer = progressionOfferFor(
            planned = listOf(
                planSet(id = "first", index = 0, weight = 90_000L),
                planSet(id = "last", index = 1, weight = 100_000L),
            ),
            performed = listOf(done(weight = 90_000L), done(weight = 100_000L)),
        )

        assertThat(offer!!.set.setId).isEqualTo("last")
    }

    @Test
    fun aSetWithNoAddedWeight_offersOnlyTheRep() {
        // A bodyweight movement has no load to raise; the rep is still a target a step can move.
        val offer = progressionOfferFor(
            planned = listOf(planSet(weight = null)),
            performed = listOf(done(weight = 0L)),
        )

        assertThat(offer).isNotNull()
        assertThat(offer!!.load).isNull()
        assertThat(offer.reps).isEqualTo(ProgressionStep(5, 6))
    }

    @Test
    fun anAssistedSet_offersOnlyTheRep() {
        // The machine's help is a magnitude, not a load, so adding a step of weight to it would be
        // the corruption N15 rejected a signed weight for.
        val offer = progressionOfferFor(
            planned = listOf(planSet(weight = null, assistance = 20_000L)),
            performed = listOf(done(weight = 0L)),
        )

        assertThat(offer!!.load).isNull()
    }

    @Test
    fun theRaiseIsTheStep_itWasGiven() {
        // The smallest loadable step is a parameter, so a barbell that jumps 5 kg is moved by one.
        val offer = progressionOfferFor(
            planned = listOf(planSet()),
            performed = listOf(done()),
            stepGrams = 5_000L,
        )

        assertThat(offer!!.load).isEqualTo(ProgressionStep(100_000L, 105_000L))
    }

    @Test
    fun acceptingTheLoad_raisesTheTargetWeight() {
        val offer = progressionOfferFor(listOf(planSet()), listOf(done()))!!

        val accepted = offer.accepted(ProgressionDirection.LOAD)

        assertThat(accepted!!.targetWeightGrams).isEqualTo(102_500L)
        assertWithMessage("the rep target is untouched").that(accepted.targetRepsMax).isEqualTo(5)
    }

    @Test
    fun acceptingTheRep_raisesTheCeiling() {
        val offer = progressionOfferFor(listOf(planSet(repsMin = 5, repsMax = 8)), listOf(done(reps = 8)))!!

        val accepted = offer.accepted(ProgressionDirection.REPS)

        assertThat(accepted!!.targetRepsMax).isEqualTo(9)
        assertWithMessage("the floor is what the range starts at, not what a step moves")
            .that(accepted.targetRepsMin)
            .isEqualTo(5)
    }

    @Test
    fun acceptingTheRep_raisesTheFloor_whenThatIsAllThePlanWrote() {
        // An AMRAP-ish "at least five" has no ceiling to raise, so the number the plan means is the
        // floor, and that is what the step moves.
        val offer = progressionOfferFor(listOf(planSet(repsMin = 5, repsMax = null)), listOf(done()))!!

        val accepted = offer.accepted(ProgressionDirection.REPS)

        assertThat(accepted!!.targetRepsMin).isEqualTo(6)
        assertThat(accepted.targetRepsMax).isNull()
    }

    @Test
    fun acceptingADirectionThePlanNeverOffered_isNull() {
        val offer = progressionOfferFor(listOf(planSet(weight = null)), listOf(done()))!!

        assertThat(offer.accepted(ProgressionDirection.LOAD)).isNull()
    }

    @Test
    fun aPercentagePrescription_keepsItWhenARepIsAccepted() {
        // The percentage is the slot's own way of naming a load (P3.8): a rep step must not drop it.
        val offer = progressionOfferFor(
            planned = listOf(planSet(percentOf1Rm = 85, weight = null)),
            performed = listOf(done()),
        )!!

        val accepted = offer.accepted(ProgressionDirection.REPS)

        assertThat(accepted!!.targetPercentOf1Rm).isEqualTo(85)
    }

    @Test
    fun theAcceptedSet_keepsItsSource_andEverythingElse() {
        val offer = progressionOfferFor(
            planned = listOf(planSet(source = ProgressionSource.SLOT, assistance = null, note = "belt on")),
            performed = listOf(done()),
        )!!

        val accepted = offer.accepted(ProgressionDirection.LOAD)!!

        assertThat(accepted.source).isEqualTo(ProgressionSource.SLOT)
        assertThat(accepted.setId).isEqualTo("ts-0")
        assertThat(accepted.note).isEqualTo("belt on")
        assertThat(accepted.targetRpeHalves).isEqualTo(8)
    }

    @Test
    fun thePrompt_statesThePlan_andWhatWasDone() {
        val prompt = progressionPromptFor(
            planned = listOf(planSet()),
            performed = listOf(done(reps = 5, rpe = 7)),
        )

        assertThat(prompt.planned?.targetWeightGrams).isEqualTo(100_000L)
        assertThat(prompt.planned?.targetReps).isEqualTo(5)
        assertThat(prompt.performed?.reps).isEqualTo(5)
        assertThat(prompt.performed?.rpeHalves).isEqualTo(7)
        assertThat(prompt.offer).isNotNull()
    }

    @Test
    fun thePromptSaysNothingPlanned_forAnExerciseWithNoPlan() {
        // Done still has something to say — the rating is behind it — but it does not invent a plan.
        val prompt = progressionPromptFor(planned = emptyList(), performed = listOf(done()))

        assertThat(prompt.planned).isNull()
        assertThat(prompt.offer).isNull()
    }

    @Test
    fun thePrompt_statesNoOffer_whenTheSessionWasNotRated() {
        // The prompt is still worth reading — it says what the plan asked — it just suggests nothing.
        val prompt = progressionPromptFor(
            planned = listOf(planSet()),
            performed = listOf(done(rpe = null)),
        )

        assertThat(prompt.planned).isNotNull()
        assertThat(prompt.offer).isNull()
    }

    private fun planSet(
        id: String = "ts-0",
        index: Int = 0,
        source: ProgressionSource = ProgressionSource.TEMPLATE,
        role: SetType = SetType.NORMAL,
        weight: Long? = 100_000L,
        assistance: Long? = null,
        repsMin: Int? = null,
        repsMax: Int? = 5,
        /** The exercise's one target RPE, which the caller carries onto each set (N59, amended). */
        rpe: Int? = 8,
        percentOf1Rm: Int? = null,
        note: String? = null,
    ) = ProgressionPlanSet(
        setId = id,
        setIndex = index,
        source = source,
        role = role,
        targetWeightGrams = weight,
        targetAssistanceGrams = assistance,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetRpeHalves = rpe,
        targetPercentOf1Rm = percentOf1Rm,
        note = note,
    )

    private fun done(
        reps: Int = 5,
        rpe: Int? = 7,
        role: SetType = SetType.NORMAL,
        weight: Long = 100_000L,
    ) = ProgressionPerformance(reps = reps, weightGrams = weight, rpeHalves = rpe, role = role)
}
