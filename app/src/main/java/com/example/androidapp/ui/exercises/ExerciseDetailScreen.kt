package com.example.androidapp.ui.exercises

import com.example.androidapp.ui.components.CenteredMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.effectivePrimaryMuscle
import com.example.androidapp.domain.effectiveSecondaryMuscles
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SELECTABLE_MUSCLE_GROUPS
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.restLabel
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme

/** Stateful entry point for the detail destination; reads its id from the route. */
@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    onOpenTrends: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailScreen(
        state = state,
        onBack = onBack,
        onOpenTrends = onOpenTrends,
        onEdit = viewModel::onEdit,
        onNewVariation = viewModel::onCreateVariation,
        onCancelEdit = viewModel::onCancelEdit,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    state: ExerciseDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTrends: (String) -> Unit = {},
    onEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSave: (ExerciseEdit) -> Unit = {},
    /** Files a new variation of the exercise on screen and opens it for naming (ROADMAP N95). */
    onNewVariation: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
                    // A variant of this movement (ROADMAP N95), offered for a movement only: a variation
                    // hangs under an exercise, and one under a category would be a third level.
                    if (state.canCreateVariation) {
                        AppTextButton(
                            onClick = onNewVariation,
                            modifier = Modifier.testTag(TestTags.EXERCISE_NEW_VARIATION),
                        ) {
                            Text(stringResource(R.string.exercise_new_variation))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        ExerciseDetailBody(
            state = state,
            onCancelEdit = onCancelEdit,
            onSave = onSave,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * What the screen shows once it has a state: a spinner, a failure, "no such
 * exercise", the edit form, or the exercise itself.
 *
 * Split out because the screen crossed the method limit when B4 added the failure
 * branch — and because the branch order *is* the behaviour worth reading in one
 * piece: a failure must be named before "not found", or a broken read would be
 * reported as a missing exercise.
 */
@Composable
private fun ExerciseDetailBody(
    state: ExerciseDetailUiState,
    onCancelEdit: () -> Unit,
    onSave: (ExerciseEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = state.exercise
    when {
        state.isLoading -> CenteredMessage(
            text = stringResource(R.string.exercise_library_loading),
            showSpinner = true,
            modifier = modifier,
        )

        // A failed read says so, where the exercise would have been (B4).
        state.error != null && !state.isEditing -> CenteredMessage(
            text = dataErrorMessage(state.error),
            showSpinner = false,
            modifier = modifier.testTag(TestTags.EXERCISE_READ_ERROR),
        )

        state.notFound -> CenteredMessage(
            text = stringResource(R.string.exercise_detail_not_found),
            showSpinner = false,
            modifier = modifier,
        )

        exercise != null && state.isEditing -> ExerciseEditForm(
            exercise = exercise,
            error = state.error,
            categoryOptions = state.categoryOptions,
            onCancel = onCancelEdit,
            onSave = onSave,
            modifier = modifier,
        )

        exercise != null -> ExerciseDetails(
            exercise = exercise,
            head = state.head,
            headName = state.headName,
            modifier = modifier,
        )
    }
}

/**
 * The exercise's own display unit, as a read-only row (ROADMAP N64).
 *
 * "App default" names the unit in force as well as saying where the value comes from: a row reading
 * only "kg" would look like this exercise's own setting, which is the opposite of what it means.
 */
@Composable
private fun WeightUnitRow(exercise: Exercise, modifier: Modifier = Modifier) {
    AttributeRow(
        label = stringResource(R.string.exercise_detail_weight_unit),
        value = exercise.weightUnit?.label()
            ?: stringResource(
                R.string.exercise_detail_weight_unit_default,
                LocalWeightUnit.current.label(),
            ),
        modifier = modifier.testTag(TestTags.EXERCISE_WEIGHT_UNIT),
    )
}

/**
 * The exercise's own weight step, as a read-only row (ROADMAP N77).
 *
 * The default names the step in force as well as saying where it comes from, the shape the unit row
 * beside it uses: "2.5 kg" alone would read as this exercise's own setting rather than the unit's.
 */
@Composable
private fun WeightStepRow(exercise: Exercise, modifier: Modifier = Modifier) {
    val unit = exerciseWeightUnit(exercise.weightUnit)
    AttributeRow(
        label = stringResource(R.string.exercise_detail_weight_step),
        value = exercise.stepGrams?.let {
            stringResource(R.string.exercise_detail_weight_step_value, Weight.format(it, unit), unit.label())
        } ?: stringResource(
            R.string.exercise_detail_weight_step_default,
            Weight.format(Weight.stepGrams(unit), unit),
            unit.label(),
        ),
        modifier = modifier.testTag(TestTags.EXERCISE_WEIGHT_STEP),
    )
}

@Composable
private fun ExerciseDetails(
    exercise: Exercise,
    /** The head this row hangs under, or null (ROADMAP N95): what it inherits its muscles from. */
    head: Exercise?,
    /** What that head is called, or null. Read apart from [head] because a removed head still names it. */
    headName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FamilyRows(exercise = exercise, head = head, headName = headName)
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_secondary),
            value = exercise.secondaryMuscles
                .joinToString(", ") { it.label }
                .ifEmpty { stringResource(R.string.exercise_detail_none) },
        )
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_equipment),
            value = exercise.equipment.label,
        )
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_pattern),
            value = exercise.movementPattern.label,
        )
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_rest),
            // Unset means "use the app default", shown as such rather than as a
            // dash: the user needs to know what will actually happen (N5). A stored
            // zero is the other state again, and reads as a word (N45).
            value = exercise.restSeconds?.let { restLabel(it) }
                ?: stringResource(
                    R.string.exercise_detail_rest_default,
                    restLabel(RestTimer.DEFAULT_SECONDS),
                ),
        )
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_cue),
            value = exercise.techniqueNote ?: stringResource(R.string.exercise_detail_none),
        )
        HorizontalDivider()

        WeightUnitRow(exercise = exercise)

        WeightStepRow(exercise = exercise)
        HorizontalDivider()

        // Honest placeholder: history is the next milestone, not a broken screen.
        Text(
            text = stringResource(R.string.exercise_detail_history_planned),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}


