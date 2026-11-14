package com.example.androidapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pattern a single session cannot show (ROADMAP N237, N304).
 *
 * Every case here is a way the rule can say *nothing*: two sessions instead of three, a session that did not
 * train the movement, a set that earned its step, a plan that cannot progress, a movement whose own answer is
 * Off. A wrong yes would tell a lifter to back off a plan that is working, which is the failure the rule exists
 * to avoid — so the negative cases are the point. The positives pin the unit N304 moved the rule to: the
 * exercise, not one of its sets, so a reason that changes between sessions still adds up.
 */
class PlanStallTest {

    @Test
    fun threeSessionsInWhichNothingMoved_areAStall() {
        val history = history(
            planned = listOf(planSet(reps = 5), planSet(reps = 5, index = 1)),
            sessions = listOf(
                listOf(performed(reps = 3), performed(reps = 4)),
                listOf(performed(reps = 3), performed(reps = 3)),
                listOf(performed(reps = 4), performed(reps = 3)),
            ),
        )

        val stall = history.stall()

        assertEquals("Back Squat", stall?.exerciseName)
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
    fun theReasonMayChangeBetweenSessions_stillAStall() {
        // N304: the rule used to key a run on one set *and one reason*, so three sessions that all missed a
        // different way were three runs of one. The exercise is the unit now.
        val history = history(
            planned = listOf(planSet(reps = 5, weight = 100_000L)),
            sessions = listOf(
                listOf(performed(reps = 3, weight = 100_000L)),
                listOf(performed(reps = 5, weight = 100_000L, rpe = 10)),
                listOf(performed(reps = 5, weight = 90_000L)),
            ),
        )

        assertEquals(3, history.stall()?.sessions)
    }

    @Test
    fun aSessionWhereASetMetThePlan_endsTheRun_evenThoughAnotherSetKeepsMissing() {
        // The exercise moved: one set answered its plan, so this is progress, not a no-progress session. The
        // old per-set rule would have kept counting the set that stayed stuck (N304).
        val history = history(
            planned = listOf(planSet(reps = 5), planSet(reps = 5, index = 1)),
            sessions = listOf(
                listOf(performed(reps = 3), performed(reps = 3)),
                listOf(performed(reps = 3), performed(reps = 5, rpe = 7)),
                listOf(performed(reps = 3), performed(reps = 3)),
            ),
        )

        assertNull("the answered set was progress", history.stall())
    }

    @Test
    fun aGapOfNoSetLogged_doesNotBreakTheRun() {
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
    fun aGapOfNoSetLogged_doesNotStartTheRun() {
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(emptyList(), listOf(performed(reps = 3)), listOf(performed(reps = 3))),
        )

        assertNull("two misses and an absence are not three", history.stall())
    }

    @Test
    fun aSkippedSet_isNotAMissOfItsOwn() {
        // A set nobody logged is absence, not a failure — but the set that *was* logged and missed is enough
        // for the session to say nothing moved (N304).
        val history = history(
            planned = listOf(planSet(reps = 5), planSet(reps = 5, index = 1)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertEquals(3, history.stall()?.sessions)
    }

    @Test
    fun aSetAtTheTopOfItsRange_isProgress_notAMiss() {
        // A set that met the plan with nothing left to raise is not a failure. Counting it would tell a lifter
        // doing exactly what the plan asks to deload for ever (N304).
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 8, rpe = 7)),
                listOf(performed(reps = 8, rpe = 7)),
                listOf(performed(reps = 8, rpe = 7)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun anUnratedSetThatMetThePlan_isNotAMiss() {
        // The effort is skippable (N6). A set whose reps and load were met says no measurable harm, so it is
        // progress rather than a miss the rule can count.
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 7, rpe = null)),
                listOf(performed(reps = 7, rpe = null)),
                listOf(performed(reps = 7, rpe = null)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun aHoldThatFellShort_isAMiss() {
        // N242's direction, read by the same rule: a prescribed hold that keeps coming up short is a stall
        // like any other.
        val history = history(
            planned = listOf(planSet(reps = null, hold = 45)),
            sessions = listOf(
                listOf(performed(reps = 0, hold = 30)),
                listOf(performed(reps = 0, hold = 35)),
                listOf(performed(reps = 0, hold = 30)),
            ),
        )

        assertEquals(3, history.stall()?.sessions)
    }

    @Test
    fun aPlanOfWarmUpsOnly_hasNothingToStall() {
        val history = history(
            planned = listOf(planSet(reps = 5, role = SetType.WARMUP)),
            sessions = listOf(
                listOf(performed(reps = 1)),
                listOf(performed(reps = 1)),
                listOf(performed(reps = 1)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun aPlanThatCannotProgress_isNeverAStall() {
        // No target effort means no step can be earned: the miss is of a plan that could not move, and the fix
        // is a plan, not a deload (N304).
        val history = history(
            planned = listOf(planSet(reps = 5, rpe = null)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun aPlanWithNothingToMove_isNeverAStall() {
        // Fixed reps and no load: there is no direction the plan could take, so nothing can have stalled.
        val history = history(
            planned = listOf(planSet(reps = 5, range = false, weight = null)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun anOffMovement_isNeverAStall() {
        // N304: the movement's own answer wins (N235). The lifter said not to touch this lift's plan, and the
        // dev ice-wide setting is a different answer.
        val history = history(
            planned = listOf(planSet(reps = 5)),
            mode = ProgressionMode.OFF,
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertNull(history.stall())
    }

    @Test
    fun theRunKeepsGrowingPastThree() {
        val history = history(
            planned = listOf(planSet(reps = 5)),
            sessions = listOf(
                listOf(performed(reps = 3)),
                listOf(performed(reps = 4)),
                listOf(performed(reps = 3)),
                listOf(performed(reps = 3)),
            ),
        )

        assertEquals("the card says how many", 4, history.stall()?.sessions)
    }

    @Test
    fun aLoggedRung_isNotAWorkingSet_soTheAnchorStillStalls() {
        // B420: the rung is derived from the anchor and has no targets of its own. Counting it as a logged
        // set made every session with a drop ladder read as progress, so the plan never stalled.
        val history = history(
            planned = listOf(
                planSet(reps = 5),
                planSet(reps = null, index = 1, role = SetType.DROP, range = false),
            ),
            sessions = List(3) {
                listOf(
                    performed(reps = 3),
                    performed(reps = 10, role = SetType.DROP),
                )
            },
        )

        assertEquals("the anchor is the work set", 3, history.stall()?.sessions)
    }

    @Test
    fun aRungOnlySession_isAbsence_notProgress() {
        val history = history(
            planned = listOf(
                planSet(reps = 5),
                planSet(reps = null, index = 1, role = SetType.DROP, range = false),
            ),
            sessions = List(3) { listOf(performed(reps = 10, role = SetType.DROP)) },
        )

        assertNull(history.stall())
    }

    @Test
    fun aTargetlessSet_doesNotTurnEverySessionIntoProgress() {
        // B420: a set with no reps and no hold cannot miss, so it used to reset the run by being logged.
        val history = history(
            planned = listOf(
                planSet(reps = 5),
                planSet(reps = null, index = 1, range = false),
            ),
            sessions = List(3) { listOf(performed(reps = 3), performed(reps = 10)) },
        )

        assertEquals("the targetless set is not judged", 3, history.stall()?.sessions)
    }

    private fun history(
        planned: List<ProgressionPlanSet>,
        sessions: List<List<ProgressionPerformance>>,
        mode: ProgressionMode? = null,
    ) = PrescriptionHistory(
        exerciseName = "Back Squat",
        planned = planned,
        performedBySession = sessions,
        progressionMode = mode,
        templateId = "t1",
        templateExerciseId = "te1",
        exerciseId = "squat",
    )

    private fun planSet(
        reps: Int? = 5,
        index: Int = 0,
        weight: Long? = null,
        rpe: Int? = 8,
        role: SetType = SetType.NORMAL,
        hold: Int? = null,
        /** Whether the plan names a range to climb: fixed reps with no load cannot progress (N304). */
        range: Boolean = true,
    ) = ProgressionPlanSet(
        setId = "ts-$index",
        setIndex = index,
        role = role,
        targetWeightGrams = weight,
        targetRepsMin = reps,
        targetRepsMax = if (range && reps != null) reps + 3 else reps,
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
