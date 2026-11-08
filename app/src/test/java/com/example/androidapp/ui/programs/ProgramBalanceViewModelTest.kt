package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.testing.FakeExerciseRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.google.common.truth.Truth.assertThat
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

/**
 * The balance read's wiring (ROADMAP N119, N122).
 *
 * `muscleBalance` and `plannedWorkingSets` are pure and tested as rules; the balance screen renders the
 * state it is handed. What sat untested between them is the read: every slot's template, all of them
 * flattened, and a schedule edit replacing the read rather than adding to it.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramBalanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val programs = FakeProgramRepository()
    private val templates = FakeTemplateRepository()
    private val exercises = FakeExerciseRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        programs.program.value = WorkoutProgram(id = PROGRAM_ID, name = "Upper/Lower")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProgramBalanceViewModel(
        programRepository = programs,
        templateRepository = templates,
        exerciseRepository = exercises,
        savedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
    )

    @Test
    fun everySlotsTemplate_isCounted_andTheHeaderCountsWorkingSets() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0), slot("s2", "t2", position = 1))
        templates.exercises.value = mapOf(
            "t1" to listOf(planned("e1", workingSets = 2)),
            "t2" to listOf(planned("e2", workingSets = 3)),
        )
        exercises.library.value = DataResult.Success(
            listOf(
                Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS),
                Exercise(id = "e2", name = "Bench", primaryMuscle = MuscleGroup.CHEST),
            ),
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.programName).isEqualTo("Upper/Lower")
        // The plan's own size, not the sum of the credits: a primary set is two halves (N112, N122).
        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(5)
        assertThat(viewModel.uiState.value.balance)
            .containsExactly(
                MuscleSets(MuscleGroup.CHEST, 6),
                MuscleSets(MuscleGroup.QUADS, 4),
            )
            .inOrder()
    }

    @Test
    fun addingASlot_replacesTheRead_ratherThanKeepingTheFirstTemplatesWork() = runTest(dispatcher) {
        programs.slots.value = listOf(slot("s1", "t1", position = 0))
        templates.exercises.value = mapOf("t1" to listOf(planned("e1", workingSets = 1)))
        exercises.library.value = DataResult.Success(
            listOf(Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS)),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(1)

        programs.slots.value = listOf(slot("s1", "t1", position = 0), slot("s2", "t2", position = 1))
        templates.exercises.value = templates.exercises.value +
            ("t2" to listOf(planned("e2", workingSets = 2)))
        exercises.library.value = DataResult.Success(
            listOf(
                Exercise(id = "e1", name = "Squat", primaryMuscle = MuscleGroup.QUADS),
                Exercise(id = "e2", name = "Bench", primaryMuscle = MuscleGroup.CHEST),
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.plannedSets).isEqualTo(3)
        assertThat(viewModel.uiState.value.balance.map { it.muscle })
            .containsExactly(MuscleGroup.CHEST, MuscleGroup.QUADS)
            .inOrder()
    }

    private fun slot(id: String, templateId: String, position: Int) = ProgramSlot(
        id = id,
        programId = PROGRAM_ID,
        templateId = templateId,
        position = position,
    )

    /** One planned exercise whose working sets are all ordinary, so each counts as work (N112). */
    private fun planned(exerciseId: String, workingSets: Int) = TemplateExercise(
        id = "te-$exerciseId",
        templateId = "t",
        exerciseId = exerciseId,
        position = 0,
        exerciseName = exerciseId,
        primaryMuscle = null,
        equipment = null,
        sets = (0 until workingSets).map { index ->
            TemplateSet(
                id = "ts-$exerciseId-$index",
                templateExerciseId = "te-$exerciseId",
                setIndex = index,
            )
        },
    )

    private companion object {
        const val PROGRAM_ID = "p1"
    }
}
