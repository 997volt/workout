package com.example.androidapp.domain

import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PerformedExercise
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSession
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.VolumeReason
import com.example.androidapp.domain.model.progressionPromptFor
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.ui.testing.FakeMeasurementRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeTrendsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * The plan earning a set back (ROADMAP N311).
 *
 * The rule is read from the plan and the finished sessions; these cases pin the two triggers, the crossing
 * that makes the finish say it once, and the gates (the cap and the stall) that keep the card from offering
 * more work where the plan is stuck or already long.
 */
class PlanVolumeFinderTest {

    private val measurements = FakeMeasurementRepository()

    private val finder = PlanVolumeFinder(
        FakeProgramRepository(),
        FakeTemplateRepository(),
        FakeTrendsRepository(),
        measurements,
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
    fun anExtraHold_isWrittenAsAHold() {
        // N319: the earned-set edit carried reps unconditionally for a counted plan, and this branch
        // (a held extra set) had no test at all.
        val exercise = exercise()
        val hold = ProgressionPerformance(
            reps = 0,
            weightGrams = 20_000L,
            rpeHalves = 16,
            durationSeconds = 45,
        )
        val current = prompt(exercise, List(3) { performed() } + hold)
        val previous = listOf(
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(3) { performed() } + hold),
        )

        val suggestion = finder.doneSuggestion(exercise, current, previous)

        assertThat(suggestion?.reason).isEqualTo(VolumeReason.EXTRA_SET)
        assertThat(suggestion?.edit?.targetDurationSeconds).isEqualTo(45)
        assertThat(suggestion?.edit?.targetRepsMin).isNull()
        assertThat(suggestion?.edit?.targetRepsMax).isNull()
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
    fun anExtraRung_isNotAnExtraWorkingSet() {
        // B454: a second drop is part of the group, not a working set the plan is short of.
        val exercise = rungExercise()
        val anchor = performed()
        val drop = ProgressionPerformance(
            reps = 8,
            weightGrams = 80_000L,
            rpeHalves = 16,
            role = SetType.DROP,
        )
        val extraDrop = drop.copy(weightGrams = 60_000L)
        val withoutExtra = prompt(exercise, listOf(anchor, drop))
        // The anchor is short, so the only pattern the sessions make is the extra rung's; the drop is
        // not a working set and must not turn that into an EXTRA_SET.
        val withExtra = prompt(exercise, listOf(performed(reps = 3), drop, extraDrop))

        val suggestion = finder.doneSuggestion(
            exercise,
            withExtra,
            listOf(withoutExtra, withExtra),
        )

        assertThat(suggestion).isNull()
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
    fun nothingIsOffered_whenTheCurrentSessionCompletesTheStall() {
        // B427: the old case had no trigger, so it stayed green with the gate deleted. Here the current
        // session has the extra-set trigger, and the gate must see it as the third missed session too.
        val exercise = exercise()
        val missed = List(3) { performed(reps = 3) }
        val current = prompt(exercise, missed + performed())
        val previous = listOf(
            prompt(exercise, missed),
            prompt(exercise, missed + performed()),
        )

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

    @Test
    fun theDoneSuggestion_usesTheResolvedRow_ofADuplicateMovement() = runTest {
        // B428: a plan may name the same movement twice (the N54 duplicate shape). The suggestion must
        // belong to the row that was performed, not the first row with that movement id.
        val first = exercise().copy(id = "te1", exerciseName = "Squat A")
        val second = exercise().copy(id = "te2", exerciseName = "Squat B")
        val templates = FakeTemplateRepository().apply { exercises.value = mapOf("t1" to listOf(first, second)) }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(
                    // Both rows were trained; the first row logged its three sets, the second four.
                    duplicateSession(first = List(3) { performed() }, second = List(3) { performed() }),
                    duplicateSession(first = List(3) { performed() }, second = List(4) { performed() }),
                ),
            )
        }
        val finder = PlanVolumeFinder(FakeProgramRepository(), templates, trends, measurements)
        val current = prompt(second, List(4) { performed() })

        val suggestion = finder.doneSuggestion("t1", "te2", current, List(4) { performed() }).getOrNull()

        assertThat(suggestion?.templateExerciseId).isEqualTo("te2")
        assertThat(suggestion?.exerciseName).isEqualTo("Squat B")
    }

    @Test
    fun theFinishReadsEachSessionsOwnPlan_whenTodaysPlanHasMoved() = runTest {
        // B471: the previous sessions earned their steps under plans asking 6 and 7; today's plan asks
        // 8 because progression moved it. Built from today's plan, their prompts would all be short and
        // the comfortable card would never appear for exactly the program the feature is for.
        val currentPlan = rangeExercise(current = 8)
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(currentPlan))
        }
        val history = listOf(4 to 5, 6 to 6, 7 to 7)
        val sessions = history.mapIndexed { index, (reps, _) ->
            TemplateSession(
                sessionId = "s$index",
                startedAt = Instant.ofEpochMilli((index + 1).toLong()),
                performed = listOf(
                    PerformedExercise(
                        exerciseId = "squat",
                        sessionExerciseId = "se-$index",
                        sets = listOf(performed(reps = reps)),
                    ),
                ),
            )
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(sessions)
            prescriptions = history.mapIndexed { index, (_, current) ->
                "se-$index" to rangeExercise(current).progressionSets()
            }.toMap()
        }
        val finder = PlanVolumeFinder(FakeProgramRepository(), templates, trends, measurements)
        val current = progressionPromptFor(currentPlan.progressionSets(), listOf(performed(reps = 8)))

