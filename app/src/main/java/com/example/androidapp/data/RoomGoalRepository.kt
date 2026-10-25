package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.GoalBoundary
import com.example.androidapp.data.local.GoalTimeline
import com.example.androidapp.data.local.MetricGoalDao
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.domain.repository.GoalRepository
import com.example.androidapp.domain.repository.storedKind
import com.example.androidapp.domain.repository.storedValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The goal history in Room (ROADMAP N137, N215).
 *
 * A change **opens a period** rather than overwriting one, which is the whole of the feature: the newest row per
 * metric is the goal that is open, and every earlier row is what was asked for before it. Since N215 the lifter
 * may also **correct** the timeline — add a stretch that began in the past, edit one, remove one — which is the
 * one thing the append-only rule could not do, and why a period carries its id now.
 *
 * **Days, not moments.** A boundary is matched and ordered by its local day, so a row the old `setGoal` path
 * wrote at a clock time and one N215 writes at midnight are the same statement. A goal-only correction keeps the
 * stored instant; a moved or added boundary is local midnight. `setLevelGoal` keeps the clock because a metric's
 * single number has no editor yet.
 */
@Singleton
class RoomGoalRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : GoalRepository {

    private val dao: MetricGoalDao = database.metricGoalDao()

    override fun observeCurrent(): Flow<Map<String, GoalPeriod>> =
        dao.observeCurrent().map { rows -> rows.associate { it.metricId to it.toPeriod() } }

    override fun observePeriods(metricId: String): Flow<List<GoalPeriod>> =
        dao.observePeriods(metricId).map { rows -> rows.map { it.toPeriod() } }

    override suspend fun setLevelGoal(metricId: String, value: Double?): DataResult<Unit> =
        openPeriod(metricId, kind = null, value = value)

    /**
     * Writes one period, or edits the one [id] names (ROADMAP N215).
     *
     * [start] may be null only for the undated first period — the row the N137 migration creates has no
     * recorded beginning, and a goal-only correction must not invent one (B232). A new period needs a day.
     *
     * The guesses a caller would otherwise make are refused here, in one transaction: the day is not in the
     * future, the end follows the start, a start does not land on a boundary another period owns, and an end does
     * not pass the boundary after it. A start that lands on a boundary **edits** that boundary — the same rule
     * that makes setting a goal twice in one day one statement.
     */
    override suspend fun savePeriod(
        metricId: String,
        id: String?,
        start: LocalDate?,
        end: LocalDate?,
        goal: WeightGoal?,
    ): DataResult<Unit> = dataResultOf {
        val zone = ZoneId.systemDefault()
        val today = timeSource.now().atZone(zone).toLocalDate()
        validatePeriod(start, end, today)
        val startMillis = start?.let { GoalTimeline.startOfDay(it, zone) }
        val endMillis = end?.let { GoalTimeline.startOfDay(it, zone) }
        database.withTransaction {
            val write = PeriodWrite(
                metricId = metricId,
                start = startMillis,
                startDay = start,
                end = endMillis,
                endDay = end,
                goal = goal,
                zone = zone,
                now = timeSource.nowEpochMillis(),
            )
            val periods = dao.periodsFor(metricId)
            val named = id?.let {
                val row = dao.findById(it) ?: throw NotFoundException("goal period $it")
                if (row.metricId != metricId) throw NotFoundException("goal period $it")
                row
            }
            val existing = existingPeriod(named, periods, start, zone)
            when {
                existing == null -> insertPeriod(periods, write)
                // A null start is the undated period's "keep it unknown"; a dated row cannot lose its day.
                start == null && existing.startedAt != null -> throw InvalidInputException(KEEP_UNKNOWN_ONLY)
                else -> editPeriod(existing, periods, write)
            }
        }
    }

