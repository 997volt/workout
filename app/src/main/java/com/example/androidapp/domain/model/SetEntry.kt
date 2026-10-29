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
     * Not a signed weight: an assisted pull-up is not negative tonnage. On an ordinary set the load is
     * [weightGrams] and assistance is a fact about the machine; on a bodyweight set it is subtracted from
     * the bodyweight, because there it is the difference between a pull-up and a jump ([loadGrams]).
     */
    val assistanceGrams: Long = 0,
    /**
     * The lifter's own weight when this set was bodyweight-loaded, in grams, or null for an ordinary set
     * (ROADMAP N219).
     *
     * **Snapshotted at logging time, not resolved when the set is read.** A later correction to a
     * measurement says something about the body on the day it was taken; rewriting what an old set weighed
     * would change every trend, record and volume figure that set supports, retroactively and without the
     * lifter asking. Null is "this set was its added weight", which is what every set written before this
     * column, and every ordinary set after it, means.
     */
    val bodyweightGrams: Long? = null,
    /**
     * How long this set was held, in seconds, or null for a counted set (ROADMAP N220).
     *
     * A timed set states **this** instead of reps: [reps] is 0, because a forty-five-second plank is not
     * forty-five of anything, and every total built on reps — volume, records, the trend's rep count — then
     * excludes it without a second rule. Load stays on the set, since a farmer's carry is both.
     */
    val durationSeconds: Int? = null,
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
)

/**
 * The load a set was performed at, in grams (ROADMAP N219).
 *
 * An ordinary set is its added weight, exactly as before. A **bodyweight** set is the lifter plus what was
 * added, less what the machine took off: 75 kg of bodyweight with a 20 kg belt is 95 kg, and an assisted
 * pull-up taking 30 kg off is 45 kg. The floor at zero is deliberate — a heavily assisted set is still a
 * set, and a negative load would silently subtract from every total built on it.
 *
 * One function for the whole app, because volume, the 1RM estimate and the record check all have to agree
 * about what "the load" is, and the previous shape of this rule was `weightGrams * reps` written out at
 * each of them.
 */
fun loadGrams(weightGrams: Long, bodyweightGrams: Long?, assistanceGrams: Long): Long =
    if (bodyweightGrams == null) {
        weightGrams
    } else {
        (bodyweightGrams + weightGrams - assistanceGrams).coerceAtLeast(0L)
    }

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

    /**
     * Whether this role is the plan's work rather than its preparation (ROADMAP N112).
     *
     * A warm-up is preparation, so it takes no set number and counts neither towards the plan's
     * remainder nor against it: five warm-ups and three working sets is a plan of **three**, numbered
     * 1, 2, 3. A **rung is work** — it keeps its number and its place in the count — even though it is
     * not rated on its own. That is what separates this from [recordsEffort]: that one asks what may be
     * recorded, this one asks what the plan asked for.
     */
    val isWork: Boolean get() = this != WARMUP
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
    /**
     * When that session started, or null when there is none (ROADMAP N233).
     *
     * Carried because the same read answers two questions: the workout prefill asks what to put in the
     * fields, and the exercise's own screen asks when the lift was last trained. Asking twice would be two
     * reads that could disagree about which session "last" means.
     */
    val sessionStartedAt: Instant? = null,
)
