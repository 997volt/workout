package com.example.androidapp.domain.model

/**
 * What one lift's replacement plan writes to the new exercise row (ROADMAP N308).
 *
 * The replace is a **plan edit** (N291's session substitution is the other thing): a new exercise row at
 * the old position, with new set ids, so the existing Auto progression baseline cannot carry the old
 * movement's numbers onto the new one. This is the pure half — the trim, the seed by place and the
 * warm-up clearing — so the confirm and the write read the same function.
 *
 * The trim is N307's: N stand-alone working sets keep `min(N, max(2, N − round(0.3N)))`, the later
 * anchors go with their rungs, and a kept anchor keeps its ladder. The kept work sets are seeded **by
 * place** from the new movement's last finished session ([seeds], one per kept work set, in order); a
 * seeded set takes the performed weight, assistance and reps (or its hold) and **not** the old plan's
 * range. With no seed its target is cleared. **Empty** is the deliberate other choice: the old plan's
 * reps and current stay, the loads are cleared, and the marker it sets is N309's. Warm-ups stay with
 * their targets cleared; kept rungs keep their role and drop value; the cue lives on the exercise and is
 * cleared by the row write, not here.
 */
data class ReplacementPlan(val sets: List<ReplacementSet>)

/** One performed work set the new plan is seeded from (N308), in the order it was done. */
data class ReplacementSeed(
    val weightGrams: Long,
    val assistanceGrams: Long,
    val reps: Int,
    val durationSeconds: Int?,
)

/** One planned set the replacement writes, already resolved to the values the new row stores. */
data class ReplacementSet(
    val role: SetType,
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    val repsMin: Int?,
    val repsMax: Int?,
    val repsCurrent: Int?,
    val dropValueGrams: Long?,
    val targetDurationSeconds: Int?,
    val note: String?,
)

/** What one replace did, enough for the plan screen's whole-edit undo (N308, decision 4a). */
data class ReplaceOutcome(
    val oldTemplateExerciseId: String,
    val newTemplateExerciseId: String,
    val newSetIds: List<String>,
)

/**
 * The replacement plan for [oldSets]: the kept warm-ups (cleared), the kept work sets (seeded or
 * [empty]), and the rungs whose anchor survived.
 */
fun replacementOf(
    oldSets: List<TemplateSet>,
    seeds: List<ReplacementSeed?>,
    empty: Boolean,
): ReplacementPlan {
    val ordered = oldSets.sortedBy { it.setIndex }
    val work = ordered.filter { it.role.recordsEffort }
    val keptIds = work.take(keepWorkingSets(work.size)).map { it.id }.toSet()
    val keptWork = work.filter { it.id in keptIds }
    val place = keptWork.withIndex().associate { it.value.id to it.index }
    var anchorKept = false
    val sets = buildList {
        for (set in ordered) {
            when {
                set.role == SetType.WARMUP -> add(warmUp(set))
                set.role.recordsEffort -> {
                    anchorKept = set.id in keptIds
                    if (anchorKept) add(workSet(set, seeds.getOrNull(place.getValue(set.id)), empty))
                }
                set.role.isRung -> if (anchorKept) add(rung(set))
            }
        }
    }
    return ReplacementPlan(sets)
}

private fun warmUp(set: TemplateSet) = ReplacementSet(
    role = SetType.WARMUP,
    weightGrams = null,
    assistanceGrams = null,
    repsMin = null,
    repsMax = null,
    repsCurrent = null,
    dropValueGrams = null,
    targetDurationSeconds = null,
    note = set.note,
)

private fun workSet(set: TemplateSet, seed: ReplacementSeed?, empty: Boolean): ReplacementSet = when {
    // The plan's reps and the effort it builds to stay; the number to find does not.
    empty -> ReplacementSet(
        role = set.role,
        weightGrams = null,
        assistanceGrams = null,
        repsMin = set.targetRepsMin,
        repsMax = set.targetRepsMax,
        repsCurrent = set.targetRepsCurrent,
        dropValueGrams = null,
        targetDurationSeconds = set.targetDurationSeconds,
        note = set.note,
    )
    // No counterpart in the new movement's history: the target is cleared, not guessed from the old.
    seed == null -> ReplacementSet(
        role = set.role,
        weightGrams = null,
        assistanceGrams = null,
        repsMin = null,
        repsMax = null,
        repsCurrent = null,
        dropValueGrams = null,
        targetDurationSeconds = null,
        note = set.note,
    )
    else -> {
        val held = seed.durationSeconds != null
        ReplacementSet(
            role = set.role,
            weightGrams = seed.weightGrams,
            assistanceGrams = seed.assistanceGrams,
            repsMin = if (held) null else seed.reps,
            repsMax = if (held) null else seed.reps,
            repsCurrent = null,
            dropValueGrams = null,
            targetDurationSeconds = if (held) seed.durationSeconds else null,
            note = set.note,
        )
    }
}

private fun rung(set: TemplateSet) = ReplacementSet(
    role = set.role,
    weightGrams = null,
    assistanceGrams = null,
    repsMin = null,
    repsMax = null,
    repsCurrent = null,
    dropValueGrams = set.dropValueGrams,
    targetDurationSeconds = null,
    note = set.note,
)
