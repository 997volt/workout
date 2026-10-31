package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The question a manual merge asks: which movement does this one become (ROADMAP N245).
 *
 * The automatic repair refuses to delete a row the lifter configured (B298), so this is the deliberate
 * version: the source's history moves to the chosen survivor and the source row goes. The sentence says what
 * the choice costs **before** the confirm, because there is no undo and the row's history is what makes it
 * worth asking.
 *
 * Search is inside the dialog rather than a second screen: a library is a hundred-odd names, and the answer is
 * usually one of the first few matches.
 */
@Composable
internal fun MergeMovementDialog(
    sourceName: String,
    candidates: List<Exercise>,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val needle = query.trim()
    val filtered = remember(candidates, needle) {
        candidates.filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.MERGE_DIALOG),
        title = { Text(stringResource(R.string.exercise_merge_title, sourceName)) },
        text = {
            MergeDialogBody(
                query = query,
                onQueryChange = { query = it },
                candidates = filtered,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                error = error,
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { selectedId?.let(onConfirm) },
                enabled = selectedId != null,
                modifier = Modifier.testTag(TestTags.MERGE_CONFIRM),
            ) {
                Text(stringResource(R.string.exercise_merge_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.MERGE_CANCEL),
            ) {
                Text(stringResource(R.string.exercise_merge_cancel))
            }
        },
    )
}

/** The dialog's content: what the merge costs, the search, the candidates and a failure (N245). */
@Composable
private fun MergeDialogBody(
    query: String,
    onQueryChange: (String) -> Unit,
    candidates: List<Exercise>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.exercise_merge_text),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.MERGE_SEARCH),
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_merge_search)) },
        )
        if (candidates.isEmpty()) {
            Text(
                text = stringResource(R.string.exercise_merge_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            MergeCandidateList(
                candidates = candidates,
                selectedId = selectedId,
                onSelect = onSelect,
            )
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** The searchable list of survivors: a row per movement, with the chosen one checked (N245). */
@Composable
private fun MergeCandidateList(
    candidates: List<Exercise>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
        items(candidates, key = { it.id }) { candidate ->
            AppRow(
                headline = candidate.name,
                onClick = { onSelect(candidate.id) },
                onClickLabel = stringResource(R.string.exercise_merge_into, candidate.name),
                trailing = if (candidate.id == selectedId) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
                testTag = TestTags.mergeCandidate(candidate.id),
            )
        }
    }
}
