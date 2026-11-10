package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgressionChoice
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionTargets
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.domain.repository.ProgressionSetWrite
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit

// What the next set is shown with and what a plan step writes (ROADMAP N285): the pure prefill and
// progression rules the ViewModel calls into, kept apart from the session writes that invoke them.

/**
 * The plan's sets for one exercise, reduced for the progression rule (ROADMAP N50, N73).
 *
 * The template's own sets, whole. N73 removed the slot's prescription, which used to be merged in
 * here set by set, so the plan the rule reads is the same plan the screen shows with nothing to
 * reconcile.
 *
 * The **target RPE is one number for the exercise** (N59), so every set carries it rather than its
 * own, which is what lets the rule check every working set against that single target.
 */
internal fun PlanContext.progressionSets(): List<ProgressionPlanSet> = plannedEntry?.progressionSets().orEmpty()

/** One logged set as the rule reads it: the role decides whether it is work at all (N50). */
internal fun SetRow.toProgressionPerformance(): ProgressionPerformance = ProgressionPerformance(
    reps = reps,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    rpeHalves = rpeHalves,
    role = setType,
    durationSeconds = durationSeconds,
)

/**
 * Writes every step the lifter picked, and reports the first failure (ROADMAP N74).
 *
 * File-level rather than a member because the class is at the function ceiling detekt enforces — the
 * shape [planEntriesFor] already uses — and because this is a sequence over the repository rather than
 * a question about the session. Each accepted set is handed to [onWritten] as it lands, so a retry
 * after a failure writes only what is left.
 */
internal suspend fun writeProgressionSteps(
    chosen: List<ProgressionChoice>,
    repository: TemplateRepository,
    onWritten: (ProgressionPlanSet) -> Unit,
): DataError? {
    for (choice in chosen) {
        val accepted = choice.offer.accepted(choice.direction) ?: continue
        val written = repository.updateSet(accepted.setId, accepted.toTemplateEdit())
        if (written is DataResult.Failure) return written.error
        onWritten(accepted)
    }
    return null
}

/**
 * Writes every step auto progression took, and records each one as it lands (ROADMAP N299).
 *
 * The manual path above writes only the plan. This one writes the plan **and** its log row through
 * [TemplateRepository.applyProgression], so the two are one transaction; [onWritten] receives the id
 * the undo will address, alongside what moved, so the workout can say what happened without reading
 * the row back.
 */
internal suspend fun writeAutoProgressionSteps(
    chosen: List<ProgressionChoice>,
    sessionExerciseId: String,
    programId: String?,
    repository: TemplateRepository,
    onWritten: (AutoProgressionStep) -> Unit,
): DataError? {
    for (choice in chosen) {
        val accepted = choice.offer.accepted(choice.direction) ?: continue
        val write = ProgressionSetWrite(
            programId = programId,
            sessionExerciseId = sessionExerciseId,
            direction = choice.direction,
            before = choice.offer.set.targets(),
            after = accepted.targets(),
        )
        when (val written = repository.applyProgression(accepted.setId, accepted.toTemplateEdit(), write)) {
            is DataResult.Failure -> return written.error
            is DataResult.Success -> onWritten(
                AutoProgressionStep(
                    writeId = written.data,
                    direction = choice.direction,
                    before = write.before,
                    after = write.after,
                ),
            )
        }
    }
    return null
}

/** The three targets a step can move. */
private fun ProgressionPlanSet.targets(): ProgressionTargets = ProgressionTargets(
    weightGrams = targetWeightGrams,
    repsCurrent = targetRepsCurrent,
    durationSeconds = targetDurationSeconds,
)

/**
 * The accepted set as the template write takes it (N14).
 *
 * The whole target travels, not only the field the step moved: `updateSet` overwrites a planned set,
 * so a partial edit would clear every target the step did not name.
 */
private fun ProgressionPlanSet.toTemplateEdit(): TemplateSetEdit = TemplateSetEdit(
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    // The lifter's place in the range travels with the write (N74), or the step would be lost and
    // the next session would ask for the floor again.
    targetRepsCurrent = targetRepsCurrent,
    // A hold's prescription travels with it too (N242): `updateSet` overwrites the set, so a seconds
    // step that carried no duration would write null over the hold the step was taken on.
    targetDurationSeconds = targetDurationSeconds,
    note = note,
)

/**
 * What the next set is shown with: the plan's target where it speaks, then history (ROADMAP N14, N59,
 * P3.8). The plan's RPE rides in the value rather than beside it: the stepper opens on it, the lifter
 * reads it and changes it when the set felt different, and what is recorded is still that set's own
 * number — with no plan the field opens on [DEFAULT_RPE_HALVES] instead of staying blank.
 */
internal fun SessionExercise.suggestionFor(
    loggedSets: List<SetRow>,
    previous: PreviousPerformance?,
    plan: PlanContext,
): SetSuggestion {
    val planned = plannedTargetFor(
        planned = plan.plannedEntry,
        nextIndex = loggedSets.size,
        anchorLoad = loggedSets.anchorLoadForTheNextRung(),
    )
    // A rung's reps come from what this same set did last time, matched by **place** rather than by the
    // stored index (B157): a soft delete leaves a hole in the indices, while the list's own order is the
    // position the plan counts.
    val sameSetLastTime = previous?.sets?.getOrNull(loggedSets.size)
    val suggestion = suggestionForNextSet(
        loggedSets = loggedSets,
        previous = previous,
        planned = planned,
        sameSetLastTime = sameSetLastTime,
        // What the weight field means here: on a bodyweight movement it is the added part, and a hold
        // needs no load at all unless the plan or the history names one (B242).
        bodyweight = isBodyweight,
        timed = isTimed,
    )
    // The line names the set the prefill read, which for a rung is its own place last time (N168).
    return suggestion.copy(reason = suggestionReason(previous, planned, sameSetLastTime))
}

/**
 * What the set the next rung hangs off actually loaded, or null (ROADMAP N79).
 *
 * Read from the back of the session's own log and skipping anything that does not stand on its own,
 * because a run hangs off such a set: for the second drop of a run the set above it is the first drop,
 * and the anchor is still the working set before them — and a **warm-up is not an anchor either**, the
 * rule `runAt` holds, so a drop logged under one derives nothing here rather than taking the ramp's
 * load (B63). It is what a drop is taken off: the bar in front of you, not the number the plan wrote
 * down.
 */
private fun List<SetRow>.anchorLoadForTheNextRung(): Load? =
    lastOrNull { it.setType.recordsEffort }?.let { Load(it.weightGrams, it.assistanceGrams) }