        val suggestion = finder.doneSuggestion("t1", "te1", current, listOf(performed(reps = 8))).getOrNull()

        assertThat(suggestion?.reason).isEqualTo(VolumeReason.COMFORTABLE)
    }

    @Test
    fun theCard_readsTheRowsOwnPerformedEntry_ofADuplicateMovement() = runTest {
        // B446: the card read still matched by exerciseId alone, so a plan naming the same movement twice
        // read the first row's sets for both. Only the row that logged four sets earned the extra one.
        val first = exercise().copy(id = "te1", exerciseName = "Squat A")
        val second = exercise().copy(id = "te2", exerciseName = "Squat B")
        val templates = FakeTemplateRepository().apply { exercises.value = mapOf("t1" to listOf(first, second)) }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(
                    duplicateSession(first = List(3) { performed() }, second = List(4) { performed() }),
                    duplicateSession(first = List(3) { performed() }, second = List(4) { performed() }),
                ),
            )
        }
        val finder = PlanVolumeFinder(FakeProgramRepository(), templates, trends, measurements)

        val suggestions = finder.suggestionsForTemplate("t1").getOrNull().orEmpty()

        assertThat(suggestions.map { it.templateExerciseId }).containsExactly("te2")
        assertThat(suggestions.single().reason).isEqualTo(VolumeReason.EXTRA_SET)
    }

    private fun duplicateSession(
        first: List<ProgressionPerformance>,
        second: List<ProgressionPerformance>,
    ) = TemplateSession(
        sessionId = "dup-${first.size}-${second.size}-${System.identityHashCode(first)}",
        startedAt = Instant.parse("2026-09-01T07:00:00Z"),
        performed = listOf(
            PerformedExercise(exerciseId = "squat", sets = first),
            PerformedExercise(exerciseId = "squat", sets = second),
        ),
    )

    private fun session(
        sets: List<com.example.androidapp.domain.model.ProgressionPerformance>,
        exerciseId: String = "squat",
        joints: List<JointPain> = emptyList(),
    ) = TemplateSession(
        sessionId = "s-${sets.size}-${System.identityHashCode(sets)}",
        startedAt = Instant.parse("2026-09-01T07:00:00Z"),
        performed = listOf(
            PerformedExercise(exerciseId = exerciseId, sets = sets, joints = joints),
        ),
    )

    @Test
    fun aHeldPreviousSession_breaksTheExtraSetRun() {
        // N377: a held session is not one of the pattern's clear sessions. The same pair offers the set
        // when neither is held and none when one is — the gate is the only difference.
        val exercise = exercise()
        val current = prompt(exercise, List(4) { performed() })
        val previous = listOf(
            prompt(exercise, List(3) { performed() }),
            prompt(exercise, List(4) { performed() }),
        )

        val held = finder.doneSuggestion(
            exercise,
            current,
            previous,
            previousHeld = listOf(false, true),
        )
        val clear = finder.doneSuggestion(
            exercise,
            current,
            previous,
            previousHeld = listOf(false, false),
        )

        assertThat(held).isNull()
        assertThat(clear?.reason).isEqualTo(VolumeReason.EXTRA_SET)
    }

    @Test
    fun theCard_withholdsTheOffer_whenTheDayWasSore() = runTest {
        // N377: the card reads the same gate the *Done* snackbar reads, from the day at the session's start.
        val exercise = exercise().copy(primaryMuscle = MuscleGroup.QUADS)
        val templates = FakeTemplateRepository().apply { exercises.value = mapOf("t1" to listOf(exercise)) }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(session(List(4) { performed() }), session(List(4) { performed() })),
            )
        }
        val sore = FakeMeasurementRepository().apply {
            measurements.value = listOf(
                BodyMeasurement(
                    id = "day",
                    measuredAt = Instant.parse("2026-09-01T06:00:00Z"),
                    kind = MeasurementKind.DAILY,
                    soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 9)),
                ),
            )
        }
        val clear = FakeMeasurementRepository()

        val clearSuggestion = PlanVolumeFinder(FakeProgramRepository(), templates, trends, clear)
            .suggestionsForTemplate("t1").getOrNull().orEmpty()
        val heldSuggestion = PlanVolumeFinder(FakeProgramRepository(), templates, trends, sore)
            .suggestionsForTemplate("t1").getOrNull().orEmpty()

        assertThat(clearSuggestion.map { it.reason }).containsExactly(VolumeReason.EXTRA_SET)
        assertThat(heldSuggestion).isEmpty()
    }

    @Test
    fun theCard_withholdsTheOffer_whenTheSessionReportedPain() = runTest {
        // The joint half of the same gate, from the session's own rating rather than the day's record.
        val templates = FakeTemplateRepository().apply { exercises.value = mapOf("t1" to listOf(exercise())) }
        val pain = listOf(JointPain(Joint.KNEE, Side.RIGHT, 6))
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(
                    session(List(4) { performed() }, joints = pain),
                    session(List(4) { performed() }, joints = pain),
                ),
            )
        }
        val finder = PlanVolumeFinder(
            FakeProgramRepository(),
            templates,
            trends,
            FakeMeasurementRepository(),
        )

        val suggestions = finder.suggestionsForTemplate("t1").getOrNull().orEmpty()

        assertThat(suggestions).isEmpty()
    }

    @Test
    fun theCard_ignoresASessionThatDidNotTrainTheMovement() = runTest {
        // B429: absence is neither evidence nor a break. A session that trained another movement used to
        // sit between the two extra-set sessions and hide the pattern.
        val templates = FakeTemplateRepository().apply { exercises.value = mapOf("t1" to listOf(exercise())) }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(
                    session(List(4) { performed() }),
                    session(List(3) { performed() }, exerciseId = "bench"),
                    session(List(4) { performed() }),
                ),
            )
        }
        val finder = PlanVolumeFinder(FakeProgramRepository(), templates, trends, measurements)

        val suggestion = finder.suggestionsForTemplate("t1").getOrNull().orEmpty().single()

        assertThat(suggestion.reason).isEqualTo(VolumeReason.EXTRA_SET)
    }

    @Test
    fun theProgramCard_collectsEachTemplatesSuggestions() = runTest {
        // N312: the card's program path (suggestionsForProgram) was untested; only the per-template read was.
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(exercise()))
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Success(
                listOf(session(List(4) { performed() }), session(List(4) { performed() })),
            )
        }
        val programs = FakeProgramRepository().apply {
            slots.value = listOf(
                ProgramSlot(id = "ps1", programId = "p1", templateId = "t1", position = 0),
            )
        }
        val finder = PlanVolumeFinder(programs, templates, trends, measurements)

        val suggestions = finder.suggestionsForProgram("p1").getOrNull().orEmpty()

        assertThat(suggestions.single().templateExerciseId).isEqualTo("te1")
        assertThat(suggestions.single().reason).isEqualTo(VolumeReason.EXTRA_SET)
    }

    @Test
    fun aFailedHistoryRead_isAFailure() = runTest {
        // N312: the card's failure path.
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf("t1" to listOf(exercise()))
        }
        val trends = FakeTrendsRepository().apply {
            templateSessions = DataResult.Failure(DataError.Storage(IllegalStateException("history")))
        }
        val finder = PlanVolumeFinder(FakeProgramRepository(), templates, trends, measurements)

        assertThat(finder.suggestionsForTemplate("t1")).isInstanceOf(DataResult.Failure::class.java)
    }

    /** One exercise with a 5–8 range at 100 kg and the current rep the test wants. */
    private fun rangeExercise(current: Int) = exercise(sets = 1).let { plan ->
        plan.copy(
            sets = plan.sets.map { set ->
                set.copy(targetRepsMin = 5, targetRepsMax = 8, targetRepsCurrent = current)
            },
        )
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

    /** A plan with one working set and one drop rung, for the rung-is-not-volume case (B454). */
    private fun rungExercise() = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = null,
        equipment = null,
        targetRpeHalves = 16,
        sets = listOf(
            TemplateSet(
                id = "s0",
                templateExerciseId = "te1",
                setIndex = 0,
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
            TemplateSet(
                id = "s1",
                templateExerciseId = "te1",
                setIndex = 1,
                role = SetType.DROP,
                dropValueGrams = 2_500L,
            ),
        ),
    )

    private fun performed(reps: Int = 5) = ProgressionPerformance(
        reps = reps,
        weightGrams = 100_000L,
        rpeHalves = 16,
    )

    private fun prompt(exercise: TemplateExercise, sets: List<ProgressionPerformance>): ProgressionPrompt =
        progressionPromptFor(exercise.progressionSets(), sets)
}
