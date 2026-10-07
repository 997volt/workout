package com.example.androidapp.ui.history

import androidx.compose.material3.OutlinedTextField
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SoreMuscle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.ui.components.ExerciseRatingSection
import com.example.androidapp.ui.components.SetEditorDialog
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.rpeMarker
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutFormat
import java.time.ZoneId
import java.time.Instant

@Composable
fun WorkoutDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
    onOpenExerciseTrends: (String) -> Unit = {},
    onOpenTemplate: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val savedTemplate by viewModel.savedTemplate.collectAsStateWithLifecycle()

    // Deleting the workout leaves nothing to look at, so the screen closes itself
    // rather than sitting on "no longer stored".
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(deleted) {
        if (deleted) currentOnBack()
    }

    WorkoutDetailScreen(
        onOpenExerciseTrends = onOpenExerciseTrends,
        state = state,
        onUpdateSet = viewModel::onUpdateSet,
        onDeleteSet = viewModel::onDeleteSet,
        onRateExercise = viewModel::onRateExercise,
        onDeleteWorkout = viewModel::onDeleteWorkout,
        onBack = onBack,
        modifier = modifier,
        savedTemplate = savedTemplate,
        onSaveAsTemplate = viewModel::onSaveAsTemplate,
        onDismissSavedTemplate = viewModel::onDismissSavedTemplate,
        onOpenTemplate = onOpenTemplate,
    )
}

/** Asks for the plan's name, since a copy called "Untitled" is a copy nobody finds again (N31). */
@Composable
private fun SaveAsPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_save_as_plan)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.detail_plan_name)) },
                modifier = Modifier.testTag(TestTags.DETAIL_PLAN_NAME),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { onConfirm(name) },
                // A blank name is refused by the repository too; disabling the button says so before
                // the tap rather than after it.
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(TestTags.DETAIL_PLAN_CONFIRM),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Confirms the copy and offers to go to it (ROADMAP N31). */
@Composable
private fun SavedAsPlanDialog(
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_saved_as_plan)) },
        confirmButton = {
            AppTextButton(
                onClick = onOpen,
                modifier = Modifier.testTag(TestTags.DETAIL_OPEN_NEW_PLAN),
            ) {
                Text(stringResource(R.string.detail_open_plan))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    state: WorkoutDetailUiState,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long) -> Unit,
    onDeleteSet: (String) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onDeleteWorkout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenExerciseTrends: (String) -> Unit = {},
    /** ROADMAP N31: the plan this workout was saved as, or null while there is nothing to offer. */
    savedTemplate: String? = null,
    onSaveAsTemplate: (String) -> Unit = {},
    onDismissSavedTemplate: () -> Unit = {},
    onOpenTemplate: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    state.error?.let { error ->
        val message = dataErrorMessage(error)
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }

    var editing by remember { mutableStateOf<HistorySet?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var savingAsPlan by remember { mutableStateOf(false) }
    // Reading or editing (N84). `rememberSaveable`, unlike the three above: a data class is not something a
    // Bundle can hold, but a mode the user chose is, and this activity declares no `configChanges` — so a
    // rotation would otherwise put the workout back into reading and switch every edit off under them.
    var editingWorkout by rememberSaveable { mutableStateOf(false) }

    PlanDialogs(
        savingAsPlan = savingAsPlan,
        savedTemplate = savedTemplate,
        onConfirmName = { name ->
            savingAsPlan = false
            onSaveAsTemplate(name)
        },
        onDismissName = { savingAsPlan = false },
        onDismissOffer = onDismissSavedTemplate,
        onOpenTemplate = onOpenTemplate,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                session = state.session,
                templateName = state.templateName,
                editing = editingWorkout,
                onToggleEdit = { editingWorkout = !editingWorkout },
                onDelete = { confirmingDelete = true },
                onBack = onBack,
                // Offered only when there is something to copy; the rule is enforced again by the
                // repository, which refuses an empty workout rather than making an empty plan.
                onSaveAsPlan = if (state.exercises.isEmpty()) null else { { savingAsPlan = true } },
            )
        },
    ) { innerPadding ->
        DetailContent(
            onOpenExerciseTrends = onOpenExerciseTrends,
            state = state,
            editing = editingWorkout,
            onEditSet = { editing = it },
            onDeleteSet = onDeleteSet,
            onRate = onRateExercise,
            modifier = Modifier.padding(innerPadding),
        )
    }

    editing?.let { set ->
        EditSetDialog(set = set, onDismiss = { editing = null }, onUpdateSet = onUpdateSet)
    }

    if (confirmingDelete) {
        DeleteWorkoutDialog(
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                onDeleteWorkout()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    session: WorkoutSession?,
    templateName: String?,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    /** Null when the workout has nothing to copy (ROADMAP N31). */
    onSaveAsPlan: (() -> Unit)? = null,
) {
    val date = session?.let {
        HistoryFormat.date(it.startedAt, zone = it.zoneIdOrNull() ?: ZoneId.systemDefault())
    }

    TopAppBar(
        title = {
            // The plan's name is the title and the date is what happened, under it (N84). The history
            // *list* states the same two the other way round — date first — because a list is scanned by
            // when things happened, while a single workout is named by what it was (N58). A workout with
            // no plan behind it has no name, so its date stays the title, as it always was.
            Column {
                Text(
                    text = templateName ?: date ?: stringResource(R.string.history_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag(TestTags.HISTORY_DETAIL_TITLE),
                )
                if (templateName != null && date != null) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(TestTags.HISTORY_DETAIL_DATE),
                    )
                }
            }
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
            // Only offer the menu once there is a workout for its entries to act on.
            if (session != null) {
                DetailMenu(
                    editing = editing,
                    onToggleEdit = onToggleEdit,
                    onSaveAsPlan = onSaveAsPlan,
                    onDelete = onDelete,
                )
            }
        },
    )
}

