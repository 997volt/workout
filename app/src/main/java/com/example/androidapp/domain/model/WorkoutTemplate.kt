package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit

/**
 * A named, reusable workout (ROADMAP N3): a name and an ordered list of exercises,
 * started in one tap.
 *
 * [exerciseCount] is carried on the list rather than derived from [TemplateExercise]s
 * so the list screen can show "5 exercises" without loading the join table.
 *
 * It carries **no weekday** (ROADMAP N56). The N16 pin lived here and was the weaker of two places
 * answering "what am I doing on Tuesday": a template has no order, no next-up and no adherence to
 * belong to. The day belongs to a program's slot, and the column went with the field in v25.
 */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exerciseCount: Int = 0,
)

/**
 * One exercise in a template, at an explicit [position].
 *
 * Carries the library attributes because that is exactly what the editor displays,
 * and fetching them in the same query avoids an N+1 walk over the library.
 */
data class TemplateExercise(
    val id: String,
    val templateId: String,
    val exerciseId: String,
    val position: Int,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment,
    /** A rest this exercise prescribes, or null to use the library's (N14). */
    val restSeconds: Int? = null,
    /** A cue this exercise prescribes, or null to use the library's (N14). */
    val techniqueNote: String? = null,
    /**
     * The effort this exercise's plan builds to, in half-points, or null (N59, amended).
     *
     * One number per exercise rather than one per set: the plan states a single effort to reach, and
     * a legacy per-set [TemplateSet.targetRpeHalves] is only a fallback for a plan imported from a
     * backup written before the change.
     */
    val targetRpeHalves: Int? = null,
    /**
     * The superset or circuit this exercise is planned in, or null (ROADMAP N24, B16).
     *
     * The same ordinal a session carries, so starting a workout from this plan groups the
     * exercises without the user pairing them again.
     */
    val supersetGroup: Int? = null,
    /**
     * The library exercise's own display unit, or null to follow the app setting (ROADMAP N64).
     *
     * Read from the library rather than copied onto the plan, like the name: a display preference
     * is not something a plan prescribes, and changing it should reach a plan already on screen.
     */
    val weightUnit: WeightUnit? = null,
    /** The planned sets, in order (ROADMAP N14). Empty for a template with none. */
    val sets: List<TemplateSet> = emptyList(),
)

/**
 * One planned set on a template exercise (ROADMAP N14).
 *
 * A target, not a record: the set the user logs is a separate row and is expected to
 * differ, and nothing verifies the plan. Every target is nullable, because "work up
 * to a heavy single" has no weight to write down and a zero would be a claim the app
 * cannot check.
 */
data class TemplateSet(
    val id: String,
    val templateExerciseId: String,
    val setIndex: Int,
    val role: SetType = SetType.NORMAL,
    val targetWeightGrams: Long? = null,
    /** The assistance the plan prescribes, or null (ROADMAP N15). */
    val targetAssistanceGrams: Long? = null,
    /** The target reps: both ends nullable, since a plan may write only an upper bound. */
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetRpeHalves: Int? = null,
    val note: String? = null,
)
