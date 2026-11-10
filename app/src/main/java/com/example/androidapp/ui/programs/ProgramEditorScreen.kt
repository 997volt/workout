package com.example.androidapp.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramPhases
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.shortLabel
import com.example.androidapp.ui.components.summary
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.theme.IndigoLink
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * One program: its name, whether it is the active one, and its ordered slots (ROADMAP P3.3).
 *
 * The order is the feature: a slot's weekday answers "what happens on a Tuesday", and its
 * position answers "which one is next". Both are edited here, and neither is inferred. A slot
 * prescribes nothing of its own (N73): the plan is the template's, and it is read from this screen
 * rather than edited here.
 */
@Composable
fun ProgramEditorRoute(
    onBack: () -> Unit,
    /**
     * Opens the template a slot names (ROADMAP N120), which is the editor Templates reaches.
     *
     * A program is a schedule *over* templates, so this is the way across: the slot's name keeps the
     * in-place preview, and this goes and changes the thing being previewed.
     */
    onOpenTemplate: (String) -> Unit,
    /** Opens this program's balance view (ROADMAP N119), which the bar's ⋮ carries. */
    onOpenBalance: () -> Unit,
    /** Opens the deload creator direct, from a stall card (ROADMAP N147). */
    onOpenDeload: () -> Unit,
    /** Opens the recovery-week choices: exempt this/next week, or build a deload (ROADMAP N300). */
    onOpenWeeks: () -> Unit,
    /** Opens this program's auto progression switch and preflight (ROADMAP N299). */
    onOpenProgression: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val templateToEdit by viewModel.templateToEdit.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnOpenTemplate by rememberUpdatedState(onOpenTemplate)

    // The workout created inline has no exercises yet, so the editor it opens is where it becomes a plan
    // (P3.18). Opened once, then cleared, so a recomposition cannot re-open it over the lifter's work.
    LaunchedEffect(templateToEdit) {
        templateToEdit?.let { templateId ->
            viewModel.onTemplateEditHandled()
            currentOnOpenTemplate(templateId)
        }
    }

    // Exporting reports through the editor's own host (ROADMAP N47). The file write is the
    // composable's, because it holds the `Uri`.
    var transferMessage by remember { mutableStateOf<String?>(null) }
    val exportProgram = rememberProgramExport(viewModel) { transferMessage = it }

    // Deleting — or opening a program that is already gone — leaves the editor, rather than
    // leaving an empty shell behind with a live Delete button.
    LaunchedEffect(deleted, state.notFound) { if (deleted || state.notFound) currentOnBack() }

    ProgramEditorScreen(
        state = state,
        onRename = viewModel::onRename,
        onSetActive = viewModel::onSetActive,
        onSetKind = viewModel::onSetKind,
        onSetStartDate = viewModel::onSetStartDate,
        onAddSlot = viewModel::onAddSlot,
        onAddNewSlot = viewModel::onAddNewSlot,
        onRemovePhaseWeek = viewModel::onRemovePhaseWeek,
        onSetSlotWeekday = viewModel::onSetSlotWeekday,
        onMoveSlot = viewModel::onMoveSlot,
        onRemoveSlot = viewModel::onRemoveSlot,
        onOpenTemplate = onOpenTemplate,
        onOpenBalance = onOpenBalance,
        onOpenDeload = onOpenDeload,
        onOpenWeeks = onOpenWeeks,
        onOpenProgression = onOpenProgression,
        onPreviewSlot = viewModel::onPreviewSlot,
        onClosePreview = viewModel::onClosePreview,
        onDeleteProgram = viewModel::onDeleteProgram,
        onExportProgram = exportProgram,
        transferMessage = transferMessage,
        onDismissTransferMessage = { transferMessage = null },
        onDismissMessage = viewModel::onErrorShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
// The body is the scaffold's wiring — one named callback per action, with no decision of its own —
// and the auto progression entry (N299) is the line that put it at the ceiling.
@Suppress("LongMethod")
@Composable
fun ProgramEditorScreen(
    state: ProgramEditorUiState,
    onRename: (String) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onSetKind: (ProgramKind) -> Unit,
    onSetStartDate: (LocalDate?) -> Unit,
    onAddSlot: (String, Int?) -> Unit,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onOpenBalance: () -> Unit,
    onOpenDeload: () -> Unit,
    onOpenWeeks: () -> Unit,
    onOpenProgression: () -> Unit,
    onPreviewSlot: (String, String) -> Unit,
    onClosePreview: () -> Unit,
    onDeleteProgram: () -> Unit,
    onBack: () -> Unit,
    /** Required rather than defaulted, so a route that forgets it fails the build (B49, B52). */
    onExportProgram: () -> Unit,
    /** Creates a workout and adds it as a slot, then opens it (P3.18). */
    onAddNewSlot: (String, Int?) -> Unit,
    /** Removes one authored week's days (ROADMAP N292). */
    onRemovePhaseWeek: (Int) -> Unit,
    onDismissMessage: () -> Unit,
    onDismissTransferMessage: () -> Unit,
    modifier: Modifier = Modifier,
    transferMessage: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var pickingTemplate by rememberSaveable { mutableStateOf(false) }
    val isSequence = state.program?.kind == ProgramKind.SEQUENCE
    val phases = rememberPhaseWeekSelection(state.program?.id, state.slots, isSequence, onRemovePhaseWeek)

    EditorMessages(state.error, transferMessage, snackbarHostState, onDismissMessage, onDismissTransferMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProgramEditorTopBar(
                name = state.program?.name,
                isSequence = isSequence,
                onBack = onBack,
                onOpenBalance = onOpenBalance,
                onOpenWeeks = onOpenWeeks,
                onOpenProgression = onOpenProgression,
                onDelete = { confirmingDelete = true },
                onExport = onExportProgram,
            )
        },
        floatingActionButton = {
            AddSlotFab(onClick = { pickingTemplate = true })
        },
    ) { innerPadding ->
        ProgramEditorBody(
            state = state,
            phases = phases,
            onRename = onRename,
            onSetActive = onSetActive,
            onSetKind = onSetKind,
            onSetStartDate = onSetStartDate,
            onSetSlotWeekday = onSetSlotWeekday,
            onMoveSlot = onMoveSlot,
            onRemoveSlot = onRemoveSlot,
            onOpenTemplate = onOpenTemplate,
            onPreviewSlot = onPreviewSlot,
            onOpenDeload = onOpenDeload,
            onOpenWeeks = onOpenWeeks,
            modifier = Modifier.padding(innerPadding),
        )
    }

    ProgramEditorDialogs(
        state = state,
        pickingTemplate = pickingTemplate,
        confirmingDelete = confirmingDelete,
        onPick = {
            pickingTemplate = false
            onAddSlot(it, phases.newDayWeek)
        },
        onAddNewSlot = { name -> onAddNewSlot(name, phases.newDayWeek) },
        onDismissPicker = { pickingTemplate = false },
        onDismissDelete = { confirmingDelete = false },
        onConfirmDelete = {
            confirmingDelete = false
            onDeleteProgram()
        },
        onClosePreview = onClosePreview,
    )
}

/**
 * The editor's two messages: a failed write and a finished export.
 *
 * One call because the snackbar shows one at a time and both are the same surface; kept here so the
 * screen around them stays inside the length this project allows.
 */
@Composable
private fun EditorMessages(
    error: DataError?,
    transferMessage: String?,
    snackbarHostState: SnackbarHostState,
    onDismissError: () -> Unit,
    onDismissTransfer: () -> Unit,
) {
    EditorErrorMessage(error = error, snackbarHostState = snackbarHostState, onDismiss = onDismissError)
    MessageSnackbar(transferMessage, snackbarHostState, onDismissTransfer)
}

/**
 * Shows a write failure on the editor's snackbar (F7).
 *
 * Extracted so the screen stays the length this project allows: the message is resolved during
 * composition, because a string resource cannot be read inside an effect, and then shown from one.
 */
@Composable
private fun EditorErrorMessage(
    error: DataError?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    error?.let { failure ->
        val message = dataErrorMessage(failure)
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            currentOnDismiss()
        }
    }
}

