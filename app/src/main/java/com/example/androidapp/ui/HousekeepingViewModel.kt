package com.example.androidapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.data.local.WorkoutDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The failed housekeeping state and its retry, for the notice that says it once (ROADMAP B324).
 *
 * The state itself lives in [Housekeeping] rather than here: the failure is produced by the database's open,
 * which happens before any screen has a ViewModel, and the Diagnostics screen reads the same state later.
 */
@HiltViewModel
class HousekeepingViewModel @Inject constructor(
    private val housekeeping: Housekeeping,
    private val database: WorkoutDatabase,
) : ViewModel() {

    val failure: StateFlow<Housekeeping.Failure?> = housekeeping.failure

    private val _retrying = MutableStateFlow(false)
    val retrying: StateFlow<Boolean> = _retrying.asStateFlow()

    /** Runs the pass again; the notice clears only if it finishes. */
    fun retry() {
        if (_retrying.value) return
        viewModelScope.launch {
            _retrying.value = true
            housekeeping.retry(database)
            _retrying.value = false
        }
    }

    /** "Later": the next open tries again, and the recorded failure stays in Diagnostics. */
    fun dismiss() = housekeeping.dismiss()
}
