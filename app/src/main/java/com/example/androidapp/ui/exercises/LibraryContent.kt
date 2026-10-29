package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.LibraryFilter
import com.example.androidapp.domain.LibraryRow
import com.example.androidapp.domain.LibrarySort
import com.example.androidapp.domain.changedFamilies
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.NewExerciseDialog
import com.example.androidapp.ui.components.NewRowTags
import com.example.androidapp.ui.components.RowFold
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.rememberRowFold

/**
 * Everything the library screen draws, resolved once (ROADMAP N95, N228, N230, N231, N232).
 *
 * Three values that come from one state: which families are open, the rows that state makes, and the families
 * a new movement can be filed under. They are one holder rather than three locals because the screen is at the
 * length this project allows, and because they are one question — what does this library look like.
 */
internal class LibraryContent(
    val fold: RowFold,
    val rows: List<LibraryRow>,
    /** The families a movement made here can be filed under, in the list's own order (N232). */
    val families: List<Exercise>,
)

/** The holder above, remembered against the state it comes from. */
@Composable
internal fun rememberLibraryContent(state: ExerciseLibraryUiState): LibraryContent {
    // **Every family starts folded**, so the library opens as the list of families it is rather than as a wall
    // of movements — which is also why the editor's "what was already there is folded, what arrives opens"
    // seed is not wanted here: this list loads asynchronously, so nothing would look "already there"
    // (RowFold.kt). A **query** opens what it draws, and `libraryRows` is where that rule lives.
    val fold = rememberRowFold(ids = state.exercises.map { it.id }, newRowsOpen = false)
    // A family whose membership changed opens, once, so filing a movement is not a count changing on a
    // folded row and nothing else (N228).
    OpenFamiliesThatChanged(exercises = state.exercises, fold = fold)
    val rows = rememberLibraryRows(state = state, fold = fold)
    val families = remember(state.exercises) {
        state.exercises.filter { it.rowKind == RowKind.CATEGORY }.sortedBy { it.name }
    }
    return LibraryContent(fold = fold, rows = rows, families = families)
}

/**
 * Opens the family a movement joined or left (ROADMAP N228).
 *
 * The comparison runs on the composition after the change, which is why the previous memberships are kept
 * here rather than derived: what the screen needs is the difference from what it drew, and the library's list
 * arrives from the database already grouped.
 *
 * **`remember` rather than `rememberSaveable`**: the folds are what survive a rotation (N84), and a compare
 * map restored across one would describe a screen that no longer exists — an empty map is the honest answer,
 * because after a rotation nothing has changed yet.
 */
@Composable
private fun OpenFamiliesThatChanged(exercises: List<Exercise>, fold: RowFold) {
    var previous by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    LaunchedEffect(exercises) {
        val changed = changedFamilies(previous, exercises)
        previous = exercises.filter { it.rowKind == RowKind.MOVEMENT }.associate { it.id to it.parentId }
        changed.forEach(fold::open)
    }
}

/**
 * The grouped rows for this state, recomputed only when something that decides them changes.
 *
 * Split out of the screen because the screen is at the length this project allows and because this is the
 * list-builder's one call: the folds, the search, the order and the read are all inputs to one pure function.
 */
@Composable
private fun rememberLibraryRows(state: ExerciseLibraryUiState, fold: RowFold): List<LibraryRow> =
    remember(
        state.exercises,
        state.query,
        fold.foldedIds,
        state.removedHeads,
        state.sort,
        state.lastTrained,
        state.filter,
        state.notTrainedSince,
    ) {
        libraryRows(
            exercises = state.exercises,
            query = state.query,
            foldedCategories = fold.foldedIds,
            // The fuller map, so a child reads what a head passed down even after that head is removed. The
            // list still draws only what `state.exercises` holds: a removed row is never a row (N95).
            headResolver = state.exercises + state.removedHeads.values,
            sort = state.sort,
            lastTrained = state.lastTrained,
            filter = state.filter,
            notTrainedSince = state.notTrainedSince,
        )
    }

/**
 * The library's whole screen: what it draws, its action, and the three questions it can open (N95, N117, N232).
 *
 * Held in this file rather than in the screen's, because the screen function is at the length this project
 * allows and every line here is one of those questions. The dialogs are **fuelled by the state they close
 * over**, so a rotation cannot leave one open over a list that moved.
 */
