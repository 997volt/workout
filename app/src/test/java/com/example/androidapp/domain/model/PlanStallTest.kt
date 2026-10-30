package com.example.androidapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pattern a single session cannot show (ROADMAP N237).
 *
 * Every case here is a way the rule can say *nothing*: two sessions instead of three, a session that did not
 * train the movement, a set nobody logged, and a set that earned its step. A wrong yes would tell a lifter to
 * back off a plan that is working, which is the failure the rule exists to avoid — so the negative cases are the
 * point, and the positives are the three reasons it may say something.
 */
class PlanStallTest {

    @Test
    fun theSameSetShort_threeSessionsRunning_isAStall() {
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 4)),
                listOf(performed(reps = 3)),
            ),
        )

        val stall = history.stall()

        assertEquals("Back Squat", stall?.exerciseName)
        assertEquals("the first working set", 1, stall?.setNumber)
        assertEquals(ProgressionMiss.REPS_SHORT, stall?.kind)
        assertEquals(3, stall?.sessions)
    }

    @Test
    fun twoSessions_areNotYetAStall() {
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(listOf(performed(reps = 3)), listOf(performed(reps = 3))),
        )

        assertNull(history.stall())
    }

    @Test
    fun aSessionThatEarnedItsStep_endsTheRun() {
        // The prescription moved, so what happened three sessions ago is no longer the same sentence.
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 5, rpe = 7)),
                listOf(performed(reps = 3)),
            ),
        )

        assertNull("the answering session reset it", history.stall())
    }

    @Test
    fun aRunThreeLong_thatALaterSessionAnswered_isGone() {
        // B303: the walk used to keep the longest run it had seen, so three misses followed by a session that
        // met the plan still asked for a deload -- for as long as the run stayed in the read's window.
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 5)),
            ),
        )

        assertNull("the answering session ended the three", history.stall())
    }

    @Test
    fun theSecondSetCanBeTheOneThatFails() {
        // The counting is per prescription rather than per exercise: a plan whose second set always fails says
        // that, and the first set — which is being answered — is not blamed for it (N237).
        val history = history(
            planned = listOf(planSet(reps = 5), planSet(reps = 5, index = 1)),
            sessions = listOf(
                listOf(performed(reps = 5), performed(reps = 2)),
                listOf(performed(reps = 5), performed(reps = 3)),
                listOf(performed(reps = 5), performed(reps = 2)),
            ),
        )

        val stall = history.stall()

        assertEquals("the second set", 2, stall?.setNumber)
        assertEquals(ProgressionMiss.REPS_SHORT, stall?.kind)
    }

    @Test
    fun overTheEffort_isCountedEvenWhereTheOfferReportsTheReps() {
        // The offer states one reason by precedence, so a set that was short *and* over its effort reported the
        // reps. Counting the reported reason alone would let "over the target" become "short" between two
        // sessions failing in the same way (N237).
        val history = history(
            planned = listOf(planSet(reps = 5, rpe = 8)),
            sessions = listOf(
                listOf(performed(reps = 5, rpe = 9)),
                listOf(performed(reps = 5, rpe = 9)),
                listOf(performed(reps = 5, rpe = 10)),
            ),
        )

        val stall = history.stall()

        assertEquals(ProgressionMiss.OVER_TARGET_RPE, stall?.kind)
        assertEquals(1, stall?.setNumber)
    }

    @Test
    fun aLighterLoad_isAStallOfItsOwn() {
        val history = history(
            planned = listOf(planSet(reps = 5, weight = 100_000L)),
            sessions = listOf(
                listOf(performed(reps = 5, weight = 90_000L)),
                listOf(performed(reps = 5, weight = 95_000L)),
                listOf(performed(reps = 5, weight = 92_500L)),
            ),
        )

        assertEquals(ProgressionMiss.LOAD_SHORT, history.stall()?.kind)
    }

    @Test
    fun aSetNobodyLogged_isAbsence_notFailure() {
        // NOT_DONE is not one of the measurable kinds: a skipped set is not a failed one, and a streak it was
        // never part of cannot be started by it (N237).
        val history = history(
            planned = listOf(planSet(reps = 5), planSet(reps = 5, index = 1)),
            sessions = listOf(
                listOf(performed(reps = 5)),
                listOf(performed(reps = 5)),
                listOf(performed(reps = 5)),
            ),
        )

        assertNull("the second set was never attempted", history.stall())
    }

    @Test
    fun aSetNobodyLogged_doesNotBreakTheRun() {
        // B304: absence used to fall into the reset branch, so one session that logged no set wiped every run
        // -- the rule says it can neither start nor break a streak, and this is the break half.
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                emptyList(),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertEquals("the gap is not a miss", 3, history.stall()?.sessions)
    }

    @Test
    fun aPlanOfWarmUpsOnly_hasNothingToStall() {
        val history = history(
            planned = listOf(planSet(reps = 5, role = SetType.WARMUP)),
            sessions = listOf(listOf(performed(reps = 1)), listOf(performed(reps = 1)), listOf(performed(reps = 1))),
        )

        assertNull(history.stall())
    }

    @Test
    fun aHoldThatFellShort_isMeasuredInSeconds() {
        // ROADMAP N242's direction, read by the same rule: a prescribed hold that keeps coming up short is a
        // stall like any other.
        val history = history(
            planned = listOf(planSet(reps = null, hold = 45)),
            sessions = listOf(
                listOf(performed(reps = 0, hold = 30)),
                listOf(performed(reps = 0, hold = 35)),
                listOf(performed(reps = 0, hold = 30)),
            ),
        )

        assertEquals(ProgressionMiss.HOLD_SHORT, history.stall()?.kind)
    }

    @Test
    fun aDropWhereAWorkSetWasMissing_isNotAStallOfThatWorkSet() {
        // B305: pairing by raw position compared the plan's second work set against the drop logged after the
        // first, and three sessions of that read as the work set being under its prescribed load.
        val history = history(
            planned = listOf(
                planSet(reps = 5, weight = 100_000L),
                planSet(reps = 5, weight = 90_000L, index = 1),
            ),
            sessions = listOf(
                listOf(
                    performed(reps = 5, weight = 100_000L),
                    performed(reps = 5, weight = 70_000L, role = SetType.DROP),
                ),
                listOf(
                    performed(reps = 5, weight = 100_000L),
                    performed(reps = 5, weight = 70_000L, role = SetType.DROP),
                ),
                listOf(
                    performed(reps = 5, weight = 100_000L),
                    performed(reps = 5, weight = 70_000L, role = SetType.DROP),
                ),
            ),
        )

        assertNull("the second work set was never logged", history.stall())
    }

    private fun history(
        planned: List<ProgressionPlanSet>,
        sessions: List<List<ProgressionPerformance>>,
    ) = PrescriptionHistory(
        exerciseName = "Back Squat",
        planned = planned,
        performedBySession = sessions,
    )

    private fun planSet(
        reps: Int? = 5,
        index: Int = 0,
        weight: Long? = null,
        rpe: Int? = null,
        role: SetType = SetType.NORMAL,
        hold: Int? = null,
    ) = ProgressionPlanSet(
        setId = "ts-$index",
        setIndex = index,
        role = role,
        targetWeightGrams = weight,
        targetRepsMin = reps,
        targetRepsMax = reps,
        targetRpeHalves = rpe,
        targetDurationSeconds = hold,
    )

    private fun performed(
        reps: Int,
        weight: Long = 100_000L,
        rpe: Int? = 7,
        hold: Int? = null,
        role: SetType = SetType.NORMAL,
    ) = ProgressionPerformance(
        reps = reps,
        weightGrams = weight,
        rpeHalves = rpe,
        durationSeconds = hold,
        role = role,
    )
}
