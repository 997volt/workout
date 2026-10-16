package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The Statistics screen's own queries (ROADMAP N35).
 *
 * Its own DAO rather than another method on `WorkoutDao`, which is at its function ceiling — and the
 * statistics screen's questions are not the workout editor's.
 */
@Dao
interface StatisticsDao {

    /**
     * How many sets inside the window were records *when they were performed*.
     *
     * A record is the app's own rule (`PersonalRecords.isRecord`) applied over history: strictly heavier
     * than every earlier set at the same rep count for the same exercise, and never a warm-up. Matching a
     * best does not beat it, which is why the comparison is `>=` on the earlier set and `NOT EXISTS`
     * overall — so a repeat of yesterday's best counts as nothing rather than as a second record.
     *
     * **Earlier is by performance order, not by row id**: the session's start, then the exercise's position
     * in it, then the set's index. That is the order a lifter did them in, and it is the only order that
     * makes "earlier" mean anything when two sessions share a timestamp.
     *
     * **"Earlier" spans the whole of a session, including a second row for the same exercise.** The live path
     * (`onLogSet`) is narrower than that: it reads history excluding the current session and then merges only
     * the sets of the row being added to, so a second row for the same exercise does not see the first, and
     * each row's first set repeating the same weight is announced as a record twice. This query counts one.
     * The count is the rule `PersonalRecords.isRecord` states — matching a best is not beating it — and the
     * announce path is the one that diverges; narrowing this to match it was tried and rejected because it
     * made a single session's repeated set stop counting and contradicted this query's own tests. Fixing the
     * divergence means the live path merging the whole session's sets for the exercise rather than one row.
     *
     * Warm-ups are excluded on both sides, which is B17's rule — a heavy warm-up is not a record — and a
     * bodyweight set has no weight to compare, so it is excluded by the same `weightGrams > 0` the domain
     * rule uses rather than being counted as one. `reps > 0` matches `PersonalRecords.from`, which drops a
     * set with no reps; only a restored backup file can produce one, since every write path clamps.
     *
     * **The roles whose effort counts come from the caller, not from a literal here** (B155). This predicate
     * named `'WARMUP'` alone, so a DROP or CLUSTER rung — which the domain rule refuses
     * (`SetType.recordsEffort`) and the live announce never mentions — was counted as a record the lifter was
     * never told about, and, on the other side of the `NOT EXISTS`, blocked a later honest set from a count
     * that would have been announced. A rung's stored weight really is positive (it is the anchor's load less
     * the run's value), so the query genuinely matched it. Passing
     * `SetType.entries.filter { it.recordsEffort }` keeps one rule with one home: the SQL cannot drift from
     * the enum again.
     */
    @Query(
        """
        SELECT COUNT(*) FROM set_entries s
        JOIN session_exercises se ON se.id = s.sessionExerciseId
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE s.deletedAt IS NULL AND se.deletedAt IS NULL AND ws.deletedAt IS NULL
          AND ws.finishedAt IS NOT NULL
          AND s.setType IN (:recordableRoles) AND s.weightGrams > 0 AND s.reps > 0
          AND ws.startedAt >= :from AND ws.startedAt < :to
          AND NOT EXISTS (
              SELECT 1 FROM set_entries earlier
              JOIN session_exercises ese ON ese.id = earlier.sessionExerciseId
              JOIN workout_sessions ews ON ews.id = ese.sessionId
              WHERE ese.exerciseId = se.exerciseId
                AND earlier.reps = s.reps
                AND earlier.deletedAt IS NULL AND ese.deletedAt IS NULL AND ews.deletedAt IS NULL
                AND ews.finishedAt IS NOT NULL
                AND earlier.setType IN (:recordableRoles) AND earlier.weightGrams >= s.weightGrams
                AND (
                    ews.startedAt < ws.startedAt
                    OR (ews.startedAt = ws.startedAt AND ese.position < se.position)
                    OR (
                        ews.startedAt = ws.startedAt
                        AND ese.position = se.position
                        AND earlier.setIndex < s.setIndex
                    )
                )
          )
        """,
    )
    suspend fun countRecordsIn(from: Long, to: Long, recordableRoles: List<String>): Int

    /**
     * What the live plans prescribe for the lifts in [exerciseIds] (ROADMAP N39, B143).
     *
     * A flat read of the three plan tables, reduced to one row per plan in `planTargets`: the aggregation is
     * "the heaviest set *and the reps it is prescribed for*", which is two columns of one row and therefore not
     * something `GROUP BY` can answer without a second pass.
     *
     * A **list** rather than one id, because the series is read for a whole family where the metric accepts a
     * head (N97): a set never names a category, so querying the chosen id alone drew the family's readings with
     * no plan line at all.
     *
     * The reps carry the same fallback the session's own suggestion makes (N74, B142) — the rung progression
     * reached, else the range's **floor**, else its top — so the chart's line is one answer to "what does this
     * set ask for" rather than a second one.
     *
     * Every liveness predicate is here on purpose. A **kept version** is not a plan (N134) — it is what the plan
     * used to be — and a soft-deleted set was taken out of the plan, so drawing either would show a target the
     * lifter is not following.
     */
    @Query(
        """
        SELECT t.id AS templateId, t.name AS templateName,
               s.targetWeightGrams AS weightGrams,
               s.targetAssistanceGrams AS assistanceGrams,
               COALESCE(s.targetRepsCurrent, s.targetRepsMin, s.targetRepsMax) AS reps,
               e.targetRpeHalves AS rpeHalves
        FROM templates t
        JOIN template_exercises e ON e.templateId = t.id
        JOIN template_sets s ON s.templateExerciseId = e.id
        WHERE t.deletedAt IS NULL AND t.versionOf IS NULL
          AND e.deletedAt IS NULL AND s.deletedAt IS NULL
          AND e.exerciseId IN (:exerciseIds)
        """,
    )
    fun observePlanTargets(exerciseIds: List<String>): Flow<List<PlanTargetRow>>
}
