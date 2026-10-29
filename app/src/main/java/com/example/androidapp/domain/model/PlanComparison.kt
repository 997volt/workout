package com.example.androidapp.domain.model

import com.example.androidapp.domain.Load
import com.example.androidapp.domain.WeightUnit
import kotlin.math.roundToInt

/**
 * A plan and what actually happened, side by side (ROADMAP N20).
 *
 * The app has written plans since N14 and logged performed sets since v1, and has never
 * compared them: targets only mattered *during* a workout, so the payoff for planning — the
 * review — was missing. This is that comparison, as values rather than sentences, because
 * the wording belongs to the screen and the arithmetic belongs here.
 *
 * **Warm-up sets are excluded from both sides**, for the reason N17 excluded them from a
 * load series: a warm-up is not the work the plan prescribes, and letting one stand in for
 * the top set would flatter every comparison.
 */

/** One planned set, reduced to what a comparison needs. */
data class PlannedSetSpec(
    val role: SetType,
    val weightGrams: Long? = null,
    val assistanceGrams: Long? = null,
    val minReps: Int? = null,
    val maxReps: Int? = null,
)

/** One performed set, reduced the same way. */
data class PerformedSetSpec(
    val role: SetType,
    val weightGrams: Long,
    val assistanceGrams: Long = 0,
    val reps: Int,
    /**
     * What the set felt like, in half-points, or null when it was not recorded (ROADMAP N131).
     *
     * Carried per set because that is how the log holds it (N6), even though the plan names one
     * number per exercise (N59). Reducing the sets to that one number is [PlanComparison]'s, and it
     * is the whole of what this comparison had to decide.
     */
    val rpeHalves: Int? = null,
    /** How long the set was held, or null for a counted set (ROADMAP N220, B243). */
    val durationSeconds: Int? = null,
)

/**
 * An exercise as the plan prescribed it.
 *
 * [exerciseId] is what matches the two sides (ROADMAP B20). Keying by [name] collapsed two rows
 * of the same movement — nothing prevents the same exercise appearing twice in a session — and
 * silently dropped the earlier one's sets from the review while the totals still counted them.
 */
data class ExercisePlan(
    val exerciseId: String,
    val name: String,
    val sets: List<PlannedSetSpec>,
    /** The unit this exercise's numbers read in, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    /**
     * The effort this exercise's plan builds to, in half-points, or null (ROADMAP N59, N131).
     *
     * **One number for the exercise, not one per set** — it is carried onto every prescribed set by
     * the caller (N59) — which is exactly why this comparison needed a reduction the weight and rep
     * ones did not.
     */
    val targetRpeHalves: Int? = null,
)

/** An exercise as it was performed. */
data class ExerciseActual(
    val exerciseId: String,
    val name: String,
    val sets: List<PerformedSetSpec>,
    /** The same as [ExercisePlan.weightUnit], for an exercise the plan never named (N64). */
    val weightUnit: WeightUnit? = null,
    /**
     * The directions of a step this exercise was offered and did not take (ROADMAP N236).
     *
     * Carried through the comparison because the review is the one place the whole session is read back, and
     * "there was a step here and you passed" is the fact the log cannot otherwise state.
     */
    val declinedDirections: Set<ProgressionDirection> = emptySet(),
)

/**
 * One exercise's plan next to its performance.
 *
 * Every nullable field is honestly nullable: a plan may prescribe reps without a weight, a
 * session may log a lift the plan never mentioned, and [topSetDeltaGrams] is null when
 * there is nothing to subtract — rather than a zero that would read as "matched".
 *
 * **The effort comparison is the one that needed a reduction** (N131). The plan names one target
 * per exercise (N59) while the log holds one rating per set (N6), so [performedAverageRpeHalves] is
 * the working sets' mean rounded to the app's own RPE resolution — **half a point**, the finest
 * difference a lifter can log, and the same reason `estimatedOneRepMax` rounds to the nearest
 * 500 g: printing `8.3` would dress an average as a rating nobody gave.
 */
