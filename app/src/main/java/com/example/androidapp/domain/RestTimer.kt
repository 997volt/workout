package com.example.androidapp.domain

import java.time.Duration
import java.time.Instant

/**
 * Rest-timer arithmetic (ROADMAP P1.4).
 *
 * The timer is stored as an absolute end instant, so "how long is left" is a
 * pure function of the clock rather than a counter someone has to keep ticking
 * correctly. That is what makes it survive a process death and stay accurate
 * while the screen is off.
 */
object RestTimer {

    /**
     * The default rest between sets. A user-configurable value is part of P1.4's
     * "configurable default"; until there is a settings screen, one constant.
     */
    const val DEFAULT_SECONDS = 90

    /** How much the +15s / -15s controls move the timer. */
    const val ADJUST_STEP_SECONDS = 15

    /**
     * The lowest rest a prescription may name (ROADMAP N45).
     *
     * **Zero is a value**: it means "this exercise has no rest", and it already behaves that way
     * downstream — `startRest` computes an end instant at *now*, so a zero rest simply never runs.
     * The floor is therefore zero, not one, and a negative is the only number the field refuses.
     */
    const val MIN_PRESCRIBED_SECONDS = 0

    /**
     * The one sentence a negative rest is refused with (ROADMAP N45).
     *
     * It replaces three sentences for one rule, written in three repositories, each of which also
     * called zero a mistake. The field's hint carries the construction that should be used instead.
     */
    const val NEGATIVE_REST_REFUSAL =
        "A rest cannot be negative. Leave it empty for the default, or 0 for none."

    /** Whole seconds left, never negative. 0 means the rest is over or not running. */
    fun remainingSeconds(restEndsAt: Instant?, now: Instant): Int {
        val endsAt = restEndsAt ?: return 0
        return Duration.between(now, endsAt).seconds.coerceAtLeast(0L).toInt()
    }

    fun isRunning(restEndsAt: Instant?, now: Instant): Boolean =
        remainingSeconds(restEndsAt, now) > 0

    /** The same formatter the workout's elapsed time uses — see [DurationFormat]. */
    fun format(seconds: Int): String = DurationFormat.ofSeconds(seconds.toLong())
}
