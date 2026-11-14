package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.ui.testing.FakeExerciseRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.example.androidapp.ui.testing.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
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

/**
 * The replace sheet's reads and choices (ROADMAP N308): the category suggestions, the seed by place,
 * Empty, the shared warning and the write. The plan edit's arithmetic is the domain's own test.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramReplaceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val templates = FakeTemplateRepository()
    private val workouts = FakeWorkoutRepository()
    private val exercises = FakeExerciseRepository()
    private val programs = FakeProgramRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        templates.exercises.value = mapOf(
            "t1" to listOf(
                planExercise("te1", "squat", "Back Squat"),
                planExercise("te2", "hack-squat", "Hack Squat"),
            ),
        )
        templates.sets.value = mapOf("t1" to (0 until 5).map { planSet("s$it", it) })
        exercises.library.value = DataResult.Success(
            listOf(
                exercise("squat", "Back Squat", parentId = "legs"),
                exercise("front-squat", "Front Squat", parentId = "legs"),
                exercise("hack-squat", "Hack Squat", parentId = "legs"),
                exercise("bench", "Bench Press", parentId = "push"),
                exercise("legs", "Legs", kind = RowKind.CATEGORY),
            ),
        )
        workouts.previous = PreviousPerformance(
            listOf(
                SetEntry("p0", "se", 0, reps = 6, weightGrams = 80_000L, setType = SetType.NORMAL),
                SetEntry("p1", "se", 1, reps = 6, weightGrams = 80_000L, setType = SetType.NORMAL),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProgramReplaceViewModel(
        templates = templates,
        workouts = workouts,
        exercises = exercises,
        programs = programs,
        savedStateHandle = SavedStateHandle(
            mapOf(
                "programId" to "p1",
                "templateId" to "t1",
                "templateExerciseId" to "te1",
            ),
        ),
    )

    @Test
    fun theSuggestions_areTheCategoryMinusTheOldAndThePlansOwn() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Hack Squat is in the plan, Bench Press is another family, and the category head is not a movement.
        assertThat(viewModel.state.value.suggestions.map { it.name }).containsExactly("Front Squat")
        assertThat(viewModel.state.value.setCountBefore).isEqualTo(5)
        assertThat(viewModel.state.value.setCountAfter).isEqualTo(3)
    }

    @Test
    fun theSeed_isByPlace_andTheEmptyChoiceIgnoresIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.select("front-squat")
        advanceUntilIdle()

        assertThat(viewModel.state.value.selected?.id).isEqualTo("front-squat")
        assertThat(viewModel.state.value.seeds).hasSize(2)
        assertThat(viewModel.state.value.seeds.first()?.weightGrams).isEqualTo(80_000L)
        assertThat(viewModel.state.value.seeds.first()?.reps).isEqualTo(6)

        viewModel.setEmpty(true)
        viewModel.confirm()
        advanceUntilIdle()

        assertThat(templates.replacements).containsExactly("te1" to "front-squat")
        assertThat(viewModel.state.value.replaced?.oldTemplateExerciseId).isEqualTo("te1")
    }

    @Test
    fun anotherProgramUsingThePlan_isNamed_onlyWhenThereIsOne() = runTest(dispatcher) {
        programs.otherProgramNames = listOf("Full Body")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.sharedProgramNames).containsExactly("Full Body")
    }

    @Test
    fun anUnfiledMovement_hasNoSuggestions() = runTest(dispatcher) {
        exercises.library.value = DataResult.Success(
            listOf(exercise("squat", "Back Squat", parentId = null)),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.suggestions).isEmpty()
    }

    private fun exercise(
        id: String,
        name: String,
        parentId: String? = null,
        kind: RowKind = RowKind.MOVEMENT,
    ) = Exercise(id = id, name = name, parentId = parentId, rowKind = kind)

    private fun planExercise(id: String, exerciseId: String, name: String) = TemplateExercise(
        id = id,
        templateId = "t1",
        exerciseId = exerciseId,
        position = 0,
        exerciseName = name,
        primaryMuscle = null,
        equipment = null,
    )

    private fun planSet(id: String, index: Int) = TemplateSet(
        id = id,
        templateExerciseId = "te1",
        setIndex = index,
        targetWeightGrams = 100_000L,
        targetRepsMin = 5,
        targetRepsMax = 8,
    )
}
