package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Rpe

/**
 * Why the next set is shown these numbers (P3.17): what the lift did last time, and what the plan asks.
 *
 * A sentence, not a prescription: both halves are sources the prefill already read, and the line exists so
 * the lifter can tell a number remembered from a number planned. It is omitted when neither source speaks.
 *
 * Beside [WorkoutExerciseSection.kt]'s exercise block rather than inside it: that file is at the function
 * ceiling this project enforces, and the line is a self-contained sentence over [SuggestionReason].
 */
@Composable
internal fun SuggestionReasonLine(
    reason: SuggestionReason,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val unitLabel = stringResource(
        if (unit == WeightUnit.KILOGRAMS) R.string.suggestion_unit_kg else R.string.suggestion_unit_lb,
    )
    val parts = mutableListOf<String>()
    if (reason.lastReps != null) {
        var last = stringResource(
            R.string.suggestion_set_shape,
            reason.lastReps,
            weightWithUnit(reason.lastWeightGrams, unit, unitLabel),
        )
        reason.lastRpeHalves?.let { last += stringResource(R.string.suggestion_rpe, Rpe.format(it)) }
        parts += stringResource(R.string.suggestion_last_time, last)
    }
    if (reason.planReps != null || reason.planWeightGrams != null) {
        val weight = weightWithUnit(reason.planWeightGrams, unit, unitLabel)
        parts += stringResource(R.string.suggestion_plan, planReasonPart(reason, weight))
    }
    Text(
        text = parts.joinToString(stringResource(R.string.suggestion_separator)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}

/**
 * The plan's half of the reason line: its authored range where it wrote one, its current place otherwise.
 *
 * Split out so the line stays under the complexity ceiling. The range wins over the current place because
 * the field above already states the latter: a 5–10 set that a load step restarted at 5 used to read
 * "Plan 5 reps @ 102.5 kg", hiding the ceiling the lifter had just reached (B478).
 */
@Composable
private fun planReasonPart(reason: SuggestionReason, weight: String): String {
    val range = reason.planRepsMin?.let { min ->
        reason.planRepsMax?.takeIf { max -> min < max }?.let { max -> min to max }
    }
    var plan = when {
        range != null && reason.planWeightGrams != null -> pluralStringResource(
            R.plurals.suggestion_rep_range_at,
            range.second,
            range.first,
            range.second,
            weight,
        )

        range != null -> pluralStringResource(
            R.plurals.suggestion_rep_range,
            range.second,
            range.first,
            range.second,
        )

        reason.planReps != null && reason.planWeightGrams != null -> pluralStringResource(
            R.plurals.suggestion_reps_at,
            reason.planReps,
            reason.planReps,
            weight,
        )

        reason.planReps != null -> pluralStringResource(
            R.plurals.suggestion_reps,
            reason.planReps,
            reason.planReps,
        )
        else -> weight
    }
    reason.planRpeHalves?.let { plan += stringResource(R.string.suggestion_rpe, Rpe.format(it)) }
    return plan
}

/** A weight and its unit, or an empty string when the source named none. */
private fun weightWithUnit(grams: Long?, unit: WeightUnit, label: String): String =
    grams?.let { Weight.format(it, unit) + " " + label }.orEmpty()
