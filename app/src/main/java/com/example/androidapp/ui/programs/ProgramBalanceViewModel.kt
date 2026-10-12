package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.muscleBalance
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.ui.navigation.ProgramBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** What one program's balance view shows (ROADMAP N119). */
data class ProgramBalanceUiState(
    val isLoading: Boolean = true,
    val programName: String? = null,
    /** The plan's work per muscle, largest first, with the unresolved bucket last (see `muscleBalance`). */
    val balance: List<MuscleSets> = emptyList(),
    val totalSets: Int = 0,
)

/**
 * A program's planned work per muscle (ROADMAP N119).
 *
 * **The read is new**: the editor loads one slot's template at a time for its preview (N72), so this is the
 * first thing to read every template the program's slots name — and it reads the library too, because a
 * movement's muscle is own-if-stated-else-its-category's (N110) and only the library knows the family.
 *
 * The counting itself is [muscleBalance], a pure function, so what is left here is the wiring: three reads,
 * each of which answers something the others cannot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgramBalanceViewModel @Inject constructor(
    programRepository: ProgramRepository,
    templateRepository: TemplateRepository,
    exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramBalance>().programId

    /**
     * Every planned row the program's slots name, flattened.
     *
     * A slot points at a template, and a template's planned sets are what the balance counts, so this is a
     * read per slot rather than one query — the shape the editor's preview already has, widened from one slot
     * to all of them. `flatMapLatest` because editing the schedule must replace the read rather than add to it.
     */
    private val planned: Flow<List<TemplateExercise>> =
        programRepository.observeSlots(programId).flatMapLatest { slots ->
            val perTemplate = slots.map { templateRepository.observeExercises(it.templateId) }
            if (perTemplate.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(perTemplate) { groups -> groups.toList().flatten() }
            }
        }

    val uiState: StateFlow<ProgramBalanceUiState> = combine(
        programRepository.observeProgram(programId),
        planned,
        exerciseRepository.observeExercises(),
    ) { program, plannedRows, library ->
        val exercises = (library as? DataResult.Success)?.data.orEmpty()
        val balance = muscleBalance(plannedRows, exercises)
        ProgramBalanceUiState(
            isLoading = false,
            programName = program?.name,
            balance = balance,
            totalSets = balance.sumOf { it.sets },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProgramBalanceUiState(),
    )

    private companion object {
        /** The editor's own timeout: the screen is a read, so an idle one should not hold a query open. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
