// The tags record rides with the menu it names, the way WarmUpTarget rides with `warmUpRamp`: a
// second file for five strings would be a file to find rather than a fact to read.
@file:Suppress("MatchingDeclarationName")

package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R

/**
 * The test tags one screen's exercise actions answer to (ROADMAP N71).
 *
 * A record rather than four loose strings, and per screen rather than shared, because a tag has to
 * address **one** control: the workout's rows and a template's rows are two lists that can be on
 * screen in the same process, and the shapes [SetFieldTags] and [SetRoleSelector] already use are the
 * ones this follows.
 */
data class ExerciseMenuTags(
    val menu: String,
    val moveUp: String,
    val moveDown: String,
    val superset: String,
    val remove: String,
)

/**
 * The optional warm-up entry one screen's ⋮ may carry (ROADMAP N81): the label it reads under, the tag
 * it answers to, and what it writes.
 *
 * A record rather than three parameters because the three only travel together, and it carries its own
 * tag rather than joining [ExerciseMenuTags] because it is the one entry that is *not* every screen's:
 * a template offers it wherever a ramp can be built (N28, B50) and the workout's menu must not grow a
 * control that writes a plan, so "no entry" has to be expressible — the shape a nullable
 * `supersetGrouped` uses one entry up. The pair shape `WarmUpTarget` rides with `warmUpRamp` is the
 * same one.
 */
data class WarmUpAction(
    val label: String,
    val tag: String,
    val onClick: () -> Unit,
)

/**
 * One exercise's rare actions, behind its own ⋮: order, pairing, and the destructive one last
 * (ROADMAP N53, N71).
 *
 * N53 moved the workout's *Superset with above* and *Delete* off the header and into a per-exercise
 * overflow, because the header is read constantly mid-session and a text link in every one of them
 * cost more attention than the action earned. The template editor kept its own four trailing controls
 * — a superset toggle, two arrows and a delete icon — for the same actions. This is that overflow,
 * extracted at its second caller, because a near-copy of a menu on a second screen is how two
 * vocabularies for one idea start.
 *
 * The order is the workout's: **order first**, because it is the entry most likely to be reached for
 * mid-session and the one that changes what everything below it means; then pairing; then the
 * destructive entry, last and coloured. Each is offered only where it exists — a direction with
 * nowhere to go is absent rather than writing nothing, and a row with nothing above it is passed a
 * null [supersetGrouped], which is B28's row-0 exclusion.
 *
 * **Remove asks first**, because it takes the exercise's sets with it and there is no undo to reach
 * for; the sentence is the caller's, since what the sets leave is a workout or a template.
 *
 * Since N81 it also carries one entry only a template offers — *Add warm-ups* (N28) — as an optional
 * [WarmUpAction]. The entry moved here from the plan dialog the template editor no longer has, and it
 * comes with its own label and tag because the workout's menu, which shares this component (N71), draws
 * no such control: what a ramp needs is a plan's working weight, and a workout has logs.
 */
@Composable
fun ExerciseActionsMenu(
    contentDescription: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    /** Whether the exercise is already in a superset, or null when the entry is not offered (B28). */
    supersetGrouped: Boolean?,
    removeTitle: String,
    removeText: String,
    onToggleSuperset: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    tags: ExerciseMenuTags,
    modifier: Modifier = Modifier,
    /** The warm-up entry, or null where this screen offers none — which is the workout's own (N81). */
    addWarmUps: WarmUpAction? = null,
) {
    // `rememberSaveable`, like the program slot's own menu (N72): the activity declares no
    // `configChanges`, so rotation rebuilds it, and a confirmation the lifter had open would
    // otherwise vanish mid-decision. Nothing is written here, so this is only about the question.
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmingRemoval by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.testTag(tags.menu),
        ) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = contentDescription)
        }
        ExerciseMenuEntries(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            supersetGrouped = supersetGrouped,
            addWarmUps = addWarmUps,
            onToggleSuperset = onToggleSuperset,
            onMove = onMove,
            onRemove = { confirmingRemoval = true },
            tags = tags,
        )
    }

    if (confirmingRemoval) {
        RemoveExerciseDialog(
            title = removeTitle,
            text = removeText,
            onConfirm = {
                confirmingRemoval = false
                onRemove()
            },
            onDismiss = { confirmingRemoval = false },
        )
    }
}

/**
 * The entries themselves, split out so the state above reads as state (ROADMAP N71).
 *
 * The shape the workout screen's own overflow used before this was shared: the button, the menu and
 * the dialog are three things, and one function holding all three is how it reached the length this
 * project allows.
 */
@Composable
private fun ExerciseMenuEntries(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    supersetGrouped: Boolean?,
    addWarmUps: WarmUpAction?,
    onToggleSuperset: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    tags: ExerciseMenuTags,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canMoveUp) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercise_menu_move_up)) },
                onClick = menuClick(onDismiss) { onMove(-1) },
                modifier = Modifier.testTag(tags.moveUp),
            )
        }
        if (canMoveDown) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercise_menu_move_down)) },
                onClick = menuClick(onDismiss) { onMove(1) },
                modifier = Modifier.testTag(tags.moveDown),
            )
        }
        if (supersetGrouped != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (supersetGrouped) {
                                R.string.superset_unpair
                            } else {
                                R.string.superset_pair
                            },
                        ),
                    )
                },
                onClick = menuClick(onDismiss, onToggleSuperset),
                modifier = Modifier.testTag(tags.superset),
            )
        }
        // Above Remove and below pairing: it writes sets rather than taking them away, so N53's order
        // puts it after the entry that changes the round and before the destructive one. It is drawn
        // only where the caller supplies it (N81).
        addWarmUps?.let { entry ->
            DropdownMenuItem(
                text = { Text(entry.label) },
                onClick = menuClick(onDismiss, entry.onClick),
                modifier = Modifier.testTag(entry.tag),
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.exercise_menu_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = menuClick(onDismiss, onRemove),
            modifier = Modifier.testTag(tags.remove),
        )
    }
}

/**
 * One entry's click, which is always the same two things: close the menu, then act.
 *
 * File-level and pure, the shape [WarmUpAction] and the tag records use, so the entries above read as a
 * list of labels and actions rather than as one repeated block of three lines each.
 */
private fun menuClick(onDismiss: () -> Unit, action: () -> Unit): () -> Unit = {
    onDismiss()
    action()
}

/**
 * The question a removal asks before it takes anything (ROADMAP B2, moved by N53, shared by N71).
 *
 * Its own composable because the menu around it is at the length this project allows, and because the
 * guard is the part worth reading on its own: removing an exercise takes its sets with it and has no
 * undo to reach for. The tags are shared rather than per screen — one dialog is open at a time, and
 * the control it names is the same control either way.
 */
@Composable
private fun RemoveExerciseDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CONFIRM),
            ) {
                Text(stringResource(R.string.exercise_remove_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
