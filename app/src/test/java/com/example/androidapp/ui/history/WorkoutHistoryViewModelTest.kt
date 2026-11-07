package com.example.androidapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.workout.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The history list's scalars survive process death (B372). */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

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
    fun theSavedState_roundTripsThroughABundle() = runTest(dispatcher) {
        // B372/N296: the same handle proves the wiring; this crosses the actual saved-state boundary.
        val first = SavedStateHandle()
        val initial = WorkoutHistoryViewModel(FakeWorkoutRepository(), first)
        backgroundScope.launch { initial.uiState.collect {} }
        advanceUntilIdle()
        initial.onQueryChange("bench")
        initial.onFilterChange(HistoryFilter.ACTIVITIES)

        val restored = SavedStateHandle.createHandle(first.savedStateProvider().saveState(), null)
        val viewModel = WorkoutHistoryViewModel(FakeWorkoutRepository(), restored)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("bench", viewModel.uiState.value.query)
        assertEquals(HistoryFilter.ACTIVITIES, viewModel.uiState.value.filter)
    }

    @Test
    fun theQueryAndFilter_surviveProcessDeath() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val first = WorkoutHistoryViewModel(FakeWorkoutRepository(), saved)
        backgroundScope.launch { first.uiState.collect {} }
        advanceUntilIdle()
        first.onQueryChange("bench")
        first.onFilterChange(HistoryFilter.ACTIVITIES)
        advanceUntilIdle()

        val restored = WorkoutHistoryViewModel(FakeWorkoutRepository(), saved)
        backgroundScope.launch { restored.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("bench", restored.uiState.value.query)
        assertEquals(HistoryFilter.ACTIVITIES, restored.uiState.value.filter)
    }
}
