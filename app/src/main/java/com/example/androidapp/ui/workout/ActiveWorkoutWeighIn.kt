package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.LatestWeight
import com.example.androidapp.domain.repository.MeasurementRepository
import java.time.Duration
import java.time.Instant

// The lifter's weight on record (ROADMAP N219, B251; moved out of ActiveWorkoutViewModel by N285): the one
// read the logging path makes, the staleness rule that asks rather than assumes, and the targeted write an
// answer performs.

/** The newest weight on record, with its day (ROADMAP N219, B251). */
internal suspend fun newestWeight(measurements: MeasurementRepository): DataResult<LatestWeight?> =
    measurements.latestWeight()

/**
 * Whether the newest weigh-in is too old to be used without being asked about (B251).
 *
 * A month is the line: the body moves, and a number from last season written into today's sets would
 * put a figure in every total that was never measured. The lifter answers with today's weight, which
 * is then what the app keeps.
 */
internal fun LatestWeight.isStale(now: Instant): Boolean = ageInDays(now) >= STALE_WEIGH_IN_DAYS

internal fun LatestWeight.ageInDays(now: Instant): Long =
    Duration.between(at, now).toDays().coerceAtLeast(0L)

/**
 * Writes [grams] as today's daily weight, keeping whatever else today already says (N62, N113).
 *
 * The targeted write rather than a whole-entry save: a save replaces the day's record, so answering this
 * prompt used to copy the day's body fat and soreness back by hand — after scanning every measurement to
 * find them (B255). The day keeps its numbers, and the logging path performs one query.
 */
internal suspend fun saveTodayWeight(
    measurements: MeasurementRepository,
    at: Instant,
    grams: Long,
): DataResult<Unit> = measurements.setDailyWeight(at = at, grams = grams)

/** A weigh-in older than this is a question rather than an answer (ROADMAP N219, B251). */
private const val STALE_WEIGH_IN_DAYS = 30L
