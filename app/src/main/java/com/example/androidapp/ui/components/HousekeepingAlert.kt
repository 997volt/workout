package com.example.androidapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.data.Housekeeping

/**
 * The one-time notice a failed housekeeping pass raises (ROADMAP B324).
 *
 * A caught repair failure has to be **visible**: the alternative — a silent skip — leaves duplicate or
 * unfiled rows the lifter would never know about, the same class of quiet divergence the seed's own guards
 * exist to avoid. So the app says it once, on every screen, with a retry that is safe because the pass writes
 * in one transaction.
 */
@Composable
internal fun HousekeepingAlert(
    failure: Housekeeping.Failure?,
    retrying: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (failure == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.HOUSEKEEPING_ALERT),
        title = { Text(stringResource(R.string.housekeeping_failed_title)) },
        text = { Text(stringResource(R.string.housekeeping_failed_text)) },
        confirmButton = {
            AppTextButton(
                onClick = onRetry,
                enabled = !retrying,
                modifier = Modifier.testTag(TestTags.HOUSEKEEPING_RETRY),
            ) {
                Text(
                    stringResource(
                        if (retrying) R.string.housekeeping_retrying else R.string.housekeeping_retry,
                    ),
                )
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.HOUSEKEEPING_LATER),
            ) {
                Text(stringResource(R.string.housekeeping_later))
            }
        },
    )
}
