package com.example.androidapp.ui.workout

import androidx.compose.runtime.Composable

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
