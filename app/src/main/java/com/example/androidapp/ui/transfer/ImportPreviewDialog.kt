package com.example.androidapp.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.BackupProblem
import com.example.androidapp.domain.repository.PreviewCounts
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * What a backup would do, asked before it is merged (ROADMAP N246, N248).
 *
 * The import is additive and never overwrites, so the question is not a warning — it is the answer to "what am
 * I about to bring back?". The table splits each kind into what the file holds and what the merge would do:
 * already here, coming back from a delete, or new. A measurement whose day is already recorded counts as here
 * because that is what the merge does with it.
 */
@Composable
internal fun ImportPreviewDialog(
    preview: BackupPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.IMPORT_PREVIEW_DIALOG),
        title = { Text(stringResource(R.string.import_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.import_preview_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PreviewTable(preview = preview)
                if (preview.problems.isNotEmpty()) PreviewProblems(preview.problems)
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                enabled = preview.problems.isEmpty(),
                modifier = Modifier.testTag(TestTags.IMPORT_PREVIEW_CONFIRM),
            ) {
                Text(stringResource(R.string.import_preview_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.IMPORT_PREVIEW_CANCEL),
            ) {
                Text(stringResource(R.string.import_preview_cancel))
            }
        },
    )
}

/** The values the app cannot store, with the first few named and the rest counted (B342). */
@Composable
private fun PreviewProblems(problems: List<BackupProblem>) {
    Column(
        modifier = Modifier.testTag(TestTags.IMPORT_PREVIEW_PROBLEMS),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.import_preview_problems, problems.size, problems.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        problems.take(PROBLEMS_SHOWN).forEach { problem ->
            Text(
                text = stringResource(R.string.import_preview_problem_line, problem.where, problem.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (problems.size > PROBLEMS_SHOWN) {
            Text(
                text = pluralStringResource(
                    R.plurals.import_preview_problem_more,
                    problems.size - PROBLEMS_SHOWN,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Four kinds against four numbers: the file's row, and the merge's three outcomes (N248). */
@Composable
private fun PreviewTable(preview: BackupPreview) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PreviewHeader()
        PreviewLine(
            label = stringResource(R.string.import_preview_sessions_label),
            counts = preview.sessions,
        )
        PreviewLine(
            label = stringResource(R.string.import_preview_sets_label),
            counts = preview.sets,
        )
        PreviewLine(
            label = stringResource(R.string.import_preview_measurements_label),
            counts = preview.measurements,
        )
        PreviewLine(
            label = stringResource(R.string.import_preview_movements_label),
            counts = preview.exercises,
        )
    }
}

@Composable
private fun PreviewHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "", modifier = Modifier.weight(LABEL_WEIGHT))
        HeaderCell(stringResource(R.string.import_preview_file), Modifier.weight(NUMBER_WEIGHT))
        HeaderCell(stringResource(R.string.import_preview_here), Modifier.weight(NUMBER_WEIGHT))
        HeaderCell(stringResource(R.string.import_preview_back), Modifier.weight(NUMBER_WEIGHT))
        HeaderCell(stringResource(R.string.import_preview_new), Modifier.weight(NUMBER_WEIGHT))
    }
}

@Composable
private fun PreviewLine(label: String, counts: PreviewCounts) {
    // One sentence per row (N258): four bare numbers are four numbers a reader cannot attribute to a column,
    // and the row is the unit the lifter is asking about, so the row states itself and its cells stay visual.
    val spoken = stringResource(
        R.string.import_preview_row_spoken,
        label,
        counts.inFile,
        counts.alreadyHere,
        counts.restored,
        counts.added,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(LABEL_WEIGHT),
            style = MaterialTheme.typography.bodySmall,
        )
        NumberCell(counts.inFile, Modifier.weight(NUMBER_WEIGHT))
        NumberCell(counts.alreadyHere, Modifier.weight(NUMBER_WEIGHT))
        NumberCell(counts.restored, Modifier.weight(NUMBER_WEIGHT))
        NumberCell(counts.added, Modifier.weight(NUMBER_WEIGHT))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun NumberCell(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.End,
    )
}

private const val LABEL_WEIGHT = 1.5f
private const val NUMBER_WEIGHT = 0.45f
private const val PROBLEMS_SHOWN = 3
