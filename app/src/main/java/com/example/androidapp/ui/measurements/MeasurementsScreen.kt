package com.example.androidapp.ui.measurements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SORE_MUSCLE_GROUPS
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.at
import com.example.androidapp.domain.model.statesSomething
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.DEFAULT_SCORED_PICK
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.ScoredPick
import com.example.androidapp.ui.components.ScoredPickStrings
import com.example.androidapp.ui.components.ScoredPicksEditor
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.BodyMetric
import com.example.androidapp.ui.statistics.EnergyAdjustment
import com.example.androidapp.ui.statistics.MetricKey
import com.example.androidapp.ui.statistics.MetricRegistry
import com.example.androidapp.ui.statistics.WeightGoalRow
import java.time.ZoneId

/** Measurements, reached from home (ROADMAP N32, two records since N113). */
@Composable
fun MeasurementsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * True to arrive on **today's** daily entry (ROADMAP N114, B182), from home's own row: the entry itself
     * when the day has one, and the blank daily form when it does not.
     */
    openDaily: Boolean = false,
    viewModel: MeasurementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val pendingUndo by viewModel.pendingUndo.collectAsStateWithLifecycle()
    val rateVerdict by viewModel.rateVerdict.collectAsStateWithLifecycle()
    // The shortcut opens **today's record** (B182), and only the loaded list can say whether that is an
    // entry to edit or a day with nothing on it — so it waits for the answer rather than opening a blank
    // form first, which would replace the day's numbers on save.
    if (openDaily && state.isLoading) {
        CenteredMessage(text = stringResource(R.string.history_loading), modifier = modifier)
        return
    }
    MeasurementsScreen(
        state = state,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        onBack = onBack,
        modifier = modifier,
        // A failed write is a snackbar, the surface every other screen uses (B102).
        error = error,
        onDismissError = viewModel::onDismissError,
        initialAdd = if (openDaily && state.todayEntry == null) MeasurementKind.DAILY else null,
        initialEdit = if (openDaily) state.todayEntry else null,
        // The way back out of a delete (N133), held by the ViewModel until the message is taken.
        pendingUndo = pendingUndo,
        onUndo = viewModel::onUndoDelete,
        onDismissUndo = viewModel::onDismissUndo,
        // What the weigh-in just saved says about the goal's rate (N136), the same way.
        rateVerdict = rateVerdict,
        onDismissRateVerdict = viewModel::onDismissRateVerdict,
        // The weight goal is set here now: it is a fact about the body, and this is where the body is recorded
        // (a request in use; no roadmap id covers the move).
        onSetWeightGoal = viewModel::onSetWeightGoal,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    state: MeasurementsUiState,
    onSave: (BodyMeasurement) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** A write that failed, or null (B102) — shown as a snackbar, like every other screen's. */
    error: DataError? = null,
    onDismissError: () -> Unit = {},
    /**
     * The blank form to open on arrival, or null (ROADMAP N114).
     *
     * Read once, as `rememberSaveable`'s initial value: dismissing the form sets it back to null, and a
     * rotation keeps that dismissal rather than reopening what the lifter just closed. The home shortcut
     * passes it only on a day with no entry — an entry is [initialEdit] (B182).
     */
    initialAdd: MeasurementKind? = null,
    /**
     * The record to open for editing on arrival, or null (B182).
     *
     * Resolved by the route, which can wait for the list: today's entry when the day has one, so the
     * shortcut edits the day instead of opening a blank form whose save would replace it.
     */
    initialEdit: BodyMeasurement? = null,
    /** The entry a delete just took, awaiting the snackbar's undo, or null (ROADMAP N133). */
    pendingUndo: BodyMeasurement? = null,
    onUndo: () -> Unit = {},
    onDismissUndo: () -> Unit = {},
    /**
     * What a weigh-in just said about the goal's rate, or null (ROADMAP N136).
     *
     * A **line after the save**, not a dialog: it states, it is dismissible and it does not pursue (P4.7) —
     * which is why it is a snackbar like the undo beside it, and why the ViewModel hands it over once rather
     * than deriving it from the data on show.
     */
    rateVerdict: EnergyAdjustment? = null,
    onDismissRateVerdict: () -> Unit = {},
    /** Sets the weight goal, or clears it with null (N135) — the row the statistics screen used to carry. */
    onSetWeightGoal: (WeightGoal?) -> Unit = {},
) {
    // Two steps rather than one: the record is two records, so *Add* asks which before it opens a form
    // (N113). Null is "no form open"; `choosing` is the question.
    var choosing by remember { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(initialAdd) }
    // Whether the list is in its edit mode, the one that offers a delete (N132). Saveable so a rotation
    // does not drop the lifter out of the mode they deliberately entered, or worse, back into it.
    var editing by rememberSaveable { mutableStateOf(false) }
    // The record being read, and the record the edit mode is correcting: each a look at one row rather
    // than a mode, so neither is restored after a rotation (requests in use; no roadmap id covers them).
    var openEntry by remember { mutableStateOf<BodyMeasurement?>(null) }
    var editingEntry by remember { mutableStateOf(initialEdit) }

    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(error?.let { dataErrorMessage(it) }, snackbarHostState, onDismissError)
    ShowDeletedMeasurementUndo(pendingUndo, snackbarHostState, onUndo, onDismissUndo)
    ShowRateVerdict(rateVerdict, snackbarHostState, onDismissRateVerdict)

    if (choosing) {
        AddMeasurementDialog(
            onDismiss = { choosing = false },
            onPick = { kind ->
                choosing = false
                adding = kind
            },
        )
    }
    MeasurementFormDialogs(
        adding = adding,
        editingEntry = editingEntry,
        onDismiss = {
            adding = null
            editingEntry = null
        },
        onSave = onSave,
    )
    openEntry?.let { entry ->
        MeasurementDetailsDialog(entry = entry, onDismiss = { openEntry = null })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MeasurementsTopBar(editing = editing, onToggleEditing = { editing = !editing }, onBack = onBack) },
        floatingActionButton = {
            // The default view is scroll and add, the edit mode is delete (N132): while deleting, the
            // one control that would write a new entry is out of the way.
            if (!editing) {
                FloatingActionButton(
                    onClick = { choosing = true },
                    modifier = Modifier.testTag(TestTags.Measurements.ADD),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.measurements_add),
                    )
                }
            }
        },
    ) { innerPadding ->
        MeasurementList(
            state = state,
            editing = editing,
            // In the edit mode the row opens its form — that is where the writes live — and in the
            // default view it opens the record read-only (N132, and the request in use).
            onOpen = { entry -> if (editing) editingEntry = entry else openEntry = entry },
            onDelete = onDelete,
            onSetWeightGoal = onSetWeightGoal,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The bar over the list: back, the title, and the one control that changes the mode (ROADMAP N132).
 *
 * The mode lives in the button rather than beside it: one control that says which state the list is in
 * and changes it, so entering and leaving are one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementsTopBar(editing: Boolean, onToggleEditing: () -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.measurements_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            AppTextButton(
                onClick = onToggleEditing,
                modifier = Modifier.testTag(TestTags.Measurements.EDIT),
            ) {
                Text(
                    stringResource(
                        if (editing) R.string.measurements_editing else R.string.measurements_edit,
                    ),
                )
            }
        },
    )
}

