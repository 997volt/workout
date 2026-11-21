package com.example.androidapp.data

import com.example.androidapp.data.local.SessionPlanSetDao
import com.example.androidapp.data.local.SessionPlanSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TrainingDay
import com.example.androidapp.domain.model.TrainingExercise
import com.example.androidapp.domain.model.TrainingMeasurement
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.model.TrainingSession
import com.example.androidapp.domain.model.TrainingSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.repository.TrainingBlockChoice
import com.example.androidapp.domain.repository.TrainingReportRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Assembles the last training block for the CSV export (ROADMAP N331).
 *
 * Read-only, and composed from the repositories the screens already use rather than a second query
 * layer: one assembly path is easier to keep correct than four SQL reads that must agree about plans,
 * exercises and units.
 *
 * **It does read the whole history and every measurement before filtering** (N354): the window is
 * applied in Kotlin, which is fine for a user-initiated one-off export of a block and is not what the
 * first version of this comment claimed. A bounded read — a `startedAt` range on the history row and
 * a `measuredAt` range on the measurements — is the next step if the export ever has to run over a
 * large library; it was not taken here because it means a second history projection beside the
 * existing one.
 */
class RoomTrainingReportRepository @Inject constructor(
    database: WorkoutDatabase,
    private val programs: ProgramRepository,
    private val workouts: WorkoutRepository,
    private val measurements: MeasurementRepository,
    private val settings: SettingsRepository,
    private val timeSource: TimeSource,
) : TrainingReportRepository {

    private val planSets: SessionPlanSetDao = database.sessionPlanSetDao()

    override suspend fun lastBlock(fallbackName: String): DataResult<TrainingReport> = dataResultOf {
        val sequence = programs.observePrograms().first()
            .filter { it.kind == ProgramKind.SEQUENCE }
            .maxByOrNull { it.startDate ?: LocalDate.MIN }
        if (sequence == null) {
            val today = today()
            report(
                name = fallbackName,
                start = today.minusWeeks(FALLBACK_WEEKS),
                end = today,
                complete = false,
                program = null,
            )
        } else {
            blockReport(sequence)
        }
    }

    override suspend fun block(programId: String): DataResult<TrainingReport> = dataResultOf {
        val program = programs.observePrograms().first().firstOrNull { it.id == programId }
            ?: throw NotFoundException("program $programId")
        blockReport(program)
    }

    override suspend fun range(
        start: LocalDate,
        end: LocalDate,
        name: String,
    ): DataResult<TrainingReport> = dataResultOf {
        report(name = name, start = start, end = end, complete = false, program = null)
    }

    override suspend fun recentBlocks(): DataResult<List<TrainingBlockChoice>> = dataResultOf {
        programs.observePrograms().first()
            .filter { it.kind == ProgramKind.SEQUENCE }
            .sortedByDescending { it.startDate ?: LocalDate.MIN }
            .take(RECENT_BLOCK_COUNT)
            .map { TrainingBlockChoice(it.id, it.name, it.startDate ?: today()) }
    }

    private suspend fun blockReport(program: WorkoutProgram): TrainingReport {
        val today = today()
        val start = blockStart(program, today, ZoneId.systemDefault())
        // A complete block ends when it last moved, not when the device last trained (N333): the
        // schedule is the thing that knows which sessions settled this block, and its last settled
        // day is where the block stopped. Reading history here would carry every later workout.
        val schedule = programs.observeSequenceSchedule(program.id).first()
        val complete = schedule?.isComplete == true
        val end = if (complete) schedule?.lastSettled ?: start else today
        return report(
            name = program.name,
            start = start,
            end = end,
            complete = complete,
            program = program,
        )
    }

    private suspend fun report(
        name: String,
        start: LocalDate,
        end: LocalDate,
        complete: Boolean,
        program: WorkoutProgram?,
    ): TrainingReport {
        val unit = settings.observeWeightUnit().first()
        val slots = program?.let { programs.observeSlots(it.id).first() }.orEmpty()
        // One calendar for the window (N349): the schedule that produced the block's dates, the
        // measurements below and "today" all bucket by the device's current zone. The session's own
        // offset still labels its CSV row; it does not decide whether the session is in the block.
        val zone = ZoneId.systemDefault()
        val history = workouts.observeHistory().first()
            .filter { it.finishedAt != null && it.startedAt.atZone(zone).toLocalDate() in start..end }
            .sortedBy { it.startedAt }
        val rawSessions = history.mapNotNull { summary -> buildSession(summary, unit) }
        // Resolve the days before naming them (N340): a plan may name one template on more than one
        // day, and the session that stood for day 3 is the next unmatched one, not the first match.
        val daySessions = sessionsByDay(slots, rawSessions)
        val slotAtPosition = slots.associateBy { it.position }
        val dayNameBySession = daySessions.entries.associate { (position, sessionId) ->
            sessionId to slotAtPosition.getValue(position).templateName
        }
        val sessions = rawSessions.map { it.copy(dayName = dayNameBySession[it.id]) }
        val days = slots.sortedBy { it.position }.map { slot ->
            TrainingDay(
                name = slot.templateName,
                position = slot.position,
                sessionId = daySessions[slot.position],
            )
        }
        val body = measurements.observeAll().first()
            .filter { it.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate() in start..end }
            .map { it.toTrainingMeasurement() }
        return TrainingReport(
            blockName = name,
            blockStart = start,
            blockEnd = end,
            blockComplete = complete,
            exportedAt = Instant.ofEpochMilli(timeSource.nowEpochMillis()),
            unit = unit,
            days = days,
            sessions = sessions,
            measurements = body,
        )
    }

    private suspend fun buildSession(
        summary: WorkoutSummary,
        unit: WeightUnit,
    ): TrainingSession? {
        val session = workouts.observeSession(summary.id).first() ?: return null
        val exercises = workouts.observeSessionExercises(summary.id).first()
        val sets = workouts.observeSets(summary.id).first()
        val plans = planSets.findFor(exercises.map { it.id }).groupBy { it.sessionExerciseId }
        val templateId = session.templateId
        return TrainingSession(
            id = summary.id,
            workout = summary.templateName ?: session.activity?.name ?: "Workout",
            templateId = templateId,
            startedAt = summary.startedAt,
            zoneOffsetMinutes = summary.zoneOffsetMinutes,
            finishedAt = summary.finishedAt ?: session.finishedAt ?: summary.startedAt,
            comment = session.notes,
            readinessNote = session.readinessNote,
            soreMuscles = session.soreMuscles,
            activity = session.activity,
            activityDurationSeconds = session.activityDurationSeconds,
            activityDistanceMeters = session.activityDistanceMeters,
            exercises = exercises.map { exercise ->
                TrainingExercise(
                    name = exercise.exerciseName,
                    sets = setsFor(
                        plans = plans[exercise.id].orEmpty(),
                        actual = sets.filter { it.sessionExerciseId == exercise.id },
                        unit = exercise.weightUnit ?: unit,
                    ),
                    joints = exercise.joints,
                )
            },
        )
    }

    private fun setsFor(
        plans: List<SessionPlanSetEntity>,
        actual: List<SetEntry>,
        unit: WeightUnit,
    ): List<TrainingSet> {
        val planByIndex = plans.associateBy { it.setIndex }
        val actualByIndex = actual.associateBy { it.setIndex }
        return (planByIndex.keys + actualByIndex.keys).sorted().map { index ->
            val plan = planByIndex[index]
            val performed = actualByIndex[index]
            TrainingSet(
                setIndex = index,
                performed = performed != null,
                role = performed?.setType ?: plan?.role ?: SetType.NORMAL,
                unit = unit,
                reps = performed?.reps ?: 0,
                durationSeconds = performed?.durationSeconds,
                weightGrams = performed?.weightGrams ?: 0L,
                assistanceGrams = performed?.assistanceGrams ?: 0L,
                bodyweightGrams = performed?.bodyweightGrams,
                rpeHalves = performed?.rpeHalves,
                comment = performed?.note,
                plannedRole = plan?.role,
                plannedReps = plan?.let {
                    it.targetRepsCurrent ?: it.targetRepsMax ?: it.targetRepsMin
                },
                plannedWeightGrams = plan?.targetWeightGrams,
                plannedAssistanceGrams = plan?.targetAssistanceGrams,
                plannedDurationSeconds = plan?.targetDurationSeconds,
                plannedRpeHalves = plan?.targetRpeHalves,
            )
        }
    }

    private fun BodyMeasurement.toTrainingMeasurement(): TrainingMeasurement = TrainingMeasurement(
        measuredAt = measuredAt,
        weightGrams = weightGrams,
        bodyFatTenths = bodyFatTenths,
        muscleTenths = muscleTenths,
        tape = tape,
        soreMuscles = soreMuscles,
    )

    private fun today(): LocalDate =
        Instant.ofEpochMilli(timeSource.nowEpochMillis()).atZone(ZoneId.systemDefault()).toLocalDate()

    private companion object {
        const val FALLBACK_WEEKS = 8L
        const val RECENT_BLOCK_COUNT = 5
    }
}

