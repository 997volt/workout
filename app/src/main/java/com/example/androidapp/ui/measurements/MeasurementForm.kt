package com.example.androidapp.ui.measurements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.withDay
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.DatePrompt
import com.example.androidapp.ui.components.TestTags
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The day an edited record belongs to, and the one operation the form applies to it (B212, B214).
 *
 * A state holder rather than a `var` in the dialog so the path the calendar feeds — a picked day becomes the
 * draft's day, keeping the reading's hour — can be asserted without driving a Material calendar under
 * Robolectric. The draft is built from [measuredAt], so this is the whole of the pick → save path that is not
 * Compose.
 */
internal class MeasurementDayState(initial: Instant?, private val zone: ZoneId) {
    /** The record's instant; the day is read from it in [zone], as it is everywhere else. */
    var measuredAt by mutableStateOf(initial ?: Instant.now())
        private set

    /** The day the record now belongs to, in [zone]. */
    val day: LocalDate get() = measuredAt.atZone(zone).toLocalDate()

    /** Moves the record to [picked], keeping the hour it was taken at. */
    fun pick(picked: LocalDate) {
        measuredAt = measuredAt.withDay(picked, zone)
    }
}

/**
 * What one record's form shows, and the day it belongs to (ROADMAP N113; the day is B212).
 *
 * Split from the dialog rather than inlined: the dialog holds the form's state and its two buttons, and the
 * body here is the layout — a record's day when one is being corrected, then the numbers its kind states.
 * The day is a fact *about* the record rather than one of its numbers, so it reads above them: a date sitting
 * among the fields that ask for a weight is read as part of the reading, and this is a correction of which
 * reading it is.
 *
 * **The day is offered only when [day] is given**, which the dialog passes for a record that exists: a new
 * entry belongs to today, and the form that corrects a record is the one that can say it was not today after
 * all.
 *
 * [day] is a **calendar day in the reader's zone**, not the reading's instant: a record's day is the local day
 * it was taken on (`BodyMeasurement.localDayBounds`), so an entry saved at 23:30 in Berlin belongs to that day
 * even though its instant is already the next one in UTC — and a picker seeded from the instant would open on
 * the wrong day for every reader west of UTC.
 */
@Composable
internal fun MeasurementFormBody(
    kind: MeasurementKind,
    day: LocalDate?,
    dayText: String,
    onPickDay: (LocalDate) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    bodyFat: String,
    onBodyFatChange: (String) -> Unit,
    muscle: String,
    onMuscleChange: (String) -> Unit,
    soreMuscles: List<SoreMuscle>,
    onSoreMusclesChange: (List<SoreMuscle>) -> Unit,
    tape: Map<TapeSite, String>,
    onTapeChange: (TapeSite, String) -> Unit,
) {
    // The calendar is a screen-sized thing that happens to be nested in the form, so it is this body's own
    // state: the dialog above knows the day, not whether it is picking one.
    var picking by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (day != null) {
            MeasurementDayRow(dayText) { picking = true }
        }
        MeasurementFields(
            kind = kind,
            weight = weight,
            onWeightChange = onWeightChange,
            bodyFat = bodyFat,
            onBodyFatChange = onBodyFatChange,
            muscle = muscle,
            onMuscleChange = onMuscleChange,
            soreMuscles = soreMuscles,
            onSoreMusclesChange = onSoreMusclesChange,
            tape = tape,
            onTapeChange = onTapeChange,
        )
    }

    if (picking && day != null) {
        // A record is of the past (B212), so the calendar offers today and every day before it. The calendar
        // itself is shared with the statistics range (B217).
        DatePrompt(
            initial = day,
            onDismiss = { picking = false },
            onPick = { picked ->
                onPickDay(picked)
                picking = false
            },
            confirmLabel = stringResource(R.string.measurements_date_confirm),
            confirmTestTag = TestTags.Measurements.EDIT_DATE_CONFIRM,
            latestDay = LocalDate.now(ZoneId.systemDefault()),
        )
    }
}

/**
 * The numbers a record states, which follow from its kind (N113).
 *
 * One `when` per kind, in its own function so the body above stays a layout rather than a list of fields.
 */
@Composable
private fun MeasurementFields(
    kind: MeasurementKind,
    weight: String,
    onWeightChange: (String) -> Unit,
    bodyFat: String,
    onBodyFatChange: (String) -> Unit,
    muscle: String,
    onMuscleChange: (String) -> Unit,
    soreMuscles: List<SoreMuscle>,
    onSoreMusclesChange: (List<SoreMuscle>) -> Unit,
    tape: Map<TapeSite, String>,
    onTapeChange: (TapeSite, String) -> Unit,
) {
    when (kind) {
        MeasurementKind.DAILY -> DailyFields(
            weight = weight,
            onWeightChange = onWeightChange,
            bodyFat = bodyFat,
            onBodyFatChange = onBodyFatChange,
            muscle = muscle,
            onMuscleChange = onMuscleChange,
            soreMuscles = soreMuscles,
            onSoreMusclesChange = onSoreMusclesChange,
        )

        MeasurementKind.TAPE -> TapeFields(tape = tape, onTapeChange = onTapeChange)
    }
}

/** The day an edited measurement belongs to, and the way to change it. */
@Composable
private fun MeasurementDayRow(day: String, onChange: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.Measurements.EDIT_DATE),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.measurements_date_label),
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day)
            AppTextButton(
                onClick = onChange,
                modifier = Modifier.testTag(TestTags.Measurements.EDIT_DATE_CHANGE),
            ) {
                Text(stringResource(R.string.measurements_date_change))
            }
        }
    }
}
