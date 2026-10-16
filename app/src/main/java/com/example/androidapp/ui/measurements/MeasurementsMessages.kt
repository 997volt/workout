package com.example.androidapp.ui.measurements

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.ui.statistics.EnergyAdjustment
import com.example.androidapp.ui.statistics.adjustmentBandText

/**
 * What the measurements screen says after a write — the undo it offers and the answer a weigh-in gets.
 *
 * Split out for the reason the template editor's messages are: the screen's own body is at this project's
 * function ceiling, and the two are the same kind of thing — a snackbar the ViewModel hands over once.
 */

/**
 * Undo for an entry a delete just took (ROADMAP N133).
 *
 * A soft delete is what makes the way back possible, and the snackbar naming what went is the shape the
 * workout screen's deleted set already uses (N7, B3, N90) — not a modal "are you sure?" in front of a
 * reversible act, which is the interruption that shape exists to avoid. One undo at a time: the
 * ViewModel holds the single [pendingUndo], so the offer is that entry rather than a queue.
 */
@Composable
internal fun ShowDeletedMeasurementUndo(
    pendingUndo: BodyMeasurement?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.measurements_deleted)
    val undoLabel = stringResource(R.string.measurements_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Held as a State so the effect's key stays the removed entry, the workout screen's rule (B3): a
    // second delete of the same row must still offer its own undo.
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

/**
 * What a weigh-in says about the goal's rate (ROADMAP N136).
 *
 * The same sentence the statistics screen states under its chart, said where the weighing happens — because
 * that is where a lifter finds out *this week* whether the trend is the rate they asked for. A **line after
 * the save**, not a dialog: it states, it is dismissible, it does not pursue (P4.7), which is the snackbar's
 * own shape.
 *
 * Null is silence, and the ViewModel is what decides it: no goal, no weight in the entry, or too little
 * history to be sure. This is the presentation of an answer that was already judged worth saying.
 */
@Composable
internal fun ShowRateVerdict(
    rateVerdict: EnergyAdjustment?,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val message = rateVerdict?.let { adjustmentText(it) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Keyed on the verdict, so a second weigh-in says its own piece rather than being swallowed as "the same
    // value" — the rule the delete's undo follows (B3).
    LaunchedEffect(rateVerdict) {
        if (message != null) {
            hostState.showSnackbar(message)
            currentOnDismiss()
        }
    }
}

/** The arithmetic in words: the answer, or the move that would make it true (N98, N136). */
@Composable
internal fun adjustmentText(adjustment: EnergyAdjustment): String = when {
    adjustment.isOnTarget -> stringResource(R.string.statistics_energy_on_target)
    adjustment.eatsMore ->
        stringResource(R.string.statistics_energy_more, adjustmentBandText(adjustment.magnitude))

    else -> stringResource(R.string.statistics_energy_less, adjustmentBandText(adjustment.magnitude))
}
