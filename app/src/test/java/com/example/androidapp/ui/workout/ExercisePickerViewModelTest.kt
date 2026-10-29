package com.example.androidapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.libraryRows
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.util.UUID
/**
 * Under Robolectric rather than plain JVM: the ViewModel reads its route argument
 * out of a `SavedStateHandle`, and that path touches a real `android.os.Bundle`
 * (the same reason [com.example.androidapp.ui.exercises.ExerciseDetailViewModelTest]
 * is).
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * The picker's target is a route argument (ROADMAP N3), so the fake handle
     * carries `templateId` under the name `toRoute` reads it by.
     */
    private fun viewModelFor(
        exercises: ExerciseRepository = FakeExerciseRepository(),
        workouts: WorkoutRepository = FakeWorkoutRepository(),
        templates: TemplateRepository = FakeTemplateRepository(),
        templateId: String? = null,
    ) = ExercisePickerViewModel(
        exercises,
        workouts,
        templates,
        SavedStateHandle(mapOf("templateId" to templateId)),
    )

    /** `uiState` uses `WhileSubscribed`, so nothing flows until it is collected. */
    private fun kotlinx.coroutines.test.TestScope.observe(viewModel: ExercisePickerViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun creatingAnExercise_storesIt_andAddsItToTheSession() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository()
        val workouts = FakeWorkoutRepository()
        val viewModel = viewModelFor(exercises, workouts)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("  Sled Push  ")
        advanceUntilIdle()

        // Created from the name alone, as an unedited custom exercise (N2).
        assertEquals("Sled Push", exercises.created?.name)
        assertEquals(MuscleGroup.OTHER, exercises.created?.primaryMuscle)
        assertEquals(Equipment.OTHER, exercises.created?.equipment)
        assertEquals(MovementPattern.OTHER, exercises.created?.movementPattern)

        // And appended to the open workout, so the picker can pop.
        assertEquals(listOf("s1" to exercises.created?.id), workouts.added)
        assertTrue(viewModel.added.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun theList_putsTheMovementsUnderTheirCategory() = runTest(dispatcher) {
        // ROADMAP N95, N110: the picker draws the library's shape, so a lifter who keeps their bench lifts in
        // one family browses them together here too rather than scrolling a hundred movements for one. What is
        // *offered* is still a movement alone — a category is never logged — which the list enforces by sending
        // a head's tap to the fold rather than to the choice.
        val exercises = FakeExerciseRepository(
            listOf(
                movement("back-squat", "Back Squat"),
                category("cat-bench", "Bench Press"),
                movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench"),
            ),
        )
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows()

        assertEquals(
            "families first, each with its movements under it, then the movements filed under nothing",
            listOf("Bench Press", "Barbell Bench Press", "Back Squat"),
            rows.map { it.name },
        )
        assertEquals(
            "the head is the one row that is not a movement, and it sits at the top",
            listOf("Bench Press"),
            rows.filter { it.isCategory }.map { it.name },
        )
        assertEquals(
            "the movement filed under it is drawn one step in",
            1,
            rows.single { it.id == "barbell-bench-press" }.depth,
        )
        assertEquals(0, rows.single { it.id == "back-squat" }.depth)
    }

    @Test
    fun aMovementFiledUnderACategory_isNested_neverFlatBesideIt() = runTest(dispatcher) {
        // N110's shape on the picker's own screen: one list browses the families and chooses from them, so a
        // movement's depth is what says it belongs to the head above it.
        val exercises = FakeExerciseRepository(
            listOf(
                category("cat-bench", "Bench Press"),
                movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench"),
            ),
        )
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows()

        assertEquals(listOf(0, 1), rows.map { it.depth })
        assertEquals(listOf("Bench Press", "Barbell Bench Press"), rows.map { it.name })
    }

    @Test
    fun theList_isFilteredByTheQuery() = runTest(dispatcher) {
        // B79: the flat branch ignored its query, so the picker's search box did nothing at all — the whole
        // movements list stayed on screen however much was typed.
        val exercises = FakeExerciseRepository(
            listOf(
                movement("back-squat", "Back Squat"),
                movement("bench", "Barbell Bench Press"),
            ),
        )
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("back squat")
        advanceUntilIdle()

        assertEquals(listOf("Back Squat"), viewModel.uiState.value.rows().map { it.name })
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun aQueryThatMatchesNothing_isASearchMiss_notAnEmptyLibrary() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository(listOf(movement("back-squat", "Back Squat")))
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertFalse("the library is not empty; the search just missed", state.libraryIsEmpty)
    }

    @Test
    fun aLibraryWithNoMovements_readsAsEmpty_ratherThanAsAFailedSearch() = runTest(dispatcher) {
        // B79's other half: the picker's empty state hardcoded `libraryIsEmpty = false`, so an empty library
        // was reported as a search that found nothing.
        val exercises = FakeExerciseRepository(listOf(category("cat-bench", "Bench Press")))
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertTrue(viewModel.uiState.value.libraryIsEmpty)
    }

    private fun movement(id: String, name: String, parent: String? = null) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        parentId = parent,
    )

    private fun category(id: String, name: String) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.OTHER,
        movementPattern = MovementPattern.OTHER,
        rowKind = RowKind.CATEGORY,
    )

    /**
     * The rows the picker draws, derived the way the screen derives them (ROADMAP N95).
     *
     * Without folds, which is the picker's own rule while a query is typed and the half this test owns: the
     * state holds the library and the screen holds the folds, so a ViewModel test asserts the grouping.
     */
    private fun ExercisePickerUiState.rows() = libraryRows(exercises, query)

    @Test
    fun aFailedCreate_reportsIt_andAddsNothing() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository().apply { failCreates = true }
        val workouts = FakeWorkoutRepository()
        val viewModel = viewModelFor(exercises, workouts)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("Sled Push")
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertTrue(workouts.added.isEmpty())
        assertFalse(viewModel.added.value)
    }

    @Test
    fun withNoOpenSession_theAppendFails_ratherThanFilingItUnderNothing() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository()
        val workouts = FakeWorkoutRepository().apply { session.value = null }
        val viewModel = viewModelFor(exercises, workouts)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("Sled Push")
        advanceUntilIdle()

        // The library row still exists; only the append failed.
        assertNotNull(exercises.created)
        assertEquals(DataError.NotFound, viewModel.error.value)
        assertFalse(viewModel.added.value)
    }

    @Test
    fun aFailedAppend_isReported() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository()
        val workouts = FakeWorkoutRepository().apply { failAdds = true }
        val viewModel = viewModelFor(exercises, workouts)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("Sled Push")
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertFalse(viewModel.added.value)
    }

    @Test
    fun pickingAnExistingExercise_alsoReportsAFailedAppend() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository(listOf(seeded))
        val workouts = FakeWorkoutRepository().apply { failAdds = true }
        val viewModel = viewModelFor(exercises, workouts)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertNotNull("a dropped append must not be silent", viewModel.error.value)
        assertFalse(viewModel.added.value)
    }

    @Test
    fun twoTapsOnAMovement_beforeTheFirstLands_appendItOnce() = runTest(dispatcher) {
        // B273: the picker closes on `added`, which the first tap only publishes after its write. A
        // second tap inside that window is the same movement, not a second plan row.
        val exercises = FakeExerciseRepository(listOf(seeded))
        val templates = FakeTemplateRepository()
        val viewModel = viewModelFor(exercises, FakeWorkoutRepository(), templates, templateId = "t1")
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertEquals("one tap and its echo are one append", listOf("t1" to "back-squat"), templates.added)
    }

    @Test
    fun pickingForATemplate_appendsToTheTemplate_insteadOfTheSession() = runTest(dispatcher) {
        // The same picker fills a template (N3): only the destination changes.
        val exercises = FakeExerciseRepository(listOf(seeded))
        val workouts = FakeWorkoutRepository()
        val templates = FakeTemplateRepository()
        val viewModel = viewModelFor(exercises, workouts, templates, templateId = "t1")
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertEquals(listOf("t1" to "back-squat"), templates.added)
        assertTrue(workouts.added.isEmpty())
        assertTrue(viewModel.added.value)
    }

    @Test
    fun addingToATemplate_prefillsItsSetsFromTheLastSession() = runTest(dispatcher) {
        // P3.18: the last time the lift was trained becomes the plan's first targets. The warm-up is not
        // carried — a plan prescribes the work — and the numbers are the performed ones, editable.
        val workouts = FakeWorkoutRepository().apply {
            previous = PreviousPerformance(
                listOf(
                    SetEntry(
                        id = "s1",
                        sessionExerciseId = "se1",
                        setIndex = 0,
                        reps = 10,
                        weightGrams = 60_000L,
                        setType = SetType.WARMUP,
                    ),
                    SetEntry(
                        id = "s2",
                        sessionExerciseId = "se1",
                        setIndex = 1,
                        reps = 5,
                        weightGrams = 100_000L,
                    ),
                    SetEntry(
                        id = "s3",
                        sessionExerciseId = "se1",
                        setIndex = 2,
                        reps = 8,
                        weightGrams = 95_000L,
                    ),
                ),
            )
        }
        val templates = FakeTemplateRepository().apply {
            planned = TemplateExercise(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                exerciseName = "Back Squat",
                primaryMuscle = null,
                equipment = null,
            )
        }
        val viewModel = viewModelFor(
            FakeExerciseRepository(listOf(seeded)),
            workouts,
            templates,
            templateId = "t1",
        )
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertEquals(2, templates.prefilled.size)
        assertEquals("te1", templates.prefilled[0].first)
        assertEquals(100_000L, templates.prefilled[0].second.targetWeightGrams)
        assertEquals(5, templates.prefilled[0].second.targetRepsMin)
        assertEquals(95_000L, templates.prefilled[1].second.targetWeightGrams)
        assertEquals(8, templates.prefilled[1].second.targetRepsMax)
    }

    @Test
    fun aTargetThatCannotBeWritten_isReported_andTheExerciseIsNotAddedTwice() = runTest(dispatcher) {
        // N168: the append is the action that matters, so it stands; but a storage failure while filling
        // its targets used to vanish. It is reported now, the picker does not claim a clean add, and a
        // second tap is answered as the add that already happened rather than a duplicate plan row.
        val workouts = FakeWorkoutRepository().apply {
            previous = PreviousPerformance(
                listOf(
                    SetEntry(
                        id = "s1",
                        sessionExerciseId = "se1",
                        setIndex = 0,
                        reps = 5,
                        weightGrams = 100_000L,
                    ),
                ),
            )
        }
        val templates = FakeTemplateRepository().apply {
            planned = TemplateExercise(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                exerciseName = "Back Squat",
                primaryMuscle = null,
                equipment = null,
            )
            failSets = true
        }
        val viewModel = viewModelFor(
            FakeExerciseRepository(listOf(seeded)),
            workouts,
            templates,
            templateId = "t1",
        )
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertNotNull("the failed prefill is reported", viewModel.error.value)
        assertFalse("and the add is not claimed as complete", viewModel.added.value)
        assertEquals("the exercise itself still landed", 1, templates.added.size)

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertEquals("a second tap does not duplicate the plan row", 1, templates.added.size)
    }

    @Test
    fun aNewExercise_canBeCreatedStraightIntoATemplate() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository()
        val templates = FakeTemplateRepository()
        val viewModel = viewModelFor(exercises, templates = templates, templateId = "t1")
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("Sled Push")
        advanceUntilIdle()

        assertEquals(listOf("t1" to exercises.created?.id), templates.added)
        assertTrue(viewModel.added.value)
    }

    @Test
    fun aFailedTemplateAppend_reportsIt_ratherThanPopping() = runTest(dispatcher) {
        val templates = FakeTemplateRepository().apply { failAdds = true }
        val viewModel = viewModelFor(templates = templates, templateId = "t1")
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onExerciseSelected("back-squat")
        advanceUntilIdle()

        assertNotNull(viewModel.error.value)
        assertFalse(viewModel.added.value)
    }

    @Test
    fun theErrorCanBeCleared_onceItHasBeenShown() = runTest(dispatcher) {
        val exercises = FakeExerciseRepository().apply { failCreates = true }
        val viewModel = viewModelFor(exercises, FakeWorkoutRepository())
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCreateExercise("Sled Push")
        advanceUntilIdle()
        viewModel.onErrorShown()

        assertNull(viewModel.error.value)
    }

    private class FakeExerciseRepository(
        initial: List<Exercise> = emptyList(),
    ) : ExerciseRepository {
        private val state = MutableStateFlow(initial)
        var created: Exercise? = null
        var failCreates = false

        override fun observeExercises(): Flow<DataResult<List<Exercise>>> =
            state.map { DataResult.Success(it) }

        override suspend fun getExercise(id: String): DataResult<Exercise?> =
            DataResult.Success(state.value.firstOrNull { it.id == id })

        override suspend fun createCustomExercise(name: String): DataResult<Exercise> {
            if (failCreates) return DataResult.Failure(DataError.Storage(IOException("disk full")))
            val exercise = Exercise(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                primaryMuscle = MuscleGroup.OTHER,
                equipment = Equipment.OTHER,
                movementPattern = MovementPattern.OTHER,
            )
            created = exercise
            state.value = state.value + exercise
            return DataResult.Success(exercise)
        }

        /** A category is never offered by the picker, so nothing here should ever ask for one. */
        override suspend fun createCategory(name: String): DataResult<Exercise> =
            error("the picker must not create categories")

        // The library's last-trained read is not part of this screen (ROADMAP N230).
        override fun observeLastTrained(): Flow<DataResult<Map<String, Instant>>> =
            flowOf(DataResult.Success(emptyMap()))

        override suspend fun getAllIncludingDeleted(): DataResult<List<Exercise>> =
            DataResult.Success(state.value)

        override suspend fun updateExercise(exercise: Exercise): DataResult<Unit> =
            DataResult.Success(Unit)
    }

    /** Records which template an exercise landed in (ROADMAP N3). */
    private class FakeTemplateRepository : TemplateRepository {
        override suspend fun setSupersetGroup(
            templateExerciseIds: List<String>,
            group: Int?,
        ): DataResult<Unit> = DataResult.Success(Unit)
        val added = mutableListOf<Pair<String, String>>()
        var failAdds = false

        override fun observeTemplates(includeHidden: Boolean): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override fun observeDeletedTemplates(): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> = flowOf(null)
        override fun observeTemplateName(templateId: String): Flow<String?> = flowOf(null)
        override fun observeVersions(templateId: String): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override suspend fun keepVersion(templateId: String): DataResult<Unit> = DataResult.Success(Unit)

        /** The row the append created, as the picker reads it back to fill its sets (P3.18). */
        var planned: TemplateExercise? = null

        /** Every prefilled set, as the plan row and the edit. */
        val prefilled = mutableListOf<Pair<String, TemplateSetEdit>>()

        /** Whether writing a prefilled target fails, for the reported-failure case (N168). */
        var failSets = false

        override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> =
            flowOf(listOfNotNull(planned))
        override fun observeSets(templateId: String): Flow<List<TemplateSet>> = flowOf(emptyList())
        override suspend fun prependSets(
            templateExerciseId: String,
            edits: List<TemplateSetEdit>,
        ): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun addSet(
            templateExerciseId: String,
            edit: TemplateSetEdit,
        ): DataResult<Unit> {
            if (failSets) return DataResult.Failure(DataError.Storage(IOException("disk full")))
            prefilled += templateExerciseId to edit
            return DataResult.Success(Unit)
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

        override suspend fun createTemplate(name: String): DataResult<String> = unused()
        override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> = unused()
        override suspend fun deleteTemplate(templateId: String): DataResult<Unit> = unused()
        override suspend fun restoreTemplate(templateId: String): DataResult<Unit> = unused()
        override suspend fun setTemplateHidden(templateId: String, hidden: Boolean): DataResult<Unit> =
            unused()

        override suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit> {
            if (failAdds) return DataResult.Failure(DataError.Storage(IOException("disk full")))
            added += templateId to exerciseId
            return DataResult.Success(Unit)
        }

        override suspend fun removeExercise(templateExerciseId: String): DataResult<Unit> = unused()
        override suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit> =
            unused()

        private fun unused(): Nothing = error("the picker must not call this")
    }

    /** Only the picker's two calls matter here; everything else fails loudly. */
    private companion object {
        val seeded = Exercise(
            id = "back-squat",
            name = "Back Squat",
            primaryMuscle = MuscleGroup.QUADS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
        )
    }
}
