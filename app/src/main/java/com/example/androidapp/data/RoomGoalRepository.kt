package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.domain.repository.GoalRepository
import com.example.androidapp.domain.repository.storedKind
import com.example.androidapp.domain.repository.storedValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The goal history in Room (ROADMAP N137).
 *
 * A change **opens a period** rather than overwriting one, which is the whole of the feature: the newest row per
 * metric is the goal that is open, and every earlier row is what was asked for before it.
 */
@Singleton
class RoomGoalRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : GoalRepository {

    private val dao = database.metricGoalDao()

    override fun observeCurrent(): Flow<Map<String, GoalPeriod>> =
        dao.observeCurrent().map { rows -> rows.associate { it.metricId to it.toPeriod() } }

    override fun observePeriods(metricId: String): Flow<List<GoalPeriod>> =
        dao.observePeriods(metricId).map { rows -> rows.map { it.toPeriod() } }

    override suspend fun setGoal(metricId: String, goal: WeightGoal?): DataResult<Unit> =
        openPeriod(metricId, kind = goal.storedKind(), value = goal.storedValue())

    override suspend fun setLevelGoal(metricId: String, value: Double?): DataResult<Unit> =
        openPeriod(metricId, kind = null, value = value)

    /**
     * Opens a period, unless the open one already asks for exactly this.
     *
     * That guard is what keeps the history **changes** rather than button presses: pressing *Set* twice with the
     * same number is one period, and a boundary in the history where the lifter asked for the same thing twice
     * would make every later reading of it a puzzle. A read and a write in one transaction, so two writers
     * cannot both decide they are the change.
     */
    private suspend fun openPeriod(metricId: String, kind: String?, value: Double?): DataResult<Unit> =
        dataResultOf {
            database.withTransaction {
                val open = dao.observePeriods(metricId).first().lastOrNull()
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
}

/** The row as the caller reads it: the store's millis become an instant, or stay absent (N137). */
private fun MetricGoalEntity.toPeriod(): GoalPeriod = GoalPeriod(
    startedAt = startedAt?.let { Instant.ofEpochMilli(it) },
    kind = kind,
    value = value,
)
