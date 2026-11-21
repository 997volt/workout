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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.ui.components.AppTextButton
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
                        onChoose = onChoose,
                        onLastWeeks = onLastWeeks,
                    )
                }
                state.outcome?.let { outcome ->
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

@Composable
private fun ReportContent(
    report: TrainingReport,
    blocks: List<com.example.androidapp.domain.repository.TrainingBlockChoice>,
    onChoose: (String) -> Unit,
    onLastWeeks: () -> Unit,
) {
    val start = text(report.blockStart)
    val end = text(report.blockEnd)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.history_block_window, report.blockName, start, end))
        Text(
            stringResource(
                R.string.history_block_counts,
                report.sessions.size,
                report.sessions.sumOf { session ->
                    session.exercises.sumOf { exercise -> exercise.sets.count { set -> set.performed } }
                },
                report.sessions.sumOf { session ->
                    session.exercises.sumOf { exercise -> exercise.sets.count { set -> !set.performed } }
                },
            ),
        )
        Text(stringResource(R.string.history_block_includes_title))
        Text(stringResource(R.string.history_block_includes))
        Text(stringResource(R.string.history_block_warning))
        ChangeMenu(blocks = blocks, onChoose = onChoose, onLastWeeks = onLastWeeks)
    }
}

@Composable
private fun ChangeMenu(
    blocks: List<com.example.androidapp.domain.repository.TrainingBlockChoice>,
    onChoose: (String) -> Unit,
    onLastWeeks: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppTextButton(onClick = { open = true }) {
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
        }
    }
}

private fun text(date: LocalDate): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(date)
