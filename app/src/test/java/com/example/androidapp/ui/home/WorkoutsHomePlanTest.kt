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
 * What home shows for today (ROADMAP P3.3, unioned by P3.12, falling back to N16's pins).
 *
 * The rule is short and easy to get subtly wrong: with anything active the union *is* the
 * schedule — an empty day is rest — and only "no active program" returns the pins.
 */
class WorkoutsHomePlanTest {

    private val program = WorkoutProgram(id = "p1", name = "Upper/Lower", isActive = true, position = 0)
    private val second = WorkoutProgram(id = "p2", name = "Conditioning", isActive = true, position = 1)

    private fun slot(
        id: String,
        weekday: DayOfWeek?,
        position: Int,
        templateId: String,
        programId: String = "p1",
    ) = ProgramSlot(
        id = id,
        programId = programId,
        templateId = templateId,
        position = position,
        weekday = weekday,
        templateName = "Template $templateId",
        exerciseCount = position + 1,
    )

    @Test
    fun withAProgramActive_theSlotsDayIsThePlan_inProgramOrder() {
        val slots = listOf(
            slot("s2", DayOfWeek.FRIDAY, position = 1, templateId = "t2"),
            slot("s1", DayOfWeek.FRIDAY, position = 0, templateId = "t1"),
            slot("s3", DayOfWeek.MONDAY, position = 2, templateId = "t3"),
        )

        val plan = todaysPlanFor(listOf(program), slots, day = DayOfWeek.FRIDAY)

        assertThat(plan.map { it.id }).containsExactly("s1", "s2").inOrder()
        assertThat(plan.map { it.templateId }).containsExactly("t1", "t2").inOrder()
    }

    @Test
    fun aDaysPlan_namesTheProgramItCameFrom() {
        // ROADMAP N127: the preview's subtitle names the program, because a day can carry slots from
        // more than one active program (P3.12) and two cards may name the same workout.
        val slots = listOf(
            slot("s1", DayOfWeek.FRIDAY, position = 0, templateId = "t1"),
            slot("s2", DayOfWeek.FRIDAY, position = 0, templateId = "t2", programId = "p2"),
        )

        val plan = todaysPlanFor(listOf(program, second), slots, day = DayOfWeek.FRIDAY)

        assertThat(plan.map { it.programName })
            .containsExactly("Upper/Lower", "Conditioning")
            .inOrder()
    }

    @Test
    fun anEmptyDay_isRest_onlyWhileAProgramIsFollowed() {
        // ROADMAP N126: the two empty days are different facts, and the surface says which. A program with
        // no slot today is a rest day on every day it schedules nothing; with nothing followed there is no
        // schedule to be resting from.
        val restDay = WorkoutsHomeUiState(
            isLoading = false,
            todaysPlan = emptyList(),
            hasActiveProgram = true,
        )
        val nothingFollowed = restDay.copy(hasActiveProgram = false)
        val planned = restDay.copy(
            todaysPlan = listOf(
                TodayPlan(id = "s1", templateId = "t1", name = "Push", exerciseCount = 3, slotId = "s1"),
            ),
        )

        assertThat(restDay.planIsRestDay).isTrue()
        assertThat(nothingFollowed.planIsRestDay).isFalse()
        assertThat(planned.planIsRestDay).isFalse()
    }

    @Test
    fun aProgramsRowIsKeyedByItsSlot_notByItsTemplate() {
        // The same template twice in a program is legal, and a list keyed by template would
        // collide the moment it happened.
        val slots = listOf(
            slot("s1", DayOfWeek.FRIDAY, position = 0, templateId = "t1"),
            slot("s2", DayOfWeek.FRIDAY, position = 1, templateId = "t1"),
        )

        val plan = todaysPlanFor(listOf(program), slots, DayOfWeek.FRIDAY)

        assertThat(plan.map { it.id }).containsExactly("s1", "s2")
        assertThat(plan.map { it.templateId }).containsExactly("t1", "t1")
    }

    @Test
    fun twoActivePrograms_areUnioned_byProgramPositionThenSlotPosition() {
        // Both programs number their slots from zero, so ordering by slot position alone would
        // interleave them; the program's authored place comes first (ROADMAP P3.12).
        val slots = listOf(
            slot("b1", DayOfWeek.FRIDAY, position = 0, templateId = "tb", programId = "p2"),
            slot("a1", DayOfWeek.FRIDAY, position = 1, templateId = "ta1", programId = "p1"),
            slot("a0", DayOfWeek.FRIDAY, position = 0, templateId = "ta0", programId = "p1"),
        )

        val plan = todaysPlanFor(listOf(program, second), slots, DayOfWeek.FRIDAY)

        assertThat(plan.map { it.id }).containsExactly("a0", "a1", "b1").inOrder()
    }

    @Test
    fun aFinishedBlock_isNamed_ratherThanCalledRest() {
        // B197: nothing is coming from a block with no session left, and the day it leaves is not a
        // rest day in the ordinary sense.
        val block = program.copy(id = "p3", name = "Peak", kind = ProgramKind.SEQUENCE)
        val schedules = mapOf(
            "p3" to SequenceSchedule(
                next = null,
                due = null,
                isDue = false,
                length = 9,
                settled = 9,
                isComplete = true,
            ),
        )

        assertThat(finishedBlockNames(listOf(block), schedules)).containsExactly("Peak")
        assertThat(finishedBlockNames(listOf(program), emptyMap())).isEmpty()
    }

    @Test
    fun aBlocksSlot_isNotADaysPlan() {
        // B194: a sequence's weekday places one session rather than scheduling a recurring one, so a
        // slot that happens to fall on Friday is not Friday's plan — its card is the block's own.
        val block = program.copy(id = "p3", name = "Peak", kind = ProgramKind.SEQUENCE)
        val slots = listOf(slot("s1", DayOfWeek.FRIDAY, position = 0, templateId = "t1", programId = "p3"))

        val plan = todaysPlanFor(listOf(block), slots, DayOfWeek.FRIDAY)

        assertThat(plan).isEmpty()
    }

    @Test
    fun aDayNoProgramSchedules_isRest_ratherThanAFallback() {
        // N56 left this as the only branch: with nothing scheduled the list is empty, and the screen
        // says so rather than reaching for a template's old pin.
        val slots = listOf(slot("s1", DayOfWeek.MONDAY, position = 0, templateId = "t1"))

        val plan = todaysPlanFor(listOf(program), slots, DayOfWeek.FRIDAY)

        assertThat(plan).isEmpty()
    }

    @Test
    fun aBuiltDeloadWeek_replacesTheSlotsOnTheDaysItKept() {
        // ROADMAP N147: a week with a plan schedules its own days, and the slots it did not keep
        // offer nothing — so a two-day deload week is a rest day on the other three.
        val slots = listOf(
            slot("s1", DayOfWeek.MONDAY, position = 0, templateId = "t1"),
            slot("s2", DayOfWeek.WEDNESDAY, position = 1, templateId = "t2"),
            slot("s3", DayOfWeek.FRIDAY, position = 2, templateId = "t3"),
        )
        val plan = DeloadWeek(
            programId = "p1",
            weekStart = LocalDate.of(2026, 10, 5),
            days = listOf(
                DeloadDay(
                    id = "d1",
                    programId = "p1",
                    templateId = "dt1",
                    templateName = "Heavy lower — deload",
                    exerciseCount = 3,
                    position = 0,
                    weekday = DayOfWeek.MONDAY,
                ),
                DeloadDay(
                    id = "d2",
                    programId = "p1",
                    templateId = "dt2",
                    templateName = "Push — deload",
                    exerciseCount = 4,
                    position = 1,
                    weekday = DayOfWeek.WEDNESDAY,
                ),
            ),
        )

        val monday = todaysPlanFor(listOf(program), slots, DayOfWeek.MONDAY, listOf(plan))
        val friday = todaysPlanFor(listOf(program), slots, DayOfWeek.FRIDAY, listOf(plan))

        assertThat(monday.map { it.id }).containsExactly("d1")
        assertThat(monday.single().templateId).isEqualTo("dt1")
        assertThat(monday.single().name).isEqualTo("Heavy lower — deload")
        assertThat(monday.single().programName).isEqualTo("Upper/Lower")
        assertThat(monday.single().slotId).isNull()
        assertThat(monday.single().isDeload).isTrue()
        assertThat(friday).isEmpty()
    }

    @Test
    fun aMarkedWeekWithNoDays_leavesTheSlotsAlone() {
        // P3.10's shape: the mark exempts the week and the slots still prescribe their workouts. The
        // creator is additive, not a change to what a mark means.
        val slots = listOf(slot("s1", DayOfWeek.MONDAY, position = 0, templateId = "t1"))
        val emptyPlan = DeloadWeek(programId = "p1", weekStart = LocalDate.of(2026, 10, 5))

        val plan = todaysPlanFor(listOf(program), slots, DayOfWeek.MONDAY, listOf(emptyPlan))

        assertThat(plan.map { it.id }).containsExactly("s1")
        assertThat(plan.single().isDeload).isFalse()
    }

    @Test
    fun oneProgramsDeload_doesNotTakeTheOtherProgramsDay() {
        val slots = listOf(
            slot("s1", DayOfWeek.MONDAY, position = 0, templateId = "t1"),
            slot("s2", DayOfWeek.MONDAY, position = 0, templateId = "t2", programId = "p2"),
        )
        val plan = DeloadWeek(
            programId = "p1",
            weekStart = LocalDate.of(2026, 10, 5),
            days = listOf(
                DeloadDay(
                    id = "d1",
                    programId = "p1",
                    templateId = "dt1",
                    templateName = "Heavy lower — deload",
                    position = 0,
                    weekday = DayOfWeek.MONDAY,
                ),
            ),
        )

        val today = todaysPlanFor(listOf(program, second), slots, DayOfWeek.MONDAY, listOf(plan))

        assertThat(today.map { it.id }).containsExactly("d1", "s2").inOrder()
        assertThat(today.map { it.isDeload }).containsExactly(true, false).inOrder()
    }

    @Test
    fun withNoProgramActive_thereIsNoDatedPlan() {
        // The rule N56 states: a day is a scheduling fact, and scheduling is what a program is for.
        val slots = listOf(slot("s1", DayOfWeek.FRIDAY, position = 0, templateId = "t1"))

        val plan = todaysPlanFor(programs = emptyList(), slots = slots, day = DayOfWeek.FRIDAY)

        assertThat(plan).isEmpty()
    }
}
