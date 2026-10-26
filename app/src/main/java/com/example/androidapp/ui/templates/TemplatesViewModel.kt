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

/**
 * Which group of the library the list draws (ROADMAP N217).
 *
 * A **mode**, not a reveal: picking one draws that group *instead of* the live workouts, which is what
 * a chip carrying a count promises, and the same rule serves N216's deleted group beside it.
 */
enum class TemplatesFilter { ALL, HIDDEN, DELETED }

data class TemplatesUiState(
    val isLoading: Boolean = true,
    /**
     * The rows the list draws: the live workouts, or the one group [filter] names (ROADMAP N217).
     *
     * A hidden workout is drawn under [TemplatesFilter.HIDDEN] and a deleted one under
     * [TemplatesFilter.DELETED], so the whole of "not offered" lives in this one split of one list.
     */
    val templates: List<WorkoutTemplate> = emptyList(),
    /** How many live workouts are hidden, so the filter can say the number and hide itself when it is 0. */
    val hiddenCount: Int = 0,
    /** How many workouts are deleted, on [hiddenCount]'s rule (ROADMAP N216). */
    val deletedCount: Int = 0,
    /** The group being drawn; [TemplatesFilter.ALL] until the lifter asks for another (ROADMAP N217). */
    val filter: TemplatesFilter = TemplatesFilter.ALL,
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

    /** The list's own filter (ROADMAP N217). The form's memory, not the document's. */
    private val filter = MutableStateFlow(TemplatesFilter.ALL)

    /** Emits the id of a just-created template, so the screen can open it. */
    private val _created = MutableStateFlow<String?>(null)
    val createdTemplateId: StateFlow<String?> = _created

    val uiState: StateFlow<TemplatesUiState> = combine(
        // The one list that shows the put-away rows: its mode decides what is drawn, so the reads
        // carry everything and the state below splits it (ROADMAP N217, N216).
        repository.observeTemplates(includeHidden = true),
        repository.observeDeletedTemplates(),
        workoutRepository.observeActiveSession(),
        error,
        filter,
    ) { templates, deleted, session, currentError, selected ->
        val hidden = templates.filter { it.hiddenAt != null }
        val live = templates.filter { it.hiddenAt == null }
        // A mode holds only while its group does (ROADMAP N217): the last unhide or restore empties it,
        // and the list falls back to All rather than drawing a blank list under a selected chip. Written
        // back here, guarded, so the chip and the rows it stands for cannot disagree for a frame.
        val mode = when {
            selected == TemplatesFilter.HIDDEN && hidden.isEmpty() -> TemplatesFilter.ALL
            selected == TemplatesFilter.DELETED && deleted.isEmpty() -> TemplatesFilter.ALL
            else -> selected
        }
        if (mode != selected) filter.value = mode
        TemplatesUiState(
            isLoading = false,
            templates = when (mode) {
                TemplatesFilter.ALL -> live
                TemplatesFilter.HIDDEN -> hidden
                TemplatesFilter.DELETED -> deleted
            },
            hiddenCount = hidden.size,
            deletedCount = deleted.size,
            filter = mode,
            hasActiveWorkout = session != null,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TemplatesUiState(),
    )

    /** Draws one group of the library (ROADMAP N217). */
    fun onFilterChange(next: TemplatesFilter) {
        filter.value = next
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

    /**
     * Brings a deleted workout back, with its plan (ROADMAP N216).
     *
     * A write like any other, so a failure is said rather than swallowed. On success the row leaves the
     * deleted group — falling the list back to All when it was the last one — and the workout is live
     * again, which is what puts a program day that still names it back on its weekday.
     */
    fun onRestore(templateId: String) {
        viewModelScope.launch {
            when (val result = repository.restoreTemplate(templateId)) {
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
