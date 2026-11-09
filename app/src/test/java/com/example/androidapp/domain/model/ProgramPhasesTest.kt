package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Which authored week a weekly program is training, and which slots that week prescribes
 * (ROADMAP N292).
 *
 * Pure, because the rule is the feature: the app derives the week from the calendar and never
 * advances a load, the same line N22 draws for the rest of the app.
 */
class ProgramPhasesTest {

    // A Monday, so "one whole week later" is a date a reader can check by eye.
    private val monday = LocalDate.of(2026, 10, 5)
    private val utc = ZoneOffset.UTC

    private fun program(
        startDate: LocalDate? = monday,
        createdAtMillis: Long = 0L,
        kind: ProgramKind = ProgramKind.WEEKLY,
    ) = WorkoutProgram(
        id = "p1",
        name = "Block",
        isActive = true,
        kind = kind,
        startDate = startDate,
        createdAtMillis = createdAtMillis,
    )

    private fun slot(id: String, weekday: DayOfWeek?, week: Int?, position: Int = 0) = ProgramSlot(
        id = id,
        programId = "p1",
        templateId = "t_$id",
        position = position,
        weekday = weekday,
        phaseWeek = week,
        templateName = id,
    )

    private val phased = listOf(
        slot("w1-mon", DayOfWeek.MONDAY, week = 1),
        slot("w2-wed", DayOfWeek.WEDNESDAY, week = 2, position = 0),
        slot("w3-fri", DayOfWeek.FRIDAY, week = 3, position = 0),
    )

    @Test
    fun aProgramWithNoPhases_readsEverySlotAsItAlwaysDid() {
        val slots = listOf(
            slot("mon", DayOfWeek.MONDAY, week = null),
            slot("wed", DayOfWeek.WEDNESDAY, week = null),
        )

        assertThat(ProgramPhases.currentWeek(program(startDate = null), slots, monday, utc)).isNull()
        assertThat(ProgramPhases.currentSlots(program(startDate = null), slots, monday, utc))
            .containsExactlyElementsIn(slots)
    }

    @Test
    fun theWeekIsTheWholeWeeksSinceTheMondayTheProgramStartedOn() {
        assertThat(ProgramPhases.currentWeek(program(), phased, monday, utc)).isEqualTo(1)
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusWeeks(1), utc)).isEqualTo(2)
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusWeeks(2), utc)).isEqualTo(3)
        // A weekly program repeats forever, so the authored weeks do too (N174, N292).
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusWeeks(3), utc)).isEqualTo(1)
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusWeeks(4), utc)).isEqualTo(2)
    }

    @Test
    fun aWeekIsTheWholeOfItsMondayToSunday() {
        // The phase changes on the Monday, not seven days after the start's own weekday.
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusDays(6), utc)).isEqualTo(1)
        assertThat(ProgramPhases.currentWeek(program(), phased, monday.plusDays(7), utc)).isEqualTo(2)
    }

    @Test
    fun aStartInTheFuture_isWeekOne_ratherThanAWrappedLastWeek() {
        // A program scheduled to begin next Monday is not already at its end (N175's rule).
        assertThat(ProgramPhases.currentWeek(program(startDate = monday.plusWeeks(2)), phased, monday, utc))
            .isEqualTo(1)
    }

    @Test
    fun aMidweekStart_readsFromThatWeeksMonday() {
        val wednesday = monday.plusDays(2)

        assertThat(ProgramPhases.currentWeek(program(startDate = wednesday), phased, wednesday, utc))
            .isEqualTo(1)
        assertThat(ProgramPhases.currentWeek(program(startDate = wednesday), phased, monday.plusWeeks(1), utc))
            .isEqualTo(2)
    }

    @Test
    fun withoutAnAuthoredStart_theCreationWeekAnchorsIt() {
        // The fallback a sequence already applies (N175): the week the program was created.
        val created = monday.plusDays(2).atStartOfDay(utc).toInstant().toEpochMilli()

        val createdProgram = program(startDate = null, createdAtMillis = created)

        assertThat(ProgramPhases.currentWeek(createdProgram, phased, monday.plusWeeks(1), utc)).isEqualTo(2)
    }

    @Test
    fun onlyTheCurrentWeeksSlotsAreTodaysSchedule() {
        val secondWeek = monday.plusWeeks(1)

        assertThat(ProgramPhases.currentSlots(program(), phased, secondWeek, utc).map { it.id })
            .containsExactly("w2-wed")
        assertThat(ProgramPhases.currentSlots(program(), phased, monday, utc).map { it.id })
            .containsExactly("w1-mon")
    }

    @Test
    fun aGapBetweenAuthoredWeeks_isAWeekThatSchedulesNothing() {
        // Weeks 1 and 3 with nothing in 2: the rotation still passes through week 2, and it is a
        // rest week rather than a reason to renumber week 3 (N292).
        val gapped = listOf(
            slot("w1", DayOfWeek.MONDAY, week = 1),
            slot("w3", DayOfWeek.FRIDAY, week = 3, position = 0),
        )

        assertThat(ProgramPhases.currentWeek(program(), gapped, monday.plusWeeks(1), utc)).isEqualTo(2)
        assertThat(ProgramPhases.currentSlots(program(), gapped, monday.plusWeeks(1), utc)).isEmpty()
        assertThat(ProgramPhases.weeks(gapped)).containsExactly(1, 3).inOrder()
    }

    @Test
    fun aBlock_ignoresPhaseWeeks_becauseItsWeekdayPlacesOneSession() {
        val block = program(kind = ProgramKind.SEQUENCE)

        assertThat(ProgramPhases.currentSlots(block, phased, monday.plusWeeks(1), utc))
            .containsExactlyElementsIn(phased)
    }

    @Test
    fun theEditorShowsEveryDayUnderWeekOne_ofAProgramWithoutPhases() {
        val slots = listOf(slot("mon", DayOfWeek.MONDAY, week = null))

        assertThat(ProgramPhases.slotsOf(slots, week = 1)).containsExactlyElementsIn(slots)
        assertThat(ProgramPhases.slotsOf(slots, week = 2)).isEmpty()
    }

    @Test
    fun theEditorShowsOnlyTheSelectedWeek_ofAPhasedProgram() {
        assertThat(ProgramPhases.slotsOf(phased, week = 2).map { it.id }).containsExactly("w2-wed")
        assertThat(ProgramPhases.slotsOf(phased, week = 4)).isEmpty()
    }
}
