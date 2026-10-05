package com.example.androidapp.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.warmUpRampFor
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.ExerciseActionsMenu
import com.example.androidapp.ui.components.ExerciseMenuTags
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TemplatePlanDialog
import com.example.androidapp.ui.components.TemplateSetDialog
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme

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
    modifier: Modifier = Modifier,
    viewModel: TemplateEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val templateId = state.template?.id

    // Deleting — or opening a template that is already gone — leaves the editor,
    // rather than leaving an empty shell behind with a live Delete button.
    LaunchedEffect(deleted, state.notFound) {
        if (deleted || state.notFound) currentOnBack()
    }

    TemplateEditorScreen(
        state = state,
        onRename = viewModel::onRename,
        onRemoveExercise = viewModel::onRemoveExercise,
        onToggleSuperset = viewModel::onToggleSuperset,
        onMoveExercise = viewModel::onMoveExercise,
        onDeleteTemplate = viewModel::onDeleteTemplate,
        onAddExercise = { templateId?.let(onAddExercise) },
        onAddSet = viewModel::onAddSet,
        onUpdateSet = viewModel::onUpdateSet,
        onRemoveSet = viewModel::onRemoveSet,
        onAddWarmUpSets = viewModel::onAddWarmUpSets,
        onSaveExercisePlan = viewModel::onSaveExercisePlan,
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
    onMoveExercise: (String, Int) -> Unit,
    onDeleteTemplate: () -> Unit,
    onAddExercise: () -> Unit,
    onBack: () -> Unit,
    onAddWarmUpSets: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
    onDismissMessage: () -> Unit = {},
    onAddSet: (String, TemplateSetEdit) -> Unit = { _, _ -> },
    onUpdateSet: (String, TemplateSetEdit) -> Unit = { _, _ -> },
    onRemoveSet: (String) -> Unit = {},
    onToggleSuperset: (String) -> Unit = {},
    onSaveExercisePlan: (String, Int?, String?, Int?) -> Unit = { _, _, _, _ -> },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)

    state.error?.let { failure ->
        val message = dataErrorMessage(failure)
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            currentOnDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TemplateEditorTopBar(
                name = state.template?.name,
                onBack = onBack,
                onDelete = { confirmingDelete = true },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExercise,
                text = { Text(stringResource(R.string.template_add_exercise)) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.testTag(TestTags.TEMPLATE_ADD_EXERCISE),
            )
        },
    ) { innerPadding ->
        TemplateEditorBody(
            state = state,
            onRename = onRename,
            onRemoveExercise = onRemoveExercise,
            onToggleSuperset = onToggleSuperset,
            onMoveExercise = onMoveExercise,
            onAddSet = onAddSet,
            onUpdateSet = onUpdateSet,
            onRemoveSet = onRemoveSet,
            onAddWarmUpSets = onAddWarmUpSets,
            onSaveExercisePlan = onSaveExercisePlan,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (confirmingDelete) {
        DeleteTemplateDialog(
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                onDeleteTemplate()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorTopBar(
    name: String?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = name ?: stringResource(R.string.template_edit_title),
                modifier = Modifier.testTag(TestTags.TEMPLATE_EDIT_TITLE),
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
            IconButton(onClick = onDelete, modifier = Modifier.testTag(TestTags.TEMPLATE_DELETE)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.template_delete),
                )
            }
        },
    )
}

