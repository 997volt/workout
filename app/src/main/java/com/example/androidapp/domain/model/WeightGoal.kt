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
 * The shares of bodyweight a week each goal is held to (ROADMAP N135, amended by N149).
 *
 * The gain band is the reference's shape — a share of the body rather than a fixed number of kilograms — and
 * the loss band is the faster rule the lifter asked for: gaining is deliberately the slower direction. Each
 * band is the range the app will not call too fast or too slow, and each stated target sits inside its own
 * band (N149 amends N135's single band).
 */
private const val GAIN_LOW_SHARE = 0.0025
private const val GAIN_HIGH_SHARE = 0.005
private const val GAIN_TARGET_SHARE = 0.0035
private const val LOSS_LOW_SHARE = 0.005
private const val LOSS_HIGH_SHARE = 0.008
private const val LOSS_TARGET_SHARE = 0.0065

/** The band for a direction, for a body of [weightGrams], in stored grams per week. */
fun weightRateBandPerWeekGrams(weightGrams: Long, gain: Boolean): ClosedRange<Double> =
    if (gain) {
        (weightGrams * GAIN_LOW_SHARE)..(weightGrams * GAIN_HIGH_SHARE)
    } else {
        (weightGrams * LOSS_LOW_SHARE)..(weightGrams * LOSS_HIGH_SHARE)
    }

/**
 * The direction this goal is heading, or null when it is not a rate goal at all (N149).
 *
 * A target takes its direction from where the lifter is now; a maintenance goal has a corridor instead, and
 * a target already met has no direction to be on target for.
 */
private fun WeightGoal.directionFrom(currentWeightGrams: Long): Boolean? = when (this) {
    is WeightGoal.Direction -> gain
    is WeightGoal.Target -> when {
        weightGrams > currentWeightGrams -> true
        weightGrams < currentWeightGrams -> false
        else -> null
    }

    is WeightGoal.Maintain -> null
}

/**
 * The band this goal's trend is judged against, signed for the direction, or null (N149).
 *
 * A loss band is negative at both ends — it is read as "between −0.64 and −0.4 kg a week" — while a gain band
 * keeps the positive sign. The magnitude is always [weightRateBandPerWeekGrams]'s.
 */
fun WeightGoal.rateBandPerWeekGrams(currentWeightGrams: Long): ClosedRange<Double>? {
    val gain = if (currentWeightGrams > 0L) directionFrom(currentWeightGrams) else null
    return gain?.let {
        val band = weightRateBandPerWeekGrams(currentWeightGrams, it)
        if (it) band else -(band.endInclusive)..-(band.start)
    }
}

/**
 * The rate this goal asks for, in stored grams per week, or null when it asks for no rate (ROADMAP N135, N149).
 *
 * **Each direction has its own band and its own stated target.** A **gain** is held to 0.25–0.5% of the body a
 * week and asks for **0.35%**; a **loss** is held to 0.5–0.8% and asks for **0.65%**. The target is clamped into
 * its band, so a future change to how it is derived cannot leave the range that says neither too fast nor too
 * slow. Loss is deliberately the faster rule: gaining lean mass is the slower direction.
 *
 * A **target** takes its direction from where the lifter is now, and asks for nothing when the target is already
 * met; a **direction** takes the direction the lifter stated and invents no weight for it; a **maintenance** goal
 * asks for no rate at all, because its answer is a corridor (see [WeightGoal.Maintain]).
 *
 * Null when there is no current weight to scale by: a share of nothing is not a rate.
 */
fun WeightGoal.ratePerWeekGrams(currentWeightGrams: Long): Double? {
    if (currentWeightGrams <= 0L) return null
    return when (this) {
        is WeightGoal.Target -> when {
            weightGrams > currentWeightGrams -> rateOn(currentWeightGrams, gain = true)
            weightGrams < currentWeightGrams -> rateOn(currentWeightGrams, gain = false)
            else -> 0.0
        }

        is WeightGoal.Direction -> rateOn(currentWeightGrams, gain)
        is WeightGoal.Maintain -> null
    }
}

/** The stated target for one direction, in stored grams a week, clamped into that direction's band. */
private fun rateOn(currentWeightGrams: Long, gain: Boolean): Double {
    val band = weightRateBandPerWeekGrams(currentWeightGrams, gain)
    val share = if (gain) GAIN_TARGET_SHARE else LOSS_TARGET_SHARE
    val magnitude = (currentWeightGrams * share).coerceIn(band.start, band.endInclusive)
    return if (gain) magnitude else -magnitude
}
