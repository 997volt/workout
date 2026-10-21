package com.example.androidapp.ui.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.isDailyOn
import com.example.androidapp.domain.repository.GoalRepository
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.ui.statistics.BodyMetric
import com.example.androidapp.ui.statistics.EnergyAdjustment
import com.example.androidapp.ui.statistics.MetricKey
import com.example.androidapp.ui.statistics.MetricReading
import com.example.androidapp.ui.statistics.MetricSeries
import com.example.androidapp.ui.statistics.WEIGHT_METRIC_ID
import com.example.androidapp.ui.statistics.weighInVerdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What the measurements screen renders (ROADMAP N32). */
data class MeasurementsUiState(
    val isLoading: Boolean = true,
    /** Entries, newest first — the order the list and the chart both read in. */
    val entries: List<BodyMeasurement> = emptyList(),
    /**
     * Today's daily entry, or null while the day has none (B182).
     *
     * The home row opens the screen wanting **this** day: with an entry it is an edit, and without one it is
     * the blank form. A blank form on a day that already has numbers would overwrite them through
     * `findOnDay`, so the shortcut has to know which it is asking for.
     */
    val todayEntry: BodyMeasurement? = null,
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
    private val goals: GoalRepository,
    timeSource: TimeSource,
) : ViewModel() {

    /**
     * The day this screen calls today, captured with its zone (B182).
     *
     * Captured for the reason home captures its own (N114): the row and the write have to agree about which
     * day "today" is, and a screen left open across midnight keeps the day it opened on.
     */
    private val todayDate: LocalDate = timeSource.now().atZone(ZoneId.systemDefault()).toLocalDate()
    private val zone: ZoneId = ZoneId.systemDefault()

    private val lastError = MutableStateFlow<DataError?>(null)

    val uiState: StateFlow<MeasurementsUiState> = repository.observeAll()
        .map { entries ->
            MeasurementsUiState(
                isLoading = false,
                entries = entries,
                todayEntry = entries.firstOrNull { it.isDailyOn(todayDate, zone) },
            )
        }
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
     * What the weigh-in that was just saved says about the goal's rate (ROADMAP N136).
     *
     * Set by a save rather than derived from the flows, because it is a **line after the save**: a value read
     * from the flows would be drawn again on every return to the screen, which is the pursuit (P4.7) this
     * sentence must not have. Null is "nothing to say" — no goal, no weight in the entry, or too little
     * history to be sure — and the screen then says nothing at all.
     */
    private val _rateVerdict = MutableStateFlow<EnergyAdjustment?>(null)

    val rateVerdict: StateFlow<EnergyAdjustment?> = _rateVerdict.asStateFlow()

    /**
     * Saves an entry.
     *
     * An entry with a blank id is a new one, placed by its day; one with an id edits that row. Which is
     * which is the repository's decision to make, since it is the one that can see the day.
     *
     * A **weigh-in** — a daily entry that states a weight — is answered with what the trend implies about the
     * goal's rate (N136). The series is read back from the repository rather than patched together from the
     * entry, so the trend is the one the app actually holds, this save included.
     */
    fun onSave(measurement: BodyMeasurement) {
        viewModelScope.launch {
            when (val result = repository.save(measurement)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _rateVerdict.value = verdictFor(measurement)
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /**
     * The verdict this save implies, or null when there is nothing honest to say (ROADMAP N136).
     *
     * A tape day and a soreness-only morning are not weigh-ins (N113), and the floor inside [weighInVerdict]
     * is what keeps the line from firing on water weight.
     */
    private suspend fun verdictFor(measurement: BodyMeasurement): EnergyAdjustment? {
        if (measurement.kind != MeasurementKind.DAILY || measurement.weightGrams == null) return null
        val series = MetricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            readings = repository.observeAll().first()
                .mapNotNull { entry ->
                    entry.weightGrams?.let { grams -> MetricReading(entry.measuredAt, grams.toDouble()) }
                }
                .sortedBy { it.at },
        )
        // The period this metric has open (N137), read as the weight goal N135 gave it.
        val goal = goals.observeCurrent().first()[WEIGHT_METRIC_ID]?.weightGoal
        return series.weighInVerdict(goal, series.readings.lastOrNull()?.value?.toLong())
    }

    /** Takes the sentence away once its snackbar has been read (ROADMAP N136). */
    fun onDismissRateVerdict() {
        _rateVerdict.value = null
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
