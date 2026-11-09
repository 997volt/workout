package com.example.androidapp.ui.workout

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

private const val SECONDS_PER_MINUTE = 60
private const val METRES_PER_KILOMETRE = 1000.0

/**
 * A session that is an **activity** rather than a workout (ROADMAP N141).
 *
 * Its own file because the screen was at this project's function ceiling, and these are one subject: what a
 * session holds when it holds no sets. An activity is a session — same row, same day, same history — that
 * **takes no sets at all**, so what is left of it is the readiness note and the comment asked for at the end.
 */

/**
 * The activity this session is, and the way back out of it (N141).
 *
 * Drawn **instead of** the empty workout's line: what the day holds is the activity and the note, not a set
 * anybody forgot to log. The way out is offered because a mispick would otherwise be a session with no sets
 * that cannot be made a workout again.
 */
@Composable
internal fun ActivityBody(
    activity: Activity,
    durationSeconds: Int?,
    distanceMeters: Long?,
    onSaveMetrics: (Int?, Long?) -> Unit,
    onClear: () -> Unit,
) {
    var duration by rememberSaveable(activity, durationSeconds) {
        mutableStateOf(durationSeconds?.let { (it / SECONDS_PER_MINUTE).toString() }.orEmpty())
    }
    var distance by rememberSaveable(activity, distanceMeters) {
        mutableStateOf(formatDistance(distanceMeters))
    }
    Column(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.ACTIVE_WORKOUT_ACTIVITY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(activityLabel(activity)),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.active_workout_activity_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.active_workout_activity_duration)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.ACTIVE_WORKOUT_ACTIVITY_DURATION),
        )
        OutlinedTextField(
            value = distance,
            onValueChange = { distance = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text(stringResource(R.string.active_workout_activity_distance)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.ACTIVE_WORKOUT_ACTIVITY_DISTANCE),
        )
        FilledTonalButton(
            onClick = {
                val minutes = duration.toIntOrNull()
                val km = distance.toDoubleOrNull()
                onSaveMetrics(
                    minutes?.let { (it * SECONDS_PER_MINUTE).takeIf { value -> value >= 0 } },
                    km?.let { (it * METRES_PER_KILOMETRE).toLong().takeIf { value -> value >= 0 } },
                )
            },
            modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_ACTIVITY_SAVE),
        ) {
            Text(stringResource(R.string.active_workout_activity_save))
        }
        AppTextButton(
            onClick = onClear,
            modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_ACTIVITY_CLEAR),
        ) {
            Text(stringResource(R.string.active_workout_activity_clear))
        }
    }
}

/**
 * The empty slot: the activity, or the line a session with nothing in it says.
 *
*/
@Composable
internal fun ActivityBodyOrEmpty(
    activity: Activity?,
    durationSeconds: Int?,
    distanceMeters: Long?,
    onSaveMetrics: (Int?, Long?) -> Unit,
    onClearActivity: () -> Unit,
) {
    if (activity != null) {
        ActivityBody(
            activity = activity,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            onSaveMetrics = onSaveMetrics,
            onClear = onClearActivity,
        )
    } else {
        EmptyWorkout()
    }
}

/**
 * The add rows, under the exercises (ROADMAP N82, N141).
 *
 * **Add activity is offered only while the session holds nothing**: an activity is not a workout with a
 * different label, and the two are never mixed. Nothing at all is drawn once the session *is* an activity —
 * that is the screen's rule that it can hold no sets.
 *
 * *Add exercise* is the one action a workout always offers (ROADMAP F1), at the foot of the list (N82): it was
 * an extended floating button over the list's last rows, which is why that list carried 96 dp of bottom padding
 * to stay clear of it. It keeps the colour it had and takes *Log set*'s form (N59) — full width, in the flow,
 * with the + it already carried. `PrimaryActionButton` is the near miss rather than the answer: it is already a
 * full-width pill that "used to be" an extended floating button (P1.16, N1) and takes both colours as
 * parameters, but at 52 dp with its own glyph spacing it is not *Log set*'s shape.
 */
@Composable
internal fun ActivityAddRows(
    activity: Activity?,
    isEmpty: Boolean,
    onAddExercise: () -> Unit,
    onAddActivity: () -> Unit,
) {
    if (activity != null) return
    Column(modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = onAddExercise,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(TestTags.ACTIVE_WORKOUT_ADD_EXERCISE),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.active_workout_add_exercise))
        }
        if (isEmpty) {
            AppTextButton(
                onClick = onAddActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(TestTags.ACTIVE_WORKOUT_ADD_ACTIVITY),
            ) {
                Text(stringResource(R.string.active_workout_add_activity))
            }
        }
    }
}

/**
 * The short closed list (ROADMAP N141), and picking one is what closes it.
 *
 * Stateless on purpose: whether it is open is transient UI, so it belongs beside the row that opens it — which
 * is the screen, not this file.
 */
@Composable
internal fun ActivityPicker(open: Boolean, onPick: (Activity) -> Unit, onDismiss: () -> Unit) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.ACTIVITY_PICKER),
        title = { Text(stringResource(R.string.activity_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Activity.entries.forEach { activity ->
                    AppTextButton(
                        onClick = {
                            onPick(activity)
                            onDismiss()
                        },
                        modifier = Modifier.testTag(TestTags.activity(activity)),
                    ) {
                        Text(stringResource(activityLabel(activity)))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * What an activity is called (ROADMAP N141): one place, so the picker, the body and the history row cannot
 * disagree (N146).
 */
@StringRes
fun activityLabel(activity: Activity): Int = when (activity) {
    Activity.CYCLING -> R.string.activity_cycling
    Activity.RUNNING -> R.string.activity_running
    Activity.SWIMMING -> R.string.activity_swimming
    Activity.ROCK_CLIMBING -> R.string.activity_rock_climbing
    Activity.VOLLEYBALL -> R.string.activity_volleyball
}

/** Kilometres for an activity field, without trailing zeroes: 1500 m -> "1.5", 2000 m -> "2". */
private fun formatDistance(metres: Long?): String =
    metres?.let { (it / METRES_PER_KILOMETRE).toString().trimEnd('0').trimEnd('.') }.orEmpty()
