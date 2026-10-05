package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A restored warm-up carries no effort (ROADMAP N67).
 *
 * Migration 28→29 clears the RPE off the warm-ups already on disk, but a backup file is the other
 * way in: an export written before the rule carries a ramp's number, and a restore that kept it
 * would put back exactly what the migration removed — and the RPE trend averages any non-null value,
 * so a ramp would read as work. The same rule therefore holds at the import boundary, for the logged
 * set and for a plan's target alike.
 */
class BackupWarmUpEffortTest {

    private fun set(setType: SetType, rpeHalves: Int? = null, rpe: Int? = null) = SetDto(
        id = "s1",
        sessionExerciseId = "se1",
        setIndex = 0,
        reps = 5,
        weightGrams = 60_000L,
        setType = setType,
        rpeHalves = rpeHalves,
        rpe = rpe,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun aWarmUpSet_restoresWithNoEffort() {
        assertThat(set(SetType.WARMUP, rpeHalves = 17).toEntity().rpeHalves).isNull()
        // The pre-N6 whole-number field cannot sneak one in either.
        assertThat(set(SetType.WARMUP, rpe = 8).toEntity().rpeHalves).isNull()
    }

    @Test
    fun aWorkingSet_restoresWithItsEffort() {
        assertThat(set(SetType.NORMAL, rpeHalves = 17).toEntity().rpeHalves).isEqualTo(17)
        // The legacy whole-number field still converts for a role that records one.
        assertThat(set(SetType.DROP, rpe = 8).toEntity().rpeHalves)
            .isEqualTo(8 * Rpe.HALVES_PER_POINT)
    }

    private fun planSet(
        role: SetType,
        targetRpeHalves: Int? = null,
        targetRpe: Int? = null,
    ) = TemplateSetDto(
        id = "ts1",
        templateExerciseId = "te1",
        setIndex = 0,
        role = role,
        targetRpeHalves = targetRpeHalves,
        targetRpe = targetRpe,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun aPlannedWarmUp_restoresWithNoTargetEffort() {
        assertThat(planSet(SetType.WARMUP, targetRpeHalves = 16).toEntity().targetRpeHalves).isNull()
        assertThat(planSet(SetType.WARMUP, targetRpe = 8).toEntity().targetRpeHalves).isNull()
    }

    @Test
    fun aPlannedWorkingSet_restoresWithItsTarget() {
        assertThat(planSet(SetType.NORMAL, targetRpeHalves = 16).toEntity().targetRpeHalves)
            .isEqualTo(16)
    }
}
