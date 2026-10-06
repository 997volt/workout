package com.example.androidapp.domain.model

/**
 * The next step each planned set earned, as a decision the lifter makes (ROADMAP N50, N74).
 *
 * N59 withdrew the app's proposal rather than the question: with the next set's values already fields
 * on the screen, a chip beside them had nothing to add. What it left was *where* a proposal belongs
 * and *what* earns one. **Done** is where — the work is over, and what comes next is the decision
 * that remains — and the plan's own targets are what: the offer exists only where the plan named a
 * target RPE for the exercise and the set met it, so the app states a step it can read out of the plan
 * and the log rather than guessing from history.
 *
 * **N74 made that step per set, and held it inside the plan's rep range.** Every prescribed working
 * set is judged on its own rather than the exercise as a whole, so a set that was answered earns its
 * step even when a sibling missed. The range a set was authored with — "Reps from"/"Reps to" — is
 * never edited by progression: a set below its ceiling earns **reps**, and a set at the ceiling earns
 * the **weight**, which puts the climb back on the range's floor. The two directions are therefore
 * exclusive wherever the plan wrote a ceiling; a plan that wrote none keeps both, as it always has.
 *
 * **The app still only suggests.** The offer is inert until the lifter accepts one of its directions,
 * and accepting writes the plan and nothing else (DECISIONS.md's "the app suggests; it never writes").
 */

/** The directions a lifter can accept (N50). Enums by name, never ordinal. */
enum class ProgressionDirection { LOAD, REPS }

/** One target and the raised one (N50). */
data class ProgressionStep<T>(val from: T, val to: T)

/**
 * One prescribed set, reduced to what progression reads and what the accepted write must keep
 * (N50, N74).
 *
 * Every target is nullable, exactly as it is on the plan: a set that names no load cannot have one
 * raised, and a set that names no reps cannot have its work checked. There is one plan to write back
 * to since N73 — the template's — so nothing here says which one it came from.
 *
 * [targetRpeHalves] is the **exercise's one number**, carried onto every prescribed set by the
 * caller (N59, amended): the rule measures each set against the plan's single target, and a plan that
 * names none leaves every set here null. A set's own stored value is the caller's fallback for a plan
 * imported from a backup written before the effort moved to the exercise.
 */
data class ProgressionPlanSet(
    val setId: String,
    val setIndex: Int,
    val role: SetType = SetType.NORMAL,
    val targetWeightGrams: Long? = null,
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    /** Where in the range the lifter has climbed, or null (ROADMAP N74). */
    val targetRepsCurrent: Int? = null,
    val targetRpeHalves: Int? = null,
    val note: String? = null,
    /**
     * The set's **own** stored value of the legacy per-set RPE column, or null (N59).
     *
     * [targetRpeHalves] is the exercise's one number, which the caller carries onto every set so the
     * rule can measure each against it. This is what the set row itself holds, and it is what an
     * accepted step must put back: writing [targetRpeHalves] into the set would copy the exercise's
     * number into a column the reader falls back to, so clearing the exercise's field later would
     * silently resurrect a value the lifter never gave that set.
     */
    val legacyRpeHalves: Int? = null,
) {
    /**
     * The reps this set asks for: where the lifter has climbed to, falling back to the range's floor
     * and then its ceiling — the reading `plannedTargetFor` shares, so the session and the prompt
     * agree about what "5–8" asks for once progression has moved it (N74).
     */
    val targetReps: Int? get() = targetRepsCurrent ?: targetRepsMin ?: targetRepsMax

    /**
     * True when the plan wrote a real range for the weight step to restart (N74).
     *
     * Both ends have to be there and differ: "5 to 5" is one number rather than a range, and a plan
     * that named only one end has no floor to return to — so a weight step on either leaves the reps
     * exactly where they are.
     */
    val restartsAtFloor: Boolean
        get() = targetRepsMin != null && targetRepsMax != null && targetRepsMin < targetRepsMax
}

