package com.example.androidapp.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val isLoading: Boolean = true,
    /**
     * The rows the list draws: every live workout, or only the visible ones while the filter is off.
     *
     * A hidden row is drawn **only** when [showHidden] is on, and then it is marked (ROADMAP N214) — the
     * list is the one surface that shows them, so the whole of "not offered" lives in this one split.
     */
    val templates: List<WorkoutTemplate> = emptyList(),
    /** How many live workouts are hidden, so the filter can say the number and hide itself when it is 0. */
    val hiddenCount: Int = 0,
    /** True while the hidden ones are drawn (ROADMAP N214): the filter's selected state. */
    val showHidden: Boolean = false,
    /**
     * True while a session is already open (ROADMAP N78).
     *
     * Starting is idempotent — the repository hands back the open session rather than making a second
     * — so this is not a safety rail: it stops a *Start* tapped on one template from silently taking
     * the lifter into the workout already running from another.
     */
    val hasActiveWorkout: Boolean = false,
    /** Set when a write failed, so the screen can say so instead of lying. */
    val error: DataError? = null,
)

/**
 * The template list (ROADMAP N3).
 *
 * Creating a template takes a name from the screen — a ViewModel cannot read a
 * string resource — and then reports the new id so the list can open it straight
 * into the editor, which is where its exercises are added.
 */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val error = MutableStateFlow<DataError?>(null)

    /** The list's own filter (ROADMAP N214). The form's memory, not the document's. */
    private val showHidden = MutableStateFlow(false)

    /** Emits the id of a just-created template, so the screen can open it. */
    private val _created = MutableStateFlow<String?>(null)
    val createdTemplateId: StateFlow<String?> = _created

    val uiState: StateFlow<TemplatesUiState> = combine(
        // The one list that shows the hidden ones: its filter decides what is drawn, so the read
        // carries them all and the state below splits them (ROADMAP N214).
        repository.observeTemplates(includeHidden = true),
        workoutRepository.observeActiveSession(),
        error,
        showHidden,
    ) { templates, session, currentError, showing ->
        TemplatesUiState(
            isLoading = false,
            templates = if (showing) templates else templates.filter { it.hiddenAt == null },
            hiddenCount = templates.count { it.hiddenAt != null },
            showHidden = showing,
            hasActiveWorkout = session != null,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TemplatesUiState(),
    )

    /** Shows or hides the hidden rows, which is the filter and nothing else (ROADMAP N214). */
    fun onShowHiddenChange(show: Boolean) {
        showHidden.value = show
    }

    /**
     * Puts a workout back in the choices from the list itself, without opening its editor (N214).
     *
     * A write like any other, so a failure is said rather than swallowed; on success the row simply
     * stops being hidden, and the filter's count drops.
     */
    fun onUnhide(templateId: String) {
        viewModelScope.launch {
            when (val result = repository.setTemplateHidden(templateId, hidden = false)) {
                is DataResult.Success -> error.value = null
                is DataResult.Failure -> error.value = result.error
            }
        }
    }

    fun onCreateTemplate(name: String) {
        viewModelScope.launch {
            when (val result = repository.createTemplate(name)) {
                is DataResult.Success -> {
                    error.value = null
                    _created.value = result.data
                }

                is DataResult.Failure -> error.value = result.error
            }
        }
    }

    /** Called once the screen has navigated, so a recomposition cannot re-open it. */
    fun onCreatedHandled() {
        _created.value = null
    }

    fun onErrorShown() {
        error.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
