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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.LibraryRow
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.rememberRowFold
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

    // The grouped rows, derived here because **which families are open is screen state** (ROADMAP N95).
    // `rememberRowFold` is the sharing state N91 built, for the same reason: this activity declares no
    // `configChanges`, so a rotation would otherwise fold what the lifter opened.
    // Every family starts open: a library that opened folded would be a list of names with the movements
    // behind them, which is the screen's subject rather than a detail of it.
    val fold = rememberRowFold(ids = state.exercises.map { it.id }, initiallyOpen = true)
    val rows = remember(state.exercises, state.query, fold.foldedIds) {
        libraryRows(state.exercises, state.query, fold.foldedIds)
    }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Only the picker offers creation: the library is a reference you navigate to, while the gap
            // is felt mid-workout (ROADMAP N2). A composable rather than an `if` here, because the screen
            // is at the length this project allows.
            NewExerciseButton(onClick = onNewExercise)
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
                else -> ExerciseList(
                    items = rows,
                    isExpanded = fold::isExpanded,
                    onToggle = fold::toggle,
                    onExerciseClick = onExerciseClick,
                )
            }
        }
    }
}

/** The picker's one action, or nothing at all where creation is not offered (ROADMAP N2). */
@Composable
private fun NewExerciseButton(onClick: (() -> Unit)?) {
    if (onClick == null) return
    ExtendedFloatingActionButton(
        onClick = onClick,
        text = { Text(stringResource(R.string.exercise_new)) },
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        modifier = Modifier.testTag(TestTags.LIBRARY_NEW_EXERCISE),
    )
}

@Composable
internal fun LoadingState(modifier: Modifier = Modifier) {
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
internal fun EmptyState(
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
internal fun LibrarySearchField(
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

/**
 * The library as rows: a family head, its children under it, and the movements filed under nothing.
 *
 * A head's tap folds it (ROADMAP N95), the shape a planned exercise's row already uses (N91): the name is
 * the control, the label names the action, and the state is announced beside it rather than left to be
 * discovered. A child is indented by [LibraryRow.depth], which keeps "how deep is this" a fact about the
 * library rather than about this list.
 */
@Composable
internal fun ExerciseList(
    items: List<LibraryRow>,
    isExpanded: (String) -> Boolean,
    onToggle: (String) -> Unit,
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
        items(items = items, key = { it.id }) { row ->
            if (row.isCategory) {
                CategoryRow(
                    row = row,
                    isExpanded = isExpanded(row.id),
                    onToggle = { onToggle(row.id) },
                )
            } else {
                ExerciseRow(row = row, onExerciseClick = onExerciseClick)
            }
        }
    }
}

@Composable
private fun CategoryRow(
    row: LibraryRow,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(R.string.library_category_open)
    val foldedLabel = stringResource(R.string.library_category_folded)
    AppRow(
        headline = row.name,
        // The count is the whole point of a folded head: it says a family is there without spending the
        // room to show it.
        supporting = pluralStringResource(R.plurals.library_exercises, row.childCount, row.childCount),
        leading = {
            IconTile(icon = Icons.Filled.FitnessCenter, accent = TileAccent.Indigo)
        },
        trailing = {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onToggle,
        // The action, not the family: "Open Bench Press", and "Fold" once it is open (N91's rule).
        onClickLabel = stringResource(
            if (isExpanded) R.string.library_close_category else R.string.library_open_category,
            row.name,
        ),
        stateDescription = if (isExpanded) openLabel else foldedLabel,
        testTag = TestTags.libraryCategory(row.id),
        modifier = modifier,
    )
}

@Composable
private fun ExerciseRow(
    row: LibraryRow,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppRow(
        headline = row.name,
        // Null while an unedited custom exercise has no taxonomy: the row
        // shows its name alone rather than "Other · Other" (N2).
        supporting = row.subtitle,
        leading = { IconTile(icon = Icons.Filled.FitnessCenter, accent = TileAccent.Teal) },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = { onExerciseClick(row.id) },
        // The headline is a lift's name, which names the thing but not the tap; the row
        // would otherwise announce "Back Squat, button".
        onClickLabel = stringResource(R.string.library_open_exercise, row.name),
        // A variation filed under a family is drawn in from the edge, which is the only thing that says
        // it belongs to the row above it rather than standing beside it.
        modifier = modifier.padding(start = (row.depth * CHILD_INDENT).dp),
    )
}

/** How far a family's child is inset, in dp. One step, because the shape is two rules deep (N95). */
private const val CHILD_INDENT = 16

/** One preview row, so the two previews above stay short. */
private fun previewExercise(
    id: String,
    name: String,
    kind: RowKind,
    parent: String? = null,
) = Exercise(
    id = id,
    name = name,
    primaryMuscle = MuscleGroup.CHEST,
    equipment = Equipment.BARBELL,
    movementPattern = MovementPattern.HORIZONTAL_PUSH,
    parentId = parent,
    rowKind = kind,
)

@Preview(showBackground = true)
@Composable
private fun ExerciseLibraryScreenPreview() {
    AndroidAppTheme {
        ExerciseLibraryScreen(
            state = ExerciseLibraryUiState(
                query = "",
                isLoading = false,
                exercises = listOf(
                    previewExercise("cat-bench", "Bench Press", RowKind.CATEGORY),
                    previewExercise("bench-press", "Barbell Bench Press", RowKind.MOVEMENT, "cat-bench"),
                    previewExercise("back-squat", "Back Squat", RowKind.MOVEMENT),
                    previewExercise("my-lift", "Sled Push", RowKind.MOVEMENT),
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
            state = ExerciseLibraryUiState(query = "zzz", isLoading = false, exercises = emptyList()),
            title = "Exercise library",
            onQueryChange = {},
            onExerciseClick = {},
        )
    }
}
