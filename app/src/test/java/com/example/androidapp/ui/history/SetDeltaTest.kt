package com.example.androidapp.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The by-place comparison's arithmetic (ROADMAP N306, B424). */
class SetDeltaTest {

    @Test
    fun anAssistedSet_thatNeedsLessHelp_readsAsHarder() {
        // The number a set's own field states for an assisted set is its assistance: 30 -> 20 kg is +10 kg
        // of real load, so "same as last time" would be wrong.
        val previous = PastSet(reps = 5, weightGrams = 0, assistanceGrams = 30_000)
        val set = HistorySet(id = "s", reps = 5, weightGrams = 0, assistanceGrams = 20_000)

        val delta = setDelta(set, previous)!!

        assertFalse(delta.unchanged)
        assertEquals(10_000, delta.weightGrams)
    }

    @Test
    fun aWeightThatTurnedIntoAssistance_isOneSignedLoad() {
        val previous = PastSet(reps = 5, weightGrams = 40_000)
        val set = HistorySet(id = "s", reps = 5, weightGrams = 0, assistanceGrams = 10_000)

        assertEquals(-50_000, setDelta(set, previous)!!.weightGrams)
    }

    @Test
    fun anUnchangedAssistedSet_isUnchanged() {
        val previous = PastSet(reps = 5, weightGrams = 0, assistanceGrams = 30_000)
        val set = HistorySet(id = "s", reps = 5, weightGrams = 0, assistanceGrams = 30_000)

        assertTrue(setDelta(set, previous)!!.unchanged)
    }
}
