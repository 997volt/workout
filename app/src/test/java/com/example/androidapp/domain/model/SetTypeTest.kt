package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The role rules, on the role (ROADMAP N67, N79).
 *
 * Both of these are read from several places — the write boundary, the editors, the rows, the record
 * scan — so they are asserted here rather than through whichever screen happens to read them first.
 * A rule that lives on the enum is worth a test that asks the enum.
 */
class SetTypeTest {

    @Test
    fun onlyDropAndCluster_areRungs() {
        // A rung hangs off the set before it. Everything else is a set of its own, including the
        // warm-up, which has its own rule rather than this one (N79).
        assertTrue(SetType.DROP.isRung)
        assertTrue(SetType.CLUSTER.isRung)
        assertFalse(SetType.NORMAL.isRung)
        assertFalse(SetType.WARMUP.isRung)
        assertFalse(SetType.TOP_SET.isRung)
        assertFalse(SetType.FAILURE.isRung)
        assertFalse(SetType.FAILED_ATTEMPT.isRung)
    }

    @Test
    fun aWarmUpAndARung_recordNoEffort() {
        // The two exclusions have different reasons and the same answer: a warm-up is preparation,
        // a rung is work that is not rated on its own (N67, N79).
        assertFalse(SetType.WARMUP.recordsEffort)
        assertFalse(SetType.DROP.recordsEffort)
        assertFalse(SetType.CLUSTER.recordsEffort)
    }

    @Test
    fun everyOtherRole_recordsAnEffort() {
        assertTrue(SetType.NORMAL.recordsEffort)
        assertTrue(SetType.TOP_SET.recordsEffort)
        assertTrue(SetType.FAILURE.recordsEffort)
    }

    @Test
    fun aFailedAttempt_isNotWork_andRecordsNoEffort() {
        // N329: the role says a load was tried and not completed. It must not become a working set, a
        // plan set, a record or a 1RM estimate.
        assertFalse(SetType.FAILED_ATTEMPT.isWork)
        assertFalse(SetType.FAILED_ATTEMPT.recordsEffort)
        assertTrue(SetType.FAILURE.isWork)
    }

    @Test
    fun theFailureRole_readsAsTakenToFailure() {
        // N329 disambiguates the old label, because the role is a set that ended in failure, not an attempt
        // that produced no rep.
        assertThat(SetType.FAILURE.label).isEqualTo("To failure")
        assertThat(SetType.FAILED_ATTEMPT.label).isEqualTo("Failed attempt")
    }
}
