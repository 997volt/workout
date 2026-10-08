package com.example.androidapp.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) {
    /**
     * A search that matched nothing — deliberately distinct from [isLoading] so
     * the UI shows "no results" rather than a spinner that never resolves.
     */
    /**
     * Nothing to show — the list is empty, whether because the library is or because the search missed.
     *
     * **Which of the two it is** is [libraryIsEmpty]'s answer, and the screen picks its copy from that;
     * asking it here too would be a second place for one fact to be read.
     */
    val isEmpty: Boolean
        get() = !isLoading && libraryRows(exercises, query).isEmpty()
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
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val error = MutableStateFlow<DataError?>(null)

    /**
     * The whole library **including removed rows**, read once for naming (ROADMAP N95).
     *
     * A deleted head still names its children (N58's rule), so a child's family has to be resolvable after
     * the head stops being offered. Kept beside the observed list rather than in it: the list must not offer
     * a removed row, and this is only ever asked "what is this id called".
     */
    private val includingRemoved = MutableStateFlow<Map<String, Exercise>>(emptyMap())

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        exerciseRepository.observeExercises(),
        query,
        error,
        includingRemoved,
    ) { result, currentQuery, currentError, removed ->
        // A read failure is rendered where the list would have been (ROADMAP B4),
        // instead of escaping the flow and taking the screen down.
        val exercises = result.getOrNull().orEmpty()
        ExerciseLibraryUiState(
            query = currentQuery,
            exercises = exercises,
            isLoading = false,
            libraryIsEmpty = exercises.isEmpty(),
            // Either failure is kept apart (B88): a failed read takes the page, a failed write is a message.
            error = (result as? DataResult.Failure)?.error,
            writeError = currentError,
            removedHeads = removed,
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
        query.value = value
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
