package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeProgressionRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
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
import java.time.Instant

/**
 * The auto-progression screen's wiring (ROADMAP N299).
 *
 * The preflight is a pure rule with its own test and the screen renders the state it is handed; what
 * sat between them is this: the consent order (off is immediate, on asks first), the reset's one write
 * at a time (B414), and a program that is gone.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramProgressionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val programs = FakeProgramRepository()
    private val templates = FakeTemplateRepository()
    private val progression = FakeProgressionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        programs.program.value = WorkoutProgram(id = PROGRAM_ID, name = "Upper/Lower")
        progression.baseline = Instant.parse("2026-11-01T09:00:00Z").toEpochMilli()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProgramProgressionViewModel(
        programs = programs,
        templates = templates,
        progression = progression,
        savedStateHandle = SavedStateHandle(mapOf("programId" to PROGRAM_ID)),
    )

    @Test
    fun turningItOn_asksFirst_andTheConfirmationIsTheWrite() = runTest(dispatcher) {
        // N299: the one direction that lets the app write is the one that asks.
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSetAuto(true)

        assertThat(viewModel.uiState.value.confirmEnable).isTrue()
        assertThat(progression.toggles).isEmpty()

        viewModel.onConfirmEnable()
        advanceUntilIdle()

        assertThat(progression.toggles).containsExactly(PROGRAM_ID to true)
        assertThat(viewModel.uiState.value.confirmEnable).isFalse()
    }

    @Test
    fun turningItOff_isImmediate() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSetAuto(false)
        advanceUntilIdle()

        assertThat(progression.toggles).containsExactly(PROGRAM_ID to false)
        assertThat(viewModel.uiState.value.confirmEnable).isFalse()
    }

    @Test
    fun aSecondReset_whileTheFirstRuns_isIgnored() = runTest(dispatcher) {
        // B414: `writing` is cleared by the first write's answer, so a second confirm that raced it
        // used to start a second reset and then report the first one's completion.
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onConfirmReset()
        assertThat(viewModel.uiState.value.writing).isTrue()
        viewModel.onConfirmReset()
        advanceUntilIdle()

        assertThat(progression.resets).isEqualTo(1)
        assertThat(viewModel.uiState.value.writing).isFalse()
    }

    @Test
    fun theResetQuestion_isNotReopened_whileAResetWrites() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onConfirmReset()

        viewModel.onResetToBaseline()

        assertThat(viewModel.uiState.value.confirmReset).isFalse()
    }

    @Test
    fun aDeletedProgram_saysSo() = runTest(dispatcher) {
        programs.program.value = null
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.notFound).isTrue()
    }

    private companion object {
        const val PROGRAM_ID = "p1"
    }
}
