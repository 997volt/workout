package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Per-workout averages of what the app collects (ROADMAP N13).
 *
 * A DAO of its own rather than more methods on `WorkoutDao`: these are read-only
 * aggregates across three tables, which is a different shape from the session and set
 * writes that interface holds — and it is already at its ceiling.
 *
 * Two queries rather than one: averaging a set's RPE and a session exercise's ratings
 * in a single statement would multiply the rows against each other, so the RPE average
 * would be weighted by how many exercises were rated. The repository merges them.
 */
@Dao
interface TrendsDao {

    /**
     * Average RPE per finished workout, newest first.
     *
     * The join filters `rpeHalves IS NOT NULL`, so a workout with nothing rated does not
     * appear at all rather than appearing as null: a gap in the chart should mean
     * "not recorded", and a row of nulls is harder to tell apart from a bug.
     *
     * **A warm-up is excluded** (ROADMAP N67): it records no effort, so an average that counted one
     * would read a ramp as work. The literal is the stored name the `SetType` converter writes, the
     * same way the migrations name a role; a row that somehow carries one is ignored here too.
     */
    @Query(
        """
        SELECT ws.id AS sessionId,
               ws.startedAt AS startedAt,
               AVG(s.rpeHalves) AS averageRpe
        FROM workout_sessions ws
        JOIN session_exercises se
          ON se.sessionId = ws.id AND se.deletedAt IS NULL
        JOIN set_entries s
          ON s.sessionExerciseId = se.id AND s.deletedAt IS NULL
         AND s.rpeHalves IS NOT NULL
         AND s.setType != 'WARMUP'
        WHERE ws.deletedAt IS NULL AND ws.finishedAt IS NOT NULL
        GROUP BY ws.id
        ORDER BY ws.startedAt DESC
        LIMIT :limit
        """,
    )
    fun observeRpeTrend(limit: Int): Flow<List<RpeTrendRow>>

    /**
     * Average muscle feel and joint pain per finished workout, newest first.
     *
     * `AVG` skips nulls, so an exercise that recorded only one of the two still contributes to that
     * one — which is what "skippable" has to mean for a trend to be readable.
     *
     * Joint pain per exercise is the **worst** joint it reported (ROADMAP N63), falling back to the
     * legacy single `se.jointPain` column for a session rated before the picked list existed. It is
     * the worst rather than an average of sides because "left knee 6, right knee 2" is one bad knee,
     * and averaging the two would report 4 for a knee that hurt 6.
     */
    @Query(
        """
        SELECT ws.id AS sessionId,
               ws.startedAt AS startedAt,
               AVG(se.muscleFeel) AS averageMuscleFeel,
               AVG(
                   COALESCE(
                       (
                           SELECT MAX(j.score) FROM session_exercise_joints j
                           WHERE j.sessionExerciseId = se.id AND j.deletedAt IS NULL
                       ),
                       se.jointPain
                   )
               ) AS averageJointPain
        FROM workout_sessions ws
        JOIN session_exercises se
          ON se.sessionId = ws.id
         AND se.deletedAt IS NULL
         AND (
             se.muscleFeel IS NOT NULL
             OR se.jointPain IS NOT NULL
             OR EXISTS (
                 SELECT 1 FROM session_exercise_joints j
                 WHERE j.sessionExerciseId = se.id AND j.deletedAt IS NULL
             )
         )
        WHERE ws.deletedAt IS NULL AND ws.finishedAt IS NOT NULL
        GROUP BY ws.id
        ORDER BY ws.startedAt DESC
        LIMIT :limit
        """,
    )
    fun observeFeelTrend(limit: Int): Flow<List<FeelTrendRow>>

