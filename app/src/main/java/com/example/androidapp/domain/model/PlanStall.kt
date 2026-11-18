package com.example.androidapp.domain.model

import java.time.Instant

/**
 * A prescription that has stopped moving, told about the exercise rather than one of its sets (N237, N304).
 *
 * The app can already say what each set missed and why; this is the first thing it says about the *pattern*.
 * Three sessions in a row in which nothing moved is a stall, and the answer to a stall is a deload rather than
 * another attempt at the same number — which is why the suggestion is worth making, and why it only ever
 * *suggests*: a deload is a decision about a week of training.
 *
 * **The unit is the exercise** (N304): which set missed and how is a detail that changes session to session —
 * one set short this week, the load short the next — so a rule keyed on the set and the reason never added up
 * for a lift that kept failing. The deload it suggests is addressed to the lift, not to one of its sets.
 */
data class PlanStall(
    /** The movement whose prescription stalled, as the library names it today. */
    val exerciseName: String,
    /** How many consecutive sessions in which nothing moved: [STALL_SESSIONS] or more. */
    val sessions: Int,
    /** The template the stalled plan belongs to, so the answer edits the right plan (N307). */
    val templateId: String,
    /** The plan's exercise row: the target a deload or replace writes (N307, N308). */
    val templateExerciseId: String,
    /** The library movement the row names, which a replace starts from (N308). */
    val exerciseId: String,
)

/** One exercise as one finished session performed it (ROADMAP N237), in the order its sets were logged. */
data class PerformedExercise(
    val exerciseId: String,
    val sets: List<ProgressionPerformance>,
    /** The session exercise these sets belong to, or null in a test/older read (B471). */
    val sessionExerciseId: String? = null,
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
 * The [occurrence]th performed entry for a movement in this session (B446), or null.
 *
 * A session's rows are in the order the plan prescribed, and a plan may name the same movement twice (N54).
 * The nth plan row's history is therefore the nth entry of that movement, not whichever came first (B428's
 * rule for the finish, applied to the card reads).
 */
fun TemplateSession.performedOccurrence(exerciseId: String, occurrence: Int): PerformedExercise? =
    performed.filter { it.exerciseId == exerciseId }.getOrNull(occurrence)

/**
 * One exercise's plan next to what its last few sessions did with it (ROADMAP N237).
 *
 * [performedBySession] is **oldest first**, one entry per finished session that trained the movement, and the
 * sets in each are that session's own rows. A session that did not train it at all is simply absent: absence is
 * not a failure, and a session that was skipped cannot break a streak it was never part of.
 *
 * [progressionMode] is the movement's own answer to what *Done* does (N235), nullable as it is stored. Only
 * `OFF` excludes the movement from the rule (N304); the device-wide setting is not read here.
 */
data class PrescriptionHistory(
    val exerciseName: String,
    /**
     * The plan as it stands today, used for the exercise's eligibility and as the fallback for a
     * session that predates the snapshot.
     */
    val planned: List<ProgressionPlanSet>,
    val performedBySession: List<List<ProgressionPerformance>>,
    /**
     * The prescription each session was trained under, aligned with [performedBySession] (B471).
     *
     * A null entry is a session from before the snapshot existed; it falls back to [planned], which
     * is the old behaviour. Empty means the same for every session.
     */
    val plannedBySession: List<List<ProgressionPlanSet>?> = emptyList(),
    val progressionMode: ProgressionMode? = null,
    /** The plan's identity, carried through to the [PlanStall] so its answer can find the row (N307). */
    val templateId: String,
    val templateExerciseId: String,
    val exerciseId: String,
)

/**
 * The kinds this set missed by, measured from the plan's own terms, or empty when it missed nothing (N237).
 *
 * **The rule asks for the kinds it can still measure, not the one the offer reported.** [ProgressionMiss] is
 * stated by precedence — a set that came up short *and* was over its effort reports the reps — so counting the
 * reported reason would let "over the target" silently become "short" between two sessions failing the same
 * way. Only four kinds exist here at all: `NOT_DONE` is absence, `NOT_IN_PLAN` is extra work, and the rest
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

/**
 * The same three questions asked of a hold: was the set a hold at all, was it long enough, and was it
 * inside the effort (N242, B457).
 *
 * A counted set is not a hold. Leaving that case out let it count as "met the plan" — there was no
 * short duration to measure and no effort to fault — so three sessions of reps against a plan of
 * seconds reset the stall run instead of raising the deload. The offer already calls the same input
 * [ProgressionMiss.HOLD].
 */
private fun holdKinds(
    planned: ProgressionPlanSet,
    done: ProgressionPerformance,
    plannedHold: Int,
): Set<ProgressionMiss> = buildSet {
    val held = done.durationSeconds
    when {
        held == null -> add(ProgressionMiss.HOLD)
        held < plannedHold -> add(ProgressionMiss.HOLD_SHORT)
    }
    if (overEffort(planned, done)) add(ProgressionMiss.OVER_TARGET_RPE)
}

