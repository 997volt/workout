package com.example.androidapp.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import kotlin.math.abs

/**
 * One set's change from the same place last time, or nothing when it has no counterpart (N306).
 *
 * "same as last time" when there is one and nothing moved: the line never means two things, and a set with
 * no counterpart says nothing at all rather than claiming it matched.
 */
@Composable
internal fun SetDeltaLine(set: HistorySet, previous: PastSet?, modifier: Modifier = Modifier) {
    setDelta(set, previous)?.let { delta ->
        Text(
            text = if (delta.unchanged) {
                stringResource(R.string.history_same_as_last_time)
            } else {
                setDeltaText(delta, exerciseWeightUnit(set.weightUnit))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag(TestTags.HISTORY_SET_DELTA),
        )
    }
}

/**
 * The delta as one line (N306): the load in the set's own unit, then the reps or the hold, each with its
 * direction. The unit is the row's own (N64), so a pound-loading movement reads pounds.
 */
@Composable
private fun setDeltaText(delta: SetDelta, unit: WeightUnit): String {
    val unitLabel = unit.label()
    val weight = if (delta.weightGrams != 0L) {
        val magnitude = Weight.format(abs(delta.weightGrams), unit)
        (if (delta.weightGrams > 0) "+" else "-") + magnitude + " " + unitLabel
    } else {
        null
    }
    val reps = if (delta.reps != 0) {
        pluralStringResource(R.plurals.history_delta_reps, abs(delta.reps), signed(delta.reps))
    } else {
        null
    }
    val hold = delta.holdSeconds?.takeIf { it != 0 }?.let { change ->
        pluralStringResource(R.plurals.history_delta_seconds, abs(change), signed(change))
    }
    return listOfNotNull(weight, reps, hold).joinToString(" · ")
}

/** `+2`, `-1`: the number with the direction it moved, which is what the plural's sentence adds to. */
private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
