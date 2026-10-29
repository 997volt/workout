package com.example.androidapp.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.TestTags

/**
 * How *Done* answers for this movement, or *App default* (ROADMAP N235).
 *
 * Four chips rather than the settings screen's three, because null is a real answer here — follow the app
 * setting — and a three-way control would have to express it as one of the answers, which is what an override
 * is not. The chips are in the order the question is asked: the app's answer first, then the three.
 *
 * **A movement's own answer is training data, not a preference**: it decides what the app writes into a plan,
 * which is why it lives on the movement and survives a restore while the device setting does not.
 */
@Composable
fun ProgressionModeField(
    selected: ProgressionMode?,
    onSelect: (ProgressionMode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exercise_detail_progression),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.exercise_detail_progression_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppFilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = stringResource(R.string.exercise_progression_default),
                testTag = TestTags.EXERCISE_EDIT_PROGRESSION_DEFAULT,
            )
            ProgressionMode.entries.forEach { mode ->
                AppFilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = stringResource(mode.editChipLabel()),
                    testTag = mode.editChipTag(),
                )
            }
        }
    }
}

/** What one answer is called where it is picked, in this screen's words (N235). */
@StringRes
private fun ProgressionMode.editChipLabel(): Int = when (this) {
    ProgressionMode.SUGGEST -> R.string.settings_progression_suggest
    ProgressionMode.APPLY_AT_DONE -> R.string.settings_progression_apply
    ProgressionMode.OFF -> R.string.settings_progression_off
}

/** One tag per answer, so a test can pick one by name rather than by position. */
private fun ProgressionMode.editChipTag(): String = when (this) {
    ProgressionMode.SUGGEST -> TestTags.EXERCISE_EDIT_PROGRESSION_SUGGEST
    ProgressionMode.APPLY_AT_DONE -> TestTags.EXERCISE_EDIT_PROGRESSION_APPLY
    ProgressionMode.OFF -> TestTags.EXERCISE_EDIT_PROGRESSION_OFF
}
