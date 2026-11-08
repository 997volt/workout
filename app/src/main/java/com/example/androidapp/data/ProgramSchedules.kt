package com.example.androidapp.data

import com.example.androidapp.data.local.ProgramSummaryRow
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSession
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RecordedSkip
import com.example.androidapp.domain.model.SequenceSchedule
import com.example.androidapp.domain.model.sequenceSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// A sequence block's derived place (ROADMAP N175, N186): the pure schedule the editor and home read, from
// the program's authored start and the sessions that settled it.

/**
 * A sequence's schedule, from the row and the evidence (ROADMAP N175).
 *
 * The start is the authored one, or the day the program was created: the fallback N174 gives a null
 * start, and the only place it is read.
 */
internal fun scheduleOf(
    program: ProgramSummaryRow,
    slots: List<ProgramSlot>,
    sessions: List<ProgramSession>,
    skips: List<RecordedSkip>,
    zone: ZoneId,
): SequenceSchedule? {
    val start = program.startDate?.let(LocalDate::ofEpochDay)
        ?: Instant.ofEpochMilli(program.createdAt).atZone(zone).toLocalDate()
    return sequenceSchedule(
        slots = slots,
        sessions = sessions,
        skips = skips,
        start = start,
        today = LocalDate.now(zone),
    )
}

/**
 * A sequence's place, as the editor and home read it (ROADMAP N175).
 *
 * From a *finished* block there is no next session, and the run says so instead of pointing back
 * at the first: the slot it carries is the last one, which is where the block ended.
 */
internal fun sequenceRun(
    program: ProgramSummaryRow,
    slots: List<ProgramSlot>,
    sessions: List<ProgramSession>,
    skips: List<RecordedSkip>,
    zone: ZoneId,
): ProgramRun? {
    val schedule = scheduleOf(program, slots, sessions, skips, zone) ?: return null
    val next = schedule.next
    return if (next != null) {
        ProgramRun(slot = next, isAtStart = schedule.settled == 0)
    } else {
        ProgramRun(
            slot = slots.sortedBy { it.position }.last(),
            isAtStart = false,
            isComplete = true,
        )
    }
}