/**
 * The weight goal, and its history (N135, N137).
 *
 * A card rather than a row of its own beside the data: the goal is one statement about the series, and the row
 * it draws is the same one the statistics screen showed under its chart — moved here because a goal is set
 * where the body is recorded, not beside a drawing of the past.
 */
@Composable
private fun WeightGoalCard(state: MeasurementsUiState, onSetWeightGoal: (WeightGoal?) -> Unit) {
    AppCard {
        WeightGoalRow(
            goal = state.weightGoal,
            metric = MetricRegistry.entryFor(MetricKey.Body(BodyMetric.WEIGHT)),
            history = state.goalHistory,
            onSetGoal = onSetWeightGoal,
        )
    }
}

/**
 * The list under the app bar, with the sentence an empty one says (N32, N113).
 *
 * A `LazyColumn` with the empty sentence as an item rather than a `Column` of rows (B130): a `Column`
 * measured every entry and could not scroll, so a few weeks of daily entries were unreachable — and the
 * add button floated over whichever row happened to be last. The empty state is an *item* because an
 * empty list has no rows to hang a sentence off, and the two are mutually exclusive anyway.
 */
@Composable
private fun MeasurementList(
    state: MeasurementsUiState,
    editing: Boolean,
    onOpen: (BodyMeasurement) -> Unit,
    onDelete: (String) -> Unit,
    onSetWeightGoal: (WeightGoal?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.Measurements.LIST),
        // The last row must clear the add button, which floats over the list's foot.
        contentPadding = PaddingValues(bottom = LIST_FOOT_CLEARANCE),
    ) {
        // The goal comes before the entries: it is what those readings are *for*, and the row it draws is the
        // one the statistics screen used to carry under its chart (N135).
        item(key = GOAL_KEY) {
            WeightGoalCard(state = state, onSetWeightGoal = onSetWeightGoal)
        }
        if (state.entries.isEmpty() && !state.isLoading) {
            item(key = EMPTY_KEY) {
                Text(
                    text = stringResource(R.string.measurements_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestTags.Measurements.EMPTY).padding(16.dp),
                )
            }
        }
        items(items = state.entries, key = { it.id }) { entry ->
            MeasurementRow(
                entry = entry,
                editing = editing,
                onOpen = { onOpen(entry) },
                onDelete = { onDelete(entry.id) },
            )
        }
    }
}