data class PlanComparison(
    val name: String,
    /** The unit this exercise's numbers read in, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: WeightUnit? = null,
    val prescribedSets: Int,
    val performedSets: Int,
    val prescribedReps: Int?,
    val performedReps: Int,
    val prescribedTopWeightGrams: Long?,
    val performedTopWeightGrams: Long?,
    /**
     * The help the heaviest prescribed working set was given, or null (B153, N144).
     *
     * A plan's load is **one** number: `-20` is 20 kg of help and no added weight, and it is stored as
     * `weightGrams = 0` with `assistanceGrams = 20000`. Reading only the weight half therefore reported an
     * assisted plan as "at 0 kg" — a weight the plan never prescribed — while the assistance the plan actually
     * named appeared nowhere. Carried beside the weight so the dialog can state the load the lifter reads.
     */
    val prescribedTopAssistanceGrams: Long? = null,
    /** The same for what was performed, so an assisted set's line can name its help too. */
    val performedTopAssistanceGrams: Long? = null,
    /** Performed minus prescribed on the heaviest working set, or null if either is unknown. */
    val topSetDeltaGrams: Long?,
    /**
     * The last hold the exercise performed, in seconds, or null when it was counted (ROADMAP N220, B243).
     *
     * A hold states a duration instead of reps, so the two lines that read reps — the performed shape and
     * the plan's rep target — say nothing true about it. This is the fact that lets them say the hold
     * instead, and it is what tells the dialog the plan's rep target is ignored here rather than missed.
     */
    val performedHoldSeconds: Int? = null,
    /** The plan's one effort for this exercise, in half-points, or null (ROADMAP N59, N131). */
    val prescribedRpeHalves: Int? = null,
    /**
     * The working sets' mean effort, in half-points, or null when none recorded one (N131).
     *
     * Null rather than zero, and null rather than the target: a session that rated no working set
     * says nothing about the effort, and a zero would read as "as easy as it gets".
     */
    val performedAverageRpeHalves: Int? = null,
    /** Performed minus prescribed on effort, or null when either side is unknown (N131). */
    val rpeDeltaHalves: Int? = null,
    /** The directions of a step that was offered and not taken, or empty (ROADMAP N236). */
    val declinedDirections: Set<ProgressionDirection> = emptySet(),
) {
    /** True when the plan had nothing to compare — an exercise added mid-workout. */
    val wasPlanned: Boolean get() = prescribedSets > 0

    /** True when the top set landed exactly where the plan put it. */
    val matchedPlan: Boolean get() = topSetDeltaGrams == 0L

    /** True when the effort landed exactly where the plan put it (ROADMAP N131). */
    val matchedEffort: Boolean get() = rpeDeltaHalves == 0
}

/**
 * Compares a plan to a performance, exercise by exercise, in the plan's order.
 *
 * Exercises that were performed but never planned come last, so the first thing read is the
 * plan being answered rather than the improvisation.
 */
fun comparePlanToActual(
    planned: List<ExercisePlan>,
    performed: List<ExerciseActual>,
): List<PlanComparison> {
    // **Paired by occurrence, not by key** (B274). A session may hold the same movement twice — nothing
    // stops it — and a map keyed by exercise id keeps only the last row, dropping the other's sets from the
    // review while the totals still count them. That is the loss B20 fixed for the *name* key; the id key
    // had the same hole. One planned row consumes one performed row, in the order they were done.
    val remaining = performed.indices
        .groupBy { performed[it].exerciseId }
        .mapValues { (_, indices) -> indices.toMutableList() }
    val consumed = BooleanArray(performed.size)

    fun take(exerciseId: String): ExerciseActual? {
        val index = remaining[exerciseId]?.removeFirstOrNull() ?: return null
        consumed[index] = true
        return performed[index]
    }

    val answered = planned.map { plan ->
        // The taken row carries both halves of what the plan is compared with: what was performed, and
        // whether a step was offered and passed (N236).
        val actual = take(plan.exerciseId)
        comparisonOf(
            name = plan.name,
            plannedSets = plan.sets,
            performed = actual,
            weightUnit = plan.weightUnit,
            targetRpeHalves = plan.targetRpeHalves,
        )
    }
    val improvised = performed
        .filterIndexed { index, _ -> !consumed[index] }
        .map {
            comparisonOf(
                name = it.name,
                plannedSets = emptyList(),
                performed = it,
                weightUnit = it.weightUnit,
                // The plan never named this lift, so it named no effort for it either (N131).
                targetRpeHalves = null,
            )
        }

    return answered + improvised
}

