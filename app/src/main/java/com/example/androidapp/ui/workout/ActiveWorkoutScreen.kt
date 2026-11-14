package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.VolumeReason
import com.example.androidapp.domain.model.VolumeSuggestion
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.ProgressionDialog
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.SetEditorDialog
import com.example.androidapp.ui.components.WriteOutcome
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.theme.AndroidAppTheme

@Suppress("LongMethod")
@Composable
fun ActiveWorkoutRoute(
    onAddExercise: () -> Unit,
    onSubstituteExercise: (String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    /** Where a stall decision goes (N310): the plan editor with the target it must open. */
    onOpenPlan: (PlanHandOff) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Finishing closes the session; a stall notice still has to be read, so this is what leaves the
    // screen once it is dismissed (N305).
    LeaveWhenClosed(closed = viewModel.closed.collectAsStateWithLifecycle().value, onLeave = onDone)
    // The finish's stall decision leaves for the plan editor, which owns the edit and its undo (N310).
    val planHandOff by viewModel.planHandOff.collectAsStateWithLifecycle()
    val currentOnOpenPlan by rememberUpdatedState(onOpenPlan)
    LaunchedEffect(planHandOff) {
        planHandOff?.let {
            viewModel.onPlanHandOffHandled()
            currentOnOpenPlan(it)
        }
    }

    // A State object rather than unwrapped with `by`: reading it here would recompose everything below
    // once a second, which is the whole point of F16. Only the header and the rest bar read it.
    val clock = viewModel.clock.collectAsStateWithLifecycle()
    val restCueEnabled by viewModel.restCueEnabled.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val restTimerEnabled by viewModel.restTimerEnabled.collectAsStateWithLifecycle()
    val defaultRestSeconds by viewModel.defaultRestSeconds.collectAsStateWithLifecycle()
    val pendingProgression by viewModel.pendingProgression.collectAsStateWithLifecycle()
    RestCueAndScreenOn(clock, restCueEnabled, keepScreenOn, restTimerEnabled)
    val stallNotice by viewModel.stallNotice.collectAsStateWithLifecycle()
    val volumeSuggestion by viewModel.volumeSuggestion.collectAsStateWithLifecycle()
    val volumeUndo by viewModel.volumeUndo.collectAsStateWithLifecycle()
    val calibrationNotice by viewModel.calibrationNotice.collectAsStateWithLifecycle()
    val personalRecord by viewModel.personalRecord.collectAsStateWithLifecycle()
    val autoStep by viewModel.autoStep.collectAsStateWithLifecycle()
    val setWriteOutcome by viewModel.setWriteOutcome.collectAsStateWithLifecycle()

    ActiveWorkoutScreen(
        state = state,
        clock = clock,
        onAddExercise = onAddExercise,
        onSubstituteExercise = onSubstituteExercise,
        onSetActivity = viewModel::onSetActivity,
        onSetActivityMetrics = viewModel::onSetActivityMetrics,
        onLogSet = viewModel::onLogSet,
        onToggleSuperset = viewModel::onToggleSuperset,
        stallNotice = stallNotice,
        onDismissStallNotice = viewModel::onDismissStallNotice,
        onDeloadStall = viewModel::onDeloadStall,
        onReplaceStall = viewModel::onReplaceStall,
        volumeSuggestion = volumeSuggestion,
        volumeUndo = volumeUndo,
        onAddVolume = viewModel::onAddVolume,
        onUndoVolume = viewModel::onUndoVolume,
        onDismissVolume = viewModel::onDismissVolume,
        onDismissVolumeUndo = viewModel::onDismissVolumeUndo,
        calibrationNotice = calibrationNotice,
        onUseCalibration = viewModel::onUseCalibration,
        onSkipCalibration = viewModel::onSkipCalibration,
        personalRecord = personalRecord,
        autoStep = autoStep,
        onUndoAutoStep = viewModel::onUndoAutoStep,
        onDismissAutoStep = viewModel::onDismissAutoStep,
        bodyweightPrompt = viewModel.bodyweightPrompt.collectAsStateWithLifecycle().value,
        onEnterBodyweight = viewModel::onBodyweightTaken,
        onDismissBodyweight = viewModel::onDismissBodyweightPrompt,
        onUpdateSet = viewModel::onUpdateSet,
        setWriteOutcome = setWriteOutcome,
        onRemoveExercise = viewModel::onRemoveExercise,
        onMoveExercise = viewModel::onMoveExercise,
        onDeleteSet = viewModel::onDeleteSet,
        onUndoDelete = viewModel::onUndoDelete,
        onDismissUndo = viewModel::onDismissUndo,
        onSkipRest = viewModel::onSkipRest,
        onAdjustRest = viewModel::onAdjustRest,
        onSaveReadinessNote = viewModel::onSaveReadinessNote,
        onFinishExercise = viewModel::onFinishExercise,
        pendingProgression = pendingProgression,
        // One question, three operations on it: picking a step, writing them all, and declining.
        onToggleProgression = { setId, direction -> viewModel.onSelectProgression(setId, direction) },
        onSelectAllProgression = { direction -> viewModel.onSelectProgression(null, direction) },
        onSelectEveryProgression = { viewModel.onSelectProgression(null, null) },
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
        onOpenExercise = onOpenExercise,
        onFinish = viewModel::onFinish,
        onDiscard = viewModel::onDiscard,
        onBack = onBack,
        countsAgainstProgram = viewModel.startedFromProgram,
        restTimerEnabled = restTimerEnabled,
        defaultRestSeconds = defaultRestSeconds,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutUiState,
    clock: State<WorkoutClock>,
    onAddExercise: () -> Unit,
    /** The last set edit's answer, so its editor closes on success only (B284). */
    setWriteOutcome: SetWrite?,
    /** Makes the session an activity, or a workout again (ROADMAP N141). */
    onSetActivity: (Activity?) -> Unit,
    onLogSet: (String, SetEdit) -> Unit,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long, Int?) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit,
    onSkipRest: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSaveReadinessNote: (String?) -> Unit,
    onFinishExercise: (String) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onUndoFinishExercise: () -> Unit,
    onDismissFinishUndo: () -> Unit,
    onReopenExercise: (String) -> Unit,
    /** Pushes the exercise's own screen from inside a running session (ROADMAP N234). */
    onOpenExercise: (String) -> Unit,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    /** Moves one exercise one place in the session's own order (ROADMAP N54). */
    onMoveExercise: (String, Int) -> Unit,
    onToggleSuperset: (String) -> Unit,
    onDismissStallNotice: () -> Unit,
    /** The finish's two plan decisions; both hand off to the plan editor (N310). */
    onDeloadStall: (PlanStall) -> Unit,
    onReplaceStall: (PlanStall) -> Unit,
    /** The find-the-weight prompt at the finish, before the stall notice (N309). */
    calibrationNotice: List<CalibrationNotice>?,
    onUseCalibration: (CalibrationNotice) -> Unit,
    onSkipCalibration: (CalibrationNotice) -> Unit,
    onEnterBodyweight: (Long) -> Unit,
    onDismissBodyweight: () -> Unit,
    /** Picks one set's step, or takes the pick back when it is already there (ROADMAP N74). */
    onToggleProgression: (String, ProgressionDirection) -> Unit,
    /** Picks one direction on every set of the question that offers it (ROADMAP N74). */
    onSelectAllProgression: (ProgressionDirection) -> Unit,
    /** Picks each set's own step, whatever direction it earned (P3.17). */
    onSelectEveryProgression: () -> Unit,
    /** Writes every pick and closes the exercise (ROADMAP N74). */
    onConfirmProgression: () -> Unit,
    /** Declines the whole question and closes the exercise (ROADMAP N74). */
    onDismissProgression: () -> Unit,
    modifier: Modifier = Modifier,
    /** The plan's earned set at *Done* (N311), its undo, and the two dismissals. */
    volumeSuggestion: VolumeSuggestion? = null,
    volumeUndo: String? = null,
    onAddVolume: () -> Unit = {},
    onUndoVolume: () -> Unit = {},
    onDismissVolume: () -> Unit = {},
    onDismissVolumeUndo: () -> Unit = {},
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
    /** The stalled lifts a just-finished workout says, or null (ROADMAP N305). */
    stallNotice: List<PlanStall>? = null,
    personalRecord: PersonalRecordMoment? = null,
    /** The steps auto progression just wrote, or null when there is nothing to read back (N299). */
    autoStep: AutoProgressionMoment? = null,
    onUndoAutoStep: () -> Unit = {},
    onDismissAutoStep: () -> Unit = {},
    bodyweightPrompt: PendingBodyweight? = null,
    /**
     * The progression question a *Done* froze, or null while none is open (ROADMAP N74).
     *
     * Non-null is what draws it: the ViewModel decides whether tapping *Done* opens one at all, so
     * neither the setting (N66) nor "there is no plan" (N50) is re-decided here.
     */
    pendingProgression: PendingProgression? = null,
    /** Opens the picker to replace an exercise that has no logged sets (N291). */
    onSubstituteExercise: (String) -> Unit = {},
    /** Stores an activity's optional numbers (N293). */
    onSetActivityMetrics: (Int?, Long?) -> Unit = { _, _ -> },
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // The set being edited, by id (B283): a `SetRow` is not saveable, and a rotation must not close a
    // dialog whose own fields are `rememberSaveable`. The row is looked up from the state that has it.
    var editingSetId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = state.exercises.flatMap { it.sets }.firstOrNull { it.id == editingSetId }

    if (calibrationNotice != null) {
        CalibrationDialog(
            notices = calibrationNotice,
            onUse = onUseCalibration,
            onSkip = onSkipCalibration,
        )
    }
    if (stallNotice != null) {
        WorkoutStallDialog(
            stalls = stallNotice,
            onDone = onDismissStallNotice,
            onDeload = onDeloadStall,
            onReplace = onReplaceStall,
        )
    }
    BodyweightPrompt(prompt = bodyweightPrompt, onSave = onEnterBodyweight, onDismiss = onDismissBodyweight)

    UndoOffers(state, snackbarHostState, onUndoDelete, onDismissUndo, onUndoFinishExercise, onDismissFinishUndo)

    // The plan's earned set (N311): one snackbar, Add or dismiss; an accepted Add leaves the same
    // snackbar holding its undo. The message names the exercise and the plan's own count.
    val suggestion = volumeSuggestion
    val volumeMessage = when (suggestion?.reason) {
        VolumeReason.EXTRA_SET -> stringResource(
            R.string.volume_extra_message,
            suggestion.exerciseName,
            suggestion.plannedSets,
        )

        VolumeReason.COMFORTABLE -> stringResource(R.string.volume_comfortable_message, suggestion.exerciseName)

        null -> null
    }
    val volumeAddLabel = stringResource(R.string.volume_add)
    val volumeAddedMessage = stringResource(R.string.volume_added_message)
    val volumeUndoLabel = stringResource(R.string.volume_undo)
    val currentAddVolume by rememberUpdatedState(onAddVolume)
    val currentDismissVolume by rememberUpdatedState(onDismissVolume)
    val currentUndoVolume by rememberUpdatedState(onUndoVolume)
    val currentDismissVolumeUndo by rememberUpdatedState(onDismissVolumeUndo)
    LaunchedEffect(volumeMessage) {
        if (volumeMessage == null) return@LaunchedEffect
        when (
            snackbarHostState.showSnackbar(
                message = volumeMessage,
                actionLabel = volumeAddLabel,
                duration = SnackbarDuration.Long,
            )
        ) {
            SnackbarResult.ActionPerformed -> currentAddVolume()
            SnackbarResult.Dismissed -> currentDismissVolume()
        }
    }
    LaunchedEffect(volumeUndo) {
        if (volumeUndo == null) return@LaunchedEffect
        when (snackbarHostState.showSnackbar(volumeAddedMessage, actionLabel = volumeUndoLabel)) {
            SnackbarResult.ActionPerformed -> currentUndoVolume()
            SnackbarResult.Dismissed -> currentDismissVolumeUndo()
        }
    }

    // Whether the activity picker is open: transient UI, so it lives beside the row that opens it (N141).
    var choosingActivity by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WorkoutTopBarSection(
                state = state,
                countsAgainstProgram = countsAgainstProgram,
                onFinish = onFinish,
                onDiscard = onDiscard,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        WorkoutBody(
            state = state,
            clock = clock,
            onLogSet = onLogSet,
            onRemoveExercise = onRemoveExercise,
            onMoveExercise = onMoveExercise,
            onEditSet = { row -> editingSetId = row.id },
            onDeleteSet = onDeleteSet,
            onSkipRest = onSkipRest,
            onAdjustRest = onAdjustRest,
            onSaveReadinessNote = onSaveReadinessNote,
            onFinishExercise = onFinishExercise,
            onRateExercise = onRateExercise,
            onReopenExercise = onReopenExercise,
            onOpenExercise = onOpenExercise,
            onAddExercise = onAddExercise,
            onSubstituteExercise = onSubstituteExercise,
            onSetActivity = onSetActivity,
            onSetActivityMetrics = onSetActivityMetrics,
            onAddActivity = { choosingActivity = true },
            personalRecord = personalRecord,
            autoStep = autoStep,
            onUndoAutoStep = onUndoAutoStep,
            onDismissAutoStep = onDismissAutoStep,
            onToggleSuperset = onToggleSuperset,
            restTimerEnabled = restTimerEnabled,
            defaultRestSeconds = defaultRestSeconds,
            // The IME inset is added to the body, not the whole Scaffold: the window is edge-to-edge
            // (`enableEdgeToEdge`, targetSdk 37), so it does **not** resize for the keyboard and nothing
            // else lifts the fields clear of it — a set's fields further down the list stayed under the
            // keyboard however far the list was scrolled (B119).
            modifier = Modifier.padding(innerPadding).imePadding(),
        )
    }

    ActivityPicker(open = choosingActivity, onPick = onSetActivity, onDismiss = { choosingActivity = false })

    SetEditorSection(editing, state.exercises, onUpdateSet, setWriteOutcome) { editingSetId = null }

    // One host, from the frozen state, so a rotation mid-answer reopens it on the picks (N74).
    pendingProgression?.let {
        ProgressionDialog(
            it,
            onToggleProgression,
            onSelectAllProgression,
            onSelectEveryProgression,
            onConfirmProgression,
            onDismissProgression,
        )
    }
}

/**
 * The dialog for one logged set, split out so the screen stays a scaffold.
 *
 * The *open* state stays with the screen, because the body is what opens it; this is
 * only the rendering, which is where the lines were.
 */

@Composable
internal fun SetEditorSection(
    editing: SetRow?,
    /** The session's exercises, so the row being edited can be placed among them (N145). */
    exercises: List<SessionExerciseRow>,
    onUpdateSet: (String, Int, Long, Int?, String?, SetType, Long, Int?) -> Unit,
    /** The last set edit's answer, so its editor closes on success only (B284). */
    setWriteOutcome: SetWrite? = null,
    onDismiss: () -> Unit,
) {
    val set = editing ?: return
    // Only an answer to *this* opening of the dialog closes it (B284): the outcome stays on the ViewModel
    // after a written set, so an effect keyed by the value alone would close the next opening on its first
    // frame. `saving` is set by the tap and cleared by the answer — and survives a rotation, because a
    // write that lands across one still has to be able to close the dialog it belongs to (B340).
    var saving by rememberSaveable(set.id) { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(setWriteOutcome) {
        if (saving && setWriteOutcome == WriteOutcome.Written(set.id)) {
            saving = false
            dismiss()
        }
    }
    // The live sets **above** the one being edited, in stored order — which is what a rung's anchor question is
    // asked of. The exercise is whichever one holds the row, since a row carries no back-reference to it.
    val exercise = exercises.firstOrNull { exercise -> exercise.sets.any { it.id == set.id } }
    val above = exercise
        ?.sets
        ?.takeWhile { it.id != set.id }
        .orEmpty()

    SetEditorDialog(
        saveKey = set.id,
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
        // The set's own shape, or a hold would open as a counted set with no reps to save (N220), and the
        // movement's own meaning, or its weight field would ask for a load the wrong way (B245).
        initialDurationSeconds = set.durationSeconds,
        initialBodyweight = exercise?.isBodyweight == true,
        // The write boundary's own question, asked before the tap: a rung needs a working set directly above
        // it, and this dialog now says so by not offering the role rather than by refusing the save (N145).
        offers = { role -> above.refusalForLoggingAs(role) == null },
        onDismiss = onDismiss,
        onSave = { edit ->
            saving = true
            // No dismiss here (B284): the write answers through `setWriteOutcome`, so a refusal keeps the
            // dialog and its values — the goal editor's rule (B234).
            onUpdateSet(
                set.id,
                edit.reps,
                edit.weightGrams,
                edit.rpeHalves,
                edit.note,
                edit.setType,
                edit.assistanceGrams,
                edit.durationSeconds,
            )
        },
    )
}

@Suppress("LongMethod")
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
    onSaveReadinessNote: (String?) -> Unit,
    onFinishExercise: (String) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: (String) -> Unit,
    /** Pushes the exercise's own screen from inside a running session (ROADMAP N234). */
    onOpenExercise: (String) -> Unit,
    onAddExercise: () -> Unit,
    onSubstituteExercise: (String) -> Unit,
    onSetActivity: (Activity?) -> Unit,
    onSetActivityMetrics: (Int?, Long?) -> Unit,
    onAddActivity: () -> Unit,
    onToggleSuperset: (String) -> Unit,
    modifier: Modifier = Modifier,
    personalRecord: PersonalRecordMoment? = null,
    autoStep: AutoProgressionMoment? = null,
    onUndoAutoStep: () -> Unit = {},
    onDismissAutoStep: () -> Unit = {},
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The app's own write sits above the work with its undo, before the record: a step the
        // lifter did not tap should be the first thing they can take back (N299).
        autoStep?.let {
            AutoProgressionBanner(
                moment = it,
                onUndo = onUndoAutoStep,
                onDismiss = onDismissAutoStep,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // The record sits above the work, not in a dialog: it happens *between* sets, and
        // anything that has to be dismissed is in the way there (ROADMAP N23).
        personalRecord?.let {
            PersonalRecordBanner(
                record = it,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        state.error?.let { error ->
            Text(
                text = dataErrorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                // Said, not only drawn (B290): a write that failed is a state change, and the rest of the
                // app's failures go through a snackbar, which is a polite live region.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        when {
            state.isLoading -> CenteredMessage(stringResource(R.string.active_workout_loading), showSpinner = true)

            state.hasNoSession -> CenteredMessage(stringResource(R.string.active_workout_none), showSpinner = false)

            else -> {
                // The clock stays above the list, and so does the rest bar below it (N82): both are true
                // while the work is scrolled somewhere else — the countdown especially, since the set it
                // belongs to is off screen by then. The readiness row is not that, so it moved into the
                // list and scrolls with the exercises, which is the whole of the request.
                WorkoutHeader(startedAt = state.startedAt, clock = clock)
                // Off means no countdown at all (N44): the per-exercise prescription is drawn in the list
                // instead, so the timer is genuinely not running.
                RestBar(clock = clock, onSkip = onSkipRest, onAdjust = onAdjustRest, enabled = restTimerEnabled)
                HorizontalDivider()

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
                    onOpenExercise = onOpenExercise,
                    onSubstituteExercise = onSubstituteExercise,
                    onToggleSuperset = onToggleSuperset,
                    restTimerEnabled = restTimerEnabled,
                    defaultRestSeconds = defaultRestSeconds,
                    header = { ReadinessSection(note = state.readinessNote, onSave = onSaveReadinessNote) },
                    empty = {
                        ActivityBodyOrEmpty(
                            activity = state.activity,
                            durationSeconds = state.activityDurationSeconds,
                            distanceMeters = state.activityDistanceMeters,
                            onSaveMetrics = onSetActivityMetrics,
                            onClearActivity = { onSetActivity(null) },
                        )
                    },
                    // Below the last exercise rather than over it (N82) — and drawn for an empty session
                    // too, which is drawn *instead of* the rows and had no other way to add a movement.
                    footer = {
                        ActivityAddRows(
                            activity = state.activity,
                            isEmpty = state.isEmpty,
                            onAddExercise = onAddExercise,
                            onAddActivity = onAddActivity,
                        )
                    },
                )
            }
        }
    }
}

/**
 * The preview's session: one exercise with two logged sets.
 *
 * A property rather than a literal inside the preview, because the sample data plus the screen's now-required
 * callbacks is longer than the function length this project allows.
 */
private val previewWorkoutState = ActiveWorkoutUiState(
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
)

@Preview(showBackground = true)
@Composable
private fun ActiveWorkoutScreenPreview() {
    AndroidAppTheme {
        ActiveWorkoutScreen(
            setWriteOutcome = null,
            state = previewWorkoutState,
            clock = remember {
                mutableStateOf(WorkoutClock(elapsed = "12:05", restSecondsRemaining = 83))
            },
            onAddExercise = {},
            onSetActivity = {},
            onLogSet = { _, _ -> },
            onUpdateSet = { _, _, _, _, _, _, _, _ -> },
            onRemoveExercise = {},
            onDeleteSet = {},
            onUndoDelete = {},
            onDismissUndo = {},
            onSkipRest = {},
            onAdjustRest = {},
            onSaveReadinessNote = {},
            onFinishExercise = { _ -> },
            onRateExercise = { _, _, _ -> },
            onUndoFinishExercise = {},
            onDismissFinishUndo = {},
            onReopenExercise = {},
            onOpenExercise = {},
            onFinish = {},
            onDiscard = {},
            onBack = {},
            onMoveExercise = { _, _ -> },
            onToggleSuperset = { _ -> },
            onDismissStallNotice = {},
            onDeloadStall = {},
            onReplaceStall = {},
            calibrationNotice = null,
            onUseCalibration = {},
            onSkipCalibration = {},
            onEnterBodyweight = { _ -> },
            onDismissBodyweight = {},
            onToggleProgression = { _, _ -> },
            onSelectAllProgression = { _ -> },
            onSelectEveryProgression = {},
            onConfirmProgression = {},
            onDismissProgression = {},
        )
    }
}
