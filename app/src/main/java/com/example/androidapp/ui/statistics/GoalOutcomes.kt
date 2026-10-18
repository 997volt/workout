package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.model.MAINTENANCE_CORRIDOR_GRAMS
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.weightRateBandPerWeekGrams
import com.example.androidapp.domain.repository.GoalPeriod
import kotlin.math.abs

/**
 * What a period's goal turned out to be, as far as the weight series can say (ROADMAP N137).
 *
 * [Unjudged] is its own answer rather than a miss: a period nobody weighed in during, or one too short to fit a
 * rate from, says nothing about whether the lifter reached what they asked for — and calling that a failure
 * would be the app inventing a verdict out of no data.
 */
sealed interface GoalOutcome {
    /** The period asked for nothing — the row a *Clear* writes, which keeps the past rather than deleting it. */
    data object NoGoal : GoalOutcome

    /** The goal was met: the target was reached, the rate sat in its band, the weight stayed in its corridor. */
    data object Met : GoalOutcome

    /** It was not, and the series is long enough to say so. */
    data object Missed : GoalOutcome

    /** Too little was recorded in the period to judge it (N98's floor). */
    data object Unjudged : GoalOutcome
}

/** One period of a goal, with what it asked for and what became of it (ROADMAP N137). */
data class GoalPeriodOutcome(val period: GoalPeriod, val outcome: GoalOutcome)

/**
 * How close to a stated weight counts as **reaching** it (ROADMAP N137).
 *
 * The corridor, rather than a second tolerance: the app already has exactly one answer to "close enough to a
 * weight", and it is the one maintenance holds (N135). A number invented here would be a second opinion about
 * the same question.
 */
private const val TARGET_TOLERANCE_GRAMS = MAINTENANCE_CORRIDOR_GRAMS

/**
 * What became of each of [periods], read from the weight series (ROADMAP N137).
 *
 * The reader the entry asks for: **clip the series to the period and compare it with what that period asked
 * for**. Each period runs from its own start to the next one's — [GoalPeriod] holds only its beginning — and the
 * last is open, so it runs to the end of the series. A period that recorded nothing is [GoalOutcome.Unjudged]
 * rather than a miss.
 *
 * What "met" means depends on the kind, which is why this needs N135's kinds at all:
 *
 *  - a **target** is a fact about a reading, so it needs no trend — the weight was reached when something in the
 *    period sat within [TARGET_TOLERANCE_GRAMS] of it;
 *  - a **direction** is a rate, so it is judged on the fitted trend being inside N135's band *and* on the side
 *    the lifter asked for — and it therefore needs a fit, and N98's floor behind it, because a verdict about the
 *    past must not be a rate from three weigh-ins either;
 *  - **maintenance** is a corridor, judged on the **fitted line** where there is one so a day of water is
 *    absorbed rather than counted against the period (N135's own rule for its corridor).
 *
 * A period whose row states neither kind nor value is [GoalOutcome.NoGoal]: it is not a goal that was missed, it
 * is a stretch of time the lifter was not asking for anything.
 */
fun goalOutcomes(periods: List<GoalPeriod>, readings: List<MetricReading>): List<GoalPeriodOutcome> =
    periods.mapIndexed { index, period ->
        GoalPeriodOutcome(period, outcomeOf(period, readingsWithin(period, periods.getOrNull(index + 1), readings)))
    }

/** The readings a period owns: from its start (unbounded when that is unknown) to the next period's start. */
private fun readingsWithin(
    period: GoalPeriod,
    next: GoalPeriod?,
    readings: List<MetricReading>,
): List<MetricReading> = readings.filter { reading ->
    val afterStart = period.startedAt?.let { !reading.at.isBefore(it) } ?: true
    val beforeEnd = next?.startedAt?.let { reading.at.isBefore(it) } ?: true
    afterStart && beforeEnd
}

private fun outcomeOf(period: GoalPeriod, readings: List<MetricReading>): GoalOutcome {
    val goal = period.weightGoal
    return when {
        goal == null -> GoalOutcome.NoGoal
        readings.none { it.value != null } -> GoalOutcome.Unjudged
        goal is WeightGoal.Target ->
            if (readings.any { reached(it, goal.weightGrams) }) GoalOutcome.Met else GoalOutcome.Missed

        goal is WeightGoal.Maintain ->
            if (stayedInCorridor(goal.weightGrams, readings)) GoalOutcome.Met else GoalOutcome.Missed

        else -> rateOutcome(goal as WeightGoal.Direction, readings)
    }
}

private fun reached(reading: MetricReading, targetGrams: Long): Boolean =
    reading.value?.let { abs(it - targetGrams) <= TARGET_TOLERANCE_GRAMS } ?: false

private fun stayedInCorridor(targetGrams: Long, readings: List<MetricReading>): Boolean {
    val inside = { value: Double -> abs(value - targetGrams) <= MAINTENANCE_CORRIDOR_GRAMS }
    val recorded = readings.filter { it.value != null }
    val line = MetricSeries(MetricKey.Body(BodyMetric.WEIGHT), readings).trend()
    return if (line != null) {
        // The line's own ends, which is "where the weight was heading at each end of the period" rather than
        // what one morning's reading happened to be.
        val first = recorded.first().at.toEpochMilli().toDouble()
        val last = recorded.last().at.toEpochMilli().toDouble()
        inside(line.intercept + line.slope * first) && inside(line.intercept + line.slope * last)
    } else {
        recorded.all { inside(it.value!!) }
    }
}

/**
 * Whether the period's trend sat in the band the goal asked for (N135, N137, N149).
 *
 * Both halves matter: inside the band and **on the side the lifter asked for**. A trend of +0.3 kg a week is a
 * perfectly ordinary magnitude and the opposite of a lose goal, and a check that only looked at the magnitude
 * would call it met.
 */
private fun rateOutcome(goal: WeightGoal.Direction, readings: List<MetricReading>): GoalOutcome {
    val series = MetricSeries(MetricKey.Body(BodyMetric.WEIGHT), readings)
    // N98's floor, for N98's reason: a rate from three weigh-ins is noise, and a verdict about the past must
    // not be either. A series with no weight in it has no band to judge against, which is the same answer.
    val fitted = series.trend()?.takeIf { series.hasEnoughToSpeak() }
    val band = readings.last { it.value != null }.value?.let {
        weightRateBandPerWeekGrams(it.toLong(), gain = goal.gain)
    }
    return if (fitted == null || band == null) {
        GoalOutcome.Unjudged
    } else {
        val rightWay = if (goal.gain) fitted.perWeek > 0 else fitted.perWeek < 0
        if (rightWay && abs(fitted.perWeek) in band) GoalOutcome.Met else GoalOutcome.Missed
    }
}
