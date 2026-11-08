package com.example.androidapp.ui.history

import androidx.lifecycle.SavedStateHandle
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.ui.testing.FakeWorkoutRepository
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
import java.time.Instant

/**
 * The history list's query and chips (ROADMAP N146).
 *
 * `HistorySearchTest` pins the rule — what a query can reach and what a chip hides. What had no test is
 * the ViewModel half: that typing and picking reach that rule, that the state keeps the query the field
 * shows, and that the two empty lists stay distinguishable (B372 covers the restore; this covers the
 * transitions that are saved in the first place).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryQueryTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWorkoutRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WorkoutHistoryViewModel(repository, SavedStateHandle())

    @Test
    fun aQuery_narrowsTheRows_andTheStateSaysWhyTheListIsEmpty() = runTest(dispatcher) {
        repository.history.value = listOf(
            summary(id = "squat", template = "Push day"),
            summary(id = "swim", activity = Activity.SWIMMING),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.groups.flatMap { it.workouts }.map { it.id })
            .containsExactly("squat", "swim")

        viewModel.onQueryChange("push")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query).isEqualTo("push")
        assertThat(viewModel.uiState.value.groups.flatMap { it.workouts }.map { it.id })
            .containsExactly("squat")

        viewModel.onQueryChange("nothing like this")
        advanceUntilIdle()

        assertWithMessage("a query that matches is still a list with nothing in it")
            .that(viewModel.uiState.value.isEmpty)
            .isTrue()
        assertWithMessage("but it is empty for a reason the screen can name")
            .that(viewModel.uiState.value.isFilteredEmpty)
            .isTrue()
    }

    @Test
    fun theChips_narrowTheRowsToActivitiesOrWorkouts() = runTest(dispatcher) {
        repository.history.value = listOf(
            summary(id = "squat", template = "Push day"),
            summary(id = "swim", activity = Activity.SWIMMING),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onFilterChange(HistoryFilter.ACTIVITIES)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.filter).isEqualTo(HistoryFilter.ACTIVITIES)
        assertThat(viewModel.uiState.value.groups.flatMap { it.workouts }.map { it.id })
            .containsExactly("swim")

        viewModel.onFilterChange(HistoryFilter.WORKOUTS)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.groups.flatMap { it.workouts }.map { it.id })
            .containsExactly("squat")
    }

    @Test
    fun anUntrainedLog_isEmpty_andOnlyAQueryOrChip_makesItFilteredEmpty() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isEmpty).isTrue()
        assertWithMessage("nothing has been trained yet, not filtered away")
            .that(viewModel.uiState.value.isFilteredEmpty)
            .isFalse()

        viewModel.onQueryChange("bench")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isFilteredEmpty).isTrue()

        // A chip alone is enough: the sentence must offer the way back even with an empty field.
        viewModel.onQueryChange("")
        viewModel.onFilterChange(HistoryFilter.ACTIVITIES)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isFilteredEmpty).isTrue()
    }

    private fun summary(
        id: String,
        template: String? = null,
        activity: Activity? = null,
    ): WorkoutSummary = WorkoutSummary(
        id = id,
        startedAt = Instant.parse("2026-09-15T12:00:00Z"),
        finishedAt = Instant.parse("2026-09-15T13:00:00Z"),
        exerciseCount = if (activity == null) 1 else 0,
        setCount = if (activity == null) 3 else 0,
        volumeGrams = 0,
        templateName = template,
        activity = activity,
    )
}
