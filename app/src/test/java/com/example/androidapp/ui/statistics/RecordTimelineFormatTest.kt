package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.RecordEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * The records timeline's pure half (ROADMAP N290).
 *
 * Grouping and ordering are the two things a list gets wrong quietly: a record set near midnight in another
 * zone can land in the wrong month, and a read that is already ordered can be re-sorted into a different
 * story without anyone noticing. Both are asserted here rather than eyeballed on a screen.
 */
class RecordTimelineFormatTest {

    @Test
    fun recordsAreNewestFirst_andGroupedByTheMonthTheyWerePerformedIn() {
        val records = listOf(
            load("Back squat", at = "2026-09-28T10:00:00Z"),
            load("Bench press", at = "2026-10-04T10:00:00Z"),
            load("Deadlift", at = "2026-10-01T10:00:00Z"),
        )

        val months = RecordTimelineFormat.months(records, fallbackZone = ZoneOffset.UTC)

        assertThat(months.map { it.month })
            .containsExactly(YearMonth.of(2026, 10), YearMonth.of(2026, 9))
            .inOrder()
        assertThat(months[0].records.map { it.exerciseName })
            .containsExactly("Bench press", "Deadlift")
            .inOrder()
    }

    @Test
    fun aRecordNearMidnight_belongsToTheMonthItWasPerformedIn_notTheReaders() {
        // 23:30 UTC on 30 September is 01:30 on 1 October in a +02:00 session. The session's own zone is the
        // fact, so the record belongs to October even when it is read from a UTC device (ROADMAP N25).
        val record = load("Back squat", at = "2026-09-30T23:30:00Z", zoneOffsetMinutes = 120)

        val months = RecordTimelineFormat.months(listOf(record), fallbackZone = ZoneOffset.UTC)

        assertThat(months.single().month).isEqualTo(YearMonth.of(2026, 10))
    }

    @Test
    fun recordsWithTheSameTimestamp_keepTheQuerysOrder() {
        // The query orders a session's later set first, which is the order the record rule calls "later".
        // Sorting is stable, so the helper must not turn two records performed in one session into a tie it
        // breaks by itself.
        val first = load("Back squat", at = "2026-10-04T10:00:00Z", reps = 5)
        val second = load("Back squat", at = "2026-10-04T10:00:00Z", reps = 3)

        val months = RecordTimelineFormat.months(listOf(first, second), fallbackZone = ZoneOffset.UTC)

        assertThat(months.single().records.map { (it as RecordEvent.Load).reps })
            .containsExactly(5, 3)
            .inOrder()
    }

    @Test
    fun aWeightedRecord_readsInTheUnitTheExerciseUses() {
        val kilograms = load("Back squat", at = "2026-10-04T10:00:00Z", weightGrams = 100_000L, reps = 5)
        val pounds = load(
            "Back squat",
            at = "2026-10-04T10:00:00Z",
            weightGrams = 60_000L,
            reps = 5,
            weightUnit = WeightUnit.POUNDS,
        )

        assertThat(RecordTimelineFormat.loadText(kilograms, WeightUnit.KILOGRAMS, "kg"))
            .isEqualTo("100 kg × 5")
        assertThat(RecordTimelineFormat.loadText(pounds, WeightUnit.POUNDS, "lb"))
            .isEqualTo("132.3 lb × 5")
    }

    @Test
    fun aHold_readsAsATime() {
        val hold = RecordEvent.Hold(
            exerciseName = "Farmer carry",
            performedAt = Instant.parse("2026-10-04T10:00:00Z"),
            zoneOffsetMinutes = 0,
            seconds = 45,
        )

        assertThat(RecordTimelineFormat.holdText(hold)).isEqualTo("0:45")
    }

    private fun load(
        name: String,
        at: String,
        zoneOffsetMinutes: Int = 0,
        weightGrams: Long = 100_000L,
        reps: Int = 5,
        weightUnit: WeightUnit? = null,
    ) = RecordEvent.Load(
        exerciseName = name,
        performedAt = Instant.parse(at),
        zoneOffsetMinutes = zoneOffsetMinutes,
        weightUnit = weightUnit,
        weightGrams = weightGrams,
        reps = reps,
    )
}
