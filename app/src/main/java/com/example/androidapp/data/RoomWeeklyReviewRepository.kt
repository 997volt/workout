package com.example.androidapp.data

import com.example.androidapp.data.local.WeeklyMovementRow
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.AdherenceRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.WeeklyReviewRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed week gains (ROADMAP N251). */
@Singleton
class RoomWeeklyReviewRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val workouts: WorkoutRepository,
    private val adherence: AdherenceRepository,
    private val programs: ProgramRepository,
    private val stalls: PlanStallFinder,
) : WeeklyReviewRepository {

    override suspend fun movementGains(
        weekStart: LocalDate,
        zone: ZoneId,
    ): DataResult<List<MovementGain>> = dataResultOf {
        val from = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = weekStart.plusDays(DAYS_IN_WEEK).atStartOfDay(zone).toInstant().toEpochMilli()
        // The roles come from the rule, not from the query (B17, N79, B331): `recordsEffort` is the one place
        // that decides what may be a best, and the SQL gets the stored names it compares against.
        val recordableRoles = SetType.entries.filter { it.recordsEffort }.map { it.name }
        database.weeklyReviewDao().movementGains(from, to, recordableRoles)
            .mapNotNull { it.toGainOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun history(): DataResult<List<WorkoutSummary>> = dataResultOf {
        workouts.observeHistory().first()
    }

    override suspend fun occurrencesOn(
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<DayOccurrence>> = adherence.occurrencesOn(date, today, zone)

    override suspend fun activePrograms(): DataResult<List<WorkoutProgram>> = dataResultOf {
        programs.observeActivePrograms().first()
    }

    override suspend fun firstStall(programId: String): DataResult<PlanStall?> =
        stalls.firstStall(programId)

    override suspend fun blocks(): DataResult<List<BlockProgress>> = dataResultOf {
        adherence.observeBlockProgress().first()
    }

    private companion object {
        const val DAYS_IN_WEEK = 7L
    }
}

/**
 * The row a gain is, or null when the week did not move it (ROADMAP N251).
 *
 * A first training is a gain even at bodyweight; a later week is a gain only when its heaviest set is strictly
 * heavier than the heaviest before. A repeat is not progress. A movement trained before without a weight — at
 * bodyweight, assisted or held — has nothing to compare the week against, so the gain carries no previous
 * number rather than a zero dressed up as one (B332).
 */
internal fun WeeklyMovementRow.toGainOrNull(): MovementGain? {
    val firstTime = priorSets == 0
    if (!firstTime && weekTop <= beforeTop) return null
    return MovementGain(
        exerciseId = exerciseId,
        name = name,
        topGrams = weekTop,
        previousGrams = if (firstTime || beforeTop == 0L) null else beforeTop,
        firstTime = firstTime,
    )
}
