package com.example.androidapp.domain.model

import java.time.Duration

/**
 * How often an automatic backup writes a file (ROADMAP N218).
 *
 * Three periods rather than "every N days": the setting is a row of chips like the rest and the weight
 * unit, and a free number is a field to validate for a choice nobody has needed yet. A month is thirty
 * days and not a calendar month on purpose — the question the schedule asks is "has it been long enough",
 * not "which day of the month is it", and a calendar month would make February's own date the one that
 * never fires.
 *
 * Stored **by name**, like every enum here, so a schedule written by a build that knows one this build
 * does not falls back rather than being read as a different period.
 */
enum class BackupSchedule(val period: Duration) {
    DAILY(Duration.ofDays(1)),
    WEEKLY(Duration.ofDays(DAYS_IN_A_WEEK)),
    MONTHLY(Duration.ofDays(DAYS_IN_A_MONTH)),
    ;

    companion object {
        /** Weekly: often enough that a lost device costs a week, rare enough not to fill the folder. */
        val DEFAULT = WEEKLY

        /** The schedule a stored name means, or null when this build does not know it. */
        fun named(name: String?): BackupSchedule? = entries.firstOrNull { it.name == name }
    }
}

/** A week is seven days by definition, which is why this one is not a decision. */
private const val DAYS_IN_A_WEEK = 7L

/**
 * A month, as thirty days.
 *
 * Named because the number is a choice and not a fact: the period asks "has it been long enough", and a
 * calendar month would make February the one month whose own date never fires.
 */
private const val DAYS_IN_A_MONTH = 30L
