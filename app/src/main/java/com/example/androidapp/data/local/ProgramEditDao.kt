package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

/**
 * The writes an edit needs, kept apart from [BackupDao] (ROADMAP N171).
 *
 * A backup is insert-or-ignore: it may add rows and must never change one, which is why its DAO is
 * shaped that way. An edit is the opposite promise — it updates rows the device already holds and
 * soft-deletes the ones the document no longer names — so its writes live in their own interface
 * rather than widening the one that promises not to overwrite. A reader can then tell which path a
 * write belongs to from the DAO it calls.
 */
@Dao
interface ProgramEditDao {

    /** Rewrites slots in place: a weekday, a position, or the workout the slot points at. */
    @Update
    suspend fun updateProgramSlots(rows: List<ProgramSlotEntity>): Int

    @Update
    suspend fun updateTemplates(rows: List<TemplateEntity>): Int

    @Update
    suspend fun updateTemplateExercises(rows: List<TemplateExerciseEntity>): Int

    @Update
    suspend fun updateTemplateSets(rows: List<TemplateSetEntity>): Int

    /** A removed day leaves the schedule; its workout and history stay (the app's own removal rule). */
    @Query("UPDATE program_slots SET deletedAt = :at, updatedAt = :at WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun softDeleteProgramSlots(ids: List<String>, at: Long): Int

    @Query("UPDATE template_exercises SET deletedAt = :at, updatedAt = :at WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun softDeleteTemplateExercises(ids: List<String>, at: Long): Int

    /** A removed exercise takes its planned sets with it; nothing else reads them. */
    @Query(
        """
        UPDATE template_sets
        SET deletedAt = :at, updatedAt = :at
        WHERE templateExerciseId IN (:ids) AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteSetsOfTemplateExercises(ids: List<String>, at: Long): Int

    @Query("UPDATE template_sets SET deletedAt = :at, updatedAt = :at WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun softDeleteTemplateSets(ids: List<String>, at: Long): Int
}
