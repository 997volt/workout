package com.example.androidapp.ui.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.ProgressionDirection

/**
 * *Step offered, not taken*, naming the directions the offer named, or null when nothing was declined.
 *
 * Stated rather than judged (ROADMAP N236): declining is a decision, and nothing about it says the work was
 * short. The finish's review that used to say this sentence too is gone (N305); history is where the record
 * is read.
 */
@Composable
internal fun declinedStepText(directions: Set<ProgressionDirection>): String? {
    if (directions.isEmpty()) return null
    val named = directions.map { stringResource(it.label()) }
    return stringResource(R.string.step_declined, named.joinToString(", "))
}

/** What one direction is called in a sentence (ROADMAP N236). */
@StringRes
private fun ProgressionDirection.label(): Int = when (this) {
    ProgressionDirection.LOAD -> R.string.progression_direction_load
    ProgressionDirection.REPS -> R.string.progression_direction_reps
    ProgressionDirection.SECONDS -> R.string.progression_direction_seconds
}
