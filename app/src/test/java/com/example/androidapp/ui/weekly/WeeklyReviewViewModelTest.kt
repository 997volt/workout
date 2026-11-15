package com.example.androidapp.ui.weekly

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanVolumeFinder
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.OccurrenceState
import com.example.androidapp.domain.model.PerformedExercise
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSession
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.WeeklyReviewRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeTrendsRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week's assembly (ROADMAP N252): the loop over the seven days, the two totals, the plan and the stall.
 *
 * The domain math and the screen are tested; this is the glue between them — the part a wrong date would break
 * without any single piece looking wrong.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.systemDefault()
    private var now = Instant.parse("2026-10-28T12:00:00Z")
    private val today = now.atZone(zone).toLocalDate()
    private val monday = ProgramSchedule.weekStartOf(today)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theFirstResume_doesNotStartASecondLoad() = runTest(dispatcher) {
        // B392: init's load and the route's first ON_RESUME used to race; the later one won unpredictably.
        val fake = FakeWeekly(history = emptyList(), occurrences = emptyMap())
        val viewModel = reviewViewModel(weekly = fake, timeSource = TimeSource { now })
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(fake.historyReads).isEqualTo(1)
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun onResume_reloadsTheWeekTheClockNowNames() = runTest(dispatcher) {
        // N286: a week boundary must not wait for the screen to be recreated.
        val viewModel = reviewViewModel(
            weekly = FakeWeekly(history = emptyList(), occurrences = emptyMap()),
            timeSource = TimeSource { now },
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.weekStart).isEqualTo(monday)

        now = now.plusSeconds(7 * 86_400)
        viewModel.onResume()
        advanceUntilIdle()

        assertThat(viewModel.state.value.weekStart).isEqualTo(monday.plusWeeks(1))
    }

    @Test
    fun theWeek_isAssembled_fromTheHistory_theDays_thePlan_theGains_andTheStall() = runTest(dispatcher) {
        val history = listOf(
            session("this", monday.atTime(10, 0), sets = 6, volume = 12_000),
            session("last", monday.minusWeeks(1).atTime(10, 0), sets = 4, volume = 8_000),
        )
        val fake = FakeWeekly(
            history = history,
            occurrences = mapOf(
                monday to listOf(occurrence(monday, OccurrenceState.DONE)),
                monday.plusDays(1) to listOf(occurrence(monday.plusDays(1), OccurrenceState.MISSED)),
            ),
            programs = listOf(program()),
            blocks = listOf(BlockProgress(name = "Push Pull Legs", position = 3, length = 9, complete = false)),
            stall = PlanStall(
                exerciseName = "Back Squat",
                sessions = 3,
                templateId = "t1",
                templateExerciseId = "te1",
                exerciseId = "squat"
            ),
        )
        val viewModel = reviewViewModel(weekly = fake, timeSource = TimeSource { now })

        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.weekStart).isEqualTo(monday)
        assertThat(state.weekEnd).isEqualTo(monday.plusDays(6))
        assertThat(state.thisWeek.sessions).isEqualTo(1)
        assertThat(state.thisWeek.sets).isEqualTo(6)
        assertThat(state.lastWeek.sessions).isEqualTo(1)
        assertThat(state.lastWeek.sets).isEqualTo(4)
        assertThat(state.plan.planned).isEqualTo(2)
        assertThat(state.plan.done).isEqualTo(1)
        assertThat(state.days).hasSize(7)
        assertThat(state.stalls.firstOrNull()?.exerciseName).isEqualTo("Back Squat")
        assertThat(state.stallProgramId).isEqualTo("p1")
        assertWithMessage("the block is part of the week")
            .that(state.blocks.map { it.name })
            .containsExactly("Push Pull Legs")
        assertWithMessage("an active program is followed even without weekdays")
            .that(state.hasActivePlan)
            .isTrue()
        // The row itself, not "something was returned": the gain is the one read this test cannot reach
        // through a database, so its shape is pinned here (N260).
        assertThat(state.movements).containsExactly(
            MovementGain(
                exerciseId = "back-squat",
                name = "Back Squat",
                topGrams = 120_000,
                previousGrams = 115_000,
                firstTime = false,
            ),
        )
    }

    @Test
    fun aFailedDayRead_isAnError_andRetryLoadsTheWeek() = runTest(dispatcher) {
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = mapOf(monday to listOf(occurrence(monday, OccurrenceState.DONE))),
            failDate = monday,
        )
        val viewModel = reviewViewModel(weekly = fake, timeSource = TimeSource { now })
        advanceUntilIdle()

        assertWithMessage("a failed read must be said rather than drawn as an empty week")
            .that(viewModel.state.value.error)
            .isNotNull()
        assertThat(viewModel.state.value.weekStart).isNull()

        fake.failDate = null
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.weekStart).isEqualTo(monday)
        assertThat(state.days).hasSize(7)
    }

    @Test
    fun aFailedStallRead_isSaidBesideTheWeek_ratherThanReplacingIt() = runTest(dispatcher) {
        // B439/B448: the week still draws, the failure is kept for its own section, and the screen no longer
        // claims nothing stalled from a read that never answered.
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = listOf(program()),
        ).apply { failStalls = true }
        val viewModel = reviewViewModel(fake)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.stallError).isNotNull()
        assertThat(state.weekStart).isEqualTo(monday)
        assertThat(state.days).hasSize(7)
    }

    @Test
    fun aFailedProgramRead_isAnError_ratherThanNoProgramFollowed() = runTest(dispatcher) {
        // N255: `hasActivePlan` feeds the sentence "No program is being followed", so a failed read cannot
        // answer it with false.
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = mapOf(monday to listOf(occurrence(monday, OccurrenceState.DONE))),
            failPrograms = true,
        )
        val viewModel = reviewViewModel(weekly = fake, timeSource = TimeSource { now })
        advanceUntilIdle()

        val state = viewModel.state.value
        assertWithMessage("a failed read must not be drawn as a statement about the plan")
            .that(state.error)
            .isNotNull()
        assertThat(state.weekStart).isNull()
    }

    private fun session(id: String, at: java.time.LocalDateTime, sets: Int, volume: Long) = WorkoutSummary(
        id = id,
        startedAt = at.atZone(zone).toInstant(),
        finishedAt = at.plusHours(1).atZone(zone).toInstant(),
        exerciseCount = 1,
        setCount = sets,
        volumeGrams = volume,
    )

    private fun occurrence(date: LocalDate, state: OccurrenceState) = DayOccurrence(
        slotId = "slot",
        templateId = "template",
        templateName = "Squat day",
        date = date,
        weekStart = monday,
        state = state,
        canCorrect = false,
    )

    private fun program() = WorkoutProgram(
        id = "p1",
        name = "Push Pull Legs",
        slotCount = 2,
        isActive = true,
        position = 0,
        kind = ProgramKind.WEEKLY,
    )

    private fun reviewViewModel(
        weekly: WeeklyReviewRepository,
        timeSource: TimeSource = TimeSource { now },
        planVolumeFinder: PlanVolumeFinder = PlanVolumeFinder(
            FakeProgramRepository(),
            FakeTemplateRepository(),
            FakeTrendsRepository(),
        ),
        templateRepository: FakeTemplateRepository = FakeTemplateRepository(),
    ) = WeeklyReviewViewModel(
        weekly = weekly,
        planVolumeFinder = planVolumeFinder,
        templateRepository = templateRepository,
        timeSource = timeSource,
    )

    @Test
    fun theEarnedSet_prefersTheProgramTheStallCameFrom() = runTest(dispatcher) {
        // N312: with two followed programs, the card belongs to the one the week is already answering for,
        // not to whichever happens to be listed first.
        val (programs, templates) = twoEarnedPlans()
        val trends = trendsFor(mapOf("t1" to earnedSessions("squat"), "t2" to earnedSessions("bench")))
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = twoPrograms(),
            stall = stall("te2", "bench"),
            stallProgramId = "p2",
        )
        val viewModel = reviewViewModel(
            fake,
            planVolumeFinder = PlanVolumeFinder(programs, templates, trends),
            templateRepository = templates,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.stallProgramId).isEqualTo("p2")
        assertThat(viewModel.state.value.volumes.map { it.templateExerciseId }).containsExactly("te2")
    }

    @Test
    fun theEarnedSet_fallsBackToTheFirstFollowedProgramWithOne() = runTest(dispatcher) {
        // N312: no stall of its own, so the first followed program that has a suggestion answers, and the
        // rest of the list is not read past it.
        val (programs, templates) = twoEarnedPlans()
        val trends = trendsFor(mapOf("t1" to emptyList(), "t2" to earnedSessions("bench")))
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = twoPrograms(),
        )
        val viewModel = reviewViewModel(
            fake,
            planVolumeFinder = PlanVolumeFinder(programs, templates, trends),
            templateRepository = templates,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.volumeError).isNull()
        assertThat(viewModel.state.value.volumes.map { it.templateExerciseId }).containsExactly("te2")
    }

    @Test
    fun theEarnedSetsAdd_andItsUndo_areTheRepositories() = runTest(dispatcher) {
        // N312: the weekly review's own copy of N311's Add and undo; only the plan editor's was tested.
        val (programs, templates) = twoEarnedPlans()
        val trends = trendsFor(mapOf("t1" to earnedSessions("squat")))
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = listOf(program()),
        )
        val viewModel = reviewViewModel(
            fake,
            planVolumeFinder = PlanVolumeFinder(programs, templates, trends),
            templateRepository = templates,
        )
        advanceUntilIdle()
        val suggestion = viewModel.state.value.volumes.single()
        assertThat(suggestion.templateExerciseId).isEqualTo("te1")

        viewModel.onAddVolume(suggestion)
        advanceUntilIdle()

        assertThat(viewModel.volumeUndo.value).isEqualTo("new-set")
        assertWithMessage("the card goes with the write").that(viewModel.state.value.volumes).isEmpty()

        viewModel.onUndoVolume()
        advanceUntilIdle()

        assertThat(viewModel.volumeUndo.value).isNull()
        assertThat(templates.removedSets).containsExactly("new-set")
    }

    @Test
    fun aFailedAdd_isSaidInTheSection_andTheCardStaysToRetry() = runTest(dispatcher) {
        // B439: a failed Add is the earned-set section's failure, not the week's, and the card does not vanish.
        val (programs, templates) = twoEarnedPlans()
        val trends = trendsFor(mapOf("t1" to earnedSessions("squat")))
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = listOf(program()),
        )
        val viewModel = reviewViewModel(
            fake,
            planVolumeFinder = PlanVolumeFinder(programs, templates, trends),
            templateRepository = templates,
        )
        advanceUntilIdle()
        val suggestion = viewModel.state.value.volumes.single()
        templates.failAddSet = true

        viewModel.onAddVolume(suggestion)
        advanceUntilIdle()

        assertThat(viewModel.state.value.volumeError).isNotNull()
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.volumes).containsExactly(suggestion)
        assertThat(viewModel.volumeUndo.value).isNull()
    }

    @Test
    fun aSecondAdd_andASecondUndo_beforeTheWriteLands_areIgnored() = runTest(dispatcher) {
        // B449: the card's Add is one tap; two before the write lands must append one set, and its undo one.
        val (programs, templates) = twoEarnedPlans()
        val trends = trendsFor(mapOf("t1" to earnedSessions("squat")))
        val fake = FakeWeekly(
            history = emptyList(),
            occurrences = emptyMap(),
            programs = listOf(program()),
        )
        val viewModel = reviewViewModel(
            fake,
            planVolumeFinder = PlanVolumeFinder(programs, templates, trends),
            templateRepository = templates,
        )
        advanceUntilIdle()
        val suggestion = viewModel.state.value.volumes.single()

        viewModel.onAddVolume(suggestion)
        viewModel.onAddVolume(suggestion)
        advanceUntilIdle()
        assertThat(templates.addedSets).hasSize(1)

        viewModel.onUndoVolume()
        viewModel.onUndoVolume()
        advanceUntilIdle()
        assertThat(templates.removedSets).hasSize(1)
    }

    /** Two followed weekly programs, each naming one movement, for the earned-set reads (N312). */
    private fun twoEarnedPlans(): Pair<FakeProgramRepository, FakeTemplateRepository> {
        val templates = FakeTemplateRepository().apply {
            exercises.value = mapOf(
                "t1" to listOf(earnedPlanExercise("te1", "t1", "squat", "Back Squat")),
                "t2" to listOf(earnedPlanExercise("te2", "t2", "bench", "Bench Press")),
            )
        }
        val programs = FakeProgramRepository().apply {
            slotsByProgram = mapOf(
                "p1" to listOf(ProgramSlot(id = "ps1", programId = "p1", templateId = "t1", position = 0)),
                "p2" to listOf(ProgramSlot(id = "ps2", programId = "p2", templateId = "t2", position = 0)),
            )
        }
        return programs to templates
    }

    private fun twoPrograms() = listOf(program(), program().copy(id = "p2", name = "Upper"))

    /** A three-set plan whose movement can earn a step, so the volume rule judges it (N311). */
    private fun earnedPlanExercise(
        id: String,
        templateId: String,
        exerciseId: String,
        name: String,
    ) = TemplateExercise(
        id = id,
        templateId = templateId,
        exerciseId = exerciseId,
        position = 0,
        exerciseName = name,
        primaryMuscle = null,
        equipment = null,
        targetRpeHalves = 16,
        sets = (0 until 3).map { index ->
            TemplateSet(
                id = "$id-set-$index",
                templateExerciseId = id,
                setIndex = index,
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            )
        },
    )

    /** Two four-set sessions: the extra-set pattern the card holds while it holds (N311). */
    private fun earnedSessions(exerciseId: String) = listOf(
        storedSession(exerciseId, sets = 4, id = "$exerciseId-a"),
        storedSession(exerciseId, sets = 4, id = "$exerciseId-b"),
    )

    private fun storedSession(
        exerciseId: String,
        sets: Int,
        id: String = "$exerciseId-$sets",
    ) = TemplateSession(
        sessionId = id,
        startedAt = Instant.parse("2026-10-01T07:00:00Z"),
        performed = listOf(
            PerformedExercise(
                exerciseId = exerciseId,
                sets = List(sets) { ProgressionPerformance(reps = 5, weightGrams = 100_000L) },
            ),
        ),
    )

    private fun trendsFor(sessions: Map<String, List<TemplateSession>>) = FakeTrendsRepository().apply {
        sessionsByTemplate = sessions.mapValues { DataResult.Success(it.value) }
    }

    private fun stall(templateExerciseId: String, exerciseId: String) = PlanStall(
        exerciseName = if (exerciseId == "bench") "Bench Press" else "Back Squat",
        sessions = 3,
        templateId = if (templateExerciseId == "te2") "t2" else "t1",
        templateExerciseId = templateExerciseId,
        exerciseId = exerciseId,
    )

    private class FakeWeekly(
        private val history: List<WorkoutSummary>,
        private val occurrences: Map<LocalDate, List<DayOccurrence>>,
        private val programs: List<WorkoutProgram> = emptyList(),
        private val blocks: List<BlockProgress> = emptyList(),
        private val stall: PlanStall? = null,
        /** The program that holds [stall]; the others answer none, so a preferred program is reachable. */
        private val stallProgramId: String? = null,
        var failDate: LocalDate? = null,
        var failPrograms: Boolean = false,
        var historyReads: Int = 0,
    ) : WeeklyReviewRepository {
        override suspend fun movementGains(
            weekStart: LocalDate,
            zone: ZoneId,
        ): DataResult<List<MovementGain>> = DataResult.Success(
            listOf(
                MovementGain(
                    exerciseId = "back-squat",
                    name = "Back Squat",
                    topGrams = 120_000,
                    previousGrams = 115_000,
                    firstTime = false,
                ),
            ),
        )

        override suspend fun history(): DataResult<List<WorkoutSummary>> {
            historyReads++
            return DataResult.Success(history)
        }

        override suspend fun occurrencesOn(
            date: LocalDate,
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<List<DayOccurrence>> =
            if (date == failDate) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the day could not be read")))
            } else {
                DataResult.Success(occurrences[date].orEmpty())
            }

        override suspend fun activePrograms(): DataResult<List<WorkoutProgram>> =
            if (failPrograms) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the programs could not be read")))
            } else {
                DataResult.Success(programs)
            }

        var failStalls = false

        override suspend fun stallsForProgram(programId: String): DataResult<List<PlanStall>> =
            if (failStalls) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the stalls could not be read")))
            } else if (stallProgramId != null && stallProgramId != programId) {
                DataResult.Success(emptyList())
            } else {
                DataResult.Success(listOfNotNull(stall))
            }

        override suspend fun blocks(): DataResult<List<BlockProgress>> = DataResult.Success(blocks)
    }
}
