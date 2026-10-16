package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R

/**
 * A skippable note: a title, an optional line of explanation, a text box, and a
 * secondary button that reads *Skip* on a prompt and *Cancel* on an edit.
 *
 * Extracted at its second caller (ROADMAP F8): the readiness note (N4) and the
 * workout comment (N11) are the same dialog with different words, and the second one
 * is what the rule waits for. The strings and the test tags come from the caller, so
 * each keeps its own vocabulary and its own tags.
 *
 * [content] is an optional block rendered **below** the text box (ROADMAP N62): the readiness
 * dialog's sore-muscle list is its first caller, and it belongs under the free-text line rather than
 * beside it. The shared dialog owns neither that block's state nor its wording, and a caller that
 * passes nothing gets exactly the dialog it had before.
 *
 * The field is `rememberSaveable`, so a rotation mid-sentence does not throw the note
 * away — and both callers treat dismissal as "skip", never as a write.
 */
@Composable
fun NoteDialog(
    title: String,
    body: String?,
    initialNote: String,
    label: String,
    isPrompt: Boolean,
    fieldTag: String,
    saveTag: String,
    dismissTag: String,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    var note by rememberSaveable { mutableStateOf(initialNote) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                body?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().testTag(fieldTag),
                    label = { Text(label) },
                    minLines = 2,
                )
                content?.invoke()
            }
        },
        confirmButton = {
            AppTextButton(
                modifier = Modifier.testTag(saveTag),
                onClick = { onSave(note.trim().ifEmpty { null }) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(modifier = Modifier.testTag(dismissTag), onClick = onDismiss) {
                Text(stringResource(if (isPrompt) R.string.readiness_skip else R.string.action_cancel))
            }
        },
    )
}

/**
 * The session's readiness (ROADMAP N4): the edit reached from the workout header.
 *
 * **The header row is the only way in** since N140 retired the prompt a new workout used to open with, so the
 * dialog's own dismiss is the whole of closing it.
 *
 * **The note is all it is.** The sore-muscle list sat beside it until N113, when soreness moved to the
 * day's own record in the measurements screen: it is a fact about the body's recovery rather than about
 * the workout, and on a session it could only exist on a day that was trained — the rest day, when it is
 * the thing worth writing down, had nowhere to put it. A session that recorded one before the move keeps
 * it, and history still reads it.
 */
@Composable
fun ReadinessNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    NoteDialog(
        title = stringResource(R.string.readiness_edit_title),
        // Nothing prompts this dialog any more (N140), so it never carries the prompt's own body text.
        body = null,
        initialNote = initialNote,
        label = stringResource(R.string.readiness_label),
        isPrompt = false,
        fieldTag = TestTags.READINESS_NOTE,
        saveTag = TestTags.READINESS_SAVE,
        dismissTag = TestTags.READINESS_DISMISS,
        onDismiss = onDismiss,
        onSave = onSave,
        modifier = modifier,
    )
}

/**
 * The workout's own comment, asked for once when it is finished (ROADMAP N11).
 *
 * Skippable on purpose: the moment of finishing is when the reason it went well or
 * badly is remembered, and a required field at that moment would collect nothing but
 * filler. Dismissing finishes the workout without a comment, exactly as *Skip* does.
 */
@Composable
fun WorkoutNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    NoteDialog(
        title = stringResource(R.string.workout_note_title),
        body = stringResource(R.string.workout_note_body),
        initialNote = "",
        label = stringResource(R.string.workout_note_label),
        isPrompt = true,
        fieldTag = TestTags.WORKOUT_NOTE,
        saveTag = TestTags.WORKOUT_NOTE_SAVE,
        dismissTag = TestTags.WORKOUT_NOTE_SKIP,
        onDismiss = onDismiss,
        onSave = onSave,
        modifier = modifier,
    )
}
