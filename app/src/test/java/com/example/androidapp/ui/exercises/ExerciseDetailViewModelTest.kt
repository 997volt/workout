package com.example.androidapp.ui.exercises

import kotlinx.coroutines.flow.map
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.repository.ExerciseRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * Under Robolectric rather than plain JVM: the ViewModel reads its argument out
 * of a `SavedStateHandle` through the type-safe route, and that path touches a
 * real `android.os.Bundle`.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTheExerciseNamedByTheRoute() = runTest(dispatcher) {
        val viewModel = viewModelFor(FakeRepository(mutableListOf(custom)))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Sled Push", state.exercise?.name)
        assertFalse(state.notFound)
    }

    @Test
    fun anUnknownId_isNotFound_ratherThanAnEndlessSpinner() = runTest(dispatcher) {
        val viewModel = viewModelFor(FakeRepository(mutableListOf()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notFound)
    }

    @Test
    fun everyExercise_canBeEdited() = runTest(dispatcher) {
        // N5 widened N2's custom-only rule: a seeded exercise is editable too.
        val customViewModel = viewModelFor(FakeRepository(mutableListOf(custom)))
        advanceUntilIdle()
        assertTrue(customViewModel.uiState.value.canEdit)

        val seededViewModel = viewModelFor(
            FakeRepository(mutableListOf(seeded)),
            exerciseId = "back-squat",
        )
        advanceUntilIdle()
        assertTrue(seededViewModel.uiState.value.canEdit)
    }

    @Test
    fun saving_writesTheEditedAttributes_andLeavesEditMode() = runTest(dispatcher) {
        val repository = FakeRepository(mutableListOf(custom))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onEdit()
        viewModel.onSave(
            ExerciseEdit(
                name = "  Sled Push Heavy  ",
                primaryMuscle = MuscleGroup.QUADS,
                equipment = Equipment.MACHINE,
                movementPattern = MovementPattern.SQUAT,
                restSeconds = 180,
                techniqueNote = "Drive the floor away",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        // The name is trimmed on the way in, so the stored row and the screen agree.
        assertEquals("Sled Push Heavy", state.exercise?.name)
        assertEquals(MuscleGroup.QUADS, state.exercise?.primaryMuscle)
        assertEquals(Equipment.MACHINE, state.exercise?.equipment)
        assertEquals(MovementPattern.SQUAT, state.exercise?.movementPattern)
        assertEquals(180, state.exercise?.restSeconds)
        assertEquals("Drive the floor away", state.exercise?.techniqueNote)
        assertEquals("Sled Push Heavy", repository.saved?.name)
        assertEquals(180, repository.saved?.restSeconds)
    }

    @Test
    fun aFailedSave_keepsTheFormOpen_andSurfacesTheError() = runTest(dispatcher) {
        val repository = FakeRepository(mutableListOf(custom)).apply { failWrites = true }
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onEdit()
        viewModel.onSave(
            ExerciseEdit("Sled Push Heavy", MuscleGroup.QUADS, Equipment.MACHINE, MovementPattern.SQUAT),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("a failed save must not look like it worked", state.isEditing)
        assertNotNull(state.error)
        // The stored exercise is untouched, so re-opening the form shows the truth.
        assertEquals("Sled Push", state.exercise?.name)
    }

    @Test
    fun onlyCategories_areOfferedToFileUnder() = runTest(dispatcher) {
        // N110: the shape is one rule deep, so a movement is nobody's head. Offering one would let the editor
        // write a link the repository refuses and no grouped list would ever draw.
        val other = seeded.copy(id = "front-squat", name = "Front Squat")
        val repository = FakeRepository(mutableListOf(seeded, other, category))
        val viewModel = viewModelFor(repository, exerciseId = seeded.id)
        advanceUntilIdle()

        assertEquals(
            "the category, and neither the row itself nor another movement",
            listOf(category.id),
            viewModel.uiState.value.categoryOptions.map { it.id },
        )
    }

    @Test
    fun savingAMoveToACategory_reloadsTheHead() = runTest(dispatcher) {
        // B84: `head`/`headName` were read once in `init`, so a save that moved the row left the page showing
        // no family and the pre-move inherited muscle.
        val repository = FakeRepository(mutableListOf(custom, category))
        val viewModel = viewModelFor(repository, exerciseId = custom.id)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.headName)

        viewModel.onEdit()
        viewModel.onSave(
            ExerciseEdit(
                name = custom.name,
                primaryMuscle = custom.primaryMuscle,
                equipment = custom.equipment,
                movementPattern = custom.movementPattern,
                parentId = category.id,
            ),
        )
        advanceUntilIdle()

        assertEquals("Bench Press", viewModel.uiState.value.headName)
        assertEquals(category.id, viewModel.uiState.value.head?.id)
    }

    @Test
    fun aMovementsChain_isItsCategory_andNoFurther() = runTest(dispatcher) {
        // ROADMAP N110: the shape is one rule deep, so what sits above a movement is its category — and a
        // movement in no category has nothing above it to read from.
        val movement = seeded.copy(
            id = "barbell-bench-press",
            name = "Barbell Bench Press",
            parentId = category.id,
        )
        val filedViewModel = viewModelFor(
            FakeRepository(mutableListOf(category, movement)),
            exerciseId = movement.id,
        )
        val unfiledViewModel = viewModelFor(
            FakeRepository(mutableListOf(category, movement, seeded)),
            exerciseId = seeded.id,
        )
        advanceUntilIdle()

        assertEquals(
            "the chain above a filed movement is its category",
            listOf("cat-bench"),
            filedViewModel.uiState.value.ancestors.map { it.id },
        )
        assertTrue(
            "and a loose movement has nothing above it to inherit from",
            unfiledViewModel.uiState.value.ancestors.isEmpty(),
        )
    }

    private fun viewModelFor(
        repository: FakeRepository,
        exerciseId: String = "custom-1",
    ) = ExerciseDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("exerciseId" to exerciseId)),
    )

    @Test
    fun aFailedRead_saysSo_ratherThanClaimingTheExerciseDoesNotExist() = runTest(dispatcher) {
        val repository = FakeRepository(mutableListOf(seeded)).apply { failReads = true }
        val viewModel = viewModelFor(repository, exerciseId = seeded.id)
        advanceUntilIdle()

        assertNotNull("the failure must reach the screen", viewModel.uiState.value.error)
        assertFalse(
            "a failed read is not the same as a missing exercise",
            viewModel.uiState.value.notFound,
        )
    }

    private val seeded = Exercise(
        id = "back-squat",
        name = "Back Squat",
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
    )

    private val category = Exercise(
        id = "cat-bench",
        name = "Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.OTHER,
        movementPattern = MovementPattern.PRESS,
        rowKind = RowKind.CATEGORY,
    )

    private class FakeRepository(initial: MutableList<Exercise>) : ExerciseRepository {
        private val state = MutableStateFlow(initial.toList())
        var saved: Exercise? = null
        var failWrites = false

        /** Every row the fake holds, so a test can prove a write did — or did not — happen. */
        val stored: List<Exercise> get() = state.value

        /** Lets a test drive the B4 path: a read that fails rather than throws. */
        var failReads = false

        override fun observeExercises(): Flow<DataResult<List<Exercise>>> =
            state.map { DataResult.Success(it) }

        override suspend fun getExercise(id: String): DataResult<Exercise?> =
            if (failReads) {
                DataResult.Failure(DataError.Storage(IOException("database is locked")))
            } else {
                DataResult.Success(state.value.firstOrNull { it.id == id })
            }

        override suspend fun createCustomExercise(name: String): DataResult<Exercise> =
            error("the detail screen must not create exercises")

        override suspend fun createCategory(name: String): DataResult<Exercise> =
            error("the detail screen must not create categories")

        /**
         * The naming read (ROADMAP N95). The fake answers with the live rows it holds, which is enough for
         * these tests: nothing here asserts on a *removed* head naming its child.
         */
        override suspend fun getAllIncludingDeleted(): DataResult<List<Exercise>> =
            DataResult.Success(state.value)

        override suspend fun updateExercise(exercise: Exercise): DataResult<Unit> {
            if (failWrites) return DataResult.Failure(DataError.Storage(IOException("disk full")))
            saved = exercise
            state.value = state.value.map { if (it.id == exercise.id) exercise else it }
            return DataResult.Success(Unit)
        }
    }

    private companion object {
        val seeded = Exercise(
            id = "back-squat",
            name = "Back Squat",
            primaryMuscle = MuscleGroup.QUADS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
        )

        val custom = Exercise(
            id = "custom-1",
            name = "Sled Push",
            primaryMuscle = MuscleGroup.OTHER,
            equipment = Equipment.OTHER,
            movementPattern = MovementPattern.OTHER,
        )
    }
}
