package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.model.SetEntry
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
