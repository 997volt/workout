package com.example.androidapp.ui

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/**
 * Every setting at its default, for a test that only cares about one of them.
 *
 * The interface is a screen's worth of questions, and a fake that has to answer all of them buries the answer
 * the test is about in nineteen refusals. A test delegates to this object and overrides the one member it is
 * asking about — `: SettingsRepository by EmptySettings` — which is also what keeps a new setting from
 * breaking every fake in the suite.
 *
 * The values are the ones a fresh install has: the app's rest, the counting-down timer, kilograms, a six-month
 * range, backups off. A write here succeeds and stores nothing, because a test that asserts a write either
 * drives the member itself or reads the value it set.
 */
internal object EmptySettings : SettingsRepository {
    override fun observeDefaultRestSeconds(): Flow<Int> = flowOf(90)

    override suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeRestCueEnabled(): Flow<Boolean> = flowOf(true)

    override suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeKeepScreenOn(): Flow<Boolean> = flowOf(true)

    override suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeRestTimerEnabled(): Flow<Boolean> = flowOf(true)

    override suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeProgressionMode(): Flow<ProgressionMode> = flowOf(ProgressionMode.SUGGEST)

    override suspend fun setProgressionMode(mode: ProgressionMode): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeWeightUnit(): Flow<WeightUnit> = flowOf(WeightUnit.KILOGRAMS)

    override suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeStatisticsRange(): Flow<StatisticsRange> = flowOf(StatisticsRange())

    override suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeRecentSeries(): Flow<List<String>> = flowOf(emptyList())

    override suspend fun setRecentSeries(entries: List<String>): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeBackupSettings(): Flow<BackupSettings> = flowOf(BackupSettings())

    override suspend fun setBackupEnabled(enabled: Boolean): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun setBackupSchedule(schedule: BackupSchedule): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun setBackupFolder(folder: BackupFolder?): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun recordBackupResult(
        at: Instant?,
        attemptAt: Instant,
        failed: Boolean,
    ): DataResult<Unit> = DataResult.Success(Unit)
}
