package com.example.androidapp.data.local

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One boundary as the timeline rules read it (ROADMAP N215, B223, B238).
 *
 * Deliberately not [MetricGoalEntity]: the rules are about days and goals, and a pure test can hold them
 * without a database. The entity is mapped at the repository's edge.
 */
internal data class GoalBoundary(
    val id: String,
    val startedAt: Long?,
    val kind: String?,
    val value: Double?,
)

/**
 * The day rules the goal timeline is written by (ROADMAP N215, B223, B238).
 *
 * A goal is a statement about **days**, but the store holds rows written by two generations: the old
 * `setGoal` path wrote the clock instant, and N215 writes local midnight. Comparing the instants made the
 * same day two different statements — a goal set on the day of an old boundary could be written before it
 * and never become the goal in force (B223). Everything here answers in days, so the two generations meet.
 */
internal object GoalTimeline {

    /** The local day a stored instant belongs to. */
    fun dayOf(startedAt: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()

    /** Whether a boundary (dated or not) begins on [day]. */
    fun startsOn(startedAt: Long?, day: LocalDate, zone: ZoneId): Boolean =
        startedAt != null && dayOf(startedAt, zone) == day

    /** Local midnight of [day], the instant N215 writes. */
    fun startOfDay(day: LocalDate, zone: ZoneId): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli()

    /** The row whose boundary is [day], whether it was written at midnight or at a clock time. */

    /**
     * The row in force before [day]: the last one that starts on an earlier day, or the undated first row.
     *
     * A row that starts **on** [day] is not the one governing it — it *is* the boundary a write on that day
     * edits — so it is excluded. This is the row B140's no-op guard compares a new period with (B224).
     */
    fun governing(rows: List<GoalBoundary>, day: LocalDate, zone: ZoneId): GoalBoundary? =
        rows.lastOrNull { it.startedAt == null || dayOf(it.startedAt, zone) < day }

    /** Whether another row already begins on [day]. [id] is the row being edited, or null for an insert. */
    fun isTaken(rows: List<GoalBoundary>, id: String?, day: LocalDate, zone: ZoneId): Boolean =
        rows.any { it.id != id && startsOn(it.startedAt, day, zone) }

    /** Whether two periods state the same goal, which B140's guard reads as "not a change". */
    fun sameGoal(kind: String?, value: Double?, otherKind: String?, otherValue: Double?): Boolean =
        kind == otherKind && value == otherValue
}
