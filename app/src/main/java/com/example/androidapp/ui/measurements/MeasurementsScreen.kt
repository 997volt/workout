// The measurement screen is one screen and many small composables; the split is N285, not this fix.
@file:Suppress("TooManyFunctions")

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MAX_BODY_PERCENT_TENTHS
import com.example.androidapp.domain.model.MAX_TAPE_CENTIMETRES
import com.example.androidapp.domain.model.MAX_TAPE_MILLIMETRES
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SORE_MUSCLE_GROUPS
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.at
import com.example.androidapp.domain.model.statedBodyWeightGrams
import com.example.androidapp.domain.model.statedWeightGrams
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
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.statistics.BodyMetric
import com.example.androidapp.ui.statistics.EnergyAdjustment
import com.example.androidapp.ui.statistics.MetricKey
import com.example.androidapp.ui.statistics.MetricRegistry
import com.example.androidapp.ui.statistics.WeightGoalRow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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
    val pendingGoalUndo by viewModel.pendingGoalUndo.collectAsStateWithLifecycle()
    val goalSave by viewModel.goalSave.collectAsStateWithLifecycle()
    val rateVerdict by viewModel.rateVerdict.collectAsStateWithLifecycle()
    // The shortcut opens **today's record** (B182), and only the loaded list can say whether that is an
    // entry to edit or a day with nothing on it — so it waits for the answer rather than opening a blank
    // form first, which would replace the day's numbers on save. The wait is the screen's body now (B371),
    // so the Scaffold, title and back path are there while it lasts.
    val loading = openDaily && state.isLoading
    MeasurementsScreen(
        state = state,
        loading = loading,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        onBack = onBack,
        modifier = modifier,
        // A failed write is a snackbar, the surface every other screen uses (B102).
        error = error,
        onDismissError = viewModel::onDismissError,
        initialAdd = if (!loading && openDaily && state.todayEntry == null) MeasurementKind.DAILY else null,
        initialEdit = if (!loading && openDaily) state.todayEntry else null,
        // The way back out of a delete (N133), held by the ViewModel until the message is taken.
        pendingUndo = pendingUndo,
        onUndo = viewModel::onUndoDelete,
        onDismissUndo = viewModel::onDismissUndo,
        // What the weigh-in just saved says about the goal's rate (N136), the same way.
        rateVerdict = rateVerdict,
        onDismissRateVerdict = viewModel::onDismissRateVerdict,
        // The weight goal is set here now: it is a fact about the body, and this is where the body is recorded
        // (a request in use; no roadmap id covers the move). Since N215 a past period can be written too.
        onSaveGoalPeriod = viewModel::onSaveGoalPeriod,
        onRemoveGoalPeriod = viewModel::onRemoveGoalPeriod,
        goalSave = goalSave,
        onDismissGoalSave = viewModel::onDismissGoalSave,
        goalPeriodUi = GoalPeriodUi(
            pendingRemoval = pendingGoalUndo,
            onUndo = viewModel::onUndoGoalPeriodRemoval,
            onDismiss = viewModel::onDismissGoalUndo,
        ),
    )
}

/** The screen's three dialogs: which record, the record's form, and a record read (N113, B182). */
@Composable
private fun MeasurementsDialogs(
    choosing: Boolean,
    onPick: (MeasurementKind) -> Unit,
    adding: MeasurementKind?,
    editingEntry: BodyMeasurement?,
    openEntry: BodyMeasurement?,
    onDismissForm: () -> Unit,
    onSave: (BodyMeasurement) -> Unit,
    onDismissOpen: () -> Unit,
    onDismissChoosing: () -> Unit,
) {
    if (choosing) {
        AddMeasurementDialog(onDismiss = onDismissChoosing, onPick = onPick)
    }
    MeasurementFormDialogs(
        adding = adding,
        editingEntry = editingEntry,
        onDismiss = onDismissForm,
        onSave = onSave,
    )
    openEntry?.let { entry ->
        MeasurementDetailsDialog(entry = entry, onDismiss = onDismissOpen)
    }
}

