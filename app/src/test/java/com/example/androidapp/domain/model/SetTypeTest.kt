package com.example.androidapp.domain.model

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
}
