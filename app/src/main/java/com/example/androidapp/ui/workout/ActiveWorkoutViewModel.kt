package com.example.androidapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.CalibrationTarget
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.PerformedSetSpec
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.STALL_SESSIONS
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.pendingProgressionFor
import com.example.androidapp.domain.model.runContinuesAfter
import com.example.androidapp.domain.repository.DeloadRepository
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.ProgressionRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.WriteOutcome
import com.example.androidapp.ui.navigation.ActiveWorkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import javax.inject.Inject

/**
 * One marked plan's "use today's weights" line at the finish (ROADMAP N309).
 *
 * The targets are the plan's own work sets by place, carrying only the performed weight and assistance:
 * reps, range, hold and target effort stay the plan's, and a place with no counterpart is not invented.
 */
data class CalibrationNotice(
    val templateExerciseId: String,
    val exerciseName: String,
    val setCount: Int,
    val topWeightGrams: Long,
    val topAssistanceGrams: Long,
    val weightUnit: WeightUnit,
    val targets: List<CalibrationTarget>,
)

/**
 * Where the finish's stall decision goes (ROADMAP N310): the plan screen, with the target it must open.
 *
 * [replace] false opens the deload confirm, true the replace sheet; both are the plan editor's, so the undo
 * lives there (decision 3a). The workout screen navigates and is popped; it never writes the plan itself.
 */
