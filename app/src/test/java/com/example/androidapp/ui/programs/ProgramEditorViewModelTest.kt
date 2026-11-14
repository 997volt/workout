package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.testing.FakeDocumentIo
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeTrendsRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * The program editor's write and export paths (ROADMAP F7, N47, B159).
 *
 * The screen and the document encoder have their own tests. What is asserted here is the editor's half:
 * a refused write reaches the screen's error channel instead of looking applied, a delete marks the editor
 * gone only after the repository agreed, and an export is reported through a state that outlives the
 * composition — the rotation B159 fixed.
 *
 * The document write keeps its Android `Context`/`Uri` parameters; the work itself is the fake port's, so
 * what is under test is the ordering (produce, then write) and what the state says afterwards.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri: Uri = Uri.parse("content://test/program.json")
    private val repository = FakeProgramRepository()
    private val templates = FakeTemplateRepository()
    private val documentIo = FakeDocumentIo()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository.program.value = WorkoutProgram(id = PROGRAM_ID, name = "Upper/Lower")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
    ) = ProgramEditorViewModel(
        repository = repository,
        templateRepository = templates,
        stallFinder = PlanStallFinder(repository, templates, FakeTrendsRepository()),
        documentIo = documentIo,
        savedStateHandle = handle,
    )

    private fun planExercise() = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = null,
        equipment = null,
        stepGrams = 2_500L,
    )

    private fun planSet(id: String, index: Int) = TemplateSet(
        id = id,
        templateExerciseId = "te1",
        setIndex = index,
        targetWeightGrams = 100_000L,
        targetRepsMin = 5,
        targetRepsMax = 8,
    )

    private fun plannedSquat() {
        templates.exercises.value = mapOf("t1" to listOf(planExercise()))
        templates.sets.value = mapOf("t1" to (0 until 5).map { planSet("s$it", it) })
    }

    @Test
    fun thePerLiftDeload_waitsForTheConfirm_andTheUndoRestoresTheWholeEdit() = runTest(dispatcher) {
        plannedSquat()
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeloadLift(PlanStall("Back Squat", 3, "t1", "te1", "squat"))
        advanceUntilIdle()

        // The confirm is the whole plan: five working sets become three, with the last loads three steps down.
        assertThat(viewModel.deloadPrompt.value?.kept?.map { it.before.id })
            .containsExactly("s0", "s1", "s2").inOrder()
        assertThat(viewModel.deloadPrompt.value?.removedCount).isEqualTo(2)
        assertThat(viewModel.deloadPrompt.value?.kept?.first()?.after?.weightGrams).isEqualTo(92_500L)
        assertWithMessage("nothing is written before the confirm").that(templates.deloads).isEmpty()

        viewModel.onConfirmDeload()
        advanceUntilIdle()

        assertThat(templates.deloads).hasSize(1)
        assertThat(viewModel.deloadPrompt.value).isNull()
        assertThat(viewModel.deloadUndo.value?.removedSetIds).containsExactly("s3", "s4").inOrder()
        assertThat(viewModel.deloadUndo.value?.keptBefore?.map { it.id })
            .containsExactly("s0", "s1", "s2").inOrder()

        viewModel.onUndoDeload()
        advanceUntilIdle()

        assertWithMessage("one removed id restores the removal")
            .that(templates.restoredSets).containsExactly("s3")
        assertWithMessage("the kept targets are rewritten to their before-state")
            .that(templates.updatedSets.map { it.first }).containsExactly("s0", "s1", "s2").inOrder()
        assertThat(templates.updatedSets.first().second.targetWeightGrams).isEqualTo(100_000L)
    }

    @Test
    fun theSharedWarning_isShownOnlyWhenAnotherProgramNamesThePlan() = runTest(dispatcher) {
        plannedSquat()
        repository.otherProgramNames = listOf("Full Body")
        val viewModel = viewModel()

        viewModel.onDeloadLift(PlanStall("Back Squat", 3, "t1", "te1", "squat"))
        advanceUntilIdle()

        assertThat(viewModel.deloadPrompt.value?.sharedProgramNames).containsExactly("Full Body")
    }

    @Test
    fun theHandOff_opensTheConfirmOnce_andClearsItself() = runTest(dispatcher) {
        plannedSquat()
        val handle = SavedStateHandle(
            mapOf(
                "programId" to PROGRAM_ID,
                "deloadTemplateId" to "t1",
                "deloadTemplateExerciseId" to "te1",
            ),
        )
        val viewModel = viewModel(handle)
        advanceUntilIdle()

        assertThat(viewModel.deloadPrompt.value?.templateExerciseId).isEqualTo("te1")
        assertWithMessage("the one-shot target is cleared so a recreation does not re-open it")
            .that(handle.get<String>("deloadTemplateExerciseId"))
            .isNull()
    }

    @Test
    fun aRefusedWrite_isReported_andAcknowledgingItClearsTheMessage() = runTest(dispatcher) {
        repository.renameResult = DataResult.Failure(DataError.Invalid("the name is blank"))
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRename("")
        advanceUntilIdle()

        assertThat(repository.renames).containsExactly(PROGRAM_ID to "")
        assertThat(viewModel.uiState.value.error).isEqualTo(DataError.Invalid("the name is blank"))
        assertWithMessage("the program is still there")
            .that(viewModel.uiState.value.program?.name)
            .isEqualTo("Upper/Lower")

        viewModel.onErrorShown()
        advanceUntilIdle()
        assertWithMessage("an acknowledged failure is not reported twice")
            .that(viewModel.uiState.value.error)
            .isNull()
    }

    @Test
    fun deletingTheProgram_marksTheEditorGoneOnlyWhenTheWriteSucceeded() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        repository.deleteResult = DataResult.Failure(DataError.Storage(IOException("locked")))
        viewModel.onDeleteProgram()
        advanceUntilIdle()

        assertWithMessage("a refused delete must leave the editor open")
            .that(viewModel.deleted.value)
            .isFalse()
        assertThat(viewModel.uiState.value.error).isInstanceOf(DataError.Storage::class.java)

        repository.deleteResult = DataResult.Success(Unit)
        viewModel.onDeleteProgram()
        advanceUntilIdle()

        assertThat(viewModel.deleted.value).isTrue()
        assertWithMessage("a successful write clears the old message")
            .that(viewModel.uiState.value.error)
            .isNull()
        assertThat(repository.deletions).containsExactly(PROGRAM_ID, PROGRAM_ID).inOrder()
    }

    @Test
    fun writingTheDocument_reportsTheWrite_andAFailedExportWritesNoFile() = runTest(dispatcher) {
        repository.exportResult = DataResult.Success("{\"program\":\"Upper/Lower\"}")
        val viewModel = viewModel()

        viewModel.writeDocument(context, uri)
        advanceUntilIdle()

        assertThat(viewModel.exported.value).isEqualTo(DataResult.Success(Unit))
        assertThat(documentIo.written).containsExactly("{\"program\":\"Upper/Lower\"}")

        viewModel.onExportShown()
        assertWithMessage("the report is not said twice").that(viewModel.exported.value).isNull()

        repository.exportResult = DataResult.Failure(DataError.NotFound)
        viewModel.writeDocument(context, uri)
        advanceUntilIdle()

        assertThat(viewModel.exported.value).isEqualTo(DataResult.Failure(DataError.NotFound))
        assertWithMessage("nothing is written when there is no document")
            .that(documentIo.written)
            .hasSize(1)
    }

    @Test
    fun addingADay_carriesTheAuthoredWeekTheLifterIsLookingAt() = runTest(dispatcher) {
        // ROADMAP N292: the editor shows one week at a time, so the week travels with the tap rather
        // than being inferred from the schedule's current week.
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAddSlot(templateId = "t2", phaseWeek = 2)
        advanceUntilIdle()

        assertThat(repository.addedSlots)
            .containsExactly(Triple(PROGRAM_ID, "t2", 2))
    }

    @Test
    fun removingAPhaseWeek_isOneWrite_andItsFailureIsReported() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        repository.writeResult = DataResult.Failure(DataError.Storage(IOException("locked")))
        viewModel.onRemovePhaseWeek(2)
        advanceUntilIdle()

        assertThat(repository.removedWeeks).containsExactly(PROGRAM_ID to 2)
        assertThat(viewModel.uiState.value.error).isInstanceOf(DataError.Storage::class.java)

        repository.writeResult = DataResult.Success(Unit)
        viewModel.onRemovePhaseWeek(2)
        advanceUntilIdle()

        assertWithMessage("a successful write clears the old message")
            .that(viewModel.uiState.value.error)
            .isNull()
    }

    private companion object {
        const val PROGRAM_ID = "p1"
    }

    @Test
    fun theReplace_setsTheUndo_andTheUndoWritesItBack() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "programId" to PROGRAM_ID,
                "replaceOldId" to "old",
                "replaceNewId" to "new",
                "replaceSetIds" to arrayListOf("s0", "s1"),
            ),
        )
        val viewModel = viewModel(handle)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.replaceUndo.value?.outcome?.oldTemplateExerciseId).isEqualTo("old")

        viewModel.onUndoReplace()
        advanceUntilIdle()

        assertThat(templates.undoneReplacements.map { it.oldTemplateExerciseId })
            .containsExactly("old")
        assertThat(viewModel.replaceUndo.value).isNull()
    }

    @Test
    fun theReplaceHandOff_opensOnce_andClearsItself() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "programId" to PROGRAM_ID,
                "replaceTemplateId" to "t1",
                "replaceTemplateExerciseId" to "te1",
            ),
        )
        val viewModel = viewModel(handle)
        advanceUntilIdle()

        assertThat(viewModel.replaceTarget.value?.templateExerciseId).isEqualTo("te1")
        assertThat(handle.get<String>("replaceTemplateExerciseId")).isNull()
        viewModel.onReplaceTargetHandled()
        assertThat(viewModel.replaceTarget.value).isNull()
    }
}
