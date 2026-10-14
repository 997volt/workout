package com.example.androidapp.data

import com.example.androidapp.data.local.JointPainSiteRow
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.ExerciseTrendRow
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.TrendPoint
import com.example.androidapp.domain.model.jointSiteKey
import com.example.androidapp.domain.model.toExerciseTrendPoints
import com.example.androidapp.domain.repository.TrendsRepository
import com.example.androidapp.domain.toDataError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [TrendsRepository] (ROADMAP N13).
 *
 * The two aggregate queries are merged here rather than in SQL, because averaging a
 * set's RPE and a session exercise's ratings in one statement multiplies the rows and
 * silently weights one by the other.
 *
 * A workout can appear in either query and not the other — RPE with no ratings is
 * normal — so the merge is a union keyed by session id, and the result is reversed
 * into the oldest-first order a chart reads in.
 */
@Singleton
class RoomTrendsRepository @Inject constructor(
    database: WorkoutDatabase,
) : TrendsRepository {

    private val dao = database.trendsDao()

    /**
     * The per-site pain read serves every lift at once, so exercises outside the chosen window arrive too
     * and contribute maps nothing asks for. That is the price of not running a query per chosen lift, and
     * it buys a flat query count: N17's series is bounded by the sessions the rows query returned, so the
     * extra entries are never read.
     */
    override fun observeExerciseTrends(
        exerciseIds: List<String>,
        limit: Int,
    ): Flow<DataResult<List<ExerciseTrendPoint>>> =
        combine(
            dao.observeExerciseTrendRows(exerciseIds, limit),
            dao.observeJointPainByExercise(),
        ) { rows, joints ->
            // Grouping and arithmetic live in the domain type, where they are pure
            // and tested; this is only the storage-to-domain translation.
            val rowsDomain = rows.map { row ->
                ExerciseTrendRow(
                    sessionId = row.sessionId,
                    sessionExerciseId = row.sessionExerciseId,
                    startedAt = Instant.ofEpochMilli(row.startedAt),
                    muscleFeel = row.muscleFeel,
                    jointPain = row.jointPain,
                    weightGrams = row.weightGrams,
                    reps = row.reps,
                    rpeHalves = row.rpeHalves,
                    setType = row.setType?.let(SetType::valueOf),
                    assistanceGrams = row.assistanceGrams,
                )
            }
            rowsDomain.toExerciseTrendPoints(jointPainBySite = joints.toJointPainByExercise())
        }
            .map<List<ExerciseTrendPoint>, DataResult<List<ExerciseTrendPoint>>> {
                DataResult.Success(it)
            }
            .catch { emit(DataResult.Failure(it.toDataError())) }

    override fun observeTrends(limit: Int): Flow<DataResult<List<TrendPoint>>> =
        combine(
            dao.observeRpeTrend(limit),
            dao.observeFeelTrend(limit),
            dao.observeJointPainBySite(limit),
        ) { rpeRows, feelRows, jointRows ->
            val rpeBySession = rpeRows.associateBy { it.sessionId }
            val feelBySession = feelRows.associateBy { it.sessionId }
            val jointsBySession = jointRows.toJointPainBySession()

            (rpeBySession.keys + feelBySession.keys + jointsBySession.keys)
                .mapNotNull { sessionId ->
                    val rpeHalves = rpeBySession[sessionId]
                    val feel = feelBySession[sessionId]
                    val joints = jointsBySession[sessionId]
                    val startedAt = rpeHalves?.startedAt
                        ?: feel?.startedAt
                        ?: jointRows.firstOrNull { it.sessionId == sessionId }?.startedAt
                        ?: return@mapNotNull null
                    TrendPoint(
                        startedAt = Instant.ofEpochMilli(startedAt),
                        // SQL averaged halves; the series is in RPE units (N6, N13).
                        averageRpe = rpeHalves?.averageRpe?.div(Rpe.HALVES_PER_POINT),
                        averageMuscleFeel = feel?.averageMuscleFeel,
                        averageJointPain = feel?.averageJointPain,
                        jointPainBySite = joints.orEmpty(),
                    )
                }
                // The queries return newest first, which is right for a limit and
                // wrong for a chart.
                .sortedBy { it.startedAt }
        }
            .map<List<TrendPoint>, DataResult<List<TrendPoint>>> { DataResult.Success(it) }
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                emit(DataResult.Failure(throwable.toDataError()))
            }
}

/**
 * The per-site rows as one map per session, keyed by [jointSiteKey] (ROADMAP N130).
 *
 * The stored names become [Joint] and [Side] here, the way a set's role does: a row naming something the
 * enums do not know is a data problem, and `valueOf` says so loudly rather than filing it under a
 * silently different site.
 */
private fun List<JointPainSiteRow>.toJointPainBySession(): Map<String, Map<String, Double>> =
    groupBy { it.sessionId }.mapValues { (_, rows) -> rows.associate { it.siteKey() to it.score.toDouble() } }

/** The same map keyed by the session exercise, for one lift's own series (ROADMAP N130). */
private fun List<JointPainSiteRow>.toJointPainByExercise(): Map<String, Map<String, Double>> =
    mapNotNull { row -> row.sessionExerciseId?.let { exercise -> exercise to row } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, rows) -> rows.associate { it.siteKey() to it.score.toDouble() } }

private fun JointPainSiteRow.siteKey(): String =
    jointSiteKey(Joint.valueOf(joint), Side.valueOf(side))
