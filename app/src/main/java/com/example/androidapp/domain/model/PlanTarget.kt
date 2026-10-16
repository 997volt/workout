package com.example.androidapp.domain.model

/**
 * One planned set's target, with the plan it belongs to (ROADMAP N39).
 *
 * The row a plan's prescription is read as: the plan's name for the label, and the four numbers a set can state.
 * [PlannedSetTarget.reps] is the reps **this** set is prescribed for — the range's current rung (N74), or its
 * floor, or its top — because that is what the plan asks for now, and it is the same reading the session's own
 * suggestion makes ([PlannedSetTarget.targetReps]).
 */
data class PlannedSetTarget(
    val templateId: String,
    val templateName: String,
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    val reps: Int?,
    val rpeHalves: Int?,
)

/**
 * What one **plan** prescribes for a lift, reduced from its sets (ROADMAP N39).
 *
 * One per plan rather than one per set: what a chart can draw is a level, and a plan's prescription is "the
 * hardest set it asks for" — so this is the heaviest load with the reps it is prescribed for, the **least**
 * assistance (the least help is the hardest work), and the hardest effort.
 */
data class PlanTarget(
    val templateId: String,
    val templateName: String,
    val weightGrams: Long?,
    val reps: Int?,
    val assistanceGrams: Long?,
    val rpeHalves: Int?,
)

/**
 * One reading per plan, reduced from [sets] (ROADMAP N39).
 *
 * A plan that states nothing anywhere is still returned: the row is "this plan prescribes this lift", and what
 * a *series* can draw of it is [levelFor]'s question, not this one's.
 */
fun planTargets(sets: List<PlannedSetTarget>): List<PlanTarget> =
    sets.groupBy { it.templateId }.map { (templateId, planned) ->
        // The heaviest prescribed load, and the reps *that* set is prescribed for — two numbers from one row,
        // because a 1RM target estimated from one set's weight and another's reps would be a lifter the plan
        // never described.
        val heaviest = planned.filter { it.weightGrams != null }.maxByOrNull { it.weightGrams!! }
        PlanTarget(
            templateId = templateId,
            templateName = planned.first().templateName,
            weightGrams = heaviest?.weightGrams,
            reps = heaviest?.reps,
            assistanceGrams = planned.mapNotNull { it.assistanceGrams }.minOrNull(),
            // The hardest effort asked for, in half-points.
            rpeHalves = planned.mapNotNull { it.rpeHalves }.maxOrNull(),
        )
    }

/**
 * The level this plan's target draws on [metric], or null when the plan states nothing that series can show
 * (ROADMAP N39).
 *
 * Only the **level-shaped** targets have an answer: a weight, an estimated one-rep max from that weight and its
 * rep target, an assistance, an effort. Volume and total reps are sums of a prescription rather than a level in
 * it, and muscle feel and joint pain are not things a plan prescribes at all — inventing a line for any of them
 * would be the "plausible rather than true" this app keeps refusing.
 */
fun PlanTarget.levelFor(metric: ExerciseTrendMetric): Double? = when (metric) {
    ExerciseTrendMetric.HEAVIEST_SET -> weightGrams?.toDouble()
    ExerciseTrendMetric.ESTIMATED_1RM ->
        weightGrams?.let { weight -> reps?.let { estimate -> estimatedOneRepMax(weight, estimate) } }?.toDouble()

    ExerciseTrendMetric.ASSISTANCE -> assistanceGrams?.toDouble()
    // The series carries a *point* value where the plan states half-points, so the line is drawn in the unit
    // the readings are in rather than at half its height (N62's rule, applied to a drawable target).
    ExerciseTrendMetric.RPE -> rpeHalves?.toDouble()?.div(Rpe.HALVES_PER_POINT)
    ExerciseTrendMetric.VOLUME,
    ExerciseTrendMetric.TOTAL_REPS,
    ExerciseTrendMetric.MUSCLE_FEEL,
    ExerciseTrendMetric.JOINT_PAIN,
    -> null
}