/** One performed set, reduced to what progression reads (N50). Warm-ups are dropped by the rule. */
data class ProgressionPerformance(
    val reps: Int,
    val weightGrams: Long = 0L,
    val assistanceGrams: Long = 0L,
    val rpeHalves: Int? = null,
    val role: SetType = SetType.NORMAL,
)

/**
 * Why a working set earned no step (ROADMAP N74).
 *
 * Stated rather than left blank because *Done* has to say something about work that was not answered:
 * a lifter reading "2 reps short of the plan" learns what the app measured, where an empty row would
 * read as a bug. The last one is the only case where the set *was* answered and still has nowhere to
 * go.
 */
enum class ProgressionMiss {
    /** The plan prescribed it and the session never logged it. */
    NOT_DONE,

    /** Work the plan does not name — an extra set — so there is no target a step could move. */
    NOT_IN_PLAN,

    /** The set names no reps, so there is nothing to check the work against or to raise. */
    NO_REP_TARGET,

    /** The exercise names no target RPE, so the app cannot tell whether there was room in hand. */
    NO_TARGET_RPE,

    /** The set was logged without an effort, so nothing is known about the room in hand. */
    UNRATED,

    /** Performed short of the reps this set asked for. */
    REPS_SHORT,

    /** Performed above the effort the plan asked for. */
    OVER_TARGET_RPE,

    /**
     * Answered at the top of the range with no load to raise (N74).
     *
     * A bodyweight or assisted set has no added weight a step can move, so reaching its ceiling is
     * the end of the app's suggestions — it says so rather than offering a rep past the range.
     */
    TOPPED_OUT,

    /**
     * A rung of a group, which is never judged on its own (ROADMAP N79).
     *
     * A drop or cluster set carries no target of its own: the load is derived from the set above it,
     * the reps are the same set's own history, and the step the group earned belongs to that first
     * set. Stated rather than left blank, because a lifter reading a row with nothing beside it would
     * take it for a bug.
     */
    RUNG_OF_A_GROUP,
}

/** One working set as the prompt states it: the plan, what was done, and what it earned (N74). */
data class ProgressionSetPrompt(
    val planned: ProgressionPlanSet?,
    val performed: ProgressionPerformance?,
    /** The step this set earned, or null — in which case [miss] says why (N74). */
    val offer: ProgressionOffer? = null,
    /** Why no step was offered, or null when one was (N74). */
    val miss: ProgressionMiss? = null,
)

/**
 * What the done prompt states, and the steps it offers (ROADMAP N50, N74).
 *
 * One entry per prescribed working set, in the plan's own order, and then any logged work the plan
 * does not name — so the prompt is the whole session rather than the exercise's last set. An exercise
 * with no plan at all has nothing to state, and the screen does not ask (N50).
 */
data class ProgressionPrompt(
    val sets: List<ProgressionSetPrompt> = emptyList(),
) {
    /** True where the plan can answer *Done* at all: one prescribed set is enough (N50, N74). */
    val hasPlan: Boolean get() = sets.any { it.planned != null }

    /** How many sets earned a step: what the screen says when nothing did (N74). */
    val earned: Int get() = sets.count { it.offer != null }
}

/**
 * The step one set earned, and the plan set it would change (ROADMAP N50, N74).
 *
 * Either direction may be absent — a bodyweight set has no load to raise, a plan may name no reps —
 * and for a set inside a written range **exactly one is offered**: below the ceiling the reps move
 * and the weight waits, at the ceiling the weight moves and the reps restart. A plan that wrote no
 * ceiling is the one case that keeps both.
 */
