package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The weeks a program was deliberately backed off, and the days those weeks prescribe (ROADMAP P3.10,
 * N147).
 *
 * A DAO of its own because [ProgramDao] is at the function ceiling this project enforces, and
 * because a deload is an *event* rather than a program's own row — the same reasoning that gave
 * the run its own reads (P3.9).
 */
@Dao
interface ProgramDeloadDao {

    /** The live deloads for any week in [from, to] inclusive, both epoch days (P3.10). */
    @Query(
        """
        SELECT * FROM program_deloads
        WHERE weekStart >= :from AND weekStart <= :to AND deletedAt IS NULL
        """,
    )
    suspend fun findDeloadsBetween(from: Long, to: Long): List<ProgramDeloadEntity>

    @Query(
        """
        SELECT COUNT(*) FROM program_deloads
        WHERE programId = :programId AND weekStart = :weekStart AND deletedAt IS NULL
        """,
    )
    suspend fun countDeload(programId: String, weekStart: Long): Int

    @Insert
    suspend fun insertDeload(row: ProgramDeloadEntity)

    /** The live mark for one program and week, or null (ROADMAP N147). */
    @Query(
        """
        SELECT * FROM program_deloads
        WHERE programId = :programId AND weekStart = :weekStart AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findLiveDeload(programId: String, weekStart: Long): ProgramDeloadEntity?

    /** Moves the mark's own timestamp when its plan is rewritten (N147). */
    @Query("UPDATE program_deloads SET updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun touchDeload(id: String, at: Long): Int

    /**
     * The days one marked week offers, with their templates, live only (ROADMAP N147).
     *
     * The join is the rule that a deload day points at a living plan: a deleted template takes its day
     * out of every read rather than leaving a card that starts nothing.
     */
    @Query(
        """
        SELECT d.id AS id,
               dl.programId AS programId,
               dl.weekStart AS weekStart,
               d.deloadId AS deloadId,
               d.templateId AS templateId,
               t.name AS templateName,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               d.position AS position,
               d.weekday AS weekday
        FROM program_deload_days d
        JOIN program_deloads dl ON dl.id = d.deloadId
        JOIN templates t ON t.id = d.templateId
        WHERE dl.weekStart = :weekStart
          AND dl.deletedAt IS NULL
          AND d.deletedAt IS NULL
          AND t.deletedAt IS NULL
        ORDER BY dl.programId ASC, d.position ASC
        """,
    )
    fun observeDeloadDaysForWeek(weekStart: Long): Flow<List<DeloadDayRow>>

    /** The same read, one shot — what the missed-day question asks (N147). */
    @Query(
        """
        SELECT d.id AS id,
               dl.programId AS programId,
               dl.weekStart AS weekStart,
               d.deloadId AS deloadId,
               d.templateId AS templateId,
               t.name AS templateName,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               d.position AS position,
               d.weekday AS weekday
        FROM program_deload_days d
        JOIN program_deloads dl ON dl.id = d.deloadId
        JOIN templates t ON t.id = d.templateId
        WHERE dl.weekStart = :weekStart
          AND dl.deletedAt IS NULL
          AND d.deletedAt IS NULL
          AND t.deletedAt IS NULL
        ORDER BY dl.programId ASC, d.position ASC
        """,
    )
    suspend fun findDeloadDaysForWeek(weekStart: Long): List<DeloadDayRow>

    /**
     * The days of every marked week in [from, to], for the calendar and the day dialog (B161).
     *
     * The same projection as the one-week read, so a built week's days reach both surfaces with their
     * templates and weekdays; a week with only a mark contributes no rows.
     */
    @Query(
        """
        SELECT d.id AS id,
               dl.programId AS programId,
               dl.weekStart AS weekStart,
               d.deloadId AS deloadId,
               d.templateId AS templateId,
               t.name AS templateName,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               d.position AS position,
               d.weekday AS weekday
        FROM program_deload_days d
        JOIN program_deloads dl ON dl.id = d.deloadId
        JOIN templates t ON t.id = d.templateId
        WHERE dl.weekStart >= :from
          AND dl.weekStart <= :to
          AND dl.deletedAt IS NULL
          AND d.deletedAt IS NULL
          AND t.deletedAt IS NULL
        ORDER BY dl.weekStart ASC, dl.programId ASC, d.position ASC
        """,
    )
    suspend fun findDeloadDaysBetween(from: Long, to: Long): List<DeloadDayRow>

    @Insert
    suspend fun insertDeloadDay(row: ProgramDeloadDayEntity)

    /**
     * Soft-deletes a week's plan when it is rebuilt (N147).
     *
     * Soft, like every other delete here: "this week had a plan and then had another" is history too, and
     * the export carries the rows. The templates the old plan named stay in the library untouched.
     */
    @Query(
        """
        UPDATE program_deload_days
        SET deletedAt = :at, updatedAt = :at
        WHERE deloadId = :deloadId AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteDeloadDays(deloadId: String, at: Long): Int

    /**
     * Soft-deletes one deload, returning the rows changed.
     *
     * Unmarking is always allowed and always a soft delete, like every other table here: an export
     * carries the row, and "this week was a deload and then was not" is history too.
     */
    @Query(
        """
        UPDATE program_deloads
        SET deletedAt = :at, updatedAt = :at
        WHERE programId = :programId AND weekStart = :weekStart AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteDeload(programId: String, weekStart: Long, at: Long): Int
}
