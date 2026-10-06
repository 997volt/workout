package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.Weight
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SoreMuscle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.SetEditorDialog
import com.example.androidapp.ui.components.WorkoutNoteDialog
import com.example.androidapp.ui.components.ReadinessNoteDialog
import com.example.androidapp.ui.components.ProgressionDialog
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme

@Composable
fun ActiveWorkoutRoute(
    onAddExercise: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The plans a running workout can still look at, from its own overflow (ROADMAP N78). */
    onOpenTemplates: () -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()

    // Finishing closes the session; the review still has to be read, so this is what leaves
    // the screen once it is dismissed (N20).
    LeaveWhenClosed(closed = closed, onLeave = onDone)

    // Kept as a State object rather than unwrapped with `by`: reading it here would
    // recompose this composable — and everything below it — once a second, which is
    // the whole point of F16. Only the header and the rest bar read it.
    val clock = viewModel.clock.collectAsStateWithLifecycle()
    val restCueEnabled by viewModel.restCueEnabled.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val restTimerEnabled by viewModel.restTimerEnabled.collectAsStateWithLifecycle()
    val defaultRestSeconds by viewModel.defaultRestSeconds.collectAsStateWithLifecycle()
    val pendingProgression by viewModel.pendingProgression.collectAsStateWithLifecycle()
    RestCueAndScreenOn(clock, restCueEnabled, keepScreenOn, restTimerEnabled)
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val personalRecord by viewModel.personalRecord.collectAsStateWithLifecycle()

    ActiveWorkoutScreen(
        state = state,
        clock = clock,
        onAddExercise = onAddExercise,
        onLogSet = viewModel::onLogSet,
        onToggleSuperset = viewModel::onToggleSuperset,
        summary = summary,
        onDismissSummary = viewModel::onDismissSummary,
        personalRecord = personalRecord,
        onUpdateSet = viewModel::onUpdateSet,
        onRemoveExercise = viewModel::onRemoveExercise,
        onMoveExercise = viewModel::onMoveExercise,
        onDeleteSet = viewModel::onDeleteSet,
        onUndoDelete = viewModel::onUndoDelete,
        onDismissUndo = viewModel::onDismissUndo,
        onSkipRest = viewModel::onSkipRest,
        onAdjustRest = viewModel::onAdjustRest,
        onSaveReadinessNote = viewModel::onSaveReadinessNote,
        onDismissReadinessPrompt = viewModel::onDismissReadinessPrompt,
        onFinishExercise = viewModel::onFinishExercise,
        pendingProgression = pendingProgression,
        // One question, three operations on it: picking a step, writing them all, and declining.
        onToggleProgression = { setId, direction -> viewModel.onSelectProgression(setId, direction) },
        onSelectAllProgression = { direction -> viewModel.onSelectProgression(null, direction) },
        onConfirmProgression = viewModel::onConfirmProgression,
        onDismissProgression = { pendingProgression?.let { viewModel.onFinishExercise(it.sessionExerciseId) } },
        onRateExercise = viewModel::onRateExercise,
        // The undo is reopening, addressed by the id the state already carries — the
        // wrapper that used to sit here was a second name for one operation (N24).
        onUndoFinishExercise = {
            state.pendingFinishedExerciseId?.let(viewModel::onReopenExercise)
        },
        onDismissFinishUndo = viewModel::onDismissFinishUndo,
        onReopenExercise = viewModel::onReopenExercise,
        onFinish = viewModel::onFinish,
        onDiscard = viewModel::onDiscard,
        onBack = onBack,
        onOpenTemplates = onOpenTemplates,
        onOpenPrograms = onOpenPrograms,
        countsAgainstProgram = viewModel.startedFromProgram,
        restTimerEnabled = restTimerEnabled,
        defaultRestSeconds = defaultRestSeconds,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutUiState,
    clock: State<WorkoutClock>,
    onAddExercise: () -> Unit,
    onLogSet: (String, SetEdit) -> Unit,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSaveReadinessNote: (String?, List<SoreMuscle>) -> Unit,
    onDismissReadinessPrompt: () -> Unit,
    onFinishExercise: (String) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onUndoFinishExercise: () -> Unit,
    onDismissFinishUndo: () -> Unit,
    onReopenExercise: (String) -> Unit,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The two plan screens reachable from the overflow while a session runs (ROADMAP N78). */
    onOpenTemplates: () -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    /** Moves one exercise one place in the session's own order (ROADMAP N54). */
    onMoveExercise: (String, Int) -> Unit = { _, _ -> },
    onToggleSuperset: (String) -> Unit = {},
    summary: WorkoutReview? = null,
    onDismissSummary: () -> Unit = {},
    personalRecord: PersonalRecordMoment? = null,
    /**
     * True when dropping out is recorded against a program rather than reading as no workout at
     * all (ROADMAP N41): the discard prompt says so, because only a finished session settles a
     * scheduled occurrence (P3.5).
     */
    countsAgainstProgram: Boolean = false,
    /**
     * Whether a rest is counted down (ROADMAP N44). Off shows each exercise's own prescription as a
     * fixed label instead — no countdown, no ±15s, no chime.
     */
    restTimerEnabled: Boolean = true,
    /** The fallback the static prescription label uses when an exercise prescribes no rest (N44). */
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
    /**
     * The progression question a *Done* froze, or null while none is open (ROADMAP N74).
     *
     * Non-null is what draws it: the ViewModel decides whether tapping *Done* opens one at all, so
     * neither the setting (N66) nor "there is no plan" (N50) is re-decided here.
     */
    pendingProgression: PendingProgression? = null,
    /** Picks one set's step, or takes the pick back when it is already there (ROADMAP N74). */
    onToggleProgression: (String, ProgressionDirection) -> Unit = { _, _ -> },
    /** Picks one direction on every set of the question that offers it (ROADMAP N74). */
    onSelectAllProgression: (ProgressionDirection) -> Unit = {},
    /** Writes every pick and closes the exercise (ROADMAP N74). */
    onConfirmProgression: () -> Unit = {},
    /** Declines the whole question and closes the exercise (ROADMAP N74). */
    onDismissProgression: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // The set being edited, held here so the caller does not have to track it.
    var editing by remember { mutableStateOf<SetRow?>(null) }

    if (summary != null) WorkoutReviewDialog(summary = summary, onDismiss = onDismissSummary)

    UndoOffers(state, snackbarHostState, onUndoDelete, onDismissUndo, onUndoFinishExercise, onDismissFinishUndo)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WorkoutTopBar(
                canFinish = state.exercises.any { it.sets.isNotEmpty() },
                // The empty workout already offers a prompt-free discard inside its own body; this
                // is the exit for a workout that holds something (ROADMAP N41).
                canDiscard = state.sessionId != null && !state.isEmpty,
                // The menu is drawn whenever a session is open, because the plan entries live in it
                // and an empty workout must still reach them (N78).
                showMenu = state.sessionId != null,
                setCount = state.exercises.sumOf { it.sets.size },
                countsAgainstProgram = countsAgainstProgram,
                onFinish = onFinish,
                onDiscard = onDiscard,
                onBack = onBack,
                onOpenTemplates = onOpenTemplates,
                onOpenPrograms = onOpenPrograms,
            )
        },
        floatingActionButton = {
            AddExerciseButton(onClick = onAddExercise)
        },
    ) { innerPadding ->
        WorkoutBody(
            state = state,
            clock = clock,
            onLogSet = onLogSet,
            onRemoveExercise = onRemoveExercise,
            onMoveExercise = onMoveExercise,
            onEditSet = { row -> editing = row },
            onDeleteSet = onDeleteSet,
            onSkipRest = onSkipRest,
            onAdjustRest = onAdjustRest,
            onSaveReadinessNote = onSaveReadinessNote,
            onDismissReadinessPrompt = onDismissReadinessPrompt,
            onFinishExercise = onFinishExercise,
            onRateExercise = onRateExercise,
            onReopenExercise = onReopenExercise,
            onDiscard = onDiscard,
            personalRecord = personalRecord,
            onToggleSuperset = onToggleSuperset,
            restTimerEnabled = restTimerEnabled,
            defaultRestSeconds = defaultRestSeconds,
            modifier = Modifier.padding(innerPadding),
        )
    }

    SetEditorSection(
        set = editing,
        onUpdateSet = onUpdateSet,
        onDismiss = { editing = null },
    )

    // One host, from the frozen state, so a rotation mid-answer reopens it on the picks (N74).
    pendingProgression?.let {
        ProgressionDialog(it, onToggleProgression, onSelectAllProgression, onConfirmProgression, onDismissProgression)
    }
}

