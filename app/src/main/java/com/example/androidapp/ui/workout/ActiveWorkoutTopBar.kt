package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.WorkoutNoteDialog

// Leaving a workout (ROADMAP N11, N41, N87): the bar's finish and discard actions, the overflow, and
// the prompt a discard asks.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutTopBar(
    canFinish: Boolean,
    canDiscard: Boolean,
    /** An open session with nothing in it, so a discard costs nothing and is not asked about (N41). */
    isEmpty: Boolean,
    /**
     * The session is an **activity**, so "nothing in it" does not mean "nothing to lose" (N143).
     *
     * An activity holds no sets by construction, so `isEmpty` is true for exactly the session a lifter
     * deliberately recorded — and the one unconfirmed tap then deleted the day's record with no undo. The
     * prompt-free branch is for a session that is empty *because nothing was done*, which an activity never is.
     */
    isActivity: Boolean,
    setCount: Int,
    countsAgainstProgram: Boolean,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    // Whether the finish prompt is up. Dismissing it finishes without a comment
    // (ROADMAP N11): the user asked to finish, and the comment is optional.
    var commenting by rememberSaveable { mutableStateOf(false) }
    // The discard lives behind the overflow rather than beside Finish: it is the destructive
    // action, and the top bar's visible slot is the one the user reaches for mid-workout.
    var confirmingDiscard by remember { mutableStateOf(false) }

    TopAppBar(
        title = { TopBarTitle(text = stringResource(R.string.active_workout_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // Nothing to finish until at least one set is logged — or the session is an activity, which
            // holds none and is finished as what it records (N141).
            FinishAction(canFinish = canFinish, onClick = { commenting = true })
            if (canDiscard) {
                WorkoutMenu(
                    onDiscard = {
                        // **Nothing to lose, so nothing to ask** (N41, N87): the dialog exists to name what a
                        // discard costs, and an empty *workout* has nothing to name. An activity is the
                        // exception that proves it (N143): it is empty because it holds no sets, not because
                        // nobody did anything, so it is always asked about.
                        if (isEmpty && !isActivity) onDiscard() else confirmingDiscard = true
                    },
                )
            }
        },
    )

    if (confirmingDiscard) {
        DiscardWorkoutDialog(
            setCount = setCount,
            isActivity = isActivity,
            countsAgainstProgram = countsAgainstProgram,
            onDismiss = { confirmingDiscard = false },
            onConfirm = {
                confirmingDiscard = false
                onDiscard()
            },
        )
    }

    if (commenting) {
        WorkoutNoteDialog(
            onDismiss = {
                commenting = false
                onFinish(null)
            },
            onSave = { note ->
                commenting = false
                onFinish(note)
            },
        )
    }
}

/**
 * The workout's overflow, which holds the discard (ROADMAP N41, N87).
 *
 * **It is the one entry it has**, because N87 took the two plans back out: mid-workout the overflow is the
 * logger's own business, and the lists that answer "what is next" belong where the decision to train is
 * taken — home's start bar (N42), which stays visible while a session is open, so looking at a plan is
 * still one step back rather than the end of the workout. N34 is what keeps this screen off the tab bar,
 * and N78 put the plans here for that reason; N87 keeps the argument and drops the half that was never
 * argued, since home reaches both without ending anything.
 *
 * Because the discard is all that is left, the ⋮ is drawn only when there is something to discard — see
 * [WorkoutTopBar]. An empty session's discard is prompt-free and drawn in the body (N41), so it has no use
 * for a menu at all. The menu's open state belongs with the button that opens it, which is why it is split
 * out rather than inlined into a top bar already at the length this project allows.
 */
@Composable
private fun WorkoutMenu(onDiscard: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_MENU),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.active_workout_more),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.active_workout_discard)) },
                onClick = {
                    menuOpen = false
                    onDiscard()
                },
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD),
            )
        }
    }
}

/**
 * The prompt a discard asks before it deletes anything (ROADMAP N41).
 *
 * It says what goes — the count, because the user is deciding whether it is worth keeping — and
 * when the workout came from a program it says the second consequence too: only a *finished*
 * session settles a scheduled occurrence (P3.5), so abandoning this one is recorded as a miss
 * rather than as no workout at all. The empty workout never reaches this dialog, because there is
 * nothing to lose and its prompt-free discard is right.
 */
@Composable
private fun DiscardWorkoutDialog(
    setCount: Int,
    /** An activity records the day rather than sets, so what a discard costs is said differently (N143). */
    isActivity: Boolean,
    countsAgainstProgram: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_workout_discard_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when {
                        isActivity -> stringResource(R.string.active_workout_discard_text_activity)
                        setCount > 0 -> pluralStringResource(
                            R.plurals.active_workout_discard_text,
                            setCount,
                            setCount,
                        )

                        else -> stringResource(R.string.active_workout_discard_text_no_sets)
                    },
                    modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_TEXT),
                )
                if (countsAgainstProgram) {
                    Text(
                        text = stringResource(R.string.active_workout_discard_program),
                        modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_PROGRAM),
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_CONFIRM),
            ) {
                Text(stringResource(R.string.active_workout_discard))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