private fun comparisonOf(
    name: String,
    plannedSets: List<PlannedSetSpec>,
    /** The taken row, or null when the plan named an exercise nobody performed. */
    performed: ExerciseActual?,
    weightUnit: WeightUnit?,
    targetRpeHalves: Int?,
): PlanComparison {
    val working = { role: SetType -> role != SetType.WARMUP }
    val plannedWork = plannedSets.filter { working(it.role) }
    val performedWork = performed?.sets.orEmpty().filter { working(it.role) }

    // The heaviest working set on each side: a light back-off set must not stand in for the
    // top set any more than a warm-up may. The *load* decides, not the weight column (B274): an assisted
    // set stores no added weight, so two assisted top sets at different help both read zero and the verdict
    // was "Matched" whatever the machine took.
    val prescribedTopLoad = plannedWork.mapNotNull { signedLoad(it.weightGrams, it.assistanceGrams) }.maxOrNull()
    val performedTopLoad = performedWork.mapNotNull { signedLoad(it.weightGrams, it.assistanceGrams) }.maxOrNull()
    val prescribedTop = plannedWork.mapNotNull { it.weightGrams }.maxOrNull()
    val performedTop = performedWork.maxOfOrNull { it.weightGrams }

    // The help that goes with each top set (B153, N144). Read from the plan's own split rather than derived:
    // `assistanceGrams` is what the editor wrote, and a load is one number carried in two columns.
    val prescribedTopAssistance = plannedWork
        .filter { it.weightGrams == prescribedTop }
        .mapNotNull { it.assistanceGrams }
        .maxOrNull()
    val performedTopAssistance = performedWork
        .filter { it.weightGrams == performedTop }
        .maxOfOrNull { it.assistanceGrams }
        .takeIf { it != null && it > 0L }

    // B19: the guard belonged to the mapped list. Testing the *input* meant a plan whose working
    // sets name no reps produced an empty sum — a truthy guard and a zero on screen, against the
    // file's own promise that every nullable field is honestly nullable.
    val plannedReps = plannedWork.mapNotNull { it.maxReps }

    return PlanComparison(
        name = name,
        weightUnit = weightUnit,
        // B21: the counts exclude warm-ups, exactly as the rep sums do — otherwise "prescribed
        // 2×3" meant two sets, one of them a warm-up, totalling three reps. The dialog says so.
        prescribedSets = plannedWork.size,
        performedSets = performedWork.size,
        prescribedReps = plannedReps.sum().takeIf { plannedReps.isNotEmpty() },
        performedReps = performedWork.sumOf { it.reps },
        prescribedTopWeightGrams = prescribedTop,
        performedTopWeightGrams = performedTop,
        prescribedTopAssistanceGrams = prescribedTopAssistance?.takeIf { it > 0L },
        performedTopAssistanceGrams = performedTopAssistance,
        topSetDeltaGrams = if (prescribedTopLoad == null || performedTopLoad == null) {
            null
        } else {
            performedTopLoad - prescribedTopLoad
        },
        prescribedRpeHalves = targetRpeHalves,
        performedHoldSeconds = performedWork.mapNotNull { it.durationSeconds }.lastOrNull(),
        performedAverageRpeHalves = performedWork.averageRpeHalves(),
        // Null unless both sides state one, for the reason the weight delta is: a difference from
        // nothing is not a comparison.
        rpeDeltaHalves = performedWork.averageRpeHalves()?.let { performed ->
            targetRpeHalves?.let { performed - it }
        },
        // A step offered and not taken, carried rather than computed (N236).
        declinedDirections = performed?.declinedDirections.orEmpty(),
    )
}

/**
 * The mean effort of these sets, in half-points, rounded to the app's own resolution (N131).
 *
 * **The reduction the plan's one-number-per-exercise target forces.** Unlike weight or reps there is
 * no single set the plan's effort belongs to: N59's target is carried onto every prescribed set, so
 * the question is whether the work landed where the plan put it, and the mean over the sets that
 * recorded one is that answer. The heaviest set's rating was rejected — it answers what the top set
 * felt like, which is a different question, and a session with one on-target top set and three
 * off-target back-offs would read as matched.
 *
 * **Rounded to half a point**, because that is the finest difference an RPE records here: a mean of
 * `8.3` is not a rating anybody gave, and printing it would dress the arithmetic as one.
 *
 * Sets that record no effort are skipped rather than counted as zero (N6's "skippable"), and a
 * warm-up is skipped whatever a stale row says, because it is not the work the plan prescribes.
 */
private fun List<PerformedSetSpec>.averageRpeHalves(): Int? {
    val rated = filter { it.role.recordsEffort }.mapNotNull { it.rpeHalves }
    if (rated.isEmpty()) return null
    return rated.average().roundToInt()
}

/**
 * A set's load as one signed number, or null when it states no weight at all (B274).
 *
 * The app's rule, not a second one: [Load] is what the steppers and the rows read. An assisted set is
 * negative — `-20` is twenty kilos of help — which is what lets two assisted top sets be compared.
 */
private fun signedLoad(weightGrams: Long?, assistanceGrams: Long?): Long? =
    weightGrams?.let { Load(it, assistanceGrams ?: 0L).signedGrams }
