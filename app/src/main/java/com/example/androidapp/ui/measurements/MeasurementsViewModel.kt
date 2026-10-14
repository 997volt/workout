package com.example.androidapp.ui.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.repository.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the measurements screen renders (ROADMAP N32). */
data class MeasurementsUiState(
    val isLoading: Boolean = true,
    /** Entries, newest first — the order the list and the chart both read in. */
    val entries: List<BodyMeasurement> = emptyList(),
)

/**
 * The measurements screen's state (ROADMAP N32).
 *
 * Small on purpose: the rules are the repository's — one entry to a day, an unmeasured site left blank —
 * and this only carries what was typed to it and surfaces what came back.
 */
@HiltViewModel
class MeasurementsViewModel @Inject constructor(
    private val repository: MeasurementRepository,
) : ViewModel() {

    private val lastError = MutableStateFlow<DataError?>(null)

    val uiState: StateFlow<MeasurementsUiState> = repository.observeAll()
        .map { entries -> MeasurementsUiState(isLoading = false, entries = entries) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MeasurementsUiState(),
        )

    val error: StateFlow<DataError?> = lastError.asStateFlow()

    /**
     * The entry a delete just took, awaiting the snackbar's undo (ROADMAP N133).
     *
     * Held until the message is taken, one at a time, the shape the workout and template screens use
     * (N7, B3, N90). The screen reads this rather than the repository, so the undo cannot outlive its
     * own snackbar.
     */
    private val _pendingUndo = MutableStateFlow<BodyMeasurement?>(null)

    val pendingUndo: StateFlow<BodyMeasurement?> = _pendingUndo.asStateFlow()

    /**
     * Saves an entry.
     *
     * An entry with a blank id is a new one, placed by its day; one with an id edits that row. Which is
     * which is the repository's decision to make, since it is the one that can see the day.
     */
    fun onSave(measurement: BodyMeasurement) {
        viewModelScope.launch {
            when (val result = repository.save(measurement)) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    fun onDelete(id: String) {
        // The row is read before it goes: what the snackbar offers back is the entry itself, and after
        // the write it is no longer in the list (N133).
        val entry = uiState.value.entries.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _pendingUndo.value = entry
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /**
     * Puts a deleted entry back (ROADMAP N133).
     *
     * The delete was soft, so the way back is clearing `deletedAt` rather than re-inserting anything —
     * N90's mechanism. The collision is the repository's to refuse: a measurement is identified by its
     * **day and kind** (N32, N113), so if the day has been re-entered since, the restore must not
     * overwrite the newer reading. That refusal arrives as an [DataError.Invalid] with the sentence.
     */
    fun onUndoDelete() {
        val entry = _pendingUndo.value ?: return
        _pendingUndo.value = null
        viewModelScope.launch {
            when (val result = repository.restore(entry.id)) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    fun onDismissUndo() {
        _pendingUndo.value = null
    }

    fun onDismissError() {
        lastError.value = null
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
