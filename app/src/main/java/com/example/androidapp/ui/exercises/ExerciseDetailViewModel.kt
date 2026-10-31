package com.example.androidapp.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.navigation.ExerciseDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * What the detail screen's edit form produces (ROADMAP N2, N5).
 *
 * Deliberately a screen-level draft rather than an [Exercise]: the form edits the
 * attributes a user can change, and secondary muscles are not among them, so
 * passing a whole exercise around would invite editing the rest by accident.
 */
data class ExerciseEdit(
    val name: String,
    /**
     * What the form states for this row's taxonomy, each null for *unstated* (ROADMAP N103).
     *
     * Nullable rather than defaulted to `OTHER`, because null is the row's real answer for a field it reads
     * from its category. **Every field is offered either way** (N110): a movement is allowed to disagree with
     * its family, so the form cannot withhold a picker and still let the lifter say so.
     */
    val primaryMuscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val movementPattern: MovementPattern? = null,
    /** The exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /** A cue to read while lifting, or null (ROADMAP N5). */
    val techniqueNote: String? = null,
    /** This exercise's own display unit, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    /** This exercise's own weight step in grams, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
    /** Whether the movement is loaded by the body (ROADMAP N219). */
    val isBodyweight: Boolean = false,
    /** Whether a set of the movement is held rather than counted (ROADMAP N220). */
    val isTimed: Boolean = false,
    /** This movement's own answer to *Done*, or null to follow the app setting (ROADMAP N235). */
    val progressionMode: ProgressionMode? = null,
    /**
     * The category this row is filed under, or null for none (ROADMAP N95, N110).
     *
     * Part of the edit rather than a write of its own: *move to category* is this same form saving one more
     * field, which is what keeps it from being a second flow that has to agree about validation.
     */
    val parentId: String? = null,
)

/**
 * What this movement's history says at a glance (ROADMAP N233).
 *
 * Three answers from two reads the app already had: the newest finished session that trained it (with the
 * sets that session holds), and the best at each rep count — or the longest hold, because a timed movement
 * has no rep count to be best at.
 */
data class ExerciseHistoryUi(
    /** When the lift was last trained, or null when it never was. */
    val lastTrainedAt: Instant? = null,
    /** The working sets that last session holds, in the order they were logged. */
    val lastSets: List<SetEntry> = emptyList(),
    /** Best at each rep count, and the longest hold (ROADMAP N23, N233). */
    val records: PersonalRecords = PersonalRecords(),
)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val error: DataError? = null,
    /**
     * The categories this row can be filed under (ROADMAP N95, N110), live from the library.
     *
     * A **category**, and only a category: the shape is one rule deep, so a movement is nobody's head.
     * Empty for a category, which sits at the top level.
     */
    val categoryOptions: List<Exercise> = emptyList(),
    /**
     * What the category this row is filed under is called, or null when it is in none (ROADMAP N95).
     *
     * Read rather than copied, and **still answered by a category that has been removed** (N58's rule),
     * which is why the ViewModel reads the library including removed rows for this one question.
     */
    val headName: String? = null,
    /** This row's category, for the muscle and pattern it passes down (N95). Null when it is in none. */
    val head: Exercise? = null,
    /**
     * Every row above this one, nearest first, **removed rows included** (ROADMAP N103, N110).
     *
     * The chain rather than one resolved answer, because the screen asks several questions of it: what this
     * row's muscle and pattern resolve to, and which of those the category already supplies — so the picker
     * can show what *not stated* will mean. The walk goes to the top because a file is a place a longer chain
     * can arrive, even though the shape this app writes is one level (B92, B96).
     */
    val ancestors: List<Exercise> = emptyList(),
    /** This movement's history, empty until the read answers (ROADMAP N233). */
    val history: ExerciseHistoryUi = ExerciseHistoryUi(),
    /**
     * The history read failed, or null.
     *
     * A message inside the section rather than the page's error: the exercise itself is readable, and what is
     * missing is one section — but an empty section must not stand in for "never trained", which is a claim
     * about the training rather than about the read.
     */
    val historyError: DataError? = null,
    /** Why the last merge did not happen, shown inside the merge dialog (ROADMAP N245). */
    val mergeError: DataError? = null,
) {
    /**
     * Loaded, but no such exercise — a real state, not an error to hide.
     *
     * Excludes a failed read (ROADMAP B4): that is a different sentence on screen,
     * and telling the user the exercise does not exist when the database is the
     * thing that failed would be a lie.
     */
    val notFound: Boolean get() = !isLoading && exercise == null && error == null

    /**
     * Whether the editor is offered (ROADMAP N2, widened by N5).
     *
     * Every row is editable, seeded ones included, because the seeder never updates a row that exists.
     * N95 leans on that rather than changing it: a seeded **category** has to be renamable and its children
     * re-filable, since the seeded families are a starting set rather than a closed one — a taxonomy
     * somebody maintains is the whole reason a category exists.
     */
    val canEdit: Boolean get() = !isLoading && !isEditing && exercise != null
}

