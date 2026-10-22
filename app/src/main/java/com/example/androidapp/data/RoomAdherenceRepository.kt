package com.example.androidapp.data

import com.example.androidapp.data.local.ExerciseDao
import com.example.androidapp.data.local.ProgramDao
import com.example.androidapp.data.local.ProgramDeloadDao
import com.example.androidapp.data.local.ProgramDeloadEntity
import com.example.androidapp.data.local.ProgramSkipDao
import com.example.androidapp.data.local.ProgramSkipEntity
import com.example.androidapp.data.local.ProgramSubstitutionDao
import com.example.androidapp.data.local.TemplateDao
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toAdherenceSession
import com.example.androidapp.data.local.ProgramSummaryRow
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.AdherenceReport
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.DeloadDay
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.MonthlyRatio
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RecordedDeload
import com.example.androidapp.domain.model.RecordedSkip
import com.example.androidapp.domain.model.RecordedSubstitution
import com.example.androidapp.domain.model.Streak
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.AdherenceRepository
import com.example.androidapp.domain.repository.ProgramRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Room-backed [AdherenceRepository] (ROADMAP P3.5, P3.13).
 *
 * The arithmetic lives in [ProgramSchedule], which is pure and tested without a database; this
 * class only gathers the rows a window needs — the active programs' slots, the week's finished
 * sessions, and the skips, deloads and substitutions recorded against them.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomAdherenceRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val programs: ProgramRepository,
    private val timeSource: TimeSource,
) : AdherenceRepository {

    private val programDao: ProgramDao = database.programDao()

    private val skipDao: ProgramSkipDao = database.programSkipDao()

    private val deloadDao: ProgramDeloadDao = database.programDeloadDao()

    private val substitutionDao: ProgramSubstitutionDao = database.programSubstitutionDao()

    private val templateDao: TemplateDao = database.templateDao()

    private val exerciseDao: ExerciseDao = database.exerciseDao()

    override suspend fun monthAdherence(
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<AdherenceReport> = dataResultOf {
        val firstWeek = ProgramSchedule.weekStartOf(month.atDay(1))
        val lastWeek = ProgramSchedule.weekStartOf(month.atEndOfMonth())

        // The read covers whole weeks rather than the month's days: a slot inside the month can
        // be settled by a session earlier or later in its own week (P3.3), and a skip is keyed by
        // that week's Monday. The day of slack at each end is for sessions performed in another
        // zone, whose week the device clock does not name (N25).
        val from = firstWeek.minusDays(SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = lastWeek.plusDays(DAYS_IN_WEEK + SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = programDao.finishedSessionsBetween(from, to).map { it.toAdherenceSession(zone) }

        val active = programDao.findActivePrograms()
        // Read once, whether or not a program is active: the trained days are still drawn, and a
        // deload week's days are still scheduled (P3.10).
        val deloads = deloadsBetween(firstWeek, lastWeek)

        if (active.isEmpty()) {
            // A trained day needs no schedule, so the calendar is still drawn; there is simply
            // nothing to score it against.
            return@dataResultOf AdherenceReport(
                hasActiveProgram = false,
                adherence = ProgramSchedule.monthAdherence(
                    slots = emptyList(),
                    sessions = sessions,
                    skips = emptyList(),
                    deloads = emptyList(),
                    month = month,
                    today = today,
                ),
            )
        }

        // The union of every active program's slots (P3.12), in the authored order.
        val slots = activeSlots(active)
        val skips = skipDao.findSkipsBetween(firstWeek.toEpochDay(), lastWeek.toEpochDay()).map {
            RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
        }
        // A substituted occurrence is scored against its slot: the day was scheduled, and it was
        // done, whatever it was done with (P3.11).
        val substitutions = substitutionsBetween(firstWeek, lastWeek)
        val activeIds = active.map { it.id }.toSet()
        // The built plans, not just the marks (B161): a plan's own days replace the slots that week, so
        // both the calendar and the day dialog have to read them.
        val planWeeks = deloadWeeksBetween(firstWeek, lastWeek).filter { it.programId in activeIds }

        // The per-lift breakdown's join (P3.14): what each slot's template prescribes, and what
        // those exercises are called. Read once per distinct template, not once per slot.
        val exercisesByTemplate = mutableMapOf<String, List<String>>()
        slots.map { it.templateId }.distinct().forEach { templateId ->
            exercisesByTemplate[templateId] =
                templateDao.findPlannedExercises(templateId).map { it.exerciseId }
        }
        // The breakdown's names and parent links (P3.14, N58, N97), from the **whole** library rather than
        // from the rows a template happens to name. Neither question can be answered from the named rows
        // alone: a category is never named by a template, so its name has to be fetched, and a movement's link
        // to its category is only there if the category is in the map — building it from the named rows made a
        // plan count under the movement while the same movement named elsewhere counted under the category,
        // splitting one family across two rows of one breakdown (B99). Removed rows are included because a
        // removed category still names its children and the walk still passes through it (N58's rule), and it
        // is one read for the lot.
        val library = exerciseDao.findAllIncludingDeleted().associateBy { it.id }
        val exerciseNames = library.mapValues { (_, row) -> row.name }
        val exerciseParents = library.mapValues { (_, row) -> row.parentId }

        AdherenceReport(
            hasActiveProgram = true,
            adherence = ProgramSchedule.monthAdherence(
                slots = slots,
                sessions = sessions,
                skips = skips,
                deloads = deloads,
                deloadWeeks = planWeeks,
                substitutions = substitutions,
                exercisesByTemplate = exercisesByTemplate,
                exerciseNames = exerciseNames,
                exerciseParents = exerciseParents,
                month = month,
                today = today,
            ),
            programs = active.map { it.toDomain() },
            // Only the active programs' weeks: a program no longer followed has nothing to toggle.
            deloadWeeks = deloadWeeksByProgram(deloads, activeIds),
            deloadPlans = deloadPlansByProgram(planWeeks),
        )
    }

    override suspend fun occurrencesOn(
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<DayOccurrence>> = dataResultOf {
        val active = programDao.findActivePrograms()
        if (active.isEmpty()) return@dataResultOf emptyList()

        val weekStart = ProgramSchedule.weekStartOf(date)
        // The same slack as the month's read: a session's own week comes from its own zone (N25),
        // so the day a week was trained can sit a day outside the device's idea of it.
        val from = weekStart.minusDays(SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = weekStart.plusDays(DAYS_IN_WEEK + SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = programDao.finishedSessionsBetween(from, to).map { it.toAdherenceSession(zone) }
        val skips = skipDao.findSkipsForWeek(weekStart.toEpochDay()).map {
            RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
        }

        ProgramSchedule.occurrencesOn(
            slots = activeSlots(active),
            sessions = sessions,
            skips = skips,
            date = date,
            today = today,
            deloads = deloadsBetween(weekStart, weekStart),
            // The plan's own days are what the dialog lists for a built week (B161).
            deloadWeeks = deloadWeeksBetween(weekStart, weekStart)
                .filter { it.programId in active.map { program -> program.id } },
            substitutions = substitutionsBetween(weekStart, weekStart),
        )
    }

    override suspend fun streak(today: LocalDate, zone: ZoneId): DataResult<Streak?> = dataResultOf {
        val active = programDao.findActivePrograms()
        if (active.isEmpty()) return@dataResultOf null

        // The walk cannot start before the earliest active program existed: a slot day before that
        // was not an occurrence (P3.15).
        val created = programDao.earliestActiveProgramCreatedAt() ?: return@dataResultOf null
        val since = ProgramSchedule.weekStartOf(
            Instant.ofEpochMilli(created).atZone(zone).toLocalDate(),
        )
        val lastWeek = ProgramSchedule.weekStartOf(today)
        val from = since.minusDays(SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(DAYS_IN_WEEK + SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()

        ProgramSchedule.streak(
            slots = activeSlots(active),
            sessions = programDao.finishedSessionsBetween(from, to).map { it.toAdherenceSession(zone) },
            skips = skipDao.findSkipsBetween(since.toEpochDay(), lastWeek.toEpochDay()).map {
                RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
            },
            since = since,
            today = today,
            deloads = deloadsBetween(since, lastWeek),
            substitutions = substitutionsBetween(since, lastWeek),
        )
    }

    override suspend fun ratioHistory(
        months: Int,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<MonthlyRatio>> = dataResultOf {
        val last = YearMonth.from(today)
        val first = last.minusMonths((months - 1).toLong())
        val window = (0 until months).map { first.plusMonths(it.toLong()) }

        // One read for the whole window rather than one per month: a slot inside a month can be
        // settled by a session earlier or later in its own week (P3.3), and a skip is keyed by that
        // week's Monday.
        val firstWeek = ProgramSchedule.weekStartOf(first.atDay(1))
        val lastWeek = ProgramSchedule.weekStartOf(last.atEndOfMonth())
        val from = firstWeek.minusDays(SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = lastWeek.plusDays(DAYS_IN_WEEK + SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = programDao.finishedSessionsBetween(from, to).map { it.toAdherenceSession(zone) }

        val active = programDao.findActivePrograms()
        ProgramSchedule.monthlyRatios(
            // No active program means no ratio at all, so every point is a gap, for P3.5's reason.
            slots = if (active.isEmpty()) emptyList() else activeSlots(active),
            sessions = sessions,
            skips = skipDao.findSkipsBetween(firstWeek.toEpochDay(), lastWeek.toEpochDay()).map {
                RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
            },
            months = window,
            today = today,
            deloads = deloadsBetween(firstWeek, lastWeek),
            substitutions = substitutionsBetween(firstWeek, lastWeek),
        )
    }

    override suspend fun setDeloadWeek(
        programId: String,
        weekStart: LocalDate,
        marked: Boolean,
    ): DataResult<Unit> = dataResultOf {
        val epochDay = weekStart.toEpochDay()
        val now = timeSource.nowEpochMillis()
        if (marked) {
            if (programDao.findProgram(programId) == null) throw NotFoundException("program $programId")
            // Idempotent: marking a marked week is not an error and must not double-record.
            if (deloadDao.countDeload(programId, epochDay) == 0) {
                deloadDao.insertDeload(
                    ProgramDeloadEntity(
                        id = UUID.randomUUID().toString(),
                        programId = programId,
                        weekStart = epochDay,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                    ),
                )
            }
        } else {
            // Unmarking an unmarked week is a no-op rather than a failure: the state asked for is
            // the state it is in.
            deloadDao.softDeleteDeload(programId, epochDay, now)
        }
    }

    override suspend fun setOccurrenceSkipped(
        slotId: String,
        weekStart: LocalDate,
        skipped: Boolean,
    ): DataResult<Unit> = dataResultOf {
        val epochDay = weekStart.toEpochDay()
        val now = timeSource.nowEpochMillis()
        if (skipped) {
            if (programDao.findSlot(slotId) == null) throw NotFoundException("program slot $slotId")
            // Idempotent, exactly as the prompt's whole-week write is: marking a marked occurrence
            // must not double-record.
            if (skipDao.countSkips(slotId, epochDay) == 0) {
                skipDao.insertSkip(
                    ProgramSkipEntity(
                        id = UUID.randomUUID().toString(),
                        slotId = slotId,
                        weekStart = epochDay,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                    ),
                )
            }
        } else {
            skipDao.softDeleteSkip(slotId, epochDay, now)
        }
    }

    override fun observeBlockProgress(): Flow<List<BlockProgress>> =
        programs.observePrograms()
            .map { list -> list.filter { it.kind == ProgramKind.SEQUENCE && it.isActive } }
            .flatMapLatest { sequences ->
                if (sequences.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        sequences.map { program ->
                            programs.observeSequenceSchedule(program.id)
                                .map { schedule -> program.name to schedule }
                        },
                    ) { pairs ->
                        pairs.mapNotNull { (name, schedule) ->
                            schedule?.let {
                                BlockProgress(
                                    name = name,
                                    position = it.position,
                                    length = it.length,
                                    complete = it.isComplete,
                                )
                            }
                        }
                    }
                }
            }

    /** The union of the active programs' slots, in the authored order (P3.12). */
    /**
     * Every followed program's slots, **except a sequence's** (ROADMAP N176).
     *
     * The ratio, the grid, the breakdown and the streak all count a slot's weekday as a weekly
     * occurrence, and a block's weekday places one session rather than scheduling a recurring one —
     * scored here, every Monday of a nine-session block would count as a scheduled day forever. A
     * sequence is a different question with its own answer, which is its progress through the block.
     */
    private suspend fun activeSlots(programs: List<ProgramSummaryRow>): List<ProgramSlot> {
        val slots = mutableListOf<ProgramSlot>()
        programs.filter { it.kind != ProgramKind.SEQUENCE }.forEach { program ->
            slots += programDao.findSlotDetails(program.id).map { it.toDomain() }
        }
        return slots
    }

    /** Which weeks each active program marked, by program id (P3.10, B161). */
    private fun deloadWeeksByProgram(
        deloads: List<RecordedDeload>,
        activeIds: Set<String>,
    ): Map<String, Set<LocalDate>> = deloads
        .filter { it.programId in activeIds }
        .groupBy({ it.programId }, { it.weekStart })
        .mapValues { (_, weeks) -> weeks.toSet() }

    /** Which of those weeks have a built plan, by program id (B161, B162, N148). */
    private fun deloadPlansByProgram(plans: List<DeloadWeek>): Map<String, Set<LocalDate>> = plans
        .filter { it.days.isNotEmpty() }
        .groupBy({ it.programId }, { it.weekStart })
        .mapValues { (_, weeks) -> weeks.toSet() }

    /** The built plans in the window, with their days, for the calendar and the day dialog (B161). */
    private suspend fun deloadWeeksBetween(from: LocalDate, to: LocalDate): List<DeloadWeek> =
        deloadDao.findDeloadDaysBetween(from.toEpochDay(), to.toEpochDay())
            .groupBy { it.programId to it.weekStart }
            .map { (key, rows) ->
                DeloadWeek(
                    programId = key.first,
                    weekStart = LocalDate.ofEpochDay(key.second),
                    days = rows.map {
                        DeloadDay(
                            id = it.id,
                            programId = it.programId,
                            templateId = it.templateId,
                            templateName = it.templateName,
                            exerciseCount = it.exerciseCount,
                            position = it.position,
                            weekday = it.weekday,
                        )
                    },
                )
            }

    private suspend fun deloadsBetween(from: LocalDate, to: LocalDate): List<RecordedDeload> =
        deloadDao.findDeloadsBetween(from.toEpochDay(), to.toEpochDay()).map {
            RecordedDeload(programId = it.programId, weekStart = LocalDate.ofEpochDay(it.weekStart))
        }

    private suspend fun substitutionsBetween(from: LocalDate, to: LocalDate): List<RecordedSubstitution> =
        substitutionDao.findSubstitutionsBetween(from.toEpochDay(), to.toEpochDay()).map {
            RecordedSubstitution(
                slotId = it.slotId,
                weekStart = LocalDate.ofEpochDay(it.weekStart),
                templateId = it.templateId,
            )
        }

    private companion object {
        /** One day of slack at each end of the week, for sessions in another zone. */
        const val SLACK_DAYS = 1L

        const val DAYS_IN_WEEK = 7L
    }
}
