package com.example.androidapp.ui.statistics

import java.time.Duration
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import com.example.androidapp.domain.model.MAINTENANCE_CORRIDOR_GRAMS
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.ratePerWeekGrams
import kotlin.math.roundToInt

/**
 * What a weight trend implies about eating (ROADMAP N98).
 *
 * **A relative adjustment, never a number to eat.** The app holds the weight series and a target *rate*; it
 * does not hold — and deliberately never stores — what anybody ate. So this says how far current intake has to
 * move for the trend to become the target, "about 200 kcal a day less than you are eating now", and the
 * absolute target stays where the food is logged. An absolute number needs intake, and intake's absence here
 * is a boundary rather than a gap to close with a formula.
 *
 * [lowKcal] and [highKcal] are signed: negative is *eat less*, positive is *eat more*.
 */
data class EnergyAdjustment(val lowKcal: Int, val highKcal: Int) {

    /** The trend is already inside the target, so nothing needs to change. */
    val isOnTarget: Boolean get() = lowKcal <= 0 && highKcal >= 0

    /** The adjustment is to eat *more*, which is the sign the sentence turns on. */
    val eatsMore: Boolean get() = lowKcal > 0

    /** The band as an ordered magnitude, for the sentence — a range, never a point estimate. */
    val magnitude: IntRange
        get() = minOf(abs(lowKcal), abs(highKcal))..maxOf(abs(lowKcal), abs(highKcal))
}

/**
 * The floor below which a rate is noise wearing a trend's clothes (ROADMAP N98).
 *
 * Eight readings across three weeks is the starting rule the entry names, stated as one constant each so it
 * can be moved when real data says it should be — a floor, not a target.
 */
const val ENERGY_ADJUSTMENT_MIN_READINGS = 8
const val ENERGY_ADJUSTMENT_MIN_DAYS = 21L

/** A kilogram of body mass, conventionally — the approximation the band exists to admit (ROADMAP N98). */
private const val KCAL_PER_KILOGRAM = 7700.0
private const val GRAMS_PER_KILOGRAM = 1000.0
private const val DAYS_PER_WEEK = 7.0

/**
 * The adjustment the series implies for [targetPerWeekGrams], or null when there is too little to say.
 *
 * Null is a real answer and the screen says so rather than drawing a number: a rate from three weigh-ins is
 * noise, and a figure computed from it would be the *plausible rather than true* that P2.8 is parked on.
 *
 * Both directions are in the series' stored grams, so the sign of the target is the direction that was asked
 * for — negative to lose — and the sign of the answer follows from the same arithmetic.
 */
fun MetricSeries.energyAdjustment(targetPerWeekGrams: Double): EnergyAdjustment? =
    // Null is the answer when there is too little to say, and when the fitted line would be a lie.
    trend()?.takeIf { hasEnoughToSpeak() }?.let { fitted -> adjustmentFor(fitted, targetPerWeekGrams) }

/**
 * Whether this series has the recorded readings and the span N98's floor asks for (ROADMAP N98, N136).
 *
 * **The guard is about *recorded* readings**, so an entry with no weight in it does not count towards having
 * enough to say — and the span is between the readings that exist rather than between the first and last rows,
 * which a tape measurement alone would stretch. It is a function rather than a line inside
 * [energyAdjustment] because the unsolicited prompt N136 adds has to ask the same question, and two copies of
 * "enough" would be two answers.
 */
internal fun MetricSeries.hasEnoughToSpeak(): Boolean {
    val recorded = readings
        .mapNotNull { reading -> reading.value?.let { reading.at } }
        .sorted()
    val span = recorded
        .takeIf { it.isNotEmpty() }
        ?.let { Duration.between(it.first(), it.last()) }
        ?: Duration.ZERO
    return recorded.size >= ENERGY_ADJUSTMENT_MIN_READINGS &&
        span >= Duration.ofDays(ENERGY_ADJUSTMENT_MIN_DAYS)
}