/**
 * What this row takes from the head it hangs under (ROADMAP N95): the family's name, the muscle it inherits,
 * and the secondary list it defaults to.
 *
 * One subject — what this row is, by virtue of where it is filed — and split out of [ExerciseDetails], which
 * is at the length this project allows. A loose movement inherits nothing and shows only what it holds.
 */
@Composable
private fun FamilyRows(exercise: Exercise, head: Exercise?, headName: String?) {
    // One emitting root, because three sibling `AttributeRow`s and their dividers are one block of content:
    // a composable that emits several siblings at its top level has no single anchor for them (N95's rows are
    // read together anyway, as the answer to "what is this filed under").
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Shown only when there is a head: a loose movement has no family to name rather than an empty row.
        headName?.let { name ->
            AttributeRow(
                label = stringResource(R.string.exercise_category_label),
                value = name,
                modifier = Modifier.testTag(TestTags.EXERCISE_DETAIL_CATEGORY),
            )
            HorizontalDivider()
        }

        AttributeRow(
            label = stringResource(R.string.exercise_detail_primary),
            // The head's muscle, because an exercise **inherits** it (N95). Nothing is written to the row for
            // it, which is why this reads the head rather than the row's own value.
            // Through the resolver rather than the head's field, so a head that says nothing is silent (N95):
            // reading `head?.primaryMuscle` here was the bug the device caught, and it survived the domain fix
            // because this call site was a second copy of the rule.
            value = exercise.effectivePrimaryMuscle(listOfNotNull(head, exercise)).label,
        )
        HorizontalDivider()

        AttributeRow(
            label = stringResource(R.string.exercise_detail_secondary),
            // The head's list while this exercise has named none of its own: "default from the category and
            // are the exercise's to change", so the exercise's own list wins the moment it has one.
            value = exercise.effectiveSecondaryMuscles(listOfNotNull(head, exercise))
                .joinToString(", ") { it.label }
                .ifEmpty { stringResource(R.string.exercise_detail_none) },
        )
        HorizontalDivider()
    }
}

/**
 * The edit form's working copy (ROADMAP N2, N5).
 *
 * The rest is held as raw text, not as an `Int?`: a half-typed value must not be
 * silently coerced into a rest the user did not type. Empty means "the app
 * default", a number — **zero included** — is the rest (N45), and only a negative
 * or unparseable value keeps Save disabled.
 */