/**
 * The one ⋮ on a past workout: the mode, the copy, and the destruction (ROADMAP N84).
 *
 * N53's rule, at screen level: what writes is rare and what reads is constant, so the edits stopped being
 * live controls on every row and became one menu entry. The order and the colour are the exercise menu's
 * (`ExerciseActionsMenu`): the mode first because it is the entry reached for most, then the constructive
 * one, then the destructive one last and coloured — and the confirmation behind it stays (B2), because the
 * delete takes the workout and there is no undo to reach for.
 *
 * `rememberSaveable` for the open state, the same rule that menu follows: this activity declares no
 * `configChanges`, so rotation rebuilds it and a menu that was open would vanish mid-decision.
 */
@Composable
private fun DetailMenu(
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onSaveAsPlan: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.testTag(TestTags.HISTORY_DETAIL_MENU),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.history_detail_more),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            // The label states the state it is in, the shape the workout's own Reopen/Done uses (N7, N69):
            // a mode whose exit is another trip to this menu has to say which way it goes.
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (editing) {
                                R.string.history_detail_edit_done
                            } else {
                                R.string.history_detail_edit
                            },
                        ),
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggleEdit()
                },
                modifier = Modifier.testTag(TestTags.HISTORY_DETAIL_EDIT),
            )
            onSaveAsPlan?.let { save ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detail_save_as_plan)) },
                    onClick = {
                        menuOpen = false
                        save()
                    },
                    modifier = Modifier.testTag(TestTags.DETAIL_SAVE_AS_PLAN),
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.history_delete_workout),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
                modifier = Modifier.testTag(TestTags.HISTORY_DETAIL_DELETE),
            )
        }
    }
}

@Composable
private fun DetailContent(
    state: WorkoutDetailUiState,
    /** Whether the workout is being edited (N84), which is what the rows' write affordances answer to. */
    editing: Boolean,
    onEditSet: (HistorySet) -> Unit,
    onDeleteSet: (String) -> Unit,
    onRate: (String, Int?, List<JointPain>) -> Unit,
    onOpenExerciseTrends: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> DetailMessage(
            text = stringResource(R.string.history_loading),
            modifier = modifier,
        )

        state.notFound -> DetailMessage(
            text = stringResource(R.string.history_detail_missing),
            modifier = modifier,
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "totals") {
                Totals(state)
                HorizontalDivider()
            }
            // The readiness the workout opened with rides through history (ROADMAP N4, N62): its
            // free-text note and the muscles it named sore. Only shown when there is one of the
            // two — an empty block on every past workout would be noise, not information.
            val readinessNote = state.session?.readinessNote
            val soreMuscles = state.session?.soreMuscles.orEmpty()
            if (readinessNote != null || soreMuscles.isNotEmpty()) {
                item(key = "readiness") {
                    ReadinessBlock(note = readinessNote, soreMuscles = soreMuscles)
                    HorizontalDivider()
                }
            }
            // The workout's own comment (ROADMAP N11), on the same terms as the
            // readiness note: shown only when there is one.
            state.session?.notes?.let { comment ->
                item(key = "comment") {
                    NoteBlock(
                        title = stringResource(R.string.workout_note_row),
                        note = comment,
                    )
                    HorizontalDivider()
                }
            }
            items(items = state.exercises, key = { it.id }) { exercise ->
                    ExerciseBlock(
                        onOpenTrends = onOpenExerciseTrends,
                        exercise = exercise,
                        editing = editing,
                        onEditSet = onEditSet,
                        onDeleteSet = { onDeleteSet(it.id) },
                        onRate = onRate,
                    )
                    HorizontalDivider()
                }
        }
    }
}

