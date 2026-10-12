package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The muscles a day's measurement reported sore (ROADMAP N62, moved to the daily record by N113).
 *
 * Three jobs share this surface, the shape [SessionSoreMuscleDao] has: the live list the measurements
 * screen reads, the replace-on-save the form performs, and the backup's half.
 *
 * The backup insert is `IGNORE` for the reason [BackupDao]'s are: ids are stable, so importing the same
 * file twice adds nothing the second time and no live row is overwritten.
 */
@Dao
interface MeasurementSoreMuscleDao {

    /**
     * Every live row, in the order the lifter picked it.
     *
     * Read whole rather than per measurement: the screen shows every entry at once, so a query per row
     * would be one round trip each — the same reason the measurements themselves are one read.
     */
    @Query("SELECT * FROM measurement_sore_muscles WHERE deletedAt IS NULL ORDER BY position")
    fun observeAll(): Flow<List<MeasurementSoreMuscleEntity>>

    /**
     * Writes a save's whole list, so a replace is one statement rather than a row-per-muscle loop that
     * a failure could stop halfway.
     */
    @Insert
    suspend fun insertAll(rows: List<MeasurementSoreMuscleEntity>)

    /**
     * Hides one measurement's live rows before a save writes the new list (N62's rule, N113's table).
     *
     * A save **replaces** the list rather than merging into it: the form shows exactly what is stored,
     * so a muscle absent from it was removed by the lifter. Returns the rows hidden.
     */
    @Query(
        "UPDATE measurement_sore_muscles SET deletedAt = :at, updatedAt = :at " +
            "WHERE measurementId = :measurementId AND deletedAt IS NULL",
    )
    suspend fun softDeleteForMeasurement(measurementId: String, at: Long): Int

    // ------------------------------------------------------- backup (P1.12, N113)

    /** Deliberately unfiltered: a soft-deleted row is data an export must carry. */
    @Query("SELECT * FROM measurement_sore_muscles")
    suspend fun allForBackup(): List<MeasurementSoreMuscleEntity>

    @Query("SELECT id FROM measurement_sore_muscles WHERE deletedAt IS NOT NULL")
    suspend fun softDeletedIds(): List<String>

    /**
     * Rewrites rows by primary key, which restores a soft-deleted row *and* clears its `deletedAt`
     * because the file's value overwrites it. Only ever called for ids already soft-deleted.
     */
    @Update
    suspend fun restore(rows: List<MeasurementSoreMuscleEntity>): Int

    /** Returns one rowid per input row, `-1` where a row was skipped as a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<MeasurementSoreMuscleEntity>): List<Long>
}
