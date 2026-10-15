package com.example.androidapp.ui.measurements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.at
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneId

/**
 * One record read in full, and read-only (a request in use; no roadmap id covers it).
 *
 * The list's supporting line is a summary: `82.4 kg · 18.3%` does not say which number is which, and the
 * tape sites and the sore list read as bare numbers and bare words. Here every number carries the name of
 * what it measured, and **only what the record states is drawn** — an absent weight or an unmeasured site
 * is not a zero, so it is not a row either (N113).
 *
 * It is a look and nothing else. Correcting a reading is a different question (N132), so the one control
 * is Close and there is no field to change.
 */
@Composable
internal fun MeasurementDetailsDialog(entry: BodyMeasurement, onDismiss: () -> Unit) {
    val date = HistoryFormat.date(entry.measuredAt, zone = ZoneId.systemDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Measurements.DETAILS),
        title = {
            Column {
                Text(date, modifier = Modifier.testTag(TestTags.Measurements.DETAILS_TITLE))
                Text(
                    text = stringResource(kindLabel(entry.kind)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                measurementDetails(entry).forEach { (label, value) ->
                    DetailRow(label = label, value = value)
                }
                if (entry.soreMuscles.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.readiness_sore_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    entry.soreMuscles.forEach { sore ->
                        DetailRow(
                            label = sore.muscle.label,
                            value = stringResource(R.string.scored_pick_score, sore.score),
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Measurements.DETAILS_CLOSE),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * The numbers a record states, each under the name of what it measured.
 *
 * A tape site and the two percentages are one shape of fact, so they are gathered here and drawn by one
 * row each; an absent one is simply absent, because a reading nobody took is not a zero (N113).
 */
@Composable
private fun measurementDetails(entry: BodyMeasurement): List<Pair<String, String>> {
    val details = mutableListOf<Pair<String, String>>()
    entry.weightGrams?.let {
        details += stringResource(R.string.measurements_weight) to Weight.kilograms(it)
    }
    entry.bodyFatTenths?.let {
        details += stringResource(R.string.measurements_body_fat) to MeasurementFormat.percent(it)
    }
    entry.muscleTenths?.let {
        details += stringResource(R.string.measurements_muscle) to MeasurementFormat.percent(it)
    }
    TapeSite.entries.forEach { site ->
        entry.at(site)?.let {
            details += stringResource(tapeLabel(site)) to MeasurementFormat.centimetres(it)
        }
    }
    return details
}

/** One named number of a record: what was measured, and what it read (a request in use). */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