/**
 * Confirms before a destructive, in-app-irreversible action.
 *
 * The delete is a soft one, so an exported backup would carry the rows — but there
 * is no restore in the UI, so from the user's side this is permanent.
 */
@Composable
private fun DeleteWorkoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_delete_confirm_title)) },
        text = { Text(stringResource(R.string.history_delete_confirm_text)) },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.history_delete_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.history_cancel))
            }
        },
    )
}

/** A free-text note on the workout: its comment (N11). */
@Composable
private fun NoteBlock(title: String, note: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = note, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * What the workout opened with (ROADMAP N4, N62): its readiness note and the muscles it named sore.
 *
 * One block rather than two, because they answered the same question and were captured in the same
 * dialog. Either half may be absent — a note with no soreness, or soreness with no note — and each
 * is simply not drawn, exactly as the note alone used to be.
 */
@Composable
private fun ReadinessBlock(
    note: String?,
    soreMuscles: List<SoreMuscle>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.readiness_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        note?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        soreMuscles.forEach { sore ->
            Text(
                text = stringResource(R.string.readiness_sore_line, sore.muscle.label, sore.score),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(TestTags.Readiness.soreLine(sore.muscle.name)),
            )
        }
    }
}

@Composable
private fun Totals(state: WorkoutDetailUiState, modifier: Modifier = Modifier) {
    val sets = pluralStringResource(R.plurals.history_sets, state.setCount, state.setCount)
    val duration = state.duration?.let { WorkoutFormat.elapsed(it) }.orEmpty()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = duration, style = MaterialTheme.typography.titleMedium)
        Text(text = sets, style = MaterialTheme.typography.bodyMedium)
        val unit = LocalWeightUnit.current
        Text(
            // The workout's total, so it follows the app setting rather than any one exercise (N64).
            text = stringResource(
                R.string.history_volume,
                Weight.format(state.volumeGrams, unit),
                unit.label(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ExerciseBlock(
    exercise: HistoryExercise,
    /** Whether this block's sets and rating can be written, or are only being read (N84). */
    editing: Boolean,
    onEditSet: (HistorySet) -> Unit,
    onDeleteSet: (HistorySet) -> Unit,
    onRate: (String, Int?, List<JointPain>) -> Unit,
    onOpenTrends: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.set_edit_action)
    val trendsLabel = stringResource(R.string.exercise_trends_open)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .testTag(TestTags.historyExerciseTrends(exercise.id))
                .clickable(onClickLabel = trendsLabel) { onOpenTrends(exercise.exerciseId) },
        )

        exercise.sets.forEachIndexed { index, set ->
            HistorySetRow(
                set = set,
                number = index + 1,
                editLabel = editLabel,
                editable = editing,
                onEditSet = onEditSet,
                onDeleteSet = onDeleteSet,
            )
        }

        ExerciseRatingSection(
            muscleFeel = exercise.muscleFeel,
            joints = exercise.joints,
            legacyJointPain = exercise.jointPain,
            legacyJointPainNote = exercise.jointPainNote,
            // Read-only while reading (N84). N8 and N50 went out of their way to make the ratings something
            // the lifter opens, so what stays on the screen is the *reading* of them — a summary with no
            // tap — rather than a block that disappears behind the mode. The exercise's name above is not
            // gated either: it opens that movement's trends, a way out rather than a write.
            onRate = if (editing) {
                { feel, joints -> onRate(exercise.id, feel, joints) }
            } else {
                null
            },
        )
    }
}