/**
 * Loads one exercise for the detail screen, and saves edits to it.
 *
 * The id comes out of [SavedStateHandle] via the type-safe route, so the
 * ViewModel is recreated correctly after process death with the same argument.
 */
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val workouts: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.toRoute<ExerciseDetail>().exerciseId

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // A plain one-shot read, held in state so a successful edit is
            // reflected without a second round trip. Wrapped like the writes
            // (ROADMAP B4), so a failed read is a message on the screen rather
            // than an exception lost inside this coroutine.
            when (val result = repository.getExercise(exerciseId)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(exercise = result.data, isLoading = false, error = null)
                }

                is DataResult.Failure -> _uiState.update {
                    it.copy(exercise = null, isLoading = false, error = result.error)
                }
            }
            loadLibraryContext()
            loadHistory()
        }
    }

    fun onEdit() {
        _uiState.update { it.copy(isEditing = true, error = null) }
    }

    /**
     * Leaves the editor without writing (ROADMAP N95).
     *
     * Nothing was stored, so cancelling leaves the library exactly as it was.
     */
    fun onCancelEdit() {
        _uiState.update { state ->
            state.copy(
                isEditing = false,
                error = null,
            )
        }
    }

    /**
     * The library context this row is read through (ROADMAP N95, B85, B96).
     *
     * Three answers from one pair of reads, because they are the same question asked three ways: the
     * categories the picker may offer, the one this row is already filed under, and the **family pattern**
     * the row resolves to. Read separately from the exercise itself and *not* wrapped in a failure branch: a
     * list that could not be read leaves the picker empty, which hides a control rather than breaking the
     * page. The exercise's own row is left out of the options — filing something under itself is not a thing
     * the shape allows — and **only categories are offered** (N110): a movement is nobody's head.
     */
    private suspend fun loadLibraryContext() {
        // Two reads, because they answer two different questions. The **live** library is what the picker may
        // offer: filing a row under a category that was removed would be filing it nowhere, and N95's rule is
        // that a removed category keeps naming what is already under it rather than taking anything new. The
        // read *including* removed rows is what names the category this row is already filed under, because a
        // removed category still names its children (N58's rule) — and it is the library the resolver walks,
        // so a movement under a removed category still reads that category's pattern and muscle.
        val live = (repository.observeExercises().first() as? DataResult.Success)?.data.orEmpty()
        val everything = (repository.getAllIncludingDeleted() as? DataResult.Success)?.data.orEmpty()
        _uiState.update { state ->
            val head = everything.firstOrNull { it.id == state.exercise?.parentId }
            state.copy(
                // Only a category is a place to be filed (N110): the shape is one rule deep, so nothing
                // else can be offered without offering a row the list would then refuse to draw.
                categoryOptions = live.filter { candidate ->
                    candidate.rowKind == RowKind.CATEGORY && candidate.id != state.exercise?.id
                },
                head = head,
                headName = head?.name,
                ancestors = ancestorsOf(state.exercise, everything),
            )
        }
    }

    /**
     * The movement's own history (ROADMAP N233).
     *
     * Two reads the app already had — the prefill's previous performance and the record rule — asked with no
     * session to exclude, because this screen is not a workout. **Deliberately not part of the page's error**:
     * a section that could not be read draws its own sentence, and the rest of the exercise stays readable.
     */
    private suspend fun loadHistory() {
        val exercise = _uiState.value.exercise ?: return
        val previous = workouts.previousPerformance(exercise.id)
        val records = workouts.personalRecords(exercise.id, excludingSessionId = null)
        val failure = listOfNotNull(previous, records)
            .firstNotNullOfOrNull { (it as? DataResult.Failure)?.error }
        if (failure != null) {
            _uiState.update { it.copy(historyError = failure) }
            return
        }
        _uiState.update { state ->
            val performed = (previous as? DataResult.Success)?.data
            state.copy(
                historyError = null,
                history = ExerciseHistoryUi(
                    lastTrainedAt = performed?.sessionStartedAt,
                    // **Working sets only**: a warm-up is not what "the last time you did this" means (N67),
                    // and a rung is part of the group above it rather than a set of its own (N79).
                    lastSets = performed?.sets.orEmpty().filter { it.setType.recordsEffort },
                    records = (records as? DataResult.Success)?.data ?: PersonalRecords(),
                ),
            )
        }
    }

    /**
     * Every row above [exercise], nearest first, ending at the first cycle or missing link (ROADMAP N103).
     *
     * Read from the library including removed rows, because a removed head still supplies what it stated
     * (N58's rule) — and a cycle is not a shape this app writes, but a file is a place one arrives, so
     * `seen` ends the walk rather than hanging on it (B92).
     */
    private fun ancestorsOf(exercise: Exercise?, library: List<Exercise>): List<Exercise> {
        val byId = library.associateBy { it.id }
        val seen = mutableSetOf(exercise?.id)
        val chain = mutableListOf<Exercise>()
        var parent = exercise?.parentId?.let { byId[it] }
        while (parent != null && seen.add(parent.id)) {
            chain += parent
            parent = byId[parent.parentId]
        }
        return chain
    }

    /**
     * Writes the edited attributes, keeping the row's id, secondary muscles and custom flag.
     *
     * A failure keeps the form open with what was typed rather than pretending it saved. The library context
     * is re-read on success, so a *move to category* shows the new family and the muscle it now inherits
     * rather than the pre-move answers (B84).
     */
    fun onSave(edit: ExerciseEdit) {
        val state = _uiState.value
        val current = state.exercise ?: return
        viewModelScope.launch {
            val outcome: DataResult<Exercise> = write(current.withEdit(edit))
            when (outcome) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exercise = outcome.data,
                            isEditing = false,
                            error = null,
                        )
                    }
                    loadLibraryContext()
                    // The unit the history reads in follows the exercise's own setting (N64), so an edit
                    // that changed it has to be re-read rather than left showing the old one.
                    loadHistory()
                }

                is DataResult.Failure -> _uiState.update { it.copy(error = outcome.error) }
            }
        }
    }

    /**
     * Every other live movement, as the merge dialog's candidates (ROADMAP N245).
     *
     * A category is never a candidate — a merge moves history, and a category has none — and neither is the
     * row the screen is on. Alphabetical because the dialog searches within it.
     */
    val mergeCandidates: StateFlow<List<Exercise>> = repository.observeExercises()
        .map { result -> (result as? DataResult.Success)?.data.orEmpty() }
        .map { rows ->
            rows.filter { it.id != exerciseId && it.rowKind == RowKind.MOVEMENT }
                .sortedBy { it.name.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(MERGE_CANDIDATE_STOP_MILLIS), emptyList())

    private val _merged = MutableStateFlow(false)

    /** True once a merge landed; the route leaves the screen of the row that no longer exists. */
    val merged: StateFlow<Boolean> = _merged.asStateFlow()

    /** Merges this row into [intoId]; a failure stays in the dialog rather than replacing the screen. */
    fun onMerge(intoId: String) {
        _uiState.update { it.copy(mergeError = null) }
        viewModelScope.launch {
            when (val result = repository.mergeExercises(exerciseId, intoId)) {
                is DataResult.Success -> _merged.value = true
                is DataResult.Failure -> _uiState.update { it.copy(mergeError = result.error) }
            }
        }
    }

    /** Writes [row], returning it on success so the screen can hold what was stored. */
    private suspend fun write(row: Exercise): DataResult<Exercise> =
        when (val result = repository.updateExercise(row)) {
            is DataResult.Success -> DataResult.Success(row)
            is DataResult.Failure -> result
        }

    private fun Exercise.withEdit(edit: ExerciseEdit): Exercise = copy(
        name = edit.name.trim(),
        primaryMuscle = edit.primaryMuscle,
        equipment = edit.equipment,
        movementPattern = edit.movementPattern,
        restSeconds = edit.restSeconds,
        techniqueNote = edit.techniqueNote,
        weightUnit = edit.weightUnit,
        stepGrams = edit.stepGrams,
        isBodyweight = edit.isBodyweight,
        isTimed = edit.isTimed,
        // The movement's own answer to Done, or null for the app setting (N235).
        progressionMode = edit.progressionMode,
        // The one field that makes this write *move to category* as well as an edit (N95).
        parentId = edit.parentId,
    )
}

/** The candidate flow stops a moment after the dialog closes: one screen's read, not a live watch (N245). */
private const val MERGE_CANDIDATE_STOP_MILLIS = 5_000L
