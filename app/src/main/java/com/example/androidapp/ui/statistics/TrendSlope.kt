package com.example.androidapp.ui.statistics

import kotlin.math.sqrt

/**
 * A straight line fitted to a series, and what it says per week (ROADMAP N39).
 *
 * Per week rather than per reading, because a slope per point means nothing: two readings a day apart and two
 * a month apart would report the same number for very different changes.
 */
data class TrendSlope(
    /** How much the value changes in a week, in the metric's own units. */
    val perWeek: Double,
    /** The fitted line itself, for drawing: value = intercept + slope × epochMillis. */
    val slope: Double,
    val intercept: Double,
    /**
     * How far [perWeek] could be out, in the metric's own units per week, or null where the fit cannot say.
     *
     * This is what the energy statement's band is made of (ROADMAP N98), so it is *measured* rather than
     * assumed: the residuals around the line are the only honest source of it. Two readings report null
     * because the line passes through both and every residual is zero — a certainty the data does not
     * support, and the one place a perfect fit would be read as knowledge.
     */
    val perWeekStandardError: Double? = null,
) {
    /**
     * Where the line is at [fraction] across the series, for drawing it.
     *
     * The chart's x is elapsed time as a fraction, so a line drawn from end to end has to be evaluated there
     * rather than stored as two readings.
     */
    fun valueAt(fraction: Double, first: Double, span: Double): Double =
        intercept + slope * (first + span * fraction)
}

/**
 * The least-squares line through the recorded readings (ROADMAP N39).
 *
 * Null when a line would be a lie: fewer than two readings, or all of them at the same instant, which has no
 * slope to speak of. A single reading is a number, not a trend — the same rule the chart's line follows.
 *
 * **A gap does not pull the line.** Only the readings that exist are fitted, so a month nobody measured is a
 * month the line knows nothing about, rather than a month of zeros dragging it down. That is the same
 * argument the average makes, applied to a line.
 */
fun MetricSeries.trend(): TrendSlope? {
    val recorded = readings.mapNotNull { reading -> reading.value?.let { reading.at to it } }
    val times = recorded.map { it.first.toEpochMilli().toDouble() }
    val values = recorded.map { it.second }

    // One return, for the two cases where a line would be a lie: fewer than two readings, and none of them
    // apart in time — which is what "no slope to speak of" means. Both are checked before the means, because
    // an average of nothing is NaN rather than an error.
    if (recorded.size < 2 || times.distinct().size < 2) return null

    val meanTime = times.average()
    val meanValue = values.average()
    val spread = times.sumOf { (it - meanTime) * (it - meanTime) }
    val covariance = times.indices.sumOf { (times[it] - meanTime) * (values[it] - meanValue) }
    val slope = covariance / spread
    val intercept = meanValue - slope * meanTime

    return TrendSlope(
        perWeek = slope * MILLIS_PER_WEEK,
        slope = slope,
        intercept = intercept,
        perWeekStandardError = slopeStandardError(times, values, slope, intercept, spread),
    )
}

/**
 * The standard error of the slope, scaled to a week (ROADMAP N98).
 *
 * `sqrt(residualVariance / spread)`, which is the textbook error of a least-squares slope, and the reason it
 * is computed here rather than guessed at by the caller: a band invented at the call site would be a second
 * opinion about the same data. Null below three readings, for [TrendSlope.perWeekStandardError]'s reason.
 */
private fun slopeStandardError(
    times: List<Double>,
    values: List<Double>,
    slope: Double,
    intercept: Double,
    spread: Double,
): Double? {
    if (times.size < MIN_READINGS_FOR_ERROR || spread == 0.0) return null
    val residualSum = times.indices.sumOf { index ->
        val residual = values[index] - (intercept + slope * times[index])
        residual * residual
    }
    val variance = residualSum / (times.size - FIT_PARAMETERS)
    return sqrt(variance / spread) * MILLIS_PER_WEEK
}

/**
 * The average and the trend as one line of text, in the metric's own unit (ROADMAP N39).
 *
 * Signed, because a slope with no sign is half the information: "0.3 kg/week" reads as progress on a
 * bodyweight and as a warning on joint pain, and which it is depends on a direction the metric knows and this
 * formatting does not.
 */
fun slopeText(perWeek: Double, format: (Double) -> String): String {
    val sign = if (perWeek < 0) "−" else "+"
    return sign + format(kotlin.math.abs(perWeek))
}

/** Kept here rather than in the data class so the arithmetic above reads as one thing. */
private const val MILLIS_PER_WEEK = 7.0 * 24 * 60 * 60 * 1000

/** What a fitted line costs the data: a slope and an intercept. Kept named so the division reads as one. */
private const val FIT_PARAMETERS = 2

/** A residual needs one reading more than the line's own parameters, or there is nothing left to measure. */
private const val MIN_READINGS_FOR_ERROR = FIT_PARAMETERS + 1
