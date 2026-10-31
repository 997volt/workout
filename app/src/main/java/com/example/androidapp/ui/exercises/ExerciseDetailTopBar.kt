package com.example.androidapp.ui.exercises

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseDetailTopBar(
    state: ExerciseDetailUiState,
    onBack: () -> Unit,
    onOpenTrends: (String) -> Unit,
    onEdit: () -> Unit,
    onMerge: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(state.exercise?.name ?: stringResource(R.string.exercise_detail_title))
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.exercise_detail_back),
                )
            }
        },
        actions = {
            // The lift's own trends (ROADMAP N17): the narrow question a lifter
            // asks is about one movement, not about everything at once.
            state.exercise?.let { exercise ->
                AppTextButton(
                    onClick = { onOpenTrends(exercise.id) },
                    modifier = Modifier.testTag(TestTags.EXERCISE_TRENDS),
                ) {
                    Text(stringResource(R.string.exercise_trends_open_short))
                }
            }
            // Only a custom exercise is editable here (ROADMAP N2), and the
            // action disappears while the form is already open.
            if (state.canEdit) {
                AppTextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag(TestTags.EXERCISE_EDIT),
                ) {
                    Text(stringResource(R.string.exercise_edit))
                }
            }
            // A movement can be merged into another one (N245), the deliberate twin of the repair that
            // leaves a row the lifter configured alone. A category has no history to move.
            state.exercise?.takeIf { it.rowKind == RowKind.MOVEMENT && !state.isEditing }?.let {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag(TestTags.EXERCISE_MORE),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.exercise_more),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.exercise_merge_action)) },
                        onClick = {
                            menuOpen = false
                            onMerge()
                        },
                        modifier = Modifier.testTag(TestTags.EXERCISE_MERGE),
                    )
                }
            }
        },
    )
}
