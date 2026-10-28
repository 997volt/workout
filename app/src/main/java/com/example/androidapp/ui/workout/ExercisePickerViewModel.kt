package com.example.androidapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.navigation.ExercisePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the picker's list shows (ROADMAP N95, N110).
 *
 * The **whole** library rather than pre-grouped rows, and that is the library's own division of labour: which
 * families are open is screen state (`rememberRowFold`, N84's rule), so a grouping done here would have to be
 * told about it on every fold and would then lag the screen by a frame. The screen holds the folds and calls
 * the pure [libraryRows] with them.
 */
data class ExercisePickerUiState(
    val query: String = "",
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val error: DataError? = null,
    /**
     * No movement exists at all, as opposed to none matching the query (ROADMAP P1.1a).
     *
     * The picker chooses movements, so a library holding nothing a lifter can log — including one that holds
     * categories but no movements — is empty here, and says so rather than blaming the search.
     */
    val libraryIsEmpty: Boolean = false,
) {
    /**
     * Nothing in the list can be chosen — whether because the library holds no movements or because the
     * search reached none.
     *
     * **A head is not an answer here**, which is the difference from the library's own emptiness (N95): a
     * picker's job is to choose a movement, so a list of families with nothing filed under them is an empty
     * picker, while the library draws those same families as rows. **Asked without the folds**, because the
     * answer must not depend on which families happen to be open — a family with movements inside is not
     * empty either way, and the two kinds of empty are [libraryIsEmpty]'s to tell apart.
     */
    val isEmpty: Boolean
        get() = !isLoading && libraryRows(exercises, query).none { !it.isCategory }
}

/**
 * Drives the exercise picker shown over an active workout.
 *
 * It resolves the session itself from the repository rather than receiving it
 * through navigation arguments. The open session is already the single source of
 * truth for "which workout am I in", so passing an id through the back stack
 * would just create a second, staleable copy of that fact.
 *
 * It also owns creating a custom exercise here, mid-workout, because that is
 * where the gap is felt (ROADMAP N2): the new exercise is stored and appended to
 * the session in one step, so the user never leaves the picker.
 *
 * The same screen also fills a template (ROADMAP N3). Rather than a second picker,
 * the route carries an optional `templateId` and only the *destination* of the
 * chosen exercise changes — the search, the list and the create dialog are shared,
 * which is why the two paths cannot drift apart.
 */
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Null when picking for the open session, set when picking for a template. */
    private val templateId: String? = savedStateHandle.toRoute<ExercisePicker>().templateId

    private val query = MutableStateFlow("")

    /** Emits true once the exercise is stored, so the screen can pop itself. */
    private val _added = MutableStateFlow(false)
    val added: StateFlow<Boolean> = _added

    /**
     * Set when a write failed, so the picker can say so instead of dropping it.
     *
     * Both halves of the create path can fail — the library write and the append
     * to the session — and the dialog stays open on the first so the typed name
     * is not lost.
     */
    private val _error = MutableStateFlow<DataError?>(null)
    val error: StateFlow<DataError?> = _error

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        exerciseRepository.observeExercises(),
        query,
    ) { result, currentQuery ->
        // The picker shows a read failure the same way the library does (B4).
        val exercises = result.getOrNull().orEmpty()
        ExercisePickerUiState(
            query = currentQuery,
            // The whole library, grouped by the screen (ROADMAP N95, N110): the picker draws the same shape
            // the library does — a category head with the movements filed under it — so a lifter browsing for
            // what they just did sees the families they filed. What a tap may **choose** is still movements
            // alone, which the list enforces by routing a head's tap to the fold rather than to the choice.
            exercises = exercises,
            isLoading = false,
            error = (result as? DataResult.Failure)?.error,
            libraryIsEmpty = exercises.none { it.rowKind.isLoggable },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ExercisePickerUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onExerciseSelected(exerciseId: String) {
        launchAdd(exerciseId)
    }

    /**
     * Saves a custom exercise named [name] and immediately adds it to the target
     * (ROADMAP N2).
     *
     * The order matters: the exercise exists in the library before anything
     * references it, so a failure of the second step still leaves a usable
     * library entry rather than a dangling reference.
     */
    fun onCreateExercise(name: String) {
        viewModelScope.launch {
            when (val created = exerciseRepository.createCustomExercise(name)) {
                is DataResult.Success -> launchAdd(created.data.id)
                is DataResult.Failure -> _error.value = created.error
            }
        }
    }

    /** Clears a shown failure, so a dismissal does not linger on the next open. */
    fun onErrorShown() {
        _error.value = null
    }

    /**
     * What one add did, as the three outcomes the picker has (N168).
     *
     * A plan add is two writes — the exercise, then the targets it is prefilled with — and the exercise
     * is the action the lifter took. A failure in the second half must leave the append standing, so it
     * is its own outcome rather than a failed add; it is still *reported*, because a storage error that
     * vanishes leaves a plan the lifter has to top up without knowing why.
     */
    private sealed interface AddOutcome {
        /** The exercise landed, targets and all. */
        data object Added : AddOutcome

        /** The exercise landed; filling its targets failed. */
        data class AddedWithoutTargets(val error: DataError) : AddOutcome

        /** The exercise did not land, so a retry is safe. */
        data class NotAdded(val error: DataError) : AddOutcome
    }

    /** The exercise a failed prefill already appended, so a second tap cannot add it twice (N168). */
    private var appendedExerciseId: String? = null

    /** The exercise a tap is already adding, so a second tap inside the write is not a second row (B273). */
    private var adding: String? = null

    /**
     * Starts one add, and refuses a second for the same movement while the first is in flight.
     *
     * The flag is taken **here**, before the coroutine is queued: two taps land in one main-thread turn,
     * so a check inside the write would see both of them before either had begun (B273).
     */
    private fun launchAdd(exerciseId: String) {
        if (adding == exerciseId) return
        adding = exerciseId
        viewModelScope.launch {
            try {
                add(exerciseId)
            } finally {
                adding = null
            }
        }
    }

    private suspend fun add(exerciseId: String) {
        // The append already happened when its targets failed; a second tap is not a retry of a failed
        // write, so it is answered as the add it already is rather than duplicating the plan row.
        if (appendedExerciseId == exerciseId) {
            _error.value = null
            _added.value = true
            return
        }
        addOnce(exerciseId)
    }

    private suspend fun addOnce(exerciseId: String) {
        val id = templateId
        val outcome = if (id != null) addToTemplate(id, exerciseId) else addToSession(exerciseId)
        when (outcome) {
            AddOutcome.Added -> {
                _error.value = null
                _added.value = true
            }

            is AddOutcome.AddedWithoutTargets -> {
                appendedExerciseId = exerciseId
                _error.value = outcome.error
            }

            is AddOutcome.NotAdded -> _error.value = outcome.error
        }
    }

    /**
     * Appends the exercise to a plan and fills its sets from the last time it was trained (P3.18).
     *
     * Only the work is carried: a plan's sets are what the session asks for, and a warm-up is the lifter's
     * ramp rather than a target. The append is the action that matters — once it has happened, a failure
     * to read history or write a set leaves a valid plan with no targets, not a missing exercise — but it
     * is reported as [AddOutcome.AddedWithoutTargets] rather than dropped (N168).
     */
    private suspend fun addToTemplate(templateId: String, exerciseId: String): AddOutcome =
        when (val appended = templateRepository.addExercise(templateId, exerciseId)) {
            is DataResult.Failure -> AddOutcome.NotAdded(appended.error)
            is DataResult.Success -> fillTargets(templateId, exerciseId)
        }

    /**
     * The append has happened; this writes the targets it is prefilled with (P3.18).
     *
     * A failure to read the history or to find the row it just made leaves the plan valid with no targets —
     * a missing exercise is the one outcome the append must not have — and the read failure is reported.
     */
    private suspend fun fillTargets(templateId: String, exerciseId: String): AddOutcome {
        val history = workoutRepository.previousPerformance(exerciseId, "")
        // The append has just happened, so this is the row it made. A read that finds nothing leaves the
        // plan valid with no targets; there is no failure to report.
        val planned = templateRepository.observeExercises(templateId).first()
            .lastOrNull { it.exerciseId == exerciseId }
        return when (history) {
            is DataResult.Failure -> AddOutcome.AddedWithoutTargets(history.error)
            is DataResult.Success ->
                if (planned == null) AddOutcome.Added else writeTargets(planned.id, history.data.sets)
        }
    }

    /** Writes one target per working set, reporting the first failure rather than the last (N168). */
    private suspend fun writeTargets(plannedId: String, sets: List<SetEntry>): AddOutcome {
        val failures = sets
            .filter { it.setType != SetType.WARMUP }
            .map { set ->
                templateRepository.addSet(
                    plannedId,
                    TemplateSetEdit(
                        role = SetType.NORMAL,
                        targetWeightGrams = set.weightGrams,
                        targetAssistanceGrams = set.assistanceGrams.takeIf { it > 0L },
                        targetRepsMin = set.reps,
                        targetRepsMax = set.reps,
                    ),
                )
            }
            .filterIsInstance<DataResult.Failure>()
        return failures.firstOrNull()?.let { AddOutcome.AddedWithoutTargets(it.error) }
            ?: AddOutcome.Added
    }

    private suspend fun addToSession(exerciseId: String): AddOutcome {
        // No active session means the picker was reached without one; reporting it
        // is better than writing a session exercise that belongs to nothing.
        val sessionId = workoutRepository.observeActiveSession().first()?.id
            ?: return AddOutcome.NotAdded(DataError.NotFound)
        return when (val appended = workoutRepository.addExercise(sessionId, exerciseId)) {
            is DataResult.Success -> AddOutcome.Added
            is DataResult.Failure -> AddOutcome.NotAdded(appended.error)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
