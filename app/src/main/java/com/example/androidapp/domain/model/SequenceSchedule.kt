package com.example.androidapp.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Where a finite program is: the next session, when it is due, and whether the block is over
 * (ROADMAP N175).
 *
 * Derived, never stored. The same rule `programRun` follows: what has been done is what moves the
 * schedule, so editing the program or training a session late re-derives the answer instead of
 * leaving a cursor pointing at a day that has passed.
 */
data class SequenceSchedule(
    /** The session to do next, or null once the block is over. */
    val next: ProgramSlot?,
    /** The day [next] is due, or null when there is nothing left. */
    val due: LocalDate?,
    /** True when [due] is today or already behind — what home offers a card for. */
    val isDue: Boolean,
    /** How many sessions the block has. */
    val length: Int,
    /** How many are done or consciously skipped: the "4 of 9" a screen says. */
    val settled: Int,
    /** True when every session is done or skipped. */
    val isComplete: Boolean,
) {
    /** The one-based place the block is at, for "Day 4 of 9" — the next session's own number. */
    val position: Int get() = (settled + 1).coerceAtMost(length)
}

/**
 * A sequence's next session and its due date, from what was done (ROADMAP N175).
 *
 * **A weekday places one session** in a sequence rather than scheduling a recurring one: the session
 * is due on the first such weekday after the last thing done, or at the start when nothing has been.
 * A slot with **no** weekday is due the next day, so a "whenever" session still works.
 *
 * [start] is the authored start date, or the day the program was created: it anchors the *first*
 * due date and nothing else. A block that starts next Monday is not overdue on Saturday, and a block
 * whose start has passed is due rather than late — the past belongs to the sessions.
 *
 * Sessions are matched to slots **in order, by template**, the way `programRun`'s fallback does: a
 * session naming a template no slot names (a workout started by hand) moves nothing.
 */
fun sequenceSchedule(
    slots: List<ProgramSlot>,
    sessions: List<ProgramSession>,
    skips: List<RecordedSkip>,
    start: LocalDate,
    today: LocalDate,
): SequenceSchedule? {
    if (slots.isEmpty()) return null
    val ordered = slots.sortedBy { it.position }
    val walk = walk(ordered, sessions, skips)
    val next = walk.next
    val due = next?.let { dueOf(it.weekday, walk.last ?: start, strictlyAfter = walk.last != null) }
    return SequenceSchedule(
        next = next,
        due = due,
        isDue = due != null && !due.isAfter(today),
        length = ordered.size,
        settled = walk.settled,
        isComplete = next == null,
    )
}

/** How far the block got, before a due date is put on it. */
private data class Walk(val next: ProgramSlot?, val last: LocalDate?, val settled: Int)

/** The first session with neither a finished session nor a skip, and the day the last one settled. */
private fun walk(
    ordered: List<ProgramSlot>,
    sessions: List<ProgramSession>,
    skips: List<RecordedSkip>,
): Walk {
    val skipped = skips.groupBy { it.slotId }
    val remaining = sessions.sortedBy { it.date }.toMutableList()
    var last: LocalDate? = null
    var settled = 0
    var next: ProgramSlot? = null
    for (slot in ordered) {
        if (next != null) break
        val session = remaining.firstOrNull { it.templateId == slot.templateId }
        val event = when {
            session != null -> session.date.also { remaining.remove(session) }
            else -> skipped[slot.id]?.maxOfOrNull { it.weekStart }
        }
        if (event == null) {
            next = slot
        } else {
            settled += 1
            last = if (last == null) event else maxOf(last, event)
        }
    }
    return Walk(next, last, settled)
}

private const val DAYS_IN_WEEK = 7

/**
 * The first day at or after [from] (or strictly after it) that falls on [weekday], or [from]'s next
 * day when the slot names none.
 */
private fun dueOf(weekday: DayOfWeek?, from: LocalDate, strictlyAfter: Boolean): LocalDate {
    val earliest = if (strictlyAfter) from.plusDays(1) else from
    if (weekday == null) return earliest
    val shift = (weekday.value - earliest.dayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    return earliest.plusDays(shift.toLong())
}
