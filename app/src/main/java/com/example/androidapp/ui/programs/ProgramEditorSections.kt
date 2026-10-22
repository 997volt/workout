package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
    state: ProgramEditorUiState,
    onSetSlotWeekday: (String, DayOfWeek?) -> Unit,
    onMoveSlot: (String, Int) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onPreviewSlot: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSequence = state.program?.kind == ProgramKind.SEQUENCE
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        itemsIndexed(items = state.slots, key = { _, slot -> slot.id }) { index, slot ->
            ProgramSlotBlock(
                slot = slot,
                isFirst = index == 0,
                isLast = index == state.slots.lastIndex,
                isRunPlace = slot.id == state.runSlotId,
                runLabel = if (isSequence) {
                    stringResource(R.string.program_sequence_day, index + 1, state.slots.size)
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
        if (program.kind == ProgramKind.SEQUENCE) {
            StartDateRow(startDate = program.startDate, onChange = { picking = true })
        }
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
                startDate?.let(::formatStartDate)
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

/** A block's start, in the reader's own locale — an authored day, not a stored number. */
private fun formatStartDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

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
            ) {
                Text(stringResource(R.string.program_starts_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.program_edit_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
