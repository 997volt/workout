package com.example.androidapp.domain.model

/**
 * Something a lifter did that is not a workout (ROADMAP N141).
 *
 * A short **closed** list on purpose: the request was "cycling, running, swimming, rock climbing, volleyball —
 * don't put too many options", and an open field would be a second vocabulary for the same few things. Stored
 * **by name**, like every enum here, so a build that meets one it does not know can say so rather than
 * misreading a number.
 *
 * What an activity is *not* is the point of it: it holds no sets, so it answers "was the day trained" without
 * claiming any work was done. That is why the adherence calendar can colour it differently (N142) and why the
 * statistics need no new rule — a session with no sets already contributes no volume, no reps and no records.
 */
enum class Activity {
    CYCLING,
    RUNNING,
    SWIMMING,
    ROCK_CLIMBING,
    VOLLEYBALL,
    ;

    companion object {
        /**
         * The activity a stored name means, or null when this build does not know it.
         *
         * Null rather than a guess: a session naming something this build cannot show is still a session, and
         * inventing an activity for it would put a word in the lifter's mouth.
         */
        fun named(name: String?): Activity? = entries.firstOrNull { it.name == name }
    }
}