/**
 * The session that settled each block day, keyed by the day's position (N340).
 *
 * The walk is the sequence schedule's own (N175): days in authored order, each taking the first
 * not-yet-used session naming its template. A program may name the same template on two days — an
 * A/B/A block is ordinary — and the first-match lookup this replaces pointed both A days at the
 * first A session, leaving the second one unrepresented. Sessions the block does not name, and
 * sessions trained after the block settled, stay unassigned; the caller keeps them as unplanned.
 *
 * A day missing from the map was not trained.
 */
internal fun sessionsByDay(
    slots: List<ProgramSlot>,
    sessions: List<TrainingSession>,
): Map<Int, String> {
    val remaining = sessions.sortedBy { it.startedAt }.toMutableList()
    return buildMap {
        slots.sortedBy { it.position }.forEach { slot ->
            val session = remaining.firstOrNull { it.templateId == slot.templateId } ?: return@forEach
            remaining.remove(session)
            put(slot.position, session.id)
        }
    }
}

/**
 * The day a block begins: its authored start, else the day it was created, else today (N350).
 *
 * A sequence program can have no start date — the model documents null as "the day it was created" —
 * and reading that as today made a legacy block's window empty: start and end on the same day, no
 * sessions, no measurements. The sequence schedule already falls back to the creation date, so the
 * report uses the same rule rather than inventing a second one.
 */
internal fun blockStart(program: WorkoutProgram, today: LocalDate, zone: ZoneId): LocalDate {
    program.startDate?.let { return it }
    if (program.createdAtMillis > 0L) {
        return Instant.ofEpochMilli(program.createdAtMillis).atZone(zone).toLocalDate()
    }
    return today
}
