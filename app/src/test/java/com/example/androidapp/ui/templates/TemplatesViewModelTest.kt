package com.example.androidapp.ui.templates

import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.workout.FakeWorkoutRepository
import com.example.androidapp.domain.repository.TemplateRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplatesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** `uiState` uses `WhileSubscribed`, so nothing flows until it is collected. */
    private fun TestScope.observe(viewModel: TemplatesViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun theList_comesFromTheRepository() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            templates.value = listOf(
                WorkoutTemplate(id = "t1", name = "Push day", exerciseCount = 5),
            )
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Push day"), state.templates.map { it.name })
    }

    @Test
    fun aHiddenWorkout_isNotDrawn_andTheFilterSaysHowManyAre() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            templates.value = listOf(
                WorkoutTemplate(id = "t1", name = "Push day"),
                WorkoutTemplate(id = "t2", name = "Autumn plan", hiddenAt = Instant.EPOCH),
            )
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("only the offered workout is drawn", listOf("Push day"), state.templates.map { it.name })
        assertEquals("and the chip can say there is one", 1, state.hiddenCount)
        assertEquals("with the list on All", TemplatesFilter.ALL, state.filter)
    }

    @Test
    fun theHiddenChip_drawsOnlyTheHiddenOnes() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            templates.value = listOf(
                WorkoutTemplate(id = "t1", name = "Push day"),
                WorkoutTemplate(id = "t2", name = "Autumn plan", hiddenAt = Instant.EPOCH),
            )
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onFilterChange(TemplatesFilter.HIDDEN)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TemplatesFilter.HIDDEN, state.filter)
        assertEquals(
            "the group, and not the live workout beside it (ROADMAP N217)",
            listOf("Autumn plan"),
            state.templates.map { it.name },
        )
        assertEquals(1, state.hiddenCount)
    }

    @Test
    fun theDeletedChip_drawsOnlyTheDeletedOnes_andTheCountsComeFromTheLibrary() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            templates.value = listOf(
                WorkoutTemplate(id = "t1", name = "Push day"),
                WorkoutTemplate(id = "t2", name = "Autumn plan", hiddenAt = Instant.EPOCH),
            )
            deleted.value = listOf(WorkoutTemplate(id = "t3", name = "Test", exerciseCount = 5))
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onFilterChange(TemplatesFilter.DELETED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Test"), state.templates.map { it.name })
        assertEquals("the hidden count is still the library's", 1, state.hiddenCount)
        assertEquals(1, state.deletedCount)
    }

    @Test
    fun restoring_writesTheChoice_andReturnsTheWorkoutToAll() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            deleted.value = listOf(WorkoutTemplate(id = "t3", name = "Test", exerciseCount = 5))
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()
        viewModel.onFilterChange(TemplatesFilter.DELETED)
        advanceUntilIdle()

        viewModel.onRestore("t3")
        advanceUntilIdle()

        assertEquals(listOf("t3"), repository.restoredIds)
        val state = viewModel.uiState.value
        assertEquals(
            "the last restore empties the group, so the list falls back (N217)",
            TemplatesFilter.ALL,
            state.filter,
        )
        assertEquals("and the workout is live again", listOf("t3"), state.templates.map { it.id })
        assertEquals(0, state.deletedCount)
    }

    @Test
    fun aFailedRestore_isSurfaced_ratherThanSwallowed() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            failWrites = true
            deleted.value = listOf(WorkoutTemplate(id = "t3", name = "Test"))
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onRestore("t3")
        advanceUntilIdle()

        assertNotNull("the failure reaches the screen's message host", viewModel.uiState.value.error)
    }

    @Test
    fun unhide_writesTheChoice_andReturnsTheWorkoutToTheList() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            templates.value = listOf(
                WorkoutTemplate(id = "t2", name = "Autumn plan", hiddenAt = Instant.EPOCH),
            )
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onUnhide("t2")
        advanceUntilIdle()

        assertEquals(listOf("t2" to false), repository.hiddenWrites)
        val state = viewModel.uiState.value
        assertEquals("the row is back", listOf("t2"), state.templates.map { it.id })
        assertEquals("and nothing is hidden any more", 0, state.hiddenCount)
    }

    @Test
    fun aFailedUnhide_isSurfaced_ratherThanSwallowed() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply {
            failWrites = true
            templates.value = listOf(
                WorkoutTemplate(id = "t2", name = "Autumn plan", hiddenAt = Instant.EPOCH),
            )
        }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onUnhide("t2")
        advanceUntilIdle()

        assertNotNull("the failure reaches the screen's message host", viewModel.uiState.value.error)
    }

    @Test
    fun aRunningWorkout_isReported_soTheListCanDisableStart() = runTest(dispatcher) {
        // ROADMAP N78: the list disables its Start controls for as long as a session is open, which is
        // the one thing it needs the workout repository for.
        val viewModel = TemplatesViewModel(FakeTemplateRepository(), FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasActiveWorkout)
    }

    @Test
    fun withNoWorkoutRunning_nothingIsHeldBack() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository().apply { session.value = null }
        val viewModel = TemplatesViewModel(FakeTemplateRepository(), workouts)
        observe(viewModel)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasActiveWorkout)
    }

    @Test
    fun creating_reportsTheNewId_soTheListCanOpenItsEditor() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply { createdId = "t9" }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()

        assertEquals("New template", repository.createdName)
        assertEquals("t9", viewModel.createdTemplateId.value)
    }

    @Test
    fun theHandledId_isCleared_soARecompositionCannotReopenTheEditor() = runTest(dispatcher) {
        val repository = FakeTemplateRepository()
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()
        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()

        viewModel.onCreatedHandled()

        assertNull(viewModel.createdTemplateId.value)
    }

    @Test
    fun aFailedCreate_surfacesTheError_insteadOfPretendingItSaved() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply { failWrites = true }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertNull("nothing was created, so nothing may be opened", viewModel.createdTemplateId.value)
    }

    @Test
    fun theErrorCanBeCleared_onceItHasBeenShown() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply { failWrites = true }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()
        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()

        viewModel.onErrorShown()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aSuccessfulCreate_clearsAnEarlierError() = runTest(dispatcher) {
        val repository = FakeTemplateRepository().apply { failWrites = true }
        val viewModel = TemplatesViewModel(repository, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()
        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        repository.failWrites = false
        viewModel.onCreateTemplate("New template")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertTrue(repository.templates.value.isNotEmpty() || viewModel.createdTemplateId.value != null)
    }

    private class FakeTemplateRepository : TemplateRepository {
        val templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())

        /** The rows in the deleted group (ROADMAP N216), apart from the live ones as the reads are. */
        val deleted = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
        val exercises = MutableStateFlow<List<TemplateExercise>>(emptyList())
        var createdId = "t-new"
        var createdName: String? = null
        var failWrites = false

        /** Every hide or unhide, as the id and the state asked for (ROADMAP N214). */
        val hiddenWrites = mutableListOf<Pair<String, Boolean>>()

        /** Every restore, in the order it was asked for (ROADMAP N216). */
        val restoredIds = mutableListOf<String>()

        override fun observeTemplates(includeHidden: Boolean): Flow<List<WorkoutTemplate>> = templates

        override fun observeDeletedTemplates(): Flow<List<WorkoutTemplate>> = deleted

        override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> =
            templates.map { rows -> rows.firstOrNull { it.id == templateId } }

        override fun observeTemplateName(templateId: String): Flow<String?> =
            templates.map { rows -> rows.firstOrNull { it.id == templateId }?.name }

        override fun observeVersions(templateId: String): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override suspend fun keepVersion(templateId: String): DataResult<Unit> = DataResult.Success(Unit)

        override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> = exercises
        override fun observeSets(templateId: String): Flow<List<TemplateSet>> = flowOf(emptyList())
        /** Recorded as one call, since writing the ramp in front is a single write (B34). */
        val prependedSets = mutableListOf<Pair<String, List<TemplateSetEdit>>>()

        override suspend fun prependSets(
            templateExerciseId: String,
            edits: List<TemplateSetEdit>,
        ): DataResult<Unit> {
            prependedSets += templateExerciseId to edits
            return DataResult.Success(Unit)
        }

        override suspend fun addSet(
            templateExerciseId: String,
            edit: TemplateSetEdit,
        ): DataResult<Unit> {
            error("these tests do not write a plan")
        }

        override suspend fun updateSet(
            templateSetId: String,
            edit: TemplateSetEdit,
        ): DataResult<Unit> {
            error("these tests do not write a plan")
        }

        override suspend fun removeSet(templateSetId: String): DataResult<Unit> {
            error("these tests do not write a plan")
        }
        override suspend fun restoreSet(templateSetId: String): DataResult<Unit> {
            error("these tests do not write a plan")
        }
        override suspend fun setExercisePlan(
            templateExerciseId: String,
            restSeconds: Int?,
            techniqueNote: String?,
            targetRpeHalves: Int?,
        ): DataResult<Unit> {
            error("these tests do not write a plan")
        }

        override suspend fun createTemplateFromSession(
            sessionId: String,
            name: String,
        ): DataResult<String> = DataResult.Success("t1")

        override suspend fun createTemplate(name: String): DataResult<String> {
            if (failWrites) return DataResult.Failure(DataError.Storage(IOException("disk full")))
            createdName = name
            templates.value = templates.value + WorkoutTemplate(id = createdId, name = name)
            return DataResult.Success(createdId)
        }

        override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> =
            writeFailureOrSuccess()

        override suspend fun setSupersetGroup(
            templateExerciseIds: List<String>,
            group: Int?,
        ): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun deleteTemplate(templateId: String): DataResult<Unit> {
            val result = writeFailureOrSuccess()
            if (result is DataResult.Success) {
                val row = templates.value.firstOrNull { it.id == templateId } ?: return result
                templates.value = templates.value.filterNot { it.id == templateId }
                deleted.value = deleted.value + row
            }
            return result
        }

        override suspend fun restoreTemplate(templateId: String): DataResult<Unit> {
            val result = writeFailureOrSuccess()
            if (result is DataResult.Success) {
                restoredIds += templateId
                val row = deleted.value.firstOrNull { it.id == templateId }
                if (row != null) {
                    deleted.value = deleted.value.filterNot { it.id == templateId }
                    templates.value = templates.value + row
                }
            }
            return result
        }

        override suspend fun setTemplateHidden(templateId: String, hidden: Boolean): DataResult<Unit> {
            val result = writeFailureOrSuccess()
            if (result is DataResult.Success) {
                hiddenWrites += templateId to hidden
                templates.value = templates.value.map { row ->
                    if (row.id == templateId) row.copy(hiddenAt = if (hidden) Instant.EPOCH else null) else row
                }
            }
            return result
        }

        override suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit> =
            writeFailureOrSuccess()

        override suspend fun removeExercise(templateExerciseId: String): DataResult<Unit> =
            writeFailureOrSuccess()

        override suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit> =
            writeFailureOrSuccess()

        private fun writeFailureOrSuccess(): DataResult<Unit> = if (failWrites) {
            DataResult.Failure(DataError.Storage(IOException("disk full")))
        } else {
            DataResult.Success(Unit)
        }
    }

}
