package com.example.androidapp.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The day rules the goal timeline is written by, on the JVM (ROADMAP B223, B224, B238).
 *
 * `GoalPeriodsTest` proves the rows on a device; this is the arithmetic that made the rows wrong when it
 * compared instants, so it can run in the default gate.
 */
class GoalTimelineTest {

    private val zone = ZoneId.of("UTC")
    private val march = LocalDate.of(2026, 3, 1)
    private val clockOnMarch = march.atTime(9, 15).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun aClockBoundary_isFoundByItsLocalDay() {
        // The row the old setGoal path wrote: not midnight, but 1 March all the same (B223).
        assertThat(GoalTimeline.startsOn(clockOnMarch, march, zone)).isTrue()
        assertThat(GoalTimeline.startsOn(clockOnMarch, march.plusDays(1), zone)).isFalse()
        assertThat(GoalTimeline.dayOf(clockOnMarch, zone)).isEqualTo(march)
    }

    @Test
    fun aMidnightStartAndAClockBoundary_onOneDay_areTheSameBoundary() {
        val rows = listOf(GoalBoundary(id = "old", startedAt = clockOnMarch, kind = "TARGET", value = 80_000.0))

        assertThat(GoalTimeline.boundaryOn(rows, march, zone)?.id).isEqualTo("old")
        assertThat(GoalTimeline.isTaken(rows, id = "new", day = march, zone = zone)).isTrue()
        assertThat(GoalTimeline.isTaken(rows, id = "old", day = march, zone = zone)).isFalse()
    }

    @Test
    fun theRowGoverningTheStart_isTheLastOneBeforeTheDay() {
        // The row on the day is the boundary a write edits, not the goal a redundant write compares with;
        // picking the wrong one is how B140's guard would become a no-op for the wrong statement (B224).
        val rows = listOf(
            GoalBoundary(
                "january",
                LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
                "TARGET",
                80_000.0,
            ),
            GoalBoundary("march", clockOnMarch, "LOSE", null),
        )

        assertThat(GoalTimeline.governing(rows, march, zone)?.id).isEqualTo("january")
        assertThat(GoalTimeline.governing(rows, LocalDate.of(2026, 2, 1), zone)?.id).isEqualTo("january")
        assertThat(GoalTimeline.governing(rows, LocalDate.of(2026, 4, 1), zone)?.id).isEqualTo("march")
    }

    @Test
    fun anUndatedFirstRow_governsBeforeEveryDatedOne() {
        val rows = listOf(
            GoalBoundary("legacy", null, "TARGET", 80_000.0),
            GoalBoundary("march", clockOnMarch, "LOSE", null),
        )

        assertThat(GoalTimeline.governing(rows, LocalDate.of(2026, 2, 1), zone)?.id).isEqualTo("legacy")
        assertThat(GoalTimeline.governing(rows, LocalDate.of(2026, 1, 1), zone)?.id).isEqualTo("legacy")
    }

    @Test
    fun theSameGoal_isKindAndValue_notTheInstant() {
        assertThat(GoalTimeline.sameGoal("TARGET", 80_000.0, "TARGET", 80_000.0)).isTrue()
        assertThat(GoalTimeline.sameGoal("TARGET", 80_000.0, "TARGET", 79_000.0)).isFalse()
        assertThat(GoalTimeline.sameGoal("TARGET", 80_000.0, "MAINTAIN", 80_000.0)).isFalse()
        assertThat(GoalTimeline.sameGoal(null, null, null, null)).isTrue()
        assertThat(GoalTimeline.sameGoal(null, null, "LOSE", null)).isFalse()
    }
}
