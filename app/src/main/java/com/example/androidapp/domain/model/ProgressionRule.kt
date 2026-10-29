package com.example.androidapp.domain.model

/**
 * What a plan does with the step its sets earn (P3.17, corrected by B181; shared by the plan editor and the
 * workout screen by N227).
 *
 * The app does not choose the step: it is the movement's own (N77, N111) and the gate is the plan's target RPE
 * (N50). The rule says which of the two directions a plan can move at all, which is why the plan editor states
 * it and why the session states it too — the same rule read from the same sets, so the two screens cannot
 * promise different steps.
 *
 * **The rule is not the weight.** N74 moves a set's reps while it is below its range's ceiling and the weight
 * only at the ceiling, so a 5–8 plan progresses by one rep per answered set long before it adds 2.5 kg. The
 * version before B181 printed the weight step as though it were the only one.
 *
 * It reads [ProgressionPlanSet]s rather than a template, because both callers have those: the plan editor maps
 * its own sets and the session already carries the plan's, so there is one shape for one rule.
 */
enum class ProgressionRule {
    /** The plan names no target RPE, so the gate never opens (N50). */
    NO_TARGET_RPE,

    /** The plan names no reps, so there is nothing to measure the work against or to raise (N74). */
    NO_REPS,

    /** Below the ceiling the reps move; at it, the movement's own weight step (N74). */
    REPS_THEN_LOAD,

    /** Reps below the ceiling, and nothing to raise once the ceiling is reached (N74). */
    REPS_ONLY,

    /** A fixed rep target, so the weight is what moves (N74). */
    LOAD_ONLY,

    /** A fixed rep target and no weight: answered work has nowhere to go (N74's TOPPED_OUT). */
    NOTHING_TO_MOVE,
}

/**
 * Which sentence a plan can truthfully print about its own progression (B181, N227).
 *
 * Pure, so each shape is covered by fast JVM tests rather than by reading a screen: the gate, the missing reps,
 * the range that gives the reps room, and the fixed target where the weight is the only step.
 *
 * [targetRpeHalves] is the exercise's one number, passed in rather than read off the sets: a plan row with an
 * effort and no sets yet is a real plan, and reading the RPE from a set would call it ungated.
 */
fun progressionRuleFor(targetRpeHalves: Int?, sets: List<ProgressionPlanSet>): ProgressionRule {
    // A warm-up is not what a target is measured against (N17, N20, N22), so it cannot decide the sentence.
    val work = sets.filterNot { it.role == SetType.WARMUP }
    val namesReps = work.any {
        it.targetRepsMin != null || it.targetRepsMax != null || it.targetRepsCurrent != null
    }
    val namesLoad = work.any { it.namesAddedLoad }
    return when {
        targetRpeHalves == null -> ProgressionRule.NO_TARGET_RPE
        !namesReps -> ProgressionRule.NO_REPS
        // A range is what gives the rep step somewhere to climb, and its ceiling is where the weight moves.
        work.any { it.restartsAtFloor } -> if (namesLoad) {
            ProgressionRule.REPS_THEN_LOAD
        } else {
            ProgressionRule.REPS_ONLY
        }

        namesLoad -> ProgressionRule.LOAD_ONLY
        else -> ProgressionRule.NOTHING_TO_MOVE
    }
}