/**
 * The things the editor can be asked to do before it writes (P3.3, P3.8).
 *
 * Split out because the screen around them is at the length this project allows, and because all
 * are dialogs over the same state: which plan to add, and whether to delete.
 */
@Composable
private fun ProgramEditorDialogs(
    state: ProgramEditorUiState,
    pickingTemplate: Boolean,
    confirmingDelete: Boolean,
    onPick: (String) -> Unit,
    onAddNewSlot: (String) -> Unit,
    onDismissPicker: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClosePreview: () -> Unit,
) {
    val newWorkoutName = stringResource(R.string.program_new_workout)
    if (pickingTemplate) {
        TemplatePickerDialog(
            templates = state.templates,
            onPick = onPick,
            onPickNew = { onAddNewSlot(newWorkoutName) },
            onDismiss = onDismissPicker,
        )
    }

    if (confirmingDelete) {
        DeleteProgramDialog(onDismiss = onDismissDelete, onConfirm = onConfirmDelete)
    }

    // What a slot's template actually trains, read-only (ROADMAP N72): the template is edited in the
    // template editor, so there is no control in here but Close.
    state.preview?.let { preview ->
        TemplatePreviewDialog(preview = preview, onDismiss = onClosePreview)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramEditorTopBar(
    name: String?,
    isSequence: Boolean,
    onBack: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenWeeks: () -> Unit,
    onOpenProgression: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        title = { TopBarTitle(text = name ?: stringResource(R.string.program_edit_title), preserveCase = true) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // One control, three entries (N119): the bar carried a text button and a delete icon beside it,
            // and the destructive one at that distance from a frequently pressed neighbour is the shape N43
            // settled against for the database's own actions — the one that cannot be undone goes last and
            // reads as destructive. Export is per program, so it stays here rather than on the list (N47).
            Box {
                IconButton(
                    onClick = { open = true },
                    modifier = Modifier.testTag(TestTags.Programs.MENU),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.program_menu, name.orEmpty()),
                    )
                }
                ProgramEditorMenu(
                    expanded = open,
                    isSequence = isSequence,
                    onDismiss = { open = false },
                    onOpenBalance = onOpenBalance,
                    onOpenWeeks = onOpenWeeks,
                    onOpenProgression = onOpenProgression,
                    onExport = onExport,
                    onDelete = onDelete,
                )
            }
        },
    )
}

