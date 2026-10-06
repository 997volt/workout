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
import com.example.androidapp.domain.WeightUnit
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
 * RPE and the comment are always shown — except for a warm-up, which records no effort at all and so
 * is not offered the field (N67) — and **neither is invented**: the RPE is a stepper (N59), so
 * it shows the set's own number where it recorded one and reads *Not recorded* where it did not, and
 * a save that does not touch it keeps that absence. Correcting a set's weight must not turn an
 * unrecorded effort into a 9.0 measurement. Save stays disabled while reps or the load is not a
 * usable value; the stepper cannot leave 1–10, so there is no off-scale RPE left to refuse.
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
    /** The unit this set's load is typed and shown in (ROADMAP N64). */
    initialUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    initialStepGrams: Long? = null,
) {
    var draft by remember {
        mutableStateOf(
            SetEntryDraft(
                repsText = initialReps.toString(),
                // Shown as one signed number: -20 is 20 kg of assistance (N15).
                weightText = Weight.display(initialWeightGrams, initialAssistanceGrams, initialUnit),
                // The halves the set recorded, shown as a lifter writes them. A set that recorded
                // none opens blank and keeps none unless the lifter states one (N59): an edit is not
                // where a measurement nobody gave appears.
                rpeText = initialRpe?.let(Rpe::format).orEmpty(),
                noteText = initialNote.orEmpty(),
                setType = initialSetType,
            ),
        )
    }

    val values = draft.values(initialUnit)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_edit_title)) },
        text = {
            SetEditingFields(
                draft = draft,
                onDraftChange = { draft = it },
                unit = initialUnit,
                stepGrams = initialStepGrams,
            )
        },
        confirmButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.SET_SAVE),
                enabled = values.isComplete,
                onClick = { onSave(draft.toEdit(initialUnit)) },
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

/**
 * The fields of a set that exists (N59), split out so the dialog around them reads as a dialog.
 *
 * They answer to [SetFieldTags.Editing] rather than the workout screen's names, because the two are
 * composed at once — the dialog is a window over the screen — and a tag has to name one control.
 */
@Composable
private fun SetEditingFields(
    draft: SetEntryDraft,
    onDraftChange: (SetEntryDraft) -> Unit,
    unit: WeightUnit,
    stepGrams: Long?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SetRoleSelector(role = draft.setType, onSelect = { onDraftChange(draft.copy(setType = it)) })
        SetEntryNumbers(
            draft = draft,
            onDraftChange = onDraftChange,
            unit = unit,
            stepGrams = stepGrams,
            tags = SetFieldTags.Editing,
        )
        // A warm-up records no effort, so the field is not offered for one (ROADMAP N67). The role
        // picker above is what brings it back, and the save drops whatever the draft held.
        if (draft.setType.recordsEffort) {
            SetRpeField(draft = draft, onDraftChange = onDraftChange, tags = SetFieldTags.Editing)
        }
        OutlinedTextField(
            value = draft.noteText,
            onValueChange = { onDraftChange(draft.copy(noteText = it)) },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.SET_NOTE_FIELD),
            label = { Text(stringResource(R.string.set_note_label)) },
            minLines = 2,
        )
    }
}
