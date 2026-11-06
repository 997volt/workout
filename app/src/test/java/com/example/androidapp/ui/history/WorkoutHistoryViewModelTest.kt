package com.example.androidapp.ui.history

import androidx.lifecycle.SavedStateHandle
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

/** The history list's scalars survive process death (B372). */
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
