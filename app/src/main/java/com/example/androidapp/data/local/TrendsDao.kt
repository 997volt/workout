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
     * Joint pain per exercise is the **worst** joint it reported (ROADMAP N63). It is the worst rather
     * than an average of sides because "left knee 6, right knee 2" is one bad knee, and averaging the
     * two would report 4 for a knee that hurt 6.
     */
    @Query(
        """
        SELECT ws.id AS sessionId,
               ws.startedAt AS startedAt,
               AVG(se.muscleFeel) AS averageMuscleFeel,
               AVG(
                   (
                       SELECT MAX(j.score) FROM session_exercise_joints j
                       WHERE j.sessionExerciseId = se.id AND j.deletedAt IS NULL
                   )
               ) AS averageJointPain
        FROM workout_sessions ws
        JOIN session_exercises se
          ON se.sessionId = ws.id
         AND se.deletedAt IS NULL
         AND (
             se.muscleFeel IS NOT NULL
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
     * Each joint site's **worst** score per finished workout, newest first (ROADMAP N130).
     *
     * The per-site counterpart of the worst-joint number above, and the read that gives joint pain back
     * the dimension the average dropped: a rating records which joint, which side and how much, and
     * rolling "left knee 6, right knee 2" into one 4 is a number about neither knee.
     *
     * **`MAX` within the session, not `AVG`**, for N63's reason one level up: a knee that ached through
     * three movements is one sore knee, and averaging the three would report it as milder than any of
     * them. `joint` and `side` are grouped by their **stored names**, because that is what the converter
     * writes and what [jointSiteKey] is built from — grouping by anything else would need a second
     * vocabulary for the same fact.
     *
     * One read for every site rather than a query per series: the caller builds the map once and each
     * joint-pain series indexes into it, which keeps the query count flat as the picker grows.
     */
    @Query(
        """
        SELECT ws.id AS sessionId,
               ws.startedAt AS startedAt,
               j.joint AS joint,
               j.side AS side,
               MAX(j.score) AS score
        FROM workout_sessions ws
        JOIN session_exercises se ON se.sessionId = ws.id AND se.deletedAt IS NULL
        JOIN session_exercise_joints j
          ON j.sessionExerciseId = se.id AND j.deletedAt IS NULL
        WHERE ws.deletedAt IS NULL AND ws.finishedAt IS NOT NULL
        GROUP BY ws.id, j.joint, j.side
        ORDER BY ws.startedAt DESC
        LIMIT :limit
        """,
    )
    fun observeJointPainBySite(limit: Int): Flow<List<JointPainSiteRow>>

    /**
     * Each joint site's worst score per session exercise (ROADMAP N130).
     *
     * The per-lift counterpart of the read above, and it keys on the *exercise* row rather than the
     * session: [observeExerciseTrendRows] already carries that identity (B97), so the two join up on
     * [JointPainSiteRow.sessionExerciseId] without a second grouping decision here.
     */
    @Query(
        """
        SELECT se.sessionId AS sessionId,
               se.id AS sessionExerciseId,
               ws.startedAt AS startedAt,
               j.joint AS joint,
               j.side AS side,
               MAX(j.score) AS score
        FROM session_exercises se
        JOIN workout_sessions ws ON ws.id = se.sessionId
        JOIN session_exercise_joints j
          ON j.sessionExerciseId = se.id AND j.deletedAt IS NULL
        WHERE se.deletedAt IS NULL AND ws.deletedAt IS NULL AND ws.finishedAt IS NOT NULL
        GROUP BY se.id, j.joint, j.side
        """,
    )
    fun observeJointPainByExercise(): Flow<List<JointPainSiteRow>>

    /**
     * The finished sessions that recorded any of [exerciseIds], one row per logged set (ROADMAP N17, N97).
     *
     * A list rather than one id because a **head** reads as one number: a category over the movements filed
     * under it is one series (N110), so the query is asked for the whole family at once. One id is the
     * ordinary case — a movement heads nothing — and reads exactly as it did.
     *
     * Flat on purpose: the grouping, the warm-up exclusion and the one-rep-max estimate
     * are decisions rather than aggregations, and they live in
     * `List<ExerciseTrendRow>.toExerciseTrendPoints()` where they are pure and tested.
     * A session that recorded the exercise but logged no set still returns one row, with
     * the set columns null, so its ratings are not lost.
     *
     * `jointPain` is the exercise's **worst** joint that session (ROADMAP N63).
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
               (
                   SELECT MAX(j.score) FROM session_exercise_joints j
                   WHERE j.sessionExerciseId = se.id AND j.deletedAt IS NULL
               ) AS jointPain,
               s.weightGrams AS weightGrams,
               s.reps AS reps,
               s.rpeHalves AS rpeHalves,
               s.setType AS setType,
               s.assistanceGrams AS assistanceGrams,
               s.bodyweightGrams AS bodyweightGrams
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
 * One (joint, side) site's worst score in one finished workout or one session exercise (ROADMAP N130).
 *
 * The joint and side arrive as their **stored names**, which is what the converter writes; the
 * repository turns them into [Joint]/[Side] with `valueOf`, the same way it does for a set's role, so a
 * name that is not a value of the enum is a data problem rather than a silently different site.
 *
 * [sessionExerciseId] is null for the workout-level read and the exercise's own row for the per-lift
 * one — the two queries differ only in what they group by.
 */
data class JointPainSiteRow(
    val sessionId: String,
    val sessionExerciseId: String?,
    val startedAt: Long,
    val joint: String,
    val side: String,
    val score: Int,
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
    /**
     * The lifter's weight if this set was bodyweight-loaded, or null (ROADMAP N219).
     *
     * Read so the trend's volume, heaviest set and 1RM can be computed at the same load the row was performed
     * at — see `loadGrams`.
     */
    val bodyweightGrams: Long?,
)
