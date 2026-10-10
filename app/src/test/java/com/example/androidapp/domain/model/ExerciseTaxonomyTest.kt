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
    fun aRetiredPattern_stillResolves_andIsNeverOffered() {
        // ROADMAP N96's rule, which N75's retired `BACK` already follows: every pattern is stored by name, so
        // deleting the four the merge replaced would make an older row — and an export written before it —
        // throw on read. They resolve, they keep the label those rows said, and no picker offers them.
        assertEquals(
            "Horizontal push",
            MovementPattern.valueOf("HORIZONTAL_PUSH").label,
        )
        assertTrue(
            "the retired four are read, never offered",
            SELECTABLE_MOVEMENT_PATTERNS.none {
                it == MovementPattern.HORIZONTAL_PUSH ||
                    it == MovementPattern.VERTICAL_PUSH ||
                    it == MovementPattern.HORIZONTAL_PULL ||
                    it == MovementPattern.VERTICAL_PULL
            },
        )
    }

    @Test
    fun theSelectablePatterns_areTheNineThatSurvived() {
        // The merge is the feature: press and pull are one question each (N96).
        assertEquals(9, SELECTABLE_MOVEMENT_PATTERNS.size)
        assertTrue(SELECTABLE_MOVEMENT_PATTERNS.contains(MovementPattern.PRESS))
        assertTrue(SELECTABLE_MOVEMENT_PATTERNS.contains(MovementPattern.PULL))
        // LUNGE stays out of SQUAT: both are knee-dominant, but "have I been squatting?" has an answer and
        // lunges are not it.
        assertTrue(SELECTABLE_MOVEMENT_PATTERNS.contains(MovementPattern.LUNGE))
        assertTrue(SELECTABLE_MOVEMENT_PATTERNS.contains(MovementPattern.SQUAT))
    }
}
