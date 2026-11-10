package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The enablement preflight's grouping (ROADMAP N299).
 *
 * The rule itself is [progressionRuleFor]'s and is tested beside it; this is the list the switch
 * screen reads — which exercises it will move, which need the plan fixed, and which answer for
 * themselves. The grouping is the whole content of the sheet, so it is asserted without a screen.
 */
class AutoProgressionTest {

    @Test
    fun aPlansOwnAnswer_groupsTheExercisesByWhatTheAppCanDo() {
        val plan = listOf(
            exercise("bench", rpe = 8, reps = 5..8),
            exercise("curl", rpe = null, reps = 5..8),
            exercise("row", rpe = 8, reps = 5..8, mode = ProgressionMode.OFF),
            exercise("carry", rpe = 8, reps = null),
        )

        val preflight = autoProgressionPreflight(
            slots = listOf(slot("t1", "Push")),
            exercisesByTemplate = mapOf("t1" to plan),
        )

        assertThat(preflight.checks).hasSize(4)
        assertThat(preflight.willProgress.map { it.exerciseName }).containsExactly("bench")
        assertThat(preflight.blocked.map { it.exerciseName }).containsExactly("curl", "carry")
        assertThat(preflight.blocked.map { it.rule })
            .containsExactly(ProgressionRule.NO_TARGET_RPE, ProgressionRule.NO_REPS)
        assertThat(preflight.overridden.map { it.exerciseName }).containsExactly("row")
        assertThat(preflight.overridden.single().mode).isEqualTo(ProgressionMode.OFF)
    }

    @Test
    fun oneWorkoutOnTwoDays_isOneCheck() {
        // The plan is shared by the slots that name it, so the switch would move it once. Listing it
        // twice would promise two adjustments for one write.
        val preflight = autoProgressionPreflight(
            slots = listOf(slot("t1", "Push", position = 0), slot("t1", "Push again", position = 1)),
            exercisesByTemplate = mapOf("t1" to listOf(exercise("bench", rpe = 8, reps = 5..8))),
        )

        assertThat(preflight.checks).hasSize(1)
        assertThat(preflight.willProgress).hasSize(1)
    }

    @Test
    fun aProgramWithNoExercises_hasNothingToAdjust() {
        val preflight = autoProgressionPreflight(
            slots = listOf(slot("t1", "Push")),
            exercisesByTemplate = emptyMap(),
        )

        assertThat(preflight.checks).isEmpty()
        assertThat(preflight.hasNothing).isTrue()
    }

    private fun slot(templateId: String, name: String, position: Int = 0) = ProgramSlot(
        id = "slot-$position",
        programId = "p1",
        templateId = templateId,
        position = position,
        templateName = name,
    )

    private fun exercise(
        name: String,
        rpe: Int?,
        reps: IntRange?,
        mode: ProgressionMode? = null,
    ) = TemplateExercise(
        id = "te-$name",
        templateId = "t1",
        exerciseId = name,
        position = 0,
        exerciseName = name,
        primaryMuscle = null,
        equipment = null,
        targetRpeHalves = rpe?.times(2),
        progressionMode = mode,
        sets = listOf(
            TemplateSet(
                id = "ts-$name",
                templateExerciseId = "te-$name",
                setIndex = 0,
                targetWeightGrams = 100_000L,
                targetRepsMin = reps?.first,
                targetRepsMax = reps?.last,
            ),
        ),
    )
}
