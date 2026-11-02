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
 * The merge itself is one-way, so the screen says what moved rather than silently popping back: the counts are
 * the picker's reads (B333) and the survivor's name is where they went. **A zero is never printed** (N262):
 * "Moved 0 sessions" reads as nothing happened, and it cannot tell a movement with no history at all from one
 * whose sessions were every one deleted or still open — so the sentence names the case instead.
 */
@Composable
internal fun MergeConfirmedDialog(
    summary: MergeSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions = if (summary.sessions > 0) {
        pluralStringResource(R.plurals.merge_confirmed_sessions, summary.sessions, summary.sessions)
    } else {
        null
    }
    val plans = if (summary.plans > 0) {
        pluralStringResource(R.plurals.merge_confirmed_plans, summary.plans, summary.plans)
    } else {
        null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.MERGE_CONFIRMED),
        title = { Text(stringResource(R.string.merge_confirmed_title, summary.survivorName)) },
        text = {
            Text(
                when {
                    sessions != null && plans != null ->
                        stringResource(R.string.merge_confirmed_text, sessions, plans)

                    sessions != null -> stringResource(R.string.merge_confirmed_sessions_only, sessions)
                    plans != null -> stringResource(R.string.merge_confirmed_plans_only, plans)
                    else -> stringResource(R.string.merge_confirmed_nothing)
                },
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