@Composable
private fun TemplateEditorBody(
    state: TemplateEditorUiState,
    onRename: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onAddSet: (String, TemplateSetEdit) -> Unit,
    onUpdateSet: (String, TemplateSetEdit) -> Unit,
    onRemoveSet: (String) -> Unit,
    onSaveExercisePlan: (String, Int?, String?, Int?) -> Unit,
    onAddWarmUpSets: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (String) -> Unit = {},
) {
    if (state.isLoading) {
        CenteredMessage(
            text = stringResource(R.string.templates_loading),
            modifier = modifier,
        )
        return
    }

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
                        position = index + 1,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        onMoveUp = { onMoveExercise(exercise.id, -1) },
                        onMoveDown = { onMoveExercise(exercise.id, 1) },
                        onRemove = { onRemoveExercise(exercise.id) },
                        // Same rule as the workout screen: the first planned exercise has no
                        // exercise above it to pair with (ROADMAP B28).
                        onToggleSuperset = if (index == 0) null else { { onToggleSuperset(exercise.id) } },
                        supersetLabels = state.supersetLabels,
                        onAddSet = { edit -> onAddSet(exercise.id, edit) },
                        onUpdateSet = onUpdateSet,
                        onRemoveSet = onRemoveSet,
                        onAddWarmUpSets = { step -> onAddWarmUpSets(exercise.id, step) },
                        onSavePlan = { rest, cue, rpe -> onSaveExercisePlan(exercise.id, rest, cue, rpe) },
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

/**
 * One exercise in the editor: its header, the plan's sets, and the rest and cue the
 * plan prescribes (ROADMAP N14).
 *
 * The dialogs live here rather than in the screen, the same shape as the exercise
 * section in a workout: the state that says "this panel is open" belongs next to the
 * row that opens it.
 */
@Composable
private fun TemplateExerciseBlock(
    exercise: TemplateExercise,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onAddSet: (TemplateSetEdit) -> Unit,
    onUpdateSet: (String, TemplateSetEdit) -> Unit,
    onRemoveSet: (String) -> Unit,
    onSavePlan: (Int?, String?, Int?) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    /** Adds a ramp rounded to the unit's step (ROADMAP N64), or null when there is none. */
    onAddWarmUpSets: ((Long) -> Unit)? = null,
    supersetLabels: Map<String, String> = emptyMap(),
) {
    // This exercise's display unit, resolved once: every load it names reads in it (ROADMAP N64).
    val unit = exerciseWeightUnit(exercise.weightUnit)
    var planOpen by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        TemplateExerciseRow(
            exercise = exercise,
            position = position,
            isFirst = isFirst,
            isLast = isLast,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove,
            onToggleSuperset = onToggleSuperset,
            supersetLabels = supersetLabels,
        )
        PlanRow(exercise = exercise, onClick = { planOpen = true })
        ExercisePlanFields(
            exercise = exercise,
            onSave = onSavePlan,
        )
    }

    if (planOpen) {
        TemplatePlanDialog(
            exerciseName = exercise.exerciseName,
            sets = exercise.sets,
            onAddSet = { adding = true },
            onEditSet = { editing = it.id },
            onDeleteSet = onRemoveSet,
            onDismiss = { planOpen = false },
            // Offered only where a ramp can actually be built, which is the same predicate the
            // action itself reads (ROADMAP N28, B50): asking whether a weight was merely *typed*
            // offered the button for an assisted set (0 kg) and for one too light to load, and a
            // press then reported success while writing nothing.
            // A ramp is a list of loads, so it exists in the unit the exercise is read in (N64).
            onAddWarmUpSets = if (onAddWarmUpSets != null &&
                warmUpRampFor(exercise.sets, Weight.stepGrams(unit)).isNotEmpty()
            ) {
                { onAddWarmUpSets(Weight.stepGrams(unit)) }
            } else {
                null
            },
            unit = unit,
        )
    }

    val edited = exercise.sets.firstOrNull { it.id == editing }
    if (adding || edited != null) {
        TemplateSetEditor(
            exercise = exercise,
            edited = edited,
            unit = unit,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = { edit ->
                if (edited == null) onAddSet(edit) else onUpdateSet(edited.id, edit)
                adding = false
                editing = null
            },
        )
    }
}

/**
 * The dialog that adds or edits one planned set (ROADMAP N14, N46).
 *
 * Split out of the block when N64's unit pushed it over the length this project allows, and it is
 * the piece that reads on its own: what a new set starts from, and what a save reports.
 */
@Composable
private fun TemplateSetEditor(
    exercise: TemplateExercise,
    edited: TemplateSet?,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (TemplateSetEdit) -> Unit,
) {
    TemplateSetDialog(
        unit = unit,
        // Add set starts from the last planned set rather than from nothing (ROADMAP N46): a set is
        // nearly always the one before it again, and Duplicate — which doubled the whole plan and
        // left the odd counts to manual adds — is gone. Blank only while there is no set to start
        // from, and the role prefills too, which is safe because Add warm-ups *prepends*: the last
        // set is the last working set.
        initial = edited?.let { it.toEdit() }
            ?: exercise.sets.lastOrNull()?.toEdit()
            ?: TemplateSetEdit(),
        isNew = edited == null,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

/** `Planned sets · 3` — the way into the plan for this exercise. */
@Composable
private fun PlanRow(
    exercise: TemplateExercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.template_plan_row)) },
        supportingContent = {
            Text(
                if (exercise.sets.isEmpty()) {
                    stringResource(R.string.template_plan_none)
                } else {
                    pluralStringResource(
                        R.plurals.template_plan_summary,
                        exercise.sets.size,
                        exercise.sets.size,
                    )
                },
            )
        },
        modifier = modifier
            .testTag(TestTags.TEMPLATE_PLAN_ROW)
            .clickable(onClick = onClick),
    )
}

/**
 * The effort, the rest and the cue this exercise's plan prescribes (N14, N59).
 *
 * The **target RPE is one number for the exercise**, beside the rest and cue the plan already
 * carried rather than on every planned set. All three blank means "the plan says nothing" — the
 * library's rest and cue show through, and the workout's RPE stepper opens on its default — so the
 * fields are empty rather than zero.
 */