/**
 * The one form the screen opens, for a new record or for the one the edit mode is correcting (N113, and a
 * request in use; no roadmap id covers it).
 *
 * Both are the same dialog: which fields it shows follows from the kind, and whether it corrects follows
 * from [editingEntry] — which is also what makes it open holding the record and keep its day and id.
 */
@Composable
private fun MeasurementFormDialogs(
    adding: MeasurementKind?,
    editingEntry: BodyMeasurement?,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit,
) {
    adding?.let { kind ->
        MeasurementDialog(
            kind = kind,
            onDismiss = onDismiss,
            onConfirm = { measurement ->
                onDismiss()
                onSave(measurement)
            },
        )
    }
    editingEntry?.let { entry ->
        MeasurementDialog(
            kind = entry.kind,
            onDismiss = onDismiss,
            onConfirm = { measurement ->
                onDismiss()
                onSave(measurement)
            },
            initial = entry,
        )
    }
}

/**
 * Which of the two records to add (ROADMAP N113).
 *
 * A question rather than one form with everything on it: a tape day has no weight to state, and a daily
 * entry has no business asking for seven sites — which is what the single form did, and why a tape-only
 * day had to invent a scale reading to save at all.
 */
@Composable
private fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onPick: (MeasurementKind) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measurements_add)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTextButton(
                    onClick = { onPick(MeasurementKind.DAILY) },
                    modifier = Modifier.testTag(TestTags.Measurements.ADD_DAILY),
                ) {
                    Text(stringResource(R.string.measurements_add_daily))
                }
                AppTextButton(
                    onClick = { onPick(MeasurementKind.TAPE) },
                    modifier = Modifier.testTag(TestTags.Measurements.ADD_TAPE),
                ) {
                    Text(stringResource(R.string.measurements_add_tape))
                }
            }
        },
        // The two choices are the whole dialog, so there is nothing for a confirm button to confirm.
        confirmButton = {},
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * One entry as a row.
 *
 * The date is rendered in the device's zone rather than the entry's own, which is the one place N25's
 * rule does not apply: a measurement is not performed anywhere in particular, and the day it belongs to
 * is already the local day the repository keyed it on. The kind leads the supporting line, so a row says
 * which of the two records it is (N113).
 *
 * **The delete is only drawn in the edit mode** (N132). A measurement is the one thing here that cannot
 * be reconstructed — a set can be logged again, a plan re-authored, but a weight on a morning is a fact
 * about the body — so the destructive action leaves the screen the lifter opened to read or add, rather
 * than sitting on every row of it.
 *
 * **The row's own tap depends on the mode**, because the two modes are two jobs: the default view reads,
 * so the row opens the record in full (its supporting line is a summary and says nothing about which
 * number is which), and the edit mode writes, so the row opens the form holding what the record already
 * states — a number left out can be added there, and a wrong one changed. The delete button beside it
 * keeps its own tap either way.
 */
