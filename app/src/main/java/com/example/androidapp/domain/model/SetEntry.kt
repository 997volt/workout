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

/**
 * What kind of set this was: a set of its own, or a *rung* of the group above it (ROADMAP N79).
 *
 * Warm-ups and rungs are excluded from records (P2.2, N79), and neither is rated on its own.
 */
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

    /**
     * A set taken at a lighter load straight after the one above it (ROADMAP N14, N79).
     *
     * Its weight is not its own: it is the anchor's less a stored drop value, once per rung, so the
     * ladder moves when the anchor does (N79).
     */
    DROP("Drop"),

    /**
     * Sets repeated at the anchor's own weight after a short break (ROADMAP N79).
     *
     * The same shape as [DROP] — a member of the group above it rather than a set of its own — and the
     * one rule between them is the load: a cluster rung carries the anchor's weight itself, a drop rung
     * the anchor's less the run's drop value. Stored by name like every other enum, so adding it
     * touched no row already on disk.
     */
    CLUSTER("Cluster"),
    FAILURE("Failure"),
    ;

    /**
     * Whether this role is a member of a group rather than a set of its own (ROADMAP N79).
     *
     * A rung hangs off the set before it: it carries no target of its own, the group is judged on its
     * first set, and (for a drop) its weight is derived rather than written down.
     */
    val isRung: Boolean get() = this == DROP || this == CLUSTER

    /**
     * Whether this role stands as a performance of its own: rated, and able to be a record (N67, N79).
     *
     * A warm-up does not, because it is preparation rather than work — an RPE beside it measures
     * nothing the plan asked for. A rung does not either, and for a different reason: it **is** work,
     * but it is not rated or recorded on its own, because the group is one effort and its first set is
     * what the app reads. The rule lives on the role so the write boundary, the editors, the rows and
     * the record scan all read the same one, rather than each deciding for itself.
     */
    val recordsEffort: Boolean get() = !isRung && this != WARMUP
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
