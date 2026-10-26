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
 * attributes a user can change, and secondary muscles are not among them, so
 * passing a whole exercise around would invite editing the rest by accident.
 */
data class ExerciseEdit(
    val name: String,
    /**
     * What the form states for this row's taxonomy, each null for *unstated* (ROADMAP N103).
     *
     * Nullable rather than defaulted to `OTHER`, because null is the row's real answer for a field it reads
     * from its category. **Every field is offered either way** (N110): a movement is allowed to disagree with
     * its family, so the form cannot withhold a picker and still let the lifter say so.
     */
    val primaryMuscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val movementPattern: MovementPattern? = null,
    /** The exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /** A cue to read while lifting, or null (ROADMAP N5). */
    val techniqueNote: String? = null,
    /** This exercise's own display unit, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    /** This exercise's own weight step in grams, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
    /** Whether the movement is loaded by the body (ROADMAP N219). */
    val isBodyweight: Boolean = false,
    /**
     * The category this row is filed under, or null for none (ROADMAP N95, N110).
     *
     * Part of the edit rather than a write of its own: *move to category* is this same form saving one more
     * field, which is what keeps it from being a second flow that has to agree about validation.
     */
    val parentId: String? = null,
)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val error: DataError? = null,
    /**
     * The categories this row can be filed under (ROADMAP N95, N110), live from the library.
     *
     * A **category**, and only a category: the shape is one rule deep, so a movement is nobody's head.
     * Empty for a category, which sits at the top level.
     */
    val categoryOptions: List<Exercise> = emptyList(),
    /**
     * What the category this row is filed under is called, or null when it is in none (ROADMAP N95).
     *
     * Read rather than copied, and **still answered by a category that has been removed** (N58's rule),
     * which is why the ViewModel reads the library including removed rows for this one question.
     */
    val headName: String? = null,
    /** This row's category, for the muscle and pattern it passes down (N95). Null when it is in none. */
    val head: Exercise? = null,
    /**
     * Every row above this one, nearest first, **removed rows included** (ROADMAP N103, N110).
     *
     * The chain rather than one resolved answer, because the screen asks several questions of it: what this
     * row's muscle and pattern resolve to, and which of those the category already supplies — so the picker
     * can show what *not stated* will mean. The walk goes to the top because a file is a place a longer chain
     * can arrive, even though the shape this app writes is one level (B92, B96).
     */
    val ancestors: List<Exercise> = emptyList(),
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
     * Leaves the editor without writing (ROADMAP N95).
     *
     * Nothing was stored, so cancelling leaves the library exactly as it was.
     */
    fun onCancelEdit() {
        _uiState.update { state ->
            state.copy(
                isEditing = false,
                error = null,
            )
        }
    }

    /**
     * The library context this row is read through (ROADMAP N95, B85, B96).
     *
     * Three answers from one pair of reads, because they are the same question asked three ways: the
     * categories the picker may offer, the one this row is already filed under, and the **family pattern**
     * the row resolves to. Read separately from the exercise itself and *not* wrapped in a failure branch: a
     * list that could not be read leaves the picker empty, which hides a control rather than breaking the
     * page. The exercise's own row is left out of the options — filing something under itself is not a thing
     * the shape allows — and **only categories are offered** (N110): a movement is nobody's head.
     */
    private suspend fun loadLibraryContext() {
        // Two reads, because they answer two different questions. The **live** library is what the picker may
        // offer: filing a row under a category that was removed would be filing it nowhere, and N95's rule is
        // that a removed category keeps naming what is already under it rather than taking anything new. The
        // read *including* removed rows is what names the category this row is already filed under, because a
        // removed category still names its children (N58's rule) — and it is the library the resolver walks,
        // so a movement under a removed category still reads that category's pattern and muscle.
        val live = (repository.observeExercises().first() as? DataResult.Success)?.data.orEmpty()
        val everything = (repository.getAllIncludingDeleted() as? DataResult.Success)?.data.orEmpty()
        _uiState.update { state ->
            val head = everything.firstOrNull { it.id == state.exercise?.parentId }
            state.copy(
                // Only a category is a place to be filed (N110): the shape is one rule deep, so nothing
                // else can be offered without offering a row the list would then refuse to draw.
                categoryOptions = live.filter { candidate ->
                    candidate.rowKind == RowKind.CATEGORY && candidate.id != state.exercise?.id
                },
                head = head,
                headName = head?.name,
                ancestors = ancestorsOf(state.exercise, everything),
            )
        }
    }

    /**
     * Every row above [exercise], nearest first, ending at the first cycle or missing link (ROADMAP N103).
     *
     * Read from the library including removed rows, because a removed head still supplies what it stated
     * (N58's rule) — and a cycle is not a shape this app writes, but a file is a place one arrives, so
     * `seen` ends the walk rather than hanging on it (B92).
     */
    private fun ancestorsOf(exercise: Exercise?, library: List<Exercise>): List<Exercise> {
        val byId = library.associateBy { it.id }
        val seen = mutableSetOf(exercise?.id)
        val chain = mutableListOf<Exercise>()
        var parent = exercise?.parentId?.let { byId[it] }
        while (parent != null && seen.add(parent.id)) {
            chain += parent
            parent = byId[parent.parentId]
        }
        return chain
    }

    /**
     * Writes the edited attributes, keeping the row's id, secondary muscles and custom flag.
     *
     * A failure keeps the form open with what was typed rather than pretending it saved. The library context
     * is re-read on success, so a *move to category* shows the new family and the muscle it now inherits
     * rather than the pre-move answers (B84).
     */
    fun onSave(edit: ExerciseEdit) {
        val state = _uiState.value
        val current = state.exercise ?: return
        viewModelScope.launch {
            val outcome: DataResult<Exercise> = write(current.withEdit(edit))
            when (outcome) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exercise = outcome.data,
                            isEditing = false,
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
        isBodyweight = edit.isBodyweight,
        // The one field that makes this write *move to category* as well as an edit (N95).
        parentId = edit.parentId,
    )
}
