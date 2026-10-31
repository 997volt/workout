package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The weekly review's arithmetic (ROADMAP N247).
 *
 * The two rules worth pinning: a session belongs to the week of **its own** day, and a deload is scheduled
 * without being a session somebody failed to do.
 */
class WeeklyReviewTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun theWeekIsMondayToSunday_inTheSessionSOwnDay() {
        val sessions = listOf(
            // 01:30 CET on the Monday: this week.
            session(id = "mon", at = "2026-10-26T00:30:00Z", sets = 2, volume = 200),
            // 00:30 CET on the next Monday: the following week, not this one.
            session(id = "sun", at = "2026-11-01T23:30:00Z", sets = 3, volume = 300),
            // Sunday of the previous week.
            session(id = "prev", at = "2026-10-25T12:00:00Z", sets = 5, volume = 500),
        )

        val thisWeek = weekTotals(sessions, monday, zone)
        val lastWeek = weekTotals(sessions, monday.minusWeeks(1), zone)

        assertThat(thisWeek).isEqualTo(WeekTotals(sessions = 1, sets = 2, volumeGrams = 200))
        assertThat(lastWeek).isEqualTo(WeekTotals(sessions = 1, sets = 5, volumeGrams = 500))
    }

    @Test
    fun aDeload_isScheduledButNotAPlannedSession() {
        val occurrences = listOf(
            occurrence(OccurrenceState.DONE),
            occurrence(OccurrenceState.DONE),
            occurrence(OccurrenceState.MISSED),
            occurrence(OccurrenceState.PENDING),
            occurrence(OccurrenceState.DELOAD),
        )

        val plan = weekPlan(occurrences)

        assertThat(plan.planned).isEqualTo(4)
        assertThat(plan.done).isEqualTo(2)
        assertThat(plan.remaining).isEqualTo(2)
    }

    private fun session(id: String, at: String, sets: Int, volume: Long) = WorkoutSummary(
        id = id,
        startedAt = Instant.parse(at),
        finishedAt = Instant.parse(at).plusSeconds(3_600),
        exerciseCount = 1,
        setCount = sets,
        volumeGrams = volume,
    )

    private fun occurrence(state: OccurrenceState) = DayOccurrence(
        slotId = "slot",
        templateId = "template",
        templateName = "Squat day",
        date = monday,
        weekStart = monday,
        state = state,
        canCorrect = false,
    )
}
