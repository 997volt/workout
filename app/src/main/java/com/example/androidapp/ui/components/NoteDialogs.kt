package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SORE_MUSCLE_GROUPS
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TenPointScale

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
            SorenessEditor(
                picked = soreMuscles,
                onAdd = { muscle ->
                    soreMuscles = soreMuscles + SoreMuscle(muscle, DEFAULT_SORE_SCORE)
                },
                onScore = { muscle, score ->
                    soreMuscles = soreMuscles.map {
                        if (it.muscle == muscle) it.copy(score = score) else it
                    }
                },
                onRemove = { muscle ->
                    soreMuscles = soreMuscles.filterNot { it.muscle == muscle }
                },
            )
        },
        modifier = modifier,
    )
}

/**
 * The sore-muscle list the readiness dialog edits (ROADMAP N62).
 *
 * A muscle is picked from the taxonomy's named groups, each picked one carries its own score on the
 * shared [TenPointScale], and one can be taken off again. The list is reported rather than stored,
 * so a save replaces the session's list with exactly what is drawn here.
 *
 * A dropdown rather than chips for the reason the role picker gives: eleven options with words on
 * them do not fit a phone's dialog width. The list scrolls within its own box, so a lifter who names
 * every muscle still reaches the buttons that commit the dialog.
 */
@Composable
private fun SorenessEditor(
    picked: List<SoreMuscle>,
    onAdd: (MuscleGroup) -> Unit,
    onScore: (MuscleGroup, Int) -> Unit,
    onRemove: (MuscleGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    // A group already on the list is not offered again: two rows for one muscle would make "quads 8"
    // and "quads 3" both true, and the note has one score per muscle.
    val available = SORE_MUSCLE_GROUPS.filter { group -> picked.none { it.muscle == group } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = SORE_LIST_MAX_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.readiness_sore_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        picked.forEach { sore ->
            SoreMuscleRow(sore = sore, onScore = onScore, onRemove = onRemove)
        }
        Box {
            AppTextButton(
                onClick = { picking = true },
                modifier = Modifier.testTag(TestTags.Readiness.SORE_ADD),
                enabled = available.isNotEmpty(),
            ) {
                Text(stringResource(R.string.readiness_sore_add))
            }
            DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                available.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.label) },
                        onClick = {
                            picking = false
                            onAdd(group)
                        },
                        modifier = Modifier.testTag(TestTags.Readiness.soreOption(group.name)),
                    )
                }
            }
        }
    }
}

/** One picked muscle: its name, its own 1–10 score, and the control that takes it off. */
@Composable
private fun SoreMuscleRow(
    sore: SoreMuscle,
    onScore: (MuscleGroup, Int) -> Unit,
    onRemove: (MuscleGroup) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.Readiness.soreRow(sore.muscle.name)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sore.muscle.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        // A stepper rather than a field: the score can only be one of ten values, and a number box
        // would need the same "is this on the scale" guard the repository already has to keep.
        IconButton(
            onClick = { onScore(sore.muscle, sore.score - 1) },
            enabled = sore.score > TenPointScale.MIN,
            modifier = Modifier.testTag(TestTags.Readiness.soreDecrease(sore.muscle.name)),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(
                    R.string.readiness_sore_decrease,
                    sore.muscle.label,
                ),
            )
        }
        Text(
            text = stringResource(R.string.readiness_sore_score, sore.score),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(TestTags.Readiness.soreScore(sore.muscle.name)),
        )
        IconButton(
            onClick = { onScore(sore.muscle, sore.score + 1) },
            enabled = sore.score < TenPointScale.MAX,
            modifier = Modifier.testTag(TestTags.Readiness.soreIncrease(sore.muscle.name)),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(
                    R.string.readiness_sore_increase,
                    sore.muscle.label,
                ),
            )
        }
        IconButton(
            onClick = { onRemove(sore.muscle) },
            modifier = Modifier.testTag(TestTags.Readiness.soreRemove(sore.muscle.name)),
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(
                    R.string.readiness_sore_remove,
                    sore.muscle.label,
                ),
            )
        }
    }
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

/** The score a freshly added muscle starts on: the middle of the scale, so a step either way is one tap. */
private const val DEFAULT_SORE_SCORE = 5

/** How tall the sore-muscle list may grow before it scrolls, keeping the dialog's buttons on screen. */
private val SORE_LIST_MAX_HEIGHT = 200.dp

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
