package com.example.androidapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.DatePrompt
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The one sheet of the block export (ROADMAP N331).
 *
 * Everything is included; there are no toggles and no format picker. What remains is the review the
 * user gives before the file leaves the app: the window, the includes, the health-data line, and the
 * two actions. **Change** is the only choice.
 */
@Composable
internal fun TrainingReportSheet(
    state: TrainingReportUiState,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onChoose: (String) -> Unit,
    onLastWeeks: () -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_SHEET),
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.history_block_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    state.isLoading -> Text(stringResource(R.string.history_block_loading))
                    state.error != null -> Text(dataErrorMessage(state.error))
                    state.report != null -> ReportContent(
                        report = state.report,
                        blocks = state.blocks,
                        blocksError = state.blocksError,
                        onChoose = onChoose,
                        onLastWeeks = onLastWeeks,
                        onCustom = onCustom,
                    )
                }
                state.outcome?.let { OutcomeLine(it) }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDownload,
                enabled = state.canWrite,
                modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_DOWNLOAD),
            ) {
                Text(stringResource(R.string.history_block_download))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTextButton(
                    onClick = onShare,
                    enabled = state.canWrite,
                    modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_SHARE),
                ) {
                    Text(stringResource(R.string.history_block_share))
                }
                AppTextButton(onClick = onClose) {
                    Text(stringResource(R.string.history_block_close))
                }
            }
        },
    )
}

/** What the write answered, under the review it was written from. */
@Composable
private fun OutcomeLine(outcome: TrainingReportOutcome) {
    Text(
        text = stringResource(
            if (outcome == TrainingReportOutcome.WRITTEN) {
                R.string.history_block_written
            } else {
                R.string.history_block_write_failed
            },
        ),
        modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_OUTCOME),
    )
}

@Composable
private fun ReportContent(
    report: TrainingReport,
    blocks: List<com.example.androidapp.domain.repository.TrainingBlockChoice>,
    blocksError: com.example.androidapp.domain.DataError?,
    onChoose: (String) -> Unit,
    onLastWeeks: () -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
) {
    val start = text(report.blockStart)
    val end = text(report.blockEnd)
    var customOpen by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.history_block_window, report.blockName, start, end))
        val sessionCount = report.sessions.size
        val setCount = report.sessions.sumOf { session ->
            session.exercises.sumOf { exercise -> exercise.sets.count { set -> set.performed } }
        }
        val notDone = report.sessions.sumOf { session ->
            session.exercises.sumOf { exercise -> exercise.sets.count { set -> !set.performed } }
        }
        Text(
            text = listOf(
                pluralStringResource(R.plurals.history_block_sessions, sessionCount, sessionCount),
                pluralStringResource(R.plurals.history_block_sets, setCount, setCount),
                pluralStringResource(R.plurals.history_block_planned_not_done, notDone, notDone),
            ).joinToString(" · "),
        )
        Text(stringResource(R.string.history_block_includes_title))
        Text(stringResource(R.string.history_block_includes))
        Text(stringResource(R.string.history_block_warning))
        // A failed list read is said rather than drawn as "no blocks" (N371).
        blocksError?.let { Text(dataErrorMessage(it)) }
        ChangeMenu(
            blocks = blocks,
            onChoose = onChoose,
            onLastWeeks = onLastWeeks,
            onCustom = { customOpen = true },
        )
    }
    if (customOpen) {
        CustomRangeDialog(
            onDismiss = { customOpen = false },
            onConfirm = { from, to ->
                customOpen = false
                onCustom(from, to)
            },
        )
    }
}

@Composable
private fun ChangeMenu(
    blocks: List<com.example.androidapp.domain.repository.TrainingBlockChoice>,
    onChoose: (String) -> Unit,
    onLastWeeks: () -> Unit,
    onCustom: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_CHANGE),
        ) {
            Text(stringResource(R.string.history_block_change))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            blocks.forEach { block ->
                DropdownMenuItem(
                    text = { Text("${block.name} · ${text(block.start)}") },
                    onClick = {
                        open = false
                        onChoose(block.programId)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_block_last_weeks)) },
                onClick = {
                    open = false
                    onLastWeeks()
                },
            )
            // The one window the sheet did not surface: two dates chosen by the lifter (N332).
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_block_custom)) },
                onClick = {
                    open = false
                    onCustom()
                },
            )
        }
    }
}

/**
 * The custom window: From and To, each chosen in its own calendar, then Apply (ROADMAP N332).
 *
 * The same shape as the statistics custom range (N35) and the same UTC reading of the picker's day, so
 * "From 1 August" does not become 31 July west of Greenwich. Apply is refused until both ends are set: a
 * window with one end is no window. The chosen range goes through the repository's existing range read,
 * so the sheet, the CSV and the health-data warning are unchanged; an empty window exports the same file
 * with no sessions.
 */
@Composable
private fun CustomRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    // Stored as days rather than LocalDate, which has no bundle saver (B465).
    var fromDay by rememberSaveable { mutableStateOf<String?>(null) }
    var toDay by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val from = fromDay?.let(LocalDate::parse)
    val to = toDay?.let(LocalDate::parse)

    AlertDialog(
        modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_CUSTOM),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_block_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRow(
                    labelRes = R.string.history_block_custom_from,
                    date = from,
                    testTag = TestTags.HISTORY_BLOCK_CUSTOM_FROM,
                    onClick = { picking = true },
                )
                DateRow(
                    labelRes = R.string.history_block_custom_to,
                    date = to,
                    testTag = TestTags.HISTORY_BLOCK_CUSTOM_TO,
                    onClick = { picking = false },
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { from?.let { start -> to?.let { end -> onConfirm(start, end) } } },
                enabled = from != null && to != null,
                modifier = Modifier.testTag(TestTags.HISTORY_BLOCK_CUSTOM_APPLY),
            ) {
                Text(stringResource(R.string.history_block_custom_apply))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    picking?.let { choosing ->
        DatePrompt(
            initial = if (choosing) from else to,
            onDismiss = { picking = null },
            onPick = { date ->
                if (choosing) fromDay = date.toString() else toDay = date.toString()
                picking = null
            },
        )
    }
}

/** One end of the window: what it is, what it currently says, and the way to change it. */
@Composable
private fun DateRow(labelRes: Int, date: LocalDate?, testTag: String, onClick: () -> Unit) {
    val shown = date?.let(::text) ?: stringResource(R.string.history_block_custom_no_date)
    AppTextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Text(stringResource(labelRes) + ": " + shown)
    }
}

private fun text(date: LocalDate): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(date)
