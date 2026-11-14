package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The one thing a finished workout still says (ROADMAP N305).
 *
 * A review of what the session did would repeat what history already reads back, and compare it to a plan
 * auto progression may already have moved. A lift that has stopped moving is the fact the screen cannot show
 * while it is being trained, so it is what the finish says — and nothing else. It is a **decision** now
 * (N310, amending N305): each stalled lift offers Deload and Replace, and both hand off to the plan editor,
 * which owns the confirm, the replace sheet and the undo. *Done* is do nothing.
 */
@Composable
internal fun WorkoutStallDialog(
    stalls: List<PlanStall>,
    onDone: () -> Unit,
    onDeload: (PlanStall) -> Unit,
    onReplace: (PlanStall) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag(TestTags.STALL_DIALOG),
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.finish_stall_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stalls.forEach { stall ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stall.exerciseName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = pluralStringResource(
                                R.plurals.program_stall_sessions,
                                stall.sessions,
                                stall.sessions,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextButton(
                                onClick = { onReplace(stall) },
                                modifier = Modifier.testTag(TestTags.STALL_DIALOG_REPLACE),
                            ) {
                                Text(stringResource(R.string.program_stall_replace_lift))
                            }
                            AppTextButton(
                                onClick = { onDeload(stall) },
                                modifier = Modifier.testTag(TestTags.STALL_DIALOG_DELOAD),
                            ) {
                                Text(stringResource(R.string.program_stall_deload_lift))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDone,
                modifier = Modifier.testTag(TestTags.STALL_DONE),
            ) {
                Text(stringResource(R.string.finish_stall_done))
            }
        },
    )
}
