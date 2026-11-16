package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query

/**
 * The week's per-movement read (ROADMAP N251).
 *
 * It answers one question the history and the summary cannot: which lifts actually moved this week. The
 * boundaries arrive as local days, and each session's own stored offset turns its instant into the day it
 * happened on — the same answer the review's totals use (B344, B459). A session without an offset (a row
 * written before N25) is read in the caller's fallback.
 */
@Dao
interface WeeklyReviewDao {

    /**
     * One row per movement trained this week, with its best before the week and whether it had any before.
     *
     * Only live finished sessions, live exercises and live sets count, and only the roles that record effort
     * do: a warm-up is not a best, and neither is a rung — the same rule the records use (B17, N79), passed in
     * as [recordableRoles] so the SQL cannot drift from the enum (B331). A movement's week top is 0 when its
     * sets carried no weight; [priorSets] separates "never trained" from "trained at bodyweight".
     */
    @Query(
        """
        WITH scored AS (
            SELECT se.exerciseId AS exerciseId,
                   e.name AS name,
                   s.weightGrams AS weightGrams,
                   date(
                       (ws.startedAt + COALESCE(ws.zoneOffsetMinutes, :fallbackOffsetMinutes) * 60000) / 1000,
                       'unixepoch'
                   ) AS localDay
            FROM set_entries s
            JOIN session_exercises se ON se.id = s.sessionExerciseId
            JOIN workout_sessions ws ON ws.id = se.sessionId
            JOIN exercises e ON e.id = se.exerciseId
            WHERE ws.finishedAt IS NOT NULL
              AND ws.deletedAt IS NULL
              AND se.deletedAt IS NULL
              AND s.deletedAt IS NULL
              AND e.deletedAt IS NULL
              AND s.setType IN (:recordableRoles)
        )
        SELECT exerciseId AS exerciseId,
               name AS name,
               MAX(CASE WHEN localDay >= :fromDay AND localDay < :toDay THEN weightGrams ELSE 0 END) AS weekTop,
               MAX(CASE WHEN localDay < :fromDay THEN weightGrams ELSE 0 END) AS beforeTop,
               SUM(CASE WHEN localDay >= :fromDay AND localDay < :toDay THEN 1 ELSE 0 END) AS weekSets,
               SUM(CASE WHEN localDay < :fromDay THEN 1 ELSE 0 END) AS priorSets
        FROM scored
        GROUP BY exerciseId, name
        HAVING weekSets > 0
        """,
    )
    suspend fun movementGains(
        /** The week's first day, ISO-8601, in each session's own zone. */
        fromDay: String,
        /** The day after its last, ISO-8601. */
        toDay: String,
        /** The zone a session without a stored offset is read in. */
        fallbackOffsetMinutes: Int,
        recordableRoles: List<String>,
    ): List<WeeklyMovementRow>
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