/**
 * The editor's one overflow menu: recovery week, auto progression, balance, export, delete (ROADMAP N119,
 * N147, N299, N300).
 *
 * Split out because the bar was at the length this project allows, and because the entries are one
 * kind of thing — the actions this editor's bar carries — with the destructive one last and coloured.
 */
@Composable
private fun ProgramEditorMenu(
    expanded: Boolean,
    isSequence: Boolean,
    onDismiss: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenWeeks: () -> Unit,
    onOpenProgression: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.program_progression)) },
            onClick = {
                onDismiss()
                onOpenProgression()
            },
            modifier = Modifier.testTag(TestTags.Programs.AUTO_PROGRESSION),
        )
        // A weekly program's weeks are the calendar's; a block has its own order (N176), so the
        // calendar exemption is not offered for one rather than offered and refused (B398).
        if (!isSequence) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.program_recovery_week)) },
                onClick = {
                    onDismiss()
                    onOpenWeeks()
                },
                modifier = Modifier.testTag(TestTags.Programs.WEEKS_MENU),
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.program_balance)) },
            onClick = {
                onDismiss()
                onOpenBalance()
            },
            modifier = Modifier.testTag(TestTags.Programs.BALANCE),
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.program_export)) },
            onClick = {
                onDismiss()
                onExport()
            },
            modifier = Modifier.testTag(TestTags.Programs.EXPORT),
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.program_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
            modifier = Modifier.testTag(TestTags.Programs.DELETE),
        )
    }
}