    /** The day itself is not a record, and an end that is not after its start would write an empty stretch. */
    private fun validatePeriod(start: LocalDate?, end: LocalDate?, today: LocalDate) {
        val problem = when {
            start != null && start.isAfter(today) -> START_AFTER_TODAY
            end != null && start != null && !end.isAfter(start) -> END_BEFORE_START
            end != null && end.isAfter(today) -> END_AFTER_TODAY
            else -> null
        }
        if (problem != null) throw InvalidInputException(problem)
    }

    /**
     * The row a write edits: the named one, or the day's boundary.
     *
     * A day that already carries a boundary is that boundary, whether the row was written at midnight or at a
     * clock time by the old path (B223). If a legacy day holds more than one row, the newest is the one in force,
     * so that is the one a new statement edits.
     */
    private fun existingPeriod(
        named: MetricGoalEntity?,
        periods: List<MetricGoalEntity>,
        start: LocalDate?,
        zone: ZoneId,
    ): MetricGoalEntity? = when {
        named != null -> named
        start == null -> throw InvalidInputException(START_REQUIRED)
        else -> periods.lastOrNull { GoalTimeline.startsOn(it.startedAt, start, zone) }
    }

    override suspend fun removePeriod(id: String): DataResult<Unit> = dataResultOf {
        if (dao.delete(id) == 0) throw NotFoundException("goal period $id")
    }

