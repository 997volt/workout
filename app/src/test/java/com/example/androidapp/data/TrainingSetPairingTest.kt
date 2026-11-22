package com.example.androidapp.data

import com.example.androidapp.data.local.SessionPlanSetEntity
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The report's plan/performance pairing (N356).
 *
 * The two sides are stored in different index spaces: a logged set takes `max + 1` (which counts a
 * soft-deleted row), the snapshot keeps the template's number, and a document plan may start at one.
 * The pairing rule is the workout screen's — plan place — so these tests are named for the cases that
 * broke the old stored-index join.
 */
class TrainingSetPairingTest {

    @Test
    fun aNormalSession_pairsEveryPrescriptionWithItsPerformance() {
        val rows = pairedTrainingSets(
            prescriptions = listOf(plan(0), plan(1, reps = 8), plan(2, reps = 10)),
            performed = listOf(done(0, reps = 5), done(1, reps = 8), done(2, reps = 10)),
            unit = WeightUnit.KILOGRAMS,
        )

        assertThat(rows).hasSize(3)
        assertThat(rows.map { it.performed }).containsExactly(true, true, true)
        assertThat(rows.map { it.plannedReps }).containsExactly(5, 8, 10).inOrder()
        assertThat(rows.map { it.reps }).containsExactly(5, 8, 10).inOrder()
    }

    @Test
    fun aDeletedAndUndoneSet_stillAnswersItsPrescription() {
        // Delete the middle set, press Undo: it comes back at max + 1 (3), while the plan still reads
        // 0, 1, 2. Every prescribed row was performed, so none is "planned, not done".
        val rows = pairedTrainingSets(
            prescriptions = listOf(plan(0), plan(1), plan(2)),
            performed = listOf(done(0), done(2), done(3)),
            unit = WeightUnit.KILOGRAMS,
        )

        assertThat(rows).hasSize(3)
        assertThat(rows.all { it.performed }).isTrue()
        assertThat(rows.count { !it.performed }).isEqualTo(0)
        assertThat(rows.map { it.plannedReps }).containsExactly(5, 5, 5)
        assertThat(rows.last().weightGrams).isEqualTo(100_000L)
    }

    @Test
    fun aOneBasedPlan_isPairedByPlace() {
        // A program document may number its sets from one; the performed rows still start at zero.
        val rows = pairedTrainingSets(
            prescriptions = listOf(plan(1), plan(2)),
            performed = listOf(done(0), done(1)),
            unit = WeightUnit.KILOGRAMS,
        )

        assertThat(rows).hasSize(2)
        assertThat(rows.all { it.performed }).isTrue()
        assertThat(rows.map { it.plannedReps }).containsExactly(5, 5)
    }

    @Test
    fun aFailedAttempt_isPerformed_andConsumesNoPlanPlace() {
        val failed = done(1, setType = SetType.FAILED_ATTEMPT, reps = 0)
        val rows = pairedTrainingSets(
            prescriptions = listOf(plan(0), plan(1)),
            performed = listOf(done(0), failed, done(2)),
            unit = WeightUnit.KILOGRAMS,
        )

        assertThat(rows).hasSize(3)
        assertThat(rows[1].performed).isTrue()
        assertThat(rows[1].role).isEqualTo(SetType.FAILED_ATTEMPT)
        assertThat(rows[1].plannedRole).isNull()
        assertThat(rows[2].role).isEqualTo(SetType.NORMAL)
        assertThat(rows[2].plannedRole).isEqualTo(SetType.NORMAL)
        assertThat(rows.count { !it.performed }).isEqualTo(0)
    }

    @Test
    fun aPrescriptionNothingAnswered_isNotDone() {
        val rows = pairedTrainingSets(
            prescriptions = listOf(plan(0), plan(1)),
            performed = listOf(done(0)),
            unit = WeightUnit.KILOGRAMS,
        )

        assertThat(rows).hasSize(2)
        assertThat(rows[0].performed).isTrue()
        assertThat(rows[1].performed).isFalse()
        assertThat(rows[1].plannedRole).isEqualTo(SetType.NORMAL)
        assertThat(rows[1].plannedReps).isEqualTo(5)
    }

    private fun plan(index: Int, reps: Int = 5): SessionPlanSetEntity = SessionPlanSetEntity(
        sessionExerciseId = "se-1",
        setIndex = index,
        role = SetType.NORMAL,
        targetWeightGrams = 100_000L,
        targetAssistanceGrams = null,
        targetRepsMin = reps,
        targetRepsMax = reps,
        targetRepsCurrent = null,
        targetDurationSeconds = null,
        targetRpeHalves = 16,
    )

    private fun done(index: Int, setType: SetType = SetType.NORMAL, reps: Int = 5): SetEntry = SetEntry(
        id = "set-$index-${setType.name}",
        sessionExerciseId = "se-1",
        setIndex = index,
        reps = reps,
        weightGrams = 100_000L,
        setType = setType,
    )
}
