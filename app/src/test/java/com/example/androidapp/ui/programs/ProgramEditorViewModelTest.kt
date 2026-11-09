package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
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

    private fun viewModel() = ProgramEditorViewModel(
        repository = repository,
        templateRepository = templates,
        stallFinder = PlanStallFinder(repository, templates, FakeTrendsRepository()),
        documentIo = documentIo,
        savedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
    )

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
}
