package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A restored warm-up — or rung — carries no effort (ROADMAP N67, N79).
 *
 * A set that records no effort is not a record, so the rule is held at every write boundary, and the
 * import is one of them: the RPE trend averages any non-null value, so a ramp that arrived with a
 * number would read as work. N79 gives a drop or cluster set the same treatment for its own reason:
 * it is work, but it is not rated on its own, so the group's rating stays on the set it hangs off
 * rather than being copied onto every rung.
 */
class BackupWarmUpEffortTest {

    private fun set(setType: SetType, rpeHalves: Int? = null) = SetDto(
        id = "s1",
        sessionExerciseId = "se1",
        setIndex = 0,
        reps = 5,
        weightGrams = 60_000L,
        setType = setType,
        rpeHalves = rpeHalves,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun aWarmUpSet_restoresWithNoEffort() {
        assertThat(set(SetType.WARMUP, rpeHalves = 17).toEntity().rpeHalves).isNull()
    }

    @Test
    fun aWorkingSet_restoresWithItsEffort() {
        assertThat(set(SetType.NORMAL, rpeHalves = 17).toEntity().rpeHalves).isEqualTo(17)
    }

    @Test
    fun aRung_restoresWithNoEffort() {
        // ROADMAP N79: a drop or cluster set is work, but it is not rated on its own — the group's
        // rating lives on the set it hangs off — so a file carrying one brings back none, exactly as
        // a warm-up does above and for a different reason.
        assertThat(set(SetType.DROP, rpeHalves = 17).toEntity().rpeHalves).isNull()
        assertThat(set(SetType.CLUSTER, rpeHalves = 17).toEntity().rpeHalves).isNull()
    }
}
