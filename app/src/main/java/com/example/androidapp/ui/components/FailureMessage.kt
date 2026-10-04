package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.androidapp.domain.DataError

/**
 * Turns a held write failure into a snackbar sentence once, then clears it (F7).
 *
 * A failure is held as a value rather than a message because `dataErrorMessage` is a composable:
 * the sentence cannot be read inside the coroutine that produced the failure, so it is resolved
 * during composition. Extracted from the home screen when the settings screen became its second
 * caller (ROADMAP N43).
 */
@Composable
fun FailureMessage(
    failure: DataError?,
    onMessage: (String) -> Unit,
    onClear: () -> Unit,
) {
    // Read through `rememberUpdatedState` so the effect cannot fire a stale callback after a
    // recomposition, which is what the lint rule below is about.
    val currentOnMessage by rememberUpdatedState(onMessage)
    val currentOnClear by rememberUpdatedState(onClear)
    val text = failure?.let { dataErrorMessage(it) }
    LaunchedEffect(text) {
        if (text != null) {
            currentOnMessage(text)
            currentOnClear()
        }
    }
}
