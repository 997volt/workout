package com.example.androidapp.data

import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.TrainingSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/** Which session stood for each block day when a template appears more than once (N340). */
class TrainingReportDayTest {

    private fun slot(position: Int, templateId: String) = ProgramSlot(
        id = "slot-$position",
        programId = "program",
        templateId = templateId,
        position = position,
        weekday = null,
    )

    private fun session(id: String, templateId: String, at: Long) = TrainingSession(
        id = id,
        workout = "Workout $templateId",
        templateId = templateId,
        startedAt = Instant.parse("2026-10-05T12:00:00Z").plusSeconds(at),
        finishedAt = Instant.parse("2026-10-05T13:00:00Z").plusSeconds(at),
    )

    @Test
    fun aRepeatedTemplate_takesOneSessionPerDay_andLaterSessionsStayUnplanned() {
        // A/B/A is an ordinary block. The old first-match lookup gave both A days session 1; the walk
        // gives day 3 the second A, and the extra A trained after the block stays unassigned.
        val slots = listOf(slot(0, "A"), slot(1, "B"), slot(2, "A"))
        val sessions = listOf(
            session("s1", "A", 0),
            session("s2", "B", 60),
            session("s3", "A", 120),
            session("after", "A", 180),
        )

        assertThat(sessionsByDay(slots, sessions)).isEqualTo(
            mapOf(0 to "s1", 1 to "s2", 2 to "s3"),
        )
    }

    @Test
    fun anUntrainedDay_isAbsent_andAWorkoutTheBlockDoesNotNameSettlesNothing() {
        val slots = listOf(slot(0, "A"), slot(1, "B"))
        val sessions = listOf(session("s1", "A", 0), session("extra", "C", 60))

        assertThat(sessionsByDay(slots, sessions)).isEqualTo(mapOf(0 to "s1"))
    }
}
