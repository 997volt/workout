package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType

/**
 * Edits one set: reps, weight, and the optional RPE and comment (ROADMAP N6).
 *
 * Shared between the live workout screen and the history detail screen (P1.7),
 * because correcting a set you just logged and correcting one from last week are
 * the same edit. It was private to the workout screen until history needed it.
 *
 * **Correcting, not logging** (ROADMAP N59). Logging the next set is the workout screen's own
 * fields now, committed by the *Log set* beside them; this dialog states a set that already exists,
 * which is why nothing opens it from the log path any more.
 *
 * The set's **role** is here too (ROADMAP N14): `SetType` has carried warm-up, drop
 * and failure since the beginning with no way to reach them, and a plan that can say
 * "top set" while a logged set cannot is two vocabularies for one idea.
 *
 * RPE and the comment are always shown but may be left empty — an RPE is skippable
 * by design (N6), so an empty field is a normal value rather than an error. Save
 * stays disabled while a field is not a usable value; in particular a typed RPE
 * outside 1–10 is refused rather than clamped, because a silent 11 → 10 would be
 * a lie about the set.
 */
@Composable
fun SetEditorDialog(
    initialReps: Int,
    initialWeightGrams: Long,
    onDismiss: () -> Unit,
    onSave: (SetEdit) -> Unit,
    modifier: Modifier = Modifier,
    initialRpe: Int? = null,
    initialNote: String? = null,
    initialSetType: SetType = SetType.NORMAL,
    initialAssistanceGrams: Long = 0,
) {
    var draft by remember {
        mutableStateOf(
            SetEntryDraft(
                repsText = initialReps.toString(),
                // Shown as one signed number: -20 is 20 kg of assistance (N15).
                weightText = Weight.display(initialWeightGrams, initialAssistanceGrams),
                // Shown as a lifter writes it, so an existing 9.5 comes back as 9.5.
                rpeText = initialRpe?.let(Rpe::format).orEmpty(),
                noteText = initialNote.orEmpty(),
                setType = initialSetType,
            ),
        )
    }

    val values = draft.values()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SetRoleSelector(
                    role = draft.setType,
                    onSelect = { draft = draft.copy(setType = it) },
                )
                SetEntryNumbers(draft = draft, onDraftChange = { draft = it })
                SetRpeField(
                    draft = draft,
                    onDraftChange = { draft = it },
                    rpeIsValid = values.rpeValid,
                )
                OutlinedTextField(
                    value = draft.noteText,
                    onValueChange = { draft = draft.copy(noteText = it) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.SET_NOTE_FIELD),
                    label = { Text(stringResource(R.string.set_note_label)) },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.SET_SAVE),
                enabled = values.isComplete,
                onClick = { onSave(draft.toEdit()) },
            ) {
                Text(stringResource(R.string.set_save))
            }
        },
        dismissButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.SET_CANCEL),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.set_cancel))
            }
        },
    )
}
