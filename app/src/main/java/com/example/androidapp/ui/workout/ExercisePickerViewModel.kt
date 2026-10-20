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
        viewModelScope.launch { add(exerciseId) }
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
                is DataResult.Success -> add(created.data.id)
                is DataResult.Failure -> _error.value = created.error
            }
        }
    }

    /** Clears a shown failure, so a dismissal does not linger on the next open. */
    fun onErrorShown() {
        _error.value = null
    }

    private suspend fun add(exerciseId: String) {
        val id = templateId
        val target = if (id != null) addToTemplate(id, exerciseId) else addToSession(exerciseId)
        when (target) {
            is DataResult.Success -> {
                _error.value = null
                _added.value = true
            }

            is DataResult.Failure -> _error.value = target.error
        }
    }

    /**
     * Appends the exercise to a plan and fills its sets from the last time it was trained (P3.18).
     *
     * Only the work is carried: a plan's sets are what the session asks for, and a warm-up is the lifter's
     * ramp rather than a target. The append is the action that matters — once it has happened, a failure to
     * read history or write a set leaves a valid plan with no targets, not a missing exercise, so it is not
     * reported as a failed add.
     */
    private suspend fun addToTemplate(templateId: String, exerciseId: String): DataResult<Unit> {
        val appended = templateRepository.addExercise(templateId, exerciseId)
        if (appended is DataResult.Failure) { return appended }

        // P3.18: the last time the lift was trained becomes the plan's first targets. The append has
        // already happened, so a failed read or write here leaves a valid plan with no targets rather
        // than a missing exercise — which is why it is not reported as a failed add.
        val history = workoutRepository.previousPerformance(exerciseId, "")
        val working = if (history is DataResult.Success) {
            history.data.sets.filter { it.setType != SetType.WARMUP }
        } else {
            emptyList()
        }
        val planned = templateRepository.observeExercises(templateId).first()
            .lastOrNull { it.exerciseId == exerciseId }
        if (planned != null) {
            working.forEach { set ->
                templateRepository.addSet(
                    planned.id,
                    TemplateSetEdit(
                        role = SetType.NORMAL,
                        targetWeightGrams = set.weightGrams,
                        targetAssistanceGrams = set.assistanceGrams.takeIf { it > 0L },
                        targetRepsMin = set.reps,
                        targetRepsMax = set.reps,
                    ),
                )
            }
        }
        return DataResult.Success(Unit)
    }

    private suspend fun addToSession(exerciseId: String): DataResult<Unit> {
        // No active session means the picker was reached without one; reporting it
        // is better than writing a session exercise that belongs to nothing.
        val sessionId = workoutRepository.observeActiveSession().first()?.id
            ?: return DataResult.Failure(DataError.NotFound)
        return workoutRepository.addExercise(sessionId, exerciseId)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
