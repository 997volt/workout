package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * How a program's planned work spreads across muscles (ROADMAP N119, N122).
 *
 * The rule is a pure function, so the cases worth pinning are the ones that would quietly make a plan look
 * different from what it is: a ramp counted as work, a movement counted against no muscle when its family
 * states one, a secondary dropped or double-counted, and work that states a muscle nowhere vanishing from
 * the total.
 *
 * The unit is **half-sets**: a working set is `PRIMARY_HALF_SETS` to its primary and `SECONDARY_HALF_SETS` to
 * each secondary, so "2" is one set and "1" is a secondary's half.
 */
class MuscleSetsTest {

    private fun movement(
        id: String,
        muscle: MuscleGroup?,
        parent: String? = null,
        secondaries: List<MuscleGroup> = emptyList(),
    ) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        secondaryMuscles = secondaries,
        parentId = parent,
    )

    private fun category(
        id: String,
        muscle: MuscleGroup?,
        secondaries: List<MuscleGroup> = emptyList(),
    ) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        secondaryMuscles = secondaries,
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

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.QUADS, 4))
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

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.CHEST, 6))
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

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.TRICEPS, 2))
    }

    @Test
    fun aSecondaryTakesHalfOfEveryWorkingSet() {
        // ROADMAP N122: the seed's own barbell row names biceps and forearms, and they are muscles the set
        // really works — so a week of rows is not a week of lats alone.
        val library = listOf(
            movement(
                "barbell-row",
                MuscleGroup.LATS,
                secondaries = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            ),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "barbell-row", SetType.NORMAL, SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.LATS, 4),
            MuscleSets(MuscleGroup.BICEPS, 2),
            MuscleSets(MuscleGroup.FOREARMS, 2),
        )
    }

    @Test
    fun aMovementsSecondaries_inheritFromItsCategory_too() {
        // The same winner rule as the primary (N110), on the list rather than the single value.
        val library = listOf(
            category("cat-row", MuscleGroup.LATS, secondaries = listOf(MuscleGroup.BICEPS)),
            movement("cable-row", muscle = null, parent = "cat-row"),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "cable-row", SetType.NORMAL, SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.LATS, 4),
            MuscleSets(MuscleGroup.BICEPS, 2),
        )
    }

    @Test
    fun aMuscleThatIsBothPrimaryAndSecondary_isCountedOnce() {
        // A set is one thing to one muscle: a list naming the primary again must not pay it twice.
        val library = listOf(
            movement("leg-press", MuscleGroup.QUADS, secondaries = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS)),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "leg-press", SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.QUADS, 2),
            MuscleSets(MuscleGroup.HAMSTRINGS, 1),
        )
    }

    @Test
    fun aSecondaryNamedTwice_isCountedOnce() {
        val library = listOf(
            movement("curl", MuscleGroup.BICEPS, secondaries = listOf(MuscleGroup.FOREARMS, MuscleGroup.FOREARMS)),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "curl", SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.BICEPS, 2),
            MuscleSets(MuscleGroup.FOREARMS, 1),
        )
    }

    @Test
    fun secondariesWithoutAPrimary_areCredited_andInventNoPrimary() {
        // The bucket is for work whose muscle is stated *nowhere*, so a movement that states secondaries does
        // not land in it: there is simply no primary share to count.
        val library = listOf(
            movement("carry", muscle = null, secondaries = listOf(MuscleGroup.FOREARMS, MuscleGroup.CORE)),
        )

        val balance = muscleBalance(
            planned = listOf(row("r1", "carry", SetType.NORMAL, SetType.NORMAL)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.FOREARMS, 2),
            MuscleSets(MuscleGroup.CORE, 2),
        )
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
            MuscleSets(MuscleGroup.QUADS, 2),
            MuscleSets(null, 2),
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

    @Test
    fun plannedWorkingSets_countsTheWork_andIgnoresTheRamps() {
        // The screen's header reads this rather than the sum of the credits, which no longer equals it.
        val planned = listOf(
            row("r1", "squat", SetType.WARMUP, SetType.NORMAL, SetType.NORMAL),
            row("r2", "curl", SetType.NORMAL),
            row("r3", "row", SetType.WARMUP),
        )

        assertThat(plannedWorkingSets(planned)).isEqualTo(3)
    }
}
