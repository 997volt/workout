package com.example.androidapp.ui.settings

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.testing.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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

/**
 * The unit the shell provides to every screen (ROADMAP N64).
 *
 * The whole state is one stored value, so what is worth pinning is the one promise its KDoc makes: the
 * first frame is kilograms rather than a flash of the wrong unit, and a stored change reaches the shell
 * without the screen being rebuilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeightUnitViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun kilogramsUntilTheStoredPreferenceArrives_thenTheStoredUnit() = runTest(dispatcher) {
        settings.weightUnit.value = WeightUnit.POUNDS

        val viewModel = WeightUnitViewModel(settings)

        assertWithMessage("the first frame is never a flash of the wrong unit")
            .that(viewModel.weightUnit.value)
            .isEqualTo(WeightUnit.KILOGRAMS)

        advanceUntilIdle()

        assertThat(viewModel.weightUnit.value).isEqualTo(WeightUnit.POUNDS)
    }

    @Test
    fun aStoredChange_reachesTheShellWithoutARebuild() = runTest(dispatcher) {
        val viewModel = WeightUnitViewModel(settings)
        advanceUntilIdle()
        assertThat(viewModel.weightUnit.value).isEqualTo(WeightUnit.KILOGRAMS)

        settings.weightUnit.value = WeightUnit.POUNDS
        advanceUntilIdle()

        assertThat(viewModel.weightUnit.value).isEqualTo(WeightUnit.POUNDS)
    }
}