/** True when the set was harder than the plan allowed, which both halves of the rule ask. */
private fun overEffort(planned: ProgressionPlanSet, done: ProgressionPerformance): Boolean {
    val target = planned.targetRpeHalves
    val recorded = done.rpeHalves
    return target != null && recorded != null && recorded > target
}

/**
 * The stall one exercise's plan is in, or null when it is still moving (ROADMAP N237, N304).
 *
 * The walk is by **session**. A session counts when the exercise logged at least one prescribed working set,
 * and it is a *no-progress* session when **every** logged prescribed set missed for a measurable reason
 * ([measurableKinds]) — which set failed and why no longer has to repeat, because the exercise is the unit. A
 * session with any logged set that met the plan is progress and ends the run, **including a set that topped
 * out**: the plan has nothing left to raise there, and a lifter doing exactly what it asks must not be told to
 * deload for ever. A session that trained the exercise without logging a prescribed working set is absence: it
 * neither counts nor breaks the run (B304). The plan's sets are paired with the session's by class through the
 * offer's own helper, so a logged rung is extra rather than the stand-in for a work set the session skipped
 * (B305).
 *
 * **Only a plan that can progress is judged** ([ProgressionRule.progresses]): a plan with no target effort, no
 * reps, or nothing to move never earns a step, so calling it stalled would ask for a deload a plan could not
 * have prevented. **A movement whose own answer is Off is never counted** (N235): the app was told not to touch
 * this lift's plan, and that is a different answer from the device-wide setting.
 *
 * The count is the run still open at the newest session (B303), and it keeps growing past [threshold]; the card
 * says how many sessions it has been. [threshold] is three by the rule, and a parameter because the number is a
 * judgement rather than a fact about the data: it is the point where "nothing is moving" stops being a
 * coincidence.
 */
fun PrescriptionHistory.stall(threshold: Int = STALL_SESSIONS): PlanStall? {
    // Only a stand-alone working set with a number the rule can judge takes part. A rung is derived from the
    // set above it (N79, B305) and a targetless set has nothing to measure (B420): either would read as a
    // set that "met" the plan because `measurableKinds` has nothing to report, resetting a run it never
    // answered. A session that logged only those is absent, not progress.
    val plannedWork = planned.judgeableWork()
    // The exercise's effort is carried onto every prescribed set (N59), so any work set answers for the plan.
    val eligible = progressionMode != ProgressionMode.OFF &&
        plannedWork.isNotEmpty() &&
        progressionRuleFor(plannedWork.first().targetRpeHalves, planned).progresses
    if (!eligible) return null

    var run = 0
    performedBySession.forEachIndexed { index, session ->
        // The prescription this session was trained under, not the plan that has moved since (B471).
        // A missing snapshot falls back to the living plan, which is how older sessions are judged.
        val sessionPlan = plannedBySession.getOrNull(index) ?: planned
        val sessionWork = sessionPlan.judgeableWork()
        // A session with no prescribed work set says nothing either way (B304).
        if (sessionWork.isEmpty()) return@forEachIndexed
        // The offer's own pairing (B305): a logged rung is extra, never the stand-in for a work set the
        // session did not log, and now never a working set of its own (B420).
        val paired = pairedPerformances(sessionPlan, session)
        val logged = sessionWork.mapNotNull { plannedSet ->
            paired.bySetId[plannedSet.setId]?.let { done -> plannedSet to done }
        }
        // A session that logged no prescribed work set says nothing either way (B304): it can neither start
        // nor break a run it was never part of.
        if (logged.isEmpty()) return@forEachIndexed
        val missedEverywhere = logged.all { (plannedSet, done) -> measurableKinds(plannedSet, done).isNotEmpty() }
        run = if (missedEverywhere) run + 1 else 0
    }

    return run.takeIf { it >= threshold }?.let {
        PlanStall(
            exerciseName = exerciseName,
            sessions = it,
            templateId = templateId,
            templateExerciseId = templateExerciseId,
            exerciseId = exerciseId,
        )
    }
}

/** The sets a stall can judge: stand-alone work with a rep or hold number, in the plan's order. */
private fun List<ProgressionPlanSet>.judgeableWork(): List<ProgressionPlanSet> = this
    .filter { set ->
        set.role.isWork && !set.role.isRung &&
            (
                set.targetRepsCurrent ?: set.targetRepsMin ?: set.targetRepsMax != null ||
                    set.targetDurationSeconds != null
                )
    }
    .sortedBy { it.setIndex }

/** How many sessions in a row make a stall: three, the rule the roadmap sets (N237, N304). */
const val STALL_SESSIONS: Int = 3