@Composable
private fun MeasurementRow(
    entry: BodyMeasurement,
    editing: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = HistoryFormat.date(entry.measuredAt, zone = ZoneId.systemDefault())
    ListItem(
        headlineContent = { Text(date) },
        supportingContent = { Text(summary(entry)) },
        trailingContent = {
            if (editing) {
                AppTextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(TestTags.Measurements.delete(entry.id)),
                ) {
                    Text(stringResource(R.string.measurements_delete))
                }
            }
        },
        modifier = Modifier
            .testTag(TestTags.Measurements.row(entry.id))
            // The date names the row and never the action, so the tap says what it does — the rule every
            // row in this app follows.
            .clickable(
                onClickLabel = stringResource(
                    if (editing) R.string.measurements_edit_entry else R.string.measurements_open,
                    date,
                ),
                onClick = onOpen,
            ),
    )
}

/** The empty sentence is a keyed item of a list that may hold none (B130). */
private const val EMPTY_KEY = "measurements_empty"

/** The goal's row is the list's own first item, so it scrolls with the readings it is about. */
private const val GOAL_KEY = "measurements_weight_goal"

/** Clearance for the add button, which floats over the list's last row. */
private val LIST_FOOT_CLEARANCE = 88.dp

/** What an entry reads as: which record it is, then whichever of its own numbers were taken (N113). */
@Composable
private fun summary(entry: BodyMeasurement): String {
    val parts = mutableListOf(stringResource(kindLabel(entry.kind)))
    // The weight is the one number that may be absent, because a soreness-only morning is a real entry.
    entry.weightGrams?.let { parts += Weight.kilograms(it) }
    entry.bodyFatTenths?.let { parts += MeasurementFormat.percent(it) }
    entry.muscleTenths?.let { parts += MeasurementFormat.percent(it) }
    TapeSite.entries.forEach { site -> entry.at(site)?.let { parts += MeasurementFormat.centimetres(it) } }
    if (entry.soreMuscles.isNotEmpty()) {
        parts += entry.soreMuscles.joinToString(", ") { "${it.muscle.label} ${it.score}" }
    }
    return parts.joinToString(" · ")
}

internal fun kindLabel(kind: MeasurementKind): Int = when (kind) {
    MeasurementKind.DAILY -> R.string.measurements_kind_daily
    MeasurementKind.TAPE -> R.string.measurements_kind_tape
}

/**
 * One record's form: the numbers that kind states, and nothing of the other kind's (ROADMAP N113).
 *
 * A form rather than a single line, because the optional numbers are optional in the sense that a person
 * may have measured none of them — leaving one blank has to be as easy as filling it in. **Save is
 * offered once the entry states at least one of its own numbers**: the weight used to be the one thing
 * an entry could not be without, and a soreness-only morning is a real entry, so what the form refuses
 * is an entry that states nothing at all.
 *
 * **[initial] opens it on a record that exists** (a request in use; no roadmap id covers it): every field
 * starts at what that entry states — including the numbers it left out, which start empty and can be
 * filled in — and the draft keeps the entry's **own day and id**, so saving corrects the row it came from
 * rather than placing a second one on the day.
 */
