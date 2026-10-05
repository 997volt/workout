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
    fun theKeyIsBuiltFromTheEnumNames_notOrdinals() {
        // Enums are stored by name (ROADMAP N63), and the key is what a test tag and a callback use.
        assertEquals("LOWER_BACK_CENTRE", jointSiteKey(Joint.LOWER_BACK, Side.CENTRE))
        assertEquals("ANKLE_LEFT", jointSiteKey(Joint.ANKLE, Side.LEFT))
    }
}