// The screen is a scaffold plus four dialog hosts and the list; the split is N285.
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    state: MeasurementsUiState,
    onSave: (BodyMeasurement) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    onDismissRateVerdict: () -> Unit,
    /** Writes one goal period — the row [id] names, or a new one from [start]; null [goal] is *No goal* (N215). */
    onSaveGoalPeriod: (String?, LocalDate?, LocalDate?, WeightGoal?) -> Unit,
    onRemoveGoalPeriod: (String) -> Unit,
    onDismissGoalSave: () -> Unit,
    modifier: Modifier = Modifier,
    /** The daily shortcut is waiting for the list, so the Scaffold stays and the body says why (B371). */
    loading: Boolean = false,
    /** A write that failed, or null (B102) — shown as a snackbar, like every other screen's. */
    error: DataError? = null,
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
    /** What a weigh-in said about the goal's rate, or null (N136): a line after the save, not a dialog. */
    rateVerdict: EnergyAdjustment? = null,
    /** The last goal-period save's result, so its editor can wait for it (B234). */
    goalSave: Boolean? = null,
    goalPeriodUi: GoalPeriodUi = GoalPeriodUi(),
) {
    // Two steps rather than one: the record is two records, so *Add* asks which before it opens a form
    // (N113). Null is "no form open"; `choosing` is the question.
    var choosing by remember { mutableStateOf(false) }
    // B382: the route first composes this screen with a null initial form while the list loads. Keying
    // the state on the form means the daily form arrives when the answer does; an input that stays the
    // same does not reopen a form the lifter dismissed.
    var adding by rememberSaveable(initialAdd) { mutableStateOf(initialAdd) }
    // Whether the list is in its edit mode, the one that offers a delete (N132). Saveable so a rotation
    // does not drop the lifter out of the mode they deliberately entered, or worse, back into it.
    var editing by rememberSaveable { mutableStateOf(false) }
    // The read-only record is a look at one row rather than a mode, so it is not restored (the request in
    // use); the record being corrected is, by id, so a rotation keeps the form and its saveable draft (B376).
    var openEntry by remember { mutableStateOf<BodyMeasurement?>(null) }
    var editingEntryId by rememberSaveable(initialEdit?.id) { mutableStateOf(initialEdit?.id) }
    val editingEntry = editingEntryId?.let { id ->
        state.entries.firstOrNull { it.id == id } ?: initialEdit?.takeIf { it.id == id }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(error?.let { dataErrorMessage(it) }, snackbarHostState, onDismissError)
    ShowDeletedMeasurementUndo(pendingUndo, snackbarHostState, onUndo, onDismissUndo)
    ShowRemovedGoalUndo(goalPeriodUi.pendingRemoval, snackbarHostState, goalPeriodUi.onUndo, goalPeriodUi.onDismiss)
    ShowRateVerdict(rateVerdict, snackbarHostState, onDismissRateVerdict)

    MeasurementsDialogs(
        choosing = choosing,
        onPick = { kind ->
            choosing = false
            adding = kind
        },
        adding = adding,
        editingEntry = editingEntry,
        openEntry = openEntry,
        onDismissForm = {
            adding = null
            editingEntryId = null
        },
        onSave = onSave,
        onDismissOpen = { openEntry = null },
        onDismissChoosing = { choosing = false },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MeasurementsTopBar(editing = editing, onToggleEditing = { editing = !editing }, onBack = onBack) },
        floatingActionButton = {
            // The default view is scroll and add, the edit mode is delete (N132): while deleting, the
            // one control that would write a new entry is out of the way.
            if (!editing && !loading) {
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
        if (loading) {
            CenteredMessage(
                text = stringResource(R.string.history_loading),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            MeasurementList(
                state = state,
                editing = editing,
                // In the edit mode the row opens its form — that is where the writes live — and in the
                // default view it opens the record read-only (N132, and the request in use).
                onOpen = { entry -> if (editing) editingEntryId = entry.id else openEntry = entry },
                onDelete = onDelete,
                onSaveGoalPeriod = onSaveGoalPeriod,
                onRemoveGoalPeriod = onRemoveGoalPeriod,
                goalSave = goalSave,
                onDismissGoalSave = onDismissGoalSave,
                modifier = Modifier.padding(innerPadding),
            )
        }
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
        title = { TopBarTitle(text = stringResource(R.string.measurements_title)) },
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
    onSaveGoalPeriod: (String?, LocalDate?, LocalDate?, WeightGoal?) -> Unit,
    onRemoveGoalPeriod: (String) -> Unit,
    goalSave: Boolean?,
    onDismissGoalSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.Measurements.LIST),
        // The last row must clear the add button, which floats over the list's foot.
        contentPadding = PaddingValues(bottom = LIST_FOOT_CLEARANCE),
    ) {
        // The goal comes before the entries: it is what those readings are *for*, and the row it draws is the
        // one the statistics screen used to carry under its chart (N135).
        // The goal is one statement about the series, so it is a card above the readings it is about
        // (N135, N137): the row the statistics screen used to carry under its chart.
        // It waits for the first emission: until then `today` has no real value, and the editor would open on
        // it and format a date that cannot exist (B225).
        if (!state.isLoading) {
            item(key = GOAL_KEY) {
                AppCard {
                    WeightGoalRow(
                        goal = state.weightGoal,
                        metric = MetricRegistry.entryFor(MetricKey.Body(BodyMetric.WEIGHT)),
                        history = state.goalHistory,
                        today = state.today,
                        onSavePeriod = onSaveGoalPeriod,
                        onRemovePeriod = onRemoveGoalPeriod,
                        saveOutcome = goalSave,
                        onDismissSave = onDismissGoalSave,
                    )
                }
            }
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
    // The weight is the one number that may be absent, because a soreness-only morning is a real entry — and a
    // stored zero states none either (B336), so the rule that hides it from the chart hides it here.
    entry.statedWeightGrams?.let { parts += Weight.kilograms(it) }
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
// The dialog is one form: its field state, the draft built from them and the three buttons. The
// screen split is N285; until then the state initializers are what push it past the method ceiling.
@Suppress("LongMethod")
@Composable
private fun MeasurementDialog(
    kind: MeasurementKind,
    onDismiss: () -> Unit,
    onConfirm: (BodyMeasurement) -> Unit,
    /** The entry being corrected, or null when this adds one. */
    initial: BodyMeasurement? = null,
) {
    // A typed zero is not a body weight, and the form refuses to write one (B336): the field states nothing, so
    // a zero alone leaves *Save* disabled and a zero beside other numbers is one fewer thing stated.
    var weight by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.statedWeightGrams?.let { Weight.kilograms(it) }.orEmpty())
    }
    var bodyFat by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.bodyFatTenths?.let { MeasurementFormat.percent(it) }.orEmpty())
    }
    var muscle by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.muscleTenths?.let { MeasurementFormat.percent(it) }.orEmpty())
    }
    var tape by rememberSaveable(initial?.id, stateSaver = TapeSaver) {
        mutableStateOf(MeasurementFormat.tape(initial?.tape.orEmpty()))
    }
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
    val dayState = rememberSaveable(initial, saver = MeasurementDaySaver) {
        MeasurementDayState(initial?.measuredAt, zone)
    }

    val draft = BodyMeasurement(
        // A blank id is a new entry, which the repository places by its day and kind; an edit keeps the
        // id it came from, which is how it corrects that row rather than writing beside it.
        id = initial?.id.orEmpty(),
        measuredAt = dayState.measuredAt,
        kind = kind,
        weightGrams = statedBodyWeightGrams(Weight.parseKilograms(weight)),
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
                // A typed value the schema cannot hold is shown as an error and stops the save: silently
                // dropping it would write a different record from the one on screen (B359).
                enabled = draft.statesSomething() && formValuesValid(bodyFat, muscle, tape),
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
    val bodyFatInvalid = bodyFat.isNotBlank() && tenths(bodyFat) == null
    val muscleInvalid = muscle.isNotBlank() && tenths(muscle) == null
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberField(
            label = stringResource(R.string.measurements_weight),
            value = weight,
            onValueChange = onWeightChange,
            testTag = TestTags.Measurements.WEIGHT,
            imeAction = ImeAction.Next,
        )
        NumberField(
            label = stringResource(R.string.measurements_body_fat),
            value = bodyFat,
            onValueChange = onBodyFatChange,
            testTag = TestTags.Measurements.BODY_FAT,
            imeAction = ImeAction.Next,
            isError = bodyFatInvalid,
            errorText = if (bodyFatInvalid) stringResource(R.string.measurements_percent_range) else null,
        )
        // Muscle is the form's last number: its action closes the keyboard rather than moving into the
        // sore-muscle editor, which is not a numeric field (N254).
        NumberField(
            label = stringResource(R.string.measurements_muscle),
            value = muscle,
            onValueChange = onMuscleChange,
            testTag = TestTags.Measurements.MUSCLE,
            imeAction = ImeAction.Done,
            isError = muscleInvalid,
            errorText = if (muscleInvalid) stringResource(R.string.measurements_percent_range) else null,
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
        TapeSite.entries.forEachIndexed { index, site ->
            val text = tape[site].orEmpty()
            NumberField(
                label = stringResource(tapeLabel(site)),
                value = text,
                onValueChange = { onTapeChange(site, it) },
                testTag = TestTags.Measurements.tape(site),
                // Calf is the last site: Done, like every form's last field (N254).
                imeAction = if (index == TapeSite.entries.lastIndex) ImeAction.Done else ImeAction.Next,
                isError = text.isNotBlank() && millimetres(text) == null,
                errorText = if (text.isNotBlank() && millimetres(text) == null) {
                    stringResource(R.string.measurements_tape_range, MAX_TAPE_CENTIMETRES)
                } else {
                    null
                },
            )
        }
    }
}