@Composable
private fun MeasurementDialog(
    kind: MeasurementKind,
    onDismiss: () -> Unit,
    onConfirm: (BodyMeasurement) -> Unit,
    /** The entry being corrected, or null when this adds one. */
    initial: BodyMeasurement? = null,
) {
    var weight by remember { mutableStateOf(initial?.weightGrams?.let { Weight.kilograms(it) }.orEmpty()) }
    var bodyFat by remember { mutableStateOf(initial?.bodyFatTenths?.let { MeasurementFormat.percent(it) }.orEmpty()) }
    var muscle by remember { mutableStateOf(initial?.muscleTenths?.let { MeasurementFormat.percent(it) }.orEmpty()) }
    var tape by remember { mutableStateOf(MeasurementFormat.tape(initial?.tape.orEmpty())) }
    // `rememberSaveable` for the same reason the note's other dialogs hold their picks that way: a
    // rotation mid-edit must not throw away the muscles already picked (N113).
    var soreMuscles by rememberSaveable(stateSaver = SoreMusclesSaver) {
        mutableStateOf(initial?.soreMuscles.orEmpty())
    }
    // The day is editable **only when correcting a record** (B212): a new entry belongs to today, and the form
    // that corrects a record is the one that can say it was not today after all. The zone is the one the entry's
    // day is read in, which is the device's. The state is keyed on the record so a recomposition cannot re-read
    // the stored day over a picked one (B214).
    val zone = ZoneId.systemDefault()
    val dayState = remember(initial) { MeasurementDayState(initial?.measuredAt, zone) }

    val draft = BodyMeasurement(
        // A blank id is a new entry, which the repository places by its day and kind; an edit keeps the
        // id it came from, which is how it corrects that row rather than writing beside it.
        id = initial?.id.orEmpty(),
        measuredAt = dayState.measuredAt,
        kind = kind,
        weightGrams = Weight.parseKilograms(weight),
        bodyFatTenths = tenths(bodyFat),
        muscleTenths = tenths(muscle),
        tape = tape.mapNotNull { (site, text) -> millimetres(text)?.let { site to it } }.toMap(),
        soreMuscles = soreMuscles,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) kindAddLabel(kind) else R.string.measurements_edit_title))
        },
        text = {
            MeasurementFormBody(
                kind = kind,
                day = initial?.let { dayState.day },
                dayText = HistoryFormat.date(dayState.measuredAt, zone = zone),
                onPickDay = { picked -> dayState.pick(picked) },
                weight = weight,
                onWeightChange = { weight = it },
                bodyFat = bodyFat,
                onBodyFatChange = { bodyFat = it },
                muscle = muscle,
                onMuscleChange = { muscle = it },
                soreMuscles = soreMuscles,
                onSoreMusclesChange = { soreMuscles = it },
                tape = tape,
                onTapeChange = { site, text -> tape = tape + (site to text) },
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { onConfirm(draft) },
                enabled = draft.statesSomething(),
                modifier = Modifier.testTag(TestTags.Measurements.CONFIRM),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun kindAddLabel(kind: MeasurementKind): Int = when (kind) {
    MeasurementKind.DAILY -> R.string.measurements_add_daily
    MeasurementKind.TAPE -> R.string.measurements_add_tape
}

/** What a day says about the body: soreness, weight and the two percentages (N113). */
@Composable
internal fun DailyFields(
    weight: String,
    onWeightChange: (String) -> Unit,
    bodyFat: String,
    onBodyFatChange: (String) -> Unit,
    muscle: String,
    onMuscleChange: (String) -> Unit,
    soreMuscles: List<SoreMuscle>,
    onSoreMusclesChange: (List<SoreMuscle>) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            label = stringResource(R.string.measurements_weight),
            value = weight,
            onValueChange = onWeightChange,
            testTag = TestTags.Measurements.WEIGHT,
        )
        NumberField(
            label = stringResource(R.string.measurements_body_fat),
            value = bodyFat,
            onValueChange = onBodyFatChange,
            testTag = TestTags.Measurements.BODY_FAT,
        )
        NumberField(
            label = stringResource(R.string.measurements_muscle),
            value = muscle,
            onValueChange = onMuscleChange,
            testTag = TestTags.Measurements.MUSCLE,
        )
        // The sore list, moved here from the workout's readiness note (N113): the same shared editor and
        // the same taxonomy, so the vocabulary is the app's one list of muscles either way.
        ScoredPicksEditor(
            picked = soreMuscles.map { ScoredPick(it.muscle.name, it.muscle.label, it.score) },
            options = SORE_MUSCLE_GROUPS.map { ScoredPick(it.name, it.label, DEFAULT_SCORED_PICK) },
            tags = TestTags.Measurements.SORE,
            strings = ScoredPickStrings(
                title = stringResource(R.string.readiness_sore_title),
                add = stringResource(R.string.readiness_sore_add),
                decrease = { stringResource(R.string.readiness_sore_decrease, it) },
                increase = { stringResource(R.string.readiness_sore_increase, it) },
                remove = { stringResource(R.string.readiness_sore_remove, it) },
            ),
            onAdd = { key ->
                onSoreMusclesChange(soreMuscles + SoreMuscle(MuscleGroup.valueOf(key), DEFAULT_SCORED_PICK))
            },
            onScore = { key, score ->
                onSoreMusclesChange(
                    soreMuscles.map { if (it.muscle.name == key) it.copy(score = score) else it },
                )
            },
            onRemove = { key -> onSoreMusclesChange(soreMuscles.filterNot { it.muscle.name == key }) },
        )
    }
}

