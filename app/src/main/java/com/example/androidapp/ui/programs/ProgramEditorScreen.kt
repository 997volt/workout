package com.example.androidapp.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.summary
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.shortLabel
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme
import java.time.DayOfWeek

/**
 * One program: its name, whether it is the active one, and its ordered slots (ROADMAP P3.3).
 *
 * The order is the feature: a slot's weekday answers "what happens on a Tuesday", and its
 * position answers "which one is next". Both are edited here, and neither is inferred. What a slot
 * *prescribes* is edited behind it (P3.8), because a slot pointing at a template is a schedule
 * until it says what to do.
 */
@Composable
fun ProgramEditorRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    // Exporting reports through the editor's own host (ROADMAP N47). The file write is the
    // composable's, because it holds the `Uri`.
    var transferMessage by remember { mutableStateOf<String?>(null) }
    val exportProgram = rememberProgramExport(viewModel) { transferMessage = it }

    // Deleting — or opening a program that is already gone — leaves the editor, rather than
    // leaving an empty shell behind with a live Delete button.
    LaunchedEffect(deleted, state.notFound) {
        if (deleted || state.notFound) currentOnBack()
    }

    ProgramEditorScreen(
        state = state,
        onRename = viewModel::onRename,
        onSetActive = viewModel::onSetActive,
        onAddSlot = viewModel::onAddSlot,
        onSetSlotWeekday = viewModel::onSetSlotWeekday,
        onMoveSlot = viewModel::onMoveSlot,
        onRemoveSlot = viewModel::onRemoveSlot,
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
@Composable
fun ProgramEditorScreen(
    state: ProgramEditorUiState,
    onRename: (String) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onAddSlot: (String) -> Unit,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onPreviewSlot: (String, String) -> Unit,
    onClosePreview: () -> Unit,
    onDeleteProgram: () -> Unit,
    onBack: () -> Unit,
    /** Required rather than defaulted, so a route that forgets it fails the build (B49, B52). */
    onExportProgram: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissMessage: () -> Unit = {},
    transferMessage: String? = null,
    onDismissTransferMessage: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var pickingTemplate by rememberSaveable { mutableStateOf(false) }

    EditorErrorMessage(
        error = state.error,
        snackbarHostState = snackbarHostState,
        onDismiss = onDismissMessage,
    )
    MessageSnackbar(transferMessage, snackbarHostState, onDismissTransferMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProgramEditorTopBar(
                name = state.program?.name,
                onBack = onBack,
                onDelete = { confirmingDelete = true },
                onExport = onExportProgram,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickingTemplate = true },
                text = { Text(stringResource(R.string.program_add_slot)) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.testTag(TestTags.Programs.ADD_SLOT),
            )
        },
    ) { innerPadding ->
        ProgramEditorBody(
            state = state,
            onRename = onRename,
            onSetActive = onSetActive,
            onSetSlotWeekday = onSetSlotWeekday,
            onMoveSlot = onMoveSlot,
            onRemoveSlot = onRemoveSlot,
            onPreviewSlot = onPreviewSlot,
            modifier = Modifier.padding(innerPadding),
        )
    }

    ProgramEditorDialogs(
        state = state,
        pickingTemplate = pickingTemplate,
        confirmingDelete = confirmingDelete,
        onPick = { templateId ->
            pickingTemplate = false
            onAddSlot(templateId)
        },
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
 * are dialogs over the same state: which plan to add, whether to delete, and what a slot
 * prescribes.
 */
@Composable
private fun ProgramEditorDialogs(
    state: ProgramEditorUiState,
    pickingTemplate: Boolean,
    confirmingDelete: Boolean,
    onPick: (String) -> Unit,
    onDismissPicker: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClosePreview: () -> Unit,
) {
    if (pickingTemplate) {
        TemplatePickerDialog(
            templates = state.templates,
            onPick = onPick,
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
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    TopAppBar(
        title = { Text(name ?: stringResource(R.string.program_edit_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // Export is per program, so it is here rather than on the list (ROADMAP N47).
            AppTextButton(
                onClick = onExport,
                modifier = Modifier.testTag(TestTags.Programs.EXPORT),
            ) {
                Text(stringResource(R.string.program_export))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag(TestTags.Programs.DELETE),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.program_delete),
                )
            }
        },
    )
}

@Composable
private fun ProgramEditorBody(
    state: ProgramEditorUiState,
    onRename: (String) -> Unit,
    onSetActive: (Boolean) -> Unit,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onPreviewSlot: (String, String) -> Unit,
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
        }
        HorizontalDivider()

        if (state.slots.isEmpty()) {
            CenteredMessage(
                text = stringResource(R.string.program_no_slots),
                hint = stringResource(R.string.program_no_slots_hint),
                modifier = Modifier.testTag(TestTags.Programs.NO_SLOTS),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(items = state.slots, key = { _, slot -> slot.id }) { index, slot ->
                    ProgramSlotBlock(
                        slot = slot,
                        isFirst = index == 0,
                        isLast = index == state.slots.lastIndex,
                        // The place the run is at, marked where the order is authored (P3.9).
                        isRunPlace = slot.id == state.runSlotId,
                        onMoveUp = { onMoveSlot(slot.id, -1) },
                        onMoveDown = { onMoveSlot(slot.id, 1) },
                        onRemove = { onRemoveSlot(slot.id) },
                        onSetWeekday = { weekday -> onSetSlotWeekday(slot.id, weekday) },
                        onOpenPreview = { onPreviewSlot(slot.templateId, slot.templateName) },
                    )
                    HorizontalDivider()
                }
            }
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
            Text(
                text = stringResource(R.string.program_active),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(
                checked = program.isActive,
                onCheckedChange = onSetActive,
                modifier = Modifier.testTag(TestTags.Programs.ACTIVE),
            )
        }
        Text(
            text = stringResource(R.string.program_active_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** One slot: its template, the order it sits in, the day it falls on, and what it prescribes. */
@Composable
private fun ProgramSlotBlock(
    slot: ProgramSlot,
    isFirst: Boolean,
    isLast: Boolean,
    isRunPlace: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
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
                            text = stringResource(R.string.program_next_up),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
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
                )
            },
            modifier = Modifier.testTag(TestTags.Programs.slot(slot.id)),
        )
        SlotWeekdayPicker(slot = slot, onSelect = onSetWeekday)
    }
}

/**
 * One slot's ⋮: its order, and the removal that takes the day out of the schedule (ROADMAP N72).
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
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotWeekdayPicker(
    slot: ProgramSlot,
    onSelect: (DayOfWeek?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.template_weekday),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayOfWeek.entries.forEach { day ->
            val selected = slot.weekday == day
            FilterChip(
                selected = selected,
                // Tapping the chosen day again makes the slot order-only.
                onClick = { onSelect(if (selected) null else day) },
                label = { Text(day.shortLabel()) },
                modifier = Modifier.testTag(TestTags.Programs.slotWeekday(slot.id, day.name)),
            )
        }
    }
}

/** The templates a slot can point at (P3.3): the plan has to exist before it can be ordered. */
@Composable
private fun TemplatePickerDialog(
    templates: List<WorkoutTemplate>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.Programs.PICKER),
        title = { Text(stringResource(R.string.program_pick_title)) },
        text = {
            if (templates.isEmpty()) {
                CenteredMessage(
                    text = stringResource(R.string.program_pick_empty),
                    hint = stringResource(R.string.program_pick_empty_hint),
                    modifier = Modifier.testTag(TestTags.Programs.PICKER_EMPTY),
                )
            } else {
                LazyColumn {
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
            onAddSlot = {},
            onSetSlotWeekday = { _, _ -> },
            onMoveSlot = { _, _ -> },
            onRemoveSlot = {},
            onPreviewSlot = { _, _ -> },
            onClosePreview = {},
            onDeleteProgram = {},
            onBack = {},
            onExportProgram = {},
        )
    }
}
