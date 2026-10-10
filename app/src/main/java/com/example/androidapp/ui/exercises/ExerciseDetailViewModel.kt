package com.example.androidapp.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.effectiveMovementPattern
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
     * The heads this row can be filed under (ROADMAP N95, B85), live from the library.
     *
     * A head is a category or an exercise that is not itself a variation: a movement is filed under a
     * family, and a variation under the exercise it is performed as. Empty for a category, which sits at the
     * top level because the shape is two rules deep and no deeper.
     */
    val categoryOptions: List<Exercise> = emptyList(),
    /**
     * What this row's head is called, or null when it hangs under nothing (ROADMAP N95).
     *
     * Read rather than copied, and **still answered by a head that has been removed** (N58's rule), which is
     * why the ViewModel reads the library including removed rows for this one question.
     */
    val headName: String? = null,
    /** This row's head, for the muscle it passes down (N95). Null when it hangs under nothing. */
    val head: Exercise? = null,
    /**
     * This row's family movement pattern, or null when it has none (ROADMAP N96, B96).
     *
     * Resolved here rather than on the screen, because the walk needs the **whole chain** and the screen is
     * handed only the immediate [head]: the pattern belongs to the category, and for a variation the category
     * sits one row above the exercise the variation hangs under. Reading `listOfNotNull(head, exercise)` on
     * the screen stopped at the movement, so exactly the rows a family exists to group drew no pattern at all.
     */
    val familyPattern: MovementPattern? = null,
    /**
     * The exercise a variation being edited hangs under, while that variation is not stored yet (B83).
     *
     * The row is written on **Save**, not when the editor opens: creating it up front left a stray row named
     * after its parent when the lifter cancelled, and there is no delete for a library row to take it back.
     */
    val variationParent: Exercise? = null,
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
     * Whether *new variation of this* is offered (ROADMAP N95).
     *
     * A movement that is **not already a variation** (B82): a variation hangs under an exercise, and a
     * variation of a variation would be the third level the shape does not have. [head] is the row this one
     * hangs under, so a movement whose head is a movement is one.
     */
    val canCreateVariation: Boolean
        get() = !isLoading && !isEditing && exercise?.rowKind == RowKind.MOVEMENT &&
            head?.rowKind != RowKind.MOVEMENT

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
            loadLibraryContext()
        }
    }

    fun onEdit() {
        _uiState.update { it.copy(isEditing = true, error = null) }
    }

    /**
     * Leaves the editor without writing (ROADMAP N95, B83).
     *
     * A variation that was only being drafted goes back to the exercise it hangs under, and **nothing was
     * ever stored**, so cancelling leaves the library exactly as it was.
     */
    fun onCancelEdit() {
        _uiState.update { state ->
            state.copy(
                exercise = state.variationParent ?: state.exercise,
                isEditing = false,
                variationParent = null,
                error = null,
            )
        }
    }

    /**
     * The library context this row is read through (ROADMAP N95, B85, B96).
     *
     * Three answers from one pair of reads, because they are the same question asked three ways: the heads
     * the picker may offer, the head this row already hangs under, and the **family pattern** the chain
     * resolves to. Read separately from the exercise itself and *not* wrapped in a failure branch: a list that
     * could not be read leaves the picker empty, which hides a control rather than breaking the page. The
     * exercise's own row is left out of the options — filing something under itself is not a thing the shape
     * allows — and so is a movement that is already a variation, because a variation of a variation is the
     * third level there is no room for (B82).
     */
    private suspend fun loadLibraryContext() {
        // Two reads, because they answer two different questions. The **live** library is what the picker may
        // offer: filing a row under a head that was removed would be filing it nowhere, and N95's rule is
        // that a removed head keeps naming what is already under it rather than taking anything new. The
        // read *including* removed rows is what names the head this row already hangs under, because a
        // removed head still names its children (N58's rule) — and it is the library the pattern walk needs,
        // so a variation filed under a removed movement still reads its category's pattern.
        val live = (repository.observeExercises().first() as? DataResult.Success)?.data.orEmpty()
        val everything = (repository.getAllIncludingDeleted() as? DataResult.Success)?.data.orEmpty()
        val liveById = live.associateBy { it.id }
        _uiState.update { state ->
            val head = everything.firstOrNull { it.id == state.exercise?.parentId }
            state.copy(
                categoryOptions = live.filter { candidate ->
                    candidate.id != state.exercise?.id &&
                        (
                            candidate.rowKind == RowKind.CATEGORY ||
                                // A movement may hold a variation only while it is not one itself.
                                liveById[candidate.parentId]?.rowKind != RowKind.MOVEMENT
                            )
                },
                head = head,
                headName = head?.name,
                familyPattern = state.exercise?.effectiveMovementPattern(everything),
            )
        }
    }

    /**
     * Starts a variation of this exercise, in the editor (ROADMAP N95, B83).
     *
     * A variation **inherits the exercise it hangs under** — its muscles, its equipment, and the unit and
     * step that equipment's weights move by — so the draft below is what "inherits" means in practice: only
     * what is performed differently will be the new row's own.
     * It arrives in the editor rather than on a finished screen, because its name is the one thing the lifter
     * must supply: "three-second paused" is a name they write, not a value from a closed set.
     *
     * **Nothing is written yet** (B83): the row used to be inserted before the editor opened, so cancelling
     * left a stray row named after its parent — and a library row has no delete. The insert moved to [onSave],
     * which is the first moment the lifter has said they want it.
     */
    fun onCreateVariation() {
        val parent = _uiState.value.exercise ?: return
        _uiState.update {
            it.copy(
                exercise = parent.copy(
                    id = "",
                    name = "",
                    isCustom = true,
                    parentId = parent.id,
                    rowKind = RowKind.MOVEMENT,
                    restSeconds = null,
                    techniqueNote = null,
                ),
                variationParent = parent,
                isEditing = true,
                error = null,
            )
        }
    }

    /**
     * Writes the edited attributes, keeping the row's id, secondary muscles and custom flag.
     *
     * A variation being drafted is created **and then** filled in, in that order, because the create path is
     * what copies the inherited fields (B83). A failure at either write keeps the form open with what was
     * typed rather than pretending it saved. The heads are re-read on success, so a *move to category* shows
     * the new family and the muscle it now inherits rather than the pre-move answers (B84).
     */
    fun onSave(edit: ExerciseEdit) {
        val state = _uiState.value
        val current = state.exercise ?: return
        viewModelScope.launch {
            val outcome: DataResult<Exercise> = if (state.variationParent != null) {
                when (val created = repository.createVariationOf(state.variationParent)) {
                    is DataResult.Success -> write(created.data.withEdit(edit))
                    is DataResult.Failure -> created
                }
            } else {
                write(current.withEdit(edit))
            }
            when (outcome) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exercise = outcome.data,
                            isEditing = false,
                            variationParent = null,
                            error = null,
                        )
                    }
                    loadLibraryContext()
                }

                is DataResult.Failure -> _uiState.update { it.copy(error = outcome.error) }
            }
        }
    }

    /** Writes [row], returning it on success so the screen can hold what was stored. */
    private suspend fun write(row: Exercise): DataResult<Exercise> =
        when (val result = repository.updateExercise(row)) {
            is DataResult.Success -> DataResult.Success(row)
            is DataResult.Failure -> result
        }

    private fun Exercise.withEdit(edit: ExerciseEdit): Exercise = copy(
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
}
