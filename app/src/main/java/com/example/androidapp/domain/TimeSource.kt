package com.example.androidapp.domain

import java.time.Instant

/**
 * The clock, injected rather than read inline.
 *
 * Sessions, durations and (in P1.4) the rest timer all need to be asserted at
 * chosen instants. `Instant.now()` called directly inside a repository would make
 * that untestable without sleeping, so the clock is a dependency.
 */
fun interface TimeSource {
    fun now(): Instant
}

/**
 * The zone the device is in, in minutes from UTC (ROADMAP N25).
 *
 * Its own port rather than a member of [TimeSource]: that one is a SAM interface used as a lambda at
 * every call site, and a second member would break all of them. Kept separate also means a session's
 * offset can be tested at any zone without moving the clock.
 */
fun interface ZoneOffsetSource {
    /** Minutes east of UTC: 540 for Tokyo, -300 for New York in winter. */
    fun offsetMinutes(): Int
}

/** Epoch milliseconds — the representation the entities store. */
fun TimeSource.nowEpochMillis(): Long = now().toEpochMilli()

/**
 * The zone a screen's local day is read in (ROADMAP N286).
 *
 * Separate from [TimeSource] for the same reason [ZoneOffsetSource] is: the API takes no arguments, so it can
 * be a SAM in tests. A screen that crosses midnight or changes zone asks this again on resume.
 */
fun interface ZoneSource {
    fun zone(): java.time.ZoneId
}
