package com.example.androidapp.domain.model

import java.time.Instant

/**
 * One dated set of body measurements (ROADMAP N32, split in two by N113).
 *
 * **Weight is no longer always there.** The record is two records, and this is one row of one of them:
 * a [MeasurementKind.DAILY] entry holds what a day says about the body — soreness, weight, body fat and
 * muscle — and a [MeasurementKind.TAPE] entry holds the tape sites. Each number is null when it was not
 * taken, and **null is not zero**: a waist measured on a morning the scale was not stepped on is a real
 * entry, and a zero would read as a measurement of nothing.
 *
 * The units are the ones the schema stores — grams, tenths of a percent, millimetres — because
 * converting at the edge is how a rounding error gets into a database. Percentages are tenths for the
 * same reason RPE is halves: 18.3 has no exact binary representation.
 */
data class BodyMeasurement(
    val id: String,
    val measuredAt: Instant,
    /** Which of the two records this entry is (ROADMAP N113). */
    val kind: MeasurementKind,
    /** Whole grams, or null when the scale was not stepped on (ROADMAP N113). */
    val weightGrams: Long? = null,
    val bodyFatTenths: Int? = null,
    val muscleTenths: Int? = null,
    /** The tape sites, in millimetres, every one optional. */
    val tape: Map<TapeSite, Long> = emptyMap(),
    /**
     * The muscles this day reported sore, each with its own score (ROADMAP N62, moved here by N113).
     *
     * A child list of the daily record rather than of the workout: soreness is a fact about the body's
     * recovery, and on a session it could only exist on a day that was trained — the rest day, when it
     * is the thing worth writing down, had nowhere to put it.
     */
    val soreMuscles: List<SoreMuscle> = emptyList(),
)

/**
 * Which of the body's two records an entry is (ROADMAP N113).
 *
 * Two records rather than one row with everything nullable, because they are taken on different days
 * and ask different questions: a tape day has no weight to state, and a daily entry has no business
 * asking for seven sites. They ride one table and one export list — each row carrying only its own
 * kind's numbers — so the statistics screen's body and tape series keep reading one list.
 */
enum class MeasurementKind {
    /** Soreness, weight, body fat and muscle: what a day says about the body (N113). */
    DAILY,

    /** The tape sites (N113). */
    TAPE,
}

/**
 * The tape sites v1 measures (ROADMAP N32).
 *
 * A fixed set rather than a user-defined list: seven named sites are answerable, and a free list is a
 * later row rather than a first one. An unmeasured site is simply absent from [BodyMeasurement.tape] —
 * it does not carry the last value forward, because carrying a number forward invents a measurement.
 */
enum class TapeSite {
    NECK,
    CHEST,
    WAIST,
    HIPS,
    UPPER_ARM,
    THIGH,
    CALF,
}

/** The value taken at [site], or null when it was not measured. */
fun BodyMeasurement.at(site: TapeSite): Long? = tape[site]

/**
 * Whether this entry states any of its own numbers (ROADMAP N113).
 *
 * The rule that enables *Save*: the weight used to be the one thing an entry could not be without, and
 * splitting the record took that away — a soreness-only morning is a real entry, so a form cannot
 * demand a weight it was not given. What it can refuse is an entry that states nothing at all.
 */
fun BodyMeasurement.statesSomething(): Boolean = when (kind) {
    MeasurementKind.DAILY ->
        weightGrams != null || bodyFatTenths != null || muscleTenths != null || soreMuscles.isNotEmpty()

    MeasurementKind.TAPE -> tape.isNotEmpty()
}
