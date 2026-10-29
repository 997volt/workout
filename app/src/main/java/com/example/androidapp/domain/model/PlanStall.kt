package com.example.androidapp.domain.model

import java.time.Instant

/**
 * A prescription that has stopped moving, told as one sentence about one set (ROADMAP N237).
 *
 * The app can already say what each set missed and why; this is the first thing it says about the *pattern*.
 * Three sessions in a row where the same set missed for the same measurable reason is a stall, and the answer
 * to a stall is a deload rather than another attempt at the same number — which is why the suggestion is worth
 * making, and why it only ever *suggests*: a deload is a decision about a week of training.
 */
data class PlanStall(
    /** The movement whose prescription stalled, as the library names it today. */
    val exerciseName: String,
    /** Which of the plan's working sets, counting the way the block numbers them — 1 is the first. */
    val setNumber: Int,
    /** The reason that repeated, as one of the kinds the rule can measure. */
    val kind: ProgressionMiss,
    /** How many sessions in a row it repeated for. */
    val sessions: Int,
)

/** One exercise as one finished session performed it (ROADMAP N237), in the order its sets were logged. */
data class PerformedExercise(
    val exerciseId: String,
    val sets: List<ProgressionPerformance>,
)

/**
 * One finished session started from a template, with what each of its exercises performed (ROADMAP N237).
 *
 * [performed] is in the session's own order, which is the order the plan was prescribed in.
 */
data class TemplateSession(
    val sessionId: String,
    val startedAt: Instant,
    val performed: List<PerformedExercise>,
)

/**
 * One exercise's plan next to what its last few sessions did with it (ROADMAP N237).
 *
 * [performedBySession] is **oldest first**, one entry per finished session that trained the movement, and the
 * sets in each are that session's own rows. A session that did not train it at all is simply absent: absence is
 * not a failure, and a session that was skipped cannot break a streak it was never part of.
 */
data class PrescriptionHistory(
    val exerciseName: String,
    val planned: List<ProgressionPlanSet>,
    val performedBySession: List<List<ProgressionPerformance>>,
)

/**
 * The kinds this set missed by, measured from the plan's own terms, or empty when it missed nothing (N237).
 *
 * **The rule asks for the kinds it can still measure, not the one the offer reported.** [ProgressionMiss] is
 * stated by precedence — a set that came up short *and* was over its effort reports the reps — so counting the
 * reported reason would let "over the target" silently become "short" between two sessions failing the same
 * way. Only three kinds exist here at all: `NOT_DONE` is absence, `NOT_IN_PLAN` is extra work, and the rest
 * describe a plan that cannot progress rather than a lifter who did not.
 *
 * A hold is the same question asked of seconds (N242).
 */
fun measurableKinds(
    planned: ProgressionPlanSet,
    done: ProgressionPerformance,
): Set<ProgressionMiss> {
    val plannedHold = planned.targetDurationSeconds
    if (plannedHold != null) return holdKinds(planned, done, plannedHold)

    return buildSet {
        // The number the session was asked for, the way the offer reads it (N74).
        val reps = planned.targetRepsCurrent ?: planned.targetRepsMin ?: planned.targetRepsMax
        if (reps != null && done.reps < reps) add(ProgressionMiss.REPS_SHORT)
        if (overEffort(planned, done)) add(ProgressionMiss.OVER_TARGET_RPE)
        val plannedLoad = planned.targetWeightGrams
        if (plannedLoad != null && plannedLoad > 0L && done.weightGrams < plannedLoad) {
            add(ProgressionMiss.LOAD_SHORT)
        }
    }
}

/** The same two questions asked of a hold: was it long enough, and was it inside the effort (N242). */
private fun holdKinds(
    planned: ProgressionPlanSet,
    done: ProgressionPerformance,
    plannedHold: Int,
): Set<ProgressionMiss> = buildSet {
    val held = done.durationSeconds
    if (held != null && held < plannedHold) add(ProgressionMiss.HOLD_SHORT)
    if (overEffort(planned, done)) add(ProgressionMiss.OVER_TARGET_RPE)
}

/** True when the set was harder than the plan allowed, which both halves of the rule ask. */
private fun overEffort(planned: ProgressionPlanSet, done: ProgressionPerformance): Boolean {
    val target = planned.targetRpeHalves
    val recorded = done.rpeHalves
    return target != null && recorded != null && recorded > target
}

/**
 * The stall one exercise's plan is in, or null when it is still moving (ROADMAP N237).
 *
 * The walk is per **prescription**: the plan's Nth working set is paired with the session's Nth, warm-ups
 * removed from both sides, exactly as the offer pairs them (N17, N74) — so a plan whose second set is always the
 * one that fails says *that*, rather than saying the exercise is stuck. A set nobody logged is absence and does
 * not count; an answered set ends that kind's run, because the prescription moved.
 *
 * [threshold] is three by the rule, and a parameter because the number is a judgement rather than a fact about
 * the data: it is the point where "the same thing keeps happening" stops being a coincidence.
 */
fun PrescriptionHistory.stall(threshold: Int = STALL_SESSIONS): PlanStall? {
    val plannedWork = planned.filterNot { it.role == SetType.WARMUP }
    if (plannedWork.isEmpty()) return null

    // One run per (set position, kind), and the longest completed one wins: a plan can stall in two places at
    // once, and the sentence to read first is the one that has been true longest.
    val run = mutableMapOf<Pair<Int, ProgressionMiss>, Int>()
    var longest: Triple<Int, ProgressionMiss, Int>? = null

    performedBySession.forEach { session ->
        val performedWork = session.filterNot { it.role == SetType.WARMUP }
        plannedWork.forEachIndexed { index, plannedSet ->
            val done = performedWork.getOrNull(index)
            val kinds = if (done == null) emptySet() else measurableKinds(plannedSet, done)
            MEASURABLE_KINDS.forEach { kind ->
                val key = index to kind
                if (kind in kinds) {
                    val next = (run[key] ?: 0) + 1
                    run[key] = next
                    if (next >= (longest?.third ?: 0) && next >= threshold) {
                        longest = Triple(index, kind, next)
                    }
                } else {
                    run[key] = 0
                }
            }
        }
    }

    return longest?.let { found ->
        PlanStall(
            exerciseName = exerciseName,
            setNumber = found.first + 1,
            kind = found.second,
            sessions = found.third,
        )
    }
}

/**
 * The kinds a stall is counted by, in the order a sentence would name them (ROADMAP N237).
 *
 * Exactly the three the rule allows. The list is here rather than derived from the enum so that adding a reason
 * to the offer cannot quietly start counting a lifter's absence as a failure.
 */
val MEASURABLE_KINDS: List<ProgressionMiss> = listOf(
    ProgressionMiss.REPS_SHORT,
    ProgressionMiss.OVER_TARGET_RPE,
    ProgressionMiss.LOAD_SHORT,
    ProgressionMiss.HOLD_SHORT,
)

/** How many sessions in a row make a stall: three, the rule the roadmap sets (N237). */
const val STALL_SESSIONS: Int = 3

/** How many of a plan's sessions the rule looks back over — enough for three, and for the run to be visible. */
const val STALL_SESSION_WINDOW: Int = 12
