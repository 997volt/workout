package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

/**
 * The writes an edit needs — and the one read only a device can answer — kept apart from [BackupDao]
 * (ROADMAP N171).
 *
 * A backup is insert-or-ignore: it may add rows and must never change one, which is why its DAO is
 * shaped that way. An edit is the opposite promise — it updates rows the device already holds and
 * soft-deletes the ones the document no longer names — so its writes live in their own interface
 * rather than widening the one that promises not to overwrite. A reader can then tell which path a
 * write belongs to from the DAO it calls.
 */
@Dao
interface ProgramEditDao {

    /**
     * The newest write anywhere in one program's definition, **soft-deleted rows included** (B183).
     *
     * The document an edit was made from cannot carry a deleted row — deleting is what removes it —
     * so the plan's own staleness read is blind to a deletion. This is the question only the device
     * can answer: the same `updatedAt` the plan compares, taken over the program, every slot it ever
     * had, the workouts those slots named, and their planned exercises and sets. A null answer means
     * the program has no rows here at all, which the plan refuses as a missing program anyway.
     */
    @Query(
        """
        SELECT MAX(updatedAt) FROM (
            SELECT updatedAt FROM programs WHERE id = :programId
            UNION ALL
            SELECT updatedAt FROM program_slots WHERE programId = :programId
            UNION ALL
            SELECT updatedAt FROM templates
                WHERE id IN (SELECT templateId FROM program_slots WHERE programId = :programId)
            UNION ALL
            SELECT updatedAt FROM template_exercises
                WHERE templateId IN (SELECT templateId FROM program_slots WHERE programId = :programId)
            UNION ALL
            SELECT updatedAt FROM template_sets
                WHERE templateExerciseId IN (
                    SELECT id FROM template_exercises
                    WHERE templateId IN (SELECT templateId FROM program_slots WHERE programId = :programId)
                )
        )
        """,
    )
    suspend fun newestWriteForProgram(programId: String): Long?

    /** Rewrites slots in place: a weekday, a position, or the workout the slot points at. */
    @Update
    suspend fun updateProgramSlots(rows: List<ProgramSlotEntity>): Int

    /**
     * Rewrites the columns a program edit **owns** on a template, and nothing else (ROADMAP N214).
     *
     * `@Update` would write every column, and a program document cannot describe `hiddenAt` — hiding is
     * the library's view, not the program's definition — so the decoded row reads as null and would
     * bring a workout the lifter hid back into the offers. The columns named here are exactly what a
     * document carries; the rest of the row is the device's.
     */
    @Query(
        """
        UPDATE templates
        SET name = :name, versionOf = :versionOf, updatedAt = :at, deletedAt = NULL
        WHERE id = :id
        """,
    )
    suspend fun updateTemplatePlan(id: String, name: String, versionOf: String?, at: Long): Int

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
