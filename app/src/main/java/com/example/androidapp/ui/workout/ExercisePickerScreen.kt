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
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.rememberRowFold
import com.example.androidapp.ui.exercises.EmptyState
import com.example.androidapp.ui.exercises.ExerciseList
import com.example.androidapp.ui.exercises.LibrarySearchField
import com.example.androidapp.ui.exercises.LoadingState

/**
 * The exercise picker: the library, grouped and filtered, with a way to add a movement mid-workout (ROADMAP N2).
 *
 * **It draws the library's shape** (N95, N110): a category head with the movements filed under it, indented,
 * and the movements filed under nothing at the top level — so a lifter who keeps *Barbell Bench Press*,
 * *Dumbbell Bench Press* and the paused lifts in one family browses them together here too, rather than
 * scrolling a hundred movements to find one. What differs is the answer to a tap: a head **folds**, and only
 * a movement can be chosen, so N95's "a category is never offered and never logged" is enforced by the list
 * rather than by leaving the heads out.
 *
 * A search opens what it reaches into, so a query never answers with a folded family (N95) — the rule lives in
 * `libraryRows`, because the library's list wants it too.
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

    // Which families are open is screen state, exactly as in the library (N95): no `configChanges`, so a
    // rotation would otherwise fold what the lifter opened. Every family starts folded, so the picker opens
    // as the families it is rather than as a wall of movements.
    val fold = rememberRowFold(ids = state.exercises.map { it.id }, newRowsOpen = false)
    val rows = remember(state.exercises, state.query, fold.foldedIds) {
        libraryRows(
            exercises = state.exercises,
            query = state.query,
            // A query opens what it draws, which `libraryRows` owns (N95) — both grouped screens want it.
            foldedCategories = fold.foldedIds,
        )
    }

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
                    items = rows,
                    // A head folds and is never chosen; only a movement reaches the choice (N95).
                    onToggle = fold::toggle,
                    onExerciseClick = onExerciseClick,
                )
            }
        }
    }
}