private data class ExerciseDraft(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment,
    val movementPattern: MovementPattern,
    val restText: String,
    val techniqueNote: String,
    /** The step as typed, in this draft's unit (ROADMAP N77). Empty means the unit's own. */
    val stepText: String = "",
    /** The three-way choice: null follows the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    /**
     * The unit in force when the form opened, carried because a draft cannot read the ambient.
     *
     * It is what the step's text is parsed and shown in when the exercise has no unit of its own, so
     * a form opened in pounds reads and writes pounds (N77).
     */
    val appUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /**
     * The head this exercise is filed under, or null (ROADMAP N95).
     *
     * Part of the draft so *move to category* is an ordinary save: the field leaves with the rest of the
     * form, and a cancellation takes the move back with everything else it would have changed.
     */
    val parentId: String? = null,
) {
    val restSeconds: Int? get() = restText.trim().ifEmpty { null }?.toIntOrNull()

    /** The unit this draft's numbers are typed in: its own choice, or the app's (N64). */
    val unit: WeightUnit get() = weightUnit ?: appUnit

    /** The step the field states, in grams, or null when it states none (N77). */
    val stepGrams: Long? get() = stepText.trim().ifEmpty { null }?.let { Weight.parse(it, unit) }

    /**
     * The step text as it reads in [next], when the unit changes (ROADMAP B66).
     *
     * The step is a load in grams and the text is only how it is read, so the number travels: 5 typed
     * in kilograms becomes the same load in pounds rather than a different one. The field shows a tenth
     * of the unit, so the re-expression is rounded to that — 5 kg reads as 11 lb and stores 4990 g,
     * which both units still display as the same step. Blank stays blank, since that is the unit's own
     * step and needs no carrying, and an unparseable text is kept as typed rather than replaced by
     * "empty", which would hide the field's own error.
     */
    fun stepTextIn(next: WeightUnit): String {
        val grams = stepText.takeIf { it.isNotBlank() }?.let { stepGrams }
        return grams?.let { Weight.format(it, next) } ?: stepText
    }

    /**
     * A step of zero is refused rather than stored: it is not a small step but no step, and the
     * warm-up ramp divides by it (N77). Blank is the unit's own, which is a real answer.
     */
    val stepIsValid: Boolean get() = stepText.isBlank() || (stepGrams ?: 0L) > 0L

    val restIsValid: Boolean
        get() {
            if (restText.isBlank()) return true
            val seconds = restSeconds ?: return false
            return seconds >= RestTimer.MIN_PRESCRIBED_SECONDS
        }

    val canSave: Boolean get() = name.isNotBlank() && restIsValid && stepIsValid

    fun toEdit(): ExerciseEdit = ExerciseEdit(
        name = name.trim(),
        primaryMuscle = primaryMuscle,
        equipment = equipment,
        movementPattern = movementPattern,
        restSeconds = restSeconds,
        techniqueNote = techniqueNote.trim().ifEmpty { null },
        stepGrams = stepGrams,
        weightUnit = weightUnit,
        parentId = parentId,
    )
}

private fun Exercise.toDraft(appUnit: WeightUnit) = ExerciseDraft(
    name = name,
    primaryMuscle = primaryMuscle,
    equipment = equipment,
    movementPattern = movementPattern,
    restText = restSeconds?.toString().orEmpty(),
    techniqueNote = techniqueNote.orEmpty(),
    // Shown in the unit it is parsed in, which is this exercise's own where it has one (N77).
    stepText = stepGrams?.let { Weight.format(it, weightUnit ?: appUnit) }.orEmpty(),
    weightUnit = weightUnit,
    appUnit = appUnit,
    parentId = parentId,
)

/**
 * The edit form behind the detail screen (ROADMAP N2, N5).
 *
 * The draft lives here rather than in the ViewModel: these are transient field
 * values, and keeping them local means a recomposition caused by an incoming
 * error cannot discard what the user typed.
 */
