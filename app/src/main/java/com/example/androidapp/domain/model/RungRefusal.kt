package com.example.androidapp.domain.model

/**
 * Why a rung cannot go where it was asked for (N145).
 *
 * Four causes, four sentences: a lifter who is told *which* one it is can act on it — move the set, change the
 * role above, or log the working set first — where being told the rule again leaves them to work it out. A
 * failed attempt is not a warm-up, so it gets its own sentence rather than borrowing that one (N346).
 */
internal enum class RungRefusal {
    /** The set is the exercise's first, so there is nothing to hang a run off. */
    NothingAbove,

    /** The set above is a warm-up, which no run reads an anchor from (B63). */
    OnlyAWarmUp,

    /** The set above is a failed attempt: it is not work, so it cannot anchor a run (N329, N346). */
    FailedAttemptAbove,

    /** The set above is a rung of the other kind, and a run's anchor has to stand on its own. */
    ARungAbove,

    ;

    /**
     * The sentence a refusal is reported with (N145).
     *
     * Built here rather than read from a resource because the write boundary is a repository and has no
     * `Context` — the same reason every other message it throws is a string. The label follows the role the
     * lifter asked for, which is the one thing the sentence has to name.
     */
    fun sentence(role: SetType): String {
        val name = role.label.lowercase()
        return when (this) {
            NothingAbove ->
                "A $name set needs a working set above it, and this is the exercise's first set."

            OnlyAWarmUp ->
                "A $name set needs a working set above it, and the set above this one is a warm-up."

            FailedAttemptAbove ->
                "A $name set needs a working set above it, and the set above this one is a failed attempt."

            ARungAbove ->
                "A $name set needs a working set above it, and the set above this one is another kind of rung."
        }
    }
}

/**
 * The anchor decision behind [RungRefusal], for both callers (N145, N365).
 *
 * The picker asks it about the rows on screen, and the write boundary asks it about the rows an edit is
 * about to write. They used to hold two copies of the `when`, and the boundary's copy never gained the
 * failed-attempt cause N346 added to the picker's — so it called a failed attempt a warm-up. One function
 * means there is no second copy to forget.
 *
 * [above] is the run's anchor: the set above its first row, or null when the run starts the exercise.
 */
internal fun rungRefusalFor(role: SetType, above: SetType?): RungRefusal? = when {
    !role.isRung -> null
    above == null -> RungRefusal.NothingAbove
    above.recordsEffort -> null
    above.isRung -> RungRefusal.ARungAbove
    above == SetType.FAILED_ATTEMPT -> RungRefusal.FailedAttemptAbove
    else -> RungRefusal.OnlyAWarmUp
}
