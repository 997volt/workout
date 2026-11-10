package com.example.androidapp.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.TestTags

/**
 * The program's visible recovery entry (ROADMAP N300).
 *
 * The overflow's entry is not enough for a program followed for years: a recovery week is a decision
 * about the plan, and the plan is this screen. The card opens the choice between exempting a week and
 * building a lighter one, rather than picking one for the lifter.
 */
@Composable
internal fun RecoveryWeekCard(onOpen: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onOpen)
            .testTag(TestTags.Programs.RECOVERY_WEEK),
    ) {
        Text(
            text = stringResource(R.string.program_recovery_week),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.program_recovery_week_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
