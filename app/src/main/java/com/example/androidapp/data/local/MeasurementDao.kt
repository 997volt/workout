package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The measurements table (ROADMAP N32).
 *
 * Newest first, because a measurement is read as "how has this been going" rather than "what happened
 * on a particular day" — the chart and the list both want the same order.
 */
@Dao
interface MeasurementDao {

    /**
     * Every measurement, including soft-deleted ones, for an export (ROADMAP N32).
     *
     * Here rather than in BackupDao, which is at its function ceiling — and this is the table's own
     * DAO, which is where its reads belong anyway. A soft-deleted row is exported like every other
     * table's, because an export is a copy of the database rather than of the current screen.
     */
    @Query("SELECT * FROM measurements")
    suspend fun allForExport(): List<MeasurementEntity>

    /**
     * Returns one rowid per input row, `-1` where a row was skipped as a duplicate (P1.12).
     *
     * `IGNORE` like every other table's import, because a row already on the device — live, or hidden by
     * a delete the restore step is about to undo — is a row the file has nothing to add to. A plain
     * `@Insert` **aborts** on the first of those and rolls the whole import back, which is what importing
     * a second time, or a file whose measurement was deleted here, used to do.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<MeasurementEntity>): List<Long>

    // ------------------------------------------------------- backup (P1.12, N32)

    /**
     * The ids of the rows a delete hid, so an import can bring the live ones back (P1.12).
     *
     * Measurements had no half of this until N113's sore rows arrived: the child could be restored while
     * its parent stayed hidden, which un-hid a daily record's soreness onto a record nothing could read.
     */
    @Query("SELECT id FROM measurements WHERE deletedAt IS NOT NULL")
    suspend fun softDeletedIds(): List<String>

    /**
     * Rewrites rows by primary key, which restores a soft-deleted row *and* clears its `deletedAt`
     * because the file's value overwrites it. Only ever called for ids already soft-deleted.
     */
    @Update
    suspend fun restore(rows: List<MeasurementEntity>): Int

    @Query("SELECT * FROM measurements WHERE deletedAt IS NULL ORDER BY measuredAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    /**
     * The live entry of one **kind** for a local day, if there is one (ROADMAP N32, N113).
     *
     * The day is a half-open range in the caller's own zone rather than a stored date column: the entry
     * belongs to the day it was taken where it was taken, and the range is what makes that a query
     * instead of a second source of truth. The kind is part of the key because a day holds **one entry
     * of each** (N113): a tape day edits its own row rather than the one the scale wrote.
     */
    @Query(
        """
        SELECT * FROM measurements
        WHERE measuredAt >= :from AND measuredAt < :to AND deletedAt IS NULL AND kind = :kind
        ORDER BY measuredAt DESC LIMIT 1
        """,
    )
    suspend fun findOnDay(from: Long, to: Long, kind: String): MeasurementEntity?

    @Query("SELECT * FROM measurements WHERE id = :id AND deletedAt IS NULL")
    suspend fun find(id: String): MeasurementEntity?

    /**
     * Writes an edited entry. The caller builds the row, `updatedAt` included.
     *
     * `@Update` rather than a query with a parameter per column: thirteen parameters is not a
     * signature, and a caller that forgets one would silently write a default.
     *
     * Rows updated: 0 means it is gone, so a stale screen cannot write into nothing.
     */
    @Update
    suspend fun update(row: MeasurementEntity): Int

    @Insert
    suspend fun insert(row: MeasurementEntity)

    @Query("UPDATE measurements SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, at: Long): Int
}
