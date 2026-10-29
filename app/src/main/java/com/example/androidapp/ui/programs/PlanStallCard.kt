package com.example.androidapp.ui.programs

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgressionMiss
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * One plan that has stopped moving, with the way out (ROADMAP N237).
 *
 * The app can already say what each set missed; this is the first thing it says about a pattern. It is a
 * **suggestion on a card**, not a dialog and not a write: three sessions of the same miss is enough to ask, and
 * far from enough to decide for a lifter that a week of their training should change.
 *
 * The answer is the deload creator the program already has (N147) rather than a second idea of what a deload
 * is — a marked week with days and a kept percentage, authored in one place.
 */
@Composable
internal fun PlanStallCard(
    stall: PlanStall,
    onOpenDeload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TestTags.Programs.STALL),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = stall.exerciseName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = pluralStringResource(
                    stall.sentence(),
                    stall.sessions,
                    stall.setNumber,
                    stall.sessions,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(
                    onClick = onOpenDeload,
                    modifier = Modifier.testTag(TestTags.Programs.STALL_DELOAD),
                ) {
                    Text(stringResource(R.string.program_stall_deload))
                }
            }
        }
    }
}

/** What this kind of stall reads as, in the words a lifter would use about their own training (N237). */
@PluralsRes
private fun PlanStall.sentence(): Int = when (kind) {
    ProgressionMiss.REPS_SHORT -> R.plurals.program_stall_reps
    ProgressionMiss.OVER_TARGET_RPE -> R.plurals.program_stall_effort
    ProgressionMiss.LOAD_SHORT -> R.plurals.program_stall_load
    // A hold's own step (N242), and the only other kind the rule can count.
    else -> R.plurals.program_stall_hold
}