    /**
     * The finished sessions that recorded any of [exerciseIds], one row per logged set (ROADMAP N17, N97).
     *
     * A list rather than one id because a **head** reads as one number: a category over its movements and a
     * movement over its variations are one series, so the query is asked for the whole family at once. One id
     * is the ordinary case and reads exactly as it did.
     *
     * Flat on purpose: the grouping, the warm-up exclusion and the one-rep-max estimate
     * are decisions rather than aggregations, and they live in
     * `List<ExerciseTrendRow>.toExerciseTrendPoints()` where they are pure and tested.
     * A session that recorded the exercise but logged no set still returns one row, with
     * the set columns null, so its ratings are not lost.
     *
     * `jointPain` is the exercise's **worst** joint that session (ROADMAP N63), falling back to the
     * legacy column for a session rated before the picked list existed — the alias is unchanged, so
     * the Kotlin mapping is too.
     *
     * The inner subquery takes the *sessions*, not the rows: `LIMIT` on a join would cut
     * a workout in half and silently drop the sets that did not fit the window. It groups by the session
     * and orders by that session's start, so the window is [limit] **sessions** however many of the family's
     * exercises each one logged — a family that logged two of them per workout used to spend two of its
     * [limit] on one workout (B100).
     */
    @Query(
        """
        SELECT se.sessionId AS sessionId,
               ws.startedAt AS startedAt,
               se.id AS sessionExerciseId,
               se.muscleFeel AS muscleFeel,
               COALESCE(
                   (
                       SELECT MAX(j.score) FROM session_exercise_joints j
                       WHERE j.sessionExerciseId = se.id AND j.deletedAt IS NULL
                   ),
                   se.jointPain
               ) AS jointPain,
               s.weightGrams AS weightGrams,
               s.reps AS reps,
               s.rpeHalves AS rpeHalves,
               s.setType AS setType,
               s.assistanceGrams AS assistanceGrams
        FROM session_exercises se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        LEFT JOIN set_entries s ON s.sessionExerciseId = se.id AND s.deletedAt IS NULL
        WHERE se.exerciseId IN (:exerciseIds)
          AND se.deletedAt IS NULL
          AND ws.deletedAt IS NULL
          AND ws.finishedAt IS NOT NULL
          AND se.sessionId IN (
              SELECT se2.sessionId
              FROM session_exercises se2
              JOIN workout_sessions ws2 ON ws2.id = se2.sessionId
              WHERE se2.exerciseId IN (:exerciseIds)
                AND se2.deletedAt IS NULL
                AND ws2.deletedAt IS NULL
                AND ws2.finishedAt IS NOT NULL
              GROUP BY se2.sessionId
              ORDER BY MAX(ws2.startedAt) DESC
              LIMIT :limit
          )
        ORDER BY ws.startedAt ASC, s.setIndex ASC
        """,
    )
    fun observeExerciseTrendRows(
        exerciseIds: List<String>,
        limit: Int,
    ): Flow<List<ExerciseTrendRowEntity>>
}

/** One workout's average RPE. */
data class RpeTrendRow(
    val sessionId: String,
    val startedAt: Long,
    val averageRpe: Double?,
)

/** One workout's average muscle feel and joint pain, each null when not recorded. */
data class FeelTrendRow(
    val sessionId: String,
    val startedAt: Long,
    val averageMuscleFeel: Double?,
    val averageJointPain: Double?,
)

/**
 * One set of one exercise in one finished session, as SQL returns it (ROADMAP N17).
 *
 * The set columns are nullable because the query left-joins them: a session that
 * recorded the exercise without logging a set still contributes its ratings.
 * `setType` stays a name here — the converter is for entities, and a projection is
 * mapped by the repository.
 */
data class ExerciseTrendRowEntity(
    val sessionId: String,
    val startedAt: Long,
    /**
     * The `session_exercises` row this set belongs to (ROADMAP B97).
     *
     * Carried because a **head** reads a family: several exercises of one family are logged in one session,
     * and each states its own rating while the DAO repeats that rating on every set row. Averaging the rows
     * would weight a movement by how many sets it happened to log, so the rating is taken once per exercise —
     * which needs the exercise's identity, and this is it.
     */
    val sessionExerciseId: String,
    val muscleFeel: Int?,
    val jointPain: Int?,
    val weightGrams: Long?,
    val reps: Int?,
    val rpeHalves: Int?,
    val setType: String?,
    val assistanceGrams: Long?,
)
