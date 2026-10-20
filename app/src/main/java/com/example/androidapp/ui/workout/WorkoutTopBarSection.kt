package com.example.androidapp.ui.workout

import androidx.compose.runtime.Composable
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.ProgressionDialog

/**
 * The workout's top bar, with the two rules it carries (N41, N141, N143).
 *
 * Extracted from the screen when it reached the length this project allows. **An activity finishes with
 * no sets** (N141), so it is finishable regardless; **the overflow is drawn whenever a session is open**
 * (N41, N87 amended), because an empty workout is exactly the one a mistap leaves open; and a discard with
 * nothing to lose does not ask (N41) unless it is an activity (N143) — empty of sets, not of record.
 */
@Composable
internal fun WorkoutTopBarSection(
    state: ActiveWorkoutUiState,
    countsAgainstProgram: Boolean,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
) {
    WorkoutTopBar(
        canFinish = state.activity != null || state.exercises.any { it.sets.isNotEmpty() },
        canDiscard = state.sessionId != null,
        isEmpty = state.isEmpty,
        isActivity = state.activity != null,
        setCount = state.exercises.sumOf { it.sets.size },
        countsAgainstProgram = countsAgainstProgram,
        onFinish = onFinish,
        onDiscard = onDiscard,
        onBack = onBack,
    )
}

/** The progression question, wired to the screen's callbacks (P3.17). */
@Composable
internal fun ProgressionSection(
    progression: PendingProgression,
    onToggle: (String, ProgressionDirection) -> Unit,
    onSelectAll: (ProgressionDirection) -> Unit,
    onSelectEvery: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ProgressionDialog(progression, onToggle, onSelectAll, onSelectEvery, onConfirm, onDismiss)
}

/** The set editor's host, so the screen's call is one line. */
@Composable
internal fun SetEditorHost(
    editing: SetRow?,
    exercises: List<SessionExerciseRow>,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    SetEditorSection(editing, exercises, onUpdateSet, onDismiss)
}
