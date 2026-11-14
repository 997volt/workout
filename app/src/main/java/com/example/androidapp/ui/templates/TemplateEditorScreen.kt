package com.example.androidapp.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.WriteOutcome
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.rememberRowFold
import com.example.androidapp.ui.theme.AndroidAppTheme

/** The [TemplateExerciseBlock] open-dialog value that means "a new set"; a set id means an edit (B284). */

/**
 * One template: its name and its ordered exercises (ROADMAP N3).
 *
 * v1 is deliberately just exercises and their order. Target sets and rep ranges,
 * per-exercise rest, supersets and drop sets are the P3.1 half of the backlog, and
 * they arrive once templates are in use — this screen is the seam they land in.
 */
@Composable
fun TemplateEditorRoute(
    onAddExercise: (String) -> Unit,
    onBack: () -> Unit,
    /**
     * Opens a planned exercise's own screen (ROADMAP N121), which is the destination the library reaches.
     *
     * Planning is where a lifter notices the lift's facts matter — its unit, step, rest and cue — and the only
     * way to them was to leave the template and find the lift in the library.
     */
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val openVersion by viewModel.openVersion.collectAsStateWithLifecycle()
    val versionKept by viewModel.versionKept.collectAsStateWithLifecycle()
    val hiddenChange by viewModel.hiddenChange.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val templateId = state.template?.id
    val setSaveOutcome by viewModel.setSaveOutcome.collectAsStateWithLifecycle()

    // Deleting — or opening a template that is already gone — leaves the editor,
    // rather than leaving an empty shell behind with a live Delete button.
    LaunchedEffect(deleted, state.notFound) {
        if (deleted || state.notFound) currentOnBack()
    }

    TemplateEditorScreen(
        state = state,
        onRename = viewModel::onRename,
        onRemoveExercise = viewModel::onRemoveExercise,
        onOpenExercise = onOpenExercise,
        onToggleSuperset = viewModel::onToggleSuperset,
        onMoveExercise = viewModel::onMoveExercise,
        onDeleteTemplate = viewModel::onDeleteTemplate,
        onAddExercise = { templateId?.let(onAddExercise) },
        setSaveOutcome = setSaveOutcome,
        onDismissSetSave = viewModel::onDismissSetSave,
        onAddSet = viewModel::onAddSet,
        onAddSets = viewModel::onAddSets,
        onUpdateSet = viewModel::onUpdateSet,
        onRemoveSet = viewModel::onRemoveSet,
        onUndoRemoveSet = viewModel::onUndoRemoveSet,
        onDismissUndo = viewModel::onDismissUndo,
        onAddWarmUpSets = viewModel::onAddWarmUpSets,
        onSaveExercisePlan = viewModel::onSaveExercisePlan,
        versionUi = TemplateVersionUi(
            versions = state.versions,
            openVersion = openVersion,
            versionKept = versionKept,
            onKeepVersion = viewModel::onKeepVersion,
            onDismissVersionNotice = viewModel::onDismissVersionNotice,
            onOpenVersion = viewModel::onOpenVersion,
            onDismissVersion = viewModel::onDismissVersion,
        ),
        onToggleHide = viewModel::onToggleHide,
        hiddenChange = hiddenChange,
        onDismissHiddenNotice = viewModel::onDismissHiddenNotice,
        onDismissMessage = viewModel::onErrorShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    state: TemplateEditorUiState,
    onRename: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onDeleteTemplate: () -> Unit,
    onAddExercise: () -> Unit,
    onBack: () -> Unit,
    onAddWarmUpSets: (String, Long) -> Unit,
    /** The last planned-set write's answer, so its editor closes on success only (B284). */
    setSaveOutcome: WriteOutcome<String>?,
    onDismissSetSave: () -> Unit,
    onDismissMessage: () -> Unit,
    onAddSet: (String, TemplateSetEdit) -> Unit,
    /** Writes several planned sets with one shape (P3.18). */
    onAddSets: (String, Int, TemplateSetEdit) -> Unit,
    onUpdateSet: (String, TemplateSetEdit) -> Unit,
    onRemoveSet: (String) -> Unit,
    /** Puts a just-removed planned set back where it was (ROADMAP N90). */
    onUndoRemoveSet: () -> Unit,
    onDismissUndo: () -> Unit,
    onToggleSuperset: (String) -> Unit,
    onSaveExercisePlan: (String, Int?, String?, Int?, Boolean) -> Unit,
    /** Hiding: the action, the one-shot sentence, and nothing to say with null (ROADMAP N214). */
    onToggleHide: () -> Unit,
    onDismissHiddenNotice: () -> Unit,
    modifier: Modifier = Modifier,
    /** The versions kept of this plan, and what may be done with them (ROADMAP N134). */
    versionUi: TemplateVersionUi = TemplateVersionUi(),
    hiddenChange: Boolean? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var showingVersions by rememberSaveable { mutableStateOf(false) } // the versions list, open
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)

    TemplateEditorMessages(
        state = state,
        versionUi = versionUi,
        hostState = snackbarHostState,
        onDismissMessage = currentOnDismissMessage,
        onUndoRemoveSet = onUndoRemoveSet,
        onDismissUndo = onDismissUndo,
        hiddenChange = hiddenChange,
        onDismissHiddenNotice = onDismissHiddenNotice,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TemplateEditorTopBar(
                state = state,
                versionUi = versionUi,
                onBack = onBack,
                onDelete = { confirmingDelete = true },
                onOpenVersions = { showingVersions = true },
                onToggleHide = onToggleHide,
            )
        },
        floatingActionButton = { AddExerciseButton(onClick = onAddExercise) },
    ) { innerPadding ->
        TemplateEditorBody(
            state = state,
            onRename = onRename,
            onRemoveExercise = onRemoveExercise,
            onOpenExercise = onOpenExercise,
            onToggleSuperset = onToggleSuperset,
            onMoveExercise = onMoveExercise,
            setSaveOutcome = setSaveOutcome,
            onDismissSetSave = onDismissSetSave,
            onAddSet = onAddSet,
            onAddSets = onAddSets,
            onUpdateSet = onUpdateSet,
            onRemoveSet = onRemoveSet,
            onAddWarmUpSets = onAddWarmUpSets,
            onSaveExercisePlan = onSaveExercisePlan,
            modifier = Modifier.padding(innerPadding).imePadding(),
        )
    }

    DeleteTemplateConfirmation(
        confirming = confirmingDelete,
        onDelete = onDeleteTemplate,
        onDismiss = { confirmingDelete = false },
    )

    TemplateVersionOverlays(
        versionUi = versionUi,
        showingVersions = showingVersions,
        onDismissVersions = { showingVersions = false },
    )
}

