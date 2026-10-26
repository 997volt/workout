package com.example.androidapp.domain

import java.time.Duration
import java.util.Locale

/**
 * The app's one elapsed-time format: `m:ss`, or `h:mm:ss` past an hour.
 *
 * `RestTimer.format` and `WorkoutFormat.elapsed` had each grown their own copy of
 * this. Two copies of one rule is how they drift, and they already had: only the
 * workout's version handled hours, so a long rest would have rendered `90:00` while
 * a workout of the same length rendered `1:30:00`. It lives in `domain` because the
 * rest timer is domain and the UI is allowed to depend on domain, not the reverse.
 */
object DurationFormat {

    /** Negative input clamps to zero rather than rendering a negative time. */
    fun ofSeconds(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / SECONDS_PER_HOUR
        val minutes = (safe % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = safe % SECONDS_PER_MINUTE

        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    fun of(duration: Duration): String = ofSeconds(duration.seconds)

    /**
     * What a typed hold means in seconds, or null when it is not a duration at all.
     *
     * Accepts what [ofSeconds] prints and what a lifter types: `45`, `1:30` and `1:30:00`. Blank is null —
     * an empty field is an unanswered question, not a zero-second hold, the same distinction reps makes
     * between blank and a number. The ceiling is the set boundary's, not this one's: a parser that refused
     * values would have to agree with a constant two layers away, and the write is where the rule belongs.
     */
    fun parseSeconds(text: String): Int? {
        val parts = text.trim().split(':')
        // One to three digit groups and nothing else: `45`, `1:30`, `1:30:00`. Anything else is unanswered.
        val numeric = parts.size in 1..MAX_FIELDS && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }
        val numbers = if (numeric) parts.map { it.toLongOrNull() } else emptyList()
        // Folded rather than branched per shape: each group is the next base-60 column, so 45, 1:30 and
        // 1:30:00 are one expression and a fourth group cannot quietly mean something else.
        val total = if (numbers.isNotEmpty() && numbers.all { it != null }) {
            numbers.filterNotNull().fold(0L) { seconds, part -> seconds * SECONDS_PER_MINUTE + part }
        } else {
            null
        }
        return total?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
    }

    private const val SECONDS_PER_MINUTE = 60L

    /** The most groups a duration has: seconds, minutes, hours. */
    private const val MAX_FIELDS = 3
    private const val SECONDS_PER_HOUR = 3_600L
}
