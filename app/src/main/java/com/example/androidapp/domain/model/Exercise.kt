package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit

/**
 * An entry in the exercise library (ROADMAP P1.1).
 *
 * [id] is a stable, human-readable slug for seeded exercises (for example
 * `barbell-bench-press`) rather than a database row id. That keeps a logged
 * set pointing at the same movement across export/import (P1.12) and a future
 * sync (P4.9), and stops ids shifting when the seed list is reordered.
 * User-created exercises (P3.1) will use a UUID instead.
 */
data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment,
    val movementPattern: MovementPattern,
    val isCustom: Boolean = false,
    /**
     * This exercise's own rest between sets in seconds, or null to use
     * [com.example.androidapp.domain.RestTimer.DEFAULT_SECONDS] (ROADMAP N5).
     *
     * Deliberately separate from the +15 s/−15 s controls, which are one-off
     * adjustments to the rest currently running.
     */
    val restSeconds: Int? = null,
    /** "Chest up, elbows tucked": read while lifting, not a description (ROADMAP N5). */
    val techniqueNote: String? = null,
    /**
     * This exercise's own display unit, or null to follow the app setting (ROADMAP N64).
     *
     * **Presentation only**, like the setting it overrides: the weight is stored in grams either
     * way. A machine that jumps in pounds is why this exists — the lat pulldown reads in lb while
     * everything else reads in kg, and every number that belongs to *this* exercise follows it.
     */
    val weightUnit: WeightUnit? = null,
    /**
     * This exercise's own weight step in whole grams, or null for the unit's own (ROADMAP N77).
     *
     * The ± buttons, the warm-up ramp and the progression offer all move a load by a step, and until
     * this existed that step came from the unit alone — so a machine that jumps 5 kg, or 1 kg, was
     * always edited against a step it did not have. Unlike [weightUnit] this is **not presentation**:
     * it is a fact about the equipment, which is why it is not cleared when the unit changes.
     */
    val stepGrams: Long? = null,
)