data class ProgressionOffer(
    val set: ProgressionPlanSet,
    /** The rep step, or null where the plan's range has no room left in it (N74). */
    val reps: ProgressionStep<Int>? = null,
    /** The load step, or null where the plan names no added weight to raise (N15, N74). */
    val load: ProgressionStep<Long>? = null,
) {
    /**
     * The set as accepting [direction] would leave the plan, or null when that direction is not
     * offered.
     *
     * A rep moves **where the lifter is in the range** and never the range's two ends (N74). A weight
     * step on a real range puts that climb back on the floor — the range is started again a step
     * heavier — while a plan that named no range keeps the reps it had.
     */
    fun accepted(direction: ProgressionDirection): ProgressionPlanSet? = when (direction) {
        ProgressionDirection.LOAD -> load?.let { step ->
            set.copy(
                targetWeightGrams = step.to,
                targetRepsCurrent = set.targetRepsMin.takeIf { set.restartsAtFloor } ?: set.targetRepsCurrent,
            )
        }

        ProgressionDirection.REPS -> reps?.let { set.copy(targetRepsCurrent = it.to) }
    }
}

/**
 * The prompt one exercise's *Done* opens with (ROADMAP N50, N74).
 *
 * The plan's Nth working set is paired with the session's Nth, warm-ups removed from both sides
 * (N17, N20, N22), and each pair is judged on its own. Work the plan does not name is stated after
 * them with no step to offer, so the prompt accounts for the whole session. An exercise with no plan
 * states no sets rather than failing.
 */
fun progressionPromptFor(
    planned: List<ProgressionPlanSet>,
    performed: List<ProgressionPerformance>,
    /**
     * The smallest loadable step in the unit this exercise is read in (ROADMAP N64): 2.5 kg for a
     * metric lifter, 5 lb for a pound-loading machine, so the offered step is one it can load.
     */
    stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
): ProgressionPrompt {
    val plannedWork = planned.plannedWork()
    val performedWork = performed.performedWork()

    // **Paired by class, not by raw position** (ROADMAP N79). A rung logged without a plan row used
    // to be counted as a work set, which shifted every later pair: the app compared a working set
    // against a lighter drop's reps and offered it a heavier weight, and accepting wrote that into the
    // plan. Prescribed work sets pair with performed work sets, and prescribed rungs with performed
    // rungs, each in order — so an extra rung can only ever be extra.
    val performedWorkIndexes = performedWork.indices.filterNot { performedWork[it].role.isRung }
    val performedRungIndexes = performedWork.indices.filter { performedWork[it].role.isRung }
    val plannedOf = { rungs: Boolean -> plannedWork.filter { it.role.isRung == rungs } }

    val pairs = mutableMapOf<String, ProgressionPerformance?>()
    val used = mutableSetOf<Int>()
    listOf(true, false).forEach { rungs ->
        plannedOf(rungs).forEachIndexed { index, set ->
            val at = (if (rungs) performedRungIndexes else performedWorkIndexes).getOrNull(index)
            pairs[set.setId] = at?.let { performedWork[it] }
            at?.let { used += it }
        }
    }

    val prescribed = plannedWork.map { set -> set.toPrompt(pairs[set.setId], stepGrams) }
    val extra = performedWork
        .filterIndexed { index, _ -> index !in used }
        .map { ProgressionSetPrompt(planned = null, performed = it, miss = ProgressionMiss.NOT_IN_PLAN) }
    return ProgressionPrompt(sets = prescribed + extra)
}

/**
 * One prescribed set and what the session did to it (N74).
 *
 * The offer and the miss are two halves of one judgement, so they are made together: a set either
 * earned a step or has a reason it did not, never both and never neither.
 */
private fun ProgressionPlanSet.toPrompt(
    done: ProgressionPerformance?,
    stepGrams: Long,
): ProgressionSetPrompt {
    // A rung is never judged: the group's step is earned or missed by the set it hangs off (N79).
    if (role.isRung) {
        return ProgressionSetPrompt(
            planned = this,
            performed = done,
            miss = ProgressionMiss.RUNG_OF_A_GROUP,
        )
    }

    val unanswered = answeredMiss(done)
    val offer = if (unanswered == null && done != null) offerFor(done, stepGrams) else null
    return ProgressionSetPrompt(
        planned = this,
        performed = done,
        offer = offer,
        // An answered set with no step is the one case the plan cannot take further (N74).
        miss = unanswered ?: ProgressionMiss.TOPPED_OUT.takeIf { offer == null },
    )
}

