package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Correcting the day a finished session happened on (ROADMAP N266).
 *
 * The arithmetic is the whole feature: both instants travel by one offset, so the workout keeps its length, and
 * the two refusals are what stop a correction from inventing a session that has not happened.
 */
class WorkoutSessionDayTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    /** 18:00–19:30 Berlin on 5 October, as it is stored. */
    private val session = WorkoutSession(
        id = "s1",
        startedAt = Instant.parse("2026-10-05T16:00:00Z"),
        finishedAt = Instant.parse("2026-10-05T17:30:00Z"),
    )

    @Test
    fun movingToAnotherDay_keepsTheHourAndTheLength() {
        val correction = session.movedToDay(
            day = LocalDate.of(2026, 10, 2),
            zone = berlin,
            now = Instant.parse("2026-10-06T09:00:00Z"),
        )

        assertThat(correction).isInstanceOf(DayCorrection.Moved::class.java)
        val moved = (correction as DayCorrection.Moved).session
        assertThat(moved.startedAt).isEqualTo(Instant.parse("2026-10-02T16:00:00Z"))
        assertThat(moved.finishedAt).isEqualTo(Instant.parse("2026-10-02T17:30:00Z"))
        assertThat(Duration.between(moved.startedAt, moved.finishedAt))
            .isEqualTo(Duration.between(session.startedAt, session.finishedAt))
    }

    @Test
    fun anOpenSession_saysItIsStillRunning() {
        // B337: the reason is part of the answer, because the sentence the screen shows is the reason.
        val running = session.copy(finishedAt = null)

        assertThat(running.movedToDay(LocalDate.of(2026, 10, 2), berlin, Instant.parse("2026-10-06T09:00:00Z")))
            .isEqualTo(DayCorrection.StillRunning)
    }

    @Test
    fun aDayWhoseSessionWouldEndInTheFuture_saysItHasNotHappened() {
        // Moving this session to today keeps its 18:00 hour; at 09:00 that workout has not happened, and a record
        // is of the past (B212's rule). Yesterday's edition of it is fine — it ended by then.
        val now = Instant.parse("2026-10-06T07:00:00Z")

        assertThat(session.movedToDay(LocalDate.of(2026, 10, 6), berlin, now))
            .isEqualTo(DayCorrection.NotYetHappened)
        assertThat(session.movedToDay(LocalDate.of(2026, 10, 5), berlin, now))
            .isInstanceOf(DayCorrection.Moved::class.java)
    }
}
