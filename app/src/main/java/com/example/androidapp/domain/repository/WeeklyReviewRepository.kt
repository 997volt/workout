package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.MovementGain
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
}
