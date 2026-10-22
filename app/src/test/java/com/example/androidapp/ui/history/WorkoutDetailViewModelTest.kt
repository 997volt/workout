package com.example.androidapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.SequenceSchedule
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.StartedSession
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The history detail's own mapping (ROADMAP B5, B9).
 *
 * This screen had no test at all, which is how B5 shipped: the live workout showed an
 * assisted set as `-20 kg` and the detail showed `0 kg`, because the ViewModel built its
 * `HistorySet` without passing the column. The screen test built its state by hand, so
 * it could not see a mapping that never ran.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeWorkoutRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeWorkoutRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Collects the state for the life of the test.
     *
     * `uiState` is shared `WhileSubscribed`, so without a collector the flows never run
     * and the screen looks empty — which is not the mapping this test is about.
     */
    private fun TestScope.observe(viewModel: WorkoutDetailViewModel) {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
    }

    @Test
    fun savingAsATemplate_passesTheName_andOffersTheNewPlan() = runTest(dispatcher) {
        // ROADMAP N31: this ViewModel's part is to hand the name over and surface what came back —
        // the copy itself is the repository's transaction, tested against a real database.
        val templates = FakeTemplateRepository(savedId = "plan-1")
        val viewModel = viewModelFor(templates)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onSaveAsTemplate("  Push day  ")
        advanceUntilIdle()

        assertEquals(listOf(SESSION_ID to "  Push day  "), templates.copied)
        assertEquals("plan-1", viewModel.savedTemplate.value)

        viewModel.onDismissSavedTemplate()
        assertNull("the offer is dismissed once taken", viewModel.savedTemplate.value)
    }

    private fun viewModelFor(
        templates: FakeTemplateRepository = FakeTemplateRepository(),
        programs: FakeProgramRepository = FakeProgramRepository(),
    ) = WorkoutDetailViewModel(
        workoutRepository = repository,
        templateRepository = templates,
        programRepository = programs,
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
    )

    @Test
    fun schedulingTheSavedPlan_addsASlot_andOffersTheProgram() = runTest(dispatcher) {
        // P3.18: the schedule offer writes the program's slot with the day chosen in the dialog, and
        // reports the program so the screen can open it.
        val templates = FakeTemplateRepository(savedId = "plan-1")
        val programs = FakeProgramRepository()
        val viewModel = viewModelFor(templates, programs)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onSaveAsTemplate("Push day")
        advanceUntilIdle()
        viewModel.onScheduleTemplate("p1", DayOfWeek.WEDNESDAY)
        advanceUntilIdle()

        assertEquals(listOf(Triple("p1", "plan-1", DayOfWeek.WEDNESDAY)), programs.added)
        assertEquals("p1", viewModel.scheduledProgram.value)

        viewModel.onScheduledHandled()
        assertNull("the offer is taken once", viewModel.scheduledProgram.value)
    }

    @Test
    fun everyColumnOfASet_reachesTheScreen() = runTest(dispatcher) {
        // The mapping is the whole test: a column the ViewModel forgets is a value the
        // screen silently renders as its default.
        repository.sets.value = listOf(
            SetEntry(
                id = "set1",
                sessionExerciseId = "se1",
                setIndex = 0,
                reps = 8,
                weightGrams = 0L,
                assistanceGrams = 20_000L,
                rpeHalves = 19,
                note = "hard",
            ),
        )

        val viewModel = viewModelFor()
        observe(viewModel)
        advanceUntilIdle()

        val set = viewModel.uiState.value.exercises.single().sets.single()
        assertEquals("B5: the help survives into history", 20_000L, set.assistanceGrams)
        assertEquals("B6's sibling: halves stay halves in the model", 19, set.rpeHalves)
        assertEquals(8, set.reps)
        assertEquals("hard", set.note)
    }

    @Test
    fun thePlansName_reachesTheTitle_readLiveFromTheTemplateRow() = runTest(dispatcher) {
        // ROADMAP N58, B74: the title is the plan's own name, read live from the template row rather than
        // copied onto the session — and read through the unfiltered name query, so a plan deleted since still
        // names the workout it was. That the query keeps a deleted row is the DAO's business; this pins the
        // wiring the review found unguarded, since the fake used to answer null and nothing asserted a name.
        repository.session.value = repository.session.value?.copy(templateId = "t1")

        val viewModel = viewModelFor(FakeTemplateRepository(templateName = "Push day"))
        observe(viewModel)
        advanceUntilIdle()

        assertEquals("Push day", viewModel.uiState.value.templateName)
    }

    @Test
    fun aWorkoutStartedFromNothing_readsNoPlanName() = runTest(dispatcher) {
        // A session with no plan behind it has no id to follow, so nothing is read and the screen falls
        // back to the workout's own date (N84).
        repository.session.value = repository.session.value?.copy(templateId = null)

        val viewModel = viewModelFor(FakeTemplateRepository(templateName = "Push day"))
        observe(viewModel)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.templateName)
    }

    @Test
    fun theMuscleFeelAndThePickedJoints_reachTheScreen() = runTest(dispatcher) {
        val viewModel = viewModelFor()
        observe(viewModel)
        advanceUntilIdle()

        val exercise = viewModel.uiState.value.exercises.single()
        assertEquals(8, exercise.muscleFeel)
        assertEquals(listOf(JointPain(Joint.SHOULDER, Side.LEFT, 4)), exercise.joints)
    }

    @Test
    fun theVolume_theScreenShows_isTheSumTheSqlComputes() = runTest(dispatcher) {
        // The getter's KDoc claims this is "one formula in two places" and that the
        // instrumented test asserts they agree. This is the formula half of that claim;
        // `WorkoutEditingTest` asserts the same number against the SQL for real rows.
        repository.sets.value = listOf(
            setEntry(id = "set1", index = 0, reps = 5, weightGrams = 100_000L),
            // An assisted set contributes nothing, exactly as bodyweight does (N15).
            setEntry(id = "set2", index = 1, reps = 8, weightGrams = 0L, assistance = 20_000L),
        )

        val viewModel = viewModelFor()
        observe(viewModel)
        advanceUntilIdle()

        assertEquals(500_000L, viewModel.uiState.value.volumeGrams)
    }

    private fun setEntry(
        id: String,
        index: Int,
        reps: Int,
        weightGrams: Long,
        assistance: Long = 0L,
    ) = SetEntry(
        id = id,
        sessionExerciseId = "se1",
        setIndex = index,
        reps = reps,
        weightGrams = weightGrams,
        assistanceGrams = assistance,
    )

    private class FakeWorkoutRepository : WorkoutRepository {
        val session = MutableStateFlow<WorkoutSession?>(
            WorkoutSession(id = SESSION_ID, startedAt = Instant.parse("2026-09-29T07:00:00Z")),
        )
        val exercises = MutableStateFlow(
            listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = SESSION_ID,
                    exerciseId = "back-squat",
                    position = 0,
                    exerciseName = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    equipment = Equipment.BARBELL,
                    muscleFeel = 8,
                    joints = listOf(JointPain(Joint.SHOULDER, Side.LEFT, 4)),
                ),
            ),
        )
        val sets = MutableStateFlow<List<SetEntry>>(emptyList())

        override fun observeSession(sessionId: String): Flow<WorkoutSession?> = session
        override fun observeSessionExercises(sessionId: String): Flow<List<SessionExercise>> = exercises
        override fun observeSets(sessionId: String): Flow<List<SetEntry>> = sets
        override fun observeActiveSession(): Flow<WorkoutSession?> = flowOf(null)
        override fun observeHistory(): Flow<List<WorkoutSummary>> = flowOf(emptyList())
        override suspend fun startOrResumeSession(
            templateId: String?,
        ): DataResult<StartedSession> =
            unused()

        override suspend fun repeatSession(sessionId: String): DataResult<StartedSession> = unused()

        override suspend fun addExercise(sessionId: String, exerciseId: String): DataResult<Unit> =
            unused()

        override suspend fun removeExercise(sessionExerciseId: String): DataResult<Unit> = unused()
        override suspend fun moveExercise(sessionExerciseId: String, delta: Int): DataResult<Unit> = unused()
        override suspend fun finishExercise(sessionExerciseId: String): DataResult<Unit> = unused()
        override suspend fun reopenExercise(sessionExerciseId: String): DataResult<Unit> = unused()
        override suspend fun rateExercise(
            sessionExerciseId: String,
            muscleFeel: Int?,
            joints: List<JointPain>,
        ): DataResult<Unit> = unused()

        override suspend fun personalRecords(
            exerciseId: String,
            excludingSessionId: String?,
        ): DataResult<PersonalRecords> = DataResult.Success(records)

        /** Every group write, as the ids it covered — one entry per call, which is the point. */
        val supersetGroups = mutableListOf<List<String>>()

        /** What the record read answers with; empty means no records yet. */
        var records: PersonalRecords = PersonalRecords()

        override suspend fun setSupersetGroup(
            sessionExerciseIds: List<String>,
            group: Int?,
        ): DataResult<Unit> {
            supersetGroups += sessionExerciseIds
            return DataResult.Success(Unit)
        }

        override suspend fun finishSession(sessionId: String): DataResult<Unit> = unused()
        override suspend fun setReadiness(sessionId: String, note: String?): DataResult<Unit> = unused()

        override suspend fun setWorkoutNotes(sessionId: String, note: String?): DataResult<Unit> =
            unused()

        /** An activity is a write like any other; this fake only has to accept it (N141). */
        override suspend fun setSessionActivity(sessionId: String, activity: Activity?): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun deleteSession(sessionId: String): DataResult<Unit> = unused()
        override suspend fun logSet(
            sessionExerciseId: String,
            reps: Int,
            weightGrams: Long,
            rpeHalves: Int?,
            note: String?,
            setType: com.example.androidapp.domain.model.SetType,
            assistanceGrams: Long,
        ): DataResult<Unit> = unused()

        override suspend fun updateSet(
            setId: String,
            reps: Int,
            weightGrams: Long,
            rpeHalves: Int?,
            note: String?,
            setType: com.example.androidapp.domain.model.SetType,
            assistanceGrams: Long,
        ): DataResult<Unit> = unused()

        override suspend fun deleteSet(setId: String): DataResult<Unit> = unused()
        override suspend fun previousPerformance(
            exerciseId: String,
            currentSessionId: String,
        ): DataResult<PreviousPerformance> =
            DataResult.Success(PreviousPerformance(emptyList()))

        override suspend fun startRest(seconds: Int) = unused()
        override suspend fun adjustRest(deltaSeconds: Int) = unused()
        override suspend fun clearRest(): DataResult<Unit> = unused()

        private fun unused(): Nothing = error("this test does not write")
    }

    private companion object {
        const val SESSION_ID = "s1"
    }
}

