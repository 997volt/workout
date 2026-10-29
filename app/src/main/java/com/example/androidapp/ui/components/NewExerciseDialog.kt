package com.example.androidapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.Exercise

/**
 * Names a new custom exercise (ROADMAP N2).
 *
 * Creation asks for the name only — the taxonomy fields are non-nullable on disk,
 * so an unedited custom exercise is stored as unspecified and filled in later
 * from the detail screen. The hint says that rather than leaving the user to
 * wonder why there is one field.
 *
 * Add stays disabled until the name is non-blank: a nameless library row would be
 * unusable, and there is no sensible "unspecified" name to fall back to.
 */
@Composable
fun NewExerciseDialog(
    error: DataError?,
    onDismiss: () -> Unit,
    /** Called with the name and the family it was filed under, or null for none (ROADMAP N232). */
    onCreate: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What the name field opens on, or empty (ROADMAP N232).
     *
     * A search that found nothing is the one place the movement about to be made is already typed: "no
     * exercises match front squat" is a name, and asking for it twice is the price this removes.
     */
    initialName: String = "",
    /**
     * The families this movement can be filed under, or none (ROADMAP N232).
     *
     * **Filed while it is made**, because an unfiled movement is the shape the library exists to clean up
     * (N229, N231): creating more of them in silence is the wrong direction, and the field is one choice the
     * dialog can offer before the row exists.
     */
    families: List<Exercise> = emptyList(),
    /**
     * What is being made, and the two tags the fields answer to (ROADMAP N95).
     *
     * A category is made by the same gesture as a custom movement — one name — so it is the same dialog with
     * its own words, rather than a second dialog that would drift from this one's validation.
     */
    title: String = stringResource(R.string.exercise_new),
    hint: String = stringResource(R.string.exercise_new_hint),
    testTags: NewRowTags = NewRowTags(TestTags.NEW_EXERCISE_NAME, TestTags.NEW_EXERCISE_SAVE),
) {
    // Saveable, so rotating the device mid-type does not throw the name away.
    var name by rememberSaveable { mutableStateOf(initialName) }
    var familyId by rememberSaveable { mutableStateOf<String?>(null) }
    val trimmed = name.trim()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().testTag(testTags.name),
                    singleLine = true,
                    label = { Text(stringResource(R.string.exercise_name_label)) },
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (families.isNotEmpty()) {
                    FamilyChoice(families = families, chosen = familyId, onChoose = { familyId = it })
                }
                error?.let { failure ->
                    Text(
                        text = dataErrorMessage(failure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                modifier = Modifier.testTag(testTags.save),
                enabled = trimmed.isNotEmpty(),
                onClick = { onCreate(trimmed, familyId) },
            ) {
                Text(stringResource(R.string.exercise_new_add))
            }
        },
        dismissButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.NEW_EXERCISE_CANCEL),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * The two tags one naming dialog applies (ROADMAP N95).
 *
 * Paired rather than passed one at a time so a screen cannot tag its field and its save from two different
 * rows — which would leave one of them addressable and the other not.
 */
data class NewRowTags(val name: String, val save: String)

/**
 * Which family the new movement is filed under, or none (ROADMAP N232).
 *
 * Two rows of chips rather than a dropdown: the families are the library's own short list, and a chosen family
 * is a fact the dialog should show rather than hide behind a field. "No family" is a real answer and is the
 * default, because a movement in a hurry is still a movement.
 */
@Composable
private fun FamilyChoice(
    families: List<Exercise>,
    chosen: String?,
    onChoose: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.exercise_new_family),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = chosen == null,
                onClick = { onChoose(null) },
                label = { Text(stringResource(R.string.exercise_new_family_none)) },
                modifier = Modifier.testTag(TestTags.NEW_EXERCISE_FAMILY_NONE),
            )
            families.forEach { family ->
                FilterChip(
                    selected = chosen == family.id,
                    onClick = { onChoose(family.id) },
                    label = { Text(family.name) },
                    modifier = Modifier.testTag(TestTags.newExerciseFamily(family.id)),
                )
            }
        }
    }
}
