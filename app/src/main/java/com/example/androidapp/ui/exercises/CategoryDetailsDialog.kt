package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * What a library family states for its movements (ROADMAP N117).
 *
 * A category head is never offered and never logged (N95), so its own facts are the one part of the library
 * with no screen of its own — and they are the facts that matter, because they are what a movement inherits
 * when it states nothing: **own-if-stated, else the category's** is N110's one winner rule, applied to the
 * pattern, the primary muscle and the secondary muscles alike. So this says what the family is contributing,
 * under the fields the exercise detail screen already uses, and a value the row does not state reads as
 * `exercise_detail_none` — the word that screen uses for null, which is *unstated* rather than *Other*.
 *
 * **Equipment is deliberately absent.** N110 makes it the one fact that never inherits, because a family spans
 * equipment — the lat pulldown family alone holds a cable, a bodyweight and a machine movement — so a dialog
 * showing one here would state an inheritance the model refuses.
 *
 * An `AlertDialog` rather than a destination: this is a peek at a row, and the exercise detail screen is built
 * for a movement — its fields are a lift's unit, step, rest and note, none of which a category can use.
 */
@Composable
internal fun CategoryDetailsDialog(
    exercise: Exercise,
    /** The movements the family holds, as the folded head's own line counts them (N95). */
    movementCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.exercise_detail_none)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.LIBRARY_CATEGORY_DETAILS),
        title = { Text(exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.library_exercises,
                        movementCount,
                        movementCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                CategoryFact(
                    label = stringResource(R.string.exercise_detail_pattern),
                    value = exercise.movementPattern?.label ?: none,
                )
                CategoryFact(
                    label = stringResource(R.string.exercise_detail_primary),
                    value = exercise.primaryMuscle?.label ?: none,
                )
                CategoryFact(
                    label = stringResource(R.string.exercise_detail_secondary),
                    value = exercise.secondaryMuscles
                        .joinToString(", ") { it.label }
                        .ifEmpty { none },
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/** One fact the family states: the field's name, then the value or the word for "states none". */
@Composable
private fun CategoryFact(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
