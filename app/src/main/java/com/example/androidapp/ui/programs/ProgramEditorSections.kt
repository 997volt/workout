package com.example.androidapp.ui.programs

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramPhases
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The program editor's blocks that are not the program's own rows (ROADMAP N178).
 *
 * A file of their own because the editor's screen is at the length and the function count this
 * project enforces, and because these are the two things about a program that are *not* its
 * schedule: what kind it is, when a block starts, and the days it orders.
 */

/** The editor's floating action button: the one way to add a day. */
@Composable
internal fun AddSlotFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        text = { Text(stringResource(R.string.program_add_slot)) },
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        modifier = Modifier.testTag(TestTags.Programs.ADD_SLOT),
    )
}

/**
 * The program's days, in the order they are done (P3.3, P3.9).
 *
 * The run's place is marked where the order is authored, and in a block it says which day of the
 * block that is rather than "next up" (N178).
 */
@Composable
internal fun ProgramSlotList(
    slots: List<ProgramSlot>,
    runSlotId: String?,
    isSequence: Boolean,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onPreviewSlot: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        itemsIndexed(items = slots, key = { _, slot -> slot.id }) { index, slot ->
            ProgramSlotBlock(
                slot = slot,
                isFirst = index == 0,
                isLast = index == slots.lastIndex,
                isRunPlace = slot.id == runSlotId,
                runLabel = if (isSequence) {
                    stringResource(R.string.program_sequence_day, index + 1, slots.size)
                } else {
                    null
                },
                isSequence = isSequence,
                onMoveUp = { onMoveSlot(slot.id, -1) },
                onMoveDown = { onMoveSlot(slot.id, 1) },
                onRemove = { onRemoveSlot(slot.id) },
                onOpenTemplate = { onOpenTemplate(slot.templateId) },
                onSetWeekday = { weekday -> onSetSlotWeekday(slot.id, weekday) },
                onOpenPreview = { onPreviewSlot(slot.templateId, slot.templateName) },
            )
            HorizontalDivider()
        }
    }
}

/**
 * Where the editor is in a weekly program's authored weeks (ROADMAP N292).
 *
 * The selection is the screen's own — it is where the lifter is looking, not a property of the
 * program — so it is held in saveable state and handed down as one value instead of five. The
 * `newDayWeek` is the week a newly added day names: null while the program is one repeating week,
 * which is exactly what its existing days are.
 */
internal class PhaseWeekSelection(
    val weeks: List<Int>,
    val selected: Int,
    /** The week a newly added day names, or null for the single repeating week (N292). */
    val newDayWeek: Int?,
    val onSelect: (Int) -> Unit,
    val onAdd: () -> Unit,
    val onRemove: (Int) -> Unit,
)

/**
 * The phase selection, remembered for the program on screen (ROADMAP N292).
 *
 * Keyed on the program's id, so the selection can never leak from one program's editor into
 * another's, and *Add week* opens the week after the last authored one rather than a gap.
 */
@Composable
internal fun rememberPhaseWeekSelection(
    programId: String?,
    slots: List<ProgramSlot>,
    isSequence: Boolean,
    onRemovePhaseWeek: (Int) -> Unit,
): PhaseWeekSelection {
    var selected by rememberSaveable(programId) {
        mutableIntStateOf(ProgramPhases.FIRST_WEEK)
    }
    val weeks = ProgramPhases.weeks(slots)
    return PhaseWeekSelection(
        weeks = weeks,
        selected = selected,
        newDayWeek = if (isSequence) {
            null
        } else {
            selected.takeIf { weeks.isNotEmpty() || it > ProgramPhases.FIRST_WEEK }
        },
        onSelect = { selected = it },
        onAdd = {
            selected = maxOf(selected, weeks.lastOrNull() ?: ProgramPhases.FIRST_WEEK) + 1
        },
        onRemove = { week ->
            // The weeks after the removal close the gap, so week 1 always exists when one does;
            // selecting it also leaves the editor off a week that is gone.
            selected = ProgramPhases.FIRST_WEEK
            onRemovePhaseWeek(week)
        },
    )
}

