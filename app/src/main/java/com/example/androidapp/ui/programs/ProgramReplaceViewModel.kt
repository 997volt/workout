package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.asThrowable
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ReplaceOutcome
import com.example.androidapp.domain.model.ReplacementSeed
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.keepWorkingSets
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.navigation.ProgramReplace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the replace sheet needs to say before the lifter confirms (ROADMAP N308). */
data class ProgramReplaceUiState(
    val isLoading: Boolean = true,
    val oldName: String = "",
    /** The movements filed under the old movement's category, minus the old one and the plan's own. */
    val suggestions: List<Exercise> = emptyList(),
    val selected: Exercise? = null,
    /** The deliberate other choice: the plan's reps stay, the loads are cleared (N309's marker). */
    val empty: Boolean = false,
    val setCountBefore: Int = 0,
    val setCountAfter: Int = 0,
    /** The selected movement's last work, by place; null entries are places it has no counterpart for. */
    val seeds: List<ReplacementSeed?> = emptyList(),
    val sharedProgramNames: List<String> = emptyList(),
    val error: DataError? = null,
    /** Set once the write succeeded; the route hands it to the plan screen's undo and leaves. */
    val replaced: ReplaceOutcome? = null,
)

/**
 * Replacing one plan's movement with another (ROADMAP N308).
 *
 * The plan edit and its trim are the data layer's; this reads the two things the lifter decides with:
 * the category suggestions (N95/N110) and the seed from the new movement's last finished session. The
 * picker's browse answer arrives through the navigation result, so a movement outside the category is one
 * tap away without a second editing surface.
 */
@HiltViewModel
class ProgramReplaceViewModel @Inject constructor(
    private val templates: TemplateRepository,
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val programs: ProgramRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ProgramReplace>()

    private val _state = MutableStateFlow(ProgramReplaceUiState())
    val state: StateFlow<ProgramReplaceUiState> = _state.asStateFlow()

    private var library: List<Exercise> = emptyList()

    /** The browse picker's answer, written by the nav into this entry's saved state (N308). */
    private val pickedId = savedStateHandle.getStateFlow<String?>("replaceExerciseId", null)

    init {
        load()
        viewModelScope.launch {
            pickedId.collect { id -> if (id != null) select(id) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val read = dataResultOf {
                val plan = templates.observeExercises(route.templateId).first()
                val old = plan.firstOrNull { it.id == route.templateExerciseId }
                    ?: throw NoSuchElementException("the plan's exercise is gone")
                val sets = templates.observeSets(route.templateId).first()
                    .filter { it.templateExerciseId == old.id }
                val work = sets.count { it.role.recordsEffort }
                val libraryRead = exercises.observeExercises().first()
                val all = when (libraryRead) {
                    is DataResult.Success -> libraryRead.data
                    is DataResult.Failure -> throw libraryRead.error.asThrowable()
                }
                library = all
                val planIds = plan.map { it.exerciseId }.toSet()
                val category = all.firstOrNull { it.id == old.exerciseId }?.parentId
                val suggestions = if (category == null) {
                    // An unfiled movement has no family to suggest from: browse or Empty only.
                    emptyList()
                } else {
                    all.filter {
                        it.rowKind == RowKind.MOVEMENT &&
                            it.parentId == category &&
                            it.id != old.exerciseId &&
                            it.id !in planIds
                    }.sortedBy { it.name }
                }
                val shared = when (
                    val names = programs.otherProgramNamesUsingTemplates(
                        listOf(route.templateId),
                        route.programId,
                    )
                ) {
                    is DataResult.Success -> names.data
                    is DataResult.Failure -> {
                        _state.update { it.copy(error = names.error) }
                        emptyList()
                    }
                }
                ProgramReplaceUiState(
                    isLoading = false,
                    oldName = old.exerciseName,
                    suggestions = suggestions,
                    setCountBefore = work,
                    setCountAfter = keepWorkingSets(work),
                    sharedProgramNames = shared,
                )
            }
            when (read) {
                is DataResult.Success -> _state.value = read.data
                is DataResult.Failure -> _state.update { it.copy(isLoading = false, error = read.error) }
            }
        }
    }

    /** A suggestion or a browse answer; reads the new movement's last work for the seed. */
    fun select(exerciseId: String) {
        val selected = library.firstOrNull { it.id == exerciseId } ?: return
        viewModelScope.launch {
            val read = dataResultOf {
                val performance = when (val previous = workouts.previousPerformance(exerciseId, null)) {
                    is DataResult.Success -> previous.data
                    is DataResult.Failure -> throw previous.error.asThrowable()
                }
                performance.sets
                    .filter { it.setType.recordsEffort }
                    .take(_state.value.setCountAfter)
                    .map { ReplacementSeed(it.weightGrams, it.assistanceGrams, it.reps, it.durationSeconds) }
            }
            when (read) {
                is DataResult.Success -> _state.update {
                    it.copy(selected = selected, seeds = read.data, empty = false)
                }
                is DataResult.Failure -> _state.update {
                    it.copy(selected = selected, seeds = emptyList(), empty = false, error = read.error)
                }
            }
        }
    }

    /** Empty is a target choice, not a movement: the selected row still names the new lift. */
    fun setEmpty(empty: Boolean) {
        _state.update { it.copy(empty = empty) }
    }

    fun confirm() {
        val current = _state.value
        val selected = current.selected ?: return
        if (current.replaced != null) return
        viewModelScope.launch {
            when (
                val read = templates.replaceExercise(
                    templateExerciseId = route.templateExerciseId,
                    newExerciseId = selected.id,
                    seeds = if (current.empty) emptyList() else current.seeds,
                    empty = current.empty,
                )
            ) {
                is DataResult.Success -> _state.update { it.copy(replaced = read.data) }
                is DataResult.Failure -> _state.update { it.copy(error = read.error) }
            }
        }
    }

    fun onErrorShown() {
        _state.update { it.copy(error = null) }
    }
}
