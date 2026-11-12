package com.example.androidapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.loadGrams
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.components.WriteOutcome
import com.example.androidapp.ui.navigation.WorkoutDetail
import com.example.androidapp.ui.workout.SetWrite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

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
    /** The bodyweight this set was logged at, or null (ROADMAP N219). */
    val bodyweightGrams: Long? = null,
    /** How long the set was held, or null for a counted set (ROADMAP N220). */
    val durationSeconds: Int? = null,
    /** Whether the movement is loaded by the body, so the editor's weight field says "added" (B245). */
    val isBodyweight: Boolean = false,
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
    /** The directions of a step offered and not taken (ROADMAP N236), or empty. */
    val declinedDirections: Set<ProgressionDirection> = emptySet(),
    /** The last session that trained the movement, for the by-place deltas (N306), or null. */
    val previous: HistoryPrevious? = null,
)

/** One set as the previous session logged it, for the detail's by-place comparison (ROADMAP N306). */
data class PastSet(
    val reps: Int,
    val weightGrams: Long,
    val durationSeconds: Int? = null,
)

/** What a movement did the last time it was trained, or why there is nothing to compare (N306). */
data class HistoryPrevious(
    /** Its sets in the order they were logged: the counterpart of this session's set at the same place. */
    val sets: List<PastSet>,
    /** When that session started, or null when the read did not carry it. */
    val startedAt: Instant? = null,
)

/**
 * What changed from the previous session's set in the same place (N306).
 *
 * [weightGrams] is the change in the number the set's own field states, **not** the bodyweight-composed load
 * a bodyweight row displays: a change in the lifter's weight is not a change in what they did. [holdSeconds]
 * is null unless both sides were holds, because a counted set and a hold have no duration to subtract.
 */
data class SetDelta(
    val weightGrams: Long,
    val reps: Int,
    val holdSeconds: Int? = null,
) {
    val unchanged: Boolean get() = weightGrams == 0L && reps == 0 && (holdSeconds ?: 0) == 0
}

/** The delta from [previous] to [set], or null when there is no counterpart in the previous session (N306). */
fun setDelta(set: HistorySet, previous: PastSet?): SetDelta? {
    if (previous == null) return null
    return SetDelta(
        weightGrams = set.weightGrams - previous.weightGrams,
        reps = set.reps - previous.reps,
        holdSeconds = if (set.durationSeconds != null && previous.durationSeconds != null) {
            set.durationSeconds - previous.durationSeconds
        } else {
            null
        },
    )
}