/** The plan's own *Add exercise*, the screen's one floating action (ROADMAP N3). */
@Composable
private fun AddExerciseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        text = { Text(stringResource(R.string.template_add_exercise)) },
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        modifier = modifier.testTag(TestTags.TEMPLATE_ADD_EXERCISE),
    )
}

/**
 * The delete confirmation, drawn only while it is being asked (ROADMAP N3).
 *
 * The confirm leaves the editor, so the dismissal and the write are one step here rather than two lines
 * at the call site.
 */
@Composable
private fun DeleteTemplateConfirmation(
    confirming: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (confirming) {
        DeleteTemplateDialog(
            onDismiss = onDismiss,
            onConfirm = {
                onDismiss()
                onDelete()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorTopBar(
    state: TemplateEditorUiState,
    versionUi: TemplateVersionUi,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onOpenVersions: () -> Unit,
    onToggleHide: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val versionCount = state.versions.size
    val versionsLabel = if (versionCount > 0) {
        stringResource(R.string.template_versions_with_count, versionCount)
    } else {
        stringResource(R.string.template_versions)
    }
    val hidden = state.template?.hiddenAt != null
    // Hide needs a plan that holds something; Unhide never does (ROADMAP N214).
    val canHide = (state.template?.exerciseCount ?: 0) > 0
    TopAppBar(
        title = {
            TopBarTitle(
                text = state.template?.name ?: stringResource(R.string.template_edit_title),
                testTag = TestTags.TEMPLATE_EDIT_TITLE,
                preserveCase = true,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // Keeping a version is done at the moment the lifter decides to change something, so it
            // sits with the plan's own actions rather than behind a settings screen (N134).
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag(TestTags.TEMPLATE_VERSIONS_MENU),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = versionsLabel,
                )
            }
            TemplateEditorMenu(
                expanded = menuOpen,
                versionsLabel = versionsLabel,
                hidden = hidden,
                canHide = canHide,
                onDismiss = { menuOpen = false },
                onKeepVersion = versionUi.onKeepVersion,
                onOpenVersions = onOpenVersions,
                onToggleHide = onToggleHide,
            )
            IconButton(onClick = onDelete, modifier = Modifier.testTag(TestTags.TEMPLATE_DELETE)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.template_delete),
                )
            }
        },
    )
}

/**
 * The plan's own list of blocks (ROADMAP N3).
 *
 * The caller applies `imePadding()` for the reason B119 gave the workout's: the window is edge-to-edge,
 * so it does not resize for the keyboard, and a block's fields near the foot of the list stayed under it
 * however far the list was scrolled (B122).
 */
@Composable
private fun TemplateEditorBody(
    state: TemplateEditorUiState,
    onRename: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    /** The last planned-set write's answer, so its editor closes on success only (B284). */
    setSaveOutcome: WriteOutcome<String>?,
    onDismissSetSave: () -> Unit,
    onAddSet: (String, TemplateSetEdit) -> Unit,
    onAddSets: (String, Int, TemplateSetEdit) -> Unit,
    onUpdateSet: (String, TemplateSetEdit) -> Unit,
    onRemoveSet: (String) -> Unit,
    onSaveExercisePlan: (String, Int?, String?, Int?, Boolean) -> Unit,
    onAddWarmUpSets: (String, Long) -> Unit,
    onToggleSuperset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        CenteredMessage(
            text = stringResource(R.string.templates_loading),
            modifier = modifier,
        )
        return
    }

    val fold = rememberRowFold(ids = state.exercises.map { it.id })

    Column(modifier = modifier.fillMaxSize()) {
        state.template?.let { template ->
            TemplateNameField(
                template = template,
                onRename = onRename,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        HorizontalDivider()

        if (state.exercises.isEmpty()) {
            CenteredMessage(
                text = stringResource(R.string.template_no_exercises),
                hint = stringResource(R.string.template_no_exercises_hint),
                modifier = Modifier.testTag(TestTags.TEMPLATE_NO_EXERCISES),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.TEMPLATE_EXERCISE_LIST),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(items = state.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                    TemplateExerciseBlock(
                        exercise = exercise,
                        appProgressionMode = state.appProgressionMode,
                        position = index + 1,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        isExpanded = fold.isExpanded(exercise.id),
                        onToggle = { fold.toggle(exercise.id) },
                        onMoveUp = { onMoveExercise(exercise.id, -1) },
                        onMoveDown = { onMoveExercise(exercise.id, 1) },
                        onRemove = { onRemoveExercise(exercise.id) },
                        onOpenExercise = { onOpenExercise(exercise.exerciseId) },
                        // Same rule as the workout screen: the first planned exercise has no
                        // exercise above it to pair with (ROADMAP B28).
                        onToggleSuperset = if (index == 0) null else { { onToggleSuperset(exercise.id) } },
                        supersetLabels = state.supersetLabels,
                        setSaveOutcome = setSaveOutcome,
                        onDismissSetSave = onDismissSetSave,
                        onAddSet = { edit -> onAddSet(exercise.id, edit) },
                        onAddSets = { count, edit -> onAddSets(exercise.id, count, edit) },
                        onUpdateSet = onUpdateSet,
                        onRemoveSet = onRemoveSet,
                        onAddWarmUpSets = { step -> onAddWarmUpSets(exercise.id, step) },
                        onSavePlan = { rest, cue, rpe, find -> onSaveExercisePlan(exercise.id, rest, cue, rpe, find) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * The template's name, committed deliberately rather than on every keystroke.
 *
 * A rename per character would write a row per character and, worse, could store a
 * half-typed name if the app died mid-word — and the template list shows that name.
 */
@Composable
private fun TemplateNameField(
    template: WorkoutTemplate,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the id, so another template's name can never leak into this field.
    var draft by rememberSaveable(template.id) { mutableStateOf(template.name) }
    val changed = draft.trim() != template.name

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f).testTag(TestTags.TEMPLATE_NAME_FIELD),
            singleLine = true,
            label = { Text(stringResource(R.string.template_name_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (changed) onRename(draft) }),
        )
        IconButton(
            onClick = { onRename(draft) },
            enabled = changed,
            modifier = Modifier.testTag(TestTags.TEMPLATE_NAME_SAVE),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.template_save_name),
            )
        }
    }
}

@Composable
private fun DeleteTemplateDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.template_delete_confirm_title)) },
        text = { Text(stringResource(R.string.template_delete_confirm_text)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.TEMPLATE_DELETE_CONFIRM),
            ) {
                Text(stringResource(R.string.template_delete))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TemplateEditorScreenPreview() {
    AndroidAppTheme {
        TemplateEditorScreen(
            setSaveOutcome = null,
            onDismissSetSave = {},
            state = TemplateEditorUiState(
                isLoading = false,
                template = WorkoutTemplate(id = "a", name = "Push day", exerciseCount = 2),
                exercises = listOf(
                    TemplateExercise(
                        id = "te1",
                        templateId = "a",
                        exerciseId = "bench-press",
                        position = 0,
                        exerciseName = "Bench Press",
                        primaryMuscle = MuscleGroup.CHEST,
                        equipment = Equipment.BARBELL,
                    ),
                    TemplateExercise(
                        id = "te2",
                        templateId = "a",
                        exerciseId = "overhead-press",
                        position = 1,
                        exerciseName = "Overhead Press",
                        primaryMuscle = MuscleGroup.SHOULDERS,
                        equipment = Equipment.BARBELL,
                    ),
                ),
            ),
            onRename = {},
            onRemoveExercise = {},
            onOpenExercise = {},
            onMoveExercise = { _, _ -> },
            onDeleteTemplate = {},
            onAddExercise = {},
            onBack = {},
            onAddWarmUpSets = { _, _ -> },
            onDismissMessage = {},
            onAddSet = { _, _ -> },
            onAddSets = { _, _, _ -> },
            onUpdateSet = { _, _ -> },
            onRemoveSet = { _ -> },
            onUndoRemoveSet = {},
            onDismissUndo = {},
            onToggleSuperset = {},
            onSaveExercisePlan = { _, _, _, _, _ -> },
            onToggleHide = {},
            onDismissHiddenNotice = {},
        )
    }
}
