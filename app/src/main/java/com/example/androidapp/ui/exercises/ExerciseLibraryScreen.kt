package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.theme.TileAccent

/**
 * Stateful entry point: wires the ViewModel to the stateless screen.
 *
 * Keeping the split means [ExerciseLibraryScreen] can be driven by a fixed
 * state in a UI test with no Hilt container and no repository.
 */
@Composable
fun ExerciseLibraryRoute(
    onExerciseClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ExerciseLibraryScreen(
        state = state,
        title = stringResource(R.string.exercise_library_title),
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onExerciseClick = onExerciseClick,
        modifier = modifier,
    )
}

/**
 * The library list.
 *
 * [onBack] and [onNewExercise] are optional so the same composable serves both
 * the standalone library destination and the in-workout exercise picker, which
 * differs in its title, what a tap does, and — for the picker — the offer to
 * create a new exercise (ROADMAP N2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    state: ExerciseLibraryUiState,
    title: String,
    onQueryChange: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
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
            // Only the picker offers creation: the library is a reference you
            // navigate to, while the gap is felt mid-workout (ROADMAP N2).
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
                title = {
                    TopBarTitle(text = title, testTag = TestTags.LIBRARY_TITLE)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.nav_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LibrarySearchField(query = state.query, onQueryChange = onQueryChange)

            when {
                state.isLoading -> LoadingState()

                // A failed read is shown here, where the list would have been (B4):
                // an empty list would misreport what is on the device.
                state.error != null -> CenteredMessage(
                    text = dataErrorMessage(state.error),
                    modifier = Modifier.testTag(TestTags.LIBRARY_READ_ERROR),
                )

                state.isEmpty -> EmptyState(
                    query = state.query,
                    libraryIsEmpty = state.libraryIsEmpty,
                )
                else -> ExerciseList(items = state.items, onExerciseClick = onExerciseClick)
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.exercise_library_loading),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun EmptyState(
    query: String,
    libraryIsEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    // An empty library and a search with no hits look the same unless the screen
    // distinguishes them, and the user's next move differs: one wants the search
    // cleared, the other is not about the search at all.
    if (libraryIsEmpty) {
        CenteredMessage(
            text = stringResource(R.string.exercise_library_no_exercises),
            hint = stringResource(R.string.exercise_library_no_exercises_hint),
            modifier = modifier.testTag(TestTags.LIBRARY_EMPTY_LIBRARY),
        )
    } else {
        CenteredMessage(
            text = stringResource(R.string.exercise_library_empty, query),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = modifier.testTag(TestTags.LIBRARY_NO_MATCH),
        )
    }
}

/**
 * The library's primary action (ROADMAP P1.16).
 *
 * Reads the clock — and is the only composable here that does, so the one-second
 * tick stops at this button instead of rebuilding the list beneath it.
 */
/** The library's search box, split out so the screen composable stays readable. */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Filled rather than outlined: on the near-black page an outline is a bright rectangle
    // competing with the rows under it, while a filled field reads as a surface you type into.
    // The indicator is transparent for the same reason — a coloured underline is a second
    // emphasis the field does not need when it already has its own tone.
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TestTags.LIBRARY_SEARCH_FIELD),
        singleLine = true,
        label = { Text(stringResource(R.string.exercise_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.exercise_search_clear),
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ExerciseList(
    items: List<ExerciseListItem>,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Horizontal insets, because the rows are cards now and a card that runs to the screen
        // edge is not a card. Bottom leaves room for the "start workout" button so it cannot
        // cover the last row.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            AppRow(
                headline = item.name,
                // Null while an unedited custom exercise has no taxonomy: the row
                // shows its name alone rather than "Other · Other" (N2).
                supporting = item.subtitle,
                leading = { IconTile(icon = Icons.Filled.FitnessCenter, accent = TileAccent.Teal) },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = { onExerciseClick(item.id) },
                // The headline is a lift's name, which names the thing but not the tap; the row
                // would otherwise announce "Back Squat, button".
                onClickLabel = stringResource(R.string.library_open_exercise, item.name),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseLibraryScreenPreview() {
    AndroidAppTheme {
        ExerciseLibraryScreen(
            state = ExerciseLibraryUiState(
                query = "",
                isLoading = false,
                items = listOf(
                    ExerciseListItem("back-squat", "Back Squat", "Quads · Barbell"),
                    ExerciseListItem("bench-press", "Barbell Bench Press", "Chest · Barbell"),
                    ExerciseListItem("my-lift", "Sled Push", null),
                ),
            ),
            title = "Exercise library",
            onQueryChange = {},
            onExerciseClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseLibraryEmptyPreview() {
    AndroidAppTheme {
        ExerciseLibraryScreen(
            state = ExerciseLibraryUiState(query = "zzz", isLoading = false, items = emptyList()),
            title = "Exercise library",
            onQueryChange = {},
            onExerciseClick = {},
        )
    }
}
