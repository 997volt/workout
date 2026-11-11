package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.model.AutoProgressionCheck
import com.example.androidapp.domain.model.AutoProgressionPreflight
import com.example.androidapp.domain.model.autoProgressionPreflight
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.ProgressionRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.ui.navigation.ProgramProgression
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** What the auto progression screen shows (ROADMAP N299). */
data class ProgramProgressionUiState(
    val isLoading: Boolean = true,
    val programName: String = "",
    val autoProgression: Boolean = false,
    val checks: List<AutoProgressionCheck> = emptyList(),
    /** When the current baseline was captured, or null when the switch has never been on. */
    val baselineAt: Instant? = null,
    val error: DataError? = null,
    /** A write is in flight, so the switch and reset are not offered twice. */
    val writing: Boolean = false,
    /** The confirmation the screen is showing, if any. */
    val confirmEnable: Boolean = false,
    val confirmReset: Boolean = false,
    /** Sets the last reset restored, for the one message it is worth saying. */
    val restored: Int? = null,
) {
    /** The groups the preflight lists; derived so the checks are the one stored source. */
    val preflight: AutoProgressionPreflight get() = AutoProgressionPreflight(checks)

    /** The program is gone, so the screen has nothing to offer. */
    val notFound: Boolean get() = !isLoading && error == DataError.NotFound
}

/**
 * One program's auto progression switch, preflight and baseline (ROADMAP N299).
 *
 * A one-shot snapshot rather than a set of live flows: this is a settings screen for one program, and
 * every action it offers writes and then re-reads the same snapshot. The preflight is the pure
 * [autoProgressionPreflight] over the plans the program schedules, so the sheet and the enablement
 * cannot disagree about what will move.
 */
@HiltViewModel
class ProgramProgressionViewModel @Inject constructor(
    private val programs: ProgramRepository,
    private val templates: TemplateRepository,
    private val progression: ProgressionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramProgression>().programId

    private val _state = MutableStateFlow(ProgramProgressionUiState())
    val uiState: StateFlow<ProgramProgressionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    /** Reads the program, its plans and its baseline, keeping a failed read a failure. */
    private suspend fun refresh() {
        when (val read = dataResultOf { snapshot() }) {
            is DataResult.Success -> {
                val snapshot = read.data
                _state.update {
                    it.copy(
                        isLoading = false,
                        programName = snapshot.name,
                        autoProgression = snapshot.autoProgression,
                        checks = snapshot.checks,
                        baselineAt = snapshot.baselineAt,
                        error = null,
                    )
                }
            }

            is DataResult.Failure -> {
                // A failed read is not "this program has nothing": the screen says the read failed
                // rather than offering a switch over an empty snapshot.
                _state.update { it.copy(isLoading = false, error = read.error) }
            }
        }
    }

    private suspend fun snapshot(): Snapshot {
        val program = programs.observeProgram(programId).first()
            ?: throw NotFoundException("program $programId")
        val slots = programs.observeSlots(programId).first()
        val byTemplate = slots.distinctBy { it.templateId }.associate { slot ->
            slot.templateId to templates.observeExercises(slot.templateId).first()
        }
        return Snapshot(
            name = program.name,
            autoProgression = program.autoProgression,
            checks = autoProgressionPreflight(slots, byTemplate).checks,
            baselineAt = progression.baselineCapturedAt(programId).getOrNull()?.let(Instant::ofEpochMilli),
        )
    }

    /** Off is immediate; on asks first, because it is the one that lets the app write (N299). */
    fun onSetAuto(enabled: Boolean) {
        if (enabled) {
            _state.update { it.copy(confirmEnable = true) }
        } else {
            write { progression.setAutoProgression(programId, false) }
        }
    }

    fun onConfirmEnable() {
        _state.update { it.copy(confirmEnable = false) }
        write { progression.setAutoProgression(programId, true) }
    }

    fun onDismissEnable() {
        _state.update { it.copy(confirmEnable = false) }
    }

    fun onResetToBaseline() {
        // One write at a time (B414): the button is disabled while it runs, and the action refuses a
        // second start even if a stale composition still offers it.
        if (_state.value.writing) return
        _state.update { it.copy(confirmReset = true) }
    }

    fun onConfirmReset() {
        if (_state.value.writing) return
        _state.update { it.copy(confirmReset = false, writing = true) }
        viewModelScope.launch {
            when (val result = progression.resetToBaseline(programId)) {
                is DataResult.Success -> {
                    _state.update { it.copy(writing = false, restored = result.data) }
                    refresh()
                }

                is DataResult.Failure -> _state.update { it.copy(writing = false, error = result.error) }
            }
        }
    }

    fun onDismissReset() {
        _state.update { it.copy(confirmReset = false) }
    }

    /** The error has been read, so it is not shown again. */
    fun onErrorShown() {
        _state.update { it.copy(error = null) }
    }

    /** The reset message has been read, so it is not shown again. */
    fun onRestoredShown() {
        _state.update { it.copy(restored = null) }
    }

    private fun write(block: suspend () -> DataResult<Unit>) {
        _state.update { it.copy(writing = true) }
        viewModelScope.launch {
            when (val result = block()) {
                is DataResult.Success -> {
                    _state.update { it.copy(writing = false) }
                    refresh()
                }

                is DataResult.Failure -> _state.update { it.copy(writing = false, error = result.error) }
            }
        }
    }

    /** The three facts the screen reads, once. */
    private data class Snapshot(
        val name: String,
        val autoProgression: Boolean,
        val checks: List<AutoProgressionCheck>,
        val baselineAt: Instant?,
    )
}
