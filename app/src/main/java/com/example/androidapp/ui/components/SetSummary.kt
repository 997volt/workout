package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DurationFormat
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.loadGrams

/**
 * What one logged set reads as: load, unit, and how many or how long (ROADMAP N219, N220).
 *
 * One composable because the workout screen and the history screen state the same sentence, and the two
 * branches a set has — a hold instead of reps, a bodyweight load instead of an added one — multiply into
 * four strings that must not drift apart (B245).
 *
 * **A bodyweight set reads at its load**, not at what was added: 75 kg of lifter with a 20 kg belt is
 * `95 kg × 8 · BW`, which is the figure the volume, the trend and the summary all count. The marker is
 * what keeps the row from pretending the belt alone was lifted — the weight *field* is the added part,
 * and it says so where it is typed.
 */
@Composable
internal fun setSummaryText(
    weightGrams: Long,
    assistanceGrams: Long,
    bodyweightGrams: Long?,
    durationSeconds: Int?,
    reps: Int,
    unit: WeightUnit,
    /** True to state the hold in words, for a screen reader (B252): "1 minute 30 seconds". */
    spokenDuration: Boolean = false,
): String {
    val bodyweight = bodyweightGrams != null
    // The same rule the totals use (N219): a bodyweight set's load is the lifter plus what was added,
    // less what the machine took off, and the assistance is already inside that number.
    val load = if (bodyweight) {
        loadGrams(weightGrams, bodyweightGrams, assistanceGrams)
    } else {
        weightGrams
    }
    val shownAssistance = if (bodyweight) 0L else assistanceGrams
    val weight = Weight.display(load, shownAssistance, unit)
    val label = unit.label()
    val hold = durationSeconds?.let {
        if (spokenDuration) DurationFormat.spokenSeconds(it.toLong()) else DurationFormat.ofSeconds(it.toLong())
    }
    return when {
        durationSeconds != null && bodyweight -> stringResource(
            R.string.set_summary_timed_bodyweight,
            weight,
            label,
            hold.orEmpty(),
        )

        durationSeconds != null -> stringResource(
            R.string.set_summary_timed,
            weight,
            label,
            hold.orEmpty(),
        )

        bodyweight -> stringResource(R.string.set_summary_bodyweight, weight, label, reps)
        else -> stringResource(R.string.set_summary, weight, label, reps)
    }
}
