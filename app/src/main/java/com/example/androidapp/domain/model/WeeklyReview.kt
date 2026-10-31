package com.example.androidapp.domain.model

import java.time.LocalDate
import java.time.ZoneId

/** What one week's finished sessions add up to (ROADMAP N247). */
data class WeekTotals(
    val sessions: Int,
    val sets: Int,
    val volumeGrams: Long,
)

/**
 * The week's scheduled occurrences, and how many happened (ROADMAP N247).
 *
 * A **deload** is a scheduled rest rather than a session, so it is not counted as planned (it is still drawn
 * in the day list). A pending or skipped occurrence is planned and not done, which is what "N of M done"
 * means while the week is still running.
 */
data class WeekPlan(
    val planned: Int,
    val done: Int,
) {
    /** Scheduled sessions that have not been done — missed, skipped or still ahead. */
    val remaining: Int get() = (planned - done).coerceAtLeast(0)
}

/** One day of the review, with what was scheduled on it (ROADMAP N247). */
data class ReviewDay(
    val date: LocalDate,
    val occurrences: List<DayOccurrence>,
)

/**
 * The finished sessions whose own day falls in the week beginning [weekStart].
 *
 * The session's **own** zone, not the device's (N25): a workout performed abroad belongs to the day it
 * happened, which is the same rule the history and the adherence grid read.
 */
fun weekTotals(sessions: List<WorkoutSummary>, weekStart: LocalDate, zone: ZoneId): WeekTotals {
    val end = weekStart.plusDays(DAYS_IN_WEEK)
    val inWeek = sessions.filter { session ->
        val day = session.startedAt.atZone(zone).toLocalDate()
        !day.isBefore(weekStart) && day.isBefore(end)
    }
    return WeekTotals(
        sessions = inWeek.size,
        sets = inWeek.sumOf { it.setCount },
        volumeGrams = inWeek.sumOf { it.volumeGrams },
    )
}

/** The week's plan, counted from its occurrences: see [WeekPlan] for what a deload counts as. */
fun weekPlan(occurrences: List<DayOccurrence>): WeekPlan {
    val sessions = occurrences.filter { it.state != OccurrenceState.DELOAD }
    return WeekPlan(
        planned = sessions.size,
        done = sessions.count { it.state == OccurrenceState.DONE },
    )
}

private const val DAYS_IN_WEEK = 7L
