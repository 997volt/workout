package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.progressionPromptFor
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.domain.model.runContinuesAfter
import com.example.androidapp.domain.model.taxonomySubtitle
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.ui.components.setNumbers
import java.time.ZoneId

// The session's flows translated into screen state (ROADMAP N285): the snapshot the ViewModel combines, and
// the pure mapping that turns it into rows, set numbers and superset labels. Read-only, so it can be tested
// without a database or a ViewModel.

/**
 * Everything that describes the workout's contents.
 *
 * File-level with [toUiState] rather than nested in the ViewModel: the two are one translation of the
 * session's flows into screen state, and the class sits at the function ceiling detekt enforces.
 */
internal data class Snapshot(
    val session: WorkoutSession?,
    val exercises: List<SessionExercise>,
    val sets: List<SetEntry>,
    val previous: Map<String, PreviousPerformance>,
    val planned: List<TemplateExercise>,
    /** The app-wide unit, so a change to it re-resolves every row's own (ROADMAP N64). */
    val unit: WeightUnit,
)

/**
 * The screen's state, translated from the snapshot the session's flows combine into.
 *
 * A file-level extension rather than a member: the class sits at the function ceiling detekt
 * enforces, and this only translates the state it is given — it reads nothing from the ViewModel and
 * writes nothing back, so it does not belong to it.
 */
internal fun Snapshot.toUiState(
    error: DataError?,
    undo: SetEntry?,
    pendingFinishedExerciseId: String?,
): ActiveWorkoutUiState {
    // Which plan entry each row follows is resolved once, by movement (ROADMAP N54), rather than per
    // row from the slot it happens to occupy.
    val planEntries = planEntriesFor(exercises, planned)
    return ActiveWorkoutUiState(
        isLoading = false,
        sessionId = session?.id,
        sessionTemplateId = session?.templateId,
        startedAt = session?.let {
            WorkoutFormat.clockTime(it.startedAt, zone = it.zoneIdOrNull() ?: ZoneId.systemDefault())
        }.orEmpty(),
        exercises = exercises.map {
            it.toRow(
                sets = sets,
                previous = previous[it.exerciseId],
                // The plan is the template's, whole (ROADMAP N73), read in this exercise's own unit
                // where it has one (N64).
                plan = PlanContext(
                    plannedEntry = planEntries[it.id],
                    unit = it.weightUnit ?: unit,
                ),
                supersetLabels = supersetLabelsFor(exercises),
            )
        },
        pendingUndo = undo,
        pendingFinishedExerciseId = pendingFinishedExerciseId,
        readinessNote = session?.readinessNote,
        activity = session?.activity,
        activityDurationSeconds = session?.activityDurationSeconds,
        activityDistanceMeters = session?.activityDistanceMeters,
        error = error,
    )
}

/**
 * True while this exercise is still part of the session (ROADMAP N7's undo).
 *
 * A file-level extension rather than a member: the class sits at the function ceiling
 * detekt enforces, and this is a question about the state, not about the ViewModel.
 */
internal fun ActiveWorkoutUiState.hasLiveExercise(sessionExerciseId: String): Boolean =
    exercises.any { it.id == sessionExerciseId }

/**
 * True when the round this exercise is part of has nothing left behind it (ROADMAP N24).
 *
 * A grouped exercise only rests once every other member of its group has logged at least as
 * many sets, or has been marked done — a member that is finished is not going to catch up, and
 * waiting for it would leave the user resting between the two exercises the grouping exists to
 * pair. An ungrouped exercise is always its own complete round, which is the old behaviour.
 *
 * **A member whose plan is used up has nothing left to catch up with either** (B154). The two members of a
 * superset need not prescribe the same number of sets, and once the shorter one logs its last set it can
 * never reach the longer one's count: the condition below stayed false for the rest of the session, so the
 * rest timer silently stopped being armed for the heavier member's remaining sets — no countdown, no ±15 s,
 * no end-of-rest cue — and nothing on screen said a round was holding it back. The plan is what says the
 * member is done; a manual *Done* was the only way to reach the exception the old expression already had,
 * which is why the unequal-totals case went unnoticed.
 */
