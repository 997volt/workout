package com.example.androidapp.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.AppTextButton

/**
 * What a next-up row has planned, read on the tap that opened it (ROADMAP N55).
 *
 * A dialog rather than the template's editor, which is the only destination a template had: the
 * question the field answers is "what is in this workout", and opening the editor to answer it would
 * put every target in the plan one mis-tap from being rewritten on the way to reading it. It is also
 * a dialog rather than a screen because the answer is short — the workout's ordered exercises — and a
 * destination for a list of names would be a screen with a back button and nothing to do on it.
 */
@Composable
fun PlannedWorkoutDialog(
    planned: PlannedWorkout?,
    onDismiss: () -> Unit,
) {
    val workout = planned ?: return

    AlertDialog(
        modifier = Modifier.testTag(TestTags.Home.PLANNED_WORKOUT),
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(workout.plan.name, modifier = Modifier.testTag(TestTags.Home.PLANNED_WORKOUT_TITLE))
                Text(
                    text = workout.programName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            if (workout.isLoading) {
                // A read that has not answered yet is not an empty plan: the spinner is the difference.
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (workout.exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_planned_empty),
                    modifier = Modifier.testTag(TestTags.Home.PLANNED_WORKOUT_EMPTY),
                )
            } else {
                LazyColumn {
                    itemsIndexed(workout.exercises) { index, name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            leadingContent = { Text("${index + 1}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Home.PLANNED_WORKOUT_CLOSE),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
