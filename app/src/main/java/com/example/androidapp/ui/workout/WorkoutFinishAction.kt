package com.example.androidapp.ui.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * Finish, or the sentence that says why it is not available yet (B368).
 *
 * A disabled primary action whose reason exists only in the code is silent to the lifter. The action is
 * absent until it can act, and the same slot carries the reason, announced as a polite live region.
 */
@Composable
internal fun FinishAction(
    canFinish: Boolean,
    onClick: () -> Unit,
) {
    if (canFinish) {
        AppTextButton(
            onClick = onClick,
            modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_FINISH),
        ) {
            Text(stringResource(R.string.active_workout_finish))
        }
    } else {
        Text(
            text = stringResource(R.string.active_workout_finish_reason),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .testTag(TestTags.ACTIVE_WORKOUT_FINISH_REASON)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
