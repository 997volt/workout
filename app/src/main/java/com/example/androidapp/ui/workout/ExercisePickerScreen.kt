package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.exercises.EmptyState
import com.example.androidapp.ui.exercises.ExerciseList
import com.example.androidapp.ui.exercises.LibrarySearchField
import com.example.androidapp.ui.exercises.LoadingState

/**
 * The exercise picker: the library, filtered, with a way to add a movement mid-workout (ROADMAP N2).
 *
 * Its own screen rather than the library's, because the two hold different things: the library keeps the
 * whole library and groups it on screen, since which families are open is screen state, while a picker has
 * nothing to fold and holds a flat, movements-only list. What they genuinely share — the search box, the
 * empty states, the row and its fold — is shared as composables rather than by one screen serving two
 * state shapes, which is what let the two drift apart the last time they were one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    state: ExercisePickerUiState,
    onQueryChange: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    onNewExercise: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (onNewExercise != null) {
                ExtendedFloatingActionButton(
                    onClick = onNewExercise,
                    text = { Text(stringResource(R.string.exercise_new)) },
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    modifier = Modifier.testTag(TestTags.LIBRARY_NEW_EXERCISE),
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.picker_title), testTag = TestTags.LIBRARY_TITLE) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LibrarySearchField(query = state.query, onQueryChange = onQueryChange)

            when {
                state.isLoading -> LoadingState()

                state.error != null -> CenteredMessage(
                    text = dataErrorMessage(state.error),
                    modifier = Modifier.testTag(TestTags.LIBRARY_READ_ERROR),
                )

                state.isEmpty -> EmptyState(query = state.query, libraryIsEmpty = state.libraryIsEmpty)

                else -> ExerciseList(
                    items = state.items,
                    // A picker shows no families, so nothing here is a head and nothing folds (N95).
                    isExpanded = { true },
                    onToggle = {},
                    onExerciseClick = onExerciseClick,
                )
            }
        }
    }
}
