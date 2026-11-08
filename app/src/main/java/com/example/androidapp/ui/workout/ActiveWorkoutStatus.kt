package com.example.androidapp.ui.workout

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.ReadinessNoteDialog
import com.example.androidapp.ui.components.TestTags

// The workout body's own status lines (ROADMAP N4, N27, N44, N82, N141): the elapsed header, the rest
// bar and its cue, the readiness row, and the empty session.
@Composable
internal fun RestBar(
    clock: State<WorkoutClock>,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** False when the timer is switched off (ROADMAP N44), so nothing is counted or adjusted. */
    enabled: Boolean = true,
) {
    // Deciding whether to show this bar in the *parent* would recompose the exercise
    // list once a second. Returning early here keeps the tick inside this composable.
    val remaining = clock.value.restSecondsRemaining
    if (!enabled || remaining <= 0) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.rest_remaining, RestTimer.format(remaining)),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The one surface whose links are drawn *on* the filled container: the readable role
                // here is the container's own content colour, not the page's link colour (ROADMAP N49).
                AppTextButton(
                    onClick = { onAdjust(-RestTimer.ADJUST_STEP_SECONDS) },
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_subtract))
                }
                AppTextButton(
                    onClick = { onAdjust(RestTimer.ADJUST_STEP_SECONDS) },
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_add))
                }
                AppTextButton(
                    onClick = onSkip,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_skip))
                }
            }
        }
    }
}

@Composable
internal fun WorkoutHeader(
    startedAt: String,
    clock: State<WorkoutClock>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.active_workout_started, startedAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The only place the elapsed time is read, so a tick stops here.
        Text(text = clock.value.elapsed, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The readiness line and the dialog behind it (ROADMAP N4).
 *
 * "A form is open" is transient UI state, so it lives here next to the row that
 * opens it, and that row is the **only** way in: the prompt a brand-new session used to open with went with
 * N140.
 */
@Composable
internal fun ReadinessSection(
    note: String?,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    ReadinessRow(note = note, onEdit = { editing = true }, modifier = modifier)
    // **Only the row opens it** (N140). There is no prompt to dismiss, so the dialog's own dismiss is the
    // whole of closing it.
    if (editing) {
        ReadinessNoteDialog(
            initialNote = note.orEmpty(),
            onDismiss = { editing = false },
            onSave = { written ->
                editing = false
                onSave(written)
            },
        )
    }
}

/**
 * The readiness note in the workout header (ROADMAP N4).
 *
 * Always present, so the field stays findable whether or not the lifter writes it — which is the whole of the
 * job now that this row is the only way in (N140). A field nobody can find again is the failure that made it a
 * prompt in the first place, and the row is the cheaper answer to it.
 *
 * **The sore list is not read here any more** (N113): a live session's header is an editor, and soreness
 * is the day's own record now. A session that recorded one before the move still shows it in history.
 */
@Composable
private fun ReadinessRow(
    note: String?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.readiness_edit_action)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.READINESS_ROW)
            .clickable(onClickLabel = editLabel, onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.readiness_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = note ?: stringResource(R.string.readiness_add),
            style = MaterialTheme.typography.bodyMedium,
            color = if (note == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/**
 * What an empty session says: nothing is in it yet, and the one action that answers that is below it.
 *
 * It no longer carries a discard of its own (ROADMAP N41, N87 amended). The overflow is drawn for an empty
 * session too now, so the action has one home rather than a second copy here — and the copy here is what a
 * *test* anchors the order of the message and the add button on.
 */
@Composable
internal fun EmptyWorkout(modifier: Modifier = Modifier) {
    Column(
        // A list item since N82, so it takes the width it is given rather than the whole screen: the
        // centring it used was against a viewport this no longer owns.
        modifier = modifier.fillMaxWidth().padding(24.dp).testTag(TestTags.ACTIVE_WORKOUT_EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.active_workout_empty),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.active_workout_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * The two things a workout asks of the device rather than of the database (ROADMAP N27).
 *
 * Both live inside the permission-free envelope N26 bought, which is recorded in DECISIONS.md:
 * keep-screen-on is a window flag rather than a wake lock, and the cue is an in-process tone plus
 * **view-level** haptics. `Vibrator` would have been the obvious call and it costs
 * `android.permission.VIBRATE` — the one permission the app just stopped declaring.
 */
@Composable
internal fun RestCueAndScreenOn(
    clock: State<WorkoutClock>,
    restCueEnabled: Boolean,
    keepScreenOn: Boolean,
    restTimerEnabled: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val tone = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    // The cue is the *transition* out of resting, not the state: a rest is cued when it ends, and a
    // workout that opens with no rest must not chime.
    var wasResting by remember { mutableStateOf(false) }
    LaunchedEffect(clock) {
        snapshotFlow { clock.value.isResting }.collect { resting ->
            // Split, because four tests in one condition is where a reader stops counting: the rest
            // ended, and both switches can silence it.
            val restEnded = !resting && wasResting
            if (restEnded && restCueEnabled && restTimerEnabled) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_MILLIS)
                // `CONFIRM` is API 30 and this app supports 26; lint's InlinedApi rule caught that,
                // which is what it is for. `VIRTUAL_KEY` is the same short tick and has existed since
                // API 1.
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            wasResting = resting
        }
    }
}

/** Loud enough to hear across a gym; short enough to be a cue rather than a ringtone. */
private const val TONE_VOLUME = 70
private const val TONE_MILLIS = 250
