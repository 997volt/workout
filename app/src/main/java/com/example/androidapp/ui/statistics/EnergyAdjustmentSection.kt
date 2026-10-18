package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.MAINTENANCE_CORRIDOR_GRAMS
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.ui.components.TestTags

/**
 * What a weight trend implies about eating, and the rate it is measured against (ROADMAP N98, N135).
 *
 * Shown for the weight series only, which is the one metric with a meaningful *rate*. Deliberately outside the
 * collapsed readings rather than inside them: this is a summary *about* the series — the family the average and
 * the trend belong to — and one that had to be expanded to be read would be hidden behind the thing it exists
 * to act on.
 *
 * **The statement reads the whole weight series, not the chart's window.** A verdict must not change with a
 * display setting (B141), and the default seven-day range would otherwise sit below N98's eight-readings-over-
 * 21-days floor, so the section could never speak. The chart and the readings stay windowed; only the verdict
 * moves.
 *
 * **The rate is derived, and this row shows it** (N135). It used to be a second number the lifter typed beside
 * the target, which is exactly the pair that could disagree; now the goal states what is wanted and the rate
 * follows from it and the weight the lifter is now, so there is nothing here to press — the goal row above is
 * where the number that sets it is chosen. A **maintenance** goal states no rate at all, and the row says the
 * corridor it holds instead of drawing a zero the first glass of water would break.
 *
 * The number is a **relative** adjustment and the note under it says so, because the absolute target needs
 * intake and this app stores none. A goal that is not set says nothing at all beyond the row that sets it: an
 * adjustment with no target to be short of has no meaning.
 */
@Composable
fun EnergyAdjustmentSection(
    series: MetricSeries?,
    metric: MetricEntry,
    weightGoal: WeightGoal?,
    rateTarget: Double?,
    modifier: Modifier = Modifier,
    rateBand: ClosedRange<Double>? = null,
) {
    if (metric.key != MetricKey.Body(BodyMetric.WEIGHT)) return

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
            Text(
                text = targetText(metric = metric, weightGoal = weightGoal, rateTarget = rateTarget),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_TARGET),
            )
        }

        // The target is the number; this is the range that says whether the trend is on it (N149).
        rateBand?.let { band ->
            Text(
                text = stringResource(
                    R.string.statistics_rate_band,
                    slopeText(band.start) { metric.unit.formatRate(it) },
                    slopeText(band.endInclusive) { metric.unit.formatRate(it) },
                    stringResource(metric.unit.labelRes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.Statistics.ENERGY_BAND),
            )
        }

        energyStatement(series = series, weightGoal = weightGoal, rateTarget = rateTarget)?.let { text ->
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
}

/**
 * The target as it reads: the derived rate, the corridor a maintenance goal holds, or the sentence that says
 * why neither can be drawn yet.
 *
 * A goal with no weigh-in behind it cannot have a rate — the rate is a share of the body, and there is no body
 * to take a share of — so it says so rather than showing a number the app made up.
 */
@Composable
private fun targetText(metric: MetricEntry, weightGoal: WeightGoal?, rateTarget: Double?): String = when {
    weightGoal == null -> stringResource(R.string.statistics_energy_target_none)
    weightGoal is WeightGoal.Maintain -> stringResource(
        R.string.statistics_weight_goal_corridor,
        metric.unit.format(weightGoal.weightGrams.toDouble()),
        metric.unit.format(MAINTENANCE_CORRIDOR_GRAMS.toDouble()),
    )

    rateTarget == null -> stringResource(R.string.statistics_energy_needs_weigh_in)
    else -> stringResource(
        R.string.statistics_per_week,
        slopeText(rateTarget) { metric.unit.formatRate(it) },
        stringResource(metric.unit.labelRes),
    )
}

/**
 * The sentence, or null when there is nothing honest to say.
 *
 * Every branch is an answer rather than a gap: a goal that was never set, and a series too short to fit, both
 * have something to *say* — and the second says it rather than drawing a number, which is the floor N98 sets.
 * A maintenance goal is judged **against its corridor** rather than against a rate of zero, which is what
 * makes it a goal at all (N135).
 */
@Composable
private fun energyStatement(series: MetricSeries?, weightGoal: WeightGoal?, rateTarget: Double?): String? {
    if (series == null || weightGoal == null) return null
    val adjustment = if (weightGoal is WeightGoal.Maintain) {
        series.energyAdjustmentToCorridor(weightGoal.weightGrams.toDouble())
    } else {
        rateTarget?.let { series.energyAdjustment(it) }
    }
    return when {
        // A series too short to fit has something to *say*, and says it rather than drawing a number. That is
        // the floor N98 sets, and an absent row would leave a gap where an answer belongs.
        adjustment == null -> stringResource(R.string.statistics_energy_waiting)
        adjustment.isOnTarget -> stringResource(R.string.statistics_energy_on_target)
        else -> stringResource(
            if (adjustment.eatsMore) {
                R.string.statistics_energy_more
            } else {
                R.string.statistics_energy_less
            },
            adjustmentBandText(adjustment.magnitude),
        )
    }
}

/**
 * The magnitude as the range it is.
 *
 * A fit that could measure its own scatter reports two numbers, and one that could not reports the same
 * number twice — a range of one, not a point hiding. `internal` because the weigh-in prompt (N136) says the
 * same sentence from the measurements screen, and two renderings of one band would be two answers.
 */
internal fun adjustmentBandText(magnitude: IntRange): String =
    if (magnitude.first == magnitude.last) {
        magnitude.first.toString()
    } else {
        "${magnitude.first}–${magnitude.last}"
    }
