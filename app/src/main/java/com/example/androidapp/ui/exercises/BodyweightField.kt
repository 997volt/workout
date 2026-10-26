package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.TestTags

/**
 * Whether the movement is loaded by the body (ROADMAP N219).
 *
 * Offered once, in the library's editor, rather than on every set: a pull-up is a pull-up on every set, and
 * asking each time would be a question with the same answer. What it changes is what the weight field means —
 * the *added* load — and what the set stores, which is the lifter's own weight in force when it was logged.
 *
 * Its own file because the exercise editor is at the length this project allows, and because this is the one
 * place that states the property and the one sentence that explains it.
 */
@Composable
fun BodyweightField(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.exercise_detail_bodyweight),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.exercise_detail_bodyweight_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(TestTags.EXERCISE_EDIT_BODYWEIGHT),
        )
    }
}
