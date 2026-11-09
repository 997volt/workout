package com.example.androidapp.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The authored phase weeks of a weekly program (ROADMAP N292).
 *
 * A weekly program used to be one week that repeats forever. It may now author several numbered
 * weeks — week 1's days, week 2's days, week 3's days — and each one is the lifter's own
 * prescription. The app chooses **which authored week is current** from the calendar; it never
 * changes a load, which is N22's line: the week is writing, not coaching.
 *
 * **The rotation is cyclic and starts from the program's anchor.** The anchor is the week of the
 * authored start date, or — when none is authored — the week the program was created, the fallback
 * a sequence already applies (N175). Whole weeks elapsed since that Monday pick the week:
 * `elapsed % count + 1`. A program with three weeks therefore runs 1, 2, 3, 1, 2, 3… A weekly
 * program repeats forever (N174), and its authored weeks repeat with it; there is no "last week"
 * that would need a completion state on a program whose kind says it does not end.
 *
 * **A start in the future is week 1.** A program scheduled to begin next Monday is not already in
 * its second week: elapsed time below zero clamps to the first week, the way a block that starts
 * next week is due rather than late (N175).
 *
 * **A week is Monday-start**, the calendar rule the rest of the schedule uses (P3.3). A program
 * started on a Wednesday has its first week's Monday as the anchor, so the following Monday is
 * week 2.
 *
 * **No phases is the old program.** When no slot names a week — every program written before this
 * existed — every slot is the schedule and nothing changes. The readers only ever ask this object,
 * so the old shape cannot drift into a different answer on one screen.
 */
object ProgramPhases {

    /** The first authored week, and the week a program without phases reads as. */
    const val FIRST_WEEK: Int = 1

    /**
     * The weeks this program authors, ascending, or empty when it has no phases.
     *
     * A week exists because a day names it: a phase with no prescribed day schedules nothing, so
     * there is no empty week to carry. A gap is possible in a hand-written document (weeks 1 and 3
     * with nothing in 2), and the gap is then a week that schedules nothing — a rest week.
     */
    fun weeks(slots: List<ProgramSlot>): List<Int> =
        slots.mapNotNull { it.phaseWeek }.filter { it >= FIRST_WEEK }.distinct().sorted()

    /**
     * The authored week [today] falls in, or null when the program authors no phases.
     *
     * The count is the highest authored week, so a gap is still a week the rotation passes through
     * rather than a reason to renumber the weeks after it.
     */
    fun currentWeek(
        program: WorkoutProgram,
        slots: List<ProgramSlot>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int? {
        val count = weeks(slots).lastOrNull() ?: return null
        val elapsed = ChronoUnit.WEEKS.between(anchorWeek(program, zone), ProgramSchedule.weekStartOf(today))
        return if (elapsed <= 0L) FIRST_WEEK else (elapsed % count).toInt() + FIRST_WEEK
    }

    /**
     * The slots that prescribe today, for any reader of the current schedule (ROADMAP N292).
     *
     * A program with no phases returns every slot, which is the behaviour every screen had before
     * phases existed. A block returns every slot too: its weekday places one session in an order,
     * and a phase week is a weekly program's idea (N174, N176). Asking this one function is what
     * keeps home, adherence, the missed-day question and the run's marker from disagreeing.
     */
    fun currentSlots(
        program: WorkoutProgram,
        slots: List<ProgramSlot>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ProgramSlot> {
        // A block has no authored weeks, and a program without phases has no current one: both read
        // every slot, which is the answer every screen gave before phases existed.
        val week = if (program.kind == ProgramKind.WEEKLY) {
            currentWeek(program, slots, today, zone)
        } else {
            null
        }
        return if (week == null) slots else slots.filter { it.phaseWeek == week }
    }

    /**
     * The slots the editor shows while [week] is selected (ROADMAP N292).
     *
     * A program with no phases shows all its days under week 1, because that is the implicit week
     * they already are; a phased program shows only that week's days. Selecting a week the program
     * has not authored yet shows nothing, which is where its first day is added.
     */
    fun slotsOf(slots: List<ProgramSlot>, week: Int): List<ProgramSlot> =
        if (weeks(slots).isEmpty()) {
            if (week == FIRST_WEEK) slots else emptyList()
        } else {
            slots.filter { it.phaseWeek == week }
        }

    /**
     * The Monday the rotation is measured from (ROADMAP N292).
     *
     * The authored start's week, or the creation week when none is authored. Taken in the reader's
     * zone, the same rule a session's week follows (N25).
     */
    fun anchorWeek(program: WorkoutProgram, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val day = program.startDate
            ?: Instant.ofEpochMilli(program.createdAtMillis).atZone(zone).toLocalDate()
        return ProgramSchedule.weekStartOf(day)
    }
}
