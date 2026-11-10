package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PlanComparison
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.ProgressionTargets
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.WriteOutcome

/** A logged set as the screen shows it: position, weight, reps, and N6's extras. */
data class SetRow(
    val id: String,
    /**
     * The set's place among the sets that stand on their own, or null for a warm-up (ROADMAP N112).
     *
     * Nullable rather than zero: a warm-up takes no number at all, and the row draws a blank column
     * where one would be ([com.example.androidapp.ui.components.SetNumber]) rather than the 1 a zero
     * would read as.
     */
    val number: Int?,
    val reps: Int,
    val weightGrams: Long,
    /** 1–10, or null when none was recorded (ROADMAP N6). */
    val rpeHalves: Int? = null,
    /** A short comment on the set, or null (ROADMAP N6). */
    val note: String? = null,
    /** The role it was performed as (ROADMAP N14). */
    val setType: SetType = SetType.NORMAL,
    /** The machine's assistance, 0 for none (ROADMAP N15). */
    val assistanceGrams: Long = 0,
    /** The bodyweight this set was logged at, or null for an ordinary set (ROADMAP N219). */
    val bodyweightGrams: Long? = null,
    /** How long the set was held, or null for a counted set (ROADMAP N220). */
    val durationSeconds: Int? = null,
    /** The unit this exercise's numbers read in (ROADMAP N64). */
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
)

/**
 * The answer to one set edit (ROADMAP B284/N284): the shared write outcome names the set.
 */
typealias SetWrite = WriteOutcome<String>

/**
 * A bodyweight set held while the lifter says what they weigh (ROADMAP N219).
 *
 * The edit travels with it, because the tap that started it was the *Log set* the fields stated — asking
 * again after the answer would lose what was typed between them.
 */
data class PendingBodyweight(
    val sessionExerciseId: String,
    val exerciseName: String,
    val edit: SetEdit,
    /**
     * How old the newest weigh-in is, in days, when the set is held for being stale, or null when there is
     * no weight at all (B251). The dialog says which question it is asking.
     */
    val staleDays: Long? = null,
)

/**
 * One step auto progression wrote while finishing an exercise (ROADMAP N299).
 *
 * The log's id is what undo addresses; the two targets are the whole set's target, not only the
 * field [direction] names, because a load step also resets the range's place.
 */
data class AutoProgressionStep(
    val writeId: String,
    val direction: ProgressionDirection,
    val before: ProgressionTargets,
    val after: ProgressionTargets,
)

/**
 * What the workout says after auto progression adjusted an exercise (ROADMAP N299).
 *
 * Non-null is what draws the banner. It holds every step of the one *Done*, because one press can
 * move several sets and an undo that took back only the last would leave the rest of the exercise
 * adjusted with nothing on screen saying so.
 */
data class AutoProgressionMoment(
    val exerciseName: String,
    val unit: WeightUnit,
    val steps: List<AutoProgressionStep>,
)

