package com.example.androidapp.ui.programs

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The question a refused load asks instead of failing (ROADMAP N243).
 *
 * A document whose program id is already here with a different plan cannot be added — the merge is insert-only
 * and never edits a program (B180) — so the lifter is asked for a name of its own, which makes the load a
 * **copy**: the plan arrives whole, with ids nothing can collide with, and the program already on the device is
 * untouched. The field opens on a name the list does not hold, because "Push Pull Legs (2)" is the answer
 * nearly every time and typing it should not be the price of the refusal.
 *
 * **A taken name is refused here rather than after the write.** Two programs a lifter cannot tell apart are
 * worse than the refusal this dialog exists to get past, and the repository checks the same thing at the write
 * boundary, so a name taken between the question and the answer is still a refusal.
 */
@Composable
internal fun ProgramRenameDialog(
    rename: ProgramDocumentPreview.NameTaken,
    /** Every name the list holds, which the field's own check is made against. */
    takenNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saveable, so rotating the device mid-type does not throw away the name the lifter is choosing.
    var name by rememberSaveable { mutableStateOf(rename.suggestion) }
    val trimmed = name.trim()
    val taken = takenNames.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        modifier = modifier.testTag(TestTags.Programs.RENAME_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.program_rename_text, rename.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.Programs.RENAME_NAME),
                    singleLine = true,
                    isError = taken,
                    label = { Text(stringResource(R.string.program_rename_label)) },
                )
                if (taken) {
                    Text(
                        text = stringResource(R.string.program_rename_taken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && !taken,
                modifier = Modifier.testTag(TestTags.Programs.RENAME_CONFIRM),
            ) {
                Text(stringResource(R.string.program_rename_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Programs.RENAME_CANCEL),
            ) {
                Text(stringResource(R.string.program_rename_cancel))
            }
        },
    )
}
