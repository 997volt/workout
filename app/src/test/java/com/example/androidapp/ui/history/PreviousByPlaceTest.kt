package com.example.androidapp.ui.history

import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The by-place comparison after N329 added a row the previous read does not carry (N306, N335). */
class PreviousByPlaceTest {

    private fun set(id: String, type: SetType = SetType.NORMAL) = HistorySet(
        id = id,
        reps = 5,
        weightGrams = 100_000L,
        setType = type,
    )

    private fun past(reps: Int) = PastSet(reps = reps, weightGrams = 100_000L)

    @Test
    fun aFailedAttempt_drawsNoCounterpart_andDoesNotShiftTheSetsAfterIt() {
        // The previous read has no failed attempt (N329); this session still shows one. Warm-ups stay
        // on both sides, so they keep their place, while the attempt neither compares nor consumes one.
        val previous = listOf(past(10), past(5), past(3))
        val current = listOf(
            set("warmup", SetType.WARMUP),
            set("failed", SetType.FAILED_ATTEMPT),
            set("work-1"),
            set("work-2"),
        )

        assertThat(previousByPlace(current, previous)).isEqualTo(
            listOf(previous[0], null, previous[1], previous[2]),
        )
    }

    @Test
    fun anAttemptAtTheEnd_doesNotInventACounterpart() {
        val previous = listOf(past(5))
        val current = listOf(set("work"), set("failed", SetType.FAILED_ATTEMPT))

        assertThat(previousByPlace(current, previous)).isEqualTo(listOf(previous[0], null))
    }
}
