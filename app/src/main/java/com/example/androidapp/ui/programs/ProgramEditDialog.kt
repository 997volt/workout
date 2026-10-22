package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.shared.ChangeKind
import com.example.androidapp.shared.WarningCode
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The confirmation an edit document stops at (ROADMAP N171).
 *
 * It shows the diff **the shared planner produced**, not a paragraph written separately: the same
 * changes the app will write, so the dialog cannot promise one thing and the transaction do another.
 * The warnings are the consequences worth stating — a movement the device does not have, a workout
 * that stays in the library when its day leaves, and another program that also uses a workout this
 * edit changes.
 *
 * The button says *Update program* rather than *Load* because this is the one program path that
 * changes rows the device already holds.
 */
@Composable
internal fun ProgramEditDialog(
    edit: ProgramDocumentPreview.Edit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.Programs.EDIT_DIALOG),
        title = { Text(stringResource(R.string.program_edit_dialog_title, edit.programName)) },
        text = { EditSummary(edit) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Programs.EDIT_CONFIRM),
            ) {
                Text(stringResource(R.string.program_edit_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Programs.EDIT_CANCEL),
            ) {
                Text(stringResource(R.string.program_edit_cancel))
            }
        },
    )
}

/** The diff, as four counts and the warnings the planner named (N171). */
@Composable
private fun EditSummary(edit: ProgramDocumentPreview.Edit) {
    val days = edit.changes.count { it.kind in DAY_CHANGES }
    val workouts = edit.changes.count { it.kind in WORKOUT_CHANGES }
    val exercises = edit.changes.count { it.kind in EXERCISE_CHANGES }
    val sets = edit.changes.count { it.kind in SET_CHANGES }
    // The new name is in the change itself, so the line says what it becomes rather than that it will
    // change (B192) — the title already carries the name the list shows.
    val renamedTo = edit.changes
        .firstOrNull { it.kind == ChangeKind.PROGRAM_RENAMED }
        ?.name
    val newMovements = edit.warnings
        .filter { it.code == WarningCode.NEW_MOVEMENT }
        .mapNotNull { it.name ?: it.id }
    val keptWorkouts = edit.warnings
        .filter { it.code == WarningCode.WORKOUT_KEPT }
        .mapNotNull { it.name ?: it.id }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.program_edit_intro))
        if (renamedTo != null) Text(stringResource(R.string.program_edit_renamed, renamedTo))
        if (days + workouts + exercises + sets > 0) {
            Text(stringResource(R.string.program_edit_rows, days, workouts, exercises, sets))
        }
        if (newMovements.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.program_edit_new_movements,
                    newMovements.joinToString(", "),
                ),
            )
        }
        if (keptWorkouts.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.program_edit_kept_workouts,
                    keptWorkouts.joinToString(", "),
                ),
            )
        }
        if (edit.sharedProgramNames.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.program_edit_shared,
                    edit.sharedProgramNames.joinToString(", "),
                ),
            )
        }
    }
}

private val DAY_CHANGES = setOf(
    ChangeKind.SLOT_ADDED,
    ChangeKind.SLOT_UPDATED,
    ChangeKind.SLOT_REMOVED,
)

private val WORKOUT_CHANGES = setOf(
    ChangeKind.TEMPLATE_ADDED,
    ChangeKind.TEMPLATE_UPDATED,
)

private val EXERCISE_CHANGES = setOf(
    ChangeKind.TEMPLATE_EXERCISE_ADDED,
    ChangeKind.TEMPLATE_EXERCISE_UPDATED,
    ChangeKind.TEMPLATE_EXERCISE_REMOVED,
)

private val SET_CHANGES = setOf(
    ChangeKind.SET_ADDED,
    ChangeKind.SET_UPDATED,
    ChangeKind.SET_REMOVED,
)
