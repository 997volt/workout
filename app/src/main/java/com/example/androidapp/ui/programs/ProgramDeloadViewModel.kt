package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.DELOAD_DEFAULT_LOAD_KEPT_PERCENT
import com.example.androidapp.domain.model.DELOAD_LOAD_KEPT_CHOICES
import com.example.androidapp.domain.model.DeloadSetTarget
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.deloadSetTargets
import com.example.androidapp.domain.repository.DeloadCreation
import com.example.androidapp.domain.repository.DeloadDayInput
import com.example.androidapp.domain.repository.DeloadExerciseInput
import com.example.androidapp.domain.repository.DeloadRepository
import com.example.androidapp.ui.navigation.ProgramDeload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One exercise the creator may keep, with the sets the rule derived for it (ROADMAP N147). */
data class DeloadExerciseUi(
    val templateExerciseId: String,
    val exerciseId: String,
    val name: String,
    val selected: Boolean,
    val sets: List<DeloadSetTarget>,
    /** The unit the load is shown in: the exercise's own, or the app's (N64). */
    val weightUnit: WeightUnit,
)

/** One day of the week being built, already reduced to what it keeps (ROADMAP N147). */
data class DeloadDayUi(
    /** The slot the day was seeded from; it addresses the day in a test (N147). */
    val sourceSlotId: String,
    val sourceName: String,
    val weekday: DayOfWeek?,
    val exercises: List<DeloadExerciseUi>,
)

data class DeloadCreatorUiState(
    val isLoading: Boolean = true,
    val programName: String? = null,
    /** The Monday of the week being built; the creator only ever writes the current one (P3.10). */
    val weekStart: LocalDate? = null,
    /** The days on screen, which is the first [dayCount] of the program's slots (N147). */
    val days: List<DeloadDayUi> = emptyList(),
    val dayCount: Int = 0,
    val maxDays: Int = 0,
    val loadKeptPercent: Int = DELOAD_DEFAULT_LOAD_KEPT_PERCENT,
    /** True when this week already has a plan, so the screen can say a rebuild replaces its days. */
    val hasExistingPlan: Boolean = false,
    val selectedExerciseCount: Int = 0,
    val creating: Boolean = false,
    /** The read failed, so the screen says so rather than claiming the program plans nothing (N147). */
    val loadFailed: Boolean = false,
    /** The week that was just written, or null. The screen says so and offers *Done*. */
    val created: DeloadCreation? = null,
    val error: DataError? = null,
) {
    val canCreate: Boolean
        get() = !isLoading && !loadFailed && !creating && created == null && dayCount > 0 &&
            selectedExerciseCount > 0

    /** True when the program plans nothing a deload could be made of (N147). */
    val hasNothingToBuild: Boolean get() = !isLoading && !loadFailed && maxDays == 0

    /** Whether the create bar belongs on screen: a loaded draft, not the success or a failure (B164). */
    val showsCreateBar: Boolean get() = !isLoading && !loadFailed && created == null && !hasNothingToBuild
}

/**
 * Builds a program's deload week (ROADMAP N147).
 *
 * The screen is a **draft**, read once: the program's slots, their planned sets, and each exercise's
 * last performance. Every number comes from `deloadSetTargets`, a pure rule the JVM tests cover, and
 * nothing is written until *Build* — so the preview and the saved plan are the same arithmetic.
 *
 * The week is the current one, captured with the day it was read on, because a deload is decided by how
 * the block is going and a week that has not started cannot be marked (P3.10).
 */
