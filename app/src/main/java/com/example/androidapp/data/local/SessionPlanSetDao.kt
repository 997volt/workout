package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The plan snapshots the stall and volume rules read (ROADMAP B471).
 *
 * Written once, when a session exercise is seeded from a template: the session's prescription is
 * the plan it started with, and a later write would turn the snapshot into a second living plan.
 * The replace strategy is there for a caller that seeds again into the same id; the reader is the
 * trends repository, which groups the rows by session exercise.
 */
@Dao
interface SessionPlanSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SessionPlanSetEntity>)

    /** The prescriptions for these session exercises, whatever session they belong to. */
    @Query("SELECT * FROM session_plan_sets WHERE sessionExerciseId IN (:sessionExerciseIds)")
    suspend fun findFor(sessionExerciseIds: List<String>): List<SessionPlanSetEntity>
}
