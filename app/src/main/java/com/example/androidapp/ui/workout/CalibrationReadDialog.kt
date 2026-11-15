package com.example.androidapp.ui.workout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The finish could not read the marked plans (B452).
 *
 * The calibration question is one-shot and advisory, so a failed read must not strand a stored workout —
 * but it must not disappear either: the next finish asks again with *that* session's numbers, and this
 * session's answer was never offered. One acknowledgement, then the finish carries on (stall or close).
 */
@Composable
internal fun CalibrationReadDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag(TestTags.CALIBRATION_READ_ERROR),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calibration_unread_title)) },
        text = { Text(stringResource(R.string.calibration_unread_body)) },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.CALIBRATION_READ_OK),
            ) {
                Text(stringResource(R.string.calibration_unread_ok))
            }
        },
    )
}
