package com.example.androidapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** One month's worth of workouts, newest month first. */
data class HistoryGroup(
    val month: YearMonth,
    val workouts: List<WorkoutSummary>,
)

data class WorkoutHistoryUiState(
    val isLoading: Boolean = true,
    val groups: List<HistoryGroup> = emptyList(),
    /** The query the list is filtered by (ROADMAP N146), owned here so a rotation does not lose it. */
    val query: String = "",
    /** Which kinds of day are shown (ROADMAP N146). */
    val filter: HistoryFilter = HistoryFilter.ALL,
) {
    /** Logged nothing yet, as opposed to "nothing matched a filter" (P1.1a's lesson). */
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()

    /**
     * The list is empty *because* of the query or the chip (ROADMAP N146).
     *
     * The distinction the whole screen turns on: "you have not trained yet" and "nothing here matches" are
     * different facts, and a filter that shows the wrong sentence is worse than no filter.
     */
    val isFilteredEmpty: Boolean
        get() = isEmpty && (query.isNotBlank() || filter != HistoryFilter.ALL)
}

/**
 * The history list (ROADMAP P1.6).
 *
 * Totals arrive pre-aggregated from SQL, so this only groups and sorts — which is
 * also why a year of training costs one query rather than one per workout.
 */
@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    /** The query the lifter is typing (ROADMAP N146). */
    private val query = MutableStateFlow("")

    /** The chip they picked (ROADMAP N146). */
    private val filter = MutableStateFlow(HistoryFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WorkoutHistoryUiState> =
        combine(workoutRepository.observeHistory(), query, filter) { workouts, typed, picked ->
            // Each session brings its own zone; this is the fallback for the ones that cannot.
            val zone = ZoneId.systemDefault()
            WorkoutHistoryUiState(
                isLoading = false,
                groups = historyGroups(workouts, typed, picked, zone),
                query = typed,
                filter = picked,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = WorkoutHistoryUiState(),
            )

    /** Types a query; empty is every row the chip admits. */
    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Picks a chip. */
    fun onFilterChange(value: HistoryFilter) {
        filter.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Groups workouts by the local month they started in, newest first.
 *
 * Pure, so the ordering rules are unit-tested rather than eyeballed. Grouping on
 * `startedAt` rather than `finishedAt` is deliberate: a workout that begins at
 * 23:50 and ends after midnight belongs to the day the user started it.
 */
internal fun groupByMonth(
    workouts: List<WorkoutSummary>,
    /** The fallback for rows that predate the offset column; required for the same reason (B38). */
    zone: ZoneId,
): List<HistoryGroup> = workouts
    // Each session is grouped by the month **it** was performed in, not the month it is where you
    // are reading this: a workout done in Tokyo on the 1st belongs to that month, and the app was
    // showing it under the previous one the moment you landed (ROADMAP N25). [zone] is the fallback
    // for the rows that predate the column, which is what they already got.
    .groupBy { YearMonth.from(it.startedAt.atZone(it.zoneIdOrNull() ?: zone)) }
    .toSortedMap(compareByDescending { it })
    .map { (month, inMonth) ->
        HistoryGroup(
            month = month,
            workouts = inMonth.sortedByDescending { it.startedAt },
        )
    }
