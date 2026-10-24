package com.example.androidapp.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
 * The local day an instant falls on, in [zone] (ROADMAP N25, N32).
 *
 * **One rule, in one place.** It decided which day a measurement belongs to inside the repository alone until
 * N114 needed the same answer for the home row that offers to record one, and the two disagreeing is the one
 * failure that would read as the data being wrong — a row hidden while the measurements screen shows nothing
 * for today.
 */
fun Instant.localDay(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

/**
 * The half-open `[start, end)` range of [instant]'s local day, in [zone] (ROADMAP N32, N113).
 *
 * The repository's form of [localDay]: it asks which instant range is "this day" when it looks for the
 * day's existing entry (one of each kind, N113) and when it decides whether a day has been re-entered
 * since a delete (N133). Two callers, one answer, so it lives beside the rule it applies.
 *
 * **A day is not 86 400 seconds.** A local day is 23 hours on a spring-forward date and 25 on a
 * fall-back one, so the end is the *next* day's midnight — `day.plusDays(1)` in the same zone — rather
 * than midnight plus a fixed span. A fixed span runs an hour into the next day on the 23-hour one, so a
 * save finds and overwrites the previous day's entry, and stops an hour short on the 25-hour one, so a
 * late-evening save misses the day's own row and writes a second.
 */
fun Instant.localDayBounds(zone: ZoneId): Pair<Long, Long> {
    val day = localDay(zone)
    return day.atStartOfDay(zone).toInstant().toEpochMilli() to
        day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}

/**
 * The same reading on another day, keeping the hour it was taken at (B212).
 *
 * The edit form corrects **which day** a measurement belongs to, and that is not a claim that the reading was
 * taken at a different hour: a morning weigh-in moved to the wrong date is still a morning weigh-in. Built in
 * [zone] because that is the zone the day is read in ([localDayBounds]), so the round trip through a picker
 * cannot land an entry on the day beside the one that was chosen — including the spring-forward day, where
 * 02:30 does not exist and the resolver moves it forward rather than into the next day.
 */
fun Instant.withDay(day: LocalDate, zone: ZoneId): Instant =
    day.atTime(atZone(zone).toLocalTime()).atZone(zone).toInstant()

/**
 * Whether this reading's day is after [now]'s day, in [zone] (B212).
 *
 * A record is of the past: a day after today would make the charts plot a reading that has not happened and
 * the trend's newest value a prediction. The comparison is between **days**, not instants — this morning is
 * today even though it is earlier, and 23:30 in Berlin still belongs to the day Berlin is on — which is the
 * same reading of a day [localDayBounds] writes rows by.
 */
fun Instant.isMeasurementDayAfterToday(now: Instant, zone: ZoneId): Boolean =
    localDay(zone).isAfter(now.localDay(zone))

/**
 * Whether this entry is [day]'s **daily** record, in [zone] (ROADMAP N114).
 *
 * The daily kind, not either kind: a day keeps one entry of each (N113), so a tape day is no reason to stop
 * offering to write down soreness, weight and the two percentages. And the zone is the caller's, because the
 * day an entry belongs to is the day it was taken on.
 */
fun BodyMeasurement.isDailyOn(day: LocalDate, zone: ZoneId): Boolean =
    kind == MeasurementKind.DAILY && measuredAt.localDay(zone) == day

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
