package com.example.androidapp.ui.home

import com.example.androidapp.domain.model.DeloadDay
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.SequenceSchedule
import com.example.androidapp.domain.model.WorkoutProgram
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What a rest day names as coming next (ROADMAP N177).
 *
 * Two derivations behind one sentence: a weekly program's next slot is the next weekday in the week
 * that has one, and a block's next session is its due date. The earlier of the two is what the line
 * says, and a day with neither keeps the line that says how to start a program.
 */
class NextSessionTest {

    // A Tuesday, so both "later this week" and "wraps to Monday" are reachable.
    private val today: LocalDate = LocalDate.of(2026, 10, 6)

    private fun program(id: String, kind: ProgramKind = ProgramKind.WEEKLY) =
        WorkoutProgram(id = id, name = id, kind = kind, isActive = true)

    private fun deloadDay(
        id: String,
        templateId: String,
        weekday: DayOfWeek,
        name: String,
    ) = DeloadDay(
        id = id,
        programId = "weekly",
        templateId = templateId,
        templateName = name,
        exerciseCount = 3,
        position = 0,
        weekday = weekday,
    )

    private fun slot(id: String, day: DayOfWeek?, programId: String = "weekly", name: String = id) =
        ProgramSlot(
            id = id,
            programId = programId,
            templateId = "tpl_$id",
            position = id.last().digitToIntOrNull() ?: 0,
            weekday = day,
            templateName = name,
        )

    @Test
    fun aDeloadWeek_namesItsOwnDay_notTheSlotItDropped() {
        // B200: a built deload week replaces the whole week, so the slot the plan dropped is not a
        // session that is coming — the plan's own day is.
        val sessions = listOf(
            slot("a0", DayOfWeek.MONDAY, name = "Push"),
            slot("a1", DayOfWeek.WEDNESDAY, name = "Pull"),
            slot("a2", DayOfWeek.THURSDAY, name = "Legs"),
        )
        val deload = DeloadWeek(
            programId = "weekly",
            weekStart = today.minusDays(1),
            days = listOf(
                deloadDay("dd1", templateId = "tpl_a1", weekday = DayOfWeek.WEDNESDAY, name = "Pull (deload)"),
                deloadDay("dd2", templateId = "tpl_a2", weekday = DayOfWeek.THURSDAY, name = "Legs (deload)"),
            ),
        )

        val next = nextWeeklySession(listOf(program("weekly")), sessions, today, listOf(deload))!!

        assertThat(next.weekday).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(next.name).isEqualTo("Pull (deload)")
    }

    @Test
    fun aDeloadWeek_doesNotNameTheWeekAfterIt() {
        // B282: the deload list read for the line is *this* week's. A candidate in the next week used to be
        // answered with it, so the Sunday of a deload week named the deload day it was leaving instead of
        // the slot that is actually coming.
        val sunday = LocalDate.of(2026, 10, 11)
        val sessions = listOf(
            slot("a0", DayOfWeek.MONDAY, name = "Push"),
        )
        val deload = DeloadWeek(
            programId = "weekly",
            weekStart = sunday.minusDays(6),
            days = listOf(
                deloadDay("dd1", templateId = "tpl_a0", weekday = DayOfWeek.MONDAY, name = "Push (deload)"),
            ),
        )

        val next = nextWeeklySession(listOf(program("weekly")), sessions, sunday, listOf(deload))!!

        assertThat(next.date).isEqualTo(sunday.plusDays(1))
        assertThat(next.name).isEqualTo("Push")
    }

    @Test
    fun aWeeklyProgram_namesTheNextWeekdayWithASlot() {
        val sessions = listOf(
            slot("a0", DayOfWeek.MONDAY, name = "Push"),
            slot("a1", DayOfWeek.WEDNESDAY, name = "Pull"),
            slot("a2", DayOfWeek.FRIDAY, name = "Legs"),
        )

        val next = nextWeeklySession(listOf(program("weekly")), sessions, today)!!

        assertThat(next.weekday).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(next.date).isEqualTo(today.plusDays(1))
        assertThat(next.name).isEqualTo("Pull")
    }

    @Test
    fun afterTheLastSlotOfTheWeek_itWrapsToTheFirst() {
        val friday = today.plusDays(3)
        val sessions = listOf(
            slot("a0", DayOfWeek.MONDAY, name = "Push"),
            slot("a2", DayOfWeek.FRIDAY, name = "Legs"),
        )

        val next = nextWeeklySession(listOf(program("weekly")), sessions, friday)!!

        assertThat(next.weekday).isEqualTo(DayOfWeek.MONDAY)
        assertThat(next.date).isEqualTo(friday.plusDays(3))
        assertThat(next.name).isEqualTo("Push")
    }

    @Test
    fun aBlock_sWeekdayIsNotAWeeklyOccurrence() {
        // A sequence's Monday slot is one session, not "every Monday": the weekly derivation must
        // not read it, or the rest line would promise a block session in a week it is not due (N176).
        val block = program("block", ProgramKind.SEQUENCE)
        val sessions = listOf(slot("b0", DayOfWeek.MONDAY, programId = "block", name = "Day 1"))

        assertThat(nextWeeklySession(listOf(block), sessions, today)).isNull()
    }

    @Test
    fun aBlock_namesItsNextSessionAndTheDayItIsDue() {
        val block = program("block", ProgramKind.SEQUENCE)
        val next = slot("b3", DayOfWeek.FRIDAY, programId = "block", name = "Day 4")
        val schedules = mapOf(
            "block" to SequenceSchedule(
                next = next,
                due = today.plusDays(3),
                isDue = false,
                length = 9,
                settled = 3,
                isComplete = false,
            ),
        )

        val result = nextSequenceSession(listOf(block), schedules)!!

        assertThat(result.weekday).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(result.name).isEqualTo("Day 4")
    }

    @Test
    fun theEarlierOfTheTwoIsWhatTheLineSays() {
        val weekly = NextSession(today.plusDays(1), DayOfWeek.WEDNESDAY, "Pull")
        val block = NextSession(today.plusDays(3), DayOfWeek.FRIDAY, "Day 4")

        assertThat(earlierOf(weekly, block)?.name).isEqualTo("Pull")
        assertThat(earlierOf(null, block)?.name).isEqualTo("Day 4")
        assertThat(earlierOf(weekly, null)?.name).isEqualTo("Pull")
        assertThat(earlierOf(null, null)).isNull()
    }
}