/** The typed tape sites survive a rotation, the same rule as the numbers (B349). */
private val TapeSaver = listSaver<Map<TapeSite, String>, String>(
    save = { tape -> tape.flatMap { (site, text) -> listOf(site.name, text) } },
    restore = { flat ->
        flat.chunked(2).mapNotNull { (name, text) ->
            TapeSite.entries.firstOrNull { it.name == name }?.let { it to text }
        }.toMap()
    },
)

/** The picked day, restored in the device's zone — the frame the form reads it in (B349). */
private val MeasurementDaySaver = Saver<MeasurementDayState, Long>(
    save = { it.measuredAt.toEpochMilli() },
    restore = { MeasurementDayState(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
)

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

/**
 * One decimal measurement field, with the form's own action key (N254).
 *
 * A numeric keyboard's one large action key used to be Done, so every number ended by dismissing the
 * keyboard and a tape day meant reopening it seven times. Next keeps it up and moves down the form; Done
 * stays on the last field. Focus pulls the field into view, because the forms scroll.
 */
@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    imeAction: ImeAction,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        supportingText = errorText?.let { message -> { Text(message) } },
        // Every field this renders is decimal — 82.5 kg, 18.3 %, 86.4 cm — and a digits-only keyboard
        // offers no separator on the IMEs that honour it (B290).
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { focusManager.clearFocus() },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { focus ->
                if (focus.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .testTag(testTag),
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
    finite(text)
        ?.let { Math.round(it * TENTHS_PER_PERCENT).toInt() }
        ?.takeIf { it in 0..MAX_BODY_PERCENT_TENTHS }

/** `86.4` reads as 864 millimetres, which is ten to the centimetre — up to the tape's ceiling (B359). */
private fun millimetres(text: String): Long? =
    finite(text)
        ?.let { Math.round(it * MILLIMETRES_PER_CENTIMETRE) }
        ?.takeIf { it in 0..MAX_TAPE_MILLIMETRES }

/** Whether the form's typed numbers can all be stored, so its Save is not offered over a dropped value. */
private fun formValuesValid(bodyFat: String, muscle: String, tape: Map<TapeSite, String>): Boolean =
    listOf(bodyFat, muscle).none { it.isNotBlank() && tenths(it) == null } &&
        tape.values.none { it.isNotBlank() && millimetres(it) == null }

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
