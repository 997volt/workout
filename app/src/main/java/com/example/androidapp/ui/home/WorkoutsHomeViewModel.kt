package com.example.androidapp.ui.home

import java.time.ZoneId
import java.time.DayOfWeek
import java.time.LocalDate
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.domain.model.isDailyOn
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.WorkoutSession
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
 * [slotId] is set only for a program slot, and it is what the session's own history reads for the
 * prefill (N73): the template is what is started, the slot is what it was scheduled as.
 */
data class TodayPlan(
    val id: String,
    val templateId: String,
    val name: String,
    val exerciseCount: Int,
    val slotId: String? = null,
    /**
     * The program the slot belongs to, or null (ROADMAP N127).
     *
     * Read for the preview's subtitle: a day can carry slots from more than one active program (P3.12),
     * and two cards may name the same workout, so the program is what tells them apart.
     */
    val programName: String? = null,
)

/**
 * What is planned for one workout, read when its card is tapped (ROADMAP N55, N127).
 *
 * [exercises] is empty while the read is in flight and after one that returned nothing — the two are
 * told apart by [isLoading], because "the plan is empty" and "the plan is not read yet" would
 * otherwise read the same on screen.
 */
data class PlannedWorkout(
    val plan: TodayPlan,
    /** The program the plan came from, or null when it belongs to none. */
    val programName: String? = null,
    val exercises: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

data class WorkoutsHomeUiState(
    val isLoading: Boolean = true,
    val activeWorkout: ActiveWorkoutInfo? = null,
    /** The device's weekday, for the "Today" heading (ROADMAP N16). */
    val today: DayOfWeek = DayOfWeek.MONDAY,
    /**
     * What is scheduled today: the union of the active programs' slots for this weekday (ROADMAP P3.3,
     * unioned by P3.12).
     *
     * Empty is both a rest day and a day with nothing followed at all; [planIsRestDay] is what tells the
     * two apart (N126), and the surface says which one it is.
     */
    val todaysPlan: List<TodayPlan> = emptyList(),
    /** Whether any program is followed, which is what makes an empty day a rest day (ROADMAP N126). */
    val hasActiveProgram: Boolean = false,
    /** The templates a substitution can choose from (ROADMAP P3.11). */
    val templates: List<WorkoutTemplate> = emptyList(),
    /**
     * Whether today's daily measurement is already recorded (ROADMAP N114).
     *
     * False offers the row that opens the daily form; true is the ordinary state of a day that has been
     * written down, and the row is simply absent — an entry that states only soreness counts (N113), while a
     * tape entry does not, because the two are separate records.
     */
    val todayIsMeasured: Boolean = false,
) {
    /**
     * Whether today is a **rest day** rather than a day with nothing planned (ROADMAP N126).
     *
     * The distinction the plan surface draws, in one place. A program is followed and schedules nothing
     * today, so the day is *rest* — which is the answer on every day for a weekday-less program, never
     * scheduled and never missed. With no program followed there is no schedule to be resting from, and a
     * line that could not tell the two apart would be wrong on one of them every single day.
     */
    val planIsRestDay: Boolean get() = hasActiveProgram && todaysPlan.isEmpty()
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
    private val measurementRepository: MeasurementRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val activeSession = workoutRepository.observeActiveSession()

    /**
     * The plan whose card is being looked at, or null (ROADMAP N55, N127).
     *
     * Separate from [uiState] because it is a transient question about *one* card: folding it in would
     * rebuild the whole list every time a dialog opens, and the list is what the card sits in.
     */
    private val _plannedWorkout = MutableStateFlow<PlannedWorkout?>(null)
    val plannedWorkout: StateFlow<PlannedWorkout?> = _plannedWorkout.asStateFlow()

    /**
     * Reads what one card has planned, and holds it until the dialog closes (ROADMAP N55, N127).
     *
     * A read per tap rather than a flow per card: the dialog is a look at one plan, and keeping a flow
     * open for every card would be a live query behind a field nobody has tapped. The template is read
     * through the same repository the editor uses, so a rename shows here too — N16's living template,
     * seen from the other end.
     */
    fun onOpenPlannedWorkout(plan: TodayPlan) {
        _plannedWorkout.value = PlannedWorkout(
            plan = plan,
            programName = plan.programName,
        )
        viewModelScope.launch {
            val exercises = templateRepository.observeExercises(plan.templateId).first()
            // Only the card that opened it may fill it in: a dialog closed while the read was in
            // flight must not come back.
            _plannedWorkout.update { current ->
                current?.takeIf { it.plan.id == plan.id }?.copy(
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
     * The zone [todayDate] was read in, captured with it (ROADMAP N114).
     *
     * The row that offers to record today's measurement asks the same question the repository answers when it
     * keys an entry to a day, so it reads the same zone the day was captured in rather than taking a second
     * reading that could cross a boundary the first one did not.
     */
    private val zone: ZoneId = ZoneId.systemDefault()

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

    /** The program-shaped sources the plan surface reads, kept together so the combine below stays flat. */
    private data class ProgramsPart(
        val programs: List<WorkoutProgram>,
        val slots: List<ProgramSlot>,
    )

    private val programsPart: Flow<ProgramsPart> =
        combine(activePrograms, activeProgramSlots) { programs, slots ->
            ProgramsPart(programs, slots)
        }

    val uiState: StateFlow<WorkoutsHomeUiState> = combine(
        activeWorkout,
        templateRepository.observeTemplates(),
        programsPart,
        measurementRepository.observeAll(),
    ) { workout, templates, programs, measurements ->
        WorkoutsHomeUiState(
            isLoading = false,
            activeWorkout = workout,
            today = today,
            // N114: the day the row is about is this screen's own captured one, not a live reading.
            todayIsMeasured = measurements.any { it.isDailyOn(todayDate, zone) },
            todaysPlan = todaysPlanFor(
                programs = programs.programs,
                slots = programs.slots,
                day = today,
            ),
            // N126: what makes an empty day a rest day rather than a day with nothing to say.
            hasActiveProgram = programs.programs.isNotEmpty(),
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
                // Named for the preview's subtitle (N127): a day can carry slots from more than one
                // program (P3.12), and two cards may name the same workout.
                programName = programs.firstOrNull { it.id == slot.programId }?.name,
            )
        }
}
