package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The writes a merge makes (ROADMAP N238).
 *
 * Its own DAO rather than methods on [ExerciseDao] or [WorkoutDao]: a merge is one operation across four tables,
 * and the interfaces it would otherwise extend are at the function ceiling this project enforces.
 */
@Dao
interface MovementMergeDao {

    /** Every retired id and what it became, for the document import to resolve through. */
    @Query("SELECT * FROM retired_movements")
    suspend fun retiredMovements(): List<RetiredMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRetired(movements: List<RetiredMovementEntity>)

    /** A session that logged the merged-away row now logs the survivor — its sets hang off the row, not the id. */
    @Query("UPDATE session_exercises SET exerciseId = :to, updatedAt = :at WHERE exerciseId = :from")
    suspend fun repointSessionExercises(from: String, to: String, at: Long)

    /** And a plan that names it names the survivor, which is what makes a loaded document differ from the plan. */
    @Query("UPDATE template_exercises SET exerciseId = :to, updatedAt = :at WHERE exerciseId = :from")
    suspend fun repointTemplateExercises(from: String, to: String, at: Long)

    /**
     * Deletes the merged-away row.
     *
     * A hard delete, after the re-point: the row holds nothing, and a tombstone would be a row the library must
     * filter and every backup must carry for an id the seed will never ship.
     */
    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteMergedExercise(id: String)
}