/**
 * What a weigh-in says about the goal's rate, or null when there is nothing honest to say (ROADMAP N136).
 *
 * The same arithmetic the statistics screen states under its chart, asked at the moment the weight is recorded
 * — which is where a lifter finds out *this week* whether the trend is the rate they asked for.
 *
 * **The floor is checked here**, rather than left to the arithmetic, because this line is **unsolicited**: it
 * appears because a weight was saved, and a prompt that fires on water weight is worse than no prompt. The
 * same answer is null for **no goal**, which is the one prompt this app must never grow — the direction is the
 * lifter's, and a line telling somebody with no such goal to lose weight is a diagnosis rather than arithmetic
 * against what they asked for.
 *
 * A **maintenance** goal is judged against its corridor, exactly as the chart's section judges it (N135).
 */
fun MetricSeries.weighInVerdict(goal: WeightGoal?, currentWeightGrams: Long?): EnergyAdjustment? {
    if (goal == null || !hasEnoughToSpeak()) return null
    return if (goal is WeightGoal.Maintain) {
        energyAdjustmentToCorridor(goal.weightGrams.toDouble())
    } else {
        currentWeightGrams
            ?.takeIf { it > 0L }
            ?.let { weight -> goal.ratePerWeekGrams(weight) }
            ?.let { rate -> energyAdjustment(rate) }
    }
}

/** The arithmetic, once the series is known to be long enough and fittable. */
private fun adjustmentFor(fitted: TrendSlope, targetPerWeekGrams: Double): EnergyAdjustment {
    val kcalPerGramPerWeek = KCAL_PER_KILOGRAM / GRAMS_PER_KILOGRAM / DAYS_PER_WEEK
    val gap = (targetPerWeekGrams - fitted.perWeek) * kcalPerGramPerWeek
    // The band is the fit's own uncertainty rather than a decoration: the 7700 above is an approximation and
    // weight moves on water, glycogen and sodium, so the answer is reported as the range it is. A fit that
    // cannot measure its own scatter contributes none, which is the honest answer for three readings.
    val uncertainty = (fitted.perWeekStandardError ?: 0.0) * kcalPerGramPerWeek
    return EnergyAdjustment(
        lowKcal = floor(gap - uncertainty).toInt(),
        highKcal = ceil(gap + uncertainty).toInt(),
    )
}

/**
 * The adjustment that would bring the weight back inside a corridor around [targetGrams] (ROADMAP N135).
 *
 * Maintenance is a **corridor**, not a rate of zero, so this is a *level* gap rather than a rate gap: the
 * question is whether the weight is inside the band, not whether the slope is zero. The **fitted line**
 * answers it rather than the newest reading — the daily wobble is what a corridor exists to absorb — falling
 * back to the newest reading when there is nothing to fit.
 *
 * A point rather than a range: the corridor is already the band, and the fit's own scatter added on top would
 * be a second band around a band. Null when the series holds no weight to ask about.
 */
fun MetricSeries.energyAdjustmentToCorridor(
    targetGrams: Double,
    corridorGrams: Double = MAINTENANCE_CORRIDOR_GRAMS.toDouble(),
): EnergyAdjustment? {
    val latest = readings.lastOrNull { it.value != null }
    // The fitted line's own value at the newest reading, so the corridor is judged on the trend rather than on
    // the day's water — falling back to that reading when there is nothing to fit.
    val fitted = latest?.let { reading ->
        trend()?.let { line -> line.intercept + line.slope * reading.at.toEpochMilli().toDouble() }
            ?: reading.value
    }
    if (fitted == null) return null
    val gapGrams = when {
        fitted > targetGrams + corridorGrams -> targetGrams + corridorGrams - fitted
        fitted < targetGrams - corridorGrams -> targetGrams - corridorGrams - fitted
        else -> 0.0
    }
    val kcal = (gapGrams * KCAL_PER_KILOGRAM / GRAMS_PER_KILOGRAM).roundToInt()
    return EnergyAdjustment(lowKcal = kcal, highKcal = kcal)
}

/**
 * A rate as an editable number, for a dialog to seed from (ROADMAP N98).
 *
 * The display form uses a typographic minus, which `toDoubleOrNull` does not read back — so a field seeded with
 * it would refuse to save the value it was shown. Two functions rather than one with a flag, because the two
 * forms exist for different readers.
 */
fun editableRate(value: Double, unit: MetricUnit): String {
    val magnitude = unit.formatRate(abs(value))
    return if (value < 0) "-$magnitude" else magnitude
}
