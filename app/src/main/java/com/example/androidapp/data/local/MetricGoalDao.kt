package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The periods each metric's goal went through (ROADMAP N137).
 *
 * Two reads, and the difference between them is the whole feature: [observeCurrent] answers "what is the goal
 * now" for the screens that draw it, and [observePeriods] answers "what was it, and when" for the history that
 * says whether each period was met.
 */
@Dao
interface MetricGoalDao {

    /**
     * The **newest** row of each metric: the period that is open.
     *
     * One query rather than a read per metric, for the reason the goals map was one read: the screen draws
     * whichever goal belongs to the metric on show, and the number of metrics is the registry's, not the
     * user's. A row with an unknown start sorts **last** among its metric's rows, because a date is newer
     * information than no date.
     */
    @Query(
        "SELECT * FROM metric_goals WHERE id = (" +
            "SELECT i.id FROM metric_goals AS i WHERE i.metricId = metric_goals.metricId " +
            "ORDER BY (i.startedAt IS NULL) ASC, i.startedAt DESC, i.createdAt DESC LIMIT 1)",
    )
    fun observeCurrent(): Flow<List<MetricGoalEntity>>

    /**
     * One metric's periods, **oldest first**, so a reader can walk them and close each at the next one's start.
     *
     * An undated row leads, which is the honest reading of "this was the goal before we knew when goals
     * started": it is the one with no beginning rather than the one with the earliest date.
     */
    @Query(
        "SELECT * FROM metric_goals WHERE metricId = :metricId " +
            "ORDER BY (startedAt IS NULL) ASC, startedAt ASC, createdAt ASC",
    )
    fun observePeriods(metricId: String): Flow<List<MetricGoalEntity>>

    /** Opens a period. Nothing is updated: a change is a new row, which is what keeps the past (N137). */
    @Insert
    suspend fun insert(row: MetricGoalEntity)

    // ------------------------------------------------------------------ backup (N137)

    /** Every period, oldest first, dated rows after the undated one. */
    @Query("SELECT * FROM metric_goals ORDER BY (startedAt IS NULL) ASC, startedAt ASC, createdAt ASC")
    suspend fun allForBackup(): List<MetricGoalEntity>

    /**
     * Returns one rowid per input row, `-1` where a row was skipped as a duplicate.
     *
     * `IGNORE`, like every other table's import: ids are stable, so importing the same file twice adds nothing
     * the second time and no live row is overwritten. The goals' old *missing-only* rule went with the
     * preferences string it existed for (N137).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<MetricGoalEntity>): List<Long>
}
