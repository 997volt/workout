package com.example.androidapp.ui.workout

import java.time.ZoneId
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.example.androidapp.domain.model.comparePlanToActual
import com.example.androidapp.domain.model.PlannedSetSpec
import com.example.androidapp.domain.model.PlanComparison
import com.example.androidapp.domain.model.PerformedSetSpec
import com.example.androidapp.domain.model.ExercisePlan
import com.example.androidapp.domain.model.ExerciseActual
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.runContinuesAfter
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.ProgressionChoice
import com.example.androidapp.domain.model.pendingProgressionFor
import com.example.androidapp.domain.model.progressionPromptFor
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.taxonomySubtitle
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.navigation.ActiveWorkout
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A logged set as the screen shows it: position, weight, reps, and N6's extras. */
data class SetRow(
    val id: String,
    val number: Int,
    val reps: Int,
    val weightGrams: Long,
    /** 1–10, or null when none was recorded (ROADMAP N6). */
    val rpeHalves: Int? = null,
    /** A short comment on the set, or null (ROADMAP N6). */
    val note: String? = null,
    /** The role it was performed as (ROADMAP N14). */
    val setType: SetType = SetType.NORMAL,
    /** The machine's assistance, 0 for none (ROADMAP N15). */
    val assistanceGrams: Long = 0,
    /** The unit this exercise's numbers read in (ROADMAP N64). */
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
)

/** One exercise in the workout, with its sets and what the next set will prefill. */
data class SessionExerciseRow(
    val id: String,
    val exerciseId: String,
    val name: String,
    /** `Quads · Barbell`, or null while a custom exercise's taxonomy is unset. */
    val subtitle: String?,
    /** A cue to read while lifting, or null (ROADMAP N5). */
    val techniqueNote: String? = null,
    /**
     * The superset or circuit this exercise is performed in, or null (ROADMAP N24).
     *
     * Exercises sharing it are done in rounds, so the screen labels them A1/A2 and the rest
     * waits for the round to finish.
     */
    val supersetGroup: Int? = null,
    /** `A1`/`A2` for a grouped exercise — giant-set notation, or null (ROADMAP N24). */
    val supersetLabel: String? = null,
    /** This exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /**
     * The unit every number belonging to this exercise reads in (ROADMAP N64).
     *
     * The exercise's own override where it has one, the app setting otherwise, resolved once here
     * rather than at each of the places that show one of its weights.
     */
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
    /**
     * The plan's set indexes whose run carries on into the next row (ROADMAP N79).
     *
     * The rest is the group's, so it starts after the run's last rung rather than between its rungs —
     * and the decision is made while a set is being logged, where the plan is no longer in hand. Empty
     * for a plan with no runs in it, which is every plan before this and most after it.
     */
    val runContinuesAfter: Set<Int> = emptySet(),
    /**
     * How many sets the plan behind this workout writes for this exercise, or null when there is no
     * plan (ROADMAP N52).
     *
     * Null is not zero: an empty workout, or one whose exercise was added by hand, has nothing to
     * be done with, so the control never turns into *Log extra set* on a plan that does not exist.
     * A number here is the whole plan for the exercise — a program's slot and a template chosen
     * directly arrive as the same `templateId`, so both count.
     */
    val plannedSetCount: Int? = null,
    /**
     * True once this exercise has been marked done (ROADMAP N7): its Log set button
     * is hidden, its sets are dimmed and not editable, and Reopen restores both.
     */
    val isFinished: Boolean = false,
    /** How well the target muscle was worked, 1–10, or null (ROADMAP N8). */
    val muscleFeel: Int? = null,
    /**
     * The joints that hurt, each with its side and its own score (ROADMAP N63).
     *
     * The picked list a new rating writes; empty means none was picked.
     */
    val joints: List<JointPain> = emptyList(),
    /** Legacy joint pain, 1–10, or null (ROADMAP N8) — shown for a session rated before N63. */
    val jointPain: Int? = null,
    /** Legacy "which joints" free text, or null (ROADMAP N9) — shown, never rewritten. */
    val jointPainNote: String? = null,
    val sets: List<SetRow> = emptyList(),
    val suggestion: SetSuggestion = SetSuggestion(DEFAULT_REPS, Weight.DEFAULT_GRAMS),
    /**
     * What *Done* opens with (ROADMAP N50, N74): one row per working set — the plan's target for it,
     * what the session did, and the step those two earned.
     *
     * Empty rather than nullable, because *Done* still has something to say for an exercise with no
     * plan — the rating is behind the same prompt — and it says "no plan" rather than inventing one.
     */
    val progression: ProgressionPrompt = ProgressionPrompt(),
)

/**
 * The values that change every second (ROADMAP F16).
 *
 * Deliberately **separate** from [ActiveWorkoutUiState]. Folding a one-second
 * ticker into the screen's single state meant every tick produced a new state
 * object, and because that object also holds `List`s — which Compose cannot treat
 * as stable — the exercise list recomposed once a second along with everything
 * else. The clock is read only by the two small composables that display it, so
 * the list is off the per-second path entirely.
 */
data class WorkoutClock(
    val elapsed: String = "",
    val restSecondsRemaining: Int = 0,
) {
    /** One source of truth: "resting" is just "there is time left". */
    val isResting: Boolean get() = restSecondsRemaining > 0
}

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val sessionId: String? = null,
    /** Fixed for the life of the session, so it does not belong on the clock. */
    val startedAt: String = "",
    val exercises: List<SessionExerciseRow> = emptyList(),
    /** A just-deleted set awaiting undo; the screen shows it as a snackbar. */
    val pendingUndo: SetEntry? = null,
    /**
     * An exercise just marked done, awaiting undo (ROADMAP N7). The id is enough:
     * the row is still in [exercises], so the snackbar can name it.
     */
    val pendingFinishedExerciseId: String? = null,
    /** What was not recovered today, or null (ROADMAP N4). */
    val readinessNote: String? = null,
    /** The muscles it reported sore, each with its own score (ROADMAP N62). */
    val readinessSoreMuscles: List<SoreMuscle> = emptyList(),
    /** True while a just-opened session is asking for that note. */
    val isReadinessPromptVisible: Boolean = false,
    /** Set when a write failed, so the screen can say so instead of lying. */
    val error: DataError? = null,
) {
    /**
     * The deleted set the screen may offer to bring back, or null (ROADMAP B3).
     *
     * An undo is only offered while its subject is still in the session. The two undo
     * snackbars share one host, so without this a deleted set's Undo can outlive its
     * exercise: the row is gone, the button stays, and the write it fires is refused.
     * The rule lives here, named, rather than in the rendering — and the ViewModel
     * still reports if a tap races through before this turns null.
     */
    val undoableSet: SetEntry?
        get() = pendingUndo?.takeIf { set -> exercises.any { it.id == set.sessionExerciseId } }

    /** The Done undo, offered on the same terms (ROADMAP B3). */
    val undoableFinishedExerciseId: String?
        get() = pendingFinishedExerciseId?.takeIf { id -> exercises.any { it.id == id } }

    /** An open session with nothing in it — the state a user can abandon. */
    val isEmpty: Boolean get() = !isLoading && sessionId != null && exercises.isEmpty()

    /** No open session at all: finished, discarded, or never started. */
    val hasNoSession: Boolean get() = !isLoading && sessionId == null
}