@Composable
private fun ProgramEditorBody(
    state: ProgramEditorUiState,
    /** Which authored week is shown, and how it changes (ROADMAP N292). */
    phases: PhaseWeekSelection,
    onRename: (String) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onSetKind: (ProgramKind) -> Unit,
    onSetStartDate: (LocalDate?) -> Unit,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onPreviewSlot: (String, String) -> Unit,
    /** Opens the deload creator this program already has (ROADMAP N237). */
    onOpenDeload: () -> Unit,
    /** Opens the recovery-week choices (ROADMAP N300). */
    onOpenWeeks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        CenteredMessage(
            text = stringResource(R.string.programs_loading),
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        state.program?.let { program ->
            ProgramNameField(
                program = program,
                onRename = onRename,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ActiveSwitch(program = program, onSetActive = onSetActive)
            ProgramKindSection(
                program = program,
                isComplete = state.blockComplete,
                hasPhases = phases.weeks.isNotEmpty(),
                onSetKind = onSetKind,
                onSetStartDate = onSetStartDate,
            )
        }
        HorizontalDivider()

        // A block's weekday places one session in an order, so it has no weeks to author (N174).
        val isSequence = state.program?.kind == ProgramKind.SEQUENCE
        if (!isSequence) {
            PhaseWeekSection(
                weeks = phases.weeks,
                selectedWeek = phases.selected,
                onSelect = phases.onSelect,
                onAddWeek = phases.onAdd,
                onRemoveWeek = phases.onRemove,
            )
            HorizontalDivider()
            RecoveryWeekCard(onOpen = onOpenWeeks)
        }

        // A prescription that stopped moving, said where the program is read (N237). Nothing is written: the
        // deload is a decision about a week of training, and this is the question.
        state.stall?.let { stall -> PlanStallCard(stall = stall, onOpenDeload = onOpenDeload) }

        // A sequence shows its whole order; a weekly program shows the selected authored week, and
        // week 1 is every day of a program that never authored a second one (N292).
        val shownSlots = if (isSequence) state.slots else ProgramPhases.slotsOf(state.slots, phases.selected)
        if (shownSlots.isEmpty()) {
            CenteredMessage(
                text = stringResource(R.string.program_no_slots),
                hint = stringResource(R.string.program_no_slots_hint),
                modifier = Modifier.testTag(TestTags.Programs.NO_SLOTS),
            )
        } else {
            ProgramSlotList(
                slots = shownSlots,
                runSlotId = state.runSlotId,
                isSequence = isSequence,
                onSetSlotWeekday = onSetSlotWeekday,
                onMoveSlot = onMoveSlot,
                onRemoveSlot = onRemoveSlot,
                onOpenTemplate = onOpenTemplate,
                onPreviewSlot = onPreviewSlot,
            )
        }
    }
}

/** The program's name, committed deliberately rather than on every keystroke (N3's rule). */
@Composable
private fun ProgramNameField(
    program: WorkoutProgram,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the id, so another program's name can never leak into this field.
    var draft by rememberSaveable(program.id) { mutableStateOf(program.name) }
    val changed = draft.trim() != program.name

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f).testTag(TestTags.Programs.NAME_FIELD),
            singleLine = true,
            label = { Text(stringResource(R.string.program_name_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (changed) onRename(draft) }),
        )
        IconButton(
            onClick = { onRename(draft) },
            enabled = changed,
            modifier = Modifier.testTag(TestTags.Programs.NAME_SAVE),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.program_save_name),
            )
        }
    }
}

