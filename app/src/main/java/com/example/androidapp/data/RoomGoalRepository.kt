package com.example.androidapp.data

import androidx.room.withTransaction
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
 * **Days, not moments.** An edited or added boundary is local midnight of the day chosen, because a goal is a
 * statement about days; `setLevelGoal` keeps the clock because a metric's single number has no editor yet.
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
     * The guesses a caller would otherwise make are refused here, in one transaction: the day is not in the
     * future, the end follows the start, a start does not land on a boundary another period owns, and an end does
     * not pass the boundary after it. A start that lands on a boundary **edits** that boundary — the same rule
     * that makes setting a goal twice in one day one statement.
     */
    override suspend fun savePeriod(
        metricId: String,
        id: String?,
        start: LocalDate,
        end: LocalDate?,
        goal: WeightGoal?,
    ): DataResult<Unit> = dataResultOf {
        val zone = ZoneId.systemDefault()
        val today = timeSource.now().atZone(zone).toLocalDate()
        if (start.isAfter(today)) throw InvalidInputException(START_AFTER_TODAY)
        if (end != null) {
            if (!end.isAfter(start)) throw InvalidInputException(END_BEFORE_START)
            if (end.isAfter(today)) throw InvalidInputException(END_AFTER_TODAY)
        }
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        database.withTransaction {
            val write = PeriodWrite(metricId, startMillis, endMillis, goal, timeSource.nowEpochMillis())
            val periods = dao.periodsFor(metricId)
            val named = id?.let {
                val row = dao.findById(it) ?: throw NotFoundException("goal period $it")
                if (row.metricId != metricId) throw NotFoundException("goal period $it")
                row
            }
            // A day that already carries a boundary is that boundary, not a second one at the same instant.
            when (val existing = named ?: periods.firstOrNull { it.startedAt == startMillis }) {
                null -> insertPeriod(periods, write)
                else -> editPeriod(existing, periods, write)
            }
        }
    }

    override suspend fun removePeriod(id: String): DataResult<Unit> = dataResultOf {
        if (dao.delete(id) == 0) throw NotFoundException("goal period $id")
    }

    override suspend fun restorePeriod(metricId: String, period: GoalPeriod): DataResult<Unit> = dataResultOf {
        val now = timeSource.nowEpochMillis()
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
        val after = periods.firstOrNull { it.startedAt != null && it.startedAt > write.start }
        val problem = when {
            write.end == null || after?.startedAt == null -> null
            write.end > after.startedAt -> END_PASSES_NEXT
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
                startedAt = write.start,
                createdAt = write.now,
                updatedAt = write.now,
            ),
        )
        // A boundary already on the end means the period that starts there is what follows; otherwise the goal
        // that covered the end resumes, which is one row rather than a gap.
        if (write.end != null && after?.startedAt != write.end) {
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
        refuse(startProblem(row, periods, index, write.start))
        refuse(endProblem(periods, index, write.end))
        val next = periods.getOrNull(index + 1)
        val unchanged = row.startedAt == write.start && row.kind == write.goal.storedKind() &&
            row.value == write.goal.storedValue() && (write.end == null || write.end == next?.startedAt)
        if (unchanged) return
        dao.update(
            row.copy(
                kind = write.goal.storedKind(),
                value = write.goal.storedValue(),
                startedAt = write.start,
                updatedAt = write.now,
            ),
        )
        if (write.end != null && next != null && next.startedAt != write.end) {
            dao.update(next.copy(startedAt = write.end, updatedAt = write.now))
        }
    }

    /** Why a period's start cannot sit where it was put, or null. */
    private fun startProblem(
        row: MetricGoalEntity,
        periods: List<MetricGoalEntity>,
        index: Int,
        start: Long,
    ): String? {
        val before = periods.getOrNull(index - 1)
        val next = periods.getOrNull(index + 1)
        return when {
            periods.any { it.id != row.id && it.startedAt == start } -> BOUNDARY_TAKEN
            before?.startedAt != null && start <= before.startedAt -> START_OUTSIDE
            next?.startedAt != null && start >= next.startedAt -> START_OUTSIDE
            else -> null
        }
    }

    /** Why a period's end cannot sit where it was put, or null. */
    private fun endProblem(periods: List<MetricGoalEntity>, index: Int, end: Long?): String? {
        val next = periods.getOrNull(index + 1)
        // The boundary after the next one is what an end may not swallow; the next boundary itself is what the
        // end *is*, so moving it either way is the correction.
        val afterNext = periods.getOrNull(index + 2)
        return when {
            end == null -> null
            next == null -> OPEN_HAS_NO_END
            afterNext?.startedAt != null && end >= afterNext.startedAt -> END_PASSES_NEXT
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

/** One write, as the caller asked for it: the day, the optional end, the goal, and the moment to stamp (N215). */
private data class PeriodWrite(
    val metricId: String,
    val start: Long,
    val end: Long?,
    val goal: WeightGoal?,
    val now: Long,
)

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
