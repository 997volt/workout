package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The muscles a session reported sore (ROADMAP N62).
 *
 * Three jobs share this surface: the live list the workout and history screens read, the
 * replace-on-save the readiness editor performs, and the backup's half. [BackupDao] and
 * [ProgramBackupDao] are at the function ceiling this project enforces, so the newest table's
 * backup queries live with the table instead of being appended to either.
 *
 * The backup insert is `IGNORE` for the reason [BackupDao]'s are: ids are stable, so importing the
 * same file twice adds nothing the second time and no live row is overwritten.
 */
@Dao
interface SessionSoreMuscleDao {

    /** The live list for one session, in the order the lifter picked it. */
    @Query(
        "SELECT * FROM session_sore_muscles WHERE sessionId = :sessionId AND deletedAt IS NULL " +
            "ORDER BY position",
    )
    fun observeForSession(sessionId: String): Flow<List<SessionSoreMuscleEntity>>

    /**
     * Writes a save's whole list, so a replace is one statement rather than a row-per-muscle loop
     * that a failure could stop halfway.
     */
    @Insert
    suspend fun insertAll(rows: List<SessionSoreMuscleEntity>)

    /**
     * Hides the session's live rows before a save writes the new list (N62).
     *
     * A save **replaces** the list rather than merging into it: the editor shows exactly what is
     * stored, so a muscle absent from it was removed by the lifter. Returns the rows hidden.
     */
    @Query(
        "UPDATE session_sore_muscles SET deletedAt = :at, updatedAt = :at " +
            "WHERE sessionId = :sessionId AND deletedAt IS NULL",
    )
    suspend fun softDeleteForSession(sessionId: String, at: Long): Int

    // ------------------------------------------------------- backup (P1.12, N62)

    /** Deliberately unfiltered: a soft-deleted row is data an export must carry. */
    @Query("SELECT * FROM session_sore_muscles")
    suspend fun allForBackup(): List<SessionSoreMuscleEntity>

    @Query("SELECT id FROM session_sore_muscles WHERE deletedAt IS NOT NULL")
    suspend fun softDeletedIds(): List<String>

    /**
     * Rewrites rows by primary key, which restores a soft-deleted row *and* clears its `deletedAt`
     * because the file's value overwrites it. Only ever called for ids already soft-deleted.
     */
    @Update
    suspend fun restore(rows: List<SessionSoreMuscleEntity>): Int

    /** Returns one rowid per input row, `-1` where a row was skipped as a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<SessionSoreMuscleEntity>): List<Long>
}