/**
 * A plan store that records what a copy asked for (ROADMAP N31).
 *
 * Top level rather than nested in the test class: it is a helper, and the tests only need to see what
 * was written and what came back.
 */
private class FakeTemplateRepository(
    private val savedId: String = "t1",
    /** What the plan's name read answers with, so the title's live read can be asserted (B74). */
    private val templateName: String? = null,
) : TemplateRepository {

    /** Every copy asked for, as the session it came from and the name it was given. */
    val copied = mutableListOf<Pair<String, String>>()

    override suspend fun createTemplateFromSession(
        sessionId: String,
        name: String,
    ): DataResult<String> {
        copied += sessionId to name
        return DataResult.Success(savedId)
    }

    override suspend fun createTemplate(name: String): DataResult<String> = unused()
    override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> = unused()
    override suspend fun deleteTemplate(templateId: String): DataResult<Unit> = unused()
    override suspend fun setSupersetGroup(
        templateExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit> = unused()
    override suspend fun removeExercise(templateExerciseId: String): DataResult<Unit> = unused()

    override suspend fun prependSets(
        templateExerciseId: String,
        edits: List<TemplateSetEdit>,
    ): DataResult<Unit> = unused()

    override suspend fun addSet(templateExerciseId: String, edit: TemplateSetEdit): DataResult<Unit> =
        unused()

    override suspend fun updateSet(templateSetId: String, edit: TemplateSetEdit): DataResult<Unit> =
        unused()

    override suspend fun removeSet(templateSetId: String): DataResult<Unit> = unused()
    override suspend fun restoreSet(templateSetId: String): DataResult<Unit> = unused()
    override suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit> =
        unused()
    override suspend fun setExercisePlan(
        templateExerciseId: String,
        restSeconds: Int?,
        techniqueNote: String?,
        targetRpeHalves: Int?,
    ): DataResult<Unit> = unused()

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
    override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> = flowOf(null)
    override fun observeTemplateName(templateId: String): Flow<String?> = flowOf(templateName)
    override fun observeVersions(templateId: String): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
    override suspend fun keepVersion(templateId: String): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> = flowOf(emptyList())
    override fun observeSets(templateId: String): Flow<List<TemplateSet>> = flowOf(emptyList())

    private fun unused(): Nothing = error("this fake does not write")
}

/** A program repository that records the one write this screen makes (P3.18). */
private class FakeProgramRepository : ProgramRepository {
    val added = mutableListOf<Triple<String, String, DayOfWeek?>>()

    override fun observePrograms(): Flow<List<WorkoutProgram>> =
        flowOf(listOf(WorkoutProgram(id = "p1", name = "Upper/lower")))

    override fun observeProgram(programId: String): Flow<WorkoutProgram?> = flowOf(null)

    override fun observeActivePrograms(): Flow<List<WorkoutProgram>> = flowOf(emptyList())

    override fun observeSlots(programId: String): Flow<List<ProgramSlot>> = flowOf(emptyList())

    override fun observeProgramRun(programId: String): Flow<ProgramRun?> = flowOf(null)

    override fun observeSequenceSchedule(programId: String): Flow<SequenceSchedule?> = flowOf(null)

    override suspend fun addSlot(
        programId: String,
        templateId: String,
        weekday: DayOfWeek?,
    ): DataResult<Unit> {
        added += Triple(programId, templateId, weekday)
        return DataResult.Success(Unit)
    }

    override suspend fun setSubstitution(
        slotId: String,
        weekStart: LocalDate,
        templateId: String?,
    ): DataResult<Unit> = error("this screen does not substitute an occurrence")

    override suspend fun slotPreviousPerformance(
        slotId: String,
        exerciseId: String,
        currentSessionId: String,
        zone: ZoneId,
    ): DataResult<PreviousPerformance> = error("this screen does not read slot history")

    override suspend fun pendingOccurrences(
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<PendingOccurrence>> = error("this screen does not read the schedule")

    override suspend fun skipOccurrences(
        slotIds: List<String>,
        weekStart: LocalDate,
    ): DataResult<Unit> = error("this screen does not skip an occurrence")

    override suspend fun exportProgramDocument(programId: String): DataResult<String> =
        error("this screen does not export a program")

    override suspend fun importProgramDocument(text: String): DataResult<ProgramImportSummary> =
        error("this screen does not import a program")

    override suspend fun previewProgramDocument(text: String): DataResult<ProgramDocumentPreview> =
        error("this screen does not import a program")

    override suspend fun skipBlockSession(slotId: String, day: LocalDate): DataResult<Unit> =
        error("this screen does not skip a block")

    override suspend fun createProgram(name: String): DataResult<String> =
        error("this screen does not create a program")

    override suspend fun setProgramKind(
        programId: String,
        kind: ProgramKind,
    ): DataResult<Unit> = error("these tests do not change a program's kind")

    override suspend fun setProgramStartDate(
        programId: String,
        startDate: LocalDate?,
    ): DataResult<Unit> = error("these tests do not change a program's start")

    override suspend fun renameProgram(programId: String, name: String): DataResult<Unit> =
        error("this screen does not rename a program")

    override suspend fun deleteProgram(programId: String): DataResult<Unit> =
        error("this screen does not delete a program")

    override suspend fun activateProgram(programId: String): DataResult<Unit> =
        error("this screen does not activate a program")

    override suspend fun deactivateProgram(programId: String): DataResult<Unit> =
        error("this screen does not deactivate a program")

    override suspend fun moveProgram(programId: String, delta: Int): DataResult<Unit> =
        error("this screen does not move a program")

    override suspend fun setSlotWeekday(slotId: String, weekday: DayOfWeek?): DataResult<Unit> =
        error("this screen does not reschedule a slot")

    override suspend fun moveSlot(slotId: String, delta: Int): DataResult<Unit> =
        error("this screen does not move a slot")

    override suspend fun removeSlot(slotId: String): DataResult<Unit> =
        error("this screen does not remove a slot")
}
