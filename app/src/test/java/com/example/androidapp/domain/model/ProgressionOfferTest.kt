package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The next step each planned set earned (ROADMAP N50, N74).
 *
 * The rule is where "earned" is decided, so what matters here is every way it can say no and every
 * way the two directions are kept apart: a rep short, an RPE at the target and one over it, no RPE
 * recorded, no target RPE written for the exercise, a plan of warm-ups only, and no plan at all. A
 * wrong yes is the app telling a lifter to add weight they have not earned, which is the failure the
 * whole rule exists to avoid.
 *
 * N74's half is the range: a set below its ceiling earns the rep and *not* the weight, a set at the
 * ceiling earns the weight and *not* the rep, and accepting the weight puts the climb back on the
 * range's floor. Only a plan that wrote no ceiling keeps both, as it always did.
 */
class ProgressionOfferTest {

    @Test
    fun aSetBelowItsCeiling_earnsTheRepAlone() {
        // Asked 5 of "5 to 8" and did them: there are reps left inside the range, so the weight waits
        // rather than being offered beside them (N74).
        val offer = singleOffer(planned = listOf(planSet(repsMin = 5, repsMax = 8)), performed = listOf(done()))

        assertThat(offer).isNotNull()
        assertThat(offer!!.reps).isEqualTo(ProgressionStep(5, 6))
        assertWithMessage("the weight waits while reps are left").that(offer.load).isNull()
    }