/**
 * The one action a workout always offers: adding a movement (ROADMAP F1).
 *
 * Split out of the screen when N66's switch pushed it over the length this project allows, and it is
 * the piece that reads on its own: an extended FAB whose whole content is its own label and glyph.
 */
@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        text = { Text(stringResource(R.string.active_workout_add_exercise)) },
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
    )
}

/**
 * The dialog for one logged set, split out so the screen stays a scaffold.
 *
 * The *open* state stays with the screen, because the body is what opens it; this is
 * only the rendering, which is where the lines were.
 */
@Composable
private fun SetEditorSection(
    set: SetRow?,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (set == null) return

    SetEditorDialog(
        initialReps = set.reps,
        initialWeightGrams = set.weightGrams,
        initialRpe = set.rpeHalves,
        initialUnit = set.weightUnit,
        initialStepGrams = set.stepGrams,
        initialNote = set.note,
        // Without these the draft starts at a plain working set with no help, and saving
        // writes that over the stored row — a one-rep correction silently destroying the role
        // and the assistance (ROADMAP B14).
        initialSetType = set.setType,
        initialAssistanceGrams = set.assistanceGrams,
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

@Composable
private fun ShowUndoSnackbar(
    pendingUndo: SetEntry?,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.set_deleted)
    val undoLabel = stringResource(R.string.set_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}

/**
 * Both undo snackbars, and the rule that keeps them honest (ROADMAP B3).
 *
 * An undo is offered only while the thing it refers to is still in the session. The
 * two share one [SnackbarHostState], so without that rule a deleted set's Undo can
 * outlive its exercise: the row is gone, the button stays, and the write it fires is
 * refused. Filtering here is the dismissal; the ViewModel reports if a tap still
 * races through.
 */
@Composable
private fun UndoOffers(
    state: ActiveWorkoutUiState,
    hostState: SnackbarHostState,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
    onUndoFinishExercise: () -> Unit,
    onDismissFinishUndo: () -> Unit,
) {
    ShowUndoSnackbar(
        pendingUndo = state.undoableSet,
        hostState = hostState,
        onUndo = onUndoDelete,
        onDismiss = onDismissUndo,
    )
    ShowFinishSnackbar(
        state = state,
        hostState = hostState,
        onUndo = onUndoFinishExercise,
        onDismiss = onDismissFinishUndo,
    )
}

/**
 * Undo for the exercise just marked done (ROADMAP N7).
 *
 * The same argument as the deleted-set snackbar: "Done" is accident protection, so
 * it needs a way back — and the exercise is only dimmed, never removed, so the undo
 * is a plain reopen rather than a re-insert.
 */
@Composable
private fun ShowFinishSnackbar(
    state: ActiveWorkoutUiState,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Null once the exercise is gone, which keys the effect below to dismiss the
    // snackbar rather than leave an Undo for a row that no longer exists (B3).
    val finishedId = state.undoableFinishedExerciseId
    val name = state.exercises.firstOrNull { it.id == finishedId }?.name
    val message = if (name == null) {
        stringResource(R.string.active_workout_exercise_done)
    } else {
        stringResource(R.string.active_workout_exercise_done_named, name)
    }
    val undoLabel = stringResource(R.string.set_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(finishedId) {
        if (finishedId == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutTopBar(
    canFinish: Boolean,
    canDiscard: Boolean,
    /** True while a session is open, which is when the plan entries are worth reaching (N78). */
    showMenu: Boolean,
    setCount: Int,
    countsAgainstProgram: Boolean,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenPrograms: () -> Unit,
) {
    // Whether the finish prompt is up. Dismissing it finishes without a comment
    // (ROADMAP N11): the user asked to finish, and the comment is optional.
    var commenting by remember { mutableStateOf(false) }
    // The discard lives behind the overflow rather than beside Finish: it is the destructive
    // action, and the top bar's visible slot is the one the user reaches for mid-workout.
    var confirmingDiscard by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(R.string.active_workout_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // Nothing to finish until at least one set is logged.
            AppTextButton(
                onClick = { commenting = true },
                enabled = canFinish,
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_FINISH),
            ) {
                Text(stringResource(R.string.active_workout_finish))
            }
            if (showMenu) {
                WorkoutMenu(
                    canDiscard = canDiscard,
                    onOpenTemplates = onOpenTemplates,
                    onOpenPrograms = onOpenPrograms,
                    onDiscard = { confirmingDiscard = true },
                )
            }
        },
    )

    if (confirmingDiscard) {
        DiscardWorkoutDialog(
            setCount = setCount,
            countsAgainstProgram = countsAgainstProgram,
            onDismiss = { confirmingDiscard = false },
            onConfirm = {
                confirmingDiscard = false
                onDiscard()
            },
        )
    }

    if (commenting) {
        WorkoutNoteDialog(
            onDismiss = {
                commenting = false
                onFinish(null)
            },
            onSave = { note ->
                commenting = false
                onFinish(note)
            },
        )
    }
}

/**
 * The workout's overflow: the two plans it can step out to, and the discard (ROADMAP N78, N41).
 *
 * The plans are here rather than on the tab bar because the bar is off this screen on purpose (N34):
 * a tab bar under a live set logger is an invitation to lose the session, while an overflow entry is
 * a deliberate step. A lifter checking what is next no longer has to end the workout to look.
 *
 * Split out because the top bar around it is at the length this project allows and because the menu's
 * open state belongs with the button that opens it. The discard is drawn only when there is something
 * to discard; the menu itself is not, because these two entries are reachable from an empty one.
 */
@Composable
private fun WorkoutMenu(
    canDiscard: Boolean,
    onOpenTemplates: () -> Unit,
    onOpenPrograms: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_MENU),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.active_workout_more),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_templates)) },
                onClick = {
                    menuOpen = false
                    onOpenTemplates()
                },
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_TEMPLATES),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_programs)) },
                onClick = {
                    menuOpen = false
                    onOpenPrograms()
                },
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_PROGRAMS),
            )
            if (canDiscard) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.active_workout_discard)) },
                    onClick = {
                        menuOpen = false
                        onDiscard()
                    },
                    modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD),
                )
            }
        }
    }
}