/**
 * Owns the active workout: its exercises, their sets, and the rest timer
 * (ROADMAP P1.2, P1.3, P1.4, P1.8).
 *
 * The screen auto-starts a session on entry. That is deliberate: the whole point
 * of P1.8 is that the session exists in the database before anything is logged,
 * so a process death or a crash mid-set leaves a recoverable workout rather than
 * nothing. A session with no exercises is left for the user to discard rather
 * than cleaned up implicitly — an invisible delete is worse than a visible one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val timeSource: TimeSource,
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * Set when the workout was started from a template (ROADMAP N3).
     *
     * It only ever matters for the call that *opens* the session; the repository
     * seeds the exercises there, inside the same transaction. A resumed session
     * leaves this unused, which is what makes a template start idempotent.
     */
    private val templateId: String? = savedStateHandle.toRoute<ActiveWorkout>().templateId

    /**
     * The finished workout whose exercises this session opens with, or null (ROADMAP N29, N48).
     *
     * A start argument like the template: it is consumed once, when the session is opened, and a
     * resumed session ignores it.
     */
    private val repeatSessionId: String? = savedStateHandle.toRoute<ActiveWorkout>().repeatSessionId

    /**
     * The program slot this workout was started from, or null (ROADMAP P3.8).
     *
     * A start argument rather than a stored fact: the session still records only the template
     * (P3.3). Since N73 it matters once — the offer the lifter sees is made from *that slot's*
     * history rather than the exercise's — because the plan itself is the template's.
     */
    private val slotId: String? = savedStateHandle.toRoute<ActiveWorkout>().slotId

    /**
     * True when this workout was started from a program's slot (ROADMAP N41).
     *
     * The discard prompt says so, because only a finished session settles a scheduled occurrence
     * (P3.5): dropping out of a scheduled slot is recorded as a miss rather than as no workout at
     * all. A session started from an unscheduled template is not a program occurrence, so it gets
     * no such warning.
     */
    val startedFromProgram: Boolean get() = slotId != null

    /**
     * The plan this workout was started from, or empty (ROADMAP N14).
     *
     * Nothing is stored on the session to link it: the route already carries the
     * template, and the back stack keeps it across process death, so the plan stays a
     * plan — editing it changes what the next workout prefills, which is what N16
     * calls a living template.
     */
    private val plannedExercises: Flow<List<TemplateExercise>> =
        templateId?.let { templateRepository.observeExercises(it) } ?: flowOf(emptyList())

    private val lastError = MutableStateFlow<DataError?>(null)

    /**
     * The review of the workout that just finished, published instead of closing the
     * screen outright (ROADMAP N20).
     *
     * Finish used to be a dead end: the ratings, the readiness note and the totals went
     * nowhere, and a plan was never compared with what was actually lifted. The summary is
     * built from state this screen already holds, so it costs no reads and needs no schema
     * — but it also means it is a *moment*, not something to come back to.
     */
    /**
     * The app-wide default rest, kept current while the screen is open (ROADMAP N21).
     *
     * Collected rather than read once: a change made in settings must reach a workout that
     * is already running, which is the reason the store exposes a `Flow` at all.
     */
    /**
     * The app-wide default rest, kept current while the screen is open (ROADMAP N21).
     *
     * Collected rather than read once: a change made in settings must reach a workout that is
     * already running, which is the reason the store exposes a `Flow` at all. Public since N44:
     * with the countdown off the screen shows what an exercise prescribes, falling back to this.
     */
    val defaultRestSeconds = settingsRepository.observeDefaultRestSeconds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, RestTimer.DEFAULT_SECONDS)

    /**
     * Whether the rest between sets is counted down at all (ROADMAP N44).
     *
     * Read from settings rather than held as its own state: turning it off must stop a rest that is
     * already running, not only the next one.
     */
    val restTimerEnabled: StateFlow<Boolean> = settingsRepository.observeRestTimerEnabled()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /**
     * Whether *Done* asks about the next step a plan earned (ROADMAP N66).
     *
     * Read from settings rather than held as its own state: the answer is the app's, and this reads it
     * at the moment Done is tapped (N74 moved that decision here from the screen). On by default, so
     * an upgrade changes nothing.
     */
    private val progressionPromptEnabled: StateFlow<Boolean> =
        settingsRepository.observeProgressionPromptEnabled()
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /**
     * The record just set, if the last logged set was one (ROADMAP N23).
     *
     * Not dismissible on purpose: it clears when the next set is logged, which is when the
     * news is stale — a banner that needs dismissing is a banner in the way between sets.
     */
    /**
     * Whether a finished rest is heard as well as seen (ROADMAP N27).
     *
     * Read from settings rather than held as its own state: the flag belongs to the app, and the
     * screen only needs the current answer.
     */
    val restCueEnabled: StateFlow<Boolean> = settingsRepository.observeRestCueEnabled()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Whether the screen should stay awake while this workout is open (ROADMAP N27). */
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.observeKeepScreenOn()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _personalRecord = MutableStateFlow<PersonalRecordMoment?>(null)
    val personalRecord: StateFlow<PersonalRecordMoment?> = _personalRecord.asStateFlow()

    private val _summary = MutableStateFlow<WorkoutReview?>(null)
    val summary: StateFlow<WorkoutReview?> = _summary.asStateFlow()

    private val pendingUndo = MutableStateFlow<SetEntry?>(null)
    private val previousByExercise = MutableStateFlow<Map<String, PreviousPerformance>>(emptyMap())

    /**
     * The app-wide unit, for the one place that needs it without a row in hand: the review's
     * planned side, which comes from the template rather than from the session (ROADMAP N64).
     */
    private val appWeightUnit: StateFlow<WeightUnit> = settingsRepository.observeWeightUnit()
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KILOGRAMS)

    /** True while a just-opened session is asking what was not recovered today (N4). */
    private val readinessPromptVisible = MutableStateFlow(false)

    /** The exercise just marked done, awaiting the snackbar's undo (ROADMAP N7). */
    private val pendingFinishedExercise = MutableStateFlow<String?>(null)

    /**
     * The progression question a *Done* froze, or null while none is open (ROADMAP N74).
     *
     * Held rather than derived, and it has to be: the offer is computed from the plan's target and the
     * session's work, so the first step written would re-arm the same set's offer on the next read.
     * Holding it is also what lets a pick survive a rotation mid-answer, since a local `remember`
     * would not.
     */
    private val _pendingProgression = MutableStateFlow<PendingProgression?>(null)
    val pendingProgression: StateFlow<PendingProgression?> = _pendingProgression.asStateFlow()

    /** Emits true once a finish or discard succeeds, so the screen can leave. */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed

    /**
     * Shared so the session is not queried once per downstream flow.
     */
    private val activeSession = workoutRepository.observeActiveSession()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private val sessionExercises: Flow<List<SessionExercise>> = activeSession
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else workoutRepository.observeSessionExercises(session.id)
        }

    /**
     * Shared as state so the ViewModel can look a set up synchronously. That is
     * what lets delete/undo be expressed by id, instead of handing the screen a
     * domain object it has no other use for.
     */
    private val setsState: StateFlow<List<SetEntry>> = activeSession
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else workoutRepository.observeSets(session.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** The three session-shaped sources, kept together so the combine below stays three deep. */
    private data class SessionPart(
        val session: WorkoutSession?,
        val exercises: List<SessionExercise>,
        val sets: List<SetEntry>,
    )

    /** What the plan says and what history answers (ROADMAP N73: the plan is the template's). */
    private data class PlanPart(
        val planned: List<TemplateExercise>,
        val previous: Map<String, PreviousPerformance>,
        /** The app-wide unit, so a change to it re-resolves every row's own (ROADMAP N64). */
        val unit: WeightUnit,
    )

    private val snapshots: Flow<Snapshot> = combine(
        combine(activeSession, sessionExercises, setsState) { session, exercises, logged ->
            SessionPart(session, exercises, logged)
        },
        combine(previousByExercise, plannedExercises, settingsRepository.observeWeightUnit()) {
                previous,
                planned,
                unit,
            ->
            PlanPart(planned, previous, unit)
        },
    ) { session, plan ->
        Snapshot(
            session = session.session,
            exercises = session.exercises,
            sets = session.sets,
            previous = plan.previous,
            planned = plan.planned,
            unit = plan.unit,
        )
    }

    /** Drives both the elapsed clock and the rest countdown; stops when unsubscribed. */
    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        snapshots,
        lastError,
        pendingUndo,
        readinessPromptVisible,
        pendingFinishedExercise,
    ) { snapshot, error, undo, promptVisible, finished ->
        snapshot.toUiState(
            error = error,
            undo = undo,
            readinessPromptVisible = promptVisible,
            pendingFinishedExerciseId = finished,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ActiveWorkoutUiState(),
    )

    /**
     * Ticks once a second.
     *
     * Nothing in the state above depends on it, so a tick can only recompose the
     * two composables that read this — the elapsed header and the rest bar.
     */
    val clock: StateFlow<WorkoutClock> = combine(activeSession, ticker) { session, _ ->
        val now = timeSource.now()
        WorkoutClock(
            elapsed = session
                ?.let { WorkoutFormat.elapsed(Duration.between(it.startedAt, now)) }
                .orEmpty(),
            restSecondsRemaining = RestTimer.remainingSeconds(session?.restEndsAt, now),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WorkoutClock(),
    )

    init {
        viewModelScope.launch {
            // Repeating is the same open, with one finished workout's exercises as the seed instead
            // of a plan's — both go through the one append path (ROADMAP N3, N29, N48). The template
            // is the whole plan, so nothing else travels with it (N73).
            val opened =
                if (repeatSessionId != null) {
                    workoutRepository.repeatSession(repeatSessionId)
                } else {
                    workoutRepository.startOrResumeSession(templateId)
                }
            when (val result = opened) {
                is DataResult.Success -> {
                    lastError.value = null
                    // Only a freshly opened session asks (ROADMAP N4). A resumed one
                    // has already had its chance, and re-asking after a process death
                    // would be nagging rather than prompting.
                    readinessPromptVisible.value = result.data.isNew
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }

        // "Off" means the timer is genuinely not running (N44): a rest already counted down is
        // cleared when the switch goes off, rather than left ticking behind a static label.
        viewModelScope.launch {
            restTimerEnabled.collect { enabled ->
                if (!enabled) workoutRepository.clearRest()
            }
        }

        // Load each exercise's previous performance once, when it first appears.
        // Doing it here rather than per recomposition keeps the next-set prefill
        // from re-querying on every tick of the elapsed clock.
        viewModelScope.launch {
            sessionExercises.collect { exercises ->
                val sessionId = activeSession.replayCache.firstOrNull()?.id ?: return@collect
                exercises
                    .map { it.exerciseId }
                    .distinct()
                    .filterNot { it in previousByExercise.value }
                    .forEach { exerciseId ->
                        // A slot's own history where there is a slot (P3.8), so two slots naming
                        // one template progress apart; otherwise the exercise's last session.
                        val result = slotId?.let { slot ->
                            programRepository.slotPreviousPerformance(
                                slotId = slot,
                                exerciseId = exerciseId,
                                currentSessionId = sessionId,
                                // Read when it is needed rather than captured (B45).
                                zone = ZoneId.systemDefault(),
                            )
                        } ?: workoutRepository.previousPerformance(exerciseId, sessionId)
                        if (result is DataResult.Success) {
                            previousByExercise.update { it + (exerciseId to result.data) }
                        }
                    }
            }
        }

    }

    fun onAddExercise(exerciseId: String) = write { sessionId ->
        workoutRepository.addExercise(sessionId, exerciseId)
    }

    fun onRemoveExercise(sessionExerciseId: String) = write {
        workoutRepository.removeExercise(sessionExerciseId)
    }

    /**
     * Moves one exercise one place in the session's own order (ROADMAP N54).
     *
     * Order matters mid-session — a rack taken, equipment moved — and the only way to change it used
     * to be editing the template, which rewrote every future run for a reason that belonged to one
     * afternoon. [delta] is -1 for up and +1 for down, the template editor's own gesture; the template
     * is never written, which is N16's "a session reads it at the start" applied to order.
     */
    fun onMoveExercise(sessionExerciseId: String, delta: Int) = write {
        workoutRepository.moveExercise(sessionExerciseId, delta)
    }

    /**
     * Closes an exercise (ROADMAP N7, N50, N74).
     *
     * With no question open this is the *Done* button, and where a plan can answer it that **opens
     * the question instead of closing the exercise** (N50, N74). The question is frozen here rather
     * than read back later: the offer is computed from the plan's target and the session's work, so
     * writing one step would re-arm the same set's offer on the next read. The setting can withdraw
     * the question entirely (N66), and an exercise with no plan never has one to ask.
     *
     * With a question already open for this exercise it is *Not now*: declining is a finish, not a
     * nudge, so the question is dropped and nothing is written (N50). Committing is
     * [onConfirmProgression] because it has work to do before the finish.
     *
     * The ratings used to ride in here (N8): *Done* opened *How did that feel?* and the answer was
     * written on the way to finishing, so it is written by [onRateExercise] instead — with nothing
     * written if the lifter never rated it.
     */
    fun onFinishExercise(sessionExerciseId: String) {
        val questionOpen = _pendingProgression.value?.sessionExerciseId == sessionExerciseId
        val row = uiState.value.exercises.firstOrNull { it.id == sessionExerciseId }
        when {
            questionOpen -> {
                _pendingProgression.value = null
                viewModelScope.launch { finishExercise(sessionExerciseId) }
            }

            progressionPromptEnabled.value && row?.progression?.hasPlan == true ->
                _pendingProgression.value = pendingProgressionFor(
                    sessionExerciseId = sessionExerciseId,
                    exerciseName = row.name,
                    unit = row.weightUnit,
                    prompt = row.progression,
                )

            else -> viewModelScope.launch { finishExercise(sessionExerciseId) }
        }
    }

    /**
     * Picks a step on the frozen question (ROADMAP N74).
     *
     * [setId] names one set; null means every set that offers [direction], which is the plan whose
     * sets share a target and would otherwise ask for the same tap once per set. A pick is not a
     * write: nothing reaches the plan until [onConfirmProgression], which is what lets a lifter read
     * the whole answer before taking it.
     */
    fun onSelectProgression(setId: String?, direction: ProgressionDirection) {
        _pendingProgression.update { pending ->
            pending?.let { if (setId == null) it.chooseAll(direction) else it.toggle(setId, direction) }
        }
    }

    /**
     * Writes every step the lifter picked, then finishes the exercise (ROADMAP N50, N74).
     *
     * **The plan is what changes**: the session's record already says what was done, and N16's living
     * template is what the next run reads (N73). The writes go one planned set at a time, and a
     * failure stops the run with the error on screen and the exercise still open — finishing anyway
     * would say every step was taken. What was already written is marked on the frozen question, so a
     * retry writes only what is left.
     */
    fun onConfirmProgression() {
        val pending = _pendingProgression.value ?: return
        viewModelScope.launch {
            val failure = writeProgressionSteps(pending.chosen, templateRepository) { accepted ->
                _pendingProgression.update { it?.applied(accepted.setId, accepted) }
            }
            if (failure != null) {
                lastError.value = failure
                return@launch
            }
            _pendingProgression.value = null
            finishExercise(pending.sessionExerciseId)
        }
    }

    /** Closes one exercise and offers the undo every finish offers (ROADMAP N7). */
    private suspend fun finishExercise(sessionExerciseId: String) {
        when (val result = workoutRepository.finishExercise(sessionExerciseId)) {
            is DataResult.Success -> {
                lastError.value = null
                pendingFinishedExercise.value = sessionExerciseId
            }

            is DataResult.Failure -> lastError.value = result.error
        }
    }

    /**
     * Puts a just-finished exercise back into edit, from the snackbar (N7).
     *
     * Checks its subject first (ROADMAP B3): the snackbar can outlive the exercise
     * it refers to, and a doomed write would report `NotFound` about a row the user
     * never asked about.
     */

    fun onDismissFinishUndo() {
        pendingFinishedExercise.value = null
    }

    /**
     * Writes how an exercise felt, at any time (ROADMAP N10).
     *
     * The exercise's own rating row is the **only** way in: N50 took the rating off the Done path, so
     * this is reached when the lifter reaches for it rather than handed over on the way out. The
     * rating stays editable for as long as the session is.
     */
    fun onRateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<JointPain>,
    ) {
        viewModelScope.launch {
            handle(
                workoutRepository.rateExercise(
                    sessionExerciseId = sessionExerciseId,
                    muscleFeel = muscleFeel,
                    joints = joints,
                ),
            )
        }
    }

    /** Reopens a done exercise from its own button (N7). */
    /**
     * Puts a done exercise back into edit (N7): from its own button, or from the
     * snackbar's undo, which is why both go through here rather than through two
     * paths that could drift apart.
     */
    fun onReopenExercise(sessionExerciseId: String) {
        // Reopening *is* the undo, so the offer goes with it: the state already answers
        // "is it still in the session?", which is why the separate wrapper for the banner
        // was a second name for one operation.
        pendingFinishedExercise.value = null
        // Reopening something that is gone — undone twice, or removed meanwhile — is reported
        // rather than silently ignored: the guard that used to sit in the undo wrapper is this
        // question, asked where the action now is.
        if (!uiState.value.hasLiveExercise(sessionExerciseId)) {
            lastError.value = GONE_FROM_SESSION
            return
        }
        viewModelScope.launch {
            when (val result = workoutRepository.reopenExercise(sessionExerciseId)) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }


    /**
     * Logs one set, exactly as the fields that stated it say (ROADMAP N59, restoring B7).
     *
     * N51 made logging *be* the editor and left a set that differed from the prefill to be corrected
     * afterwards; N59 reversed that, because the next set's values are fields on the exercise block
     * now. So this takes what those fields hold rather than what the row happened to offer: the button
     * beside them writes the set on screen, and the editor is what correcting an already-logged set
     * opens. The role travels in the same value (N19, N14) — a set is what it was performed as, and
     * one field carries that.
     */
    fun onLogSet(sessionExerciseId: String, edit: SetEdit) {
        val row = uiState.value.exercises.firstOrNull { it.id == sessionExerciseId } ?: return
        viewModelScope.launch {
            // Read *before* the set is written, and excluding this session (ROADMAP N23): a
            // record is about beating history, and the set being logged is not history yet.
            // A failed read means no claim either way rather than a missed record.
            val records = when (
                val read = workoutRepository.personalRecords(
                    exerciseId = row.exerciseId,
                    excludingSessionId = uiState.value.sessionId,
                )
            ) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> null
            }
            // History *and* what this session has already logged: without the second half the
            // same record is announced on every set that repeats it (found by a test).
            val against = records?.mergedWith(
                PersonalRecords.from(
                    row.sets.map {
                        PerformedSetSpec(
                            role = it.setType,
                            weightGrams = it.weightGrams,
                            assistanceGrams = it.assistanceGrams,
                            reps = it.reps,
                        )
                    },
                ),
            )
            _personalRecord.value = null

            val result = workoutRepository.logSet(
                sessionExerciseId = sessionExerciseId,
                reps = edit.reps,
                weightGrams = edit.weightGrams,
                // What the fields hold is what *Log set* writes (N59, N6). Leaving these out is what
                // made every inline set come back with no effort recorded.
                rpeHalves = edit.rpeHalves,
                note = edit.note,
                setType = edit.setType,
                assistanceGrams = edit.assistanceGrams,
            )
            handle(result)
            if (result is DataResult.Success) {
                // A record noticed a week later in a list is a record nobody feels (N23).
                // The role goes in, or a heavy warm-up raises a best (B17); and what it beat is
                // read from the *merged* view, because the bar may have been set earlier in this
                // same session — saying "the first time at this rep count" then would be false
                // while claiming a record over that very set (B18).
                if (
                    against != null &&
                    against.isRecord(edit.reps, edit.weightGrams, edit.setType)
                ) {
                    _personalRecord.value = PersonalRecordMoment(
                        exerciseName = row.name,
                        reps = edit.reps,
                        weightGrams = edit.weightGrams,
                        previousBestGrams = against.bestAt(edit.reps),
                        weightUnit = row.weightUnit,
                    )
                }

                // ROADMAP N24: in a superset the rest belongs to the round, not the set, so
                // it waits until nothing else in the group is behind. Resting here would
                // defeat the pairing the user just asked for.
                // And the group's own rest waits for the run to close (N79): a rung has no rest of its
                // own between it and the next, because the point of a drop or a cluster is that the
                // sets follow each other.
                val runContinues = row.sets.size in row.runContinuesAfter
                if (!runContinues && uiState.value.roundIsCompleteFor(row)) {
                    // The group's own rest wins in a superset, then this exercise's, then the
                    // app setting (ROADMAP N5, N21, B15). Taking the rest from whichever member
                    // happened to close the round made the wait depend on the order the user
                    // logged in — the same round resting differently every time round — and the
                    // settled rule is the group's longest, so the pair is paced by its slowest
                    // member.
                    val rest = uiState.value.longestRestInRound(row)
                        ?: row.restSeconds
                        ?: defaultRestSeconds.value
                    // Off means the timer is genuinely not running (N44): the end instant is simply
                    // never written, and the screen shows the prescription as a fixed label instead.
                    if (restTimerEnabled.value) startRest(rest)
                }
            }
        }
    }

    /**
     * Joins this exercise to the one above it in a superset, or leaves the group (N24).
     *
     * Pairing with the previous exercise is the only gesture that needs no new screen: the
     * order is already on screen, and "these two, together" is what a tap on the second one
     * means.
     */
    fun onToggleSuperset(sessionExerciseId: String) {
        val ordered = uiState.value.exercises
        val index = ordered.indexOfFirst { it.id == sessionExerciseId }
        if (index < 0) return
        val me = ordered[index]
        val previous = ordered.getOrNull(index - 1)

        val group = when {
            // Already grouped: leaving takes the whole group apart, because a group of one is
            // not a group and half a superset is a state nobody asked for.
            me.supersetGroup != null -> null
            previous?.supersetGroup != null -> previous.supersetGroup
            previous != null -> (ordered.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1
            else -> null
        }

        viewModelScope.launch {
            // Leaving takes the whole group apart, because a group of one is not a group and
            // half a superset is a state nobody asked for; joining pulls the exercise above in,
            // because a superset is at least two exercises.
            val changed = if (group == null) {
                ordered.filter { it.supersetGroup == me.supersetGroup }.map { it.id }
            } else {
                listOf(sessionExerciseId) + listOfNotNull(previous?.takeIf { it.supersetGroup == null }?.id)
            }
            // One write for the whole group: a partial one would leave half a superset, which is
            // the state this action exists to avoid (ROADMAP B27).
            handle(workoutRepository.setSupersetGroup(changed, group))
        }
    }

    fun onUpdateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int? = null,
        note: String? = null,
        setType: SetType = SetType.NORMAL,
        assistanceGrams: Long = 0,
    ) {
        viewModelScope.launch {
            handle(
                workoutRepository.updateSet(
                    setId = setId,
                    reps = reps,
                    weightGrams = weightGrams,
                    rpeHalves = rpeHalves,
                    note = note,
                    setType = setType,
                    assistanceGrams = assistanceGrams,
                ),
            )
        }
    }

    fun onDeleteSet(setId: String) {
        val set = setsState.value.firstOrNull { it.id == setId } ?: return
        viewModelScope.launch {
            when (val result = workoutRepository.deleteSet(set.id)) {
                is DataResult.Success -> pendingUndo.value = set
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /**
     * Restores a deleted set.
     *
     * Re-logging appends it rather than restoring its original index — the values
     * come back, the position may not. That is the honest limit of an undo backed
     * by a soft delete, and better than pretending the row was never touched.
     */
    fun onUndoDelete() {
        val set = pendingUndo.value ?: return
        pendingUndo.value = null
        // The reason this reports rather than no-ops (ROADMAP B3): the set can only
        // come back onto a live exercise, and the repository's loggable guard would
        // otherwise refuse the write and leave the user with nothing on screen —
        // the reported bug was exactly this, with the Undo still visible.
        if (!uiState.value.hasLiveExercise(set.sessionExerciseId)) {
            lastError.value = GONE_FROM_SESSION
            return
        }
        viewModelScope.launch {
            handle(
                workoutRepository.logSet(
                    sessionExerciseId = set.sessionExerciseId,
                    reps = set.reps,
                    weightGrams = set.weightGrams,
                    // An undo puts back the set that was deleted, help included (B7) — and what the row
                    // said about itself, which is its RPE and comment (N6).
                    rpeHalves = set.rpeHalves,
                    note = set.note,
                    setType = set.setType,
                    assistanceGrams = set.assistanceGrams,
                ),
            )
        }
    }

    fun onDismissUndo() {
        pendingUndo.value = null
    }

    /**
     * Writes the readiness (ROADMAP N4, N62), from the prompt a new session opens with or from the
     * workout header afterwards. A null or blank note clears it; the sore-muscle list replaces
     * whatever was stored, so an empty one clears it.
     */
    fun onSaveReadinessNote(note: String?, soreMuscles: List<SoreMuscle>) {
        val sessionId = uiState.value.sessionId ?: return
        viewModelScope.launch {
            when (val result = workoutRepository.setReadiness(sessionId, note, soreMuscles)) {
                is DataResult.Success -> {
                    lastError.value = null
                    readinessPromptVisible.value = false
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /**
     * Skips the prompt without writing anything. The header keeps offering the
     * field, so skipping is not a dead end.
     */
    fun onDismissReadinessPrompt() {
        readinessPromptVisible.value = false
    }

    fun onSkipRest() {
        viewModelScope.launch {
            handle(workoutRepository.clearRest())
        }
    }

    fun onAdjustRest(deltaSeconds: Int) {
        viewModelScope.launch {
            // No scheduling branch: the rest is in-app only now (ROADMAP N26), so a success has
            // nothing left to do beyond the end instant the repository just wrote.
            handle(workoutRepository.adjustRest(deltaSeconds))
        }
    }

    /**
     * Finishes the workout, with the comment the prompt collected (ROADMAP N11).
     *
     * A null or blank [note] writes nothing at all, so *Skip* and *Save* on an empty
     * field are the same thing — there is no second representation of "no comment"
     * to get out of step.
     */
    fun onFinish(note: String? = null) {
        viewModelScope.launch {
            val sessionId = uiState.value.sessionId
            if (sessionId == null) {
                lastError.value = DataError.NotFound
                return@launch
            }
            if (!note.isNullOrBlank()) {
                val written = workoutRepository.setWorkoutNotes(sessionId, note)
                if (written is DataResult.Failure) {
                    // The comment is not worth losing the workout over, but it must
                    // not disappear silently either.
                    lastError.value = written.error
                    return@launch
                }
            }
            // Captured *before* finishing, and this is the bug a device found: the state
            // describes the *live* session, so the moment the workout is stored its
            // exercises and sets leave the flow — a review built afterwards read
            // "Sets 0 · reps 0 · 0 kg" and called a set that was just logged "not
            // performed". The review is about work that exists only until this call
            // returns, so it is taken here.
            val finishedState = uiState.value
            val plan = plannedExercises.first()

            when (val result = workoutRepository.finishSession(sessionId)) {
                is DataResult.Success -> {
                    lastError.value = null
                    // The workout is over and stored; the review is what the user sees
                    // next, and dismissing it is what closes the screen (N20).
                    _summary.value = buildSummary(finishedState, plan, note, appWeightUnit.value)
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Dismisses the review, which is what actually closes the screen (ROADMAP N20). */
    fun onDismissSummary() {
        _summary.value = null
        closeSession()
    }


    /**
     * What "this workout is over" means, in one place.
     *
     * Every path that closes the screen goes through here. It used to have a second job — cancelling the
     * background rest alert — and the history is kept only as a warning: N11 rewrote `onFinish` and
     * skipped this call, which was silent until a test caught it. The alert is gone (ROADMAP N26), so
     * what remains is the closing, and one path that forgets it still strands the user on a finished
     * workout.
     */
    private fun closeSession() {
        _closed.value = true
    }

    fun onDiscard() = write(closeAfterwards = true) { sessionId ->
        workoutRepository.deleteSession(sessionId)
    }

    private suspend fun startRest(seconds: Int) {
        handle(workoutRepository.startRest(seconds))
    }

    /** True while the exercise is still part of the open session's list. */
    private fun handle(result: DataResult<*>) {
        when (result) {
            is DataResult.Success -> lastError.value = null
            is DataResult.Failure -> lastError.value = result.error
        }
    }

    /**
     * Runs [block] against the current session and records the failure, if any.
     *
     * The error is kept in state rather than thrown: a dropped write must be
     * visible to the user, and an exception inside a coroutine would not be.
     */
    private fun write(
        closeAfterwards: Boolean = false,
        block: suspend (String) -> DataResult<Unit>,
    ) {
        val sessionId = uiState.value.sessionId ?: return
        viewModelScope.launch {
            when (val result = block(sessionId)) {
                is DataResult.Success -> {
                    lastError.value = null
                    if (closeAfterwards) closeSession()
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    private companion object {
        /**
         * Reported when an undo's subject has gone. `Invalid` because it is the only
         * error that carries a sentence written for the user, which is what this is
         * — the one case where no write should be attempted at all.
         */
        val GONE_FROM_SESSION = DataError.Invalid("That exercise is no longer in this workout.")

        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TICK_MILLIS = 1_000L
    }
}

/**
 * Everything that describes the workout's contents.
 *
 * File-level with [toUiState] rather than nested in the ViewModel: the two are one translation of the
 * session's flows into screen state, and the class sits at the function ceiling detekt enforces.
 */
private data class Snapshot(
    val session: WorkoutSession?,
    val exercises: List<SessionExercise>,
    val sets: List<SetEntry>,
    val previous: Map<String, PreviousPerformance>,
    val planned: List<TemplateExercise>,
    /** The app-wide unit, so a change to it re-resolves every row's own (ROADMAP N64). */
    val unit: WeightUnit,
)

/**
 * The screen's state, translated from the snapshot the session's flows combine into.
 *
 * A file-level extension rather than a member: the class sits at the function ceiling detekt
 * enforces, and this only translates the state it is given — it reads nothing from the ViewModel and
 * writes nothing back, so it does not belong to it.
 */
private fun Snapshot.toUiState(
    error: DataError?,
    undo: SetEntry?,
    readinessPromptVisible: Boolean,
    pendingFinishedExerciseId: String?,
): ActiveWorkoutUiState {
    // Which plan entry each row follows is resolved once, by movement (ROADMAP N54), rather than per
    // row from the slot it happens to occupy.
    val planEntries = planEntriesFor(exercises, planned)
    return ActiveWorkoutUiState(
        isLoading = false,
        sessionId = session?.id,
        startedAt = session?.let {
            WorkoutFormat.clockTime(it.startedAt, zone = it.zoneIdOrNull() ?: ZoneId.systemDefault())
        }.orEmpty(),
        exercises = exercises.map {
            it.toRow(
                sets = sets,
                previous = previous[it.exerciseId],
                // The plan is the template's, whole (ROADMAP N73), read in this exercise's own unit
                // where it has one (N64).
                plan = PlanContext(
                    plannedEntry = planEntries[it.id],
                    unit = it.weightUnit ?: unit,
                ),
                supersetLabels = supersetLabelsFor(exercises),
            )
        },
        pendingUndo = undo,
        pendingFinishedExerciseId = pendingFinishedExerciseId,
        readinessNote = session?.readinessNote,
        readinessSoreMuscles = session?.soreMuscles.orEmpty(),
        isReadinessPromptVisible = readinessPromptVisible,
        error = error,
    )
}

/** What the screen shows once a workout is finished (ROADMAP N20). */
data class WorkoutReview(
    val note: String?,
    val readinessNote: String?,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeGrams: Long,
    /** Only the exercises that were rated — an unrated one is not a zero. */
    val ratings: List<ExerciseRating>,
    /** The plan next to the performance; empty when the workout came from no plan. */
    val comparisons: List<PlanComparison>,
)

/** How one exercise felt, as the summary reads it back. */
data class ExerciseRating(
    val name: String,
    val muscleFeel: Int?,
    /** The joints that hurt, each with its side and score (ROADMAP N63), or empty. */
    val joints: List<JointPain>,
    /** Legacy joint pain, 1–10, or null (ROADMAP N8) — read for a session rated before N63. */
    val jointPain: Int?,
)

/**
 * The review, assembled from what is already in memory (ROADMAP N20).
 *
 * A file-level function rather than a member: the class is at the function ceiling detekt
 * enforces, and this only translates the state it is given into a value — it reads nothing
 * from the ViewModel and writes nothing back, so it does not belong to it.
 */
private fun buildSummary(
    state: ActiveWorkoutUiState,
    plan: List<TemplateExercise>,
    note: String?,
    appUnit: WeightUnit,
): WorkoutReview {
    val actual = state.exercises.map { row ->
        ExerciseActual(
            exerciseId = row.exerciseId,
            name = row.name,
            // The row already carries its exercise's effective unit (N64).
            weightUnit = row.weightUnit,
            sets = row.sets.map {
                PerformedSetSpec(
                    role = it.setType,
                    weightGrams = it.weightGrams,
                    assistanceGrams = it.assistanceGrams,
                    reps = it.reps,
                )
            },
        )
    }
    val planned = plan.map { plannedExercise ->
        ExercisePlan(
            exerciseId = plannedExercise.exerciseId,
            name = plannedExercise.exerciseName,
            weightUnit = plannedExercise.weightUnit ?: appUnit,
            sets = plannedExercise.sets.map {
                PlannedSetSpec(
                    role = it.role,
                    weightGrams = it.targetWeightGrams,
                    assistanceGrams = it.targetAssistanceGrams,
                    minReps = it.targetRepsMin,
                    maxReps = it.targetRepsMax,
                )
            },
        )
    }
    val allSets = state.exercises.flatMap { it.sets }
    return WorkoutReview(
        note = note?.takeIf { it.isNotBlank() },
        readinessNote = state.readinessNote,
        totalSets = allSets.size,
        totalReps = allSets.sumOf { it.reps },
        // The same definition the history and the database use (ROADMAP B10): added
        // weight times reps, with assistance and bodyweight contributing zero.
        totalVolumeGrams = allSets.sumOf { it.weightGrams * it.reps },
        ratings = state.exercises.mapNotNull { row ->
            val feel = row.muscleFeel
            // The picked joints are the new half (N63); the legacy number is read so an exercise
            // rated before the change is still a rating rather than an unrated one.
            if (feel == null && row.joints.isEmpty() && row.jointPain == null) {
                null
            } else {
                ExerciseRating(
                    name = row.name,
                    muscleFeel = feel,
                    joints = row.joints,
                    jointPain = row.jointPain,
                )
            }
        },
        comparisons = comparePlanToActual(planned = planned, performed = actual),
    )
}

/**
 * True while this exercise is still part of the session (ROADMAP N7's undo).
 *
 * A file-level extension rather than a member: the class sits at the function ceiling
 * detekt enforces, and this is a question about the state, not about the ViewModel.
 */
private fun ActiveWorkoutUiState.hasLiveExercise(sessionExerciseId: String): Boolean =
    exercises.any { it.id == sessionExerciseId }

/**
 * True when the round this exercise is part of has nothing left behind it (ROADMAP N24).
 *
 * A grouped exercise only rests once every other member of its group has logged at least as
 * many sets, or has been marked done — a member that is finished is not going to catch up, and
 * waiting for it would leave the user resting between the two exercises the grouping exists to
 * pair. An ungrouped exercise is always its own complete round, which is the old behaviour.
 */
private fun ActiveWorkoutUiState.roundIsCompleteFor(row: SessionExerciseRow): Boolean {
    // `row` was captured before the write, and this state may not have caught up with the write
    // either — so the count comes from what this action actually logged rather than from a read
    // that can be one short. Both ways of trusting the read were live bugs: the round looked
    // finished, so the app rested between the two exercises the user had just paired.
    val logged = row.sets.size + 1

    return row.supersetGroup == null || exercises
        .filter { it.supersetGroup == row.supersetGroup && it.id != row.id }
        .all { it.isFinished || it.sets.size >= logged }
}

/**
 * What the plan says for one exercise: the template's entry resolved to it (ROADMAP N14, N73).
 *
 * One field since N73. A slot's prescription used to sit beside it, merging set by set for the
 * prefill, the plan count and the progression rule; the program is a schedule over the template now,
 * so there is nothing to merge.
 */
private data class PlanContext(
    val plannedEntry: TemplateExercise?,
    /** The unit this exercise's plan is read in: its own override, or the app setting (N64). */
    val unit: WeightUnit = WeightUnit.KILOGRAMS,
)

/**
 * How many sets the plan behind this workout writes for this exercise, or null (ROADMAP N52).
 *
 * No plan at all is null rather than zero, so an empty workout's control never says the work is
 * finished before it has started.
 */
private val PlanContext.plannedSetCount: Int?
    get() = plannedEntry?.sets?.size?.takeIf { it > 0 }

/**
 * The plan entry each session exercise follows, keyed by the session exercise's own id (ROADMAP N54).
 *
 * Matched by **movement and occurrence** rather than by position. A session's order is its own once
 * N54 lets it be edited, so the slot a row occupies says nothing about which plan entry it came from,
 * and matching on position is what pointed a moved exercise at its neighbour's targets. The movement
 * is the stable half; the occurrence count keeps a plan that names the same movement twice working,
 * with the first row of that movement following the plan's first entry and the second its second. A
 * row the plan has nothing for — added by hand, or the extra row of a movement the plan names once —
 * is simply absent, which is what leaves an improvised workout with no plan at all.
 *
 * File-level and pure, so the pairing can be tested without a database or a ViewModel: the shape
 * `todaysPlanFor` and `nextUpFor` already use.
 */
internal fun planEntriesFor(
    exercises: List<SessionExercise>,
    planned: List<TemplateExercise>,
): Map<String, TemplateExercise> {
    if (planned.isEmpty()) return emptyMap()
    val byMovement = planned.groupBy { it.exerciseId }
    val taken = mutableMapOf<String, Int>()
    return buildMap {
        exercises.forEach { exercise ->
            val occurrence = taken.getOrDefault(exercise.exerciseId, 0)
            taken[exercise.exerciseId] = occurrence + 1
            byMovement[exercise.exerciseId]?.getOrNull(occurrence)?.let { put(exercise.id, it) }
        }
    }
}

private fun SessionExercise.toRow(
    sets: List<SetEntry>,
    previous: PreviousPerformance?,
    plan: PlanContext,
    supersetLabels: Map<String, String>,
): SessionExerciseRow {
    // Every set this exercise logged reads in the exercise's own unit (ROADMAP N64), and its ±
    // buttons move by the step the movement loads in (N77).
    val loggedSets = sets.loggedRowsFor(id, stepGrams)
        .map { it.copy(weightUnit = plan.unit) }

    return SessionExerciseRow(
        id = id,
        exerciseId = exerciseId,
        name = exerciseName,
        subtitle = taxonomySubtitle(primaryMuscle, equipment),
        techniqueNote = techniqueNote,
        supersetGroup = supersetGroup,
        supersetLabel = supersetLabels[id],
        restSeconds = restSeconds,
        weightUnit = plan.unit,
        stepGrams = stepGrams,
        runContinuesAfter = plan.plannedEntry?.sets?.runContinuesAfter().orEmpty(),
        plannedSetCount = plan.plannedSetCount,
        isFinished = isFinished,
        muscleFeel = muscleFeel,
        joints = joints,
        jointPain = jointPain,
        jointPainNote = jointPainNote,
        sets = loggedSets,
        suggestion = suggestionFor(loggedSets, previous, plan),
        // The step this exercise earned, read from the same plan the prefill reads (N50).
        progression = progressionPromptFor(
            planned = plan.progressionSets(),
            performed = loggedSets.map { it.toProgressionPerformance() },
            stepGrams = Weight.stepGramsFor(stepGrams, plan.unit),
        ),
    )
}

/** The sets logged against one session exercise, displayed 1-based and renumbered (N54's neighbours). */
private fun List<SetEntry>.loggedRowsFor(
    sessionExerciseId: String,
    /** The movement's own step, carried onto every row so its editor steps the same way (N77). */
    stepGrams: Long?,
): List<SetRow> =
    filter { it.sessionExerciseId == sessionExerciseId }
        .sortedBy { it.setIndex }
        .mapIndexed { index, set ->
            SetRow(
                id = set.id,
                // Displayed 1-based and renumbered, so deleting the first set leaves the rest
                // reading 1, 2, 3 rather than 2, 3, 4.
                number = index + 1,
                reps = set.reps,
                weightGrams = set.weightGrams,
                rpeHalves = set.rpeHalves,
                note = set.note,
                setType = set.setType,
                assistanceGrams = set.assistanceGrams,
                stepGrams = stepGrams,
            )
        }

/**
 * The plan's sets for one exercise, reduced for the progression rule (ROADMAP N50, N73).
 *
 * The template's own sets, whole. N73 removed the slot's prescription, which used to be merged in
 * here set by set, so the plan the rule reads is the same plan the screen shows with nothing to
 * reconcile.
 *
 * The **target RPE is one number for the exercise** (N59), so every set carries it rather than its
 * own; a set's stored value is only the fallback a plan written before N59 arrives with. That is
 * what lets the rule check every working set against the exercise's single target.
 */
private fun PlanContext.progressionSets(): List<ProgressionPlanSet> =
    plannedEntry?.sets.orEmpty().map { set ->
        set.toProgressionSet(rpeTarget = plannedEntry?.targetRpeHalves ?: set.targetRpeHalves)
    }

private fun TemplateSet.toProgressionSet(rpeTarget: Int?): ProgressionPlanSet = ProgressionPlanSet(
    setId = id,
    setIndex = setIndex,
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    // Where in the range the lifter has climbed (N74): the number the rule measures and a step moves.
    targetRepsCurrent = targetRepsCurrent,
    targetRpeHalves = rpeTarget ?: targetRpeHalves,
    note = note,
    legacyRpeHalves = targetRpeHalves,
)

/** One logged set as the rule reads it: the role decides whether it is work at all (N50). */
private fun SetRow.toProgressionPerformance(): ProgressionPerformance = ProgressionPerformance(
    reps = reps,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    rpeHalves = rpeHalves,
    role = setType,
)

/**
 * Writes every step the lifter picked, and reports the first failure (ROADMAP N74).
 *
 * File-level rather than a member because the class is at the function ceiling detekt enforces — the
 * shape [planEntriesFor] already uses — and because this is a sequence over the repository rather than
 * a question about the session. Each accepted set is handed to [onWritten] as it lands, so a retry
 * after a failure writes only what is left.
 */
private suspend fun writeProgressionSteps(
    chosen: List<ProgressionChoice>,
    repository: TemplateRepository,
    onWritten: (ProgressionPlanSet) -> Unit,
): DataError? {
    for (choice in chosen) {
        val accepted = choice.offer.accepted(choice.direction) ?: continue
        val written = repository.updateSet(accepted.setId, accepted.toTemplateEdit())
        if (written is DataResult.Failure) return written.error
        onWritten(accepted)
    }
    return null
}

/**
 * The accepted set as the template write takes it (N14).
 *
 * The whole target travels, not only the field the step moved: `updateSet` overwrites a planned set,
 * so a partial edit would clear every target the step did not name.
 */
private fun ProgressionPlanSet.toTemplateEdit(): TemplateSetEdit = TemplateSetEdit(
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    // The lifter's place in the range travels with the write (N74), or the step would be lost and
    // the next session would ask for the floor again.
    targetRepsCurrent = targetRepsCurrent,
    // The set's **own** legacy value, not the exercise's number the rule read: copying the latter
    // into a column the reader falls back to is what made a cleared plan's RPE come back (N59).
    targetRpeHalves = legacyRpeHalves,
    note = note,
)

/**
 * What the next set is shown with: the plan's target where it speaks, then history (ROADMAP N14, N59,
 * P3.8). The plan's RPE rides in the value rather than beside it: the stepper opens on it, the lifter
 * reads it and changes it when the set felt different, and what is recorded is still that set's own
 * number — with no plan the field opens on [DEFAULT_RPE_HALVES] instead of staying blank.
 */
private fun SessionExercise.suggestionFor(
    loggedSets: List<SetRow>,
    previous: PreviousPerformance?,
    plan: PlanContext,
): SetSuggestion = suggestionForNextSet(
    loggedSets = loggedSets,
    previous = previous,
    // The template is the plan, whole (ROADMAP N73).
    planned = plannedTargetFor(
        planned = plan.plannedEntry,
        nextIndex = loggedSets.size,
        // A rung's load comes off the bar the anchor actually loaded (ROADMAP N79).
        anchorLoad = loggedSets.anchorLoadForTheNextRung(),
    ),
    // And a rung's reps come from what this same set did in the previous training, matched by the
    // set's place in the plan — the only identity a logged row keeps between sessions (N79).
    sameSetLastTime = previous
        ?.sets
        ?.firstOrNull { it.setIndex == loggedSets.size },
)

/**
 * What the set the next rung hangs off actually loaded, or null (ROADMAP N79).
 *
 * Read from the back of the session's own log and skipping anything that does not stand on its own,
 * because a run hangs off such a set: for the second drop of a run the set above it is the first drop,
 * and the anchor is still the working set before them — and a **warm-up is not an anchor either**, the
 * rule `runAt` holds, so a drop logged under one derives nothing here rather than taking the ramp's
 * load (B63). It is what a drop is taken off: the bar in front of you, not the number the plan wrote
 * down.
 */
private fun List<SetRow>.anchorLoadForTheNextRung(): Load? =
    lastOrNull { it.setType.recordsEffort }?.let { Load(it.weightGrams, it.assistanceGrams) }

/**
 * `A1`, `A2` … for a grouped exercise, or null (ROADMAP N24).
 *
 * Giant-set notation, and the group letter follows the order the groups appear in the workout
 * so the first pair the user makes is always A — a label that changed as exercises moved would
 * be worse than no label.
 */
private fun supersetLabelsFor(exercises: List<SessionExercise>): Map<String, String> {
    val groups = exercises.mapNotNull { it.supersetGroup }.distinct().sorted()
    return exercises.mapNotNull { exercise ->
        val group = exercise.supersetGroup ?: return@mapNotNull null
        val letter = 'A' + groups.indexOf(group)
        if (letter !in 'A'..'Z') return@mapNotNull null
        // The member's place in its own group, in the order the workout shows them.
        val member = exercises.filter { it.supersetGroup == group }.indexOfFirst { it.id == exercise.id } + 1
        exercise.id to "$letter$member"
    }.toMap()
}

/**
 * The longest rest any member of this exercise's group prescribes, or null (ROADMAP B15).
 *
 * Null for an ungrouped exercise, and null for a group where nobody prescribes one, so the
 * caller falls back to the exercise's own rest and then to the app-wide setting.
 */
private fun ActiveWorkoutUiState.longestRestInRound(row: SessionExerciseRow): Int? {
    val group = row.supersetGroup ?: return null
    return exercises
        .filter { it.supersetGroup == group }
        .mapNotNull { it.restSeconds }
        .maxOrNull()
}
