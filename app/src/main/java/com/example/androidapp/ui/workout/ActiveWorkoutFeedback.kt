package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.DurationFormat
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.RecoveryHold
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.label
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.BodyweightPromptDialog
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label

// What the workout says back while it runs (ROADMAP N7, N23, N219): the two undo snackbars, the
// bodyweight question and the record banner.
/** The bodyweight question, shown only while a set is held for one (N219, B251). */
@Composable
internal fun BodyweightPrompt(
    prompt: PendingBodyweight?,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (prompt == null) return
    BodyweightPromptDialog(
        exerciseName = prompt.exerciseName,
        onSave = onSave,
        onDismiss = onDismiss,
        staleDays = prompt.staleDays,
    )
}

@Composable
private fun ShowUndoSnackbar(
    pendingUndo: SetEntry?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.set_deleted)
    val undoLabel = stringResource(R.string.set_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}

/**
 * Both undo snackbars, and the rule that keeps them honest (ROADMAP B3).
 *
 * An undo is offered only while the thing it refers to is still in the session. The
 * two share one [SnackbarHostState], so without that rule a deleted set's Undo can
 * outlive its exercise: the row is gone, the button stays, and the write it fires is
 * refused. Filtering here is the dismissal; the ViewModel reports if a tap still
 * races through.
 */
@Composable
internal fun UndoOffers(
    state: ActiveWorkoutUiState,
    hostState: SnackbarHostState,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
    onUndoFinishExercise: () -> Unit,
    onDismissFinishUndo: () -> Unit,
) {
    ShowUndoSnackbar(
        pendingUndo = state.undoableSet,
        hostState = hostState,
        onUndo = onUndoDelete,
        onDismiss = onDismissUndo,
    )
    ShowFinishSnackbar(
        state = state,
        hostState = hostState,
        onUndo = onUndoFinishExercise,
        onDismiss = onDismissFinishUndo,
    )
}

/**
 * Undo for the exercise just marked done (ROADMAP N7).
 *
 * The same argument as the deleted-set snackbar: "Done" is accident protection, so
 * it needs a way back — and the exercise is only dimmed, never removed, so the undo
 * is a plain reopen rather than a re-insert.
 */
@Composable
private fun ShowFinishSnackbar(
    state: ActiveWorkoutUiState,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Null once the exercise is gone, which keys the effect below to dismiss the
    // snackbar rather than leave an Undo for a row that no longer exists (B3).
    val finishedId = state.undoableFinishedExerciseId
    val name = state.exercises.firstOrNull { it.id == finishedId }?.name
    val message = if (name == null) {
        stringResource(R.string.active_workout_exercise_done)
    } else {
        stringResource(R.string.active_workout_exercise_done_named, name)
    }
    val undoLabel = stringResource(R.string.set_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(finishedId) {
        if (finishedId == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}

/**
 * What auto progression changed, with the way back (ROADMAP N299).
 *
 * A banner rather than a snackbar: the same *Done* also raises the finish snackbar, and two snackbars
 * would queue so the second arrived after the first had gone. It stays until dismissed or undone, so
 * a lifter who looked away for a moment can still read what the app did.
 */
@Composable
internal fun AutoProgressionBanner(
    moment: AutoProgressionMoment,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TestTags.AUTO_PROGRESSION),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.auto_progression_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = moment.exerciseName, style = MaterialTheme.typography.titleSmall)
            // One line per set the batch moved, so a multi-set write reads as what changed rather than as a
            // count (N373). The direction and the before → after are the whole of "what and how".
            autoProgressionLines(moment).forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(TestTags.AUTO_PROGRESSION_DISMISS),
                ) {
                    Text(stringResource(R.string.auto_progression_dismiss))
                }
                AppTextButton(
                    onClick = onUndo,
                    modifier = Modifier.testTag(TestTags.AUTO_PROGRESSION_UNDO),
                ) {
                    Text(stringResource(R.string.auto_progression_undo))
                }
            }
        }
    }
}