/** One exercise in the workout, with its sets and what the next set will prefill. */
data class SessionExerciseRow(
    val id: String,
    val exerciseId: String,
    /** The planned exercise this row substitutes, or null (N291). */
    val substitutedFromExerciseId: String? = null,
    val name: String,
    /** `Quads · Barbell`, or null while a custom exercise's taxonomy is unset. */
    val subtitle: String?,
    /** A cue to read while lifting, or null (ROADMAP N5). */
    val techniqueNote: String? = null,
    /**
     * The superset or circuit this exercise is performed in, or null (ROADMAP N24).
     *
     * Exercises sharing it are done in rounds, so the screen labels them A1/A2 and the rest
     * waits for the round to finish.
     */
    val supersetGroup: Int? = null,
    /** `A1`/`A2` for a grouped exercise — giant-set notation, or null (ROADMAP N24). */
    val supersetLabel: String? = null,
    /** This exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int? = null,
    /**
     * The unit every number belonging to this exercise reads in (ROADMAP N64).
     *
     * The exercise's own override where it has one, the app setting otherwise, resolved once here
     * rather than at each of the places that show one of its weights.
     */
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long? = null,
    /**
     * The plan's places whose run carries on into the next row (ROADMAP N79, B257).
     *
     * A place rather than a stored `setIndex`, because the reader asks "is the run still open after
     * the sets logged so far" — a count — and a plan imported from a document numbered from one used
     * to answer one set late (B257). The rest is the group's, so it starts after the run's last rung
     * rather than between its rungs — and the decision is made while a set is being logged, where the
     * plan is no longer in hand. Empty for a plan with no runs in it, which is every plan before this
     * and most after it.
     */
    val runContinuesAfter: Set<Int> = emptySet(),
    /**
     * Whether this movement's sets are held rather than counted (ROADMAP N220).
     *
     * What the *next* set's fields state: a duration instead of a rep count.
     */
    val isTimed: Boolean = false,
    /** The movement's own answer to *Done*, or null to follow the app setting (ROADMAP N235). */
    val progressionMode: ProgressionMode? = null,
    /** The directions of a step offered and not taken, for the review (ROADMAP N236). */
    val declinedDirections: Set<ProgressionDirection> = emptySet(),
    /**
     * Whether this movement is loaded by the body (ROADMAP N219).
     *
     * What the *next* set's weight field means: on a bodyweight movement it holds what was added, and the
     * lifter's own weight is snapshotted onto the set as it is logged.
     */
    val isBodyweight: Boolean = false,
    /**
     * How many sets the plan behind this workout writes for this exercise, or null when there is no
     * plan (ROADMAP N52).
     *
     * Null is not zero: an empty workout, or one whose exercise was added by hand, has nothing to
     * be done with, so the control never turns into *Log extra set* on a plan that does not exist.
     * A number here is the whole plan for the exercise — a program's slot and a template chosen
     * directly arrive as the same `templateId`, so both count.
     */
    val plannedSetCount: Int? = null,
    /**
     * True once this exercise has been marked done (ROADMAP N7): its Log set button
     * is hidden, its sets are dimmed and not editable, and Reopen restores both.
     */
    val isFinished: Boolean = false,
    /** How well the target muscle was worked, 1–10, or null (ROADMAP N8). */
    val muscleFeel: Int? = null,
    /**
     * The joints that hurt, each with its side and its own score (ROADMAP N63).
     *
     * The picked list a new rating writes; empty means none was picked.
     */
    val joints: List<JointPain> = emptyList(),
    val sets: List<SetRow> = emptyList(),
    val suggestion: SetSuggestion = SetSuggestion(DEFAULT_REPS, Weight.DEFAULT_GRAMS),
    /**
     * What *Done* opens with (ROADMAP N50, N74): one row per working set — the plan's target for it,
     * what the session did, and the step those two earned.
     *
     * Empty rather than nullable, because *Done* still has something to say for an exercise with no
     * plan — the rating is behind the same prompt — and it says "no plan" rather than inventing one.
     */
    val progression: ProgressionPrompt = ProgressionPrompt(),
)

/**
 * The values that change every second (ROADMAP F16).
 *
 * Deliberately **separate** from [ActiveWorkoutUiState]. Folding a one-second
 * ticker into the screen's single state meant every tick produced a new state
 * object, and because that object also holds `List`s — which Compose cannot treat
 * as stable — the exercise list recomposed once a second along with everything
 * else. The clock is read only by the two small composables that display it, so
 * the list is off the per-second path entirely.
 */
