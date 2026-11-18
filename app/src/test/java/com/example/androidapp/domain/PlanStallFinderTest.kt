package com.example.androidapp.domain

import com.example.androidapp.domain.model.PerformedExercise
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
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
    fun aPlanThatMovedSinceTheSessions_isJudgedAgainstTheirOwnPrescriptions() = runTest {
        // B471: the current plan asks 8 after three steps, while the sessions did 5, 6 and 7 under
        // prescriptions that asked exactly those. Reading today's plan would call all three misses and
        // raise a deload for a lift that moved every week.
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(exercise(repsCurrent = 8)))
        }
        val sessions = (5..7).mapIndexed { index, reps ->
            TemplateSession(
                sessionId = "s$index",
                startedAt = Instant.ofEpochMilli((index + 1).toLong()),
                performed = listOf(
                    PerformedExercise(
                        exerciseId = "squat",
                        sessionExerciseId = "se-$index",
                        sets = listOf(
                            ProgressionPerformance(reps = reps, weightGrams = 100_000L, rpeHalves = 7),
                        ),
                    ),
                ),
            )
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(sessions)
            prescriptions = sessions.associate { session ->
                val performed = session.performed.single()
                val index = sessions.indexOf(session)
                performed.sessionExerciseId!! to listOf(
                    ProgressionPlanSet(
                        setId = "se-$index-set",
                        setIndex = 0,
                        role = SetType.NORMAL,
                        targetWeightGrams = 100_000L,
                        targetRepsMin = 5,
                        targetRepsMax = 8,
                        targetRepsCurrent = 5 + index,
                        targetRpeHalves = 16,
                    ),
                )
            }
        }
        val finder = PlanStallFinder(FakeProgramRepository(), templates, trends)

        val stalls = finder.stallsForTemplate("t1").getOrNull().orEmpty()

        assertThat(stalls).isEmpty()
    }

    @Test
    fun aDuplicateMovement_readsItsOwnRowsHistory() = runTest {
        // B446: a plan may name the same movement twice; the second row's history is its own. Here the first
        // row missed for three sessions and the second met the plan, so only the first is stalled.
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf(
                "t1" to listOf(exercise(id = "te1", name = "Squat A"), exercise(id = "te2", name = "Squat B")),
            )
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                List(3) { index ->
                    aSessionWithBothRows().copy(sessionId = "dup-$index")
                },
            )
        }
        val finder = PlanStallFinder(FakeProgramRepository(), templates, trends)

        val stalls = finder.stallsForTemplate("t1").getOrNull().orEmpty()

        assertThat(stalls.map { it.templateExerciseId }).containsExactly("te1")
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

    private fun exercise(
        id: String = "te1",
        name: String = "Back Squat",
        repsCurrent: Int? = null,
    ) = TemplateExercise(
        id = id,
        templateId = "t1",
        exerciseId = "squat",
        position = 0,
        exerciseName = name,
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
                targetRepsMax = 10,
                targetRepsCurrent = repsCurrent,
            )
        },
    )

    /** One session with the movement twice: the first row missed, the second met its plan (B446). */
    private fun aSessionWithBothRows() = TemplateSession(
        sessionId = "both",
        startedAt = Instant.parse("2026-09-28T07:00:00Z"),
        performed = listOf(
            PerformedExercise(
                exerciseId = "squat",
                sets = List(3) { ProgressionPerformance(reps = 3, weightGrams = 100_000L) },
            ),
            PerformedExercise(
                exerciseId = "squat",
                sets = List(3) { ProgressionPerformance(reps = 5, weightGrams = 100_000L) },
            ),
        ),
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
