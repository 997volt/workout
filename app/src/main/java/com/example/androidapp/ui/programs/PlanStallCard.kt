package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
 * The plans that have stopped moving, with the way out (ROADMAP N237, N304, N310).
 *
 * The pattern is the exercise's: three sessions in which nothing moved, whatever the set or the reason. It
 * is a **suggestion on a card**, not a dialog and not a write. **Every** stalled lift is listed now
 * (amending N237's one-stall rule): each one is a decision the lifter can take, and hiding the second
 * behind the first made the card say less than it knew.
 *
 * Each lift's answer is a **plan edit**: "Deload this lift" (N307) drops this plan's later work sets and
 * three load steps, "Replace this lift" (N308) changes the movement. The week-level creator (N147) stays
 * the secondary link at the bottom — a lighter *week* is a different decision from a lighter *lift*.
 */
@Composable
internal fun PlanStallCard(
    stalls: List<PlanStall>,
    onDeloadLift: (PlanStall) -> Unit,
    onReplaceLift: (PlanStall) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stalls.forEachIndexed { index, stall ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            onClick = { onReplaceLift(stall) },
                            modifier = Modifier.testTag(TestTags.Programs.STALL_REPLACE_LIFT),
                        ) {
                            Text(stringResource(R.string.program_stall_replace_lift))
                        }
                        AppTextButton(
                            onClick = { onDeloadLift(stall) },
                            modifier = Modifier.testTag(TestTags.Programs.STALL_DELOAD_LIFT),
                        ) {
                            Text(stringResource(R.string.program_stall_deload_lift))
                        }
                    }
                }
                if (index < stalls.lastIndex) HorizontalDivider()
            }
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