@HiltViewModel
class ProgramDeloadViewModel @Inject constructor(
    private val deloadRepository: DeloadRepository,
    private val loader: DeloadSourceReader,
    timeSource: TimeSource,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramDeload>().programId

    private val weekStart: LocalDate = ProgramSchedule.weekStartOf(
        timeSource.now().atZone(ZoneId.systemDefault()).toLocalDate(),
    )

    private val _state = MutableStateFlow(DeloadCreatorUiState(weekStart = weekStart))
    val uiState: StateFlow<DeloadCreatorUiState> = _state.asStateFlow()

    /** The program's days as they were read, before the day count and the selection narrow them. */
    private var sources: List<DeloadSourceDay> = emptyList()

    /** The planned exercises the lifter keeps, by the source template exercise's id. */
    private var selectedIds: Set<String> = emptySet()

    /**
     * A weekday the lifter moved, by source slot id (N147).
     *
     * A map with a null value, because "no day" is a choice: it has to be told apart from "not chosen",
     * which falls back to the source slot's own weekday.
     */
    private var weekdayOverrides: Map<String, DayOfWeek?> = emptyMap()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        when (val read = dataResultOf { loader.load(programId, weekStart) }) {
            is DataResult.Failure -> {
                // A read that fails is not "this program plans nothing" (N147): keep the failure apart so
                // the screen can say the plan could not be read rather than offering an empty draft.
                _state.update { it.copy(isLoading = false, loadFailed = true, error = read.error) }
            }

            is DataResult.Success -> {
                val snapshot = read.data
                if (snapshot == null) {
                    _state.update { it.copy(isLoading = false, error = DataError.NotFound) }
                    return
                }
                sources = snapshot.days
                selectedIds = snapshot.days
                    .flatMap { day -> day.exercises.map { it.templateExerciseId } }
                    .toSet()
                _state.update {
                    it.copy(
                        isLoading = false,
                        programName = snapshot.programName,
                        maxDays = snapshot.days.size,
                        dayCount = snapshot.days.size,
                        hasExistingPlan = snapshot.hasExistingPlan,
                    )
                }
                rebuild()
            }
        }
    }

    /** Ticks or unticks one planned exercise (N147). */
    fun onToggleExercise(templateExerciseId: String) {
        selectedIds = if (templateExerciseId in selectedIds) {
            selectedIds - templateExerciseId
        } else {
            selectedIds + templateExerciseId
        }
        rebuild()
    }

    /** Keeps the first [count] days of the program, within what it has (N147). */
    fun onDayCountChange(count: Int) {
        if (sources.isEmpty()) return
        _state.update { it.copy(dayCount = count.coerceIn(1, sources.size)) }
        rebuild()
    }

    /** Moves every derived load together; the rep and set rules do not move (N147). */
    fun onLoadPercentChange(percent: Int) {
        if (percent !in DELOAD_LOAD_KEPT_CHOICES) return
        _state.update { it.copy(loadKeptPercent = percent) }
        rebuild()
    }

    /** Moves one day to another weekday, or makes it order-only with null (N147). */
    fun onWeekdayChange(sourceSlotId: String, weekday: DayOfWeek?) {
        weekdayOverrides = weekdayOverrides + (sourceSlotId to weekday)
        rebuild()
    }

    /** Writes the week the screen shows, or reports why it could not (N147). */
    fun onCreate() {
        val current = _state.value
        if (!current.canCreate) return
        _state.update { it.copy(creating = true, error = null) }

        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = current.dayCount,
            selected = selectedIds,
            loadKeptPercent = current.loadKeptPercent,
            weekdayOf = ::weekdayOf,
        )

        viewModelScope.launch {
            if (inputs.isEmpty()) {
                _state.update {
                    it.copy(creating = false, selectedExerciseCount = 0)
                }
                return@launch
            }
            when (val result = deloadRepository.createDeload(programId, weekStart, inputs)) {
                is DataResult.Success -> _state.update {
                    it.copy(creating = false, created = result.data)
                }

                is DataResult.Failure -> _state.update {
                    it.copy(creating = false, error = result.error)
                }
            }
        }
    }

    /** The failure has been shown, so it is not shown twice. */
    fun onErrorShown() {
        _state.update { it.copy(error = null) }
    }

    private fun weekdayOf(day: DeloadSourceDay): DayOfWeek? =
        if (weekdayOverrides.containsKey(day.sourceSlotId)) {
            weekdayOverrides[day.sourceSlotId]
        } else {
            day.weekday
        }

    /**
     * Re-derives the visible days from the snapshot and the controls (N147).
     *
     * The reads are not re-run: this is the same source read three ways — how many days, which
     * exercises, which share — so a control cannot disagree with the plan it is about to write.
     */
    private fun rebuild() {
        val current = _state.value
        val days = buildDeloadDays(
            sources = sources,
            dayCount = current.dayCount,
            selected = selectedIds,
            loadKeptPercent = current.loadKeptPercent,
            weekdayOf = ::weekdayOf,
        )
        _state.update {
            it.copy(
                days = days,
                selectedExerciseCount = days.sumOf { day -> day.exercises.count { it.selected } },
            )
        }
    }
}

/**
 * The days on screen, from the snapshot and the controls (ROADMAP N147).
 *
 * Pure so the screen's state can be asserted without a ViewModel or a database, and so the preview and
 * the write read the same rule: [buildDeloadInputs] is this function's selected half in the write's own
 * shape.
 */
internal fun buildDeloadDays(
    sources: List<DeloadSourceDay>,
    dayCount: Int,
    selected: Set<String>,
    loadKeptPercent: Int,
    weekdayOf: (DeloadSourceDay) -> DayOfWeek?,
): List<DeloadDayUi> = sources.take(dayCount).map { day ->
    DeloadDayUi(
        sourceSlotId = day.sourceSlotId,
        sourceName = day.name,
        weekday = day.weekdayAfter(weekdayOf),
        exercises = day.exercises.map { exercise ->
            DeloadExerciseUi(
                templateExerciseId = exercise.templateExerciseId,
                exerciseId = exercise.exerciseId,
                name = exercise.name,
                selected = exercise.templateExerciseId in selected,
                sets = deloadSetTargets(
                    plannedSets = exercise.sets,
                    previous = exercise.previous,
                    loadKeptPercent = loadKeptPercent,
                    stepGrams = exercise.stepGrams,
                ),
                weightUnit = exercise.weightUnit,
            )
        },
    )
}

/**
 * What the creator writes, from the same snapshot and controls (ROADMAP N147).
 *
 * A day whose exercises are all unticks is left out — the day count is not a promise to write an empty
 * workout — and the name is the source workout's, so a template is findable in the library afterwards.
 */
internal fun buildDeloadInputs(
    sources: List<DeloadSourceDay>,
    dayCount: Int,
    selected: Set<String>,
    loadKeptPercent: Int,
    weekdayOf: (DeloadSourceDay) -> DayOfWeek?,
): List<DeloadDayInput> = sources.take(dayCount).mapNotNull { day ->
    val kept = day.exercises.filter { it.templateExerciseId in selected }
    if (kept.isEmpty()) return@mapNotNull null
    DeloadDayInput(
        name = "${day.name} — deload",
        weekday = day.weekdayAfter(weekdayOf),
        exercises = kept.map { exercise ->
            DeloadExerciseInput(
                exerciseId = exercise.exerciseId,
                restSeconds = exercise.restSeconds,
                techniqueNote = exercise.techniqueNote,
                supersetGroup = exercise.supersetGroup,
                sets = deloadSetTargets(
                    plannedSets = exercise.sets,
                    previous = exercise.previous,
                    loadKeptPercent = loadKeptPercent,
                    stepGrams = exercise.stepGrams,
                ),
            )
        },
    )
}
