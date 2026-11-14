package com.example.androidapp.domain.model

import com.example.androidapp.domain.repository.TemplateSetEdit

/** Why the plan is offered a set back (ROADMAP N311). */
enum class VolumeReason {
    /** The plan prescribes N working sets and the last two sessions logged N+1 (decision 7b). */
    EXTRA_SET,

    /** Every working set earned a step through the progression offer for three sessions (decision 1b). */
    COMFORTABLE,
}

/**
 * One planned exercise the app suggests adding a working set to (ROADMAP N311).
 *
 * The suggestion is the plan edit's whole argument: the exercise, the reason, and the set that would be
 * appended — one NORMAL working set after the plan's last one. It writes nothing; the lifter's **Add** is
 * the write, and it is undoable.
 */
data class VolumeSuggestion(
    val templateId: String,
    val templateExerciseId: String,
    val exerciseId: String,
    val exerciseName: String,
    val reason: VolumeReason,
    /** The plan's working-set count before the addition, for the sentence. */
    val plannedSets: Int,
    /** The set [TemplateRepository.addSet] would append, targets already resolved. */
    val edit: TemplateSetEdit,
)
