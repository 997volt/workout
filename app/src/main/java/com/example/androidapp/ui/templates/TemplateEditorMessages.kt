package com.example.androidapp.ui.templates

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.ui.components.dataErrorMessage

/**
 * The editor's three sentences, on the screen's one host (F7, N90, N134).
 *
 * Split out so the screen itself stays inside the length this project allows, and because the three are
 * one idea: something happened to this plan, and it is said once rather than left to be inferred.
 */
@Composable
internal fun TemplateEditorMessages(
    state: TemplateEditorUiState,
    versionUi: TemplateVersionUi,
    hostState: SnackbarHostState,
    onDismissMessage: () -> Unit,
    onUndoRemoveSet: () -> Unit,
    onDismissUndo: () -> Unit,
    hiddenChange: Boolean? = null,
    onDismissHiddenNotice: () -> Unit = {},
) {
    ShowRemovedSetUndo(
        pendingUndo = state.undoableSet,
        hostState = hostState,
        onUndo = onUndoRemoveSet,
        onDismiss = onDismissUndo,
    )
    ShowEditorError(state, hostState, onDismissMessage)
    ShowVersionKept(
        versionKept = versionUi.versionKept,
        hostState = hostState,
        onDismiss = versionUi.onDismissVersionNotice,
    )
    ShowHiddenChanged(
        hidden = hiddenChange,
        hostState = hostState,
        onDismiss = onDismissHiddenNotice,
    )
}

/**
 * Hiding or unhiding the plan, said once (ROADMAP N214).
 *
 * Nothing else on the screen changes when the item is used — the plan is not deleted and the editor
 * stays — so the sentence is the confirmation, and the item's own label is the state afterwards.
 */
@Composable
internal fun ShowHiddenChanged(
    hidden: Boolean?,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    if (hidden == null) { return }
    val message = stringResource(
        if (hidden) R.string.template_hidden_notice else R.string.template_unhidden_notice,
    )
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(hidden) {
        hostState.showSnackbar(message)
        currentOnDismiss()
    }
}

/**
 * A failed write, said once and then dismissed (F7).
 *
 * Resolved during composition and shown from the effect, because a string resource cannot be read inside
 * `LaunchedEffect`. Split out with the undo offer so the screen itself stays inside the length this
 * project allows.
 */
@Composable
internal fun ShowEditorError(
    state: TemplateEditorUiState,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val failure = state.error ?: return
    val message = dataErrorMessage(failure)
    // Held as a State, so the effect's key stays the message (B3's rule for the workout's two boxes):
    // reading the lambda directly inside a restarting effect is what the Compose rules warn about.
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(message) {
        hostState.showSnackbar(message)
        currentOnDismiss()
    }
}

/**
 * Undo for a planned set just removed (ROADMAP N90).
 *
 * The workout's deleted-set snackbar, one screen over: a mis-tap on a set's delete used to be
 * unrecoverable, and the plan was simply missing a set with nothing said. One undo at a time, so the
 * offered [pendingUndo] is a single set rather than a queue; the screen's own host means it cannot
 * outlive the screen either.
 */
@Composable
internal fun ShowRemovedSetUndo(
    pendingUndo: TemplateSet?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.set_deleted)
    val undoLabel = stringResource(R.string.set_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}
