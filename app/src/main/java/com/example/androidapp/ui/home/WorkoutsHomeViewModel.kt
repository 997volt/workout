package com.example.androidapp.ui.home

import java.time.ZoneId
import java.time.DayOfWeek
import java.time.LocalDate
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.ui.workout.WorkoutClock
import com.example.androidapp.ui.workout.WorkoutFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * How many recent workouts the home screen shows (ROADMAP N1).
 *
 * A home-sized view, not the history screen: enough to answer "where was I", with a
 * link through for the rest. Five fits on one screenful on a small phone without
 * scrolling past the button that matters.
 */
private const val RECENT_LIMIT = 5

/**
 * A workout already in progress.
 *
 * Holds no elapsed time: that ticks, and folding a ticking value into this state
 * would rebuild the list below it once a second — the trap F16 fixed on the workout
 * screen. The clock is a separate flow the button alone reads.
 */
data class ActiveWorkoutInfo(
    val startedAt: Instant,
    val exerciseCount: Int,
)

/**
 * One row of today's plan (ROADMAP P3.3).
 *
 * [id] is the row's own identity — a program slot's id, since a program's slots are the only source
 * of a dated plan (N56). It is deliberately not [templateId]: a program may put the same template in
 * two slots, and a list keyed by template would collide.
 *
 * [slotId] is set only for a program slot, and it is what carries the slot's prescription into
 * the workout (P3.8): the template is what is started, the slot is what it was scheduled as.
 */
data class TodayPlan(
    val id: String,
    val templateId: String,
    val name: String,
    val exerciseCount: Int,
    val slotId: String? = null,
)

/**
 * A program's run, offered as home's next-up row (ROADMAP P3.9).
 *
 * [plan] is what a start needs — the slot's id travels with it so the slot's prescription seeds
 * the workout (P3.8) — and [programName] is there because more than one program may be active
 * (P3.12), so two next-up rows have to be tellable apart.
 */
data class NextUp(
    val plan: TodayPlan,
    val programName: String,
    /** True when nothing has been done yet, so the row words itself as a start (P3.9). */
    val isAtStart: Boolean,
)

/**
 * What is planned for one next-up row, read when the field is tapped (ROADMAP N55).
 *
 * [exercises] is empty while the read is in flight and after one that returned nothing — the two are
 * told apart by [isLoading], because "the plan is empty" and "the plan is not read yet" would
 * otherwise read the same on screen.
 */