    @Test
    fun aMissedSibling_doesNotStopTheSetThatWasAnswered() {
        // The old rule needed every prescribed working set, so one missed set earned the exercise
        // nothing at all. Per set, the answered one still earns its own step (N74).
        val prompt = progressionPromptFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1)),
            performed = listOf(done(reps = 5), done(reps = 3)),
        )

        assertThat(prompt.sets[0].offer).isNotNull()
        assertThat(prompt.sets[0].offer!!.set.setId).isEqualTo("ts-0")
        assertThat(prompt.sets[1].offer).isNull()
        assertThat(prompt.sets[1].miss).isEqualTo(ProgressionMiss.REPS_SHORT)
    }

    @Test
    fun theRepStep_tracksWhatWasDone_cappedAtTheCeiling() {
        // Asked 5, did 7 of an 8 ceiling: the plan follows the lifter to the top of the range rather
        // than stepping to 6 (N74).
        val offer = singleOffer(planned = listOf(planSet(repsMin = 5, repsMax = 8)), performed = listOf(done(reps = 7)))

        assertThat(offer!!.reps).isEqualTo(ProgressionStep(5, 8))
    }

    @Test
    fun aSetAtItsCeiling_earnsTheWeightAlone() {
        // The range is topped out, so the only way forward is a heavier bar — offered on its own.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8)),
        )

        assertThat(offer!!.reps).isNull()
        assertThat(offer.load).isEqualTo(ProgressionStep(100_000L, 102_500L))
    }

    @Test
    fun acceptingTheWeight_restartsARealRangeOnItsFloor() {
        // "5 to 8" climbed to 8 becomes 5 reps a step heavier; the bounds themselves never move.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8)),
        )!!

        val accepted = offer.accepted(ProgressionDirection.LOAD)!!

        assertThat(accepted.targetWeightGrams).isEqualTo(102_500L)
        assertThat(accepted.targetRepsCurrent).isEqualTo(5)
        assertThat(accepted.targetRepsMin).isEqualTo(5)
        assertWithMessage("the range's ceiling is the plan's, not the step's")
            .that(accepted.targetRepsMax)
            .isEqualTo(8)
    }

    @Test
    fun aPlanWithNoCeiling_keepsBothDirections() {
        // "At least five" wrote no ceiling, so there is always room in the reps — and the weight step
        // it has always offered stays (N74).
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = null)),
            performed = listOf(done()),
        )

        assertThat(offer!!.reps).isEqualTo(ProgressionStep(5, 6))
        assertThat(offer.load).isEqualTo(ProgressionStep(100_000L, 102_500L))
    }

    @Test
    fun aPlanWhoseEndsAreEqual_offersTheWeightAlone_andKeepsTheReps() {
        // "5 to 5" is one number rather than a range: there is no climb inside it, and no floor to
        // restart at, so the weight moves and the reps stay where the plan put them.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = 5)),
            performed = listOf(done()),
        )!!

        assertThat(offer.reps).isNull()
        val accepted = offer.accepted(ProgressionDirection.LOAD)!!
        assertWithMessage("the reps the set asks for are unchanged")
            .that(accepted.targetReps)
            .isEqualTo(5)
        assertThat(accepted.targetRepsCurrent).isNull()
    }

    @Test
    fun aPlanThatNamedNoFloor_offersTheWeightAlone_andKeepsTheReps() {
        // "to 8" has a ceiling and no floor, so it is topped out at 8 and there is nowhere to restart.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = null, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8)),
        )!!

        assertThat(offer.reps).isNull()
        assertThat(offer.accepted(ProgressionDirection.LOAD)!!.targetRepsCurrent).isEqualTo(8)
    }

    @Test
    fun aRepShort_earnsNothing() {
        val prompt = singleSet(planned = listOf(planSet()), performed = listOf(done(reps = 4)))

        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.REPS_SHORT)
    }

    @Test
    fun anRpeAtTheTarget_isMet() {
        // "at or under the target": the plan was answered exactly, and there is still room to move.
        val offer = singleOffer(planned = listOf(planSet(rpe = 8)), performed = listOf(done(rpe = 8)))

        assertThat(offer).isNotNull()
    }

    @Test
    fun anRpeOverTheTarget_earnsNothing() {
        val prompt = singleSet(planned = listOf(planSet(rpe = 8)), performed = listOf(done(rpe = 9)))

        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.OVER_TARGET_RPE)
    }

    @Test
    fun anUnrecordedRpe_earnsNothing() {
        // The session did not say how hard it was, so nothing is known about the room in hand.
        val prompt = singleSet(planned = listOf(planSet(rpe = 8)), performed = listOf(done(rpe = null)))

        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.UNRATED)
    }

    @Test
    fun aPlanWithNoTargetRpe_earnsNothing() {
        val prompt = singleSet(planned = listOf(planSet(rpe = null)), performed = listOf(done(rpe = 7)))

        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.NO_TARGET_RPE)
    }

    @Test
    fun aPlanWithNoRepTarget_earnsNothing() {
        // Nothing to check the work against, and nothing to raise.
        val prompt = singleSet(
            planned = listOf(planSet(repsMin = null, repsMax = null)),
            performed = listOf(done()),
        )

        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.NO_REP_TARGET)
    }

    @Test
    fun aPlannedSetThatWasNotDone_isStated_asNotDone() {
        // The row still appears — the prompt accounts for the plan — it simply has no step.
        val prompt = singleSet(planned = listOf(planSet()), performed = emptyList())

        assertThat(prompt.planned).isNotNull()
        assertThat(prompt.offer).isNull()
        assertThat(prompt.miss).isEqualTo(ProgressionMiss.NOT_DONE)
    }

    @Test
    fun workThePlanDoesNotName_isStatedWithoutAStep() {
        // An extra set is part of the session, so the prompt states it — with no target to change.
        val prompt = progressionPromptFor(
            planned = listOf(planSet()),
            performed = listOf(done(), done(), done()),
        )

        assertThat(prompt.sets).hasSize(3)
        assertThat(prompt.sets[0].offer).isNotNull()
        assertThat(prompt.sets[1].planned).isNull()
        assertThat(prompt.sets[1].miss).isEqualTo(ProgressionMiss.NOT_IN_PLAN)
        assertThat(prompt.sets[2].miss).isEqualTo(ProgressionMiss.NOT_IN_PLAN)
    }

    @Test
    fun aPlanOfOnlyWarmUps_statesNothingToProgress() {
        val prompt = progressionPromptFor(
            planned = listOf(planSet(role = SetType.WARMUP)),
            performed = listOf(done(role = SetType.WARMUP)),
        )

        assertThat(prompt.sets).isEmpty()
        assertThat(prompt.hasPlan).isFalse()
    }

    @Test
    fun noPlan_statesNoSetsAtAll() {
        val prompt = progressionPromptFor(planned = emptyList(), performed = listOf(done()))

        assertWithMessage("nothing to progress from, and nothing invented")
            .that(prompt.sets.map { it.miss })
            .containsExactly(ProgressionMiss.NOT_IN_PLAN)
        assertThat(prompt.hasPlan).isFalse()
    }

    @Test
    fun warmUpsAreExcludedFromBothSides() {
        // The plan's warm-up went unlogged, which shifts no pairing here: the work is matched by the
        // order it was performed in, with a warm-up on either side dropped first (N17, N20, N22).
        val prompt = progressionPromptFor(
            planned = listOf(
                planSet(id = "warm", index = 0, role = SetType.WARMUP, weight = 40_000L, rpe = 5),
                planSet(id = "work", index = 1),
            ),
            performed = listOf(done()),
        )

        assertThat(prompt.sets).hasSize(1)
        assertThat(prompt.sets.single().offer!!.set.setId).isEqualTo("work")
    }

    @Test
    fun aBodyweightSetAtItsCeiling_isToppedOut() {
        // No rep is left inside the range and there is no weight to raise, so the app says exactly
        // that rather than offering a rep past the plan (N74).
        val offer = singleOffer(
            planned = listOf(planSet(weight = null, repsMin = 5, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8, weight = 0L)),
        )

        assertThat(offer).isNull()
        assertThat(
            singleSet(
                planned = listOf(planSet(weight = null, repsMin = 5, repsMax = 8, repsCurrent = 8)),
                performed = listOf(done(reps = 8, weight = 0L)),
            ).miss,
        ).isEqualTo(ProgressionMiss.TOPPED_OUT)
    }

    @Test
    fun anAssistedSetBelowItsCeiling_stillOffersTheRep() {
        // The shape both plan editors write for `-20`: zero added weight beside 20 kg of help, so a
        // weight step would put 2.5 kg on a machine doing 20 kg of the work (N15).
        val offer = singleOffer(
            planned = listOf(planSet(weight = 0L, assistance = 20_000L, repsMin = 5, repsMax = 8)),
            performed = listOf(done(weight = 0L)),
        )!!

        assertThat(offer.load).isNull()
        assertThat(offer.reps).isEqualTo(ProgressionStep(5, 6))
    }

    @Test
    fun acceptingADirectionThePlanNeverOffered_isNull() {
        // A set at its ceiling offers the weight, not the rep, and the dialog has no chip to tap.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8)),
        )!!

        assertThat(offer.accepted(ProgressionDirection.REPS)).isNull()
    }

    @Test
    fun acceptingTheRep_movesTheCurrentTarget_insideTheRange() {
        // ROADMAP N74: the range is what the plan was authored with, and progression never edits it.
        val offer = singleOffer(planned = listOf(planSet(repsMin = 5, repsMax = 8)), performed = listOf(done()))!!

        val accepted = offer.accepted(ProgressionDirection.REPS)!!

        assertThat(accepted.targetRepsCurrent).isEqualTo(6)
        assertThat(accepted.targetRepsMin).isEqualTo(5)
        assertThat(accepted.targetRepsMax).isEqualTo(8)
    }

    @Test
    fun acceptingAStep_keepsTheSetsOwnLegacyRpe_ratherThanTheExercises() {
        // The exercise's one number rides on every set so the rule can read it, but the write-back has
        // to put the set's **own** stored value back (N59): copying the exercise's number into the
        // legacy column would resurrect it after the plan's field was cleared.
        val offer = singleOffer(
            planned = listOf(planSet(rpe = 18, legacyRpe = 6)),
            performed = listOf(done()),
        )!!

        val accepted = offer.accepted(ProgressionDirection.LOAD)!!

        assertThat(accepted.legacyRpeHalves).isEqualTo(6)
        assertWithMessage("the rule still read the exercise's target")
            .that(accepted.targetRpeHalves)
            .isEqualTo(18)
    }

    @Test
    fun theAcceptedSet_keepsEverythingTheStepDidNotMove() {
        val offer = singleOffer(
            planned = listOf(planSet(note = "belt on")),
            performed = listOf(done()),
        )!!

        val accepted = offer.accepted(ProgressionDirection.LOAD)!!

        assertThat(accepted.setId).isEqualTo("ts-0")
        assertThat(accepted.note).isEqualTo("belt on")
        assertThat(accepted.targetRpeHalves).isEqualTo(8)
    }

    @Test
    fun theRaiseIsTheStep_itWasGiven() {
        // The smallest loadable step is a parameter, so a barbell that jumps 5 kg is moved by one.
        val offer = singleOffer(
            planned = listOf(planSet(repsMin = 5, repsMax = 8, repsCurrent = 8)),
            performed = listOf(done(reps = 8)),
            stepGrams = 5_000L,
        )!!

        assertThat(offer.load).isEqualTo(ProgressionStep(100_000L, 105_000L))
    }

    @Test
    fun thePrompt_statesThePlanAndWhatWasDone_forEverySet() {
        val prompt = progressionPromptFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1)),
            performed = listOf(done(reps = 5, rpe = 7), done(reps = 8, rpe = 7)),
        )

        assertThat(prompt.hasPlan).isTrue()
        assertThat(prompt.earned).isEqualTo(2)
        assertThat(prompt.sets[0].planned?.targetWeightGrams).isEqualTo(100_000L)
        assertThat(prompt.sets[0].planned?.targetReps).isEqualTo(5)
        assertThat(prompt.sets[0].performed?.reps).isEqualTo(5)
        assertThat(prompt.sets[1].performed?.reps).isEqualTo(8)
    }

    @Test
    fun anAnsweredPlan_statesHowManySetsEarnedOne() {
        val prompt = progressionPromptFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1)),
            performed = listOf(done(reps = 5), done(reps = 3)),
        )

        assertThat(prompt.earned).isEqualTo(1)
    }


    @Test
    fun anAdHocRung_doesNotDisplaceTheWorkSetsAfterIt() {
        // ROADMAP N79, and the bug the pairing fix is for: a working set followed by a drop at a
        // lighter weight used to shift every later pair, so the *second* planned set was judged against
        // the drop's ten reps and offered a heavier weight — which accepting wrote into the plan.
        //
        // The second planned set carries a **range** (B69): with both ends at five, both pairings land
        // on the ceiling and offer the same load, so the test pinned the pairing without distinguishing
        // the offer. Below a ceiling of eight, the correct pairing earns the *rep* — 5 done of 5..8 —
        // while the old pairing, reading the drop's ten, would earn the load instead.
        val prompt = progressionPromptFor(
            planned = listOf(
                planSet(id = "ts-0", index = 0),
                planSet(id = "ts-1", index = 1, repsMax = 8),
                planSet(id = "ts-2", index = 2),
            ),
            performed = listOf(
                done(reps = 5),
                done(reps = 10, role = SetType.DROP, weight = 80_000L),
                done(reps = 5),
                done(reps = 5),
            ),
        )

        assertThat(prompt.sets).hasSize(4)
        // The three prescribed sets keep their own work, in order.
        assertThat(prompt.sets[0].performed?.reps).isEqualTo(5)
        assertThat(prompt.sets[1].performed?.reps).isEqualTo(5)
        assertThat(prompt.sets[2].performed?.reps).isEqualTo(5)
        // And the second set is judged against the five it did, not the drop's ten.
        assertThat(prompt.sets[1].offer?.reps).isNotNull()
        assertThat(prompt.sets[1].offer?.load).isNull()
        // And the drop is what it is: extra work the plan does not name, earning nothing.
        assertThat(prompt.sets[3].planned).isNull()
        assertThat(prompt.sets[3].performed?.role).isEqualTo(SetType.DROP)
        assertThat(prompt.sets[3].miss).isEqualTo(ProgressionMiss.NOT_IN_PLAN)
    }

    @Test
    fun aPrescribedRung_earnsNothingOfItsOwn() {
        // The group's step belongs to the set the run hangs off, so the rung states why it has none
        // rather than being judged on numbers that are not its own (ROADMAP N79).
        val prompt = progressionPromptFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1, role = SetType.DROP)),
            performed = listOf(done(reps = 5), done(reps = 10, role = SetType.DROP, weight = 80_000L)),
        )
        val rung = prompt.sets[1]

        assertThat(rung.offer).isNull()
        assertThat(rung.miss).isEqualTo(ProgressionMiss.RUNG_OF_A_GROUP)
        assertWithMessage("the drop still reads against its own row")
            .that(rung.performed?.reps).isEqualTo(10)
        // B69: "a rung cut short does not hold the group back" was asserted only from the rung's side.
        // The rung has no target to miss, so the anchor is still judged on its own work and still earns
        // its step — which is the whole point of judging the group on its first set.
        assertWithMessage("and the anchor it hangs off still earns its own")
            .that(prompt.sets[0].offer).isNotNull()
    }

    @Test
    fun aPrescribedRung_pairsWithThePerformedRung_notWithAWorkSet() {
        // Planned as one working set and one drop, performed as a working set and two drops: the first
        // drop pairs with the plan's, and the second is extra (ROADMAP N79).
        val prompt = progressionPromptFor(
            planned = listOf(planSet(id = "ts-0", index = 0), planSet(id = "ts-1", index = 1, role = SetType.DROP)),
            performed = listOf(
                done(reps = 5),
                done(reps = 10, role = SetType.DROP, weight = 80_000L),
                done(reps = 6, role = SetType.DROP, weight = 60_000L),
            ),
        )

        assertThat(prompt.sets).hasSize(3)
        assertThat(prompt.sets[1].planned?.setId).isEqualTo("ts-1")
        assertThat(prompt.sets[1].performed?.weightGrams).isEqualTo(80_000L)
        assertThat(prompt.sets[2].planned).isNull()
        assertThat(prompt.sets[2].performed?.weightGrams).isEqualTo(60_000L)
    }

    @Test
    fun aPrescribedClusterRung_isTheSameShape() {
        val rung = progressionPromptFor(
            planned = listOf(
                planSet(id = "ts-0", index = 0),
                planSet(id = "ts-1", index = 1, role = SetType.CLUSTER),
            ),
            performed = listOf(done(reps = 5), done(reps = 3, role = SetType.CLUSTER)),
        ).sets[1]

        assertThat(rung.offer).isNull()
        assertThat(rung.miss).isEqualTo(ProgressionMiss.RUNG_OF_A_GROUP)
    }

    private fun singleSet(
        planned: List<ProgressionPlanSet>,
        performed: List<ProgressionPerformance>,
        stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
    ): ProgressionSetPrompt = progressionPromptFor(planned, performed, stepGrams).sets.first()

    private fun singleOffer(
        planned: List<ProgressionPlanSet>,
        performed: List<ProgressionPerformance>,
        stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
    ): ProgressionOffer? = singleSet(planned, performed, stepGrams).offer

    private fun planSet(
        id: String = "ts-0",
        index: Int = 0,
        role: SetType = SetType.NORMAL,
        weight: Long? = 100_000L,
        assistance: Long? = null,
        repsMin: Int? = 5,
        repsMax: Int? = 5,
        /** Where in the range the lifter is, or null for "the floor, else the ceiling" (N74). */
        repsCurrent: Int? = null,
        /** The exercise's one target RPE, which the caller carries onto each set (N59, amended). */
        rpe: Int? = 8,
        /** The set's own legacy per-set value, which an accepted write puts back (N59). */
        legacyRpe: Int? = null,
        note: String? = null,
    ) = ProgressionPlanSet(
        setId = id,
        setIndex = index,
        role = role,
        targetWeightGrams = weight,
        targetAssistanceGrams = assistance,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetRepsCurrent = repsCurrent,
        targetRpeHalves = rpe,
        note = note,
        legacyRpeHalves = legacyRpe,
    )

    private fun done(
        reps: Int = 5,
        rpe: Int? = 7,
        role: SetType = SetType.NORMAL,
        weight: Long = 100_000L,
    ) = ProgressionPerformance(reps = reps, weightGrams = weight, rpeHalves = rpe, role = role)
}
