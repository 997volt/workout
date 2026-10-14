package com.example.androidapp.domain.model

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
    /** Performed minus prescribed on the heaviest working set, or null if either is unknown. */
    val topSetDeltaGrams: Long?,
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
    val actualById = performed.associateBy { it.exerciseId }
    val plannedIds = planned.map { it.exerciseId }.toSet()

    val answered = planned.map { plan ->
        comparisonOf(
            name = plan.name,
            plannedSets = plan.sets,
            performedSets = actualById[plan.exerciseId]?.sets.orEmpty(),
            weightUnit = plan.weightUnit,
            targetRpeHalves = plan.targetRpeHalves,
        )
    }
    val improvised = performed
        .filterNot { it.exerciseId in plannedIds }
        .map {
            comparisonOf(
                name = it.name,
                plannedSets = emptyList(),
                performedSets = it.sets,
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
    performedSets: List<PerformedSetSpec>,
    weightUnit: WeightUnit?,
    targetRpeHalves: Int?,
): PlanComparison {
    val working = { role: SetType -> role != SetType.WARMUP }
    val plannedWork = plannedSets.filter { working(it.role) }
    val performedWork = performedSets.filter { working(it.role) }

    // The heaviest working set on each side: a light back-off set must not stand in for the
    // top set any more than a warm-up may.
    val prescribedTop = plannedWork.mapNotNull { it.weightGrams }.maxOrNull()
    val performedTop = performedWork.maxOfOrNull { it.weightGrams }

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
        topSetDeltaGrams = if (prescribedTop == null || performedTop == null) {
            null
        } else {
            performedTop - prescribedTop
        },
        prescribedRpeHalves = targetRpeHalves,
        performedAverageRpeHalves = performedWork.averageRpeHalves(),
        // Null unless both sides state one, for the reason the weight delta is: a difference from
        // nothing is not a comparison.
        rpeDeltaHalves = performedWork.averageRpeHalves()?.let { performed ->
            targetRpeHalves?.let { performed - it }
        },
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

