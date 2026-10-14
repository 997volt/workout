package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.TrendPoint
import com.example.androidapp.domain.model.at
import com.example.androidapp.domain.model.jointPainAt
import java.time.Instant

/**
 * One point of a series: when it was, and what the metric was then (ROADMAP N35).
 *
 * [value] is null where the reading was not taken, and that null is kept rather than dropped. It is the
 * difference between "you did not measure this then" and "there was no then": N37 breaks the line at a
 * missing reading instead of spanning it, and a series that quietly closed its own gaps could not.
 */
data class MetricReading(val at: Instant, val value: Double?)

/** A whole series, oldest first — the order the chart, the average and the moving average all want. */
data class MetricSeries(
    val key: MetricKey,
    val readings: List<MetricReading>,
)

/**
 * The readings for one metric (ROADMAP N35).
 *
 * Pure, and takes every source rather than a repository, for two reasons: the twenty-one series come from
 * three different queries, and a function that had to be handed a database to be tested would be tested
 * less. A metric whose source is not supplied simply has no readings, which is a truthful answer for a
 * screen that has just opened.
 *
 * Sorted here rather than trusted from the caller: measurements arrive newest-first because that is how a
 * list reads, and the training series arrive oldest-first because that is how a chart reads. One of them
 * would have been wrong.
 */
fun metricSeries(
    key: MetricKey,
    workouts: List<TrendPoint> = emptyList(),
    exercises: List<ExerciseTrendPoint> = emptyList(),
    measurements: List<BodyMeasurement> = emptyList(),
): MetricSeries = MetricSeries(
    key = key,
    readings = when (key) {
        is MetricKey.Workout -> workouts.map { MetricReading(it.startedAt, key.metric.valueOf(it)) }
        is MetricKey.Exercise -> exercises.map { MetricReading(it.startedAt, key.metric.valueOf(it)) }
        is MetricKey.Body -> measurements.map { MetricReading(it.measuredAt, bodyValue(key.metric, it)) }
        is MetricKey.Tape -> measurements.map { MetricReading(it.measuredAt, it.at(key.site)?.toDouble()) }
        // N118: the daily record's own list. A day that does not state this muscle is a null reading — the
        // gap the tape sites already leave — because a zero would claim a lifter was not sore on a day they
        // simply did not answer.
        is MetricKey.Soreness -> measurements.map {
            MetricReading(
                it.measuredAt,
                it.soreMuscles.firstOrNull { sore -> sore.muscle == key.muscle }?.score?.toDouble(),
            )
        }
        // N130: the dimension the average drops. Null where the session did not name *this* site, which is
        // the difference between "the left knee was fine that day" and "nobody said" — the same gap the
        // tape sites and the sore list leave.
        is MetricKey.JointSite -> workouts.map {
            MetricReading(it.startedAt, it.jointPainAt(key.joint, key.side))
        }
        // N130's per-lift half: the same site, read off the chosen exercise's own points (N17) instead of
        // the session's. A site this lift did not name is a gap, for the reason the workout-level one is.
        is MetricKey.ExerciseJointSite -> exercises.map {
            MetricReading(it.startedAt, it.jointPainAt(key.joint, key.side))
        }
    }.sortedBy { it.at },
)

/** What a body metric reads from an entry, in the units the registry formats (ROADMAP N32, N113). */
private fun bodyValue(metric: BodyMetric, measurement: BodyMeasurement): Double? = when (metric) {
    // A daily entry may state no weight at all (N113), and a tape entry never does: null is a reading
    // this series simply does not have, which is what the chart already skips.
    BodyMetric.WEIGHT -> measurement.weightGrams?.toDouble()
    BodyMetric.BODY_FAT -> measurement.bodyFatTenths?.toDouble()
    BodyMetric.MUSCLE -> measurement.muscleTenths?.toDouble()
}