/**
 * The prompt a discard asks before it deletes anything (ROADMAP N41).
 *
 * It says what goes — the count, because the user is deciding whether it is worth keeping — and
 * when the workout came from a program it says the second consequence too: only a *finished*
 * session settles a scheduled occurrence (P3.5), so abandoning this one is recorded as a miss
 * rather than as no workout at all. The empty workout never reaches this dialog, because there is
 * nothing to lose and its prompt-free discard is right.
 */
@Composable
private fun DiscardWorkoutDialog(
    setCount: Int,
    countsAgainstProgram: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_workout_discard_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (setCount > 0) {
                        pluralStringResource(
                            R.plurals.active_workout_discard_text,
                            setCount,
                            setCount,
                        )
                    } else {
                        stringResource(R.string.active_workout_discard_text_no_sets)
                    },
                    modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_TEXT),
                )
                if (countsAgainstProgram) {
                    Text(
                        text = stringResource(R.string.active_workout_discard_program),
                        modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_PROGRAM),
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_CONFIRM),
            ) {
                Text(stringResource(R.string.active_workout_discard))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.ACTIVE_WORKOUT_DISCARD_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun WorkoutBody(
    state: ActiveWorkoutUiState,
    clock: State<WorkoutClock>,
    onLogSet: (String, SetEdit) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSaveReadinessNote: (String?, List<SoreMuscle>) -> Unit,
    onDismissReadinessPrompt: () -> Unit,
    onFinishExercise: (String) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: (String) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    personalRecord: PersonalRecordMoment? = null,
    onToggleSuperset: (String) -> Unit = {},
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The record sits above the work, not in a dialog: it happens *between* sets, and
        // anything that has to be dismissed is in the way there (ROADMAP N23).
        personalRecord?.let { PersonalRecordBanner(record = it) }

        state.error?.let { error ->
            Text(
                text = dataErrorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when {
            state.isLoading -> CenteredMessage(stringResource(R.string.active_workout_loading), showSpinner = true)

            state.hasNoSession -> CenteredMessage(stringResource(R.string.active_workout_none), showSpinner = false)

            else -> {
                WorkoutHeader(startedAt = state.startedAt, clock = clock)
                ReadinessSection(
                    note = state.readinessNote,
                    soreMuscles = state.readinessSoreMuscles,
                    promptVisible = state.isReadinessPromptVisible,
                    onDismissPrompt = onDismissReadinessPrompt,
                    onSave = onSaveReadinessNote,
                )
                RestBar(
                    clock = clock,
                    onSkip = onSkipRest,
                    onAdjust = onAdjustRest,
                    // Off means no countdown at all (N44): the per-exercise prescription is drawn
                    // in the list instead, so the timer is genuinely not running.
                    enabled = restTimerEnabled,
                )
                HorizontalDivider()

                if (state.isEmpty) {
                    EmptyWorkout(onDiscard = onDiscard)
                } else {
                    ExerciseList(
                        rows = state.exercises,
                        onLogSet = onLogSet,
                        onRemoveExercise = onRemoveExercise,
                        onMoveExercise = onMoveExercise,
                        onEditSet = onEditSet,
                        onDeleteSet = onDeleteSet,
                        onFinishExercise = onFinishExercise,
                        onRateExercise = onRateExercise,
                        onReopenExercise = onReopenExercise,
                        onToggleSuperset = onToggleSuperset,
                        restTimerEnabled = restTimerEnabled,
                        defaultRestSeconds = defaultRestSeconds,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestBar(
    clock: State<WorkoutClock>,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** False when the timer is switched off (ROADMAP N44), so nothing is counted or adjusted. */
    enabled: Boolean = true,
) {
    // Deciding whether to show this bar in the *parent* would recompose the exercise
    // list once a second. Returning early here keeps the tick inside this composable.
    val remaining = clock.value.restSecondsRemaining
    if (!enabled || remaining <= 0) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.rest_remaining, RestTimer.format(remaining)),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The one surface whose links are drawn *on* the filled container: the readable role
                // here is the container's own content colour, not the page's link colour (ROADMAP N49).
                AppTextButton(
                    onClick = { onAdjust(-RestTimer.ADJUST_STEP_SECONDS) },
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_subtract))
                }
                AppTextButton(
                    onClick = { onAdjust(RestTimer.ADJUST_STEP_SECONDS) },
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_add))
                }
                AppTextButton(
                    onClick = onSkip,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(stringResource(R.string.rest_skip))
                }
            }
        }
    }
}