@Composable
private fun ExerciseEditForm(
    exercise: Exercise,
    error: DataError?,
    /** The heads this row may be filed under (ROADMAP N95); empty for a category, which is top level. */
    categoryOptions: List<Exercise>,
    onCancel: () -> Unit,
    onSave: (ExerciseEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keys on the exercise id so switching to another exercise resets the draft
    // rather than carrying the previous one's values over.
    val appUnit = LocalWeightUnit.current
    var draft by remember(exercise.id) { mutableStateOf(exercise.toDraft(appUnit)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExerciseEditFields(
            draft = draft,
            onDraftChange = { draft = it },
            categoryOptions = categoryOptions.takeIf { exercise.rowKind == RowKind.MOVEMENT },
        )
        ExercisePrescriptionFields(draft = draft, onDraftChange = { draft = it })

        error?.let { failure ->
            Text(
                text = dataErrorMessage(failure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        ExerciseEditActions(
            // A name is required; everything else may stay unset, and a rest that
            // was typed but does not parse keeps Save disabled rather than being
            // rounded to a number the user did not enter.
            saveEnabled = draft.canSave,
            onCancel = onCancel,
            onSave = { onSave(draft.toEdit()) },
        )
    }
}

/**
 * Which head this movement is filed under (ROADMAP N95, B85).
 *
 * The same labelled dropdown the taxonomy fields use, over the library's heads with a null first: moving a
 * row is *one field of the row*, and the shape's whole point is that keeping the taxonomy honest is cheap.
 * A head is a **category or an exercise** — a movement is filed under a family, and a variation under the
 * exercise it is performed as — so a variation's real head is among the options and the field reads it back
 * instead of falling through to the null label. *Not filed under anything* is an explicit choice rather than
 * an empty state, because a loose custom movement is a real place to be.
 */
@Composable
private fun CategoryPicker(
    selectedId: String?,
    options: List<Exercise>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.exercise_category_none)
    val optionIds: List<String?> = listOf(null) + options.map { it.id }
    AttributeSelector<String?>(
        label = stringResource(R.string.exercise_category_label),
        selected = selectedId,
        options = optionIds,
        optionLabel = { id -> options.firstOrNull { it.id == id }?.name ?: none },
        // A test reaches a head by its id rather than by its name, the rule the enum options follow (B71).
        optionTag = { id -> id?.let { TestTags.exerciseCategoryOption(it) } },
        testTag = TestTags.EXERCISE_EDIT_CATEGORY,
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** The identity attributes — name and taxonomy (ROADMAP N2). */
@Composable
private fun ExerciseEditFields(
    draft: ExerciseDraft,
    onDraftChange: (ExerciseDraft) -> Unit,
    modifier: Modifier = Modifier,
    /** The heads this row may be filed under, or null where it has nowhere to be filed (N95). */
    categoryOptions: List<Exercise>? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.EXERCISE_EDIT_NAME),
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_name_label)) },
        )

        // Only a movement has somewhere to be filed: a category sits at the top level, because the shape is
        // two rules deep and a head under a head would be a third (N95).
        categoryOptions?.let { options ->
            CategoryPicker(
                selectedId = draft.parentId,
                options = options,
                onSelect = { onDraftChange(draft.copy(parentId = it)) },
            )
        }

        AttributeSelector(
            label = stringResource(R.string.exercise_detail_primary),
            selected = draft.primaryMuscle,
            // The retired `BACK` is not an option (N75); a row that carries it still reads as Back.
            options = SELECTABLE_MUSCLE_GROUPS,
            optionLabel = { it.label },
            testTag = TestTags.Muscle.FIELD,
            onSelect = { onDraftChange(draft.copy(primaryMuscle = it)) },
            // Tagged by the enum's name rather than its label, so a test names the group and not the
            // English word for it (B71).
            optionTag = { TestTags.Muscle.option(it.name) },
        )

        AttributeSelector(
            label = stringResource(R.string.exercise_detail_equipment),
            selected = draft.equipment,
            options = Equipment.entries,
            optionLabel = { it.label },
            testTag = TestTags.EXERCISE_EDIT_EQUIPMENT,
            onSelect = { onDraftChange(draft.copy(equipment = it)) },
        )

        AttributeSelector(
            label = stringResource(R.string.exercise_detail_pattern),
            selected = draft.movementPattern,
            options = MovementPattern.entries,
            optionLabel = { it.label },
            testTag = TestTags.EXERCISE_EDIT_PATTERN,
            onSelect = { onDraftChange(draft.copy(movementPattern = it)) },
        )
    }
}

/**
 * The prescription attributes — rest and the technique cue (ROADMAP N5).
 *
 * Separate from the identity fields because they are a different kind of fact:
 * the identity says *what* the movement is, this says *how* to do it.
 */
