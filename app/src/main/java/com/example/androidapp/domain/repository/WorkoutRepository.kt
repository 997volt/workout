package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * The session a workout screen is now in, and whether this call opened it.
 *
 * [isNew] is what lets the readiness prompt fire exactly once per workout
 * (ROADMAP N4): a resumed session has already had its chance.
 */
data class StartedSession(val id: String, val isNew: Boolean)

/**
 * Reading and writing workout sessions (ROADMAP P1.2, P1.3, P1.4, P1.8).
 *
 * Reads return plain flows; writes return [DataResult] so the UI can tell the
 * user when a write did *not* land (F7). Silently dropping a logged set is the
 * failure mode that loses a user's trust in the app, so it is modelled rather
 * than thrown.
 */
interface WorkoutRepository {

    /**
     * The in-progress session, or null. Observed rather than fetched once, so
     * every screen showing "workout in progress" stays in step automatically.
     */
    fun observeActiveSession(): Flow<WorkoutSession?>

    /** Exercises in [sessionId], in their stored order. */
    fun observeSessionExercises(sessionId: String): Flow<List<SessionExercise>>

    /** Every live set in [sessionId], ordered by exercise position then set. */
    fun observeSets(sessionId: String): Flow<List<SetEntry>>

    /**
     * Finished workouts, newest first, with their totals already aggregated
     * (ROADMAP P1.6).
     *
     * Only finished ones: an open session belongs on the workout screen, not in a
     * history you are meant to be reading.
     */
    fun observeHistory(): Flow<List<WorkoutSummary>>

    /** One session by id, so the detail screen observes rather than fetches. */
    fun observeSession(sessionId: String): Flow<WorkoutSession?>

    /**
     * Returns the in-progress session, creating one if there is none.
     *
     * Deliberately idempotent: two concurrent "start workout" taps must not
     * produce two open sessions, which would make "the active session"
     * ambiguous everywhere downstream. [StartedSession.isNew] distinguishes the
     * call that opened it from one that found it already open.
     *
     * When [templateId] is given *and this call opens the session*, that template's
     * exercises are appended in order (ROADMAP N3), through the same append path
     * [addExercise] uses. A resumed session is left alone: it already has the
     * exercises it was started with, and seeding it again would duplicate them.
     *
     * When [slotId] is also given, the slot's prescription wins over the template's for each
     * exercise's rest and cue (ROADMAP P3.8): a slot that says "3m break" is the plan being
     * followed, and the template is what it falls back to. The slot is not stored on the session
     * — P3.3's rule stands, and the session still records only the template.
     */
    suspend fun startOrResumeSession(
        templateId: String? = null,
        slotId: String? = null,
    ): DataResult<StartedSession>

    /**
     * Starts a session holding [sessionId]'s exercises, in their order (ROADMAP N29, N48).
     *
     * **Exercises and order only — not the loads.** Copying last session's weights as targets is the
     * obvious wrong turn: progression (N22) and the "last time" prefill already answer what to lift
     * next, and freezing a week's numbers into a fresh session would put the two in conflict, with the
     * plan-shaped copy quietly winning. What "same as last time" means here is *the same movements*.
     *
     * An exercise whose library row has been deleted since is skipped and the rest repeat; the same
     * exercise performed twice repeats twice, because that is what was performed.
     *
     * **The workout is addressed rather than "the last one"** (N48): History offers the action on the
     * row the user is looking at, so the source session is a parameter. A non-existent one is a
     * `NotFound` rather than an empty session, because silently opening a blank workout from a row
     * that named another would be worse than saying the row is gone.
     *
     * **An already-open session is resumed, not seeded and not refused** — it comes back with
     * `isNew = false` and whatever it already holds, which is the same rule every other start path
     * follows. Said plainly because the obvious reading of "a session is already open" is that the
     * call fails, and it does not.
     */
    suspend fun repeatSession(sessionId: String): DataResult<StartedSession>

    /** Appends [exerciseId] to the end of the session. */
    suspend fun addExercise(sessionId: String, exerciseId: String): DataResult<Unit>

    suspend fun removeExercise(sessionExerciseId: String): DataResult<Unit>

    /**
     * Moves one session exercise one place in the session's own order (ROADMAP N54).
     *
     * [delta] is -1 for up and +1 for down, matching the template editor's own gesture. The order is
     * the **session's**: the template the workout was started from is never written, which is N16's
     * "a session reads it at the start" applied to order rather than to targets. A move from the top
     * or the bottom is nothing to do rather than a failure, and the two positions swap in one
     * transaction so a failure cannot leave the list half-moved.
     */
    suspend fun moveExercise(sessionExerciseId: String, delta: Int): DataResult<Unit>

    /**
     * Marks a session exercise done (ROADMAP N7): no more sets can be logged, its
     * existing sets stop being editable, and any running rest is cleared.
     *
     * Not a delete — the sets stay in history — and reversible through
     * [reopenExercise], because accident protection must not become its own trap.
     */
    suspend fun finishExercise(sessionExerciseId: String): DataResult<Unit>

    /**
     * Writes the workout's own comment (ROADMAP N11), or clears it when [note] is
     * null or blank. Separate from the readiness note, which is about how you felt
     * going *in*: this is about how it went.
     */
    suspend fun setWorkoutNotes(sessionId: String, note: String?): DataResult<Unit>

