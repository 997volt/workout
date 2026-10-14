package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The joints a rating may name (ROADMAP N63).
 *
 * This is the half of the change a JVM test can hold: which sides each joint offers, that left and
 * right are distinct sites, and what a picked joint is called. The storage round trip and the trend
 * SQL need a database, so they are covered on device.
 */
class JointPainTest {

    @Test
    fun aPairedJoint_offersBothSides_andACentralOneOnlyTheCentre() {
        Joint.entries.filter { it in PAIRED_JOINTS }.forEach { joint ->
            assertEquals("$joint is paired", listOf(Side.LEFT, Side.RIGHT), joint.offeredSides())
        }
        assertEquals(listOf(Side.CENTRE), Joint.NECK.offeredSides())
        assertEquals(listOf(Side.CENTRE), Joint.LOWER_BACK.offeredSides())
    }

    @Test
    fun everySiteHasADistinctKey_soLeftAndRightCannotCollide() {
        val keys = JOINT_SITES.map { (joint, side) -> jointSiteKey(joint, side) }

        assertEquals("no site is offered twice", keys.size, keys.toSet().size)
        assertThat(keys).contains("KNEE_LEFT")
        assertThat(keys).contains("KNEE_RIGHT")
        assertEquals("a central joint has one site", 1, JOINT_SITES.count { it.first == Joint.NECK })
        assertThat(JOINT_SITES).hasSize(PAIRED_JOINTS.size * 2 + (Joint.entries.size - PAIRED_JOINTS.size))
    }

    @Test
    fun aPickedJoint_isNamedWithItsSide_butACentralOneIsNot() {
        assertEquals("Left knee", JointPain(Joint.KNEE, Side.LEFT, 6).label)
        assertEquals("Right shoulder", JointPain(Joint.SHOULDER, Side.RIGHT, 2).label)
        // "Centre neck" would be a sentence the body does not say.
        assertEquals("Neck", JointPain(Joint.NECK, Side.CENTRE, 3).label)
        assertEquals("Lower back", JointPain(Joint.LOWER_BACK, Side.CENTRE, 3).label)
    }

    @Test
    fun oneSitesReading_isTheSitesOwn_orNullWhenTheSessionDidNotNameIt() {
        // ROADMAP N130. The overall figure is 9 and neither knee is a 9: a series per site reads the site,
        // and a site the workout did not name is a gap rather than the overall worst or a zero.
        val point = TrendPoint(
            startedAt = java.time.Instant.ofEpochMilli(1_000L),
            averageJointPain = 9.0,
            jointPainBySite = mapOf(
                jointSiteKey(Joint.KNEE, Side.LEFT) to 9.0,
                jointSiteKey(Joint.KNEE, Side.RIGHT) to 4.0,
            ),
        )

        assertEquals(9.0, point.jointPainAt(Joint.KNEE, Side.LEFT)!!, 0.0001)
        assertEquals(4.0, point.jointPainAt(Joint.KNEE, Side.RIGHT)!!, 0.0001)
        assertThat(point.jointPainAt(Joint.HIP, Side.LEFT)).isNull()
    }

    @Test
    fun theKeyIsBuiltFromTheEnumNames_notOrdinals() {
        // Enums are stored by name (ROADMAP N63), and the key is what a test tag and a callback use.
        assertEquals("LOWER_BACK_CENTRE", jointSiteKey(Joint.LOWER_BACK, Side.CENTRE))
        assertEquals("ANKLE_LEFT", jointSiteKey(Joint.ANKLE, Side.LEFT))
    }
}
