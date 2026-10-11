package com.example.androidapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `Quads · Barbell` line under an exercise (ROADMAP N2).
 *
 * Pure logic, so it is a JVM test: the point is which parts are shown, not how
 * they are laid out.
 */
class ExerciseTaxonomyTest {

    @Test
    fun bothPartsKnown_areJoinedWithTheMiddleDot() {
        assertEquals(
            "Quads · Barbell",
            taxonomySubtitle(primaryMuscle = MuscleGroup.QUADS, equipment = Equipment.BARBELL),
        )
    }

    @Test
    fun anUnspecifiedMuscle_isLeftOut_ratherThanReadingOther() {
        assertEquals(
            "Dumbbell",
            taxonomySubtitle(primaryMuscle = MuscleGroup.OTHER, equipment = Equipment.DUMBBELL),
        )
    }

    @Test
    fun unspecifiedEquipment_isLeftOut_ratherThanReadingOther() {
        assertEquals(
            "Chest",
            taxonomySubtitle(primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.OTHER),
        )
    }

    @Test
    fun aFullyUnspecifiedExercise_hasNoSubtitle() {
        // The state a custom exercise is created in: name only (N2).
        assertNull(taxonomySubtitle(primaryMuscle = MuscleGroup.OTHER, equipment = Equipment.OTHER))
    }

    @Test
    fun thePatterns_areTheNineAndNoLegacyValue() {
        // ROADMAP N96's merge, made final by N104: the four directional names are gone from the enum, and the
        // offered list is the enum itself again — 2.0 refuses the 1.x file that could have carried one before
        // the decoder, so there is nothing left for a legacy value to serve.
        assertEquals(
            listOf("PRESS", "PULL", "SQUAT", "HINGE", "LUNGE", "CARRY", "ISOLATION", "CORE", "OTHER"),
            MovementPattern.entries.map { it.name },
        )
        assertEquals(9, MovementPattern.entries.size)
        // LUNGE stays out of SQUAT: both are knee-dominant, but "have I been squatting?" has an answer and
        // lunges are not it.
        assertTrue(MovementPattern.entries.contains(MovementPattern.LUNGE))
        assertTrue(MovementPattern.entries.contains(MovementPattern.SQUAT))
    }
}
