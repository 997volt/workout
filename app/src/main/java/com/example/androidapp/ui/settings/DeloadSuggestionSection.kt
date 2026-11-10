package com.example.androidapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.TestTags

/**
 * The recovery-week suggestion's cadence (ROADMAP N301).
 *
 * Off is the default: a suggestion the lifter did not ask for is the app inventing a coaching
 * rhythm. The choices are a few honest cadences rather than a free number, because the app has no
 * recovery model to defend an arbitrary one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeloadSuggestionSection(
    state: SettingsUiState,
    onSetDeloadSuggestion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_deload_suggestion),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_deload_suggestion_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsRepository.DELOAD_SUGGESTION_WEEKS.sorted().forEach { weeks ->
                FilterChip(
                    selected = state.deloadSuggestionWeeks == weeks,
                    onClick = { onSetDeloadSuggestion(weeks) },
                    label = {
                        Text(
                            if (weeks == 0) {
                                stringResource(R.string.settings_deload_off)
                            } else {
                                pluralStringResource(
                                    R.plurals.settings_deload_every_weeks,
                                    weeks,
                                    weeks,
                                )
                            },
                        )
                    },
                    modifier = Modifier.testTag(TestTags.settingsDeloadSuggestion(weeks)),
                )
            }
        }
    }
}