/** One logged set on the detail screen: its numbers, its N6 extras, and its delete. */
@Composable
private fun HistorySetRow(
    set: HistorySet,
    number: Int,
    editLabel: String,
    /** False while the workout is only being read: the row stops being tappable (N84). */
    editable: Boolean,
    onEditSet: (HistorySet) -> Unit,
    onDeleteSet: (HistorySet) -> Unit,
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistorySetNumbers(
                set = set,
                number = number,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.SET_ROW)
                    // Without a label a screen reader announces the row and gives no hint that tapping it
                    // edits the set, so the label and the action both go when the workout is only being
                    // read — the rule a done exercise's sets already follow (N7, N84).
                    .let { column ->
                        if (editable) {
                            column.clickable(onClickLabel = editLabel) { onEditSet(set) }
                        } else {
                            column
                        }
                    },
            )
            if (editable) {
                IconButton(onClick = { onDeleteSet(set) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.set_delete),
                    )
                }
            }
        }
    
}

/**
 * What one logged set says: its number, its load and reps, its effort and its note (N6).
 *
 * Split out of [HistorySetRow] when N84's read-only branch pushed that function past the length this
 * project allows, and it is the piece that reads on its own — the write affordances around it are the
 * other half.
 */
@Composable
private fun HistorySetNumbers(set: HistorySet, number: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.set_summary,
                    Weight.display(
                        set.weightGrams,
                        set.assistanceGrams,
                        exerciseWeightUnit(set.weightUnit),
                    ),
                    exerciseWeightUnit(set.weightUnit).label(),
                    set.reps,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            // The detail is where the full text lives (N6); the workout row only carries a marker. A
            // warm-up carries no effort to read back (N67).
            set.rpeHalves?.takeIf { set.setType.recordsEffort }?.let { rpeHalves ->
                Text(
                    text = rpeMarker(rpeHalves),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        set.note?.let { comment ->
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailMessage(text: String, modifier: Modifier = Modifier) {
    CenteredMessage(text = text, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailScreenPreview() {
    AndroidAppTheme {
        WorkoutDetailScreen(
            state = WorkoutDetailUiState(
                isLoading = false,
                session = WorkoutSession(
                    id = "a",
                    startedAt = Instant.parse("2026-09-28T07:00:00Z"),
                    finishedAt = Instant.parse("2026-09-28T08:05:00Z"),
                    readinessNote = "Slept badly, legs heavy",
                ),
                exercises = listOf(
                    HistoryExercise(
                        id = "se1",
                        exerciseId = "back-squat",
                        name = "Back Squat",
                        sets = listOf(
                            HistorySet("1", reps = 5, weightGrams = 100_000),
                            HistorySet("2", reps = 5, weightGrams = 100_000),
                        ),
                    ),
                ),
            ),
            onUpdateSet = { _, _, _, _, _, _, _ -> },
            onDeleteSet = {},
            onRateExercise = { _, _, _ -> },
            onDeleteWorkout = {},
            onBack = {},
        )
    }
}

/**
 * The editor for one logged set (ROADMAP N6), and the reason it is its own composable: the
 * screen around it is at the length this project allows, and the parameters it must be given
 * are the ones B14 showed matter — the stored role and assistance among them.
 */
@Composable
private fun EditSetDialog(
    set: HistorySet,
    onDismiss: () -> Unit,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SetEditorDialog(
        modifier = modifier,
        initialUnit = exerciseWeightUnit(set.weightUnit),
        initialReps = set.reps,
        initialWeightGrams = set.weightGrams,
        initialRpe = set.rpeHalves,
        initialNote = set.note,
        initialSetType = set.setType,
        initialAssistanceGrams = set.assistanceGrams,
        // The exercise's own step, so the ± buttons here move the way they do in the workout this set
        // came from (ROADMAP N77, B65): the same correction made in two places must step the same.
        initialStepGrams = set.stepGrams,
        onDismiss = onDismiss,
        onSave = { edit ->
            onUpdateSet(
                set.id,
                edit.reps,
                edit.weightGrams,
                edit.rpeHalves,
                edit.note,
                edit.setType,
                edit.assistanceGrams,
            )
            onDismiss()
        },
    )
}

/** The two dialogs this screen can show around saving a plan, together (ROADMAP N31). */
@Composable
private fun PlanDialogs(
    savingAsPlan: Boolean,
    savedTemplate: String?,
    onConfirmName: (String) -> Unit,
    onDismissName: () -> Unit,
    onDismissOffer: () -> Unit,
    onOpenTemplate: (String) -> Unit,
) {
    if (savingAsPlan) {
        SaveAsPlanDialog(onDismiss = onDismissName, onConfirm = onConfirmName)
    }
    savedTemplate?.let { templateId ->
        SavedAsPlanDialog(
            onDismiss = onDismissOffer,
            onOpen = {
                onDismissOffer()
                onOpenTemplate(templateId)
            },
        )
    }
}
