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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.androidapp.domain.LibraryFilter
import com.example.androidapp.domain.LibraryRow
import com.example.androidapp.domain.LibrarySort
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.RowFold
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
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
        // A failed write is a message on the screen's own host (B88); the page's `error` stays the read's.
        // A last-trained read that failed is the same kind of sentence: the list is still readable, and what
        // is lost is one way of ordering it (N230).
        message = (state.writeError ?: state.lastTrainedError)?.let { dataErrorMessage(it) },
        onSortChange = viewModel::onSortChange,
        onFilterChange = viewModel::onFilterChange,
        onCreateMovement = viewModel::onCreateMovement,
        onNewCategory = viewModel::onCreateCategory,
        onDismissMessage = viewModel::onErrorShown,
        modifier = modifier,
    )
}

/**
 * The library list.
 *
 * The library's own screen since N95: the in-workout picker has its own
 * ([com.example.androidapp.ui.workout.ExercisePickerScreen]), because the two now hold different things — the
 * library keeps families and folds them, the picker holds a flat, movements-only list. What they share is
 * shared as composables, not by one screen serving two states, which is what let them drift apart the last
 * time they were one (B79, B94).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    state: ExerciseLibraryUiState,
    title: String,
    onQueryChange: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
    onDismissMessage: () -> Unit,
    /** How the list is ordered (ROADMAP N230). Required: a screen that forgot it would silently never sort. */
    onSortChange: (LibrarySort) -> Unit,
    /** The group the list is narrowed to (ROADMAP N231). */
    onFilterChange: (LibraryFilter) -> Unit,
    /** Makes a movement, filed under the family or nowhere (ROADMAP N232). */
    onCreateMovement: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    message: String? = null,
    /** Makes a family (ROADMAP N95). Set on the library, which is where the library's shape is kept. */
    onNewCategory: ((String) -> Unit)? = null,
) {
    LibraryContentHost(
        state = state,
        title = title,
        message = message,
        onBack = onBack,
        onQueryChange = onQueryChange,
        onSortChange = onSortChange,
        onFilterChange = onFilterChange,
        onExerciseClick = onExerciseClick,
        onDismissMessage = onDismissMessage,
        onCreateMovement = onCreateMovement,
        onNewCategory = onNewCategory,
        modifier = modifier,
    )
}

/** The screen's bar, split out so the screen stays inside the length this project allows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryTopBar(title: String, onBack: (() -> Unit)?) {
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title, testTag = TestTags.LIBRARY_TITLE) },
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
}

/**
 * The screen's body: the search, then whatever the state has to show.
 *
 * Split out of [ExerciseLibraryScreen] because that screen is at the length this project allows, and because
 * the branch order *is* the behaviour worth reading in one piece: a failed read is named before the empty
 * states, or a broken read would be reported as an empty library (B4).
 */
@Composable
internal fun LibraryBody(
    state: ExerciseLibraryUiState,
    rows: List<LibraryRow>,
    fold: RowFold,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onNewMovement: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onCategoryDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LibrarySearchField(query = state.query, onQueryChange = onQueryChange)
        LibraryControls(
            sort = state.sort,
            filter = state.filter,
            counts = state.filterCounts,
            onSortChange = onSortChange,
            onFilterChange = onFilterChange,
        )

        when {
            state.isLoading -> LoadingState()

            state.error != null -> CenteredMessage(
                text = dataErrorMessage(state.error),
                modifier = Modifier.testTag(TestTags.LIBRARY_READ_ERROR),
            )

            state.isEmpty -> EmptyState(
                query = state.query,
                libraryIsEmpty = state.libraryIsEmpty,
                // One tap from "no exercises match front squat" to making it (N232).
                onCreate = if (state.query.isBlank()) null else { { onNewMovement() } },
            )

            else -> ExerciseList(
                items = rows,
                onToggle = fold::toggle,
                onExerciseClick = onExerciseClick,
                onCategoryDetails = onCategoryDetails,
            )
        }
    }
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
    /** Offered where the search named a movement that is not there — the name is what would be made (N232). */
    onCreate: (() -> Unit)? = null,
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
            // The message's own slot: a control beside it would be drawn off the bottom of the screen.
            action = onCreate?.let { create ->
                {
                    AppTextButton(
                        onClick = create,
                        modifier = Modifier.testTag(TestTags.LIBRARY_CREATE_FROM_SEARCH),
                    ) {
                        Text(stringResource(R.string.exercise_new_from_search, query))
                    }
                }
            },
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
 * The library as rows: a family head, its movements under it, and the movements filed under nothing.
 *
 * A head's tap folds it (ROADMAP N95), the shape a planned exercise's row already uses (N91): the name is
 * the control, the label names the action, and the state is announced beside it rather than left to be
 * discovered. A child is indented by [LibraryRow.depth], which keeps "how deep is this" a fact about the
 * library rather than about this list.
 *
 * **Whether a head is open is [LibraryRow.isExpanded] and not a second lookup**, because that is already
 * the answer the grouping drew the rows with: asking the fold state again here is how the arrow and the rows
 * below it could disagree.
 */
