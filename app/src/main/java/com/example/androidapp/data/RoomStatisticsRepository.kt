package com.example.androidapp.data

import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toPlannedSetTarget
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.planTargets
import com.example.androidapp.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Statistics over Room (ROADMAP N35). Thin: the rule lives in the query, where it can see all the rows. */
@Singleton
class RoomStatisticsRepository @Inject constructor(
    database: WorkoutDatabase,
) : StatisticsRepository {

    private val dao = database.statisticsDao()
    private val workouts = database.workoutDao()

    override fun observeWorkoutSummaries(): Flow<List<WorkoutSummary>> =
        workouts.observeHistory().map { rows -> rows.map { it.toDomain() } }

    override fun observePlanTargets(exerciseIds: List<String>): Flow<List<PlanTarget>> =
        dao.observePlanTargets(exerciseIds).map { rows -> planTargets(rows.map { it.toPlannedSetTarget() }) }

    override suspend fun countRecordsIn(from: Instant, to: Instant): DataResult<Int> = dataResultOf {
        // **The roles come from the rule, not from the query** (B155): `SetType.recordsEffort` is the one place
        // that says what may be a record, and the live announce, `PersonalRecords` and the editors all read it.
        // The SQL naming `'WARMUP'` alone was a second copy that had drifted, so a drop or cluster rung was
        // counted as a record the lifter was never told about.
        dao.countRecordsIn(
            from = from.toEpochMilli(),
            to = to.toEpochMilli(),
            recordableRoles = SetType.entries.filter { it.recordsEffort }.map { it.name },
        )
    }
}
