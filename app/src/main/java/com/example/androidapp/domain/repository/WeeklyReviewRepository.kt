package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week's lift-level read (ROADMAP N251).
 *
 * A repository of its own rather than another method on the history's: the question is "which movements beat
 * themselves this week", which is an aggregate over sessions, not a session read.
 */
interface WeeklyReviewRepository {

    /**
     * The movements whose week moved, alphabetically: a first training or a heavier top set than before.
     *
     * [weekStart] is a Monday in [zone]; the boundaries are the device's week, the same one the review's days
     * and totals are read in.
     */
    suspend fun movementGains(weekStart: LocalDate, zone: ZoneId): DataResult<List<MovementGain>>

    /**
     * The finished sessions, newest first, as the history reads them.
     *
     * The week's totals and the previous week's comparison come from this one read; the facade exists so the
     * review's ViewModel asks one source for its week rather than four repositories (N252).
     */
    suspend fun history(): DataResult<List<WorkoutSummary>>

    /** What one day scheduled and what became of it, from the adherence record. */
    suspend fun occurrencesOn(
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<DayOccurrence>>

    /** The programs home follows. */
    suspend fun activePrograms(): DataResult<List<WorkoutProgram>>

    /** The first stopped prescription in [programId], or null. */
    suspend fun firstStall(programId: String): PlanStall?
}