@Composable
private fun ExercisePrescriptionFields(
    draft: ExerciseDraft,
    onDraftChange: (ExerciseDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = draft.restText,
            onValueChange = { onDraftChange(draft.copy(restText = it)) },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.EXERCISE_EDIT_REST),
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_detail_rest)) },
            supportingText = { Text(stringResource(R.string.rest_edit_hint)) },
            isError = draft.restText.isNotBlank() && !draft.restIsValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        OutlinedTextField(
            value = draft.techniqueNote,
            onValueChange = { onDraftChange(draft.copy(techniqueNote = it)) },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.EXERCISE_EDIT_CUE),
            label = { Text(stringResource(R.string.exercise_detail_cue)) },
            minLines = 2,
        )

        WeightUnitChoice(
            selected = draft.weightUnit,
            // The step is a load in grams, so a unit change carries the number rather than
            // reinterpreting the text: "5" typed in kilograms would otherwise be read as 5 lb and
            // stored as 2.27 kg, silently changing what the lifter asked for (ROADMAP B66). Choosing
            // the app's own unit reads in the app's, which is the unit the draft carries for it.
            onSelect = {
                onDraftChange(draft.copy(weightUnit = it, stepText = draft.stepTextIn(it ?: draft.appUnit)))
            },
        )

        // Last, because the step is typed in the unit chosen just above it (N77). The hint names
        // what an empty field means, since "the unit's own" is a value rather than an absence.
        OutlinedTextField(
            value = draft.stepText,
            onValueChange = { onDraftChange(draft.copy(stepText = it)) },
            modifier = Modifier.fillMaxWidth().testTag(TestTags.EXERCISE_EDIT_STEP),
            singleLine = true,
            label = { Text(stringResource(R.string.exercise_detail_weight_step)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.exercise_step_edit_hint,
                        Weight.format(Weight.stepGrams(draft.unit), draft.unit),
                        draft.unit.label(),
                    ),
                )
            },
            isError = draft.stepText.isNotBlank() && !draft.stepIsValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

/**
 * This exercise's own display unit, or the app's (ROADMAP N64).
 *
 * Three chips rather than two, because null is a real answer here — "follow the app" — and a
 * two-way control would have to express it as one of the two units, which is what an override is
 * not. Same shape the settings screen's own unit control uses.
 */
private fun WeightUnit.editChipTag(): String = when (this) {
    WeightUnit.KILOGRAMS -> TestTags.EXERCISE_EDIT_WEIGHT_UNIT_KG
    WeightUnit.POUNDS -> TestTags.EXERCISE_EDIT_WEIGHT_UNIT_LB
}

@Composable
private fun WeightUnitChoice(
    selected: WeightUnit?,
    onSelect: (WeightUnit?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exercise_detail_weight_unit),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppFilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = stringResource(
                    R.string.exercise_detail_weight_unit_default,
                    LocalWeightUnit.current.label(),
                ),
                testTag = TestTags.EXERCISE_EDIT_WEIGHT_UNIT_DEFAULT,
            )
            WeightUnit.entries.forEach { unit ->
                AppFilterChip(
                    selected = selected == unit,
                    onClick = { onSelect(unit) },
                    label = unit.label(),
                    testTag = unit.editChipTag(),
                )
            }
        }
    }
}

@Composable
private fun ExerciseEditActions(
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        AppTextButton(
            onClick = onCancel,
            modifier = Modifier.testTag(TestTags.EXERCISE_EDIT_CANCEL),
        ) {
            Text(stringResource(R.string.action_cancel))
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.testTag(TestTags.EXERCISE_EDIT_SAVE),
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

/**
 * A labelled dropdown over one enum's values.
 *
 * A `DropdownMenu` behind a button rather than an exposed-dropdown text field:
 * the value is always one of a fixed set, so there is nothing to type, and the
 * button's label reads the current choice to a screen reader without extra work.
 */
@Composable
private fun <T> AttributeSelector(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    testTag: String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * How each option is tagged, so a test reaches it by identity rather than by its English label
     * (ROADMAP B71).
     *
     * The labels are enum literals today — localization is parked (P5.4) — which is exactly why the
     * repo's rule is to address controls by a tag: a translated label would silently break a test, and
     * the choice would read as a missing option.
     */
    optionTag: (T) -> String? = { null },
) {
    var open by remember { mutableStateOf(false) }

    // Boxed so the menu anchors to its button: a composable emitting two siblings
    // at the top level has no defined anchor for the popup.
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().testTag(testTag),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = optionLabel(selected), style = MaterialTheme.typography.bodyLarge)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                val tag = optionTag(option)
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    modifier = if (tag == null) Modifier else Modifier.testTag(tag),
                )
            }
        }
    }
}

@Composable
private fun AttributeRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}


@Preview(showBackground = true)
@Composable
private fun ExerciseDetailScreenPreview() {
    AndroidAppTheme {
        ExerciseDetailScreen(
            state = ExerciseDetailUiState(
                isLoading = false,
                exercise = Exercise(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.SQUAT,
                ),
            ),
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseDetailEditPreview() {
    AndroidAppTheme {
        ExerciseDetailScreen(
            state = ExerciseDetailUiState(
                isLoading = false,
                isEditing = true,
                exercise = Exercise(
                    id = "custom-1",
                    name = "Sled Push",
                    primaryMuscle = MuscleGroup.OTHER,
                    equipment = Equipment.OTHER,
                    movementPattern = MovementPattern.OTHER,
                    isCustom = true,
                ),
            ),
            onBack = {},
        )
    }
}
