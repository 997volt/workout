package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * What a weight trend implies about eating, and the rate it is measured against (ROADMAP N98).
 *
 * Shown for the weight series only, which is the one metric with a meaningful *rate*. Deliberately outside the
 * collapsed readings rather than inside them: this is a summary *about* the series — the family the average and
 * the trend belong to — and one that had to be expanded to be read would be hidden behind the thing it exists
 * to act on.
 *
 * The number is a **relative** adjustment and the note under it says so, because the absolute target needs
 * intake and this app stores none. A rate that has not been set says nothing at all beyond the row that sets
 * it: an adjustment with no target to be short of has no meaning.
 */
@Composable
fun EnergyAdjustmentSection(
    series: MetricSeries?,
    metric: MetricEntry,
    rateTarget: Double?,
    onSetRateTarget: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (metric.key != MetricKey.Body(BodyMetric.WEIGHT)) return
    // The field's text belongs to the dialog, which is the thing that validates it — the rule GoalRow keeps.
    var editing by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().testTag(TestTags.Statistics.ENERGY),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.statistics_energy_target),
                style = MaterialTheme.typography.bodyMedium,
            )
            AppTextButton(
                onClick = { editing = true },
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_TARGET_SET),
            ) {
                Text(
                    text = rateTarget?.let { rate ->
                        stringResource(
                            R.string.statistics_per_week,
                            slopeText(rate) { metric.unit.formatRate(it) },
                            stringResource(metric.unit.labelRes),
                        )
                    } ?: stringResource(R.string.statistics_energy_target_none),
                )
            }
        }

        energyStatement(series = series, rateTarget = rateTarget)?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_STATEMENT),
            )
        }

        Text(
            text = stringResource(R.string.statistics_energy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editing) {
        RateDialog(
            metric = metric,
            rate = rateTarget,
            onSet = { value ->
                onSetRateTarget(value)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

/**
 * The sentence, or null when there is nothing honest to say.
 *
 * Every branch is an answer rather than a gap: a rate that was never set, and a series too short to fit, both
 * have something to *say* — and the second says it rather than drawing a number, which is the floor N98 sets.
 */
@Composable
private fun energyStatement(series: MetricSeries?, rateTarget: Double?): String? {
    val adjustment = if (rateTarget != null && series != null) {
        series.energyAdjustment(rateTarget)
    } else {
        null
    }
    return when {
        rateTarget == null || series == null -> null
        // A series too short to fit has something to *say*, and says it rather than drawing a number. That is
        // the floor N98 sets, and an absent row would leave a gap where an answer belongs.
        adjustment == null -> stringResource(R.string.statistics_energy_waiting)
        adjustment.isOnTarget -> stringResource(R.string.statistics_energy_on_target)
        else -> {
            // The band as the range it is. A fit that could measure its own scatter reports two numbers, and
            // one that could not reports the same number twice — a range of one, not a point hiding.
            val magnitude = adjustment.magnitude
            val band = if (magnitude.first == magnitude.last) {
                magnitude.first.toString()
            } else {
                "${magnitude.first}–${magnitude.last}"
            }
            stringResource(
                if (adjustment.eatsMore) {
                    R.string.statistics_energy_more
                } else {
                    R.string.statistics_energy_less
                },
                band,
            )
        }
    }
}

/**
 * The dialog a rate is typed into (ROADMAP N98).
 *
 * Seeded with [editableRate] rather than the display form, because the display uses a typographic minus that no
 * parser reads back — a dialog seeded with it would refuse to save the value it was shown.
 */
@Composable
private fun RateDialog(metric: MetricEntry, rate: Double?, onSet: (Double?) -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf(rate?.let { editableRate(it, metric.unit) }.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.statistics_energy_target)) },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.statistics_energy_target_hint)) },
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_TARGET_FIELD),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    // Unreadable keeps the field rather than setting a zero, the rule GoalDialog follows.
                    metric.unit.parseRate(typed)?.let(onSet)
                },
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_TARGET_CONFIRM),
            ) {
                Text(stringResource(R.string.statistics_goal_confirm))
            }
        },
        dismissButton = {
            if (rate != null) {
                AppTextButton(
                    onClick = { onSet(null) },
                    modifier = Modifier.testTag(TestTags.Statistics.ENERGY_TARGET_CLEAR),
                ) {
                    Text(stringResource(R.string.statistics_goal_clear))
                }
            }
        },
    )
}
