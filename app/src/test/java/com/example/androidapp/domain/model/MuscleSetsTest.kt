package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * How a program's planned work spreads across muscles (ROADMAP N119).
 *
 * The rule is a pure function, so the cases worth pinning are the ones that would quietly make a plan look
 * different from what it is: a ramp counted as work, a movement counted against no muscle when its family
 * states one, and work that states a muscle nowhere vanishing from the total.
 */
class MuscleSetsTest {

    private fun movement(id: String, muscle: MuscleGroup?, parent: String? = null) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        parentId = parent,
    )

    private fun category(id: String, muscle: MuscleGroup?) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        rowKind = RowKind.CATEGORY,
    )

    private fun row(id: String, exerciseId: String, vararg roles: SetType) = TemplateExercise(
        id = id,
        templateId = "t1",
        exerciseId = exerciseId,
        position = 0,
        exerciseName = id,
        primaryMuscle = null,
        equipment = null,
        sets = roles.mapIndexed { index, role ->
            TemplateSet(id = "$id-$index", templateExerciseId = id, setIndex = index, role = role)
        },
    )

    @Test
    fun aWarmUpIsNotWork_soARampDoesNotInflateItsMuscle() {
        // ROADMAP N112: only the sets that stand on their own are work, which is the rule this counts by.
        val library = listOf(movement("squat", MuscleGroup.QUADS))

        val balance = muscleBalance(
            planned = listOf(
                row("r1", "squat", SetType.WARMUP, SetType.WARMUP, SetType.NORMAL, SetType.NORMAL),
            ),
            library = library,
        )

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.QUADS, 2))
    }

    @Test
    fun aMovementInheritsItsCategorysMuscle() {
        // ROADMAP N110: own-if-stated, else the category's — which is why the library is part of the read.
        val library = listOf(
            category("cat-press", MuscleGroup.CHEST),
            movement("bench", muscle = null, parent = "cat-press"),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "bench", SetType.NORMAL, SetType.NORMAL, SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.CHEST, 3))
    }

    @Test
    fun aMovementsOwnMuscleWins_overItsCategorys() {
        val library = listOf(
            category("cat-press", MuscleGroup.CHEST),
            movement("close-grip", MuscleGroup.TRICEPS, parent = "cat-press"),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "close-grip", SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.TRICEPS, 1))
    }

    @Test
    fun workThatStatesNoMuscleAnywhere_isItsOwnBucket_ratherThanDropped() {
        // Dropping it would make the rest of the plan look more balanced than it is, and the total would
        // disagree with the sets a lifter can count in the editor.
        val balance = muscleBalance(
            planned = listOf(
                row("r1", "mystery", SetType.NORMAL),
                row("r2", "squat", SetType.NORMAL),
            ),
            library = listOf(movement("squat", MuscleGroup.QUADS), movement("mystery", muscle = null)),
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.QUADS, 1),
            MuscleSets(null, 1),
        )
    }

    @Test
    fun theBiggestSharesComeFirst_andTiesFollowTheTaxonomy() {
        val library = listOf(
            movement("squat", MuscleGroup.QUADS),
            movement("curl", MuscleGroup.BICEPS),
            movement("row", MuscleGroup.LATS),
        )

        val balance = muscleBalance(
            planned = listOf(
                row("r1", "curl", SetType.NORMAL),
                row("r2", "row", SetType.NORMAL),
                row("r3", "squat", SetType.NORMAL, SetType.NORMAL, SetType.NORMAL),
            ),
            library = library,
        )

        // Three quads sets first, then the two singles in the taxonomy's own order (lats before biceps).
        assertThat(balance.map { it.muscle }).containsExactly(
            MuscleGroup.QUADS,
            MuscleGroup.LATS,
            MuscleGroup.BICEPS,
        ).inOrder()
    }

    @Test
    fun aPlanWithNoWorkingSets_hasNoRows() {
        val balance = muscleBalance(
            planned = listOf(row("r1", "squat", SetType.WARMUP)),
            library = listOf(movement("squat", MuscleGroup.QUADS)),
        )

        assertThat(balance).isEmpty()
    }
}