    /** Reopens a done exercise (ROADMAP N7), restoring logging and editing. */
    suspend fun reopenExercise(sessionExerciseId: String): DataResult<Unit>

    /**
     * Writes how an exercise felt — muscle feel and the joints that hurt (ROADMAP N8, N63).
     *
     * An empty [joints] clears the picked list and a non-empty one **replaces** it rather than
     * merging, because the editor shows exactly what is stored. A score outside `TenPointScale` is
     * refused as `Invalid` before anything is written. The rating **replaces the legacy
     * `jointPain` / `jointPainNote`** columns too, so a list the lifter cleared stays cleared; a
     * session nobody re-rates keeps its number and its free text, which history still reads.
     *
     * Separate from [finishExercise] rather than folded into it: the ratings are
     * captured *at* Done but are skippable, and they stay editable from the workout
     * detail afterwards, which is a different write on a possibly-finished row.
     */
    suspend fun rateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<JointPain>,
    ): DataResult<Unit>

    /** Marks the session complete. It stops being "active" and enters history. */
    suspend fun finishSession(sessionId: String): DataResult<Unit>

    /**
     * Writes the session's readiness: the free-text note (ROADMAP N4) and the muscles it reported
     * sore (ROADMAP N62). A blank note is stored as null, so "nothing written" has one
     * representation; an empty [soreMuscles] clears the list, and a non-empty one **replaces** it
     * rather than merging, because the editor shows exactly what is stored.
     *
     * A score outside `TenPointScale` is refused as `Invalid` before anything is written, so a bad
     * list cannot half-land.
     */
    suspend fun setReadiness(
        sessionId: String,
        note: String?,
        soreMuscles: List<SoreMuscle>,
    ): DataResult<Unit>

    /** Soft-deletes the session and everything in it. */
    suspend fun deleteSession(sessionId: String): DataResult<Unit>

    /**
     * Logs a set at the end of [sessionExerciseId] (P1.3).
     *
     * [rpeHalves] and [note] are required rather than defaulted, for [updateSet]'s reason: the caller
     * states what the set is, so a screen that shows an effort cannot forget to write it. The inline
     * *Log set* did exactly that — the field had a number and every set it wrote came back with none
     * (N59).
     */
    suspend fun logSet(
        sessionExerciseId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType = SetType.NORMAL,
        /** The machine's assistance, as a magnitude (ROADMAP N15). */
        assistanceGrams: Long = 0,
    ): DataResult<Unit>

    /**
     * Rewrites a logged set, including its RPE and comment (ROADMAP N6).
     *
     * [rpeHalves] and [note] are required rather than defaulted, for [logSet]'s reason: an edit states
     * what the set now says, so a caller cannot clear them by forgetting to pass them. A blank RPE is
     * written as null deliberately, which is how a mistyped one is undone.
     */
    suspend fun updateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        /** The role the set was performed as (ROADMAP N14). */
        setType: SetType = SetType.NORMAL,
        /** The machine's assistance, as a magnitude (ROADMAP N15). */
        assistanceGrams: Long = 0,
    ): DataResult<Unit>

    suspend fun deleteSet(setId: String): DataResult<Unit>

    /**
     * What [exerciseId] looked like the last time it was trained (P1.3 prefill).
     *
     * Returns an empty [PreviousPerformance] when there is no history — that is a
     * normal answer, not a failure, so only a storage error is a `Failure`.
     */
    suspend fun previousPerformance(
        exerciseId: String,
        currentSessionId: String,
    ): DataResult<PreviousPerformance>

    /**
     * Starts (or restarts) the rest timer on the active session (P1.4), returning
     * the instant it will end.
     *
     * Returning the end instant rather than making the caller re-read it means an
     * alert is scheduled against exactly what was written — no window in which the
     * database and the alarm disagree.
     */
    suspend fun startRest(seconds: Int = RestTimer.DEFAULT_SECONDS): DataResult<Instant>

    /**
     * Puts this exercise in a superset with another, or takes it out (ROADMAP N24).
     *
     * The group is an ordinal shared with the exercises it is performed with; null means
     * "on its own again", which is what every exercise was before N24. **All of [sessionExerciseIds]
     * move together** — one statement — so a failure leaves the group as it was rather than half
     * changed (ROADMAP B27).
     */
    suspend fun setSupersetGroup(
        sessionExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit>

    /**
     * The heaviest working set at each rep count for this exercise (ROADMAP N23).
     *
     * [excludingSessionId] is the session in progress: a set must be compared against what
     * came *before* it, or the second set of a session would be checked against the first
     * one's record and every session would look like a breakthrough. `null` includes
     * everything, which is what a records view wants.
     */
    suspend fun personalRecords(
        exerciseId: String,
        excludingSessionId: String? = null,
    ): DataResult<PersonalRecords>

    /** Moves the running rest timer by [deltaSeconds]; a finished rest restarts from now. */
    suspend fun adjustRest(deltaSeconds: Int): DataResult<Instant>

    /** Stops the rest timer. */
    suspend fun clearRest(): DataResult<Unit>
}
