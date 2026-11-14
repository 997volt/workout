package com.example.androidapp.domain.model

/**
 * What one lift's deloaded plan becomes (ROADMAP N307).
 *
 * The per-lift deload is a **plan edit**, so its two halves are computed together: the work sets it
 * removes and the kept work sets' new targets. The confirm and the write read the same function, which is
 * what keeps the before → after the lifter approves from drifting from the one that lands.
 *
 * The trim is shared with the replace (N308): N is the plan's **stand-alone working sets**
 * ([SetType.recordsEffort] — a warm-up and a rung are neither), and it keeps the first
 * `min(N, max(2, N − round(0.3 × N)))`. Rounding is half up, so the survivor counts are
 * `1, 2, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8` for N = 1…12. The later **anchors** go, and [TemplateSet]'s
 * own removal rule takes their rungs with them; a kept anchor keeps its ladder.
 *
 * The load is the movement's own step (N77), three times: a weighted set loses three steps, clamped at
 * zero; an assisted set gains three steps of assistance (more help is lighter); a set that states
 * assistance moves its assistance and leaves the weight alone; a bodyweight set, a hold and a warm-up
 * are unchanged. A counted set whose plan wrote a range moves its current target to the range's floor;
 * a fixed-rep plan and a hold keep the target they had.
 */
data class ExerciseDeload(
    /** The later work sets to remove, in plan order. Their rungs go with them; warm-ups never do. */
    val removedSetIds: List<String>,
    /** The kept work sets, in plan order, with the targets the deload leaves them on. */
    val kept: List<DeloadedSet>,
)

/** One kept working set after the deload: the weight, assistance and current reps it is left on. */
data class DeloadedSet(
    val setId: String,
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    /**
     * The new current target for a ranged plan, or null when the plan is not a range (a fixed rep
     * count, a hold, or no reps at all) and the set's own number should not move.
     */
    val repsCurrent: Int?,
)

/** The share of the working sets the trim takes: three tenths, rounded half up. */
private const val CUT_NUMERATOR = 3
private const val CUT_DENOMINATOR = 10
private const val CUT_ROUNDING = 5

/** The load steps one deload takes off a kept set (N77, N307). */
private const val LOAD_STEPS = 3

/** The deloaded targets for [sets], using the movement's own [stepGrams] (N77, N307). */
fun deloadOf(sets: List<TemplateSet>, stepGrams: Long): ExerciseDeload {
    val work = sets.filter { it.role.recordsEffort }.sortedBy { it.setIndex }
    val keep = keepWorkingSets(work.size)
    return ExerciseDeload(
        removedSetIds = work.drop(keep).map { it.id },
        kept = work.take(keep).map { deloadTarget(it, stepGrams) },
    )
}

/**
 * The number of stand-alone work sets a plan of [workSets] keeps through one trim (N307, N308).
 *
 * The two-set floor is the rule's own: a deload that leaves one set is not a deload, it is a deletion.
 */
fun keepWorkingSets(workSets: Int): Int {
    if (workSets <= 0) return 0
    val cut = (CUT_NUMERATOR * workSets + CUT_ROUNDING) / CUT_DENOMINATOR
    return minOf(workSets, maxOf(2, workSets - cut))
}

private fun deloadTarget(set: TemplateSet, stepGrams: Long): DeloadedSet {
    val assistance = set.targetAssistanceGrams
    val weight = set.targetWeightGrams
    val newWeight: Long?
    val newAssistance: Long?
    when {
        // **Assistance first**: a set that states it is assisted, and more help is lighter. A set that
        // states both moves its assistance too, because the weight column on an assisted set is not the load.
        assistance != null && assistance > 0L -> {
            newWeight = weight
            newAssistance = assistance + LOAD_STEPS * stepGrams
        }
        weight != null && weight > 0L -> {
            newWeight = (weight - LOAD_STEPS * stepGrams).coerceAtLeast(0L)
            newAssistance = assistance
        }
        else -> {
            newWeight = weight
            newAssistance = assistance
        }
    }
    return DeloadedSet(
        setId = set.id,
        weightGrams = newWeight,
        assistanceGrams = newAssistance,
        repsCurrent = rangedFloor(set),
    )
}

/** The floor of a counted set's range, or null when the plan does not prescribe a range (N74). */
private fun rangedFloor(set: TemplateSet): Int? {
    val min = set.targetRepsMin
    val max = set.targetRepsMax
    // A hold is measured in seconds, not reps (N242): its range, if any, is not the one a rep deload moves.
    if (set.targetDurationSeconds != null) return null
    return min?.takeIf { max != null && min != max }
}
