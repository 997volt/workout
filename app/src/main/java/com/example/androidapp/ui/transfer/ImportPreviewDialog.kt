package com.example.androidapp.ui.transfer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * What a backup holds, asked before it is merged (ROADMAP N246).
 *
 * The import is additive and never overwrites, so the question is not a warning — it is the answer to "what am
 * I about to bring back?". The counts are the file's rows, not what the merge will change, and the sentence
 * says that a row this device already has is left alone.
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
            Text(
                stringResource(
                    R.string.import_preview_text,
                    pluralStringResource(
                        R.plurals.import_preview_sessions,
                        preview.sessions,
                        preview.sessions,
                    ),
                    pluralStringResource(R.plurals.import_preview_sets, preview.sets, preview.sets),
                    pluralStringResource(
                        R.plurals.import_preview_measurements,
                        preview.measurements,
                        preview.measurements,
                    ),
                    pluralStringResource(
                        R.plurals.import_preview_movements,
                        preview.exercises,
                        preview.exercises,
                    ),
                ),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
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
