package com.example.androidapp.domain.model

/**
 * The three targets one applied progression step can move (ROADMAP N299).
 *
 * Held as the full triple rather than only the field the step named, because a load step also
 * resets the range's place: undoing the load alone would leave the reps at the floor the step
 * moved them to.
 */
data class ProgressionTargets(
    val weightGrams: Long? = null,
    val repsCurrent: Int? = null,
    val durationSeconds: Int? = null,
)
