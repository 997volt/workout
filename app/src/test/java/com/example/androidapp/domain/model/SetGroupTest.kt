package com.example.androidapp.domain.model

import com.example.androidapp.domain.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The group a rung hangs off, and the load it derives (ROADMAP N79).
 *
 * Pure, so the arithmetic and the refusals are covered here rather than by tapping through a workout:
 * 100 with a 20 kg drop value is 80 then 60, an assisted anchor has nothing to take a drop off, and a
 * ladder that has run out reads as no weight at all rather than as assistance.
 */
class SetGroupTest {

    private fun set(
        index: Int,
        role: SetType = SetType.NORMAL,
        weightGrams: Long? = 100_000L,
        assistanceGrams: Long? = null,
        dropValueGrams: Long? = null,
    ) = TemplateSet(
        id = "ts$index",
        templateExerciseId = "te1",
        setIndex = index,
        role = role,
        targetWeightGrams = weightGrams,
        targetAssistanceGrams = assistanceGrams,
        dropValueGrams = dropValueGrams,
    )

    /** Three working sets, the last one dropping twice by 20 kg. */
    private val dropsOnTheLastSet = listOf(
        set(0),
        set(1),
        set(2),
        set(3, role = SetType.DROP, weightGrams = null, dropValueGrams = 20_000L),
        set(4, role = SetType.DROP, weightGrams = null),
    )

    @Test
    fun aRungReadsItsAnchor_itsPlace_andTheRunsValue() {
        val run = dropsOnTheLastSet.runAt(4)

        // The anchor is the set above the run, not the first set of the exercise, and the value comes
        // from the run's first rung while this one carries none.
        assertEquals(2, run?.anchorIndex)
        assertEquals(2, run?.rung)
        assertEquals(20_000L, run?.dropValueGrams)
    }

    @Test
    fun aDropRung_isTheAnchorLessTheValueOncePerRung() {
        val anchor = Load(100_000L, 0L)

        assertEquals(Load(80_000L, 0L), rungLoad(anchor, SetType.DROP, RungRun(2, 1, 20_000L)))
        assertEquals(Load(60_000L, 0L), rungLoad(anchor, SetType.DROP, RungRun(2, 2, 20_000L)))
    }

    @Test
    fun aClusterRung_repeatsTheAnchorExactly_assistanceIncluded() {
        // Nothing is taken off, so an assisted anchor passes straight through: the same help for the
        // same reps is what a cluster is.
        val assisted = Load(0L, 20_000L)

        assertEquals(assisted, rungLoad(assisted, SetType.CLUSTER, RungRun(0, 1, null)))
        assertEquals(Load(100_000L, 0L), rungLoad(Load(100_000L, 0L), SetType.CLUSTER, RungRun(0, 1, null)))
    }

    @Test
    fun aRunWithNoValue_derivesNothing() {
        // A cluster run names no value, and a drop run written before N79 names none either.
        assertNull(rungLoad(Load(100_000L, 0L), SetType.DROP, RungRun(0, 1, null)))
        assertNull(rungLoad(Load(100_000L, 0L), SetType.DROP, RungRun(0, 1, 0L)))
    }

    @Test
    fun aLadderThatRunsOut_derivesNothing_ratherThanAssistance() {
        // 100 with a 40 kg value: the third rung would be −20, and a negative weight in this app is
        // not a small weight but 20 kg of help (N15). It reads as no derived weight instead.
        val anchor = Load(100_000L, 0L)

        assertEquals(60_000L, rungLoad(anchor, SetType.DROP, RungRun(0, 1, 40_000L))?.weightGrams)
        assertEquals(20_000L, rungLoad(anchor, SetType.DROP, RungRun(0, 2, 40_000L))?.weightGrams)
        assertNull(rungLoad(anchor, SetType.DROP, RungRun(0, 3, 40_000L)))
    }

    @Test
    fun anAnchorWithNoAddedWeight_derivesNoDrop() {
        // An assisted or bodyweight anchor has no 20 kg to take off, which is the absence the
        // progression rule already refuses to step.
        assertNull(rungLoad(Load(0L, 20_000L), SetType.DROP, RungRun(0, 1, 20_000L)))
        assertNull(rungLoad(Load(0L, 0L), SetType.DROP, RungRun(0, 1, 20_000L)))
    }

    @Test
    fun aSetThatIsNotARung_hasNoRun() {
        assertNull(dropsOnTheLastSet.runAt(2))
    }