@Composable
internal fun LibraryContentHost(
    state: ExerciseLibraryUiState,
    title: String,
    message: String?,
    onBack: (() -> Unit)?,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onExerciseClick: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onCreateMovement: (String, String?) -> Unit,
    onNewCategory: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var namingCategory by remember { mutableStateOf(false) }
    // The movement being made, held as the name the dialog opens on rather than as a flag: the empty state's
    // "create exactly what was typed" and the action are the same dialog (N232).
    var creatingMovement by rememberSaveable { mutableStateOf<String?>(null) }
    var detailsFor by rememberSaveable { mutableStateOf<String?>(null) }
    // One dismissal for both questions: clear it, then report that nothing was written (N50).
    val closeMovement = {
        creatingMovement = null
        onDismissMessage()
    }
    val closeCategory = {
        namingCategory = false
        onDismissMessage()
    }
    val content = rememberLibraryContent(state = state)
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    LibraryScaffold(
        state = state,
        content = content,
        title = title,
        onBack = onBack,
        onQueryChange = onQueryChange,
        onSortChange = onSortChange,
        onFilterChange = onFilterChange,
        onNewMovement = { creatingMovement = state.query.trim() },
        onExerciseClick = onExerciseClick,
        onCategoryDetails = { detailsFor = it },
        snackbarHostState = snackbarHostState,
        onNewCategory = { namingCategory = true },
        modifier = modifier,
    )

    // The family the hold was made on, or nothing when it has gone since (N117).
    CategoryDetailsHost(
        rows = content.rows,
        exercises = state.exercises,
        id = detailsFor,
        onDismiss = { detailsFor = null },
    )

    LibraryDialogs(
        movement = creatingMovement,
        namingCategory = namingCategory,
        families = content.families,
        error = state.writeError,
        onMovementDismiss = closeMovement,
        onMovementCreate = { name, familyId ->
            creatingMovement = null
            onCreateMovement(name, familyId)
        },
        onCategoryDismiss = closeCategory,
        onCategoryCreate = { name ->
            namingCategory = false
            onNewCategory?.invoke(name)
        },
    )
}

/** Names a new family (ROADMAP N95): the same dialog a movement is made with, and its own words. */
@Composable
private fun CategoryNamingDialog(
    isOpen: Boolean,
    error: DataError?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    if (!isOpen) return
    NewExerciseDialog(
        error = error,
        onDismiss = onDismiss,
        onCreate = { name, _ -> onCreate(name) },
        title = stringResource(R.string.exercise_new_category),
        hint = stringResource(R.string.exercise_new_category_hint),
        testTags = NewRowTags(TestTags.NEW_CATEGORY_NAME, TestTags.NEW_CATEGORY_SAVE),
    )
}

/**
 * The two questions the library can open: the movement being made and the family being named (N232, N95).
 *
 * Their own composable because the host they belong to is at the length this project allows, and because they
 * are one shape — a name, the error that stopped it, and the two answers.
 */
@Composable
private fun LibraryDialogs(
    movement: String?,
    namingCategory: Boolean,
    families: List<Exercise>,
    error: DataError?,
    onMovementDismiss: () -> Unit,
    onMovementCreate: (String, String?) -> Unit,
    onCategoryDismiss: () -> Unit,
    onCategoryCreate: (String) -> Unit,
) {
    NewMovementDialog(
        initialName = movement,
        families = families,
        error = error,
        onDismiss = onMovementDismiss,
        onCreate = onMovementCreate,
    )

    CategoryNamingDialog(
        isOpen = namingCategory,
        error = error,
        onDismiss = onCategoryDismiss,
        onCreate = onCategoryCreate,
    )
}

/**
 * The library's frame: its bar, its list, and its one action (ROADMAP N95, N232).
 *
 * Its own composable because the host above is at the length this project allows, and because a Scaffold with
 * three slots is a layout rather than a behaviour.
 */
@Composable
private fun LibraryScaffold(
    state: ExerciseLibraryUiState,
    content: LibraryContent,
    title: String,
    onBack: (() -> Unit)?,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onNewMovement: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onCategoryDetails: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onNewCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            LibraryCreationButton(onNewMovement = onNewMovement, onNewCategory = onNewCategory)
        },
        topBar = { LibraryTopBar(title = title, onBack = onBack) },
    ) { innerPadding ->
        LibraryBody(
            state = state,
            rows = content.rows,
            fold = content.fold,
            onQueryChange = onQueryChange,
            onSortChange = onSortChange,
            onFilterChange = onFilterChange,
            onNewMovement = onNewMovement,
            onExerciseClick = onExerciseClick,
            onCategoryDetails = onCategoryDetails,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
