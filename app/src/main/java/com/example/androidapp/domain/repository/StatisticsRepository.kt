package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.ExerciseWorkingSets
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.RecordEvent
import com.example.androidapp.domain.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * The questions the app asks of the logged history (ROADMAP N35, N289).
 *
 * Its own port rather than more methods on `WorkoutRepository`: statistics reads history to summarise it,
 * which is a different job from logging a set, and the workout repository is at its function ceiling. The
 * trained balance's read is here for the same reason: it is an aggregate over the log, not a workout edit.
 */
interface StatisticsRepository {

    /**
     * Finished workouts as the history list shows them, for the overview's totals.
     *
     * On this port rather than read from `WorkoutRepository` so the screen depends on statistics and
     * nothing else — and implemented with the history query that already exists, not a second copy of its
     * SQL: two queries totalling the same rows would be two things to keep in step.
     */
    fun observeWorkoutSummaries(): Flow<List<WorkoutSummary>>

    /**
     * What the lifter's **plans** prescribe for the lifts in [exerciseIds], one entry per plan that names one
     * of them (ROADMAP N39).
     *
     * On this port rather than `TemplateRepository` for the reason the port exists: it is a question the
     * Statistics screen asks and no other screen does. The rows come from the plan's own tables, and what a
     * chart draws of them is [com.example.androidapp.domain.model.levelFor]'s question.
     *
     * A **list** because the series may read a whole family: a metric that accepts a head (N97) draws its
     * family's points, so its plan lines have to be read over the same ids (B143) — a set never names a
     * category, and querying the chosen id alone drew the family with no plan line at all.
     *
     * **Kept versions and deleted rows are not plans**: a version is history (N134) and a soft-deleted set was
     * removed, so neither is a prescription the chart may draw.
     */
    fun observePlanTargets(exerciseIds: List<String>): Flow<List<PlanTarget>>

    /**
     * How many working sets each live movement was given between [from] (inclusive) and [toExclusive]
     * (ROADMAP N289).
     *
     * **Counts, not sets**: the query groups by movement, so the balance view never loads a set per muscle.
     * Only finished, live sessions count; warm-ups are excluded by the same `SetType.isWork` rule the plan
     * uses (N112); deleted sets, exercises and sessions are not training. The muscles are deliberately not
     * resolved here — a movement's own-or-category's muscle is a live library fact (N110), so the balance
     * function takes the library and credits the counts.
     */
    fun observeWorkingSetsIn(
        from: Instant,
        toExclusive: Instant,
    ): Flow<DataResult<List<ExerciseWorkingSets>>>

    /**
     * The newest record moments, bounded to [limit], for the Statistics timeline (N290).
     *
     * A weighted record and a hold are separate shapes; the UI decides how each is printed.
     */
    fun observeRecentRecords(limit: Int): Flow<List<RecordEvent>>

    /**
     * Records set between [from] (inclusive) and [to] (exclusive).
     *
     * A record is judged against everything performed before it, so this cannot be answered by a stored
     * flag: nothing writes down that a set *was* a record, only what it weighed.
     */
    suspend fun countRecordsIn(from: Instant, to: Instant): DataResult<Int>
}
