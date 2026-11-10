package com.example.androidapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.BackupRunner
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The settings screen's state (ROADMAP N21).
 *
 * It renders what is *stored* rather than what was tapped: a choice that failed to reach
 * disk must not look applied, or the next workout silently uses the old rest.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRunner: BackupRunner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeDefaultRestSeconds().collect { seconds ->
                _uiState.update { it.copy(defaultRestSeconds = seconds) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRestCueEnabled().collect { enabled ->
                _uiState.update { it.copy(restCueEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeKeepScreenOn().collect { enabled ->
                _uiState.update { it.copy(keepScreenOn = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRestTimerEnabled().collect { enabled ->
                _uiState.update { it.copy(restTimerEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeProgressionMode().collect { mode ->
                _uiState.update { it.copy(progressionMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeWeightUnit().collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeDeloadSuggestionWeeks().collect { weeks ->
                _uiState.update { it.copy(deloadSuggestionWeeks = weeks) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeBackupSettings().collect { backup ->
                _uiState.update { it.copy(backup = backup) }
            }
        }
    }

    fun onSetRestCue(enabled: Boolean) {
        write { settingsRepository.setRestCueEnabled(enabled) }
    }

    fun onSetKeepScreenOn(enabled: Boolean) {
        write { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun onSetRestTimer(enabled: Boolean) {
        write { settingsRepository.setRestTimerEnabled(enabled) }
    }

    fun onSetProgressionMode(mode: ProgressionMode) {
        write { settingsRepository.setProgressionMode(mode) }
    }

    fun onSetWeightUnit(unit: WeightUnit) {
        write { settingsRepository.setWeightUnit(unit) }
    }

    fun onSetDeloadSuggestion(weeks: Int) {
        write { settingsRepository.setDeloadSuggestionWeeks(weeks) }
    }

    fun onEnableBackup(enabled: Boolean) {
        write { settingsRepository.setBackupEnabled(enabled) }
    }

    fun onSetBackupSchedule(schedule: BackupSchedule) {
        write { settingsRepository.setBackupSchedule(schedule) }
    }

    /** Remembers the folder a backup writes to; the picker has already taken the grant (ROADMAP N218). */
    fun onChooseBackupFolder(folder: BackupFolder) {
        write { settingsRepository.setBackupFolder(folder) }
    }

    /** Writes one copy now, whatever the period says (B362); the backup flow shows the result. */
    fun onBackupNow() {
        if (_uiState.value.backupRunning) return
        _uiState.update { it.copy(backupRunning = true) }
        viewModelScope.launch {
            try {
                backupRunner.runNow()
            } finally {
                _uiState.update { it.copy(backupRunning = false) }
            }
        }
    }

    /** One place where a failed write becomes the error the screen shows. */
    private fun write(action: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            when (val result = action()) {
                is DataResult.Success -> _uiState.update { it.copy(error = null) }
                is DataResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    /** Dismisses the snackbar's write failure; the stored value is unchanged. */
    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun onSetDefaultRest(seconds: Int) {
        viewModelScope.launch {
            when (val result = settingsRepository.setDefaultRestSeconds(seconds)) {
                is DataResult.Success -> _uiState.update { it.copy(error = null) }
                is DataResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }
}

/** What the settings screen shows. */
data class SettingsUiState(
    val defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
    /** Whether a finished rest is heard as well as seen (ROADMAP N27). */
    val restCueEnabled: Boolean = true,
    /** Whether a workout keeps the screen awake (ROADMAP N27). */
    val keepScreenOn: Boolean = true,
    /**
     * Whether a rest is counted down at all (ROADMAP N44).
     *
     * Off means the rest is still shown — the exercise's own prescription as a fixed label — but
     * nothing is counted, nothing is adjusted and nothing chimes.
     */
    val restTimerEnabled: Boolean = true,
    /** What *Done* does with the step a plan earned (P3.17); ask by default. */
    val progressionMode: ProgressionMode = ProgressionMode.SUGGEST,
    /** The unit every weight is read in unless an exercise names its own (ROADMAP N64). */
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** How often the recovery screen suggests a deload; 0 is off (ROADMAP N301). */
    val deloadSuggestionWeeks: Int = 0,
    /** The backup that writes itself: switch, period, folder and where the last attempt ended (N218). */
    val backup: BackupSettings = BackupSettings(),
    /** True while a manual copy is in flight, so repeated taps cannot queue copies (B380). */
    val backupRunning: Boolean = false,
    val error: DataError? = null,
) {
    val choices: List<Int> get() = REST_CHOICES

    companion object {
        /**
         * The rests worth offering as one tap.
         *
         * Covers a warm-up pause through a long strength single. It is a bounded list and
         * not a number field because a typed zero would be an alarm that fires instantly.
         */
        val REST_CHOICES = listOf(30, 45, 60, 75, 90, 120, 150, 180, 240, 300)
    }
}
