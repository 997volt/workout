package com.example.androidapp.ui.programs

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
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * One plan that has stopped moving, with the way out (ROADMAP N237, N304).
 *
 * The app can already say what each set missed; this is the first thing it says about a pattern, and the pattern
 * is the exercise's: three sessions in which nothing moved, whatever the set or the reason. It is a **suggestion
 * on a card**, not a dialog and not a write: three sessions is enough to ask, and far from enough to decide for a
 * lifter that a week of their training should change.
 *
 * The answer is a **plan edit**: "Deload this lift" (N307) drops this plan's later work sets and three load
 * steps. The week-level creator (N147) is the other answer and stays as the secondary link — a lighter *week*
 * is a different decision from a lighter *lift*, and the copy keeps the two apart.
 */
@Composable
internal fun PlanStallCard(
    stall: PlanStall,
    onDeloadLift: () -> Unit,
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
                    R.plurals.program_stall_sessions,
                    stall.sessions,
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
                AppTextButton(
                    onClick = onDeloadLift,
                    modifier = Modifier.testTag(TestTags.Programs.STALL_DELOAD_LIFT),
                ) {
                    Text(stringResource(R.string.program_stall_deload_lift))
                }
            }
        }
    }
}