@Composable
private fun WorkoutHeader(
    startedAt: String,
    clock: State<WorkoutClock>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.active_workout_started, startedAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The only place the elapsed time is read, so a tick stops here.
        Text(text = clock.value.elapsed, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The readiness line and the dialog behind it (ROADMAP N4).
 *
 * "A form is open" is transient UI state, so it lives here next to the row that
 * opens it. The prompt flag still comes from the session, which is why a brand-new
 * workout opens straight into the dialog while a resumed one does not.
 */
@Composable
private fun ReadinessSection(
    note: String?,
    soreMuscles: List<SoreMuscle>,
    promptVisible: Boolean,
    onDismissPrompt: () -> Unit,
    onSave: (String?, List<SoreMuscle>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }

    ReadinessRow(note = note, soreMuscles = soreMuscles, onEdit = { editing = true }, modifier = modifier)
    if (promptVisible || editing) {
        ReadinessNoteDialog(
            initialNote = note.orEmpty(),
            initialSoreMuscles = soreMuscles,
            isPrompt = promptVisible,
            onDismiss = {
                editing = false
                onDismissPrompt()
            },
            onSave = { written, sore ->
                editing = false
                onSave(written, sore)
            },
        )
    }
}

/**
 * The readiness note in the workout header (ROADMAP N4), with the sore-muscle list beside it (N62).
 *
 * Always present, so the field the prompt introduces stays reachable after the
 * prompt is skipped — a passive field nobody can find again is the failure that
 * made this a prompt in the first place.
 */
@Composable
private fun ReadinessRow(
    note: String?,
    soreMuscles: List<SoreMuscle>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.readiness_edit_action)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.READINESS_ROW)
            .clickable(onClickLabel = editLabel, onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.readiness_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = note ?: stringResource(R.string.readiness_add),
            style = MaterialTheme.typography.bodyMedium,
            color = if (note == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        // The soreness is a fact of the same readiness, so it is read in the same place — one line
        // per muscle, because "quads 8, calves 3" is what the row is for.
        soreMuscles.forEach { sore ->
            Text(
                text = stringResource(R.string.readiness_sore_line, sore.muscle.label, sore.score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(TestTags.Readiness.soreLine(sore.muscle.name)),
            )
        }
    }
}

@Composable
private fun EmptyWorkout(onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.active_workout_empty),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.active_workout_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        AppTextButton(
            onClick = onDiscard,
            modifier = Modifier.padding(top = 16.dp).testTag(TestTags.ACTIVE_WORKOUT_DISCARD_EMPTY),
        ) {
            Text(stringResource(R.string.active_workout_discard))
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ActiveWorkoutScreenPreview() {
    AndroidAppTheme {
        ActiveWorkoutScreen(
            state = ActiveWorkoutUiState(
                isLoading = false,
                sessionId = "s1",
                startedAt = "07:42",
                readinessNote = "Shoulders still sore from Monday",
                exercises = listOf(
                    SessionExerciseRow(
                        id = "a",
                        exerciseId = "back-squat",
                        name = "Back Squat",
                        subtitle = "Quads · Barbell",
                        techniqueNote = "Brace, sit back, drive the floor away",
                        restSeconds = 180,
                        sets = listOf(
                            SetRow("s1", 1, reps = 8, weightGrams = 60_000),
                            SetRow("s2", 2, reps = 8, weightGrams = 60_000),
                        ),
                        suggestion = SetSuggestion(reps = 8, weightGrams = 60_000),
                    ),
                ),
            ),
            clock = remember {
                mutableStateOf(WorkoutClock(elapsed = "12:05", restSecondsRemaining = 83))
            },
            onAddExercise = {},
            onLogSet = { _, _ -> },
            onUpdateSet = { _, _, _, _, _, _, _ -> },
            onRemoveExercise = {},
            onDeleteSet = {},
            onUndoDelete = {},
            onDismissUndo = {},
            onSkipRest = {},
            onAdjustRest = {},
            onSaveReadinessNote = { _, _ -> },
            onDismissReadinessPrompt = {},
            onFinishExercise = { _ -> },
            onRateExercise = { _, _, _ -> },
            onUndoFinishExercise = {},
            onDismissFinishUndo = {},
            onReopenExercise = {},
            onFinish = {},
            onDiscard = {},
            onBack = {},
        )
    }
}

/**
 * The record just set (ROADMAP N23).
 *
 * It says what was beaten rather than only what was done — "5 reps at 100 kg, best yet" — and
 * it says the *first* time at a rep count differently, because there was no bar to clear then
 * and claiming otherwise would be a lie the data does not support.
 */
@Composable
private fun PersonalRecordBanner(
    record: PersonalRecordMoment,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TestTags.PERSONAL_RECORD),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val unit = exerciseWeightUnit(record.weightUnit)
            Text(
                text = stringResource(
                    R.string.personal_record_title,
                    record.reps,
                    Weight.display(record.weightGrams, 0, unit),
                    unit.label(),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = record.previousBestGrams?.let { previous ->
                    stringResource(
                        R.string.personal_record_beats,
                        record.exerciseName,
                        Weight.display(previous, 0, unit),
                        unit.label(),
                    )
                } ?: stringResource(R.string.personal_record_first, record.exerciseName),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * The two things a workout asks of the device rather than of the database (ROADMAP N27).
 *
 * Both live inside the permission-free envelope N26 bought, which is recorded in DECISIONS.md:
 * keep-screen-on is a window flag rather than a wake lock, and the cue is an in-process tone plus
 * **view-level** haptics. `Vibrator` would have been the obvious call and it costs
 * `android.permission.VIBRATE` — the one permission the app just stopped declaring.
 */
@Composable
private fun RestCueAndScreenOn(
    clock: State<WorkoutClock>,
    restCueEnabled: Boolean,
    keepScreenOn: Boolean,
    restTimerEnabled: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val tone = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    // The cue is the *transition* out of resting, not the state: a rest is cued when it ends, and a
    // workout that opens with no rest must not chime.
    var wasResting by remember { mutableStateOf(false) }
    LaunchedEffect(clock) {
        snapshotFlow { clock.value.isResting }.collect { resting ->
            // Split, because four tests in one condition is where a reader stops counting: the rest
            // ended, and both switches can silence it.
            val restEnded = !resting && wasResting
            if (restEnded && restCueEnabled && restTimerEnabled) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_MILLIS)
                // `CONFIRM` is API 30 and this app supports 26; lint's InlinedApi rule caught that,
                // which is what it is for. `VIRTUAL_KEY` is the same short tick and has existed since
                // API 1.
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            wasResting = resting
        }
    }
}

/** Loud enough to hear across a gym; short enough to be a cue rather than a ringtone. */
private const val TONE_VOLUME = 70
private const val TONE_MILLIS = 250
