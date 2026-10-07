package com.example.androidapp.ui.history

import kotlinx.coroutines.flow.asStateFlow
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.navigation.WorkoutDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import androidx.navigation.toRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A set as the history detail shows it. */
data class HistorySet(
    val id: String,
    val reps: Int,
    val weightGrams: Long,
    /** 1–10, or null when none was recorded (ROADMAP N6). */
    val rpeHalves: Int? = null,
    /** The set's comment, or null (ROADMAP N6). */
    val note: String? = null,
    /** The machine's assistance, 0 for none (ROADMAP N15). */
    val assistanceGrams: Long = 0,
    /**
     * The role it was performed as (ROADMAP N14).
     *
     * Modelled here since B14: the history editor could show a role and save it — it simply
     * never knew one, so editing a warm-up turned it into a working set.
     */
    val setType: SetType = SetType.NORMAL,
    /** The unit this set's load is read in (ROADMAP N64): its exercise's own, or the app's. */
    val weightUnit: WeightUnit? = null,
    /**
     * The library exercise's own weight step, or null for the unit's (ROADMAP N77, B65).
     *
     * Carried onto the set rather than read from the exercise beside it because the editor is opened
     * per set: without it the ± buttons here moved by 2.5 kg while the same set corrected inside the
     * workout moved by the step the movement actually loads in.
     */
    val stepGrams: Long? = null,
)

/** An exercise within a past workout, with everything that was logged for it. */
data class HistoryExercise(
    /** The session's own row id — what a set's edit and delete address (N6). */
    val id: String,
    /**
     * The *library* exercise this row is an instance of (ROADMAP N17).
     *
     * Distinct from [id] on purpose: `id` is one workout's row, while the trends screen
     * wants the movement, which is the only id under which a series exists. Passing the
     * row id there is a query that silently matches nothing.
     */
    val exerciseId: String,
    val name: String,
    /** This exercise's own display unit, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    val sets: List<HistorySet>,
    /** How well the target muscle was worked, 1–10, or null (ROADMAP N8). */
    val muscleFeel: Int? = null,
    /** The joints that hurt, each with its side and own score (ROADMAP N63), or empty. */
    val joints: List<JointPain> = emptyList(),
    /** Legacy joint pain, 1–10, or null (ROADMAP N8) — read for a session rated before N63. */
    val jointPain: Int? = null,
    /** Legacy "which joints" free text, or null (ROADMAP N9) — read, never rewritten. */
    val jointPainNote: String? = null,
)

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    /**
     * The plan's name, for the title, or null for a workout that began from nothing (N58, N84).
     *
     * Read live from the template row rather than stored on the session, so renaming a plan relabels the
     * past. The screen falls back to the workout's own date when this is null.
     */
    val templateName: String? = null,
    val exercises: List<HistoryExercise> = emptyList(),
    val error: DataError? = null,
) {
    val notFound: Boolean get() = !isLoading && session == null

    val duration: Duration?
        get() = session?.let { s -> s.finishedAt?.let { Duration.between(s.startedAt, it) } }

    val setCount: Int get() = exercises.sumOf { it.sets.size }

    /**
     * Gram-reps, the same figure the list's SQL computes. Kept in step deliberately:
     * this is one formula in two places, and the instrumented history test asserts
     * they agree.
     */
    val volumeGrams: Long get() = exercises.sumOf { exercise ->
        exercise.sets.sumOf { it.weightGrams * it.reps }
    }
}

