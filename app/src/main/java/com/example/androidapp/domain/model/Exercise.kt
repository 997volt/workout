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
    /**
     * The muscle this row trains, or null when it states none (ROADMAP N103).
     *
     * Null means **unstated**, not "other": the value is read from the nearest row above that states one,
     * and there is a separate [MuscleGroup.OTHER] for a lifter who means *other*. A row that states a
     * value owns it — own-if-stated is the one winner rule for all three of these fields.
     */
    val primaryMuscle: MuscleGroup? = null,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    /** The equipment this row is performed with, or null when it states none (ROADMAP N103). */
    val equipment: Equipment? = null,
    /** The movement pattern this row states, or null when it states none (ROADMAP N103). */
    val movementPattern: MovementPattern? = null,
    /**
     * The category this movement is filed under, or null when it is in none (ROADMAP N95, N110).
     *
     * One meaning, not two: the shape is a category over movements and no deeper, so a row's parent is always
     * a category — a movement is nobody's head. Null is a real place to be rather than a missing value: an
     * unfiled movement is drawn at the top level and states its own taxonomy.
     */
    val parentId: String? = null,
    /**
     * Whether this row is a lift or a category head (ROADMAP N95).
     *
     * The field the rest of the app asks before *offering* anything: a [RowKind.CATEGORY] is never shown
     * by a picker and never named by a logged set, while staying a full row of the library — named,
     * transferable, soft-deletable and editable like any other (P1.12).
     */
    val rowKind: RowKind = RowKind.MOVEMENT,
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
    /**
     * Whether this movement is loaded by the lifter's own body (ROADMAP N219).
     *
     * A property of the movement rather than of a set, because that is what it is: a pull-up is a pull-up on
     * every set, and asking on each one would be a question with the same answer every time. What it changes
     * is what the weight field means — the *added* load — and what the set stores, which is the bodyweight in
     * force when it was logged, snapshotted so history cannot rewrite itself.
     */
    val isBodyweight: Boolean = false,
    /**
     * Whether a set of this movement is **held** rather than counted (ROADMAP N220).
     *
     * The same shape as [isBodyweight] and for the same reason: a plank is a plank on every set, so the
     * question is about the movement rather than about one row. What it changes is which number the set
     * records — seconds instead of reps — and the weight field stays, because a carry is loaded *and* timed.
     */
    val isTimed: Boolean = false,
    /**
     * This movement's own answer to *Done*, or null to follow the app setting (ROADMAP N235).
     *
     * Null is *follow the app*, which is what every row was before this existed and what a movement with no
     * opinion still is. It is a member of the movement rather than a device preference because it decides
     * what the app **writes into a plan**: a restore must not quietly return a movement to *Ask*.
     */
    val progressionMode: ProgressionMode? = null,
)
