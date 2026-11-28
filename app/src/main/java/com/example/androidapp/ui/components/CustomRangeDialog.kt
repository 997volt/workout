package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import java.time.LocalDate

/**
 * A window with two ends, chosen a day at a time (ROADMAP N35, N332).
 *
 * Statistics' custom range and the block export's share this: the same two calendars, a date the lifter
 * can read, and Apply held until both ends are set. The callers supply their own labels and tags because
 * the two surfaces name the same action in their own words; the shape is one.
 *
 * **A start after the end is not a window.** Earlier both callers passed it through and let the range be
 * empty with the ends printed backwards; the shared dialog refuses it instead, and says which end to move.
 * Apply is the only action that can leave this dialog, and it is enabled only for an ordered pair.
 *
 * The dates are read back in UTC, which is the zone the picker reports in — converting them in the device's
 * zone would shift the day for anyone west of Greenwich and make "From 1 August" mean 31 July.
 */
// The dialog's length is its state and two composables; extracting them would only pass the same
// strings and tags through another signature.
@Suppress("LongMethod")
@Composable
fun CustomRangeDialog(
    title: String,
    fromLabel: String,
    toLabel: String,
    noDateLabel: String,
    applyLabel: String,
    /** How the caller words a calendar day; the history formatter for Statistics, the sheet's own for export. */
    formatDate: (LocalDate) -> String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    initialFrom: LocalDate? = null,
    initialTo: LocalDate? = null,
    fromTestTag: String? = null,
    toTestTag: String? = null,
    applyTestTag: String? = null,
    dialogTestTag: String? = null,
) {
    // Stored as days rather than LocalDate, which has no bundle saver (B465).
    var fromDay by rememberSaveable { mutableStateOf(initialFrom?.toString()) }
    var toDay by rememberSaveable { mutableStateOf(initialTo?.toString()) }
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val from = fromDay?.let(LocalDate::parse)
    val to = toDay?.let(LocalDate::parse)
    val ordered = isOrdered(from, to)

    AlertDialog(
        modifier = dialogTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRow(
                    label = fromLabel,
                    date = from,
                    noDate = noDateLabel,
                    testTag = fromTestTag,
                    formatDate = formatDate,
                    onClick = { picking = true },
                )
                DateRow(
                    label = toLabel,
                    date = to,
                    noDate = noDateLabel,
                    testTag = toTestTag,
                    formatDate = formatDate,
                    onClick = { picking = false },
                )
                if (isReversed(from, to)) {
                    Text(stringResource(R.string.custom_range_order))
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { if (ordered) onConfirm(checkNotNull(from), checkNotNull(to)) },
                enabled = ordered,
                modifier = applyTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Text(applyLabel)
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    picking?.let { choosing ->
        RangeDatePrompt(
            choosing = choosing,
            from = from,
            to = to,
            onDismiss = { picking = null },
            onPick = { date ->
                if (choosing) fromDay = date.toString() else toDay = date.toString()
                picking = null
            },
        )
    }
}

/** A start after the end is refused rather than passed through as an empty, backwards window (N332). */
private fun isOrdered(from: LocalDate?, to: LocalDate?): Boolean =
    from != null && to != null && !from.isAfter(to)

private fun isReversed(from: LocalDate?, to: LocalDate?): Boolean =
    from != null && to != null && from.isAfter(to)

/** The calendar for whichever end is being chosen. */
@Composable
private fun RangeDatePrompt(
    choosing: Boolean,
    from: LocalDate?,
    to: LocalDate?,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    DatePrompt(
        initial = if (choosing) from else to,
        onDismiss = onDismiss,
        onPick = onPick,
    )
}

/** One end of the window: what it is, what it currently says, and the way to change it. */
@Composable
private fun DateRow(
    label: String,
    date: LocalDate?,
    noDate: String,
    testTag: String?,
    formatDate: (LocalDate) -> String,
    onClick: () -> Unit,
) {
    // The picker reports UTC days; the caller's formatter keeps the day it reported.
    val shown = date?.let(formatDate) ?: noDate
    AppTextButton(
        onClick = onClick,
        modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
    ) {
        Text("$label: $shown")
    }
}
