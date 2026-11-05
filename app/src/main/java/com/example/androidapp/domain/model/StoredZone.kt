package com.example.androidapp.domain.model

import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The zone a stored offset names, or null when the stored value is not a legal offset (B282, B341).
 *
 * A backup can carry any integer, and `ZoneOffset.ofTotalSeconds` throws outside ±18:00. The app's
 * rows are written from a real `ZoneId`, so this only matters for an imported file — which is
 * exactly the path that used to take a screen down: one bad row killed the history flow, and the
 * program/adherence mappers were still asking the throwing constructor directly.
 *
 * The multiplication is done in a `Long` before the range check: `Int.MAX_VALUE * 60` overflows an
 * `Int` and can wrap into a value the constructor accepts, which would be a total function that is
 * not one.
 */
fun storedZoneOrNull(offsetMinutes: Int?): ZoneId? =
    offsetMinutes
        ?.toLong()
        ?.times(SECONDS_PER_MINUTE)
        ?.takeIf { it in MIN_OFFSET_SECONDS..MAX_OFFSET_SECONDS }
        ?.let { ZoneOffset.ofTotalSeconds(it.toInt()) }

/** The stored offset's zone, or [fallback] for a row without one or with an illegal value. */
fun storedZoneOrFallback(offsetMinutes: Int?, fallback: ZoneId): ZoneId =
    storedZoneOrNull(offsetMinutes) ?: fallback

/** Minutes to seconds, the unit [ZoneOffset] wants. */
private const val SECONDS_PER_MINUTE = 60L

/** A `ZoneOffset` is defined only within ±18:00. */
private const val MAX_OFFSET_SECONDS = 18L * 60 * 60
private const val MIN_OFFSET_SECONDS = -MAX_OFFSET_SECONDS
