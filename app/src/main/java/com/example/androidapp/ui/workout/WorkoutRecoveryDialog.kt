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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.label
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The finish's joint-pain decision (ROADMAP N377).
 *
 * At 7 or more the pain the lifter reported is not a hold for the next session but a question about this
 * plan: the same Deload and Replace the stalled card offers, with the lifter's own score as the reason. It is
 * still the plan editor that edits and undoes, so this dialog only names the lift and hands the target off.
 *
 * A **separate dialog** from [WorkoutStallDialog] rather than another section in it: a stall is a
 * prescription that stopped moving, while this is a rating the lifter just gave, and one title claiming the
 * other would be false. The finish shows them in a fixed order instead.
 */
@Composable
internal fun WorkoutRecoveryDialog(
    notices: List<RecoveryNotice>,
    onDone: () -> Unit,
    onDeload: (RecoveryNotice) -> Unit,
    onReplace: (RecoveryNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag(TestTags.RECOVERY_DIALOG),
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.recovery_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notices.forEach { notice ->
                    val replaceDescription = stringResource(
                        R.string.program_stall_replace_lift_for,
                        notice.exerciseName,
                    )
                    val deloadDescription = stringResource(
                        R.string.program_stall_deload_lift_for,
                        notice.exerciseName,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = notice.exerciseName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = stringResource(
                                R.string.recovery_dialog_line,
                                notice.pain.label,
                                notice.pain.score,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextButton(
                                onClick = { onReplace(notice) },
                                modifier = Modifier
                                    .testTag(TestTags.recoveryDialogReplace(notice.templateExerciseId))
                                    .semantics { contentDescription = replaceDescription },
                            ) {
                                Text(stringResource(R.string.program_stall_replace_lift))
                            }
                            AppTextButton(
                                onClick = { onDeload(notice) },
                                modifier = Modifier
                                    .testTag(TestTags.recoveryDialogDeload(notice.templateExerciseId))
                                    .semantics { contentDescription = deloadDescription },
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
                modifier = Modifier.testTag(TestTags.RECOVERY_DONE),
            ) {
                Text(stringResource(R.string.finish_stall_done))
            }
        },
    )
}