/**
 * One past workout, with the edits that make a mis-tap recoverable (P1.6, P1.7).
 *
 * Reads the session, its exercises and its sets — the same three the live workout
 * screen uses — and derives the totals here rather than duplicating the list's
 * aggregate SQL for a single row.
 *
 * Correcting a set matters more than it looks: before this existed, `Finish` was a
 * one-way door. A mistyped weight was visible in history and permanently wrong.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    /** Saving a finished workout as a plan is a template write (ROADMAP N31). */
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle.toRoute<WorkoutDetail>().sessionId

    private val lastError = MutableStateFlow<DataError?>(null)

    private val _savedTemplate = MutableStateFlow<String?>(null)

    /**
     * The plan this workout was just saved as, for the screen to offer opening (ROADMAP N31).
     *
     * Null until it happens and cleared when dismissed, so the offer is a consequence of the action
     * rather than a state the screen has to reason about.
     */
    val savedTemplate: StateFlow<String?> = _savedTemplate.asStateFlow()

    /**
     * Saves this workout as a plan (ROADMAP N31).
     *
     * One repository call that copies the exercises and their performed sets as targets; the screens
     * only ask for a name and report what came back. The workout itself is untouched, and the copy is
     * independent of it in both directions.
     */
    fun onSaveAsTemplate(name: String) {
        viewModelScope.launch {
            when (val result = templateRepository.createTemplateFromSession(sessionId, name)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _savedTemplate.value = result.data
                }
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Called once the offer to open the new plan has been taken or dismissed. */
    fun onDismissSavedTemplate() {
        _savedTemplate.value = null
    }

    /** True once the workout is gone, so the screen can leave rather than sit on a blank page. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val session = workoutRepository.observeSession(sessionId)
    private val exercises = workoutRepository.observeSessionExercises(sessionId)
    private val sets = workoutRepository.observeSets(sessionId)

    /**
     * The plan's name for the title, read live from its row (N58, N84).
     *
     * Live rather than copied onto the session, which is the rule N58 argued: renaming a plan relabels the
     * past, and a deleted plan still names the workout it was — which is why this reads
     * [TemplateRepository.observeTemplateName] rather than `observeTemplate`, whose deleted filter is the
     * editor's rule and would blank the title the moment a plan was deleted (B74). A session with no plan
     * behind it has no id to follow, and the screen falls back to its own date.
     */
    private val templateName: Flow<String?> = session
        .map { it?.templateId }
        .distinctUntilChanged()
        .flatMapLatest { templateId ->
            if (templateId == null) {
                flowOf(null)
            } else {
                templateRepository.observeTemplateName(templateId)
            }
        }

    val uiState: StateFlow<WorkoutDetailUiState> =
        combine(session, exercises, sets, lastError, templateName) { current, exerciseRows, logged, error, name ->
            WorkoutDetailUiState(
                isLoading = false,
                session = current,
                templateName = name,
                exercises = exerciseRows.map { row ->
                    HistoryExercise(
                        id = row.id,
                        exerciseId = row.exerciseId,
                        name = row.exerciseName,
                        weightUnit = row.weightUnit,
                        muscleFeel = row.muscleFeel,
                        joints = row.joints,
                        jointPain = row.jointPain,
                        jointPainNote = row.jointPainNote,
                        sets = logged
                            .filter { it.sessionExerciseId == row.id }
                            .sortedBy { it.setIndex }
                            .map {
                                HistorySet(
                                    id = it.id,
                                    reps = it.reps,
                                    weightGrams = it.weightGrams,
                                    rpeHalves = it.rpeHalves,
                                    note = it.note,
                                    // Every column the row has, or the screen silently
                                    // renders a default (ROADMAP B5, N15).
                                    assistanceGrams = it.assistanceGrams,
                                    setType = it.setType,
                                    weightUnit = row.weightUnit,
                                    // And the movement's own step, so correcting a past set steps the
                                    // way the workout it came from does (ROADMAP N77, B65).
                                    stepGrams = row.stepGrams,
                                )
                            },
                    )
                },
                error = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = WorkoutDetailUiState(),
        )

    fun onUpdateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int? = null,
        note: String? = null,
        setType: SetType = SetType.NORMAL,
        assistanceGrams: Long = 0,
    ) = write {
        workoutRepository.updateSet(
            setId = setId,
            reps = reps,
            weightGrams = weightGrams,
            rpeHalves = rpeHalves,
            note = note,
            setType = setType,
            assistanceGrams = assistanceGrams,
        )
    }

    fun onDeleteSet(setId: String) = write {
        workoutRepository.deleteSet(setId)
    }

    /**
     * Saves how an exercise felt (ROADMAP N8), from the workout detail.
     *
     * This is the "editable later" half of the decision: the ratings are captured
     * when an exercise is marked done, but they can be filled in or corrected
     * afterwards without reopening the workout.
     */
    fun onRateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<JointPain>,
    ) = write {
        workoutRepository.rateExercise(sessionExerciseId, muscleFeel, joints)
    }

    /**
     * Removes the whole workout.
     *
     * A soft delete, so the rows stay for the export to carry — but with no restore
     * in the UI, so the screen confirms first.
     */
    fun onDeleteWorkout() {
        viewModelScope.launch {
            when (val result = workoutRepository.deleteSession(sessionId)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _deleted.value = true
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    private fun write(block: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            when (val result = block()) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
