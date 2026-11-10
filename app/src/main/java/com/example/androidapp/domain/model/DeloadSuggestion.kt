package com.example.androidapp.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Whether a program that has no end is due a recovery week (ROADMAP N301).
 *
 * A **suggestion**, not a computed deload: the cadence is the lifter's choice, the app only counts the
 * weeks it can see, and the count starts from the last recovery week or — when there has not been one —
 * from the program's start. Nothing here measures recovery and nothing writes a plan.
 */
data class DeloadSuggestion(
    /** Whole weeks since the last recovery week, or since the program started when there has been none. */
    val weeksSince: Int,
    /** True when the count started from a recovery week rather than the program's start. */
    val sinceLastDeload: Boolean,
    /** True when [weeksSince] reached the chosen cadence. */
    val due: Boolean,
)

/**
 * The suggestion for one program at [thisWeek], or a not-due answer when [cadenceWeeks] is off (0).
 *
 * Both dates are week starts (a Monday), so the count is whole weeks and cannot drift by the weekday the
 * screen is opened on. A start date after the current week clamps to zero rather than going negative.
 */
fun deloadSuggestion(
    cadenceWeeks: Int,
    lastDeloadWeek: LocalDate?,
    programStartedWeek: LocalDate,
    thisWeek: LocalDate,
): DeloadSuggestion {
    val base = lastDeloadWeek ?: programStartedWeek
    val weeks = ChronoUnit.WEEKS.between(base, thisWeek).toInt().coerceAtLeast(0)
    return DeloadSuggestion(
        weeksSince = weeks,
        sinceLastDeload = lastDeloadWeek != null,
        due = cadenceWeeks > 0 && weeks >= cadenceWeeks,
    )
}
