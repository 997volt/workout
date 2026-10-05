package com.example.androidapp.domain.model

import java.time.Instant

/**
 * A single logged set (ROADMAP P1.3).
 *
 * [weightGrams] is whole grams — see `Weight` for why that beats kilograms as a
 * `Double`. [setIndex] is the position within its exercise, so a set keeps its
 * place even if an earlier one is deleted (the display renumbers; the identity
 * does not).
 */
data class SetEntry(
    val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weightGrams: Long,
    /**
     * How much assistance the machine gave, in grams, or 0 for none (ROADMAP N15).
     *
     * Not a signed weight: an assisted pull-up is not negative tonnage. Volume counts
     * `weightGrams * reps` alone, so an assisted set contributes nothing.
     */
    val assistanceGrams: Long = 0,
    val setType: SetType = SetType.NORMAL,
    /** Perceived effort, 1–10, or null when none was recorded (ROADMAP N6). */
    val rpeHalves: Int? = null,
    /**
     * A short comment on the set, or null (ROADMAP N6).
     *
     * Separate from `workout_sessions.notes`: one is about this set, the other
     * about the workout.
     */
    val note: String? = null,
    val completedAt: Instant? = null,
)

/** What kind of set this was. Warm-ups must not count towards PRs (P2.2). */
enum class SetType(val label: String) {
    NORMAL("Working"),
    WARMUP("Warm-up"),

    /**
     * The heavy single the rest of the session is built around (ROADMAP N14).
     *
     * Added for *planned* sets and available to performed ones, because a plan that
     * says "top set" and a log that cannot is two vocabularies for one idea. Stored
     * by name like every other enum, so adding it touched no row already on disk.
     */
    TOP_SET("Top set"),
    DROP("Drop"),
    FAILURE("Failure"),
    ;

    /**
     * Whether a set of this role records an effort (ROADMAP N67).
     *
     * A warm-up does not. It is preparation rather than work, so an RPE beside it measures
     * nothing the plan asked for and reads as a number the set was judged against. The rule lives
     * on the role so the write boundary, the editor and the row all read the same one, rather than
     * each deciding for itself what a warm-up is.
     */
    val recordsEffort: Boolean get() = this != WARMUP
}

/**
 * What this exercise looked like the last time it was trained.
 *
 * Used to prefill a new set (P1.3): the single most useful thing the app can put
 * on screen mid-workout is "last time you did 60 kg × 8". Empty means this
 * exercise has no completed history yet.
 */
data class PreviousPerformance(
    val sets: List<SetEntry>,
)
