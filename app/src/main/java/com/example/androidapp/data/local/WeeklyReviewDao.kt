package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query

/**
 * The week's per-movement read (ROADMAP N251).
 *
 * It answers one question the history and the summary cannot: which lifts actually moved this week. The week
 * boundaries arrive in milliseconds because the caller owns the zone; a session performed abroad still counts
 * on the device's week here, which is the one the review is about.
 */
@Dao
interface WeeklyReviewDao {

    /**
     * One row per movement trained this week, with its best before the week and whether it had any before.
     *
     * Only live finished sessions, live exercises and live sets count. A movement's week top is 0 when its
     * sets carried no weight; [priorSets] separates "never trained" from "trained at bodyweight".
     */
    @Query(
        """
        SELECT se.exerciseId AS exerciseId,
               e.name AS name,
               MAX(CASE WHEN ws.startedAt >= :from AND ws.startedAt < :to THEN s.weightGrams ELSE 0 END) AS weekTop,
               MAX(CASE WHEN ws.startedAt < :from THEN s.weightGrams ELSE 0 END) AS beforeTop,
               SUM(CASE WHEN ws.startedAt >= :from AND ws.startedAt < :to THEN 1 ELSE 0 END) AS weekSets,
               SUM(CASE WHEN ws.startedAt < :from THEN 1 ELSE 0 END) AS priorSets
        FROM set_entries s
        JOIN session_exercises se ON se.id = s.sessionExerciseId
        JOIN workout_sessions ws ON ws.id = se.sessionId
        JOIN exercises e ON e.id = se.exerciseId
        WHERE ws.finishedAt IS NOT NULL
          AND ws.deletedAt IS NULL
          AND se.deletedAt IS NULL
          AND s.deletedAt IS NULL
        GROUP BY se.exerciseId, e.name
        HAVING weekSets > 0
        """,
    )
    suspend fun movementGains(from: Long, to: Long): List<WeeklyMovementRow>
}

/** One movement's week, before the domain decides whether it counts as a gain (ROADMAP N251). */
data class WeeklyMovementRow(
    val exerciseId: String,
    val name: String,
    val weekTop: Long,
    val beforeTop: Long,
    val weekSets: Int,
    val priorSets: Int,
)
