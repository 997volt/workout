package com.example.androidapp.ui.transfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The whole-data clear's state, kept outside the Settings ViewModel (B470).
 *
 * The operation itself is made non-cancellable while it writes; this store is what lets the
 * answer outlive the screen that started it. A destination that is popped clears its ViewModel,
 * but not this object, so the next Settings composition can still say what happened. The flag
 * also keeps a second clear from starting while the first is alive.
 */
@Singleton
class DataClearStore @Inject constructor() {

    private val _clearing = MutableStateFlow(false)

    /** True while a clear is writing; the data row is disabled and a second start is ignored. */
    val clearing: StateFlow<Boolean> = _clearing.asStateFlow()

    private val _outcome = MutableStateFlow<ClearOutcome?>(null)

    /** The last clear's answer, until [forget] is called. */
    val outcome: StateFlow<ClearOutcome?> = _outcome.asStateFlow()

    /** Claims the one slot, or answers false when a clear is already running. */
    fun begin(): Boolean {
        if (_clearing.value) return false
        _clearing.value = true
        return true
    }

    /** Publishes the answer and leaves the running state. */
    fun publish(outcome: ClearOutcome) {
        _outcome.value = outcome
        _clearing.value = false
    }

    /** The write ended without an answer (it threw); the next clear may start. */
    fun end() {
        _clearing.value = false
    }

    /** The answer has been shown, so a later screen starts from empty. */
    fun forget() {
        _outcome.value = null
    }
}
