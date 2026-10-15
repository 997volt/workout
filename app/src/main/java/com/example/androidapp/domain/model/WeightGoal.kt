package com.example.androidapp.domain.model

/**
 * What a lifter wants their bodyweight to do (ROADMAP N135).
 *
 * The goal *kind* is the thing N135 adds: before it, a weight goal was one number and the rate was a second
 * one the lifter typed, so the two could disagree. Here the kind is what the lifter states, and the rate is
 * **derived** from the kind and the weight they are now — there is nothing left to disagree with.
 *
 * The three shapes are deliberately not one number with a flag: a target is a weight to reach, a direction
 * is a way to go with **no weight invented for it**, and maintenance is a weight to hold **within a
 * corridor** rather than at a rate of zero, which the first glass of water would break.
 */
sealed interface WeightGoal {
    /** A bodyweight to reach (ROADMAP N39's level target, now with a kind). */
    data class Target(val weightGrams: Long) : WeightGoal

    /** A way to go, with no target weight the lifter stated (ROADMAP N135). */
    data class Direction(val gain: Boolean) : WeightGoal

    /** A bodyweight to hold, within [MAINTENANCE_CORRIDOR_GRAMS] either way (ROADMAP N135). */
    data class Maintain(val weightGrams: Long) : WeightGoal
}

/**
 * The goal kind as it is **stored and exported**: by name, never by ordinal (the app's rule for every enum).
 *
 * Four names rather than three plus a sign, because a direction-only goal's direction is part of what the
 * lifter stated, and a name is what the export carries.
 */
enum class WeightGoalKind {
    TARGET,
    LOSE,
    GAIN,
    MAINTAIN,
}

/** The kind this goal is, for storage and for the export. */
fun WeightGoal.kind(): WeightGoalKind = when (this) {
    is WeightGoal.Target -> WeightGoalKind.TARGET
    is WeightGoal.Direction -> if (gain) WeightGoalKind.GAIN else WeightGoalKind.LOSE
    is WeightGoal.Maintain -> WeightGoalKind.MAINTAIN
}

/** The weight this goal states, or null when it states none (a direction-only goal). */
fun WeightGoal.statedWeightGrams(): Long? = when (this) {
    is WeightGoal.Target -> weightGrams
    is WeightGoal.Maintain -> weightGrams
    is WeightGoal.Direction -> null
}

/**
 * The goal a stored kind and value state, or null when they state nothing.
 *
 * [kind] is a [WeightGoalKind] name, or null for a goal written before kinds existed — which is read as a
 * **target**, because that is exactly what a level goal was. An unknown name is null rather than a guess:
 * a build that does not know a kind cannot honour it, and inventing a target from it would be worse than
 * saying there is no goal.
 */
fun weightGoalOf(kind: String?, valueGrams: Double?): WeightGoal? {
    val named = kind?.let { name -> WeightGoalKind.entries.firstOrNull { it.name == name } }
    return when {
        kind != null && named == null -> null
        named == WeightGoalKind.LOSE -> WeightGoal.Direction(gain = false)
        named == WeightGoalKind.GAIN -> WeightGoal.Direction(gain = true)
        named == WeightGoalKind.TARGET || named == null -> valueGrams?.let { WeightGoal.Target(it.toLong()) }
        named == WeightGoalKind.MAINTAIN -> valueGrams?.let { WeightGoal.Maintain(it.toLong()) }
        else -> null
    }
}

/**
 * The corridor a maintenance goal holds, in stored grams (ROADMAP N135).
 *
 * A kilogram either way: the reference's own outlier rule is a kilogram off the fit, and a corridor is the
 * same kind of statement about water, glycogen and sodium — the daily wobble is not the trend.
 */
const val MAINTENANCE_CORRIDOR_GRAMS = 1_000L

/**
 * The share of bodyweight a week the app will call **neither too fast nor too slow** (ROADMAP N135).
 *
 * The reference judges a fitted slope against 0.025–0.05 kg/day, "around 0.25% weekly", and what transfers
 * is the shape rather than the numbers: a band stated as a **share** of the body it belongs to, so the same
 * rule reads correctly for a 50 kg lifter and a 120 kg one.
 */
private const val BAND_LOW_SHARE = 0.0025
private const val BAND_HIGH_SHARE = 0.005

/** That band, for a body of [weightGrams], in stored grams per week. */
fun weightRateBandPerWeekGrams(weightGrams: Long): ClosedRange<Double> =
    (weightGrams * BAND_LOW_SHARE)..(weightGrams * BAND_HIGH_SHARE)

/**
 * The rate this goal asks for, in stored grams per week, or null when it asks for no rate (ROADMAP N135).
 *
 * **The band decides the rate**, at its centre: the band is the range the app will not call too fast or too
 * slow, so its middle is the one rate that is neither edge — and the value is clamped into the band, so no
 * future change to how the rate is derived can leave it. Rejected: the band's top, "the fastest allowed" —
 * that aims a goal at the edge of too fast, which is a strange thing to ask a lifter to hold.
 *
 * A **target** takes its direction from where the lifter is now, and asks for nothing when the target is
 * already met; a **direction** takes the direction the lifter stated and invents no weight for it; a
 * **maintenance** goal asks for no rate at all, because its answer is a corridor (see [WeightGoal.Maintain]).
 *
 * Null when there is no current weight to scale by: a share of nothing is not a rate.
 */
fun WeightGoal.ratePerWeekGrams(currentWeightGrams: Long): Double? {
    if (currentWeightGrams <= 0L) return null
    val band = weightRateBandPerWeekGrams(currentWeightGrams)
    val centre = ((band.start + band.endInclusive) / 2).coerceIn(band.start, band.endInclusive)
    return when (this) {
        is WeightGoal.Direction -> if (gain) centre else -centre
        is WeightGoal.Target -> when {
            weightGrams > currentWeightGrams -> centre
            weightGrams < currentWeightGrams -> -centre
            else -> 0.0
        }

        is WeightGoal.Maintain -> null
    }
}
