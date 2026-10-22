package com.example.androidapp.ui.home

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
