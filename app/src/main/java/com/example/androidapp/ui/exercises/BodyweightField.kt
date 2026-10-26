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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    ExerciseFlagField(
        label = stringResource(R.string.exercise_detail_bodyweight),
        hint = stringResource(R.string.exercise_detail_bodyweight_hint),
        checked = checked,
        onCheckedChange = onCheckedChange,
        testTag = TestTags.EXERCISE_EDIT_BODYWEIGHT,
        modifier = modifier,
    )
}

/**
 * Whether a set of the movement is **held** rather than counted (ROADMAP N220).
 *
 * The second flag row, and the point at which the layout was extracted: two switches with a label and a
 * sentence are one component, not two copies that drift.
 */
@Composable
fun TimedField(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExerciseFlagField(
        label = stringResource(R.string.exercise_detail_timed),
        hint = stringResource(R.string.exercise_detail_timed_hint),
        checked = checked,
        onCheckedChange = onCheckedChange,
        testTag = TestTags.EXERCISE_EDIT_TIMED,
        modifier = modifier,
    )
}

/** A labelled switch with one sentence saying what it changes. */
@Composable
private fun ExerciseFlagField(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            // The label is beside it, not on it: without this a screen reader announces "Switch, off" and
            // leaves which flag to be guessed (B252).
            modifier = Modifier
                .testTag(testTag)
                .semantics { contentDescription = label },
        )
    }
}