internal fun ActiveWorkoutUiState.roundIsCompleteFor(row: SessionExerciseRow): Boolean {
    // `row` was captured before the write, and this state may not have caught up with the write
    // either — so the count comes from what this action actually logged rather than from a read
    // that can be one short. Both ways of trusting the read were live bugs: the round looked
    // finished, so the app rested between the two exercises the user had just paired.
    val logged = row.sets.size + 1

    return row.supersetGroup == null || exercises
        .filter { it.supersetGroup == row.supersetGroup && it.id != row.id }
        .all { it.isFinished || it.sets.size >= logged || it.hasNoPlanLeft() }
}

/**
 * True when this member has already logged everything its plan writes (B154).
 *
 * `plannedSetCount` is null when there is no plan or the plan is nothing but warm-ups (N112), and both mean
 * the same thing here: nothing the plan still asks for, so there is nothing to wait for. The count is of
 * **work** sets, which is the only side of the comparison with a definition to read.
 */
private fun SessionExerciseRow.hasNoPlanLeft(): Boolean =
    plannedSetCount?.let { sets.count { set -> set.setType.isWork } >= it } == true

/**
 * What the plan says for one exercise: the template's entry resolved to it (ROADMAP N14, N73).
 *
 * One field since N73. A slot's prescription used to sit beside it, merging set by set for the
 * prefill, the plan count and the progression rule; the program is a schedule over the template now,
 * so there is nothing to merge.
 */
internal data class PlanContext(
    val plannedEntry: TemplateExercise?,
    /** The unit this exercise's plan is read in: its own override, or the app setting (N64). */
    val unit: WeightUnit = WeightUnit.KILOGRAMS,
)

/**
 * How many sets the plan behind this workout writes for this exercise, or null (ROADMAP N52, N112).
 *
 * No plan at all is null rather than zero, so an empty workout's control never says the work is
 * finished before it has started — and so is a plan of **nothing but warm-ups**: a ramp is preparation,
 * so an exercise whose plan never asked for a working set has nothing to be done with either (N112).
 */
private val PlanContext.plannedSetCount: Int?
    get() = plannedEntry?.sets?.count { it.role.isWork }?.takeIf { it > 0 }

/**
 * The plan entry each session exercise follows, keyed by the session exercise's own id (ROADMAP N54).
 *
 * Matched by **movement and occurrence** rather than by position. A session's order is its own once
 * N54 lets it be edited, so the slot a row occupies says nothing about which plan entry it came from,
 * and matching on position is what pointed a moved exercise at its neighbour's targets. The movement
 * is the stable half; the occurrence count keeps a plan that names the same movement twice working,
 * with the first row of that movement following the plan's first entry and the second its second. A
 * row the plan has nothing for — added by hand, or the extra row of a movement the plan names once —
 * is simply absent, which is what leaves an improvised workout with no plan at all.
 *
 * **A substituted row consumes its origin's occurrence and gets no plan** (B395). Its numbers belong
 * to the replacement movement, so pairing it with a plan would prefill and progress a prescription
 * nobody performed; counting it under its current movement would let it steal a later entry of that
 * movement and leave the real row with nothing. The origin is `substitutedFromExerciseId`, so the
 * stand-in holds the place it replaced and the real row keeps its own entry.
 *
 * File-level and pure, so the pairing can be tested without a database or a ViewModel: the shape
 * `todaysPlanFor` already uses.
 */
internal fun planEntriesFor(
    exercises: List<SessionExercise>,
    planned: List<TemplateExercise>,
): Map<String, TemplateExercise> {
    if (planned.isEmpty()) return emptyMap()
    val byMovement = planned.groupBy { it.exerciseId }
    val taken = mutableMapOf<String, Int>()
    return buildMap {
        exercises.forEach { exercise ->
            val origin = exercise.substitutedFromExerciseId ?: exercise.exerciseId
            val occurrence = taken.getOrDefault(origin, 0)
            taken[origin] = occurrence + 1
            // A stand-in is not the movement the plan prescribed, so it earns no entry of its own.
            if (exercise.substitutedFromExerciseId != null) return@forEach
            byMovement[origin]?.getOrNull(occurrence)?.let { put(exercise.id, it) }
        }
    }
}