data class PlannedWorkout(
    val plan: TodayPlan,
    val programName: String,
    val exercises: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

data class WorkoutsHomeUiState(
    val isLoading: Boolean = true,
    val recent: List<WorkoutSummary> = emptyList(),
    val activeWorkout: ActiveWorkoutInfo? = null,
    /** The device's weekday, for the "Today" heading (ROADMAP N16). */
    val today: DayOfWeek = DayOfWeek.MONDAY,
    /**
     * What is scheduled today: the union of the active programs' slots for this weekday, or —
     * with no program active — the plans pinned to it (ROADMAP P3.3, unioned by P3.12).
     */
    val todaysPlan: List<TodayPlan> = emptyList(),
    /**
     * Where each active program's run is, for a program with nothing scheduled today (ROADMAP P3.9).
     *
     * Empty when every active program has a weekday slot today: the today plan is then the answer,
     * and a next-up row would be a second, contradictory one.
     */
    val nextUp: List<NextUp> = emptyList(),
    /** The templates a substitution can choose from (ROADMAP P3.11). */
    val templates: List<WorkoutTemplate> = emptyList(),
) {
    /**
     * Nothing logged and nothing running — the first-run case, which should point at
     * Start rather than showing an empty list with no explanation.
     */
    val isFirstRun: Boolean get() = !isLoading && recent.isEmpty() && activeWorkout == null
}

/**
 * The home screen (ROADMAP N1).
 *
 * Replaces the exercise library as the start destination. The library is a
 * *reference* — you visit it to look something up — so opening the app on it put a
 * catalogue in front of the thing the user came to do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutsHomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val activeSession = workoutRepository.observeActiveSession()

    /**
     * The next-up row whose plan is being looked at, or null (ROADMAP N55).
     *
     * Separate from [uiState] because it is a transient question about *one* row: folding it in would
     * rebuild the whole list every time a dialog opens, and the list is what the field sits under.
     */
    private val _plannedWorkout = MutableStateFlow<PlannedWorkout?>(null)
    val plannedWorkout: StateFlow<PlannedWorkout?> = _plannedWorkout.asStateFlow()

    /**
     * Reads what one next-up row has planned, and holds it until the dialog closes (ROADMAP N55).
     *
     * A read per tap rather than a flow per row: the dialog is a look at one plan, and keeping a flow
     * open for every active program would be a live query behind a field nobody has tapped. The
     * template is read through the same repository the editor uses, so a rename shows here too — N16's
     * living template, seen from the other end.
     */
    fun onOpenPlannedWorkout(nextUp: NextUp) {
        _plannedWorkout.value = PlannedWorkout(
            plan = nextUp.plan,
            programName = nextUp.programName,
        )
        viewModelScope.launch {
            val exercises = templateRepository.observeExercises(nextUp.plan.templateId).first()
            // Only the row that opened it may fill it in: a dialog closed while the read was in
            // flight must not come back.
            _plannedWorkout.update { current ->
                current?.takeIf { it.plan.id == nextUp.plan.id }?.copy(
                    exercises = exercises.map { it.exerciseName },
                    isLoading = false,
                )
            }
        }
    }

    /** Closes the preview (ROADMAP N55). */
    fun onDismissPlannedWorkout() {
        _plannedWorkout.value = null
    }

    /**
     * Ends the workout in progress without finishing it, so another can be started (ROADMAP N89).
     *
     * The rule lives in [discardActiveSession]; this is the repository it is given.
     */
    suspend fun discardActiveWorkout(): DataResult<Unit> =
        discardActiveSession(activeSession, workoutRepository::deleteSession)

    private val activeWorkout: Flow<ActiveWorkoutInfo?> = activeSession
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                workoutRepository.observeSessionExercises(session.id).map { exercises ->
                    ActiveWorkoutInfo(startedAt = session.startedAt, exerciseCount = exercises.size)
                }
            }
        }

    /**
     * The device's own day, read once when this screen is built (ROADMAP N16).
     *
     * One reading supplies both [today]'s weekday and the week a substitution is recorded
     * against, so the row a lifter taps and the week that write lands in cannot disagree when the
     * clock crosses midnight: a live read for the write and a captured one for the row meant a tap
     * on Sunday's plan could be keyed to Monday's week (P3.11). A screen left open across midnight
     * shows the day it was opened on until it is rebuilt, which is the same accepted staleness the
     * plan's own heading already carries.
     */
    private val todayDate: LocalDate = timeSource.now().atZone(ZoneId.systemDefault()).toLocalDate()

    /** The weekday of [todayDate], for the "Today" heading and the plan it selects. */
    private val today: DayOfWeek = todayDate.dayOfWeek

    /**
     * Every program home follows, in the authored order (ROADMAP P3.12).
     *
     * A list rather than one row: more than one program may be active, and the union of their
     * slots is today's plan. Empty is the state the pins below answer in.
     */
    private val activePrograms: Flow<List<WorkoutProgram>> = programRepository.observeActivePrograms()

    /**
     * The active programs' slots, concatenated in program order (ROADMAP P3.12).
     *
     * Read only while at least one program is active, so a shelf full of programs costs the
     * home screen nothing. The order is the programs' authored order, each program's own slots
     * in their own order; [todaysPlanFor] re-states that ordering rather than trusting it.
     */
    private val activeProgramSlots: Flow<List<ProgramSlot>> = activePrograms
        .flatMapLatest { programs ->
            if (programs.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(programs.map { program -> programRepository.observeSlots(program.id) }) { perProgram ->
                    perProgram.toList().flatten()
                }
            }
        }

    /** Each active program's run, keyed by program id (ROADMAP P3.9). */
    private val programRuns: Flow<Map<String, ProgramRun?>> = activePrograms
        .flatMapLatest { programs ->
            if (programs.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    programs.map { program ->
                        programRepository.observeProgramRun(program.id).map { run -> program.id to run }
                    },
                ) { perProgram -> perProgram.toMap() }
            }
        }

    /** The three program-shaped sources, kept together so the combine below stays four deep. */
    private data class ProgramsPart(
        val programs: List<WorkoutProgram>,
        val slots: List<ProgramSlot>,
        val runs: Map<String, ProgramRun?>,
    )

    private val programsPart: Flow<ProgramsPart> =
        combine(activePrograms, activeProgramSlots, programRuns) { programs, slots, runs ->
            ProgramsPart(programs, slots, runs)
        }

    val uiState: StateFlow<WorkoutsHomeUiState> = combine(
        workoutRepository.observeHistory(),
        activeWorkout,
        templateRepository.observeTemplates(),
        programsPart,
    ) { history, workout, templates, programs ->
        WorkoutsHomeUiState(
            isLoading = false,
            recent = history.take(RECENT_LIMIT),
            activeWorkout = workout,
            today = today,
            todaysPlan = todaysPlanFor(
                programs = programs.programs,
                slots = programs.slots,
                day = today,
            ),
            nextUp = nextUpFor(
                programs = programs.programs,
                slots = programs.slots,
                runs = programs.runs,
                day = today,
            ),
            templates = templates,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WorkoutsHomeUiState(),
    )

    /**
     * Ticks once a second, and **only while a workout is running**: an idle home
     * screen should not hold a timer open for a button that says "Start workout".
     */
    val clock: StateFlow<WorkoutClock> = activeSession
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(WorkoutClock())
            } else {
                ticker.map { WorkoutClock(elapsed = elapsedSince(session)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = WorkoutClock(),
        )

    private fun elapsedSince(session: WorkoutSession): String =
        WorkoutFormat.elapsed(Duration.between(session.startedAt, timeSource.now()))

    /**
     * Records that one occurrence is trained with a different workout, or clears it (ROADMAP P3.11).
     *
     * The week is the one [todayDate] falls in — the day the tapped row was drawn for — so the
     * write is keyed to the occurrence on screen rather than to whatever day the clock has reached
     * since. [templateId] null restores the slot's own workout.
     */
    suspend fun setSubstitution(slotId: String, templateId: String?): DataResult<Unit> =
        programRepository.setSubstitution(
            slotId = slotId,
            weekStart = ProgramSchedule.weekStartOf(todayDate),
            templateId = templateId,
        )

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MILLIS)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TICK_MILLIS = 1_000L
    }
}

