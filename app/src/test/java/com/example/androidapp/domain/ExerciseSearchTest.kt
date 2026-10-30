package com.example.androidapp.domain

import com.example.androidapp.data.SeedExercises
import com.example.androidapp.domain.ExerciseSearch.matches
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun matchesTheWordsOfTheName_howeverTheyAreSeparated() {
        // N224: the separators are not part of the word. Typing the name without its hyphen, or without the
        // space, finds the movement the name belongs to.
        val pullUp = exercise("pull-up", "Pull-Up", MuscleGroup.LATS, Equipment.BODYWEIGHT)

        assertTrue(pullUp.matches("pullup"))
        assertTrue(pullUp.matches("pull up"))
        assertTrue(pullUp.matches("PULL-UP"))
    }

    @Test
    fun matchesSeveralWordsOfTheName_typedWithoutTheirSeparators() {
        // "bench pr" is how a lifter writes it in a note: neither word is a prefix of the whole name, and the
        // pair has to reach the same movement the phrase does.
        assertTrue(bench.matches("bench pr"))
        assertTrue(bench.matches("benchpress"))
        assertFalse(curl.matches("bench pr"))
    }

    @Test
    fun matchesInitials() {
        assertTrue(bench.matches("bbp"))
        assertTrue(bench.matches("BB"))

        val closeGrip = exercise(
            "close-grip-bench-press",
            "Close-Grip Bench Press",
            MuscleGroup.TRICEPS,
            Equipment.BARBELL
        )
        assertTrue(closeGrip.matches("cgbp"))
    }

    @Test
    fun anInitialismOrASubstringTheNameProduces_needsNoAlias() {
        // B319: these four were in the alias table although the initials rule or the name already answers them.
        // The table is for what the name cannot say.
        val bulgarian = exercise(
            "bulgarian-split-squat",
            "Bulgarian Split Squat",
            MuscleGroup.QUADS,
            Equipment.BARBELL,
        )
        val hanging = exercise("hanging-leg-raise", "Hanging Leg Raise", MuscleGroup.CORE, Equipment.BARBELL)
        val turkish = exercise("turkish-get-up", "Turkish Get-Up", MuscleGroup.CORE, Equipment.BARBELL)
        val seated = exercise("seated-cable-row", "Seated Cable Row", MuscleGroup.CORE, Equipment.BARBELL)

        assertTrue(bulgarian.matches("bss"))
        assertTrue(hanging.matches("hlr"))
        assertTrue(turkish.matches("tgu"))
        assertTrue(seated.matches("cable row"))
    }

    @Test
    fun initialsDoNotMatchALongerQuery() {
        // The initials rule is a prefix, never a wildcard: "bbps" is not an abbreviation of anything here, and
        // a search that guessed would be the fuzzy matching N224 refused.
        assertFalse(bench.matches("bbps"))
    }

    @Test
    fun matchesAnAlias() {
        val ohp = exercise("overhead-press", "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL)
        val rdl = exercise("romanian-deadlift", "Romanian Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL)

        assertTrue(ohp.matches("ohp"))
        assertTrue(ohp.matches("military press"))
        assertTrue(rdl.matches("rdl"))
    }

    @Test
    fun anAliasBelongsToItsOwnMovement() {
        // The table is keyed by id rather than searched as a vocabulary, so one lift's shorthand cannot find
        // another lift.
        val ohp = exercise("overhead-press", "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL)
        val bench = exercise("barbell-bench-press", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL)

        assertFalse(bench.matches("ohp"))
        assertFalse(ohp.matches("rdl"))
    }

    @Test
    fun aMovementWithNoAliases_stillMatchesByItsOwnRules() {
        val curl = exercise("barbell-curl", "Barbell Curl", MuscleGroup.BICEPS, Equipment.BARBELL)

        assertTrue(curl.matches("barbell curl"))
        assertFalse(curl.matches("ohp"))
    }

    @Test
    fun everyAliasBelongsToAMovementTheSeedShips() {
        // The guard the table needs: an alias keyed to a retired or retyped id is an alias that silently stops
        // working, and the test is the only place that can see both the table and the seed.
        val seeded = SeedExercises.all.map { it.id }.toSet()

        assertThat(ExerciseAliases.byExerciseId.keys).containsExactlyElementsIn(
            ExerciseAliases.byExerciseId.keys.filter { it in seeded },
        )
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