/** A tape day: the sites, and nothing else (N113). */
@Composable
internal fun TapeFields(tape: Map<TapeSite, String>, onTapeChange: (TapeSite, String) -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TapeSite.entries.forEach { site ->
            NumberField(
                label = stringResource(tapeLabel(site)),
                value = tape[site].orEmpty(),
                onValueChange = { onTapeChange(site, it) },
                testTag = TestTags.Measurements.tape(site),
            )
        }
    }
}

/** Survives a rotation mid-edit, the shape the readiness note's own list used (N113). */
private val SoreMusclesSaver = listSaver<List<SoreMuscle>, String>(
    save = { muscles -> muscles.flatMap { listOf(it.muscle.name, it.score.toString()) } },
    restore = { flat ->
        flat.chunked(2).mapNotNull { (name, score) ->
            val muscle = MuscleGroup.entries.firstOrNull { it.name == name }
            if (muscle == null) null else SoreMuscle(muscle, score.toIntOrNull() ?: DEFAULT_SCORED_PICK)
        }
    },
)

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

/**
 * `18.3` or `18,3` reads as 183 tenths; anything else is not a number, and absence is null.
 *
 * Guarded the way [Weight.parseKilograms] is, and for the same reason: `toDoubleOrNull` accepts `NaN` and
 * `Infinity`, and `Math.round(NaN * 10)` is `0`, so a typed "NaN" was stored as a real 0.0% reading — a
 * measurement nobody took, in a series that then draws it. Negative is refused too: no tape measurement or
 * body-fat percentage in this app is below zero.
 */
private fun tenths(text: String): Int? =
    finite(text)?.let { Math.round(it * TENTHS_PER_PERCENT).toInt() }

/** `86.4` reads as 864 millimetres, which is ten to the centimetre. */
private fun millimetres(text: String): Long? =
    finite(text)?.let { Math.round(it * MILLIMETRES_PER_CENTIMETRE) }

/**
 * A typed measurement, or null when it is not one.
 *
 * `Math.round` maps anything non-finite or out of `Int` range to a value that looks like a real reading, so the
 * check has to happen before the conversion rather than after it.
 */
private fun finite(text: String): Double? =
    text.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

/** The units the form collects in, and the schema stores in tenths and millimetres. */
private const val TENTHS_PER_PERCENT = 10.0
private const val MILLIMETRES_PER_CENTIMETRE = 10.0

internal fun tapeLabel(site: TapeSite): Int = when (site) {
    TapeSite.NECK -> R.string.measurements_neck
    TapeSite.CHEST -> R.string.measurements_chest
    TapeSite.WAIST -> R.string.measurements_waist
    TapeSite.HIPS -> R.string.measurements_hips
    TapeSite.UPPER_ARM -> R.string.measurements_upper_arm
    TapeSite.THIGH -> R.string.measurements_thigh
    TapeSite.CALF -> R.string.measurements_calf
}
