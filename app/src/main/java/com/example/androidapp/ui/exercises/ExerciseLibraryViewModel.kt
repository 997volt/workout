package com.example.androidapp.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.LIBRARY_NOT_TRAINED_DAYS
import com.example.androidapp.domain.LibraryFilter
import com.example.androidapp.domain.LibrarySort
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.libraryFilterCounts
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val query: String = "",
    /**
     * The live library, which the screen groups (ROADMAP N95).
     *
     * The **whole** library rather than pre-grouped rows, and that is a division of labour rather than an
     * oversight: which families are open is screen state (`rememberRowFold`, N84's rule), so a grouping done
     * here would have to be told about it on every fold and would then lag the screen by a frame. The screen
     * holds the folds and calls the pure [com.example.androidapp.domain.libraryRows] with them.
     */
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    /**
     * No exercises exist at all, as opposed to none matching the query.
     *
     * The two looked identical on screen — both rendered "No exercises match \"\"" —
     * which told the user nothing about whether to clear the search or something
     * was actually wrong (ROADMAP P1.1a).
     */
    val libraryIsEmpty: Boolean = false,
    /**
     * The library could not be read (ROADMAP B4). Shown in place of the list, since
     * an empty list would be a lie about what is on the device.
     */
    val error: DataError? = null,
    /**
     * A write failed, and is a message rather than the page (ROADMAP B88).
     *
     * A failed read and a failed write used to share [error], so a *New category* that did not land replaced
     * the whole library with the read-failure page and left it there. The two are different sentences with
     * different consequences, and only the read one hides the list.
     */
    val writeError: DataError? = null,
    /**
     * Every row by id, removed ones included, so a child can name a head that is no longer offered (N95).
     *
     * Not part of the list: it is the answer to "what is this id called" rather than a set of rows to draw.
     */
    val removedHeads: Map<String, Exercise> = emptyMap(),
    /** How the list is ordered, which is the lifter's choice and starts at the library as it was (N230). */
    val sort: LibrarySort = LibrarySort.NAME,
    /**
     * When each movement was last trained, from its newest finished session (ROADMAP N230).
     *
     * Carried rather than applied here, like the library itself: the screen holds the folds and the list is
     * one pure function of what it was given.
     */
    val lastTrained: Map<String, Instant> = emptyMap(),
    /**
     * The last-trained read failed, or null (ROADMAP N230).
     *
     * A message rather than the page: the library is still readable, and what is lost is one way of ordering
     * it — but a sort that silently ordered everything as never trained would be a claim about the training
     * rather than about the read, which is the one thing the failure may not become.
     */
    val lastTrainedError: DataError? = null,
    /**
     * The group the list is narrowed to (ROADMAP N231), already resolved: a group that emptied falls back to
     * [LibraryFilter.ALL] rather than drawing a blank list under a selected chip.
     */
    val filter: LibraryFilter = LibraryFilter.ALL,
    /** How many movements each chip holds, from the whole library (ROADMAP N231). */
    val filterCounts: Map<LibraryFilter, Int> = emptyMap(),
    /**
     * The instant before which a session counts as *not recently trained* (ROADMAP N231).
     *
     * In the state because the screen builds the rows: the window is an input to the one pure function, not a
     * clock the screen reads.
     */
    val notTrainedSince: Instant = Instant.EPOCH,
) {
    /**
     * Nothing to show — the list is empty, whether because the library is or because the search missed.
     *
     * **Which of the two it is** is [libraryIsEmpty]'s answer, and the screen picks its copy from that;
     * asking it here too would be a second place for one fact to be read.
     */
    val isEmpty: Boolean
        get() = !isLoading && libraryRows(exercises, query, filter = filter).isEmpty()
}

