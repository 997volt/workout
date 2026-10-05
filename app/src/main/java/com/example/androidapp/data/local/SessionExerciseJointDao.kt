package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The joints a session exercise reported painful (ROADMAP N63).
 *
 * Three jobs share this surface: the live list the workout and history screens read, the
 * replace-on-save the rating dialog performs, and the backup's half. [BackupDao] is at the function
 * ceiling this project enforces, so the newest table's backup queries live with the table instead
 * of being appended to it.
 *
 * The backup insert is `IGNORE` for the reason [BackupDao]'s are: ids are stable, so importing the
 * same file twice adds nothing the second time and no live row is overwritten.
 */
@Dao
interface SessionExerciseJointDao {

    /**
     * The live rows for every exercise in one session, in the order the lifter picked them.
     *
     * Joined to `session_exercises` because the rows are keyed to the exercise and the screens read
     * a whole session at a time; the parent's own soft delete hides a removed exercise's joints too.
     */
    @Query(
        "SELECT j.* FROM session_exercise_joints j " +
            "JOIN session_exercises se ON se.id = j.sessionExerciseId " +
            "WHERE se.sessionId = :sessionId AND j.deletedAt IS NULL AND se.deletedAt IS NULL " +
            "ORDER BY j.position",
    )
    fun observeForSession(sessionId: String): Flow<List<SessionExerciseJointEntity>>

    /**
     * Writes a save's whole list, so a replace is one statement rather than a row-per-joint loop
     * that a failure could stop halfway.
     */
    @Insert
    suspend fun insertAll(rows: List<SessionExerciseJointEntity>)

    /**
     * Hides one exercise's live rows before a save writes the new list (N63).
     *
     * A save **replaces** the list rather than merging into it: the editor shows exactly what is
     * stored, so a joint absent from it was removed by the lifter. Returns the rows hidden.
     */
    @Query(
        "UPDATE session_exercise_joints SET deletedAt = :at, updatedAt = :at " +
            "WHERE sessionExerciseId = :sessionExerciseId AND deletedAt IS NULL",
    )
    suspend fun softDeleteForExercise(sessionExerciseId: String, at: Long): Int

    /**
     * Hides every joint row of a session's exercises, for the session-wide delete (N63).
     *
     * The rows belong to the exercise, so removing the session has to take them or a deleted
     * workout's joints would still read as live in a restore.
     */
    @Query(
        "UPDATE session_exercise_joints SET deletedAt = :at, updatedAt = :at " +
            "WHERE deletedAt IS NULL AND sessionExerciseId IN " +
            "(SELECT id FROM session_exercises WHERE sessionId = :sessionId)",
    )
    suspend fun softDeleteForSession(sessionId: String, at: Long): Int

    // ------------------------------------------------------- backup (P1.12, N63)

    /** Deliberately unfiltered: a soft-deleted row is data an export must carry. */
    @Query("SELECT * FROM session_exercise_joints")
    suspend fun allForBackup(): List<SessionExerciseJointEntity>

    @Query("SELECT id FROM session_exercise_joints WHERE deletedAt IS NOT NULL")
    suspend fun softDeletedIds(): List<String>

    /**
     * Rewrites rows by primary key, which restores a soft-deleted row *and* clears its `deletedAt`
     * because the file's value overwrites it. Only ever called for ids already soft-deleted.
     */
    @Update
    suspend fun restore(rows: List<SessionExerciseJointEntity>): Int

    /** Returns one rowid per input row, `-1` where a row was skipped as a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<SessionExerciseJointEntity>): List<Long>
}
