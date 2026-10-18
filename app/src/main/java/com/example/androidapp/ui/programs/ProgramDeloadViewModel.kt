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
import com.example.androidapp.domain.model.DeloadWeek
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
    /** The program days this movement appears on, for the pool's context (N152). */
    val sourceName: String = "",
    val selected: Boolean,
    val sets: List<DeloadSetTarget>,
    /** The unit the load is shown in: the exercise's own, or the app's (N64). */
    val weightUnit: WeightUnit,
)

/** One day of the week being built, already reduced to what it keeps (ROADMAP N147, N152). */
data class DeloadDayUi(
    /** The slot the day was seeded from; it supplies the weekday and addresses the day in a test (N147). */
    val sourceSlotId: String,
    /** The day's own workout name: Deload A, Deload B, … (N152). */
    val sourceName: String,
    val weekday: DayOfWeek?,
    val exercises: List<DeloadExerciseUi>,
)

data class DeloadCreatorUiState(
    val isLoading: Boolean = true,
    val programName: String? = null,
    /** The Monday of the week being built; the creator only ever writes the current one (P3.10). */
    val weekStart: LocalDate? = null,
    /**
     * The deload days on screen, each with the whole program's exercise pool to choose from (N152).
     *
     * The day count is how many deload days, not which source days: a day's exercises may come from any
     * source day, which is the point of a deload.
     */
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
    /** The week of the most recent built plan, or null when there is nothing to reuse (N150). */
    val lastDeloadWeek: LocalDate? = null,
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

    /**
     * The exercises each deload day keeps, by library exercise id (N152).
     *
     * One set per deload day, not one for the week: the same movement may be kept on two deload days, while
     * a day itself holds a movement once.
     */
    private var selectedByDay: MutableList<MutableSet<String>> = mutableListOf()

    /** The most recent plan before this week, for *Use last deload*, or null (N150). */
    private var lastPlan: DeloadWeek? = null

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
                lastPlan = snapshot.lastPlan
                // The first N deload days default to the source days at their positions, so an untouched
                // draft writes what it used to; every pool exercise is still available on every card (N152).
                selectedByDay = snapshot.days
                    .map { day -> day.exercises.map { it.exerciseId }.toMutableSet() }
                    .toMutableList()
                _state.update {
                    it.copy(
                        isLoading = false,
                        programName = snapshot.programName,
                        maxDays = snapshot.days.size,
                        dayCount = snapshot.days.size,
                        hasExistingPlan = snapshot.hasExistingPlan,
                        lastDeloadWeek = snapshot.lastPlan?.weekStart,
                    )
                }
                rebuild()
            }
        }
    }

    /**
     * Ticks or unticks one pool exercise on one deload day (N147, N152).
     *
     * The day index is the card's position; the pool is program-wide, so an exercise from a source day the
     * deload dropped is still selectable here. The day holds a movement once, which the pool's dedupe gives.
     */
    fun onToggleExercise(dayIndex: Int, exerciseId: String) {
        val selected = selectedByDay.getOrNull(dayIndex) ?: return
        if (exerciseId in selected) selected.remove(exerciseId) else selected.add(exerciseId)
        rebuild()
    }

    /**
     * Prefills the draft with the last deload's choices (N150).
     *
     * The exercises are matched by library id, so a movement the current plan still has is selected and one it
     * lost is not; the day count is the last plan's. Every number is then re-derived by [rebuild] from the
     * current plan and history — nothing from the old plan's templates is copied. The load share is not stored
     * with a plan, so it stays the creator's current choice rather than being guessed.
     */
    fun onUseLastDeload() {
        val plan = lastPlan ?: return
        val available = sources.flatMap { day -> day.exercises.map { it.exerciseId } }.toSet()
        val wanted = plan.days.size.coerceIn(1, sources.size)
        val next = plan.days
            .take(wanted)
            .map { day -> day.exerciseIds.filter { it in available }.toMutableSet() }
            .toMutableList()
        if (next.isEmpty() || next.all { it.isEmpty() }) return
        selectedByDay = next
        _state.update { it.copy(dayCount = wanted) }
        rebuild()
    }

    /**
     * Sets how many deload days the week has, within what the program has (N147, N152).
     *
     * The days are the deload's own, not a prefix of the program: adding one seeds it from the source day at
     * that position (the old default), and every card still draws from the whole pool.
     */
    fun onDayCountChange(count: Int) {
        if (sources.isEmpty()) return
        val wanted = count.coerceIn(1, sources.size)
        while (selectedByDay.size < wanted) {
            selectedByDay.add(
                sources.getOrNull(selectedByDay.size)?.exercises
                    ?.map { it.exerciseId }
                    ?.toMutableSet()
                    ?: mutableSetOf(),
            )
        }
        while (selectedByDay.size > wanted) selectedByDay.removeAt(selectedByDay.lastIndex)
        _state.update { it.copy(dayCount = wanted) }
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
            selectedByDay = selectedByDay,
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
            selectedByDay = selectedByDay,
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
 * The program's exercise pool, once per movement (N152).
 *
 * A movement on two source days is one row: the first source day supplies the plan the rule reads, and both
 * day names travel as context. That is what lets a two-day deload keep a lift the program only schedules on
 * day three while still showing where it came from.
 */
private data class PoolExercise(val exercise: DeloadSourceExercise, val sourceNames: String)

/** The written workout's name: Deload A, Deload B, … in day order (N152). */
private fun deloadDayName(index: Int): String =
    if (index < DELOAD_DAY_LETTERS) {
        "Deload ${('A'.code + index).toChar()}"
    } else {
        "Deload ${index + 1}"
    }

/** Past the alphabet a week is numbered rather than lettered; the cap is the alphabet, not a day limit. */
private const val DELOAD_DAY_LETTERS = 26

private fun poolOf(sources: List<DeloadSourceDay>): List<PoolExercise> {
    val byExercise = LinkedHashMap<String, PoolExercise>()
    sources.forEach { day ->
        day.exercises.forEach { exercise ->
            val existing = byExercise[exercise.exerciseId]
            byExercise[exercise.exerciseId] = if (existing == null) {
                PoolExercise(exercise, day.name)
            } else {
                existing.copy(
                    sourceNames = (existing.sourceNames.split(", ") + day.name)
                        .distinct()
                        .joinToString(", "),
                )
            }
        }
    }
    return byExercise.values.toList()
}

/**
 * The days on screen, from the snapshot and the controls (ROADMAP N147, N152).
 *
 * Pure so the screen's state can be asserted without a ViewModel or a database, and so the preview and
 * the write read the same rule: [buildDeloadInputs] is this function's selected half in the write's own
 * shape. Every day carries the **whole program pool**, selected per day; the day's name and weekday start
 * from the source day at its position.
 */
internal fun buildDeloadDays(
    sources: List<DeloadSourceDay>,
    dayCount: Int,
    selectedByDay: List<Set<String>>,
    loadKeptPercent: Int,
    weekdayOf: (DeloadSourceDay) -> DayOfWeek?,
): List<DeloadDayUi> {
    val pool = poolOf(sources)
    return (0 until dayCount).mapNotNull { index ->
        val day = sources.getOrNull(index) ?: return@mapNotNull null
        val selected = selectedByDay.getOrElse(index) { emptySet() }
        DeloadDayUi(
            sourceSlotId = day.sourceSlotId,
            sourceName = deloadDayName(index),
            weekday = day.weekdayAfter(weekdayOf),
            exercises = pool.map { pooled ->
                val exercise = pooled.exercise
                DeloadExerciseUi(
                    templateExerciseId = exercise.templateExerciseId,
                    exerciseId = exercise.exerciseId,
                    name = exercise.name,
                    sourceName = pooled.sourceNames,
                    selected = exercise.exerciseId in selected,
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
}

/**
 * What the creator writes, from the same snapshot and controls (ROADMAP N147, N152).
 *
 * A day whose selected pool is empty is left out — the day count is not a promise to write an empty workout —
 * and the workout is named **Deload A, Deload B, …** in day order, so the built week reads as a week rather
 * than as copies of the program's templates. The template editor renames one afterwards.
 */
internal fun buildDeloadInputs(
    sources: List<DeloadSourceDay>,
    dayCount: Int,
    selectedByDay: List<Set<String>>,
    loadKeptPercent: Int,
    weekdayOf: (DeloadSourceDay) -> DayOfWeek?,
): List<DeloadDayInput> {
    val pool = poolOf(sources)
    return (0 until dayCount).mapNotNull { index ->
        val day = sources.getOrNull(index) ?: return@mapNotNull null
        val selected = selectedByDay.getOrElse(index) { emptySet() }
        val kept = pool.map { it.exercise }.filter { it.exerciseId in selected }
        if (kept.isEmpty()) return@mapNotNull null
        DeloadDayInput(
            name = deloadDayName(index),
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
}
