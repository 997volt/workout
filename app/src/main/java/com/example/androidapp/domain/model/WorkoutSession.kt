package com.example.androidapp.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A training session (ROADMAP P1.2).
 *
 * `java.time.Instant` rather than an epoch `Long` in the domain: `minSdk` 26 was
 * chosen partly so `java.time` is available natively (ROADMAP F9), and the richer
 * type makes duration arithmetic obvious. Only the *entity* stores a Long.
 *
 * [finishedAt] being null is what makes a session "active" — and is the whole
 * basis of crash recovery (P1.8): the row is written the moment a workout starts,
 * so a process death leaves it open rather than losing it.
 */
/**
 * The zone this session was performed in, or null when it predates the column (ROADMAP N25).
 *
 * Null is answered by the caller, and the only honest answer is the current zone — which is what
 * every screen showed before this existed. Two readers want slightly different things from it, so it
 * is a function rather than a property: a row's own time, and the month or day it belongs to.
 */
fun WorkoutSession.zoneIdOrNull(): ZoneId? =
    zoneOffsetMinutes?.let { ZoneOffset.ofTotalSeconds(it * SECONDS_PER_MINUTE) }

/** Minutes to seconds, the unit [ZoneOffset] wants. */
private const val SECONDS_PER_MINUTE = 60

data class WorkoutSession(
    val id: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    /** When the current rest ends, or null when not resting (P1.4). */
    val restEndsAt: Instant? = null,
    /**
     * What was not recovered today (ROADMAP N4). Free text, prompted once when the
     * session opens and editable afterwards from the workout header.
     */
    val readinessNote: String? = null,
    /**
     * The muscles today's readiness note reported sore, each with its own score (ROADMAP N62).
     *
     * Beside [readinessNote] rather than replacing it: the note stays the free-text line for what a
     * list cannot say, and this is the structured fact — "quads 8, calves 3" — that can be read one
     * muscle at a time later. Empty means nothing was reported, which is also what a skipped prompt
     * writes.
     */
    val soreMuscles: List<SoreMuscle> = emptyList(),
    /**
     * The zone the session was performed in, in minutes from UTC, or null when it is not known
     * (ROADMAP N25).
     *
     * Null is not a missing value to be filled in later: it means the session predates this column,
     * and the honest answer for those is the current zone — which is what every screen showed before.
     */
    val zoneOffsetMinutes: Int? = null,
    /**
     * The workout's own comment (ROADMAP N11), or null.
     *
     * The column has existed since v1 and was reserved for exactly this; it is the
     * one free-text field about the workout as a whole, as distinct from
     * [readinessNote], which is about how you felt going in, and a set's note
     * (N6), which is about one set.
     */
    val notes: String? = null,
)

/**
 * An exercise as it appears *within* a session, at an explicit [position].
 *
 * Carries the library attributes ([exerciseName], [primaryMuscle], [equipment])
 * because that is exactly what the workout screen displays, and fetching them in
 * the same query avoids an N+1 walk over the library while the user is mid-set.
 *
 * [position] is stored rather than inferred from insertion order, so reordering a
 * workout never depends on row ids sorting usefully.
 */
data class SessionExercise(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
    /**
     * Which superset or circuit this exercise is performed in, or null (ROADMAP N24).
     *
     * Exercises sharing a number are done in rounds: a set of each in turn, and the rest
     * belongs to the round rather than to the set.
     */
    val supersetGroup: Int? = null,
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val equipment: Equipment,
    /** The library exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /** A cue to show under the name while lifting (ROADMAP N5). */
    val techniqueNote: String? = null,
    /**
     * When this exercise was marked done (ROADMAP N7), or null while it is open.
     *
     * Deliberately not a delete: [finishedAt] set means no more sets are added and
     * the existing ones cannot be edited until it is reopened.
     */
    val finishedAt: Instant? = null,
    /**
     * How well the target muscle was worked, 1–10, or null (ROADMAP N8).
     *
     * Stored per session exercise rather than per library exercise, so the same
     * movement is measured differently on different days. Skippable.
     */
    val muscleFeel: Int? = null,
    /** Discomfort in joints or connective tissue, 1–10, or null (ROADMAP N8). */
    val jointPain: Int? = null,
    /** Which joints, e.g. "left shoulder" (ROADMAP N9), or null. */
    val jointPainNote: String? = null,
    /**
     * The joints that hurt, each with its side and its own score (ROADMAP N63).
     *
     * Replaces the single [jointPain] number and its free-text [jointPainNote] as the way a rating
     * is given — one score per picked joint, left and right apart — but neither column is removed:
     * a session rated before the change keeps its number and its text, and history reads them.
     * Empty means no joint was picked, which is also what a skipped rating writes.
     */
    val joints: List<JointPain> = emptyList(),
) {
    val isFinished: Boolean get() = finishedAt != null
}
