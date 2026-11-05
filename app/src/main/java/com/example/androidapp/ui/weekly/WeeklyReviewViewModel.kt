package com.example.androidapp.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ReviewDay
import com.example.androidapp.domain.model.WeekPlan
import com.example.androidapp.domain.model.WeekTotals
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.weekPlan
import com.example.androidapp.domain.model.weekTotals
import com.example.androidapp.domain.repository.WeeklyReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What one week in review says (ROADMAP N247). */
data class WeeklyReviewUiState(
    val isLoading: Boolean = true,
    val weekStart: LocalDate? = null,
    val weekEnd: LocalDate? = null,
    val thisWeek: WeekTotals = WeekTotals(sessions = 0, sets = 0, volumeGrams = 0),
    val lastWeek: WeekTotals = WeekTotals(sessions = 0, sets = 0, volumeGrams = 0),
    val days: List<ReviewDay> = emptyList(),
    val plan: WeekPlan = WeekPlan(planned = 0, done = 0),
    /** The lifts that moved this week (N251), alphabetically. */
    val movements: List<MovementGain> = emptyList(),
    /** The followed blocks, whose days are taken in order rather than on weekdays (B329). */
    val blocks: List<BlockProgress> = emptyList(),
    /** True when any program is followed, even one with no weekday schedule (B329). */
    val hasActivePlan: Boolean = false,
    val stall: PlanStall? = null,
    val stallProgramId: String? = null,
    val error: DataError? = null,
)

/**
 * The week's plan against the week's sessions, and the plan that has stopped moving (ROADMAP N247).
 *
 * One read of the history and seven of the adherence record — a day at a time, the same call the correction
 * dialog uses — so the review cannot disagree with the grid about what a day scheduled. The stall is the
 * editor's own rule through [PlanStallFinder], not a second copy.
 */
@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val weekly: WeeklyReviewRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyReviewUiState())
    val state: StateFlow<WeeklyReviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Re-reads the week; the screen's retry. */
    // The week's reads and its six sources are one state update; the split is N285.
    @Suppress("LongMethod")
    fun load() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = timeSource.now().atZone(zone).toLocalDate()
            val weekStart = ProgramSchedule.weekStartOf(today)

            val history = when (val read = weekly.history()) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }

            val week = when (val read = readDays(weekStart, today, zone)) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }
            val (days, occurrences) = week

            val movements = when (val read = weekly.movementGains(weekStart, zone)) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }

            val blocks = when (val read = weekly.blocks()) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }
            val active = when (val read = weekly.activePrograms()) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    // The same rule as the reads above (N255): `hasActivePlan` feeds a sentence — "no program
                    // is being followed" — so a failed read must not answer it.
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }
            val stalled = when (val read = firstStall(active)) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    // A failed stall read is not "no stall" (B351), and this screen already says a read's
                    // failure rather than drawing an answer it did not get.
                    _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                    return@launch
                }
            }

            _state.value = WeeklyReviewUiState(
                isLoading = false,
                weekStart = weekStart,
                weekEnd = weekStart.plusDays((DAYS_IN_WEEK - 1).toLong()),
                thisWeek = weekTotals(history, weekStart, zone),
                lastWeek = weekTotals(history, weekStart.minusWeeks(1), zone),
                days = days,
                plan = weekPlan(occurrences),
                movements = movements,
                blocks = blocks,
                hasActivePlan = active.isNotEmpty(),
                stall = stalled?.first,
                stallProgramId = stalled?.second,
            )
        }
    }

    /**
     * The week's seven days and the occurrences each holds, or the first day that could not be read (N252).
     *
     * One read because a partial week is not a week: the totals and the plan ratio are computed from these
     * two lists, and a day missing from either would quietly change what the screen claims.
     */
    private suspend fun readDays(
        weekStart: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<Pair<List<ReviewDay>, List<DayOccurrence>>> {
        val days = mutableListOf<ReviewDay>()
        val occurrences = mutableListOf<DayOccurrence>()
        for (offset in 0 until DAYS_IN_WEEK) {
            val date = weekStart.plusDays(offset.toLong())
            when (val read = weekly.occurrencesOn(date, today, zone)) {
                is DataResult.Success -> {
                    days += ReviewDay(date = date, occurrences = read.data)
                    occurrences += read.data
                }

                is DataResult.Failure -> return DataResult.Failure(read.error)
            }
        }
        return DataResult.Success(days to occurrences)
    }

    /** The first programme whose plan has stopped moving, with its id, or the read's failure (B351). */
    private suspend fun firstStall(active: List<WorkoutProgram>): DataResult<Pair<PlanStall, String>?> =
        active.fold(DataResult.Success(null) as DataResult<Pair<PlanStall, String>?>) { acc, program ->
            if (acc is DataResult.Failure) {
                acc
            } else {
                when (val found = weekly.firstStall(program.id)) {
                    is DataResult.Failure -> found
                    is DataResult.Success -> found.data
                        ?.let { DataResult.Success(it to program.id) }
                        ?: acc
                }
            }
        }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
