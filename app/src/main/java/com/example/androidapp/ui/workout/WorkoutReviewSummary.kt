package com.example.androidapp.ui.workout

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.ExerciseActual
import com.example.androidapp.domain.model.ExercisePlan
import com.example.androidapp.domain.model.PerformedSetSpec
import com.example.androidapp.domain.model.PlannedSetSpec
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.comparePlanToActual
import com.example.androidapp.domain.model.loadGrams

/**
 * The review a finished workout gets, assembled from state already in memory (ROADMAP N20).
 *
 * Its own file because the view model is at the function ceiling detekt enforces and because this is a
 * translation rather than an operation: it reads the state it is given and writes nothing back.
 */

/** One performed exercise as the comparison reads it. */
private fun SessionExerciseRow.toActual(): ExerciseActual = ExerciseActual(
    exerciseId = exerciseId,
    name = name,
    // The row already carries its exercise's effective unit (N64).
    weightUnit = weightUnit,
    sets = sets.map {
        PerformedSetSpec(
            role = it.setType,
            weightGrams = it.weightGrams,
            assistanceGrams = it.assistanceGrams,
            reps = it.reps,
            // What the set felt like, so the review can compare the effort with the plan's target
            // (ROADMAP N131). Null stays null: RPE is skippable (N6).
            rpeHalves = it.rpeHalves,
            // A hold states a duration instead of reps (N220), and the comparison says the hold rather
            // than "0 reps" (B243).
            durationSeconds = it.durationSeconds,
        )
    },
)

/** One prescribed exercise as the comparison reads it. */
private fun TemplateExercise.toPlan(appUnit: WeightUnit): ExercisePlan = ExercisePlan(
    exerciseId = exerciseId,
    name = exerciseName,
    weightUnit = weightUnit ?: appUnit,
    sets = sets.map {
        PlannedSetSpec(
            role = it.role,
            weightGrams = it.targetWeightGrams,
            assistanceGrams = it.targetAssistanceGrams,
            minReps = it.targetRepsMin,
            maxReps = it.targetRepsMax,
        )
    },
    // The exercise's one target, read from the plan rather than from a set: N59's number is carried
    // onto every prescribed set, and the review compares against the exercise (N131).
    targetRpeHalves = targetRpeHalves,
)

/**
 * The review, assembled from what is already in memory (ROADMAP N20).
 *
 * A file-level function rather than a member: the class is at the function ceiling detekt
 * enforces, and this only translates the state it is given into a value — it reads nothing
 * from the ViewModel and writes nothing back, so it does not belong to it.
 */
internal fun buildSummary(
    state: ActiveWorkoutUiState,
    plan: List<TemplateExercise>,
    note: String?,
    appUnit: WeightUnit,
): WorkoutReview {
    val actual = state.exercises.map { it.toActual() }
    val planned = plan.map { it.toPlan(appUnit) }
    val allSets = state.exercises.flatMap { it.sets }
    return WorkoutReview(
        note = note?.takeIf { it.isNotBlank() },
        readinessNote = state.readinessNote,
        totalSets = allSets.size,
        // A hold is counted as a hold, not as zero of anything (B243).
        totalHolds = allSets.count { it.durationSeconds != null },
        totalReps = allSets.sumOf { it.reps },
        // The same definition the history and the database use (ROADMAP B10, N219): the load the set was
        // performed at, times its reps. A bodyweight set's load includes the lifter, so a summary that
        // counted the added weight alone ended an all-bodyweight session on zero (B241).
        totalVolumeGrams = allSets.sumOf {
            loadGrams(it.weightGrams, it.bodyweightGrams, it.assistanceGrams) * it.reps
        },
        ratings = state.exercises.mapNotNull { row ->
            val feel = row.muscleFeel
            if (feel == null && row.joints.isEmpty()) {
                null
            } else {
                ExerciseRating(
                    name = row.name,
                    muscleFeel = feel,
                    joints = row.joints,
                )
            }
        },
        comparisons = comparePlanToActual(planned = planned, performed = actual),
    )
}