@Composable
private fun ExercisePlanFields(
    exercise: TemplateExercise,
    onSave: (Int?, String?, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rest by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.restSeconds?.toString().orEmpty())
    }
    var cue by rememberSaveable(exercise.id) { mutableStateOf(exercise.techniqueNote.orEmpty()) }
    var rpe by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.targetRpeHalves?.let(Rpe::format).orEmpty())
    }
    val restSeconds = rest.trim().ifEmpty { null }?.toIntOrNull()
    val restIsValid = rest.isBlank() || (restSeconds != null && restSeconds >= RestTimer.MIN_PRESCRIBED_SECONDS)
    val rpeHalves = rpe.trim().ifEmpty { null }?.let(Rpe::parse)
    val rpeIsValid = rpe.isBlank() || rpeHalves != null
    val changed = restSeconds != exercise.restSeconds ||
        cue.trim().ifEmpty { null } != exercise.techniqueNote ||
        rpeHalves != exercise.targetRpeHalves

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = rest,
            onValueChange = { rest = it },
            modifier = Modifier.width(110.dp).testTag(TestTags.TEMPLATE_REST_FIELD),
            singleLine = true,
            isError = !restIsValid,
            label = { Text(stringResource(R.string.template_rest_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = rpe,
            onValueChange = { rpe = it },
            modifier = Modifier.width(90.dp).testTag(TestTags.TEMPLATE_EXERCISE_RPE),
            singleLine = true,
            isError = !rpeIsValid,
            label = { Text(stringResource(R.string.set_rpe_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = cue,
            onValueChange = { cue = it },
            modifier = Modifier.weight(1f).testTag(TestTags.TEMPLATE_CUE_FIELD),
            singleLine = true,
            label = { Text(stringResource(R.string.template_cue_label)) },
        )
        IconButton(
            onClick = { onSave(restSeconds, cue.trim().ifEmpty { null }, rpeHalves) },
            enabled = changed && restIsValid && rpeIsValid,
            modifier = Modifier.testTag(TestTags.TEMPLATE_REST_CUE_SAVE),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.template_rest_cue_save),
            )
        }
    }
}

private fun TemplateSet.toEdit() = TemplateSetEdit(
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRpeHalves = targetRpeHalves,
    note = note,
)

@Composable
private fun TemplateExerciseRow(
    exercise: TemplateExercise,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    supersetLabels: Map<String, String> = emptyMap(),
) {
    ListItem(
        headlineContent = {
            Text("$position. ${superscriptLabel(exercise, supersetLabels)}${exercise.exerciseName}")
        },
        supportingContent = {
            Text("${exercise.primaryMuscle.label} · ${exercise.equipment.label}")
        },
        trailingContent = {
            // One control, not four (ROADMAP N71): the superset toggle, the two arrows and the delete
            // icon became the same ⋮ the live workout uses, and removal now asks first — it takes the
            // planned sets with it and there is no undo to reach for (B2).
            ExerciseActionsMenu(
                contentDescription = stringResource(R.string.exercise_menu_more, exercise.exerciseName),
                canMoveUp = !isFirst,
                canMoveDown = !isLast,
                // The first planned exercise has nothing above it to pair with (B28), so the entry is
                // not offered rather than writing a group that rewrites every ungrouped row.
                supersetGrouped = onToggleSuperset?.let { exercise.supersetGroup != null },
                removeTitle = stringResource(R.string.template_remove_confirm_title),
                removeText = stringResource(
                    R.string.template_remove_confirm_text,
                    exercise.exerciseName,
                ),
                onToggleSuperset = { onToggleSuperset?.invoke() },
                onMove = { delta -> if (delta < 0) onMoveUp() else onMoveDown() },
                onRemove = onRemove,
                tags = ExerciseMenuTags(
                    menu = TestTags.templateMenu(exercise.id),
                    moveUp = TestTags.templateMove(exercise.id, up = true),
                    moveDown = TestTags.templateMove(exercise.id, up = false),
                    superset = TestTags.supersetToggle(exercise.id),
                    remove = TestTags.templateRemove(exercise.id),
                ),
            )
        },
        modifier = modifier.testTag(TestTags.templateExerciseRow(exercise.id)),
    )
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
            onMoveExercise = { _, _ -> },
            onDeleteTemplate = {},
            onAddExercise = {},
            onBack = {},
            onAddWarmUpSets = { _, _ -> },
        )
    }
}

/** `A1 · ` for a grouped planned exercise, or nothing (ROADMAP B16). */
private fun superscriptLabel(exercise: TemplateExercise, labels: Map<String, String>): String =
    labels[exercise.id]?.let { "$it · " }.orEmpty()