    @Test
    fun aRunWithNothingToHangOff_hasNoRun() {
        // A plan that leads with a rung: there is nothing above it to derive from or to rate.
        assertNull(listOf(set(0, role = SetType.DROP, weightGrams = null)).runAt(0))
    }

    @Test
    fun aWarmUp_cannotAnchorARun() {
        val sets = listOf(set(0, role = SetType.WARMUP), set(1, role = SetType.DROP, weightGrams = null))

        assertNull(sets.runAt(1))
    }

    @Test
    fun aDifferentKindOfRung_breaksTheRun_ratherThanAnchoringIt() {
        // A drop run followed by a cluster run: the cluster's anchor would be a rung, which is not a
        // set that stands on its own, so the second run has no anchor and derives nothing.
        val sets = listOf(
            set(0),
            set(1, role = SetType.DROP, weightGrams = null, dropValueGrams = 20_000L),
            set(2, role = SetType.CLUSTER, weightGrams = null),
        )

        assertEquals(0, sets.runAt(1)?.anchorIndex)
        assertNull(sets.runAt(2))
    }

    @Test
    fun twoRunsInOnePlan_eachReadTheirOwnAnchor() {
        val sets = listOf(
            set(0),
            set(1, role = SetType.DROP, weightGrams = null, dropValueGrams = 10_000L),
            set(2),
            set(3, role = SetType.DROP, weightGrams = null, dropValueGrams = 25_000L),
        )

        assertEquals(RungRun(0, 1, 10_000L), sets.runAt(1))
        assertEquals(RungRun(2, 1, 25_000L), sets.runAt(3))
    }

    @Test
    fun aRunContinuesThroughRungs_butNotPastItsLast() {
        // The anchor continues into its first rung, each rung into the next, and the last rung into
        // nothing — which is what tells the rest when the group is over (ROADMAP N79).
        assertEquals(setOf(2, 3), dropsOnTheLastSet.runContinuesAfter())
        assertFalse(dropsOnTheLastSet.continuesItsRunAt(4))
        assertFalse("a set with no run after it carries on to nothing", dropsOnTheLastSet.continuesItsRunAt(1))
    }

    @Test
    fun aClusterRun_continuesTheSameWay() {
        val sets = listOf(
            set(0),
            set(1, role = SetType.CLUSTER, weightGrams = null),
            set(2, role = SetType.CLUSTER, weightGrams = null),
        )

        assertEquals(setOf(0, 1), sets.runContinuesAfter())
    }

    @Test
    fun aDifferentKindOfRung_doesNotContinueTheRun() {
        // A drop followed by a cluster is two runs, so the drop closes its own (ROADMAP N79).
        val sets = listOf(
            set(0),
            set(1, role = SetType.DROP, weightGrams = null, dropValueGrams = 20_000L),
            set(2, role = SetType.CLUSTER, weightGrams = null),
        )

        assertEquals(setOf(0), sets.runContinuesAfter())
    }

    @Test
    fun aRungIsAnchoredWhereTheSetAboveTheRunStandsOnItsOwn() {
        // ROADMAP B64: the question a picker asks before it offers a rung. A run is contiguous and its
        // rows share a role, so the anchor is the set above the run's FIRST row — which is why the
        // answer depends on where the row would sit, and why asking about an index a row would take is
        // the caller's job rather than this function's.
        assertTrue("a working set anchors one", listOf(set(0)).isAnchoredAt(1, SetType.DROP))
        assertFalse("a warm-up does not", listOf(set(0, role = SetType.WARMUP)).isAnchoredAt(1, SetType.DROP))
        assertFalse("and nothing above anchors nothing", emptyList<TemplateSet>().isAnchoredAt(0, SetType.DROP))
        assertTrue("a set that is not a rung needs no anchor", emptyList<TemplateSet>().isAnchoredAt(0, SetType.NORMAL))
    }

    @Test
    fun aRungJoinsItsOwnRun_andNotAnotherKindOfOne() {
        // The second drop of a run is anchored by the working set before the run, while a cluster at
        // that same position has no anchor: the drop between it and the working set does not stand on
        // its own (ROADMAP N79, B64).
        val sets = listOf(set(0), set(1, role = SetType.DROP, weightGrams = null, dropValueGrams = 20_000L))

        assertTrue(sets.isAnchoredAt(2, SetType.DROP))
        assertFalse(sets.isAnchoredAt(2, SetType.CLUSTER))
    }
}
