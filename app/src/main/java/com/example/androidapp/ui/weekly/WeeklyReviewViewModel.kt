package com.example.androidapp.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ReviewDay
import com.example.androidapp.domain.model.WeekPlan
import com.example.androidapp.domain.model.WeekTotals
import com.example.androidapp.domain.model.weekPlan
import com.example.androidapp.domain.model.weekTotals
import com.example.androidapp.domain.repository.AdherenceRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val workouts: WorkoutRepository,
    private val adherence: AdherenceRepository,
    private val programs: ProgramRepository,
    private val stalls: PlanStallFinder,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyReviewUiState())
    val state: StateFlow<WeeklyReviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Re-reads the week; the screen's retry. */
    fun load() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = timeSource.now().atZone(zone).toLocalDate()
            val weekStart = ProgramSchedule.weekStartOf(today)

            val history = runCatching { workouts.observeHistory().first() }.getOrElse { error ->
                _state.value = WeeklyReviewUiState(
                    isLoading = false,
                    error = DataError.Storage(error),
                )
                return@launch
            }

            val days = mutableListOf<ReviewDay>()
            val occurrences = mutableListOf<DayOccurrence>()
            for (offset in 0 until DAYS_IN_WEEK) {
                val date = weekStart.plusDays(offset.toLong())
                when (val read = adherence.occurrencesOn(date, today, zone)) {
                    is DataResult.Success -> {
                        days += ReviewDay(date = date, occurrences = read.data)
                        occurrences += read.data
                    }

                    is DataResult.Failure -> {
                        _state.value = WeeklyReviewUiState(isLoading = false, error = read.error)
                        return@launch
                    }
                }
            }

            val stalled = firstStall()

            _state.value = WeeklyReviewUiState(
                isLoading = false,
                weekStart = weekStart,
                weekEnd = weekStart.plusDays((DAYS_IN_WEEK - 1).toLong()),
                thisWeek = weekTotals(history, weekStart, zone),
                lastWeek = weekTotals(history, weekStart.minusWeeks(1), zone),
                days = days,
                plan = weekPlan(occurrences),
                stall = stalled?.first,
                stallProgramId = stalled?.second,
            )
        }
    }

    /** The first programme whose plan has stopped moving, with its id, or null. */
    private suspend fun firstStall(): Pair<PlanStall, String>? {
        val active = runCatching { programs.observeActivePrograms().first() }.getOrDefault(emptyList())
        for (program in active) {
            val found = stalls.firstStall(program.id)
            if (found != null) return found to program.id
        }
        return null
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
