package com.example.androidapp.ui.exercises

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.repository.MergeSummary
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * What a completed merge did, before the screen leaves the row that no longer exists (ROADMAP N249).
 *
 * The merge itself is one-way, so the screen says what moved rather than silently popping back: the session
 * and plan counts are the ones the write touched, and the survivor's name is where they went.
 */
@Composable
internal fun MergeConfirmedDialog(
    summary: MergeSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.MERGE_CONFIRMED),
        title = { Text(stringResource(R.string.merge_confirmed_title, summary.survivorName)) },
        text = {
            Text(
                stringResource(
                    R.string.merge_confirmed_text,
                    pluralStringResource(
                        R.plurals.merge_confirmed_sessions,
                        summary.sessions,
                        summary.sessions,
                    ),
                    pluralStringResource(
                        R.plurals.merge_confirmed_plans,
                        summary.plans,
                        summary.plans,
                    ),
                ),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.MERGE_CONFIRMED_OK),
            ) {
                Text(stringResource(R.string.merge_confirmed_done))
            }
        },
    )
}
