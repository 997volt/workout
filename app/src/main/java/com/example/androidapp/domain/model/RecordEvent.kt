package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit
import java.time.Instant

/**
 * One record in the timeline (ROADMAP N290).
 *
 * The rule that decides whether a set is a record stays [PersonalRecords]': strictly heavier at the same
 * rep count, never a warm-up or a rung, and — since N233 — the longest hold for a timed movement. This type
 * is only the result, shaped for a list that says *which lift*, *what it did* and *when*, and it is a sealed
 * pair because a hold and a load do not share a number to print.
 */
sealed interface RecordEvent {

    /** The lift this record belongs to, read from the library at the time it is shown. */
    val exerciseName: String

    /** When the session that holds the record started, the instant every history list orders by. */
    val performedAt: Instant

    /** The zone the session was performed in, or null for rows that predate the column (ROADMAP N25). */
    val zoneOffsetMinutes: Int?

    /** A weighted set: the best at one rep count on the day it was set. */
    data class Load(
        override val exerciseName: String,
        override val performedAt: Instant,
        override val zoneOffsetMinutes: Int?,
        /** The unit this exercise is read in, or null to follow the app setting (ROADMAP N64). */
        val weightUnit: WeightUnit?,
        val weightGrams: Long,
        val reps: Int,
    ) : RecordEvent

    /**
     * A hold: the longest duration at the time it was performed (ROADMAP N233).
     *
     * A timed movement has no rep count to be best at, so its record is time. A bodyweight carry still has
     * one, which is why this shape carries no weight at all.
     */
    data class Hold(
        override val exerciseName: String,
        override val performedAt: Instant,
        override val zoneOffsetMinutes: Int?,
        val seconds: Int,
    ) : RecordEvent
}