/**
 * Whether home follows this program (P3.3).
 *
 * A switch rather than a button because it is a state with two directions: on means home follows
 * this program's days, off means it does not. It used to fall back to each template's own weekday
 * when no program was active, and that fallback went with the column it read (N56) — so a program
 * with nothing active is a home with no dated plan at all, which is the rule rather than an absence.
 */
@Composable
private fun ActiveSwitch(
    program: WorkoutProgram,
    onSetActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val activeLabel = stringResource(R.string.program_active)
            Text(
                text = activeLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = program.isActive,
                onCheckedChange = onSetActive,
                // The label is a sibling, so without this TalkBack says "On, switch" (B353).
                modifier = Modifier
                    .testTag(TestTags.Programs.ACTIVE)
                    .semantics { contentDescription = activeLabel },
            )
        }
        Text(
            text = stringResource(R.string.program_active_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** One slot: its template, the order it sits in, and the day it falls on. */
@Composable
internal fun ProgramSlotBlock(
    slot: ProgramSlot,
    isFirst: Boolean,
    isLast: Boolean,
    isRunPlace: Boolean,
    runLabel: String?,
    isSequence: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenTemplate: () -> Unit,
    onSetWeekday: (DayOfWeek?) -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ListItem(
            // The name is the way in: a slot is a schedule, so "what does this day actually do" is
            // the question the row raises, and it is answered without leaving the program (N72).
            // The `onClickLabel` is what a screen reader announces; the tag is how a test reaches
            // the control by identity rather than by the English it shows.
            headlineContent = {
                Text(
                    text = slot.templateName,
                    modifier = Modifier
                        .testTag(TestTags.Programs.previewOpen(slot.id))
                        .clickable(
                            onClickLabel = stringResource(
                                R.string.program_preview_open,
                                slot.templateName,
                            ),
                            onClick = onOpenPreview,
                        ),
                )
            },
            supportingContent = {
                Column {
                    Text(
                        pluralStringResource(
                            R.plurals.template_exercises,
                            slot.exerciseCount,
                            slot.exerciseCount,
                        ),
                    )
                    if (isRunPlace) {
                        Text(
                            text = runLabel ?: stringResource(R.string.program_next_up),
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoLink,
                            modifier = Modifier.testTag(TestTags.Programs.runSlot(slot.id)),
                        )
                    }
                }
            },
            trailingContent = {
                SlotActionsMenu(
                    slot = slot,
                    isFirst = isFirst,
                    isLast = isLast,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onRemove = onRemove,
                    onOpenTemplate = onOpenTemplate,
                )
            },
            modifier = Modifier.testTag(TestTags.Programs.slot(slot.id)),
        )
        SlotWeekdayPicker(slot = slot, isSequence = isSequence, onSelect = onSetWeekday)
    }
}

/**
 * One slot's ⋮: the template it names, its order, and the removal that takes the day out of the schedule
 * (ROADMAP N72, N120).
 *
 * The row used to carry an arrow for each direction and a delete icon, and the delete fired on the
 * tap. They are one menu now, in the shape the workout and the template editor already use (N53,
 * N71): each direction offered only where it exists, and the destructive entry last, coloured, and
 * behind a question — a slot that goes takes its place in the schedule with it.
 */
@Composable
private fun SlotActionsMenu(
    slot: ProgramSlot,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.Programs.slotMenu(slot.id)),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.program_slot_menu, slot.templateName),
            )
        }
        SlotMenuEntries(
            expanded = open,
            onDismiss = { open = false },
            slot = slot,
            isFirst = isFirst,
            isLast = isLast,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = { confirming = true },
            onOpenTemplate = onOpenTemplate,
        )
    }

    if (confirming) {
        RemoveSlotDialog(
            name = slot.templateName,
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                onRemove()
            },
        )
    }
}

