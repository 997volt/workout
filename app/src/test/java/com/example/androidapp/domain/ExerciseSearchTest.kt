package com.example.androidapp.domain

import com.example.androidapp.domain.ExerciseSearch.matches
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the predicate one row's search is decided by — no device, no Android framework types.
 *
 * The predicate rather than a `filter`: the list-building that used to wrap it now lives in
 * `libraryRows` (ROADMAP N95, B94), and `ExerciseLibraryTest` covers the trim and the grouping that path
 * adds. What is left here is the one question [matches] answers — does this row, by any of its fields,
 * contain the query — which is what a caller cannot see and what P1.1a fixed twice.
 */
class ExerciseSearchTest {

    private val squat = exercise("back-squat", "Back Squat", MuscleGroup.QUADS, Equipment.BARBELL)
    private val bench = exercise("barbell-bench-press", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL)
    private val curl = exercise("hammer-curl", "Hammer Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL)

    @Test
    fun blankQuery_matchesEveryRow() {
        // The caller's blank-query rule comes from this: `contains("")` is true for every field, so a blank
        // search answers with the whole library rather than nothing.
        assertTrue(squat.matches(""))
        assertTrue(bench.matches(""))
        assertTrue(curl.matches(""))
    }

    @Test
    fun matchesNameIgnoringCase() {
        assertTrue(squat.matches("back squat"))
        assertTrue(bench.matches("BENCH"))
        assertFalse(curl.matches("back squat"))
    }

    @Test
    fun matchesMuscleGroup() {
        assertTrue(bench.matches("chest"))
    }

    @Test
    fun matchesEquipment() {
        assertTrue(curl.matches("dumbbell"))
        assertFalse(bench.matches("dumbbell"))
    }

    @Test
    fun noMatch_isFalseRatherThanEverything() {
        assertFalse(squat.matches("zzz"))
    }

    @Test
    fun matchesASecondaryMuscle_notJustThePrimaryOne() {
        // The bug P1.1a fixes: "forearms" found nothing when forearms were a
        // secondary muscle, even though the row was visible on screen.
        val deadlift = exercise(
            id = "deadlift",
            name = "Deadlift",
            muscle = MuscleGroup.LATS,
            equipment = Equipment.BARBELL,
            secondary = listOf(MuscleGroup.FOREARMS, MuscleGroup.GLUTES),
        )

        assertTrue(deadlift.matches("forearms"))
        assertFalse(deadlift.matches("biceps"))
    }

    @Test
    fun matchesTheLocaleStableKey_asWellAsTheDisplayLabel() {
        // The label is translated; the enum name is not. Matching only the label
        // means the search silently stops working in another language.
        assertTrue(bench.matches("CHEST"))
        assertTrue(curl.matches("DUMBBELL"))
    }

    @Test
    fun matchesTheMovementPattern() {
        assertTrue(squat.matches("isolation"))
    }

    private fun exercise(
        id: String,
        name: String,
        muscle: MuscleGroup,
        equipment: Equipment,
        secondary: List<MuscleGroup> = emptyList(),
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = muscle,
        secondaryMuscles = secondary,
        equipment = equipment,
        movementPattern = MovementPattern.ISOLATION,
    )
}
