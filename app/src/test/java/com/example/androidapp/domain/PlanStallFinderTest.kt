package com.example.androidapp.domain

import com.example.androidapp.domain.model.PerformedExercise
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSession
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeTrendsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/** The stall finder's card reads (ROADMAP N312): the program path and a failed history read. */
class PlanStallFinderTest {

    @Test
    fun theStallCard_readsEverySlotTemplate() = runTest {
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(exercise()))
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                List(3) { index -> aSessionThatMissed().copy(sessionId = "s$index") },
            )
        }
        val programs = FakeProgramRepository().apply {
            slots.value = listOf(
                ProgramSlot(id = "ps1", programId = "p1", templateId = "t1", position = 0),
            )
        }
        val finder = PlanStallFinder(programs, templates, trends)

        val stalls = finder.stallsForProgram("p1").getOrNull().orEmpty()

        assertThat(stalls.single().exerciseName).isEqualTo("Back Squat")
        assertThat(stalls.single().sessions).isEqualTo(3)
    }

    @Test
    fun aFailedHistoryRead_isAFailure() = runTest {
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(exercise()))
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Failure(DataError.Storage(IllegalStateException("history")))
        }
        val programs = FakeProgramRepository().apply {
            slots.value = listOf(
                ProgramSlot(id = "ps1", programId = "p1", templateId = "t1", position = 0),
            )
        }
        val finder = PlanStallFinder(programs, templates, trends)

        assertThat(finder.stallsForProgram("p1")).isInstanceOf(DataResult.Failure::class.java)
    }

    private fun exercise() = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = null,
        equipment = null,
        targetRpeHalves = 16,
        sets = (0 until 3).map { index ->
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

    private fun aSessionThatMissed() = TemplateSession(
        sessionId = "s1",
        startedAt = Instant.parse("2026-09-28T07:00:00Z"),
        performed = listOf(
            PerformedExercise(
                exerciseId = "squat",
                sets = listOf(ProgressionPerformance(reps = 3, weightGrams = 100_000L)),
            ),
        ),
    )
}
