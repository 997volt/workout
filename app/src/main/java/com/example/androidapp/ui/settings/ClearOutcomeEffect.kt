package com.example.androidapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.androidapp.domain.DataError
import com.example.androidapp.ui.transfer.ClearOutcome

/**
 * The clear's one-shot answer, said once and then acknowledged (B456).
 *
 * The callbacks are read through `rememberUpdatedState`: this effect is keyed on the outcome, so a
 * lambda captured directly would keep reporting into the composition that asked for the clear.
 */
@Composable
internal fun ClearOutcomeEffect(
    outcome: ClearOutcome?,
    onClear: () -> Unit,
    onFail: (DataError) -> Unit,
    onAcknowledge: () -> Unit,
) {
    val currentOnClear by rememberUpdatedState(onClear)
    val currentOnFail by rememberUpdatedState(onFail)
    val currentOnAcknowledge by rememberUpdatedState(onAcknowledge)
    LaunchedEffect(outcome) {
        when (val result = outcome ?: return@LaunchedEffect) {
            is ClearOutcome.Cleared -> currentOnClear()
            is ClearOutcome.Failed -> currentOnFail(result.error)
        }
        currentOnAcknowledge()
    }
}