/**
 * Why the plan's own terms were not met, or null when they were (N74).
 *
 * Every check the old exercise-wide rule made, made against one set: the plan has to name reps and
 * one target effort for the exercise, the session has to have recorded an effort, and the work has to
 * have met the reps without going over that effort.
 */
private fun ProgressionPlanSet.answeredMiss(done: ProgressionPerformance?): ProgressionMiss? {
    val reps = targetReps
    val rpeTarget = targetRpeHalves
    return when {
        done == null -> ProgressionMiss.NOT_DONE
        reps == null -> ProgressionMiss.NO_REP_TARGET
        rpeTarget == null -> ProgressionMiss.NO_TARGET_RPE
        done.rpeHalves == null -> ProgressionMiss.UNRATED
        done.reps < reps -> ProgressionMiss.REPS_SHORT
        done.rpeHalves > rpeTarget -> ProgressionMiss.OVER_TARGET_RPE
        else -> null
    }
}

/**
 * The step an answered set earned, or null when the range has nothing left to give (N74).
 *
 * **Below the range's ceiling the reps move and the weight waits**, and the target becomes one past
 * what was actually done rather than one past the number the plan asked for — asked 5, did 7 of an 8
 * ceiling, next target 8, so the plan tracks the lifter. **At the ceiling the weight moves instead**,
 * and a plan that wrote no ceiling is the case that keeps both directions, as it always did. A set at
 * the ceiling with no load to raise returns null, which [toPrompt] states as [ProgressionMiss.TOPPED_OUT].
 */
private fun ProgressionPlanSet.offerFor(
    done: ProgressionPerformance,
    stepGrams: Long,
): ProgressionOffer? {
    val reps = targetReps ?: return null
    val ceiling = targetRepsMax
    val load = addedWeightGrams?.let { ProgressionStep(it, it + stepGrams) }
    return when {
        ceiling == null -> ProgressionOffer(
            set = this,
            reps = ProgressionStep(reps, reps + REPS_PER_STEP),
            load = load,
        )

        done.reps < ceiling -> ProgressionOffer(
            set = this,
            reps = ProgressionStep(reps, minOf(done.reps + REPS_PER_STEP, ceiling)),
        )

        else -> load?.let { ProgressionOffer(set = this, load = it) }
    }
}

/**
 * The **added** weight this set names, or null (N15).
 *
 * A load a step can raise is a bar's kilograms, and only that. An assisted set stores its number as
 * a magnitude of help with `targetWeightGrams = 0` — the shape both plan editors write for `-20` —
 * so a non-null column is not on its own a load to raise; raising it would put 2.5 kg on a machine
 * doing 20 kg of the work, and `Weight.display` would then show `-20` and hide the write. A
 * bodyweight set's `0` is the same absence. The warm-up ramp already guards this (`<= 0`), and this
 * is the rule's own half of it.
 */
private val ProgressionPlanSet.addedWeightGrams: Long?
    get() = targetWeightGrams?.takeIf { it > 0L && targetAssistanceGrams == null }

/** The sets that are work: a warm-up is not what a target is measured against (N17, N20, N22). */
private fun List<ProgressionPlanSet>.plannedWork(): List<ProgressionPlanSet> =
    filterNot { it.role == SetType.WARMUP }.sortedBy { it.setIndex }

/** The same exclusion on the session's side, in the order the sets were logged (N50). */
private fun List<ProgressionPerformance>.performedWork(): List<ProgressionPerformance> =
    filterNot { it.role == SetType.WARMUP }

/** One rep is the step a rep direction raises by: the smallest unit a plan counts in. */
private const val REPS_PER_STEP = 1
