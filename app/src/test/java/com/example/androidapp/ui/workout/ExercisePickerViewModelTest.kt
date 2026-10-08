package com.example.androidapp.ui.workout

import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.model.TemplateSet
import androidx.lifecycle.SavedStateHandle
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        assertEquals(true, exercises.created?.isCustom)
        assertEquals(MuscleGroup.OTHER, exercises.created?.primaryMuscle)
        assertEquals(Equipment.OTHER, exercises.created?.equipment)
        assertEquals(MovementPattern.OTHER, exercises.created?.movementPattern)

        // And appended to the open workout, so the picker can pop.
        assertEquals(listOf("s1" to exercises.created?.id), workouts.added)
        assertTrue(viewModel.added.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun theList_offersMovementsOnly_neverACategory() = runTest(dispatcher) {
        // ROADMAP N95: a category is never offered and never logged. It is a real row of the library, so it
        // arrives in the same list every other row does -- filtering it here, at the one place a lifter
        // chooses what to log, is what keeps the rule true rather than merely intended.
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

        assertEquals(
            listOf("Back Squat", "Barbell Bench Press"),
            viewModel.uiState.value.items.map { it.name },
        )
        assertTrue(
            "no row the picker offers is a head",
            viewModel.uiState.value.items.none { it.isCategory },
        )
    }

    @Test
    fun theList_isFlat_evenWhenAMovementIsFiledUnderACategory() = runTest(dispatcher) {
        // The picker has nothing to fold: a lifter choosing what they just did is looking for one name, so
        // a family's children are listed beside everything else rather than nested under a head that is not
        // there (N95).
        val exercises = FakeExerciseRepository(
            listOf(
                category("cat-bench", "Bench Press"),
                movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench"),
            ),
        )
        val viewModel = viewModelFor(exercises)
        observe(viewModel)
        advanceUntilIdle()

        assertEquals(listOf(0), viewModel.uiState.value.items.map { it.depth }.distinct())
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
                isCustom = true,
            )
            created = exercise
            state.value = state.value + exercise
            return DataResult.Success(exercise)
        }

        /** A category is never offered by the picker, so nothing here should ever ask for one. */
        override suspend fun createCategory(name: String): DataResult<Exercise> =
            error("the picker must not create categories")

        override suspend fun createVariationOf(parent: Exercise): DataResult<Exercise> =
            error("the picker must not create variations")

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

        override fun observeTemplates(): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> = flowOf(null)
        override fun observeTemplateName(templateId: String): Flow<String?> = flowOf(null)
        override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> =
            flowOf(emptyList())
        override fun observeSets(templateId: String): Flow<List<TemplateSet>> = flowOf(emptyList())
        override suspend fun prependSets(
            templateExerciseId: String,
            edits: List<TemplateSetEdit>,
        ): DataResult<Unit> = DataResult.Success(Unit)

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

        override suspend fun createTemplate(name: String): DataResult<String> = unused()
        override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> = unused()
        override suspend fun deleteTemplate(templateId: String): DataResult<Unit> = unused()

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