    override suspend fun restorePeriod(metricId: String, period: GoalPeriod): DataResult<Unit> = dataResultOf {
        val now = timeSource.nowEpochMillis()
        val zone = ZoneId.systemDefault()
        database.withTransaction {
            val day = period.startedAt?.let { GoalTimeline.dayOf(it.toEpochMilli(), zone) }
            // The undo must not lay a second boundary on a day the timeline has since taken: a save while the
            // snackbar was up is a newer statement than the row being put back (B233).
            val taken = dao.periodsFor(metricId).any { row ->
                row.id != period.id && if (day == null) {
                    row.startedAt == null
                } else {
                    GoalTimeline.startsOn(row.startedAt, day, zone)
                }
            }
            if (taken) throw InvalidInputException(RESTORE_TAKEN)
            // The id travels, so the row this puts back is the one that went; an undo repeated is a no-op.
            dao.restore(
                MetricGoalEntity(
                    id = period.id,
                    metricId = metricId,
                    kind = period.kind,
                    value = period.value,
                    startedAt = period.startedAt?.toEpochMilli(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /**
     * Opens a level goal's period, unless the open one already asks for exactly this (B140).
     *
     * That guard is what keeps the history **changes** rather than button presses: setting the same number twice
     * is one period. A read and a write in one transaction, so two writers cannot both decide they are the
     * change.
     */
    private suspend fun openPeriod(metricId: String, kind: String?, value: Double?): DataResult<Unit> =
        dataResultOf {
            database.withTransaction {
                val open = dao.findOpen(metricId)
                if (open != null && open.kind == kind && open.value == value) return@withTransaction
                val now = timeSource.nowEpochMillis()
                dao.insert(
                    MetricGoalEntity(
                        id = UUID.randomUUID().toString(),
                        metricId = metricId,
                        kind = kind,
                        value = value,
                        startedAt = now,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }

    /**
     * Writes a new boundary, and the one restoring row an end implies (ROADMAP N215).
     *
     * The restoring row is **the goal that covered the end before this write**, not a new statement: if the
     * lifter records a cut from March to June, the goal that was in force in June resumes then. When no row covers
     * the end at all, the restoring row states **no goal** — a period cannot end into nothing.
     */
    private suspend fun insertPeriod(periods: List<MetricGoalEntity>, write: PeriodWrite) {
        val start = write.start ?: throw InvalidInputException(START_REQUIRED)
        val startDay = write.startDay ?: throw InvalidInputException(START_REQUIRED)
        val boundaries = periods.map { it.toBoundary() }
        // B140's guard: a period that restates the goal in force asks for nothing (B224). A finished
        // stretch is different — the end is the statement — so only an open-ended write is a no-op.
        val governing = GoalTimeline.governing(boundaries, startDay, write.zone)
        if (write.end == null && governing != null &&
            GoalTimeline.sameGoal(
                write.goal.storedKind(),
                write.goal.storedValue(),
                governing.kind,
                governing.value,
            )
        ) {
            return
        }
        val after = periods.firstOrNull { it.startedAt != null && it.startedAt > start }
        val afterDay = after?.startedAt?.let { GoalTimeline.dayOf(it, write.zone) }
        val problem = when {
            write.endDay == null || afterDay == null -> null
            write.endDay > afterDay -> END_PASSES_NEXT
            else -> null
        }
        refuse(problem)
        val restoring = restrictedTo(write.end, periods)
        dao.insert(
            MetricGoalEntity(
                id = UUID.randomUUID().toString(),
                metricId = write.metricId,
                kind = write.goal.storedKind(),
                value = write.goal.storedValue(),
                startedAt = start,
                createdAt = write.now,
                updatedAt = write.now,
            ),
        )
        // A boundary already on the end means the period that starts there is what follows; otherwise the goal
        // that covered the end resumes, which is one row rather than a gap.
        if (write.end != null && afterDay != write.endDay) {
            dao.insert(
                MetricGoalEntity(
                    id = UUID.randomUUID().toString(),
                    metricId = write.metricId,
                    kind = restoring?.kind,
                    value = restoring?.value,
                    startedAt = write.end,
                    createdAt = write.now,
                    updatedAt = write.now,
                ),
            )
        }
    }

    /**
     * Rewrites a row in place, and moves the boundary after it when an end is given (ROADMAP N215).
     *
     * The row keeps its **place**: its start has to sit between the periods around it, so a correction cannot
     * silently reorder the history it is correcting. An end on the newest period is refused — nothing would
     * follow it, and a goal still in force has no end.
     */
    private suspend fun editPeriod(
        row: MetricGoalEntity,
        periods: List<MetricGoalEntity>,
        write: PeriodWrite,
    ) {
        val index = periods.indexOfFirst { it.id == row.id }
        refuse(startProblem(row, periods, index, write.startDay, write.zone))
        refuse(endProblem(periods, index, write.endDay, write.zone))
        val next = periods.getOrNull(index + 1)
        val nextOnEndDay = nextStartsOn(next, write)
        val effectiveStart = effectiveStart(row, write)
        val unchanged = effectiveStart == row.startedAt && row.kind == write.goal.storedKind() &&
            row.value == write.goal.storedValue() && (write.end == null || nextOnEndDay)
        if (unchanged) return
        dao.update(
            row.copy(
                kind = write.goal.storedKind(),
                value = write.goal.storedValue(),
                startedAt = effectiveStart,
                updatedAt = write.now,
            ),
        )
        if (write.end != null && next != null && !nextOnEndDay) {
            dao.update(next.copy(startedAt = write.end, updatedAt = write.now))
        }
    }

    /** Whether the row after an edited period already begins on the end it names. */
    private fun nextStartsOn(next: MetricGoalEntity?, write: PeriodWrite): Boolean =
        next?.startedAt != null && write.endDay != null &&
            GoalTimeline.startsOn(next.startedAt, write.endDay, write.zone)

    /**
     * The instant the edited row keeps: its own when the day did not move, otherwise the chosen midnight.
     *
     * A goal-only correction keeps the day's original instant — moving a clock-time boundary to midnight would
     * hand this period the readings earlier on that day (B223). A null start is the undated row's "keep it
     * unknown" (B232).
     */
    private fun effectiveStart(row: MetricGoalEntity, write: PeriodWrite): Long? = when {
        write.start == null -> null
        write.startDay == null -> write.start
        else -> row.startedAt?.takeIf { GoalTimeline.startsOn(it, write.startDay, write.zone) } ?: write.start
    }

    /** Why a period's start cannot sit where it was put, or null. */
    private fun startProblem(
        row: MetricGoalEntity,
        periods: List<MetricGoalEntity>,
        index: Int,
        startDay: LocalDate?,
        zone: ZoneId,
    ): String? {
        if (startDay == null) return null
        val before = periods.getOrNull(index - 1)
        val next = periods.getOrNull(index + 1)
        // A goal-only correction keeps the row's own day. Two rows written by the old path can share a day,
        // and the row is still the one being corrected; only a *moved* start has to find a day of its own.
        val keepsItsDay = GoalTimeline.startsOn(row.startedAt, startDay, zone)
        return when {
            !keepsItsDay &&
                GoalTimeline.isTaken(periods.map { it.toBoundary() }, row.id, startDay, zone) -> BOUNDARY_TAKEN
            !keepsItsDay &&
                before?.startedAt != null && GoalTimeline.dayOf(before.startedAt, zone) >= startDay -> START_OUTSIDE
            !keepsItsDay &&
                next?.startedAt != null && GoalTimeline.dayOf(next.startedAt, zone) <= startDay -> START_OUTSIDE
            else -> null
        }
    }

    /** Why a period's end cannot sit where it was put, or null. */
    private fun endProblem(periods: List<MetricGoalEntity>, index: Int, endDay: LocalDate?, zone: ZoneId): String? {
        val next = periods.getOrNull(index + 1)
        // The boundary after the next one is what an end may not swallow; the next boundary itself is what the
        // end *is*, so moving it either way is the correction.
        val afterNext = periods.getOrNull(index + 2)
        return when {
            endDay == null -> null
            next == null -> OPEN_HAS_NO_END
            afterNext?.startedAt != null && GoalTimeline.dayOf(afterNext.startedAt, zone) <= endDay -> END_PASSES_NEXT
            else -> null
        }
    }

    /** The goal that covered [end] before this write, or null when none did (a period of no goal). */
    private fun restrictedTo(end: Long?, periods: List<MetricGoalEntity>): MetricGoalEntity? =
        end?.let { at -> periods.lastOrNull { it.startedAt == null || it.startedAt < at } }

    /** The first thing wrong is the sentence the lifter reads, so the refusal is one place. */
    private fun refuse(problem: String?) {
        if (problem != null) throw InvalidInputException(problem)
    }
}

/** One write, as the caller asked for it: the days, the optional end, the goal, and the moment to stamp (N215). */
private data class PeriodWrite(
    val metricId: String,
    val start: Long?,
    val startDay: LocalDate?,
    val end: Long?,
    val endDay: LocalDate?,
    val goal: WeightGoal?,
    val zone: ZoneId,
    val now: Long,
)

/** A row as the day rules read it, without Room (B223, B238). */
private fun MetricGoalEntity.toBoundary(): GoalBoundary =
    GoalBoundary(id = id, startedAt = startedAt, kind = kind, value = value)

/** The row as the caller reads it: the store's millis become an instant, or stay absent (N137); the id travels. */
private fun MetricGoalEntity.toPeriod(): GoalPeriod = GoalPeriod(
    id = id,
    startedAt = startedAt?.let { Instant.ofEpochMilli(it) },
    kind = kind,
    value = value,
)

private const val START_AFTER_TODAY = "A goal cannot start after today."

private const val END_AFTER_TODAY = "A goal cannot end after today."

private const val END_BEFORE_START = "A goal must end after it starts."

private const val BOUNDARY_TAKEN = "Another goal already starts that day."

private const val START_OUTSIDE = "That start is outside the periods either side of it."

private const val END_PASSES_NEXT = "Another goal starts before that end."

private const val OPEN_HAS_NO_END = "A goal still in force has no end; add what came after it instead."

private const val START_REQUIRED = "A new goal period needs a start day."

private const val KEEP_UNKNOWN_ONLY = "A period with a start cannot lose it."

private const val RESTORE_TAKEN = "That goal period's day is already taken."
