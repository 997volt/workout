package com.example.androidapp.domain

import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.VolumeReason
import com.example.androidapp.domain.model.progressionPromptFor
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeTrendsRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The plan earning a set back (ROADMAP N311).
 *
 * The rule is read from the plan and the finished sessions; these cases pin the two triggers, the crossing
 * that makes the finish say it once, and the gates (the cap and the stall) that keep the card from offering
 * more work where the plan is stuck or already long.
 */
class PlanVolumeFinderTest {

    private val finder = PlanVolumeFinder(
        FakeProgramRepository(),
        FakeTemplateRepository(),
        FakeTrendsRepository(),
    )

    @Test
    fun theExtraSet_isDrawnFromTheLoggedOne_whenTheLastTwoSessionsLoggedIt() {
        val exercise = exercise()
        val current = prompt(exercise, List(4) { performed() })
        val previous = listOf(
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(4) { performed() }),
        )

        val suggestion = finder.doneSuggestion(exercise, current, previous)

        assertThat(suggestion?.reason).isEqualTo(VolumeReason.EXTRA_SET)
        assertThat(suggestion?.plannedSets).isEqualTo(3)
        assertThat(suggestion?.edit?.targetWeightGrams).isEqualTo(100_000L)
        assertThat(suggestion?.edit?.targetRepsMin).isEqualTo(5)
        assertThat(suggestion?.edit?.role).isEqualTo(SetType.NORMAL)
    }

    @Test
    fun theComfortableSet_copiesTheLastTarget_whenThreeSessionsEarnedEveryStep() {
        val exercise = exercise()
        val current = prompt(exercise, List(3) { performed() })
        val previous = listOf(
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(3) { performed() }),
        )

        val suggestion = finder.doneSuggestion(exercise, current, previous)

        assertThat(suggestion?.reason).isEqualTo(VolumeReason.COMFORTABLE)
        assertThat(suggestion?.edit?.targetWeightGrams).isEqualTo(100_000L)
        assertThat(suggestion?.edit?.targetRepsMin).isEqualTo(5)
        assertThat(suggestion?.edit?.targetRepsMax).isEqualTo(5)
    }

    @Test
    fun theExtraSet_isNotSaidAgain_whenTheSessionBeforeThePairAlsoHeld() {
        val exercise = exercise()
        val missed = List(3) { performed(reps = 3) }
        val current = prompt(exercise, missed + performed())
        val previous = listOf(
            prompt(exercise, missed + performed()),
            prompt(exercise, missed + performed()),
        )

        assertThat(finder.doneSuggestion(exercise, current, previous)).isNull()
    }

    @Test
    fun theComfortableSet_isNotSaidAgain_whenTheFourthSessionBackAlsoEarned() {
        val exercise = exercise()
        val current = prompt(exercise, List(3) { performed() })
        val previous = listOf(
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(3) { performed() }),
        )

        assertThat(finder.doneSuggestion(exercise, current, previous)).isNull()
    }

    @Test
    fun nothingIsOffered_whileThePlanIsStalled() {
        val exercise = exercise()
        val missed = List(3) { performed(reps = 3) }
        val current = prompt(exercise, missed)
        val previous = listOf(prompt(exercise, missed), prompt(exercise, missed))

        assertThat(finder.doneSuggestion(exercise, current, previous)).isNull()
    }

    @Test
    fun nothingIsOffered_atSixWorkingSets() {
        val exercise = exercise(sets = 6)
        val current = prompt(exercise, List(7) { performed() })
        val previous = listOf(
            prompt(exercise, List(6) { performed() }),
            prompt(exercise, List(7) { performed() }),
        )

        assertThat(finder.doneSuggestion(exercise, current, previous)).isNull()
    }

    private fun exercise(sets: Int = 3) = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = null,
        equipment = null,
        targetRpeHalves = 16,
        sets = (0 until sets).map { index ->
            TemplateSet(
                id = "s$index",
                templateExerciseId = "te1",
                setIndex = index,
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            )
        },
    )

    private fun performed(reps: Int = 5) = ProgressionPerformance(
        reps = reps,
        weightGrams = 100_000L,
        rpeHalves = 16,
    )

    private fun prompt(exercise: TemplateExercise, sets: List<ProgressionPerformance>): ProgressionPrompt =
        progressionPromptFor(exercise.progressionSets(), sets)
}