data class PlanHandOff(
    val programId: String,
    val templateId: String,
    val templateExerciseId: String,
    val replace: Boolean,
)

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
// The logger owns the session, its sets, its rest, its edits and its prompt: splitting it by function would put
// one write's read in a different object from the write it guards, which is the locking B126/B129 exists to stop.
// Each constructor parameter is a different port it has to ask (N219 added the body's weight on record). The two
// ceilings this type crosses are recorded in detekt.yml, where a reviewer sees them (B255).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val timeSource: TimeSource,
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    private val stallFinder: PlanStallFinder,
    private val progressionRepository: ProgressionRepository,
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val measurementRepository: MeasurementRepository,
    private val deloadRepository: DeloadRepository,
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

    private val lastError = MutableStateFlow<DataError?>(null)

    /**
     * Orders every write to a set against every other (ROADMAP N23, B118, B126, B129).
     *
     * Logging a set reads what this session has already done, compares the set against it, and only then
     * writes. Each of those is a suspension point, so two taps close together used to interleave: both
     * reads ran before either write landed, both compared against **history alone**, and the lighter set
     * was announced as a record over the heavier one beside it. B118 moved that read to the repository,
     * which closed the gap between a write committing and the screen's state catching up; this closes the
     * gap between two writes.
     *
     * **B129 widened it from logging to every set write.** The comparison is made of two reads —
     * history, then this session's sets for the movement — and an edit or a delete landing between them
     * moves the picture the log compared against: the comparison sees a row the write beside it is
     * removing, or misses one it is lowering, and the banner is announced over a set that is no longer
     * what it was. Neither of those announces anything, so that is the whole of the defect — a comparison
     * briefly stale rather than a false banner, which is what makes it lower severity than B126 and not
     * the same one. Ordering the writes is the answer rather than snapshotting the comparison: the log's
     * reads and its write stay one step, so what it compared is what it wrote against.
     *
     * **B134 brought the undo in too.** `onUndoDelete` writes a set — it calls `logSet` to put back the
     * row a soft delete hid — and the snackbar offering it is on screen while a log can be in flight, so
     * leaving it out let the restored set land between the comparison's two reads and be missed, or
     * announced as beaten when it is already back. The first cut argued it could not interleave because
     * it runs to its first suspension on the main dispatcher, but `logSet`'s first suspension **is** the
     * write; the lock is what actually orders it.
     *
     * `onAddExercise` stays outside, and that is not the same omission: it adds an exercise row rather
     * than a set, so it moves nothing a comparison reads.
     */
    private val writingASet = Mutex()

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
     * What *Done* does with the next step a plan earned (ROADMAP N66, P3.17).
     *
     * Read from settings rather than held as its own state: the answer is the app's, and this reads it
     * at the moment Done is tapped (N74 moved that decision here from the screen). *Ask* by default, so
     * an upgrade changes nothing.
     */
    private val progressionMode: StateFlow<ProgressionMode> =
        settingsRepository.observeProgressionMode()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ProgressionMode.SUGGEST)

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

    /**
     * The record just set, if the last logged set was one (ROADMAP N23).
     *
     * Not dismissible on purpose: it clears when the next set is logged, which is when the
     * news is stale — a banner that needs dismissing is a banner in the way between sets.
     */
    private val _personalRecord = MutableStateFlow<PersonalRecordMoment?>(null)
    val personalRecord: StateFlow<PersonalRecordMoment?> = _personalRecord.asStateFlow()

    /**
     * A set that cannot be written until the lifter's weight is known (ROADMAP N219).
     *
     * A state rather than a silent default: a bodyweight set whose load is unknown is not a set of zero
     * kilograms, and writing one would put a figure in every volume and 1RM trend that never happened.
     */
    private val _bodyweightPrompt = MutableStateFlow<PendingBodyweight?>(null)
    val bodyweightPrompt: StateFlow<PendingBodyweight?> = _bodyweightPrompt.asStateFlow()

    /**
     * The stalled lifts of a workout that has just finished (ROADMAP N305).
     *
     * Not a review: what a session did is read back in history, and the only fact the finish still states
     * is the one a lifter cannot see from the screen — a lift that has stopped moving. Null when there is
     * nothing to say.
     */
    private val _stallNotice = MutableStateFlow<List<PlanStall>?>(null)
    val stallNotice: StateFlow<List<PlanStall>?> = _stallNotice.asStateFlow()

    /** The find-the-weight prompt waiting at the finish, before the stall notice (ROADMAP N309). */
    private val _calibrationNotice = MutableStateFlow<List<CalibrationNotice>?>(null)
    val calibrationNotice: StateFlow<List<CalibrationNotice>?> = _calibrationNotice.asStateFlow()

    /** The program the finish's stalls belong to, for the hand-off (N310). */
    private val _stallProgramId = MutableStateFlow<String?>(null)

    /** The plan hand-off the route navigates to (N310). */
    private val _planHandOff = MutableStateFlow<PlanHandOff?>(null)
    val planHandOff: StateFlow<PlanHandOff?> = _planHandOff.asStateFlow()

    /** The stall the finish owes once the calibration prompt has been answered (N305, N309). */
    private val pendingStallNotice = MutableStateFlow<FinishStalls?>(null)

    private data class FinishStalls(val programId: String, val stalls: List<PlanStall>)

    private val pendingUndo = MutableStateFlow<SetEntry?>(null)
    private val previousByExercise = MutableStateFlow<Map<String, PreviousPerformance>>(emptyMap())

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

    /**
     * The steps auto progression just wrote, or null while there is nothing to read back (N299).
     *
     * One moment is on screen at a time; the rest wait in [queuedAutoSteps] until the banner is read or
     * undone, because the banner is the only place those writes can be taken back (B402). Held until
     * dismissed or undone — never replaced by the next exercise's steps, which is what used to make an
     * earlier batch un-undoable. A snackbar would queue behind the finish snackbar the same *Done*
     * produces; a banner above the list is the one place the news and its undo can both be seen.
     */
    private val _autoStep = MutableStateFlow<AutoProgressionMoment?>(null)
    val autoStep: StateFlow<AutoProgressionMoment?> = _autoStep.asStateFlow()

    /** Moments waiting behind the one on screen, oldest first (B402). Main-thread only. */
    private val queuedAutoSteps = ArrayDeque<AutoProgressionMoment>()

    /** Emits true once a finish or discard succeeds, so the screen can leave. */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed

    /** The last set edit's answer, so its editor closes on success and stays on a refusal (B284). */
    private val setWrite = MutableStateFlow<SetWrite?>(null)
    val setWriteOutcome: StateFlow<SetWrite?> = setWrite.asStateFlow()

    /**
     * Shared so the session is not queried once per downstream flow.
     */
    private val activeSession = workoutRepository.observeActiveSession()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    /**
     * The plan the open session follows (ROADMAP N14, B117).
     *
     * Read from the **session**, which records the template it was started from (`startOrResumeSession`),
     * rather than from the route alone: the route's argument says how the workout was *asked to begin* and
     * is absent on a resume, which is why resuming from home used to drop the plan. It still answers while
     * the session is being created, before there is a row to read.
     *
     * Nothing is snapshotted: editing the template changes what the plan says, which is what N16 calls a
     * living template.
     */
    private val plannedExercises: Flow<List<TemplateExercise>> =
        activeSession
            .map { it?.templateId ?: templateId }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else templateRepository.observeExercises(id)
            }

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

    /**
     * Whether the open workout is a day of a live deload week (ROADMAP B400).
     *
     * Read through the session's template, the same key auto progression resolves, so a start from home,
     * the template list or a repeat all answer, and a resume survives. False until the session exists: there
     * is no plan to stop earning a step before then.
     */
    private val isDeloadWorkout: Flow<Boolean> = activeSession
        .map { it?.templateId ?: templateId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) flowOf(false) else deloadRepository.observeDeloadWorkout(id)
        }

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
        isDeloadWorkout,
    ) { session, plan, deload ->
        Snapshot(
            session = session.session,
            exercises = session.exercises,
            sets = session.sets,
            previous = plan.previous,
            planned = plan.planned,
            unit = plan.unit,
            isDeloadWorkout = deload,
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
        pendingFinishedExercise,
    ) { snapshot, error, undo, finished ->
        snapshot.toUiState(
            error = error,
            undo = undo,
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

    /** The history read this session opens with (ROADMAP N73, P3.8), kept in its own file (N285). */
    private val previousPerformanceSeeder = PreviousPerformanceSeeder(
        sessionExercises = sessionExercises,
        activeSession = activeSession,
        previousByExercise = previousByExercise,
        slotId = slotId,
        programRepository = programRepository,
        workoutRepository = workoutRepository,
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
                // **Nothing asks for the readiness note** (N140): a new session no longer opens into the
                // dialog, and the row in the header is the only way in. Opening is not the moment to write
                // it — the lifter has not done anything yet — and the prompt was doing the work the row
                // should: being findable.
                is DataResult.Success -> lastError.value = null

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

        // Load each exercise's previous performance once, when it first appears. The read itself lives in
        // PreviousPerformanceSeeder.kt with the rest of the history lookup (ROADMAP N285).
        viewModelScope.launch {
            previousPerformanceSeeder.seed { lastError.value = it }
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
        // **The movement's own answer is the most specific** (ROADMAP N235): only a movement with no
        // answer can fall through to the program's switch and then to the app setting.
        val movementMode = row?.progressionMode
        // **A deload workout earns no step** (ROADMAP B400): the lighter week is the point, so *Done*
        // finishes it with no question and no write, whatever the movement or device mode says.
        val isDeload = uiState.value.isDeloadWorkout && row?.progression?.hasPlan == true
        when {
            questionOpen -> {
                // *Not now* finishes the exercise, and **records what was offered** (ROADMAP N236): the
                // offer was frozen from the plan as it stood, so nothing can recompute it once the plan
                // moves on.
                val declined = _pendingProgression.value?.sets
                    .orEmpty()
                    .flatMap { it.prompt.offer?.directions.orEmpty() }
                    .toSet()
                _pendingProgression.value = null
                viewModelScope.launch {
                    // A failed record does not stop the finish: the lifter declined, and the exercise is
                    // done either way. The failure is stated **after** the finish, because finishing
                    // clears the error channel on success — saying it before is saying it to nobody
                    // (B309).
                    val declineError = if (declined.isNotEmpty()) {
                        (workoutRepository.declineProgression(sessionExerciseId, declined) as? DataResult.Failure)
                            ?.error
                    } else {
                        null
                    }
                    finishExercise(sessionExerciseId)
                    if (declineError != null && lastError.value == null) lastError.value = declineError
                }
            }

            isDeload -> viewModelScope.launch { finishExercise(sessionExerciseId) }

            movementMode != null -> finishWithMode(sessionExerciseId, row, movementMode)
            row?.progression?.hasPlan != true ->
                viewModelScope.launch { finishExercise(sessionExerciseId) }

            else -> viewModelScope.launch { resolveAutoAndFinish(sessionExerciseId, row) }
        }
    }

    /**
     * Resolves whether this session is inside a program that adjusts itself (ROADMAP N299).
     *
     * The read is through the session's **template**, because a session records only that: it is
     * what makes a start from home, the template list or a repeat all count, and what survives a
     * resume. A failed read is said and then falls back to the device's own answer, so a storage
     * error cannot silently turn a mode off.
     */
    private suspend fun resolveAutoAndFinish(
        sessionExerciseId: String,
        row: SessionExerciseRow,
    ) {
        val templateId = uiState.value.sessionTemplateId
        val autoProgram = if (templateId == null) {
            null
        } else {
            when (val read = progressionRepository.autoProgramForTemplate(templateId)) {
                is DataResult.Success -> read.data
                is DataResult.Failure -> {
                    lastError.value = read.error
                    null
                }
            }
        }
        if (autoProgram != null) {
            applyAutoProgression(sessionExerciseId, row, autoProgram)
        } else {
            finishWithMode(sessionExerciseId, row, progressionMode.value)
        }
    }

    /** The question or the finish a non-auto exercise gets (N50, N66, N235). */
    private fun finishWithMode(
        sessionExerciseId: String,
        row: SessionExerciseRow?,
        mode: ProgressionMode,
    ) {
        if (mode != ProgressionMode.OFF && row?.progression?.hasPlan == true) {
            val question = pendingProgressionFor(
                sessionExerciseId = sessionExerciseId,
                exerciseName = row.name,
                unit = row.weightUnit,
                prompt = row.progression,
            )
            // *Apply at Done* starts with every earned step picked, so one press writes them all; the
            // lifter still reads the answer, and nothing is written until it (N33, P3.17).
            _pendingProgression.value = if (mode == ProgressionMode.APPLY_AT_DONE) {
                question.chooseEveryOffer()
            } else {
                question
            }
        } else {
            viewModelScope.launch { finishExercise(sessionExerciseId) }
        }
    }

    /**
     * Writes every earned step without asking, then finishes the exercise (ROADMAP N299).
     *
     * The order is the plan's: the write, its record, then the finish. A failure leaves the exercise
     * open so a retry can offer what did not land; the rows that did land no longer offer anything,
     * because the plan has moved past the work that earned them.
     */
    private suspend fun applyAutoProgression(
        sessionExerciseId: String,
        row: SessionExerciseRow,
        programId: String,
    ) {
        val question = pendingProgressionFor(
            sessionExerciseId = sessionExerciseId,
            exerciseName = row.name,
            unit = row.weightUnit,
            prompt = row.progression,
        ).chooseEveryOffer()
        if (question.chosen.isEmpty()) {
            finishExercise(sessionExerciseId)
            return
        }
        val steps = mutableListOf<AutoProgressionStep>()
        val failure = writeAutoProgressionSteps(
            chosen = question.chosen,
            sessionExerciseId = sessionExerciseId,
            programId = programId,
            repository = templateRepository,
            onWritten = { steps += it },
        )
        if (failure != null) {
            lastError.value = failure
            return
        }
        if (steps.isNotEmpty()) {
            showAutoStep(
                AutoProgressionMoment(
                    exerciseName = row.name,
                    unit = row.weightUnit,
                    steps = steps,
                ),
            )
        }
        finishExercise(sessionExerciseId)
        // A step was taken, so an earlier *Not now* no longer describes this exercise (B310).
        val cleared = workoutRepository.declineProgression(sessionExerciseId, emptySet())
        if (cleared is DataResult.Failure && lastError.value == null) lastError.value = cleared.error
    }

    /** The one on screen, or the next one queued behind it (B402). */
    private fun showAutoStep(moment: AutoProgressionMoment) {
        if (_autoStep.value == null) {
            _autoStep.value = moment
        } else {
            queuedAutoSteps.addLast(moment)
        }
    }

    /** The banner was read or undone, so the next write's news takes its place (B402). */
    private fun advanceAutoStep() {
        _autoStep.value = queuedAutoSteps.removeFirstOrNull()
    }

    /** Takes back the auto steps the banner names, newest first (ROADMAP N299). */
    fun onUndoAutoStep() {
        val moment = _autoStep.value ?: return
        viewModelScope.launch {
            var failure: DataError? = null
            moment.steps.asReversed().forEach { step ->
                if (failure == null) {
                    failure = (progressionRepository.undo(step.writeId) as? DataResult.Failure)?.error
                }
            }
            advanceAutoStep()
            if (failure != null) lastError.value = failure
        }
    }

    /** The banner has been read, so it is not shown again (ROADMAP N299). */
    fun onDismissAutoStep() {
        advanceAutoStep()
    }

    /**
     * Picks a step on the frozen question (ROADMAP N74).
     *
     * [setId] names one set; null means every set that offers [direction], which is the plan whose
     * sets share a target and would otherwise ask for the same tap once per set. A pick is not a
     * write: nothing reaches the plan until [onConfirmProgression], which is what lets a lifter read
     * the whole answer before taking it.
     */
    fun onSelectProgression(setId: String?, direction: ProgressionDirection?) {
        _pendingProgression.update { pending ->
            pending?.let {
                when {
                    // A null direction is the mixed-plan answer: each set's own step (P3.17).
                    direction == null -> it.chooseEveryOffer()
                    setId == null -> it.chooseAll(direction)
                    else -> it.toggle(setId, direction)
                }
            }
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
            val tookAStep = pending.chosen.isNotEmpty()
            finishExercise(pending.sessionExerciseId)
            if (tookAStep) {
                // The step was taken, so an earlier *Not now* no longer describes this exercise (B310). A
                // failure here is said rather than hidden, but it cannot undo the accepted steps.
                val cleared = workoutRepository.declineProgression(pending.sessionExerciseId, emptySet())
                if (cleared is DataResult.Failure && lastError.value == null) lastError.value = cleared.error
            }
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
        // A tap inside the write is the same set, not a second one (B273). The write's own mutex orders
        // the turn it takes; this is what stops two identical rows being queued for that turn.
        if (loggingSetFor == sessionExerciseId) return
        loggingSetFor = sessionExerciseId
        viewModelScope.launch {
            try {
                writeLoggedSet(row, edit)
            } finally {
                loggingSetFor = null
            }
        }
    }

    /** The row whose set is being written, so a second tap before it lands is ignored (B273). */
    private var loggingSetFor: String? = null

    /**
     * Resolves the snapshot once, then writes the set (ROADMAP N219, B251).
     *
     * The load is resolved before the write and travels with it. With nothing on record — or a weigh-in
     * more than a month old — the set is **held** while the lifter says what they weigh, rather than
     * written as zero; and a read that failed is not "no weight on record": it is said, and nothing is
     * written.
     */
    /**
     * Says a record the moment it happens, or nothing (ROADMAP N23).
     *
     * A record noticed a week later in a list is a record nobody feels, so it is announced from the write
     * that produced it: the role goes in, or a heavy warm-up raises a best (B17), and what it beat is read
     * from the *merged* view, because the bar may have been set earlier in this same session — saying "the
     * first time at this rep count" then would be false while claiming a record over that very set (B18).
     * A **bodyweight** set claims nothing (B246): its belt is not the lift.
     */
    private fun announceRecord(row: SessionExerciseRow, against: PersonalRecords?, edit: SetEdit) {
        if (against == null || row.isBodyweight) return
        if (!against.isRecord(edit.reps, edit.weightGrams, edit.setType)) return
        _personalRecord.value = PersonalRecordMoment(
            exerciseName = row.name,
            reps = edit.reps,
            weightGrams = edit.weightGrams,
            previousBestGrams = against.bestAt(edit.reps),
            weightUnit = row.weightUnit,
        )
    }

    private suspend fun writeLoggedSet(row: SessionExerciseRow, edit: SetEdit) {
        var bodyweight: Long? = null
        if (row.isBodyweight) {
            when (val read = newestWeight(measurementRepository)) {
                is DataResult.Failure -> {
                    lastError.value = read.error
                    return
                }

                is DataResult.Success -> {
                    val latest = read.data
                    if (latest == null || latest.isStale(timeSource.now())) {
                        _bodyweightPrompt.value = PendingBodyweight(
                            sessionExerciseId = row.id,
                            exerciseName = row.name,
                            edit = edit,
                            staleDays = latest?.ageInDays(timeSource.now()),
                        )
                        return
                    }
                    bodyweight = latest.grams
                }
            }
        }
        val sessionExerciseId = row.id
        // **One set is written at a time** (ROADMAP N23, B118, B126, B129): the comparison, the write
        // and the banner that follows it are one step, so neither a second tap nor an edit or delete
        // beside it can move the picture this compared against.
        writingASet.withLock {
            // Read *before* the set is written (ROADMAP N23, B118): the set being logged is not history.
            val against = recordsAgainst(row)
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
                bodyweightGrams = bodyweight,
                durationSeconds = edit.durationSeconds,
            )
            handle(result)
            if (result is DataResult.Success) {
                announceRecord(row, against, edit)

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
     * What the set about to be logged is judged against: history, plus every set this session has already
     * logged **for the movement** (ROADMAP N23, B118).
     *
     * History is read with the live session **excluded** — the set being logged is not history, and neither
     * is any other set of this workout — so what the session has already done is the other half of the
     * comparison, and without it the same record is announced on every set that repeats it. That half is
     * every row of the movement rather than the row being written to: a plan may name a movement twice
     * (N54), and a set on the second row is history for the first (B118).
     *
     * Read from the repository rather than from the screen's state, because a state that has not caught up
     * with the set just written would do the same thing one set in. Null when the history read failed, which
     * leaves the screen making no claim either way rather than a false one.
     */
    private suspend fun recordsAgainst(row: SessionExerciseRow): PersonalRecords? {
        val records = when (
            val read = workoutRepository.personalRecords(
                exerciseId = row.exerciseId,
                excludingSessionId = uiState.value.sessionId,
            )
        ) {
            is DataResult.Success -> read.data
            is DataResult.Failure -> null
        } ?: return null

        val movementRowIds = uiState.value.exercises
            .filter { it.exerciseId == row.exerciseId }
            .mapTo(mutableSetOf()) { it.id }
        val loggedForMovement = uiState.value.sessionId
            ?.let { workoutRepository.observeSets(it).first() }
            .orEmpty()
            .filter { it.sessionExerciseId in movementRowIds }

        return records.mergedWith(
            PersonalRecords.from(
                loggedForMovement.map {
                    PerformedSetSpec(
                        role = it.setType,
                        weightGrams = it.weightGrams,
                        assistanceGrams = it.assistanceGrams,
                        reps = it.reps,
                    )
                },
            ),
        )
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
        durationSeconds: Int? = null,
    ) {
        viewModelScope.launch {
            // An edit moves a set a log's comparison may be reading, so it takes its turn with logging
            // (ROADMAP B129). The edit itself compares nothing.
            writingASet.withLock {
                // A new edit is a new answer (B340): the previous Written(setId) is equal to this one,
                // so leaving it in place would let the StateFlow conflate the next success and never
                // re-run the effect that closes the editor.
                setWrite.value = null
                // The banner names the record the set was; an edit or a delete can make that false (B290).
                _personalRecord.value = null
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
                handle(result)
                setWrite.value = when (result) {
                    is DataResult.Success -> WriteOutcome.Written(setId)
                    is DataResult.Failure -> WriteOutcome.Refused(setId, result.error)
                }
            }
        }
    }

    fun onDeleteSet(setId: String) {
        val set = setsState.value.firstOrNull { it.id == setId } ?: return
        viewModelScope.launch {
            // A delete removes a set a log's comparison may be reading, for the same reason (ROADMAP B129).
            writingASet.withLock {
                // Deleting the set can delete the record it announced (B290).
                _personalRecord.value = null
                when (val result = workoutRepository.deleteSet(set.id)) {
                    is DataResult.Success -> pendingUndo.value = set
                    is DataResult.Failure -> lastError.value = result.error
                }
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
        // The write's own question (B160), not "is the row still listed": a finished exercise is still listed
        // and still refuses a set, so asking presence alone let the tap through to a refused write and a
        // sentence about a workout that is open.
        if (uiState.value.exercises.none { it.id == set.sessionExerciseId && !it.isFinished }) {
            lastError.value = GONE_FROM_SESSION
            return
        }
        viewModelScope.launch {
            // An undo writes a set, so it takes its turn with logging (B134): without the lock the
            // restored row can land between a log's two reads and be missed, or announced as beaten.
            writingASet.withLock {
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
                        bodyweightGrams = set.bodyweightGrams,
                        durationSeconds = set.durationSeconds,
                    ),
                )
            }
        }
    }

    fun onDismissUndo() {
        pendingUndo.value = null
    }

    /**
     * Answers the bodyweight prompt (ROADMAP N219): the weight is saved, then the held set is logged.
     *
     * The weigh-in is written as **today's measurement** rather than onto the set alone, so Measurements,
     * statistics and the weight goal all read one value — and the set snapshots it, so a later correction
     * changes the record of the body without rewriting what was lifted.
     */
    fun onBodyweightTaken(grams: Long) {
        val pending = _bodyweightPrompt.value ?: return
        if (grams <= 0L) return
        viewModelScope.launch {
            when (val saved = saveTodayWeight(measurementRepository, timeSource.now(), grams)) {
                is DataResult.Success -> {
                    _bodyweightPrompt.value = null
                    // The lookup inside onLogSet now finds it, which keeps one write path for a set.
                    onLogSet(pending.sessionExerciseId, pending.edit)
                }

                is DataResult.Failure -> {
                    _bodyweightPrompt.value = null
                    lastError.value = saved.error
                }
            }
        }
    }

    /** The prompt was dismissed: the set is not logged, and nothing is written (ROADMAP N219). */
    fun onDismissBodyweightPrompt() {
        _bodyweightPrompt.value = null
    }

    /**
     * Writes the readiness note (ROADMAP N4), from the row in the workout header — the only way in since N140
     * retired the prompt a new session used to open with. A null or blank note clears it.
     *
     * Soreness is not part of this any more (N113): it is recorded with the day, in measurements.
     */
    fun onSaveReadinessNote(note: String?) {
        val sessionId = uiState.value.sessionId ?: return
        viewModelScope.launch {
            when (val result = workoutRepository.setReadiness(sessionId, note)) {
                is DataResult.Success -> {
                    lastError.value = null
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Ends the running rest without waiting for it (ROADMAP P1.4). */
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
            // Read *before* finishing: the state describes the live session, so once the workout is
            // stored its rows leave the flow. Both notices are about the workout that was just trained.
            val templateId = uiState.value.sessionTemplateId
            val calibration = calibrationNotices()

            when (val result = workoutRepository.finishSession(sessionId)) {
                is DataResult.Success -> {
                    lastError.value = null
                    // The find-the-weight prompt comes first (N309); the stall is owed after it (N305).
                    val notice = finishStallNotice(templateId)
                    if (calibration.isEmpty()) {
                        if (notice == null) closeSession() else showStalls(notice)
                    } else {
                        pendingStallNotice.value = notice
                        _calibrationNotice.value = calibration
                    }
                }

                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /**
     * Makes this session an activity, or a workout again (ROADMAP N141).
     *
     * Written straight through like every other session write: the lifter chose it, and a failure has to be
     * visible rather than leaving the screen disagreeing with the store.
     */
    fun onSetActivity(activity: Activity?) {
        val sessionId = uiState.value.sessionId ?: return
        viewModelScope.launch {
            when (val result = workoutRepository.setSessionActivity(sessionId, activity)) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Stores an activity's optional duration/distance, or clears them (N293). */
    fun onSetActivityMetrics(durationSeconds: Int?, distanceMeters: Long?) {
        val sessionId = uiState.value.sessionId ?: return
        viewModelScope.launch {
            when (val result = workoutRepository.setActivityMetrics(sessionId, durationSeconds, distanceMeters)) {
                is DataResult.Success -> lastError.value = null
                is DataResult.Failure -> lastError.value = result.error
            }
        }
    }

    /** Dismisses the stall notice, which is what actually closes the screen (ROADMAP N305). */
    fun onDismissStallNotice() {
        _stallNotice.value = null
        _stallProgramId.value = null
        closeSession()
    }

    /** The plan's marked exercises, with the session's performed work sets by place (N309). */
    private suspend fun calibrationNotices(): List<CalibrationNotice> {
        val templateId = uiState.value.sessionTemplateId ?: return emptyList()
        val read = dataResultOf {
            val plan = templateRepository.observeExercises(templateId).first()
            val sets = templateRepository.observeSets(templateId).first()
            plan.filter { it.calibrationPending }.mapNotNull { exercise ->
                // A substituted row did not train the plan's movement (decision 6a); it says nothing here.
                val row = uiState.value.exercises.firstOrNull {
                    it.exerciseId == exercise.exerciseId && it.substitutedFromExerciseId == null
                } ?: return@mapNotNull null
                val performed = row.sets.filter { it.setType.recordsEffort }
                val planned = sets.filter {
                    it.templateExerciseId == exercise.id && it.role.recordsEffort
                }
                val count = minOf(performed.size, planned.size)
                val top = performed.maxByOrNull { it.weightGrams } ?: return@mapNotNull null
                if (count == 0) return@mapNotNull null
                CalibrationNotice(
                    templateExerciseId = exercise.id,
                    exerciseName = row.name,
                    setCount = count,
                    topWeightGrams = top.weightGrams,
                    topAssistanceGrams = top.assistanceGrams,
                    weightUnit = row.weightUnit,
                    // Extra sets are N311's business, not the calibration's: the plan's places only (6a).
                    targets = (0 until count).map { index ->
                        CalibrationTarget(
                            setId = planned[index].id,
                            weightGrams = performed[index].weightGrams,
                            assistanceGrams = performed[index].assistanceGrams,
                        )
                    },
                )
            }
        }
        return when (read) {
            is DataResult.Success -> read.data
            is DataResult.Failure -> {
                lastError.value = read.error
                emptyList()
            }
        }
    }

    /** Adopts every logged work set's load and clears the marker (N309). */
    fun onUseCalibration(notice: CalibrationNotice) {
        viewModelScope.launch {
            when (val read = templateRepository.calibrateExercise(notice.templateExerciseId, notice.targets)) {
                is DataResult.Success -> removeCalibration(notice)
                is DataResult.Failure -> lastError.value = read.error
            }
        }
    }

    /** Leaves the plan weightless and clears the marker (N309). */
    fun onSkipCalibration(notice: CalibrationNotice) {
        viewModelScope.launch {
            when (val read = templateRepository.setCalibration(notice.templateExerciseId, false)) {
                is DataResult.Success -> removeCalibration(notice)
                is DataResult.Failure -> lastError.value = read.error
            }
        }
    }

    private fun removeCalibration(notice: CalibrationNotice) {
        val remaining = _calibrationNotice.value.orEmpty()
            .filterNot { it.templateExerciseId == notice.templateExerciseId }
        _calibrationNotice.value = remaining.ifEmpty { null }
        if (remaining.isEmpty()) {
            val stall = pendingStallNotice.value
            pendingStallNotice.value = null
            if (stall == null) closeSession() else showStalls(stall)
        }
    }

    private fun showStalls(notice: FinishStalls) {
        _stallProgramId.value = notice.programId
        _stallNotice.value = notice.stalls
    }

    /** The finish's Deload: hand the plan screen the target it must open (N310); the plan edits, not this. */
    fun onDeloadStall(stall: PlanStall) = handOff(stall, replace = false)

    /** The finish's Replace: the same hand-off, to the plan's replace sheet (N310). */
    fun onReplaceStall(stall: PlanStall) = handOff(stall, replace = true)

    private fun handOff(stall: PlanStall, replace: Boolean) {
        val programId = _stallProgramId.value ?: return
        _stallNotice.value = null
        _stallProgramId.value = null
        _planHandOff.value = PlanHandOff(programId, stall.templateId, stall.templateExerciseId, replace)
    }

    fun onPlanHandOffHandled() {
        _planHandOff.value = null
    }

    /**
     * The stalled lifts a just-finished workout should say, or null when it should say nothing (ROADMAP N305).
     *
     * Only a workout of an **active weekly** program is told: the notice is for a plan being followed, and
     * the deload it points at belongs to a program the lifter runs. Only a run that **just** reached three is
     * told, so the dialog does not become a popup after every session it stays stuck; the program screen and
     * the weekly review carry it from there. A failed read answers null for the same reason: the workout is
     * finished and stored, and an advisory notice must not strand it — the durable surfaces report the error.
     */
    private suspend fun finishStallNotice(templateId: String?): FinishStalls? {
        val program = templateId
            ?.let { programRepository.activeWeeklyProgramForTemplate(it) }
            ?.getOrNull()
        val stalls = templateId
            ?.let { stallFinder.stallsForTemplate(it).getOrNull() }
            ?.filter { it.sessions == STALL_SESSIONS }
        return if (program == null || stalls.isNullOrEmpty()) {
            null
        } else {
            FinishStalls(program, stalls)
        }
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
