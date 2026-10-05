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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SORE_MUSCLE_GROUPS
import com.example.androidapp.domain.model.SoreMuscle

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
 * The session's readiness (ROADMAP N4, N62): the prompt a new workout opens with, and
 * the edit reached from the workout header.
 *
 * The free-text note is unchanged, and the sore-muscle list sits beside it — the note for what a
 * list cannot say, the list for the fact "quads 8, calves 3" that a sentence cannot be read back
 * one muscle at a time. Both are one save, so the two never disagree about which edit landed.
 *
 * The list is held here rather than inside the editor so the dialog's *Save* can read it: the
 * editor draws and reports, and the dialog decides what a save means.
 */
@Composable
fun ReadinessNoteDialog(
    initialNote: String,
    initialSoreMuscles: List<SoreMuscle>,
    isPrompt: Boolean,
    onDismiss: () -> Unit,
    onSave: (note: String?, soreMuscles: List<SoreMuscle>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // `rememberSaveable` for the same reason the note field is: a rotation mid-edit must not throw
    // away the muscles already picked.
    var soreMuscles by rememberSaveable(stateSaver = SoreMusclesSaver) {
        mutableStateOf(initialSoreMuscles)
    }

    NoteDialog(
        title = stringResource(
            if (isPrompt) R.string.readiness_prompt_title else R.string.readiness_edit_title,
        ),
        body = if (isPrompt) stringResource(R.string.readiness_prompt_body) else null,
        initialNote = initialNote,
        label = stringResource(R.string.readiness_label),
        isPrompt = isPrompt,
        fieldTag = TestTags.READINESS_NOTE,
        saveTag = TestTags.READINESS_SAVE,
        dismissTag = TestTags.READINESS_DISMISS,
        onDismiss = onDismiss,
        onSave = { note -> onSave(note, soreMuscles) },
        content = {
            // The shared scored-picks editor (N63): the same interaction the per-exercise joint list
            // uses, with the taxonomy's vocabulary passed in.
            ScoredPicksEditor(
                picked = soreMuscles.map { ScoredPick(it.muscle.name, it.muscle.label, it.score) },
                options = SORE_MUSCLE_GROUPS.map {
                    ScoredPick(it.name, it.label, DEFAULT_SCORED_PICK)
                },
                tags = ScoredPickTags(READINESS_SORE_TAG_PREFIX),
                strings = ScoredPickStrings(
                    title = stringResource(R.string.readiness_sore_title),
                    add = stringResource(R.string.readiness_sore_add),
                    decrease = { stringResource(R.string.readiness_sore_decrease, it) },
                    increase = { stringResource(R.string.readiness_sore_increase, it) },
                    remove = { stringResource(R.string.readiness_sore_remove, it) },
                ),
                onAdd = { key ->
                    val muscle = MuscleGroup.valueOf(key)
                    soreMuscles = soreMuscles + SoreMuscle(muscle, DEFAULT_SCORED_PICK)
                },
                onScore = { key, score ->
                    soreMuscles = soreMuscles.map {
                        if (it.muscle.name == key) it.copy(score = score) else it
                    }
                },
                onRemove = { key ->
                    soreMuscles = soreMuscles.filterNot { it.muscle.name == key }
                },
            )
        },
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

/**
 * Saves the picked list as `MUSCLE:score` strings — the shape saved instance state can hold.
 *
 * A name that no longer resolves and a score that no longer parses are both dropped rather than
 * guessed at: this is a restoration, and inventing a muscle would write something nobody picked.
 */
private val SoreMusclesSaver = listSaver<List<SoreMuscle>, String>(
    save = { muscles -> muscles.map { "${it.muscle.name}:${it.score}" } },
    restore = { saved ->
        saved.mapNotNull { entry ->
            val muscle = MuscleGroup.entries.firstOrNull { it.name == entry.substringBefore(":") }
            val score = entry.substringAfter(":").toIntOrNull()
            if (muscle == null || score == null) null else SoreMuscle(muscle, score)
        }
    },
)
