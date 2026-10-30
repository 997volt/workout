package com.example.androidapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which rows the library holds twice, and which of them stays (ROADMAP N238).
 *
 * Two sources and one rule. The **mechanical** one is a shape: a row with a seeded movement's name that states
 * nothing and is filed nowhere is an import's leftover, and the seeded row is the survivor. The **ruled** ones are
 * the author's table, which names rows on both sides — so the cases that matter here are a ruling whose source is
 * not on the device (nothing to do), one whose survivor is not there (nothing to merge into), and a row that is
 * both bare and ruled (the mechanical rule settles it, and the two must not produce two different answers).
 */
class MovementMergeTest {

    private val seed = setOf("Back Squat", "Dumbbell Bench Press", "Pec Deck")

    @Test
    fun aBareRowWithASeededName_isMergedIntoTheSeed() {
        val library = listOf(
            candidate("back-squat", "Back Squat", seeded = true),
            candidate("custom-back-squat", "Back Squat", bare = true),
        )

        val merges = mergedMovementPairs(library, seed)

        assertEquals(
            listOf(MovementMerge("custom-back-squat", "back-squat")),
            merges,
        )
    }

    @Test
    fun aRowWithASeededNameThatStatesSomething_isNotMechanical() {
        // An edit produces a row with a taxonomy or a family; an import leaves one blank. Only the blank one is
        // a duplicate, which is why the rule reads the shape rather than the name alone.
        val library = listOf(
            candidate("back-squat", "Back Squat", seeded = true),
            candidate("my-squat", "Back Squat", bare = false),
        )

        assertTrue(mergedMovementPairs(library, seed).isEmpty())
    }

    @Test
    fun aSeededRow_isNeverMergedAway() {
        val library = listOf(
            candidate("one", "Back Squat", seeded = true, bare = true),
            candidate("two", "Back Squat", seeded = true),
        )

        assertTrue(mergedMovementPairs(library, seed).isEmpty())
    }

    @Test
    fun aRuling_mergesTheTwoNamesItNames() {
        val library = listOf(
            candidate("pec-deck", "Pec Deck", seeded = true),
            candidate("machine-fly", "Machine Fly", bare = true),
        )

        val merges = mergedMovementPairs(library, seed)

        assertEquals(listOf(MovementMerge("machine-fly", "pec-deck")), merges)
    }

    @Test
    fun aRuledSourceTheLifterConfigured_isNotMerged() {
        // B298: the table was derived from import leftovers, which is the shape the mechanical rule reads. A
        // row the lifter filled in is theirs whatever its name, and the old rule deleted it on the next open.
        val library = listOf(
            candidate("pec-deck", "Pec Deck", seeded = true),
            candidate("machine-fly", "Machine Fly", bare = false),
        )

        assertTrue(mergedMovementPairs(library, seed).isEmpty())
    }

    @Test
    fun aRulingWhoseSourceIsNotHere_isNothingToDo() {
        val library = listOf(candidate("pec-deck", "Pec Deck", seeded = true))

        assertTrue(mergedMovementPairs(library, seed).isEmpty())
    }

    @Test
    fun aRulingWhoseSurvivorIsNotHere_isSkipped_ratherThanMergedIntoNothing() {
        // The table's survivor may be the lifter's own row: a device that has not trained that movement has
        // nothing to merge into, and inventing a survivor would be creating a movement nobody made.
        val library = listOf(candidate("dumbbell-curl-2", "Bicep Curl (Dumbbell)", bare = true))

        assertTrue(mergedMovementPairs(library, seed).isEmpty())
    }

    @Test
    fun twoNamesForOneMachine_bothMergeIntoIt() {
        val library = listOf(
            candidate("machine-triceps", "Machine Triceps Extension", bare = true),
            candidate("machine-arm", "Machine Arm Extension", bare = true),
            candidate("triceps-machine", "Triceps Extension (Machine)", bare = true),
        )

        val merges = mergedMovementPairs(library, seed).associateBy { it.sourceId }

        assertEquals("machine-triceps", merges["machine-arm"]?.survivorId)
        assertEquals("machine-triceps", merges["triceps-machine"]?.survivorId)
        assertTrue("the survivor is not merged into itself", "machine-triceps" !in merges)
    }

    @Test
    fun theRuledTableNamesEveryLineTheRoadmapDoes() {
        // A table this long is data entry, so what a test can pin is that each ruled source points somewhere
        // and that no source is listed twice — the counts the roadmap states, not the names it lists.
        assertEquals(19, MOVEMENT_MERGE_RULINGS.size)
        assertEquals(
            MOVEMENT_MERGE_RULINGS.size,
            MOVEMENT_MERGE_RULINGS.map { it.away }.distinct().size,
        )
        assertTrue(MOVEMENT_MERGE_RULINGS.none { it.away == it.into })

        // B319: every survivor is a movement the seed still ships, or the ruling silently never fires; and no
        // source is a seeded name, or the seeded guard would skip the line it was written for.
        val seeded = SeedExercises.all.map { it.name }.toSet()
        assertEquals(
            "every survivor is still seeded",
            MOVEMENT_MERGE_RULINGS.map { it.into }.filter { it in seeded },
            MOVEMENT_MERGE_RULINGS.map { it.into },
        )
        assertTrue(
            "no ruled source is a seeded name",
            MOVEMENT_MERGE_RULINGS.none { it.away in seeded },
        )
    }

    private fun candidate(
        id: String,
        name: String,
        seeded: Boolean = false,
        bare: Boolean = false,
    ) = MergeCandidate(id = id, name = name, seeded = seeded, isBare = bare)
}
