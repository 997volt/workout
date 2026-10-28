package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The zone a session was performed in, when the stored offset is nonsense (ROADMAP B282).
 *
 * `zoneIdOrNull` promises null for "nothing to say", but `ZoneOffset.ofTotalSeconds` throws outside ±18:00,
 * and a backup carries the column as it stands. One such row killed the history flow rather than failing the
 * import, so the helper is total now.
 */
class SessionZoneTest {

    @Test
    fun anOffsetOutsideTheRealOnes_isNullRatherThanAThrow() {
        val session = WorkoutSession(
            id = "s1",
            startedAt = java.time.Instant.ofEpochMilli(1),
            finishedAt = java.time.Instant.ofEpochMilli(2),
            zoneOffsetMinutes = 2_000,
        )

        assertThat(session.zoneIdOrNull()).isNull()
    }

    @Test
    fun aRealOffset_isTheZoneItStates() {
        val session = WorkoutSession(
            id = "s1",
            startedAt = java.time.Instant.ofEpochMilli(1),
            finishedAt = java.time.Instant.ofEpochMilli(2),
            zoneOffsetMinutes = 120,
        )

        assertThat(session.zoneIdOrNull()).isEqualTo(java.time.ZoneOffset.ofHours(2))
    }
}