/** What the previous session looked like, reduced to the place a delta needs (N306). */
private fun PreviousPerformance.toHistoryPrevious(): HistoryPrevious = HistoryPrevious(
    sets = sets.sortedBy { it.setIndex }.map {
        PastSet(reps = it.reps, weightGrams = it.weightGrams, durationSeconds = it.durationSeconds)
    },
    startedAt = sessionStartedAt,
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
        exercise.sets.sumOf { loadGrams(it.weightGrams, it.bodyweightGrams, it.assistanceGrams) * it.reps }
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
    /** Scheduling the new plan into a program is a slot write (P3.18). */
    private val programRepository: ProgramRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle.toRoute<WorkoutDetail>().sessionId

    private val lastError = MutableStateFlow<DataError?>(null)

    /** The last set edit's answer, so its editor closes on success and stays on a refusal (B350). */
    private val _setWrite = MutableStateFlow<SetWrite?>(null)
    val setWriteOutcome: StateFlow<SetWrite?> = _setWrite.asStateFlow()

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

    /** The programs the saved plan can be scheduled into (P3.18). */
    val programs: StateFlow<List<WorkoutProgram>> = programRepository.observePrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    private val _scheduledProgram = MutableStateFlow<String?>(null)

    /**
     * The program the saved plan was just added to, for the screen to open (P3.18).
     *
     * The slot is order-only when [weekday] is null; the program's editor is where more days are set,
     * which is why the offer lands there rather than in the templates list.
     */
    val scheduledProgram: StateFlow<String?> = _scheduledProgram.asStateFlow()

    /** Adds the plan just saved to [programId], on [weekday] or order-only (P3.18). */
    fun onScheduleTemplate(programId: String, weekday: DayOfWeek?) {
        val templateId = _savedTemplate.value ?: return
        viewModelScope.launch {
            when (val result = programRepository.addSlot(programId, templateId, weekday)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _scheduledProgram.value = programId
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Called once the schedule offer has been taken, so it does not open twice. */
    fun onScheduledHandled() {
        _scheduledProgram.value = null
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

    /**
     * What each movement did the last time it was trained (ROADMAP N306), read once per exercise.
     *
     * Null means "nothing to compare" — no earlier session, or a read that failed. The comparison is
     * advisory, so a failure is not turned into a claim that the lift was never trained; it simply says
     * nothing, and the reads around it report their own errors.
     */
    private val previousByExercise = MutableStateFlow<Map<String, PreviousPerformance?>>(emptyMap())

    init {
        viewModelScope.launch {
            exercises
                .map { rows -> rows.map { it.exerciseId }.distinct() }
                .distinctUntilChanged()
                .collect { exerciseIds ->
                    exerciseIds
                        .filterNot { it in previousByExercise.value }
                        .forEach { exerciseId ->
                            previousByExercise.update {
                                it + (exerciseId to readPrevious(exerciseId))
                            }
                        }
                }
        }
    }

    /** The movement's previous training, or null when there is none to compare (N306). */
    private suspend fun readPrevious(exerciseId: String): PreviousPerformance? =
        when (val read = workoutRepository.previousPerformance(exerciseId, sessionId)) {
            is DataResult.Success -> read.data.takeIf { it.sets.isNotEmpty() }
            is DataResult.Failure -> null
        }

    val uiState: StateFlow<WorkoutDetailUiState> =
        combine(
            session,
            exercises,
            sets,
            combine(lastError, templateName, previousByExercise) { error, name, previous ->
                Triple(error, name, previous)
            },
        ) { current, exerciseRows, logged, (error, name, previous) ->
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
                        declinedDirections = row.declinedDirections,
                        previous = previous[row.exerciseId]?.toHistoryPrevious(),
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
                                    bodyweightGrams = it.bodyweightGrams,
                                    durationSeconds = it.durationSeconds,
                                    isBodyweight = row.isBodyweight,
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
        durationSeconds: Int? = null,
    ) {
        // A new edit is a new answer (B350/B340): clear the previous one before writing so an equal
        // Written(setId) is not conflated and the editor's effect sees the result.
        _setWrite.value = null
        viewModelScope.launch {
            val result = workoutRepository.updateSet(
                setId = setId,
                reps = reps,
                weightGrams = weightGrams,
                rpeHalves = rpeHalves,
                note = note,
                setType = setType,
                assistanceGrams = assistanceGrams,
                durationSeconds = durationSeconds,
            )
            when (result) {
                is DataResult.Success -> {
                    lastError.value = null
                    _setWrite.value = WriteOutcome.Written(setId)
                }

                is DataResult.Failure -> {
                    lastError.value = result.error
                    _setWrite.value = WriteOutcome.Refused(setId, result.error)
                }
            }
        }
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

    /**
     * The day this workout was just moved to, for the screen to say (N267).
     *
     * Null until a correction lands and cleared once the sentence is read: the correction's whole effect is the
     * header's date, so it is announced rather than left to be noticed. Set by the write rather than derived
     * from the session, for [_rateVerdict]'s reason — a value read from the data would be said again on every
     * return to the screen.
     */
    private val _movedDay = MutableStateFlow<LocalDate?>(null)

    val movedDay: StateFlow<LocalDate?> = _movedDay.asStateFlow()

    /**
     * Corrects the day this workout happened on (ROADMAP N266, N267).
     *
     * The start is stamped when the app is opened, so a workout done without the phone lands on the day it was
     * written down rather than the day it happened — and that day is what the month grid, the week in review
     * and the adherence record read. The hour and the length are kept; a refusal is the repository's to say,
     * and it reaches the snackbar as an error while a success reaches it as the new day.
     */
    fun onMoveToDay(day: LocalDate) {
        viewModelScope.launch {
            when (val result = workoutRepository.moveSessionToDay(sessionId, day)) {
                is DataResult.Success -> {
                    lastError.value = null
                    _movedDay.value = day
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Called once the moved-to sentence has been read (N267). */
    fun onDismissMovedDay() {
        _movedDay.value = null
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