/**
 * The authored weeks of a weekly program (ROADMAP N292).
 *
 * One chip per week — the days themselves are the list below, so selecting a week is how the
 * lifter moves between prescriptions. *Add week* opens the next one; *Remove week* is offered only
 * for a week that has days, and asks first because its whole schedule leaves.
 *
 * A program with no phases shows week 1 and the days it always had; the hint says they repeat
 * every week until a second week is authored. Nothing here writes until a day is added to a new
 * week or a week is removed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhaseWeekSection(
    weeks: List<Int>,
    selectedWeek: Int,
    onSelect: (Int) -> Unit,
    onAddWeek: () -> Unit,
    onRemoveWeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingRemoval by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.program_phases),
                style = MaterialTheme.typography.titleMedium,
            )
            AssistChip(
                onClick = onAddWeek,
                label = { Text(stringResource(R.string.program_phase_add)) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.testTag(TestTags.Programs.PHASE_ADD),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The selected week is shown even when no day names it yet: that is the empty week a
            // lifter has just added and is about to fill.
            (weeks + selectedWeek).distinct().sorted().forEach { week ->
                FilterChip(
                    selected = week == selectedWeek,
                    onClick = { onSelect(week) },
                    label = { Text(stringResource(R.string.program_phase_week, week)) },
                    modifier = Modifier.testTag(TestTags.Programs.phaseWeek(week)),
                )
            }
        }
        if (selectedWeek in weeks) {
            AppTextButton(
                onClick = { confirmingRemoval = true },
                modifier = Modifier.testTag(TestTags.Programs.PHASE_REMOVE),
            ) {
                Text(stringResource(R.string.program_phase_remove))
            }
        }
        Text(
            text = stringResource(
                if (weeks.isEmpty()) {
                    R.string.program_phase_single_hint
                } else {
                    R.string.program_phase_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (confirmingRemoval) {
        RemovePhaseWeekDialog(
            week = selectedWeek,
            onDismiss = { confirmingRemoval = false },
            onConfirm = {
                confirmingRemoval = false
                onRemoveWeek(selectedWeek)
            },
        )
    }
}

/** The question a week's removal asks first: its days leave, the workouts they name stay (N292). */
@Composable
private fun RemovePhaseWeekDialog(week: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_phase_remove_title, week)) },
        text = { Text(stringResource(R.string.program_phase_remove_text)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Programs.PHASE_REMOVE_CONFIRM),
            ) {
                Text(stringResource(R.string.program_phase_remove_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// The kind section, its start-date row and its dialog: the block's three controls (N178).
/**
 * A week that repeats, or an order that ends — and, for a block, when it starts (ROADMAP N178).
 *
 * The two chips are the choice N174 stores; the start date is the anchor a block's first due date is
 * read from, and it is not shown for a weekly program because its weeks are the calendar's. A date
 * the lifter never picks stays null, which means "the day the program was created" — the fallback
 * the schedule already applies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgramKindSection(
    program: WorkoutProgram,
    isComplete: Boolean,
    /** True when the program authors phase weeks, which need the start day too (ROADMAP N292). */
    hasPhases: Boolean,
    onSetKind: (ProgramKind) -> Unit,
    onSetStartDate: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // One row, because the editor's header is already the name and the active switch and the
        // list of days must stay the thing the screen is about: a title above two chips cost a line
        // that every block paid for and said nothing the chips do not.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.program_kind),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = program.kind == ProgramKind.WEEKLY,
                onClick = { onSetKind(ProgramKind.WEEKLY) },
                label = { Text(stringResource(R.string.program_kind_weekly)) },
                modifier = Modifier.testTag(TestTags.Programs.KIND_WEEKLY),
            )
            FilterChip(
                selected = program.kind == ProgramKind.SEQUENCE,
                onClick = { onSetKind(ProgramKind.SEQUENCE) },
                label = { Text(stringResource(R.string.program_kind_sequence)) },
                modifier = Modifier.testTag(TestTags.Programs.KIND_SEQUENCE),
            )
        }
        if (program.kind == ProgramKind.SEQUENCE) {
            Text(
                text = stringResource(R.string.program_kind_sequence_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (isComplete) {
            // B197: every session settled, so the block is done rather than waiting at its last day.
            Text(
                text = stringResource(R.string.program_block_complete),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(TestTags.Programs.BLOCK_COMPLETE),
            )
        }
        StartOfProgramSection(program = program, hasPhases = hasPhases, onChange = { picking = true })
    }

    if (picking) {
        StartDateDialog(
            initial = program.startDate ?: LocalDate.now(),
            onSet = { date ->
                onSetStartDate(date)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/**
 * The day a block starts, or the day a phased weekly program's rotation is measured from (N292).
 *
 * Both are the same fact — an anchor — so they share one row; a plain weekly program keeps none,
 * because its weeks are the calendar's.
 */
@Composable
private fun StartOfProgramSection(
    program: WorkoutProgram,
    hasPhases: Boolean,
    onChange: () -> Unit,
) {
    if (program.kind != ProgramKind.SEQUENCE && !hasPhases) return
    StartDateRow(startDate = program.startDate, onChange = onChange)
    if (program.kind == ProgramKind.WEEKLY) {
        Text(
            text = stringResource(R.string.program_phase_anchor_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The day a block starts, and the control that changes it (ROADMAP N178). */
@Composable
private fun StartDateRow(startDate: LocalDate?, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.program_starts_on,
                startDate?.let(::formatProgramDay)
                    ?: stringResource(R.string.program_starts_created),
            ),
            modifier = Modifier.testTag(TestTags.Programs.START_DATE),
        )
        AppTextButton(
            onClick = onChange,
            modifier = Modifier.testTag(TestTags.Programs.START_DATE_CHANGE),
        ) {
            Text(stringResource(R.string.program_starts_change))
        }
    }
}

/** The date picker a block's start is chosen in, split out to keep the section short. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDateDialog(
    initial: LocalDate,
    onSet: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppTextButton(
                onClick = {
                    onSet(
                        state.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        },
                    )
                },
                modifier = Modifier.testTag(TestTags.Programs.START_DATE_SET),
            ) {
                Text(stringResource(R.string.program_starts_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Programs.START_DATE_CANCEL),
            ) {
                Text(stringResource(R.string.program_edit_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