/**
 * The entries one slot's ⋮ offers (ROADMAP N72).
 *
 * Its own composable for the reason the project keeps splitting them: the button, the menu and the
 * question were one function at the length this project allows, and the entries are the part that
 * reads on its own.
 */
@Composable
private fun SlotMenuEntries(
    expanded: Boolean,
    onDismiss: () -> Unit,
    slot: ProgramSlot,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenTemplate: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (!isFirst) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.program_move_up, slot.templateName)) },
                onClick = {
                    onDismiss()
                    onMoveUp()
                },
                modifier = Modifier.testTag(TestTags.Programs.move(slot.id, up = true)),
            )
        }
        if (!isLast) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.program_move_down, slot.templateName)) },
                onClick = {
                    onDismiss()
                    onMoveDown()
                },
                modifier = Modifier.testTag(TestTags.Programs.move(slot.id, up = false)),
            )
        }
        // After the moves and before the removal: the order this menu documents — order first, the
        // destructive entry last and coloured (N53) — and opening the template is neither (N120).
        DropdownMenuItem(
            text = { Text(stringResource(R.string.program_open_slot, slot.templateName)) },
            onClick = {
                onDismiss()
                onOpenTemplate()
            },
            modifier = Modifier.testTag(TestTags.Programs.openSlot(slot.id)),
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.program_remove_slot, slot.templateName),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDismiss()
                onRemove()
            },
            modifier = Modifier.testTag(TestTags.Programs.removeSlot(slot.id)),
        )
    }
}

/**
 * The question a slot's removal asks first (ROADMAP N72).
 *
 * The schedule loses a day; the workout it pointed at, and everything logged with it, stay. Both
 * halves are said, because only the first is obvious from the button.
 */
