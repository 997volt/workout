package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One progression step the app applied without being asked (ROADMAP N299).
 *
 * The row keeps the **whole target before and after**, not only the field the step named: a load
 * step also resets the range's current rep, and an undo that restored the load alone would leave
 * the reps at the floor the step moved them to. The direction is the sentence's label; null is a
 * name a later build wrote, which the undo does not need.
 *
 * No foreign keys: the row is a log of what the app did. The set, the session or the program may
 * later be deleted, and the log still says the app moved a plan — deleting it with a parent would
 * make the record deny a write that happened.
 */
@Entity(
    tableName = "progression_writes",
    indices = [
        Index("programId"),
        Index("templateSetId"),
        Index("sessionExerciseId"),
        Index("appliedAt"),
    ],
)
data class ProgressionWriteEntity(
    @PrimaryKey val id: String,
    /** The auto program that drove the write, or null when it has since been deleted. */
    val programId: String?,
    val templateSetId: String,
    val sessionExerciseId: String,
    /** The field the step named, by name, or null for a name this build does not know. */
    val direction: String?,
    val beforeWeightGrams: Long?,
    val beforeRepsCurrent: Int?,
    val beforeDurationSeconds: Int?,
    val afterWeightGrams: Long?,
    val afterRepsCurrent: Int?,
    val afterDurationSeconds: Int?,
    val appliedAt: Long,
    /** When the step was taken back, or null while it still stands. */
    val revertedAt: Long?,
)

/**
 * The target one planned set held when auto progression was switched on (ROADMAP N299).
 *
 * Keyed by **program and set**, because the baseline belongs to the program that was switched on,
 * not to the plan (a template can be shared). The three targets are the ones a step can move; the
 * range's ends and the set's role are the authored plan and reset is not allowed to rewrite them.
 */
@Entity(
    tableName = "progression_baselines",
    primaryKeys = ["programId", "templateSetId"],
    indices = [Index("programId")],
)
data class ProgressionBaselineEntity(
    val programId: String,
    val templateSetId: String,
    val weightGrams: Long?,
    val repsCurrent: Int?,
    val durationSeconds: Int?,
    val capturedAt: Long,
)
