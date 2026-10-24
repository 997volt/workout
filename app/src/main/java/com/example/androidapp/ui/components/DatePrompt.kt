package com.example.androidapp.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * One calendar, for one day (B217).
 *
 * Both places that ask for a day — the statistics custom range and the measurement edit form — read the
 * picker's millis as a **calendar day at UTC midnight**: reading them in the device's zone shifts the day for
 * anyone west of Greenwich, so "From 1 August" would mean 31 July. The second caller is where this came out of
 * `StatisticsScreen`; the two differ only in the confirmation's label and tag, and in whether a last day is
 * selectable.
 *
 * [latestDay] bounds the calendar for a record of the past (B212); null leaves every day selectable, which is
 * what a range's ends allow — a window may start or end on any date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePrompt(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    confirmLabel: String? = null,
    confirmTestTag: String? = null,
    latestDay: LocalDate? = null,
) {
    val bound = latestDay?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = remember(bound) {
            if (bound == null) {
                DatePickerDefaults.AllDates
            } else {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= bound
                }
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
                modifier = if (confirmTestTag == null) Modifier else Modifier.testTag(confirmTestTag),
            ) {
                Text(confirmLabel ?: stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