private fun SessionExercise.toRow(
    sets: List<SetEntry>,
    previous: PreviousPerformance?,
    plan: PlanContext,
    supersetLabels: Map<String, String>,
): SessionExerciseRow {
    // Every set this exercise logged reads in the exercise's own unit (ROADMAP N64), and its ±
    // buttons move by the step the movement loads in (N77).
    val loggedSets = sets.loggedRowsFor(id, stepGrams)
        .map { it.copy(weightUnit = plan.unit) }

    return SessionExerciseRow(
        id = id,
        exerciseId = exerciseId,
        substitutedFromExerciseId = substitutedFromExerciseId,
        name = exerciseName,
        subtitle = taxonomySubtitle(primaryMuscle, equipment),
        techniqueNote = techniqueNote,
        supersetGroup = supersetGroup,
        supersetLabel = supersetLabels[id],
        restSeconds = restSeconds,
        weightUnit = plan.unit,
        stepGrams = stepGrams,
        isTimed = isTimed,
        isBodyweight = isBodyweight,
        progressionMode = progressionMode,
        declinedDirections = declinedDirections,
        runContinuesAfter = plan.plannedEntry?.sets?.runContinuesAfter().orEmpty(),
        plannedSetCount = plan.plannedSetCount,
        isFinished = isFinished,
        muscleFeel = muscleFeel,
        joints = joints,
        sets = loggedSets,
        suggestion = suggestionFor(loggedSets, previous, plan),
        // The step this exercise earned, read from the same plan the prefill reads (N50).
        progression = progressionPromptFor(
            planned = plan.progressionSets(),
            performed = loggedSets.map { it.toProgressionPerformance() },
            stepGrams = Weight.stepGramsFor(stepGrams, plan.unit),
        ),
    )
}

/** The sets logged against one session exercise, displayed 1-based and renumbered (N54's neighbours). */
private fun List<SetEntry>.loggedRowsFor(
    sessionExerciseId: String,
    /** The movement's own step, carried onto every row so its editor steps the same way (N77). */
    stepGrams: Long?,
): List<SetRow> {
    val mine = filter { it.sessionExerciseId == sessionExerciseId }.sortedBy { it.setIndex }
    // Numbered by `setNumbers`, so a warm-up takes none and the working sets read 1, 2, 3 (N112) —
    // and renumbered on every read, so deleting the first leaves the rest reading 1, 2, 3.
    val numbers = mine.setNumbers { it.setType }
    return mine.mapIndexed { index, set ->
        SetRow(
            id = set.id,
            number = numbers[index],
            reps = set.reps,
            weightGrams = set.weightGrams,
            rpeHalves = set.rpeHalves,
            note = set.note,
            setType = set.setType,
            assistanceGrams = set.assistanceGrams,
            bodyweightGrams = set.bodyweightGrams,
            durationSeconds = set.durationSeconds,
            stepGrams = stepGrams,
        )
    }
}

/**
 * `A1`, `A2` … for a grouped exercise, or null (ROADMAP N24).
 *
 * Giant-set notation, and the group letter follows the order the groups appear in the workout
 * so the first pair the user makes is always A — a label that changed as exercises moved would
 * be worse than no label.
 */
private fun supersetLabelsFor(exercises: List<SessionExercise>): Map<String, String> {
    val groups = exercises.mapNotNull { it.supersetGroup }.distinct().sorted()
    return exercises.mapNotNull { exercise ->
        val group = exercise.supersetGroup ?: return@mapNotNull null
        val letter = 'A' + groups.indexOf(group)
        if (letter !in 'A'..'Z') return@mapNotNull null
        // The member's place in its own group, in the order the workout shows them.
        val member = exercises.filter { it.supersetGroup == group }.indexOfFirst { it.id == exercise.id } + 1
        exercise.id to "$letter$member"
    }.toMap()
}

/**
 * The longest rest any member of this exercise's group prescribes, or null (ROADMAP B15).
 *
 * Null for an ungrouped exercise, and null for a group where nobody prescribes one, so the
 * caller falls back to the exercise's own rest and then to the app-wide setting.
 */
internal fun ActiveWorkoutUiState.longestRestInRound(row: SessionExerciseRow): Int? {
    val group = row.supersetGroup ?: return null
    return exercises
        .filter { it.supersetGroup == group }
        .mapNotNull { it.restSeconds }
        .maxOrNull()
}
