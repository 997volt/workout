package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.ui.components.NewExerciseDialog
import com.example.androidapp.ui.components.NewRowTags
import com.example.androidapp.ui.components.TestTags

/**
 * The library's one action, and the two things it can make (ROADMAP N95, N232).
 *
 * Making a movement used to live in the workout picker alone (N2, B94), on the rule that the gap is felt
 * mid-session — but planning a lift you have never trained is the other place it is felt, and there the
 * library is the screen in front of you. So the action is a menu: a **movement**, which is the library's own
 * content, and the **family** it has always made.
 */
@Composable
internal fun LibraryCreationButton(
    onNewMovement: () -> Unit,
    /** Makes a family, or null where this screen offers none (the picker draws this same list). */
    onNewCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        ExtendedFloatingActionButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.LIBRARY_CREATE),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.library_create))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercise_new_movement)) },
                onClick = {
                    open = false
                    onNewMovement()
                },
                modifier = Modifier.testTag(TestTags.LIBRARY_NEW_EXERCISE),
            )
            onNewCategory?.let { create ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exercise_new_category)) },
                    onClick = {
                        open = false
                        create()
                    },
                    modifier = Modifier.testTag(TestTags.LIBRARY_NEW_CATEGORY),
                )
            }
        }
    }
}

/**
 * Names the movement the library is about to make, and files it while it is made (ROADMAP N232).
 *
 * The name field opens on whatever the search had in it: a query that found nothing *is* the movement's name,
 * and typing "front squat" a second time is the price this removes. A family is offered because an unfiled
 * movement is the shape the library's filters and its cleaning-up exist for (N229, N231) — creating more of
 * them in silence is the wrong direction.
 */
@Composable
internal fun NewMovementDialog(
    /**
     * The name to open on, or null when there is no dialog to show (N232).
     *
     * Null rather than a separate flag: the empty state's "create exactly what was typed" and the action's
     * "new movement" are the same dialog, and the only thing that differs is what the field starts with.
     */
    initialName: String?,
    families: List<Exercise>,
    error: DataError?,
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    val name = initialName ?: return
    NewExerciseDialog(
        error = error,
        onDismiss = onDismiss,
        onCreate = onCreate,
        initialName = name,
        families = families,
        title = stringResource(R.string.exercise_new_movement),
        hint = stringResource(R.string.exercise_new_hint),
        testTags = NewRowTags(TestTags.NEW_MOVEMENT_NAME, TestTags.NEW_MOVEMENT_SAVE),
    )
}