@Composable
private fun RemoveSlotDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_remove_confirm_title)) },
        text = { Text(stringResource(R.string.program_remove_confirm_text, name)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Programs.SLOT_REMOVE_CONFIRM),
            ) {
                Text(stringResource(R.string.program_remove_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Programs.SLOT_REMOVE_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * What a slot's template trains, read-only (ROADMAP N72).
 *
 * A preview rather than an editor, and deliberately nothing else: a program uses the template, so
 * this answers "what does this day actually do" without becoming a second place to edit a plan. The
 * sets are drawn with the line the template editor already uses, in the exercise's own unit (N64).
 */
@Composable
private fun TemplatePreviewDialog(
    preview: SlotPreview,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag(TestTags.Programs.PREVIEW),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_preview_title, preview.templateName)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (preview.exercises.isEmpty()) {
                    Text(
                        text = stringResource(R.string.program_preview_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(TestTags.Programs.PREVIEW_EMPTY),
                    )
                }
                preview.exercises.forEach { exercise ->
                    val unit = exerciseWeightUnit(exercise.weightUnit)
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (exercise.sets.isEmpty()) {
                        Text(
                            text = stringResource(R.string.template_plan_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        exercise.sets.forEach { set ->
                            Text(
                                text = set.summary(unit),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Programs.PREVIEW_CLOSE),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * The day this slot falls on (P3.3).
 *
 * Seven chips with **no** separate "none": a slot with no day is order-only, and that is
 * what tapping the selected chip leaves behind. Adding a "none" chip beside the seven would
 * make "no day" a value someone could mistake for a day.
 *
 * In a **sequence** the same seven chips are the day the session is due (ROADMAP N178): a weekday
 * places one session there rather than scheduling a recurring one, so the label says so.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotWeekdayPicker(
    slot: ProgramSlot,
    isSequence: Boolean,
    onSelect: (DayOfWeek?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(if (isSequence) R.string.program_due else R.string.template_weekday),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayOfWeek.entries.forEach { day ->
            val selected = slot.weekday == day
            // In a block the seven chips repeat under a bare "Due", so each says which session's
            // due day it sets (B201): "Due Tue for Peak day" is the control, not "Tue".
            val spoken = if (isSequence) {
                stringResource(R.string.program_due_for, day.shortLabel(), slot.templateName)
            } else {
                null
            }
            FilterChip(
                selected = selected,
                // Tapping the chosen day again makes the slot order-only.
                onClick = { onSelect(if (selected) null else day) },
                label = { Text(day.shortLabel()) },
                modifier = Modifier
                    .testTag(TestTags.Programs.slotWeekday(slot.id, day.name))
                    .then(
                        if (spoken == null) {
                            Modifier
                        } else {
                            Modifier.semantics { contentDescription = spoken }
                        },
                    ),
            )
        }
    }
}

/** The templates a slot can point at (P3.3): the plan has to exist before it can be ordered. */
@Composable
private fun TemplatePickerDialog(
    templates: List<WorkoutTemplate>,
    onPick: (String) -> Unit,
    onPickNew: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.Programs.PICKER),
        title = { Text(stringResource(R.string.program_pick_title)) },
        text = {
            LazyColumn {
                // The one entry that does not need a plan to exist first (P3.18): the program is the
                // screen the lifter is on, so a new workout can be born here and opened for its contents.
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.program_new_workout)) },
                        supportingContent = {
                            Text(stringResource(R.string.program_new_workout_hint))
                        },
                        leadingContent = {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        },
                        modifier = Modifier
                            .testTag(TestTags.Programs.PICKER_NEW)
                            .clickable { onPickNew() },
                    )
                }
                if (templates.isEmpty()) {
                    item {
                        CenteredMessage(
                            text = stringResource(R.string.program_pick_empty),
                            hint = stringResource(R.string.program_pick_empty_hint),
                            modifier = Modifier.testTag(TestTags.Programs.PICKER_EMPTY),
                        )
                    }
                } else {
                    itemsIndexed(items = templates, key = { _, template -> template.id }) { _, template ->
                        ListItem(
                            headlineContent = { Text(template.name) },
                            supportingContent = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.template_exercises,
                                        template.exerciseCount,
                                        template.exerciseCount,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .testTag(TestTags.Programs.pickTemplate(template.id))
                                .clickable { onPick(template.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DeleteProgramDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_delete_confirm_title)) },
        text = { Text(stringResource(R.string.program_delete_confirm_text)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Programs.DELETE_CONFIRM),
            ) {
                Text(stringResource(R.string.program_delete))
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
private fun ProgramEditorScreenPreview() {
    AndroidAppTheme {
        ProgramEditorScreen(
            state = ProgramEditorUiState(
                isLoading = false,
                program = WorkoutProgram(id = "p1", name = "Upper/Lower", slotCount = 2),
                slots = listOf(
                    ProgramSlot(
                        id = "s1",
                        programId = "p1",
                        templateId = "t1",
                        position = 0,
                        weekday = DayOfWeek.MONDAY,
                        templateName = "Heavy lower",
                        exerciseCount = 4,
                    ),
                    ProgramSlot(
                        id = "s2",
                        programId = "p1",
                        templateId = "t2",
                        position = 1,
                        templateName = "Push",
                        exerciseCount = 5,
                    ),
                ),
            ),
            onRename = {},
            onSetActive = {},
            onSetKind = {},
            onSetStartDate = {},
            onAddSlot = { _, _ -> },
            onSetSlotWeekday = { _, _ -> },
            onMoveSlot = { _, _ -> },
            onRemoveSlot = {},
            onOpenTemplate = {},
            onOpenBalance = {},
            onOpenDeload = {},
            onOpenWeeks = {},
            onOpenProgression = {},
            onPreviewSlot = { _, _ -> },
            onClosePreview = {},
            onDeleteProgram = {},
            onBack = {},
            onExportProgram = {},
            onAddNewSlot = { _, _ -> },
            onRemovePhaseWeek = {},
            onDismissMessage = {},
            onDismissTransferMessage = {},
        )
    }
}
