package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneSource
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ExerciseWorkingSets
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.ScreenDay
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TrainedWindow
import com.example.androidapp.domain.model.muscleBalance
import com.example.androidapp.domain.model.plannedWorkingSets
import com.example.androidapp.domain.model.trainedMuscleBalance
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.StatisticsRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.ui.navigation.ProgramBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Which answer the balance screen is showing: the plan, or what the log actually trained (ROADMAP N289). */
enum class BalanceView {
    PLAN,
    TRAINED,
}

/** What one program's balance view shows (ROADMAP N119, N289). */
data class ProgramBalanceUiState(
    val isLoading: Boolean = true,
    val programName: String? = null,
    /** The plan's work per muscle, largest first, with the unresolved bucket last (see `muscleBalance`). */
    val balance: List<MuscleSets> = emptyList(),
    /**
     * The plan's own working-set count, warm-ups excluded (ROADMAP N112, N122).
     *
     * The header's number, and deliberately not the sum of the rows: a set is worth a full one to its primary
     * and half to each secondary, so the credits no longer add up to the plan's size.
     */
    val plannedSets: Int = 0,
    /** Which side of the toggle is showing; the plan is what a program editor opens onto. */
    val view: BalanceView = BalanceView.PLAN,
    /** How far back the trained side reads; seven days is the week a lifter checks most often. */
    val window: TrainedWindow = TrainedWindow.LAST_7_DAYS,
    /**
     * What the log actually trained in [window], per muscle, as counts (ROADMAP N289).
     *
     * **Counts only**: no target, no minimum and no warning. The same credits as [balance], read over the
     * sets that were performed; the plan's bars have no counterpart here because a share of the log is not a
     * verdict on it either.
     */
    val trained: List<MuscleSets> = emptyList(),
    /** How many working sets the log holds in [window], warm-ups excluded — the trained header's number. */
    val trainedSets: Int = 0,
    /**
     * Whether the log read failed.
     *
     * A failure is not "nothing was trained": the screen says the read failed rather than drawing a zero,
     * because an empty window is a claim about the training and this is a fact about the database (B4).
     */
    val trainedReadFailed: Boolean = false,
)

/**
 * A program's planned work per muscle, and what the log actually trained (ROADMAP N119, N289).
 *
 * **The plan's read is new**: the editor loads one slot's template at a time for its preview (N72), so this
 * is the first thing to read every template the program's slots name — and it reads the library too, because
 * a movement's muscle is own-if-stated-else-its-category's (N110) and only the library knows the family.
 *
 * **The trained read is the same question over history**: [StatisticsRepository.observeWorkingSetsIn] returns
 * one working-set count per movement for the window, and [trainedMuscleBalance] credits it with the plan's own
 * rule. It is a factual read — the screening, the minimum and the warning are the parked P2.8, and nothing
 * here names one.
 *
 * The counting itself is pure, so what is left here is the wiring: the reads, the two toggles, and the
 * screen-local day whose midnight ends the window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
@HiltViewModel
class ProgramBalanceViewModel @Inject constructor(
    programRepository: ProgramRepository,
    templateRepository: TemplateRepository,
    exerciseRepository: ExerciseRepository,
    private val statisticsRepository: StatisticsRepository,
    private val timeSource: TimeSource,
    private val zoneSource: ZoneSource,
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

    /** The device's own day, re-read on resume so a screen open across midnight shows the new window (N286). */
    private val screenDay = MutableStateFlow(
        ScreenDay(
            date = timeSource.now().atZone(zoneSource.zone()).toLocalDate(),
            zone = zoneSource.zone(),
        ),
    )

    private val view = MutableStateFlow(BalanceView.PLAN)
    private val window = MutableStateFlow(TrainedWindow.LAST_7_DAYS)

    /**
     * The log's counts for the selected window, one row per movement.
     *
     * The database does the grouping, so this never holds a set; the window's midnight is resolved from the
     * screen's own day, which is why the day is part of the read rather than a constant inside it.
     */
    private val trainedCounts: Flow<DataResult<List<ExerciseWorkingSets>>> =
        combine(view, window, screenDay) { selectedView, selectedWindow, screen ->
            Triple(selectedView, selectedWindow, screen)
        }.flatMapLatest { (selectedView, selectedWindow, screen) ->
            // B394: opening the Plan side is not a reason to query the log.
            if (selectedView != BalanceView.TRAINED) {
                flowOf(DataResult.Success(emptyList()))
            } else {
                val bounds = selectedWindow.window(screen.date, screen.zone)
                statisticsRepository.observeWorkingSetsIn(bounds.from, bounds.toExclusive)
            }
        }

    /**
     * The three plan reads and the log read, gathered so the final combine stays inside its typed arity.
     *
     * A failed library read is the empty library, as the plan side has always treated it; the trained read is
     * kept as the [DataResult] it arrived as, so a failure is not counted as an empty window.
     */
    private data class Sources(
        val programName: String?,
        val planned: List<TemplateExercise>,
        val library: List<Exercise>,
        val trained: DataResult<List<ExerciseWorkingSets>>,
    )

    private val sources: Flow<Sources> = combine(
        programRepository.observeProgram(programId),
        planned,
        exerciseRepository.observeExercises(),
        trainedCounts,
    ) { program, plannedRows, libraryResult, trainedResult ->
        Sources(
            programName = program?.name,
            planned = plannedRows,
            library = (libraryResult as? DataResult.Success)?.data.orEmpty(),
            trained = trainedResult,
        )
    }

    val uiState: StateFlow<ProgramBalanceUiState> =
        combine(sources, view, window) { current, selectedView, selectedWindow ->
            val trained = (current.trained as? DataResult.Success)?.data.orEmpty()
            ProgramBalanceUiState(
                isLoading = false,
                programName = current.programName,
                balance = muscleBalance(current.planned, current.library),
                plannedSets = plannedWorkingSets(current.planned),
                view = selectedView,
                window = selectedWindow,
                trained = trainedMuscleBalance(trained, current.library),
                trainedSets = trained.sumOf { it.workingSets },
                trainedReadFailed = current.trained is DataResult.Failure,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProgramBalanceUiState(),
        )

    /** Shows the plan or the log's counts; the screen never leaves this destination to switch. */
    fun onSelectView(selected: BalanceView) {
        view.value = selected
    }

    /** Widens or narrows the trained window; the read follows the selection. */
    fun onSelectWindow(selected: TrainedWindow) {
        window.value = selected
    }

    /** Re-reads the device day after a resume; a no-op until the date actually changes. */
    fun onResume() {
        screenDay.update {
            val zone = zoneSource.zone()
            ScreenDay(timeSource.now().atZone(zone).toLocalDate(), zone)
        }
    }

    private companion object {
        /** The editor's own timeout: the screen is a read, so an idle one should not hold a query open. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