/**
 * Holds the library's search state, the filtered list, and whether a workout is
 * already running (ROADMAP F3, P1.16).
 *
 * Backing out of a workout leaves the session active in the database. Without this
 * the library screen gave no sign of it and its button still read "Start workout",
 * which quietly undid P1.8's crash recovery on the ordinary path — the one users
 * actually take.
 */
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    /** The search and the order — the form's memory rather than the document's (ROADMAP N230). */
    private data class LibraryView(
        val query: String = "",
        val sort: LibrarySort = LibrarySort.NAME,
        val filter: LibraryFilter = LibraryFilter.ALL,
    )

    /**
     * The window the *Not trained in 30 days* chip means (ROADMAP N231).
     *
     * Read once for the screen's lifetime: a window that moved while the list was open would be a different
     * question under the same chip.
     */
    private val notTrainedSince: Instant =
        timeSource.now().minus(LIBRARY_NOT_TRAINED_DAYS, ChronoUnit.DAYS)

    private val view = MutableStateFlow(LibraryView())

    private val error = MutableStateFlow<DataError?>(null)

    /**
     * The whole library **including removed rows**, read once for naming (ROADMAP N95).
     *
     * A deleted head still names its children (N58's rule), so a child's family has to be resolvable after
     * the head stops being offered. Kept beside the observed list rather than in it: the list must not offer
     * a removed row, and this is only ever asked "what is this id called".
     */
    private val includingRemoved = MutableStateFlow<Map<String, Exercise>>(emptyMap())

    /**
     * The newest finished session per movement (ROADMAP N230), asked once for the whole library rather than
     * a lift at a time.
     */
    private val lastTrained: Flow<DataResult<Map<String, Instant>>> =
        exerciseRepository.observeLastTrained()

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        exerciseRepository.observeExercises(),
        view,
        error,
        includingRemoved,
        lastTrained,
    ) { result, currentView, currentError, removed, trained ->
        // A read failure is rendered where the list would have been (ROADMAP B4),
        // instead of escaping the flow and taking the screen down.
        val exercises = result.getOrNull().orEmpty()
        val trainedMap = trained.getOrNull().orEmpty()
        val counts = libraryFilterCounts(exercises, trainedMap, notTrainedSince)
        // **A group that emptied falls back to all** (N231): a blank list under a selected chip would say the
        // library is empty when the chip is what is empty. The filter is one the lifter chose, and an empty
        // one cannot stay chosen.
        val filter = if ((counts[currentView.filter] ?: 0) == 0) LibraryFilter.ALL else currentView.filter
        ExerciseLibraryUiState(
            query = currentView.query,
            exercises = exercises,
            isLoading = false,
            libraryIsEmpty = exercises.isEmpty(),
            // Either failure is kept apart (B88): a failed read takes the page, a failed write is a message.
            error = (result as? DataResult.Failure)?.error,
            writeError = currentError,
            removedHeads = removed,
            sort = currentView.sort,
            lastTrained = trainedMap,
            lastTrainedError = (trained as? DataResult.Failure)?.error,
            filter = filter,
            filterCounts = counts,
            notTrainedSince = notTrainedSince,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ExerciseLibraryUiState(),
    )

    init {
        viewModelScope.launch {
            // Deliberately silent on failure: this only resolves names, so a read that did not happen leaves
            // a child showing no family rather than breaking the screen.
            val result = exerciseRepository.getAllIncludingDeleted()
            includingRemoved.value = (result as? DataResult.Success)
                ?.data
                .orEmpty()
                .associateBy { it.id }
        }
    }

    fun onQueryChange(value: String) {
        view.update { it.copy(query = value) }
    }

    /** Reorders the list; nothing is written, so this is a display choice (N230). */
    fun onSortChange(value: LibrarySort) {
        view.update { it.copy(sort = value) }
    }

    /** Narrows the list to one group, inside whatever the search is (N231). */
    fun onFilterChange(value: LibraryFilter) {
        view.update { it.copy(filter = value) }
    }

    /**
     * Makes a family for the lifter's own movements (ROADMAP N95).
     *
     * The seeded families are a starting set rather than a closed one, so without this a custom movement
     * could only ever be filed under a head the seed happened to ship — which is the one thing the entry
     * says must not be true. Its movements are filed afterwards, from each movement's own editor.
     */
    fun onCreateCategory(name: String) {
        viewModelScope.launch {
            when (val result = exerciseRepository.createCategory(name)) {
                is DataResult.Success -> error.value = null
                is DataResult.Failure -> error.value = result.error
            }
        }
    }

    fun onErrorShown() {
        error.value = null
    }

    private companion object {
        /** Keeps the upstream flow warm across a configuration change. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
