package com.example.androidapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.platform.CrashLog
import com.example.androidapp.platform.CrashLogStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the Diagnostics screen shows: the build, the last crash and the last failed pass (ROADMAP N244). */
data class DiagnosticsUiState(
    val appVersion: String = "",
    val versionCode: Int = 0,
    val crash: CrashLog? = null,
    val repair: Housekeeping.Failure? = null,
    val retrying: Boolean = false,
)

/**
 * Diagnostics: the records a local-only app has instead of a crash reporter (ROADMAP N244).
 *
 * The crash log is read once when the screen opens; the repair failure is observed because a retry from
 * either this screen or the shell's notice changes the same state.
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val crashLogStore: CrashLogStore,
    private val housekeeping: Housekeeping,
    private val database: WorkoutDatabase,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val version = versionOf(context)

    private val _state = MutableStateFlow(
        DiagnosticsUiState(
            appVersion = version.first,
            versionCode = version.second,
            crash = crashLogStore.latest(),
        ),
    )
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            housekeeping.failure.collect { failure -> _state.update { it.copy(repair = failure) } }
        }
    }

    /** Runs the open-time pass again; the failure clears only if it finishes. */
    fun retry() {
        if (_state.value.retrying) return
        viewModelScope.launch {
            _state.update { it.copy(retrying = true) }
            housekeeping.retry(database)
            _state.update { it.copy(retrying = false) }
        }
    }

    private fun versionOf(context: Context): Pair<String, Int> {
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()

        @Suppress("DEPRECATION") // versionCode: fine on minSdk 26, longVersionCode is 28+
        val code = info?.versionCode ?: 0
        return (info?.versionName ?: "unknown") to code
    }
}
