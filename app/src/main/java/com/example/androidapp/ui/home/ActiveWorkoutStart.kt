package com.example.androidapp.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.programs.StartIntent
import kotlinx.coroutines.launch

/**
 * Whether starting [intent] has to ask first, because a workout is already running (ROADMAP N89).
 *
 * Only a *template* start asks. An empty start is what home's own pill does, and when a session is open
 * that pill already reads *Resume*, so there is nothing to choose. File-level and pure, so the rule is
 * testable without a screen.
 */
internal fun needsActiveWorkoutChoice(intent: StartIntent, hasActiveWorkout: Boolean): Boolean =
    intent.templateId != null && hasActiveWorkout

/**
 * The active-session question, wired (ROADMAP N89).
 *
 * Returns the start the screen's actions call — the shape [com.example.androidapp.ui.programs.programStartGate]
 * uses, and for the same reason: the state that says "this question is open" belongs with the question, not
 * in the route that happens to host it. It is composed *inside* the missed-day gate's `onStart`, so it
 * intercepts a start only once that question is settled; asking the other way round could leave a lifter
 * with a discarded session and nothing started if they then dismissed the missed-day prompt.
 *
 * [isActive] is read when a start arrives rather than captured, because the session can begin or end while
 * the screen is composed. [discard] ends the running session; [onStart] carries out the start once it has.
 */
@Composable
internal fun activeWorkoutGate(
    isActive: () -> Boolean,
    onStart: (StartIntent) -> Unit,
    /** Where *Continue workout* goes: the session already running. */
    onContinueOngoing: () -> Unit,
    discard: suspend () -> DataResult<Unit>,
    onFailure: (DataError) -> Unit,
): (StartIntent) -> Unit {
    var pending by remember { mutableStateOf<StartIntent?>(null) }
    // True while the soft delete is in flight (B91): both answers stay live for the whole suspend write
    // otherwise, and a second tap either navigates twice or reports a delete that lost the race.
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ActiveWorkoutDialog(
        start = pending,
        busy = busy,
        onContinue = {
            if (!busy) {
                pending = null
                onContinueOngoing()
            }
        },
        onDiscardAndStart = { intent ->
            if (!busy) {
                busy = true
                scope.launch {
                    val result = discard()
                    busy = false
                    when (result) {
                        is DataResult.Success -> {
                            pending = null
                            onStart(intent)
                        }

                        is DataResult.Failure -> {
                            // The session is still there, so nothing starts and the failure is said (F7).
                            onFailure(result.error)
                            pending = null
                        }
                    }
                }
            }
        },
        onDismiss = { if (!busy) pending = null },
    )

    return { intent ->
        if (needsActiveWorkoutChoice(intent, isActive())) {
            pending = intent
        } else {
            onStart(intent)
        }
    }
}

/**
 * The question a planned start asks while one workout is already running (ROADMAP N89).
 *
 * `startOrResumeSession` is find-or-create, so without this the plan being started is silently dropped and
 * the running session handed back — the lie N78 named, and fixed in one place only, the templates list,
 * which disables *Start* instead. This offers the choice: **Discard and start new** ends the running
 * session the way the workout screen's own discard does and starts what was asked for, while **Continue
 * workout** opens the session already running.
 *
 * The destructive answer is the affirmative one, the shape the workout screen's discard dialog uses,
 * because starting the plan is what the lifter asked for. Dismissing cancels the start rather than choosing
 * for them — the rule the missed-day question follows (P3.3). [busy] disables every answer while the discard
 * is in flight, so the question cannot be answered twice (B91).
 */
@Composable
internal fun ActiveWorkoutDialog(
    start: StartIntent?,
    onContinue: () -> Unit,
    onDiscardAndStart: (StartIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while the destructive answer's write is running: both buttons are dead until it lands. */
    busy: Boolean = false,
) {
    val current = start ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG),
        title = { Text(stringResource(R.string.home_active_workout_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.home_active_workout_body,
                    current.label ?: stringResource(R.string.home_active_workout_this),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { onDiscardAndStart(current) },
                modifier = Modifier.testTag(TestTags.HOME_ACTIVE_WORKOUT_DISCARD),
                enabled = !busy,
            ) {
                Text(stringResource(R.string.home_active_workout_discard))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onContinue,
                modifier = Modifier.testTag(TestTags.HOME_ACTIVE_WORKOUT_CONTINUE),
                enabled = !busy,
            ) {
                Text(stringResource(R.string.home_active_workout_continue))
            }
        },
    )
}