/**
 * What is scheduled for [day] (ROADMAP P3.3, unioned by P3.12).
 *
 * The union of the active programs' slots, even on a day they schedule nothing — an empty day is
 * rest, not a fallback. The N16 weekday pins were the other branch of this and went with the column
 * they read (ROADMAP N56): with no active program there is then no *Today* list, which is the change
 * stating that a day is a scheduling fact and scheduling is what a program is for.
 *
 * File-level and pure so the union can be tested without a database or a ViewModel.
 */
internal fun todaysPlanFor(
    programs: List<WorkoutProgram>,
    slots: List<ProgramSlot>,
    day: DayOfWeek,
): List<TodayPlan> =
    // No active program, no dated plan (ROADMAP N56). An orphaned slot — a program that was
    // deactivated after its slots were written — is not a schedule either: the slots belong to the
    // program that follows them.
    if (programs.isEmpty()) emptyList() else slots.scheduledFor(day, programs)

/**
 * Where each active program's run is, as home's next-up rows (ROADMAP P3.9).
 *
 * A program with a weekday slot today is left out: the today plan already says what to do, and a
 * next-up row beside it would be a second, contradictory answer. The order follows the programs'
 * authored order (P3.12), so several rows read the way the union does.
 *
 * File-level and pure so the rule can be tested without a database or a ViewModel.
 */
internal fun nextUpFor(
    programs: List<WorkoutProgram>,
    slots: List<ProgramSlot>,
    runs: Map<String, ProgramRun?>,
    day: DayOfWeek,
): List<NextUp> = programs.mapNotNull { program ->
    val programSlots = slots.filter { it.programId == program.id }
    if (programSlots.any { it.weekday == day }) return@mapNotNull null
    val run = runs[program.id] ?: return@mapNotNull null
    NextUp(
        plan = TodayPlan(
            id = run.slot.id,
            templateId = run.slot.templateId,
            name = run.slot.templateName,
            exerciseCount = run.slot.exerciseCount,
            // The slot travels with the start, so its prescription seeds the workout (P3.8).
            slotId = run.slot.id,
        ),
        programName = program.name,
        isAtStart = run.isAtStart,
    )
}

/**
 * The active programs' slots that fall on [day], in the union's order (ROADMAP P3.12).
 *
 * Ordered by the **program's authored position first, the slot's position second**: two
 * programs each number their slots from zero, so ordering by the slot alone would interleave
 * them. The row's identity is the slot's, not the template's: a program may schedule the same
 * workout twice, and two rows keyed by one template would collide.
 */
private fun List<ProgramSlot>.scheduledFor(
    day: DayOfWeek,
    programs: List<WorkoutProgram>,
): List<TodayPlan> {
    val programOrder = programs.withIndex().associate { (index, program) -> program.id to index }
    return filter { it.weekday == day }
        .sortedWith(
            compareBy(
                { programOrder[it.programId] ?: Int.MAX_VALUE },
                { it.position },
            ),
        )
        .map { slot ->
            TodayPlan(
                id = slot.id,
                templateId = slot.templateId,
                name = slot.templateName,
                exerciseCount = slot.exerciseCount,
                slotId = slot.id,
            )
        }
}