/** One line per moved set: its place in the plan, then the move itself (ROADMAP N373). */
@Composable
private fun autoProgressionLines(moment: AutoProgressionMoment): List<String> =
    moment.steps.map { step ->
        stringResource(R.string.auto_progression_set_line, step.number, stepMove(step, moment.unit))
    }

/**
 * The move one step made: the direction and its before → after.
 *
 * The direction is the "how" — reps below the range's ceiling, the load at it, a hold's seconds — and the
 * values are the plan's own, not a recomputation. A field the step did not move is not shown.
 */
@Composable
private fun stepMove(step: AutoProgressionStep, unit: WeightUnit): String = when (step.direction) {
    ProgressionDirection.LOAD -> stringResource(
        R.string.auto_progression_load_move,
        weightWithUnit(step.before.weightGrams, unit),
        weightWithUnit(step.after.weightGrams, unit),
    )

    ProgressionDirection.REPS -> {
        val from = step.before.repsCurrent ?: 0
        val to = step.after.repsCurrent ?: 0
        pluralStringResource(R.plurals.auto_progression_reps_move, to, from, to)
    }

    ProgressionDirection.SECONDS -> stringResource(
        R.string.auto_progression_seconds_move,
        step.before.durationSeconds?.let { DurationFormat.ofSeconds(it.toLong()) }.orEmpty(),
        step.after.durationSeconds?.let { DurationFormat.ofSeconds(it.toLong()) }.orEmpty(),
    )
}

/** A planned load in this exercise's unit, or an em dash when the target names none. */
@Composable
private fun weightWithUnit(grams: Long?, unit: WeightUnit): String =
    grams?.let { Weight.display(it, assistanceGrams = 0L, unit = unit) + " " + unit.label() } ?: "—"

/**
 * The reason a *Done* held its step (ROADMAP N377).
 *
 * A hold has no undo — nothing was written — so this is a notice rather than a decision, but it still states
 * the lifter's own fact ("Right knee 6/10") instead of a blank "held": an unexplained non-write is the shape
 * a bug takes, and the auto banner exists for the same reason.
 */
@Composable
internal fun RecoveryHoldBanner(
    moment: RecoveryHoldMoment,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TestTags.RECOVERY_HOLD),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.recovery_hold_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = recoveryHoldMessage(moment), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(TestTags.RECOVERY_HOLD_DISMISS),
                ) {
                    Text(stringResource(R.string.recovery_hold_dismiss))
                }
            }
        }
    }
}

/** The one line the hold states: the lift, the lifter's score, and what was not written. */
@Composable
private fun recoveryHoldMessage(moment: RecoveryHoldMoment): String = when (val hold = moment.hold) {
    is RecoveryHold.Joint -> stringResource(
        R.string.recovery_hold_joint,
        moment.exerciseName,
        hold.pain.label,
        hold.pain.score,
    )

    is RecoveryHold.Sore -> stringResource(
        R.string.recovery_hold_sore,
        moment.exerciseName,
        hold.muscle.label,
        hold.score,
    )
}

/**
 * The record just set (ROADMAP N23).
 *
 * It says what was beaten rather than only what was done — "5 reps at 100 kg, best yet" — and
 * it says the *first* time at a rep count differently, because there was no bar to clear then
 * and claiming otherwise would be a lie the data does not support.
 */
@Composable
internal fun PersonalRecordBanner(
    record: PersonalRecordMoment,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TestTags.PERSONAL_RECORD),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val unit = exerciseWeightUnit(record.weightUnit)
            Text(
                text = stringResource(
                    R.string.personal_record_title,
                    record.reps,
                    Weight.display(record.weightGrams, 0, unit),
                    unit.label(),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = record.previousBestGrams?.let { previous ->
                    stringResource(
                        R.string.personal_record_beats,
                        record.exerciseName,
                        Weight.display(previous, 0, unit),
                        unit.label(),
                    )
                } ?: stringResource(R.string.personal_record_first, record.exerciseName),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
