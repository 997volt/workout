package com.example.androidapp.ui.settings

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The settings screen's state (ROADMAP B23).
 *
 * Its own KDoc makes a promise worth testing: it renders what is **stored**, not what was tapped,
 * so a choice that failed to reach disk must not look applied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theStoredValue_isWhatTheScreenShows() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(FakeSettingsRepository(stored = 45))
        advanceUntilIdle()

        assertEquals(45, viewModel.uiState.value.defaultRestSeconds)
    }

    @Test
    fun aChoiceThatIsStored_becomesTheShownValue() = runTest(dispatcher) {
        val repository = FakeSettingsRepository(stored = RestTimer.DEFAULT_SECONDS)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onSetDefaultRest(30)
        advanceUntilIdle()

        assertEquals(30, viewModel.uiState.value.defaultRestSeconds)
        assertEquals(listOf(30), repository.writes)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aChoiceThatFails_isReported_andDoesNotLookApplied() = runTest(dispatcher) {
        // The promise in the ViewModel's own KDoc: a failed write must not change what is shown, or
        // the next workout silently uses the old rest while the screen says otherwise.
        val repository = FakeSettingsRepository(stored = 90, refuseWrites = true)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onSetDefaultRest(30)
        advanceUntilIdle()

        assertEquals("still what is stored", 90, viewModel.uiState.value.defaultRestSeconds)
        assertEquals(DataError.Invalid("refused"), viewModel.uiState.value.error)
    }

    @Test
    fun theRestTimerSwitch_readsAndWritesTheStoredFlag() = runTest(dispatcher) {
        // ROADMAP N44: the switch is stored like the rest sound, and the screen shows what is in
        // force rather than what was tapped.
        val repository = FakeSettingsRepository(stored = 90)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        assertEquals("on by default", true, viewModel.uiState.value.restTimerEnabled)

        viewModel.onSetRestTimer(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.restTimerEnabled)
        assertEquals(listOf(false), repository.timerWrites)
    }

    @Test
    fun theProgressionPromptSwitch_readsAndWritesTheStoredFlag() = runTest(dispatcher) {
        // ROADMAP N66: on until the lifter turns it off, and the screen shows what is in force
        // rather than what was tapped.
        val repository = FakeSettingsRepository(stored = 90)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        assertEquals("ask by default", ProgressionMode.SUGGEST, viewModel.uiState.value.progressionMode)

        viewModel.onSetProgressionMode(ProgressionMode.OFF)
        advanceUntilIdle()

        assertEquals(ProgressionMode.OFF, viewModel.uiState.value.progressionMode)
        assertEquals(listOf(ProgressionMode.OFF), repository.progressionWrites)
    }
}

/** Hand-written, like every fake here: there is no mocking framework in this project. */
private class FakeSettingsRepository(
    stored: Int,
    private val refuseWrites: Boolean = false,
) : SettingsRepository {
    private val rest = MutableStateFlow(stored)
    val writes = mutableListOf<Int>()

    override fun observeDefaultRestSeconds(): Flow<Int> = rest.asStateFlow()

    override suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit> {
        writes += seconds
        return if (refuseWrites) {
            DataResult.Failure(DataError.Invalid("refused"))
        } else {
            rest.value = seconds
            DataResult.Success(Unit)
        }
    }

    override fun observeRestCueEnabled(): Flow<Boolean> = flowOf(true)

    override suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit> =
        DataResult.Success(Unit)

    override fun observeKeepScreenOn(): Flow<Boolean> = flowOf(true)

    override suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit> =
        DataResult.Success(Unit)

    /** N44: the switch reads back what was stored, so a refused write cannot look applied. */
    private val timer = MutableStateFlow(true)
    val timerWrites = mutableListOf<Boolean>()

    override fun observeRestTimerEnabled(): Flow<Boolean> = timer.asStateFlow()

    override suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit> {
        timerWrites += enabled
        return if (refuseWrites) {
            DataResult.Failure(DataError.Invalid("refused"))
        } else {
            timer.value = enabled
            DataResult.Success(Unit)
        }
    }

    /** N66: the same read-back shape as N44's, so a refused write cannot look applied. */
    private val progression = MutableStateFlow(ProgressionMode.SUGGEST)
    val progressionWrites = mutableListOf<ProgressionMode>()

    override fun observeProgressionMode(): Flow<ProgressionMode> = progression.asStateFlow()

    override suspend fun setProgressionMode(mode: ProgressionMode): DataResult<Unit> {
        progressionWrites += mode
        return if (refuseWrites) {
            DataResult.Failure(DataError.Invalid("refused"))
        } else {
            progression.value = mode
            DataResult.Success(Unit)
        }
    }

    /** N64: the same read-back shape, so a refused write cannot look applied. */
    private val unit = MutableStateFlow(WeightUnit.KILOGRAMS)
    val unitWrites = mutableListOf<WeightUnit>()

    override fun observeWeightUnit(): Flow<WeightUnit> = unit.asStateFlow()

    override suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit> {
        unitWrites += unit
        return if (refuseWrites) {
            DataResult.Failure(DataError.Invalid("refused"))
        } else {
            this.unit.value = unit
            DataResult.Success(Unit)
        }
    }

    override fun observeStatisticsRange(): Flow<StatisticsRange> = flowOf(StatisticsRange())

    override suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit> =
        DataResult.Success(Unit)

    override fun observeRecentSeries(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun setRecentSeries(entries: List<String>): DataResult<Unit> = DataResult.Success(Unit)

    /** N218: the switch, the period and the folder, each read back so a refused write cannot look applied. */
    private val backup = MutableStateFlow(BackupSettings())
    val backupEnabledWrites = mutableListOf<Boolean>()
    val backupScheduleWrites = mutableListOf<BackupSchedule>()
    val backupFolderWrites = mutableListOf<BackupFolder?>()

    override fun observeBackupSettings(): Flow<BackupSettings> = backup.asStateFlow()

    override suspend fun setBackupEnabled(enabled: Boolean): DataResult<Unit> {
        backupEnabledWrites += enabled
        if (refuseWrites) return DataResult.Failure(DataError.Invalid("refused"))
        backup.update { it.copy(enabled = enabled) }
        return DataResult.Success(Unit)
    }

    override suspend fun setBackupSchedule(schedule: BackupSchedule): DataResult<Unit> {
        backupScheduleWrites += schedule
        if (refuseWrites) return DataResult.Failure(DataError.Invalid("refused"))
        backup.update { it.copy(schedule = schedule) }
        return DataResult.Success(Unit)
    }

    override suspend fun setBackupFolder(folder: BackupFolder?): DataResult<Unit> {
        backupFolderWrites += folder
        if (refuseWrites) return DataResult.Failure(DataError.Invalid("refused"))
        backup.update { it.copy(folder = folder) }
        return DataResult.Success(Unit)
    }

    override suspend fun recordBackupResult(at: Instant?, failed: Boolean): DataResult<Unit> =
        DataResult.Success(Unit)
}
