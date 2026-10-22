package com.example.androidapp.ui.programs

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.platform.DocumentIo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The program list's half of the edit path (ROADMAP N180).
 *
 * What a picked document means is the thing between the file and the write: an add goes straight
 * through, an edit stops at a question, and the answer decides whether the kept text is written. It
 * ran only on a device before, so a change that broke the confirmation or the write still left a
 * green `./gradlew`. [ProgramsViewModel.openDocument] is that decision without the content resolver;
 * the read itself is [DocumentIo]'s, and its own test is instrumented because it is about Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: FakeProgramRepository) =
        ProgramsViewModel(repository, DocumentIo())

    @Test
    fun anAdd_isMergedWithoutAQuestion() = runTest(dispatcher) {
        val repository = FakeProgramRepository()
        val model = newViewModel(repository)

        model.openDocument("add")
        advanceUntilIdle()

        assertThat(model.pendingEdit.value).isNull()
        assertThat(model.imported.value).isEqualTo(DataResult.Success(repository.summary))
        assertThat(model.edited.value).isFalse()
        assertThat(repository.importedTexts).containsExactly("add")
    }

    @Test
    fun anEdit_stopsAtAPendingPreview_andWritesNothing() = runTest(dispatcher) {
        val repository = FakeProgramRepository(preview = DataResult.Success(anEdit()))
        val model = newViewModel(repository)

        model.openDocument("edit")
        advanceUntilIdle()

        assertThat(model.pendingEdit.value?.text).isEqualTo("edit")
        assertThat(model.pendingEdit.value?.preview?.programName).isEqualTo("Heavy lower")
        assertThat(model.imported.value).isNull()
        assertThat(repository.importedTexts).isEmpty()
    }

    @Test
    fun confirm_writesTheKeptText_andReportsAnUpdate() = runTest(dispatcher) {
        val repository = FakeProgramRepository(preview = DataResult.Success(anEdit()))
        val model = newViewModel(repository)
        model.openDocument("edit")
        advanceUntilIdle()

        model.confirmEdit()
        advanceUntilIdle()

        assertThat(model.pendingEdit.value).isNull()
        assertThat(repository.importedTexts).containsExactly("edit")
        assertThat(model.edited.value).isTrue()
        assertThat(model.imported.value).isEqualTo(DataResult.Success(repository.summary))
    }

    @Test
    fun dismiss_writesNothing() = runTest(dispatcher) {
        val repository = FakeProgramRepository(preview = DataResult.Success(anEdit()))
        val model = newViewModel(repository)
        model.openDocument("edit")
        advanceUntilIdle()

        model.dismissEdit()
        advanceUntilIdle()

        assertThat(model.pendingEdit.value).isNull()
        assertThat(repository.importedTexts).isEmpty()
        assertThat(model.imported.value).isNull()
    }

    @Test
    fun aRefusedPreview_reportsWithoutADialog() = runTest(dispatcher) {
        val refusal = DataResult.Failure(DataError.Invalid("this program changed on the phone"))
        val repository = FakeProgramRepository(preview = refusal)
        val model = newViewModel(repository)

        model.openDocument("stale")
        advanceUntilIdle()

        assertThat(model.pendingEdit.value).isNull()
        assertThat(model.imported.value).isEqualTo(refusal)
        assertThat(model.edited.value).isFalse()
        assertThat(repository.importedTexts).isEmpty()
    }

    private fun anEdit() = ProgramDocumentPreview.Edit(
        programName = "Heavy lower",
        changes = emptyList(),
        warnings = emptyList(),
        sharedProgramNames = emptyList(),
    )

    /**
     * Only the document path is real here: every other call fails loudly rather than answering
     * something plausible, because a test that substitutes a wrong answer is worse than one that
     * stops.
     */
    private class FakeProgramRepository(
        private val preview: DataResult<ProgramDocumentPreview> = DataResult.Success(ProgramDocumentPreview.Add),
    ) : ProgramRepository {

        var summary = ProgramImportSummary(
            programs = 1,
            templates = 1,
            exercises = 1,
            droppedMovements = 0,
        )

        /** Every text the merge was asked to write, in order. */
        val importedTexts = mutableListOf<String>()

        override suspend fun previewProgramDocument(text: String): DataResult<ProgramDocumentPreview> =
            preview

        override suspend fun importProgramDocument(text: String): DataResult<ProgramImportSummary> {
            importedTexts += text
            return DataResult.Success(summary)
        }

        override fun observePrograms(): Flow<List<WorkoutProgram>> = flowOf(emptyList())

        override fun observeProgram(programId: String): Flow<WorkoutProgram?> = flowOf(null)

        override fun observeActivePrograms(): Flow<List<WorkoutProgram>> = flowOf(emptyList())

        override fun observeSlots(programId: String): Flow<List<ProgramSlot>> = flowOf(emptyList())

        override fun observeProgramRun(programId: String): Flow<ProgramRun?> = flowOf(null)

        override suspend fun setSubstitution(
            slotId: String,
            weekStart: LocalDate,
            templateId: String?,
        ): DataResult<Unit> = error("these tests do not substitute an occurrence")

        override suspend fun slotPreviousPerformance(
            slotId: String,
            exerciseId: String,
            currentSessionId: String,
            zone: ZoneId,
        ): DataResult<PreviousPerformance> = error("these tests do not read slot history")

        override suspend fun pendingOccurrences(
            today: LocalDate,
            zone: ZoneId,
        ): DataResult<List<PendingOccurrence>> = error("these tests do not read the schedule")

        override suspend fun skipOccurrences(
            slotIds: List<String>,
            weekStart: LocalDate,
        ): DataResult<Unit> = error("these tests do not record a skip")

        override suspend fun exportProgramDocument(programId: String): DataResult<String> =
            error("these tests do not export a program")

        override suspend fun createProgram(name: String): DataResult<String> =
            error("these tests do not create a program")

        override suspend fun renameProgram(programId: String, name: String): DataResult<Unit> =
            error("these tests do not rename a program")

        override suspend fun deleteProgram(programId: String): DataResult<Unit> =
            error("these tests do not delete a program")

        override suspend fun activateProgram(programId: String): DataResult<Unit> =
            error("these tests do not activate a program")

        override suspend fun deactivateProgram(programId: String): DataResult<Unit> =
            error("these tests do not deactivate a program")

        override suspend fun moveProgram(programId: String, delta: Int): DataResult<Unit> =
            error("these tests do not move a program")

        override suspend fun addSlot(
            programId: String,
            templateId: String,
            weekday: DayOfWeek?,
        ): DataResult<Unit> = error("these tests do not add a slot")

        override suspend fun setSlotWeekday(slotId: String, weekday: DayOfWeek?): DataResult<Unit> =
            error("these tests do not schedule a slot")

        override suspend fun moveSlot(slotId: String, delta: Int): DataResult<Unit> =
            error("these tests do not move a slot")

        override suspend fun removeSlot(slotId: String): DataResult<Unit> =
            error("these tests do not remove a slot")
    }
}
