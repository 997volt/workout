package com.example.androidapp.ui.history

import com.example.androidapp.domain.model.WorkoutSummary

/**
 * What the history list is asked to show (ROADMAP N146).
 *
 * A query and one chip, because those are the two questions a log this long actually gets asked: *where is
 * that workout* and *show me the days I did something other than lift*. The chip is an enum rather than a
 * boolean so a third answer is a value rather than a second flag, and so the state survives a rotation as a
 * name.
 */
enum class HistoryFilter {
    /** Every finished session, which is what the screen showed before there was a filter. */
    ALL,

    /** Only the days whose record is an activity — a swim, a ride (N141). */
    ACTIVITIES,

    /** Only the workouts, so the activities are out of the way of reading a training block. */
    WORKOUTS,
    ;

    /** Whether this filter can show [session]. */
    fun admits(session: WorkoutSummary): Boolean = when (this) {
        ALL -> true
        ACTIVITIES -> session.activity != null
        WORKOUTS -> session.activity == null
    }
}