data class WorkoutClock(
    val elapsed: String = "",
    val restSecondsRemaining: Int = 0,
) {
    /** One source of truth: "resting" is just "there is time left". */
    val isResting: Boolean get() = restSecondsRemaining > 0
}

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val sessionId: String? = null,
    /**
     * The plan the session was started from, or null for an empty workout (ROADMAP N299).
     *
     * Carried here rather than read from the start route, because a resumed session has no route
     * arguments and the session's own row is the truth. Auto progression resolves the program through
     * this template.
     */
    val sessionTemplateId: String? = null,
    /** Fixed for the life of the session, so it does not belong on the clock. */
    val startedAt: String = "",
    val exercises: List<SessionExerciseRow> = emptyList(),
    /** A just-deleted set awaiting undo; the screen shows it as a snackbar. */
    val pendingUndo: SetEntry? = null,
    /**
     * An exercise just marked done, awaiting undo (ROADMAP N7). The id is enough:
     * the row is still in [exercises], so the snackbar can name it.
     */
    val pendingFinishedExerciseId: String? = null,
    /** What was not recovered today, or null (ROADMAP N4). */
    val readinessNote: String? = null,
    /**
     * The activity this session is, or null for a workout (ROADMAP N141).
     *
     * An activity holds no sets, so the screen stops offering to log any and what is left is the day's own
     * record: the readiness note and the comment asked for at the end.
     */
    val activity: Activity? = null,
    /** The activity's manually stated duration in seconds, or null (N293). */
    val activityDurationSeconds: Int? = null,
    /** The activity's manually stated distance in metres, or null (N293). */
    val activityDistanceMeters: Long? = null,
    /** Set when a write failed, so the screen can say so instead of lying. */
    val error: DataError? = null,
) {
    /**
     * The deleted set the screen may offer to bring back, or null (ROADMAP B3).
     *
     * An undo is only offered while its subject can still **take** it. The two undo
     * snackbars share one host, so without this a deleted set's Undo can outlive its
     * exercise: the row is gone, the button stays, and the write it fires is refused.
     * The rule lives here, named, rather than in the rendering — and the ViewModel
     * still reports if a tap races through before this turns null.
     *
     * **"Can still take it" is the write's own question, not mere presence** (B160). A finished exercise keeps
     * its row in the list (the screen dims it, and the Done undo reads its name from there), but `logSet`
     * refuses it — `countLoggableSessionExercise` requires `finishedAt IS NULL`. So tapping *Done* and then the
     * still-visible Undo answered "That workout is no longer open" in front of a workout that plainly was, and
     * the set stayed lost. The offer now disappears with the exercise's ability to accept it.
     */
    val undoableSet: SetEntry?
        get() = pendingUndo?.takeIf { set -> exercises.any { it.id == set.sessionExerciseId && !it.isFinished } }

    /** The Done undo, offered on the same terms (ROADMAP B3). */
    val undoableFinishedExerciseId: String?
        get() = pendingFinishedExerciseId?.takeIf { id -> exercises.any { it.id == id } }

    /** An open session with nothing in it — the state a user can abandon. */
    val isEmpty: Boolean get() = !isLoading && sessionId != null && exercises.isEmpty()

    /** No open session at all: finished, discarded, or never started. */
    val hasNoSession: Boolean get() = !isLoading && sessionId == null
}

/** What the screen shows once a workout is finished (ROADMAP N20). */
data class WorkoutReview(
    val note: String?,
    val readinessNote: String?,
    val totalSets: Int,
    /** How many of them were holds rather than counted sets (ROADMAP N220, B243). */
    val totalHolds: Int = 0,
    val totalReps: Int,
    val totalVolumeGrams: Long,
    /** Only the exercises that were rated — an unrated one is not a zero. */
    val ratings: List<ExerciseRating>,
    /** The plan next to the performance; empty when the workout came from no plan. */
    val comparisons: List<PlanComparison>,
)

/** How one exercise felt, as the summary reads it back. */
data class ExerciseRating(
    val name: String,
    val muscleFeel: Int?,
    /** The joints that hurt, each with its side and score (ROADMAP N63), or empty. */
    val joints: List<JointPain>,
)