@Composable
internal fun ExerciseList(
    items: List<LibraryRow>,
    onToggle: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What holding a family shows (ROADMAP N117): the facts it states for its movements.
     *
     * Defaulted because only the library fills it: the picker draws the same rows flat, with every category
     * filtered out, so it has no head to hold and nothing to pass.
     */
    onCategoryDetails: (String) -> Unit = {},
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
                    onToggle = { onToggle(row.id) },
                    onDetails = { onCategoryDetails(row.id) },
                )
            } else {
                ExerciseRow(row = row, onExerciseClick = onExerciseClick)
            }
        }
    }
}

/**
 * What a family head states about how much it holds (ROADMAP N225).
 *
 * One number where the search shows the whole family, and both where it shows part of it: "6 exercises" and
 * "1 of 6 exercises" are two different claims, and the second is what a narrowed family has to make.
 */
@Composable
private fun libraryCount(row: LibraryRow): String =
    if (row.isNarrowed) {
        pluralStringResource(
            R.plurals.library_exercises_of,
            row.childCount,
            row.matchedChildCount,
            row.childCount,
        )
    } else {
        pluralStringResource(R.plurals.library_exercises, row.childCount, row.childCount)
    }

@Composable
private fun CategoryRow(
    row: LibraryRow,
    onToggle: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded = row.isExpanded
    val openLabel = stringResource(R.string.library_category_open)
    val foldedLabel = stringResource(R.string.library_category_folded)
    AppRow(
        headline = row.name,
        // The count is the whole point of a folded head: it says a family is there without spending the
        // room to show it. **And it is the family's own count**, so a search that found one of six says so
        // rather than claiming the family is one movement (N225).
        supporting = libraryCount(row),
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
        // The tap already means fold (N91), so what the family *states* — the facts its movements inherit
        // (N110) — is reached by holding the row (N117). The label is what carries that to a screen reader,
        // because a hold is the one action here with no control to see.
        onLongClick = onDetails,
        onLongClickLabel = stringResource(R.string.library_category_details, row.name),
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
        // What the row states and nothing else (N110): a movement holds no rows, so there is no count to
        // add — the line is the taxonomy the resolver answers with, or absent.
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
        // A movement filed under a category is drawn in from the edge, which is the only thing that says it
        // belongs to the row above it rather than standing beside it.
        modifier = modifier.padding(start = (row.depth * CHILD_INDENT).dp),
    )
}

/** How far a family's movement is inset, in dp. One step, because a row is at depth 0 or 1 (N110). */
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
    movementPattern = MovementPattern.PRESS,
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
            onDismissMessage = {},
            onSortChange = {},
            onFilterChange = {},
            onCreateMovement = { _, _ -> },
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
            onDismissMessage = {},
            onSortChange = {},
            onFilterChange = {},
            onCreateMovement = { _, _ -> },
        )
    }
}
