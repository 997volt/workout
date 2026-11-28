package com.example.androidapp.ui.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.transfer.TrainingReportCsv
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.TrainingBlockChoice
import com.example.androidapp.domain.repository.TrainingReportRepository
import com.example.androidapp.platform.DocumentIo
import com.example.androidapp.platform.TrainingReportFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/** What the sheet says after a write; the strings live in the screen. */
enum class TrainingReportOutcome { WRITTEN, WRITE_FAILED }

/** The block sheet's state (ROADMAP N331). */
data class TrainingReportUiState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = false,
    val report: TrainingReport? = null,
    val blocks: List<TrainingBlockChoice> = emptyList(),
    /** A failed read of the block list; the report can still load, but Change has nothing to offer (N371). */
    val blocksError: DataError? = null,
    val error: DataError? = null,
    val writing: Boolean = false,
    val outcome: TrainingReportOutcome? = null,
    /** A staged file the screen should hand to the share sheet, then clear. */
    val shareFile: TrainingReportFile? = null,
) {
    /**
     * Whether the sheet may hand its CSV out.
     *
     * **No error** (N338): a failed read keeps the previous report so the sheet can retry, but that
     * report is not what the error line is describing. Writing it would export the block the lifter
     * moved away from while the sheet says the chosen one could not be read.
     */
    val canWrite: Boolean get() = report != null && error == null && !isLoading && !writing
}

/**
 * The one-file block export (ROADMAP N331).
 *
 * The sheet opens on the most recent sequence block; **Change** asks for another block or the last
 * eight weeks. The report is read when the sheet opens, not when History loads — a CSV is not worth a
 * read on every visit to the list. The download writes through [DocumentIo]; the share stages a cache
 * file and hands it to the screen, which owns the `startActivity` that a ViewModel may not.
 */
@HiltViewModel
class TrainingReportViewModel @Inject constructor(
    private val repository: TrainingReportRepository,
    private val documentIo: DocumentIo,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(TrainingReportUiState())
    val state: StateFlow<TrainingReportUiState> = _state.asStateFlow()

    fun open(fallbackName: String) {
        if (_state.value.isOpen) return
        _state.update { it.copy(isOpen = true, outcome = null, error = null) }
        loadLast(fallbackName)
    }

    fun close() {
        _state.update { it.copy(isOpen = false, outcome = null, shareFile = null) }
    }

    fun choose(programId: String) {
        load { repository.block(programId) }
    }

    fun lastWeeks(name: String) {
        val today = today()
        load { repository.range(today.minusWeeks(FALLBACK_WEEKS), today, name) }
    }

    /**
     * The chosen window, from the custom range's two dates (ROADMAP N332).
     *
     * The range is the repository's own read; the sheet and the file are unchanged, so an empty window
     * exports the same one-file CSV with no sessions rather than a second format. The caller names it,
     * as it names the last-eight-weeks window (N343).
     */
    fun custom(start: LocalDate, end: LocalDate, name: String) {
        load { repository.range(start, end, name) }
    }

    fun onShareFinished(shared: Boolean) {
        _state.update {
            it.copy(
                shareFile = null,
                outcome = if (shared) null else TrainingReportOutcome.WRITE_FAILED,
            )
        }
    }

    fun download(context: Context, uri: Uri) {
        if (!_state.value.canWrite) return
        val report = _state.value.report ?: return
        val file = reportFile(report)
        _state.update { it.copy(writing = true, outcome = null) }
        viewModelScope.launch {
            val written = documentIo.write(context, uri, file.csv)
            _state.update {
                it.copy(
                    writing = false,
                    outcome = if (written) TrainingReportOutcome.WRITTEN else TrainingReportOutcome.WRITE_FAILED,
                )
            }
        }
    }

    fun share() {
        if (!_state.value.canWrite) return
        val report = _state.value.report ?: return
        _state.update { it.copy(shareFile = reportFile(report)) }
    }

    private fun loadLast(fallbackName: String) {
        load { repository.lastBlock(fallbackName) }
    }

    private fun load(block: suspend () -> DataResult<TrainingReport>) {
        _state.update { it.copy(isLoading = true, error = null, outcome = null) }
        viewModelScope.launch {
            val recent = repository.recentBlocks()
            val blocks = (recent as? DataResult.Success)?.data.orEmpty()
            val blocksError = (recent as? DataResult.Failure)?.error
            when (val result = block()) {
                is DataResult.Success -> _state.update {
                    it.copy(isLoading = false, report = result.data, blocks = blocks, blocksError = blocksError)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error, blocks = blocks, blocksError = blocksError)
                }
            }
        }
    }

    private fun today(): LocalDate =
        Instant.ofEpochMilli(timeSource.nowEpochMillis()).atZone(ZoneId.systemDefault()).toLocalDate()

    private companion object {
        const val FALLBACK_WEEKS = 8L
    }
}

/** The file name the picker offers and the share stages. */
fun trainingReportFileName(report: TrainingReport): String = "workout-${slug(report.blockName)}-" +
    "${report.blockStart}-to-${report.blockEnd}.csv"

private fun TrainingReportViewModel.reportFile(report: TrainingReport): TrainingReportFile =
    TrainingReportFile(name = trainingReportFileName(report), csv = TrainingReportCsv.write(report))

/** A file-name-safe block name: lower case, runs of anything else collapsed to one dash. */
private fun slug(name: String): String {
    val builder = StringBuilder()
    var dashed = false
    name.lowercase(Locale.ROOT).forEach { character ->
        if (character.isLetterOrDigit()) {
            builder.append(character)
            dashed = false
        } else if (!dashed && builder.isNotEmpty()) {
            builder.append('-')
            dashed = true
        }
    }
    return builder.toString().trimEnd('-').ifEmpty { "block" }
}
