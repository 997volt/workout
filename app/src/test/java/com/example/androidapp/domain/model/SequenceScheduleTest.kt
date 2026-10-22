package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A finite program's place on the calendar (ROADMAP N175).
 *
 * The rule under test is the one thing a sequence adds to the schedule: **a weekday places a single
 * session** rather than scheduling a recurring one, so the due date moves with what was actually
 * done. Pure, so every case is a date rather than a device.
 */
class SequenceScheduleTest {

    // A Monday, so the weekday arithmetic is checkable by eye.
    private val start: LocalDate = LocalDate.of(2026, 10, 5)

    private fun slot(
        id: String,
        position: Int,
        weekday: DayOfWeek?,
        templateId: String = "tpl_$id",
    ) = ProgramSlot(
        id = id,
        programId = "pgm",
        templateId = templateId,
        position = position,
        weekday = weekday,
    )

    private fun done(templateId: String, date: LocalDate) = ProgramSession(
        sessionId = "s_$templateId$date",
        templateId = templateId,
        startedAt = date.atStartOfDay(ZoneOffset.UTC).toInstant(),
        zone = ZoneOffset.UTC,
    )

    private val mondayFriday = listOf(
        slot("a", 0, DayOfWeek.MONDAY),
        slot("b", 1, DayOfWeek.FRIDAY),
        slot("c", 2, DayOfWeek.MONDAY),
    )

    private fun schedule(
        slots: List<ProgramSlot> = mondayFriday,
        sessions: List<ProgramSession> = emptyList(),
        skips: List<RecordedSkip> = emptyList(),
        today: LocalDate = start.minusDays(1),
        from: LocalDate = start,
    ) = sequenceSchedule(slots, sessions, skips, from, today)

    @Test
    fun nothingDone_theFirstSessionIsDueOnItsFirstWeekdayAfterTheStart() {
        val schedule = schedule(today = start.minusDays(1))!!

        assertThat(schedule.next?.id).isEqualTo("a")
        assertThat(schedule.due).isEqualTo(start)
        assertThat(schedule.isDue).isFalse()
        assertThat(schedule.position).isEqualTo(1)
        assertThat(schedule.length).isEqualTo(3)
    }

    @Test
    fun onItsDueDay_aSessionIsDue_andIsOffered() {
        val schedule = schedule(today = start)!!

        assertThat(schedule.isDue).isTrue()
        assertThat(schedule.settled).isEqualTo(0)
    }

    @Test
    fun aBlockThatStartsNextWeek_isNotDueToday() {
        val schedule = schedule(today = start.minusDays(7), from = start.plusDays(7))!!

        assertThat(schedule.due).isEqualTo(start.plusDays(7))
        assertThat(schedule.isDue).isFalse()
    }

    @Test
    fun doneOnItsDay_theNextSessionMovesToItsOwnWeekday() {
        val schedule = schedule(
            sessions = listOf(done("tpl_a", start)),
            today = start,
        )!!

        assertThat(schedule.next?.id).isEqualTo("b")
        assertThat(schedule.due).isEqualTo(start.plusDays(4))
        assertThat(schedule.settled).isEqualTo(1)
        assertThat(schedule.position).isEqualTo(2)
    }

    @Test
    fun doneLate_theNextSessionSlidesFromTheDayItWasDone() {
        // Friday's session performed on Saturday afternoon, a common enough evening: Monday's is
        // still the Monday after, and nothing is instantly overdue (N175).
        val saturday = start.plusDays(5)
        val schedule = schedule(
            sessions = listOf(done("tpl_a", start), done("tpl_b", saturday)),
            today = saturday,
        )!!

        assertThat(schedule.next?.id).isEqualTo("c")
        assertThat(schedule.due).isEqualTo(start.plusDays(7))
        assertThat(schedule.isDue).isFalse()
    }

    @Test
    fun aSlotWithNoWeekday_isDueTheNextDay() {
        val whenever = listOf(
            slot("a", 0, null),
            slot("b", 1, null),
        )
        val first = schedule(slots = whenever, sessions = listOf(done("tpl_a", start)), today = start)!!

        assertThat(first.next?.id).isEqualTo("b")
        assertThat(first.due).isEqualTo(start.plusDays(1))
    }

    @Test
    fun aSkip_settlesTheSession_andMovesOn() {
        val schedule = schedule(
            sessions = listOf(done("tpl_a", start)),
            skips = listOf(RecordedSkip(slotId = "b", weekStart = start.plusDays(4))),
            today = start.plusDays(5),
        )!!

        assertThat(schedule.next?.id).isEqualTo("c")
        assertThat(schedule.settled).isEqualTo(2)
        assertThat(schedule.due).isEqualTo(start.plusDays(7))
    }

    @Test
    fun everySessionSettled_theBlockIsOver_andSaysSo() {
        val schedule = schedule(
            sessions = listOf(done("tpl_a", start), done("tpl_b", start.plusDays(4))),
            skips = listOf(RecordedSkip(slotId = "c", weekStart = start.plusDays(7))),
            today = start.plusDays(8),
        )!!

        assertThat(schedule.isComplete).isTrue()
        assertThat(schedule.next).isNull()
        assertThat(schedule.due).isNull()
        assertThat(schedule.settled).isEqualTo(3)
        assertThat(schedule.position).isEqualTo(3)
    }

    @Test
    fun aSessionForATemplateNoSlotNames_movesNothing() {
        val schedule = schedule(
            sessions = listOf(done("something_else", start)),
            today = start,
        )!!

        assertThat(schedule.next?.id).isEqualTo("a")
        assertThat(schedule.settled).isEqualTo(0)
    }

    @Test
    fun noSlots_isNoSchedule() {
        assertThat(schedule(slots = emptyList())).isNull()
    }

    @Test
    fun aSessionIsMatchedByItsTemplate_inTheBlocksOrder() {
        val repeated = listOf(
            slot("a", 0, DayOfWeek.MONDAY, templateId = "shared"),
            slot("b", 1, DayOfWeek.WEDNESDAY, templateId = "shared"),
        )
        val firstDone = Instant.parse("2026-10-05T10:00:00Z")

        val schedule = schedule(
            slots = repeated,
            sessions = listOf(
                ProgramSession("s1", "shared", firstDone, ZoneOffset.UTC),
            ),
            today = start,
        )!!

        // One session, two slots naming the same workout: the first takes it, so the block is at
        // its second session rather than counting the same workout twice.
        assertThat(schedule.next?.id).isEqualTo("b")
        assertThat(schedule.settled).isEqualTo(1)
    }
}
