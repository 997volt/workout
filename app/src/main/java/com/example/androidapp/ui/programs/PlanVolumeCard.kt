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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.VolumeReason
import com.example.androidapp.domain.model.VolumeSuggestion
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The plan's earned sets, on the plan screen (ROADMAP N311).
 *
 * The finish offers the strongest one at *Done*; this card carries the rest while the rule holds — the
 * plan's own count, the reason, and one *Add set* per suggestion. It is a suggestion, not a write: the
 * Add is the lifter's, and it is undoable.
 */
@Composable
internal fun PlanVolumeCard(
    suggestions: List<VolumeSuggestion>,
    onAdd: (VolumeSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TestTags.Programs.VOLUME),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.volume_card_title),
                style = MaterialTheme.typography.titleSmall,
            )
            suggestions.forEachIndexed { index, suggestion ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = suggestion.exerciseName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when (suggestion.reason) {
                            VolumeReason.EXTRA_SET -> stringResource(
                                R.string.volume_extra_message,
                                suggestion.exerciseName,
                                suggestion.plannedSets,
                            )

                            VolumeReason.COMFORTABLE -> stringResource(
                                R.string.volume_comfortable_message,
                                suggestion.exerciseName,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppTextButton(
                            onClick = { onAdd(suggestion) },
                            modifier = Modifier.testTag(TestTags.Programs.VOLUME_ADD),
                        ) {
                            Text(stringResource(R.string.volume_add))
                        }
                    }
                }
                if (index < suggestions.lastIndex) HorizontalDivider()
            }
        }
    }
}
