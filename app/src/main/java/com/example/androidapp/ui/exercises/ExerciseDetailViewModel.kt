package com.example.androidapp.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.ui.navigation.ExerciseDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the detail screen's edit form produces (ROADMAP N2, N5).
 *
 * Deliberately a screen-level draft rather than an [Exercise]: the form edits the
 * attributes a user can change, and secondary muscles or `isCustom` are not among
 * them, so passing a whole exercise around would invite editing the rest by
 * accident.
 */
data class ExerciseEdit(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment,
    val movementPattern: MovementPattern,
    /** The exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /** A cue to read while lifting, or null (ROADMAP N5). */
    val techniqueNote: String? = null,
    /** This exercise's own display unit, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    /** This exercise's own weight step in grams, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
    /**
     * The row this one is filed under, or null for the top level (ROADMAP N95).
     *
     * Part of the edit rather than a write of its own: *move to category* and *new variation of this* are
     * both this same form saving one more field, which is what keeps the two from being separate flows that
     * have to agree about validation.
     */
    val parentId: String? = null,
)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val error: DataError? = null,
    /**
     * The heads this row can be filed under (ROADMAP N95), live from the library.
     *
     * Offered for a movement, which can be filed under a category or a variation of another exercise; empty
     * for a category, which sits at the top level because the shape is two rules deep and no deeper.
     */
    val categoryOptions: List<Exercise> = emptyList(),
) {
    /**
     * Loaded, but no such exercise — a real state, not an error to hide.
     *
     * Excludes a failed read (ROADMAP B4): that is a different sentence on screen,
     * and telling the user the exercise does not exist when the database is the
     * thing that failed would be a lie.
     */
    val notFound: Boolean get() = !isLoading && exercise == null && error == null

    /**
     * Any exercise can be corrected here (ROADMAP N5). Seeded rows included, and
     * that is safe: the seeder tops up with `INSERT OR IGNORE` and never updates an
     * existing row, so an edit survives every future top-up.
     */
    /**
     * Whether *new variation of this* is offered (ROADMAP N95).
     *
     * A movement only, and only while not already editing: a variation hangs under an exercise, and one
     * under a category would be the third level the shape does not have.
     */
    val canCreateVariation: Boolean
        get() = !isLoading && !isEditing && exercise?.rowKind == RowKind.MOVEMENT

    /**
     * Whether the editor is offered (ROADMAP N2, widened by N5).
     *
     * Every row is editable, seeded ones included, because the seeder never updates a row that exists.
     * N95 leans on that rather than changing it: a seeded **category** has to be renamable and its children
     * re-filable, since the seeded families are a starting set rather than a closed one — a taxonomy
     * somebody maintains is the whole reason a category exists.
     */
    val canEdit: Boolean get() = !isLoading && !isEditing && exercise != null
}

/**
 * Loads one exercise for the detail screen, and saves edits to it.
 *
 * The id comes out of [SavedStateHandle] via the type-safe route, so the
 * ViewModel is recreated correctly after process death with the same argument.
 */
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.toRoute<ExerciseDetail>().exerciseId

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // A plain one-shot read, held in state so a successful edit is
            // reflected without a second round trip. Wrapped like the writes
            // (ROADMAP B4), so a failed read is a message on the screen rather
            // than an exception lost inside this coroutine.
            when (val result = repository.getExercise(exerciseId)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(exercise = result.data, isLoading = false, error = null)
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(exercise = null, isLoading = false, error = result.error)
                }
            }
            loadCategoryOptions()
        }
    }

    fun onEdit() {
        _uiState.update { it.copy(isEditing = true, error = null) }
    }

    fun onCancelEdit() {
        _uiState.update { it.copy(isEditing = false, error = null) }
    }

    /**
     * Writes the edited attributes, keeping the row's id, secondary muscles and
     * custom flag. Leaving edit mode is driven by the write, so a failure keeps
     * the form open with the user's input rather than pretending it saved.
     */
    /**
     * The heads this row may be filed under (ROADMAP N95).
     *
     * Read separately from the exercise itself and *not* wrapped in a failure branch: a list that could not
     * be read leaves the picker empty, which hides a control rather than breaking the page. The exercise's
     * own row is left out — filing something under itself is not a thing the shape allows.
     */
    private suspend fun loadCategoryOptions() {
        val result = repository.observeExercises().first()
        val exercises = (result as? DataResult.Success)?.data.orEmpty()
        _uiState.update { state ->
            state.copy(
                categoryOptions = exercises.filter {
                    it.rowKind == RowKind.CATEGORY && it.id != state.exercise?.id
                },
            )
        }
    }

    /**
     * Files a new variation of this exercise and opens it for naming (ROADMAP N95).
     *
     * A variation **inherits the exercise it hangs under** — its muscles, its equipment — so the copy below
     * is what "inherits" means in practice: only what is performed differently is the new row's own. It
     * arrives in the editor rather than on a finished screen, because its name is the one thing the lifter
     * must supply: "three-second paused" is a name they write, not a value from a closed set.
     */
    fun onCreateVariation() {
        val parent = _uiState.value.exercise ?: return
        viewModelScope.launch {
            when (val result = repository.createVariationOf(parent)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(exercise = result.data, isEditing = true, error = null)
                }

                is DataResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun onSave(edit: ExerciseEdit) {
        val current = _uiState.value.exercise ?: return
        viewModelScope.launch {
            val updated = current.copy(
                name = edit.name.trim(),
                primaryMuscle = edit.primaryMuscle,
                equipment = edit.equipment,
                movementPattern = edit.movementPattern,
                restSeconds = edit.restSeconds,
                techniqueNote = edit.techniqueNote,
                weightUnit = edit.weightUnit,
                stepGrams = edit.stepGrams,
                // The one field that makes this write *move to category* as well as an edit (N95).
                parentId = edit.parentId,
            )
            when (val result = repository.updateExercise(updated)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(exercise = updated, isEditing = false, error = null)
                }

                is DataResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }
}
