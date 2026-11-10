package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The tables auto progression keeps beside the plan (ROADMAP N299).
 *
 * Runs and baselines are written only through their own repositories; this interface is the
 * storage shape — the write log, the baseline, and the two reads a reset needs.
 */
@Dao
interface ProgressionDao {

    @Insert
    suspend fun insertWrite(row: ProgressionWriteEntity)

    @Query("SELECT * FROM progression_writes WHERE id = :id")
    suspend fun findWrite(id: String): ProgressionWriteEntity?

    /** Marks an undo; 0 means the row is gone or was already undone. */
    @Query("UPDATE progression_writes SET revertedAt = :at WHERE id = :id AND revertedAt IS NULL")
    suspend fun markReverted(id: String, at: Long): Int

    @Query("SELECT * FROM progression_writes")
    suspend fun allWrites(): List<ProgressionWriteEntity>

    @Query("DELETE FROM progression_baselines WHERE programId = :programId")
    suspend fun clearBaselines(programId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaselines(rows: List<ProgressionBaselineEntity>)

    @Query("SELECT * FROM progression_baselines WHERE programId = :programId")
    suspend fun findBaselines(programId: String): List<ProgressionBaselineEntity>

    @Query("SELECT MAX(capturedAt) FROM progression_baselines WHERE programId = :programId")
    suspend fun baselineCapturedAt(programId: String): Long?

    @Query("SELECT * FROM progression_baselines")
    suspend fun allBaselines(): List<ProgressionBaselineEntity>
}
