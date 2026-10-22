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
 * What home does with a block's next session (ROADMAP N176).
 *
 * The card exists only when the session is due or overdue, and it carries its place in the block and
 * the day it was due — the three things a weekly card has no use for. Pure, like `todaysPlanFor`.
 */
class SequenceTodayPlansTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 7)

    private val program = WorkoutProgram(
        id = "pgm",
        name = "Peak",
        kind = ProgramKind.SEQUENCE,
    )

    private fun slot(id: String, position: Int, weekday: DayOfWeek?) = ProgramSlot(
        id = id,
        programId = "pgm",
        templateId = "tpl_$id",
        position = position,
        weekday = weekday,
        templateName = "Session $id",
        exerciseCount = 3,
    )

    private fun schedule(
        next: ProgramSlot?,
        due: LocalDate?,
        dueToday: Boolean,
        settled: Int = 0,
        length: Int = 3,
    ) = SequenceSchedule(
        next = next,
        due = due,
        isDue = dueToday,
        length = length,
        settled = settled,
        isComplete = next == null,
    )

    @Test
    fun aSessionDueToday_isOneCardWithItsPlaceInTheBlock() {
        val next = slot("b", 1, DayOfWeek.FRIDAY)

        val cards = sequenceTodayPlans(
            programs = listOf(program),
            schedules = mapOf("pgm" to schedule(next, today, dueToday = true, settled = 1)),
            today = today,
        )

        assertThat(cards).hasSize(1)
        val card = cards.single()
        assertThat(card.id).isEqualTo("b")
        assertThat(card.templateId).isEqualTo("tpl_b")
        assertThat(card.slotId).isEqualTo("b")
        assertThat(card.isSequence).isTrue()
        assertThat(card.sequencePosition).isEqualTo(2)
        assertThat(card.sequenceLength).isEqualTo(3)
        assertThat(card.dueDate).isEqualTo(today)
        assertThat(card.isOverdue).isFalse()
    }

    @Test
    fun aSessionThatIsNotDueYet_hasNoCard() {
        val cards = sequenceTodayPlans(
            programs = listOf(program),
            schedules = mapOf("pgm" to schedule(slot("a", 0, DayOfWeek.MONDAY), today.plusDays(2), false)),
            today = today,
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun aFinishedBlock_hasNoCard() {
        val cards = sequenceTodayPlans(
            programs = listOf(program),
            schedules = mapOf("pgm" to schedule(null, null, dueToday = false, settled = 3)),
            today = today,
        )

        assertThat(cards).isEmpty()
    }

    @Test
    fun aSessionBehindItsDay_isOverdue_andStillOffered() {
        val next = slot("a", 0, DayOfWeek.MONDAY)

        val cards = sequenceTodayPlans(
            programs = listOf(program),
            schedules = mapOf("pgm" to schedule(next, today.minusDays(2), dueToday = true)),
            today = today,
        )

        assertThat(cards.single().isOverdue).isTrue()
    }

    @Test
    fun aBlockWithNoSchedule_yet_offersNothing() {
        val cards = sequenceTodayPlans(
            programs = listOf(program),
            schedules = emptyMap(),
            today = today,
        )

        assertThat(cards).isEmpty()
    }
}
