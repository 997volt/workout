package com.example.androidapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.RangeKind
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings in `SharedPreferences` (ROADMAP N21).
 *
 * Chosen over DataStore because it needs **no new dependency**: what lives here is a
 * handful of integers and booleans owned by one process, which is the case
 * `SharedPreferences` is still the right tool for. If settings ever need to hold a
 * collection, a schema or a migration, that is the moment to move.
 *
 * Writes are applied with `commit` inside the caller's coroutine rather than `apply`: the
 * caller is a screen that reports success or failure, and a fire-and-forget write would let
 * it claim "saved" about something that did not reach disk (this is `DataResult`, not a
 * hoped-for success).
 */
@Singleton
class PreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun observeDefaultRestSeconds(): Flow<Int> = callbackFlow {
        // The current value first, so a collector never has to wait for a change to render.
        trySend(currentRestSeconds())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DEFAULT_REST_SECONDS) trySend(currentRestSeconds())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    override fun observeRestCueEnabled(): Flow<Boolean> = observeFlag(KEY_REST_CUE_ENABLED, true)

    override suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit> =
        writeFlag(KEY_REST_CUE_ENABLED, enabled)

    override fun observeKeepScreenOn(): Flow<Boolean> = observeFlag(KEY_KEEP_SCREEN_ON, true)

    override suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit> =
        writeFlag(KEY_KEEP_SCREEN_ON, enabled)

    override fun observeRestTimerEnabled(): Flow<Boolean> = observeFlag(KEY_REST_TIMER_ENABLED, true)

    override suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit> =
        writeFlag(KEY_REST_TIMER_ENABLED, enabled)

    override fun observeProgressionMode(): Flow<ProgressionMode> = callbackFlow {
        trySend(currentProgressionMode())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == KEY_PROGRESSION_MODE || changed == KEY_PROGRESSION_PROMPT_ENABLED) {
                trySend(currentProgressionMode())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    override suspend fun setProgressionMode(mode: ProgressionMode): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            preferences.edit(commit = true) { putString(KEY_PROGRESSION_MODE, mode.name) }
            if (currentProgressionMode() == mode) {
                DataResult.Success(Unit)
            } else {
                DataResult.Failure(DataError.Storage(IllegalStateException("the setting was not stored")))
            }
        }

    /**
     * The stored mode, or the switch it replaced (N66).
     *
     * A device that never opened the new control still has the old boolean: on meant *ask*, off meant finish
     * without asking, so the fallback is SUGGEST or OFF and never APPLY_AT_DONE. The old key is read, never
     * written again.
     */
    private fun currentProgressionMode(): ProgressionMode {
        ProgressionMode.fromName(preferences.getString(KEY_PROGRESSION_MODE, null))?.let { return it }
        return if (preferences.getBoolean(KEY_PROGRESSION_PROMPT_ENABLED, true)) {
            ProgressionMode.SUGGEST
        } else {
            ProgressionMode.OFF
        }
    }

    override fun observeWeightUnit(): Flow<WeightUnit> = callbackFlow {
        trySend(currentWeightUnit())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == KEY_WEIGHT_UNIT) trySend(currentWeightUnit())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    override suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit> = withContext(Dispatchers.IO) {
        preferences.edit(commit = true) { putString(KEY_WEIGHT_UNIT, unit.name) }
        if (currentWeightUnit() == unit) {
            DataResult.Success(Unit)
        } else {
            DataResult.Failure(DataError.Storage(IllegalStateException("the setting was not stored")))
        }
    }

    /**
     * The stored unit, defaulted.
     *
     * An unknown name falls back to kilograms rather than throwing: a preference is not worth a
     * broken screen, and storing the enum **by name** is what makes that possible.
     */
    private fun currentWeightUnit(): WeightUnit =
        WeightUnit.fromName(preferences.getString(KEY_WEIGHT_UNIT, null)) ?: WeightUnit.KILOGRAMS

    /** A boolean preference, defaulted rather than null: these flags have always had a meaning. */
    private fun observeFlag(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
        trySend(preferences.getBoolean(key, default))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(preferences.getBoolean(key, default))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    override fun observeStatisticsRange(): Flow<StatisticsRange> = callbackFlow {
        trySend(currentStatisticsRange())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            // All three keys make up one value, so any of them changing is the range changing.
            if (changed == null || changed in RANGE_KEYS) trySend(currentStatisticsRange())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    /** Committed and read back off the main thread, for the reason [writeFlag] states (B158). */
    override suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            preferences.edit(commit = true) {
                putString(KEY_RANGE_KIND, range.kind.name)
                if (range.kind == RangeKind.CUSTOM) {
                    // Absent rather than a sentinel value: an end nobody chose is not a date, and a sentinel
                    // would be a date-shaped thing that has to be remembered not to mean anything.
                    range.from?.let { putLong(KEY_RANGE_FROM, it.toEpochDay()) } ?: remove(KEY_RANGE_FROM)
                    range.to?.let { putLong(KEY_RANGE_TO, it.toEpochDay()) } ?: remove(KEY_RANGE_TO)
                } else {
                    // Dates are kept only where they mean something: a stale From–To left on a rolling range
                    // would be a second source of truth waiting to disagree with the kind.
                    remove(KEY_RANGE_FROM)
                    remove(KEY_RANGE_TO)
                }
            }
            if (currentStatisticsRange() == range) {
                DataResult.Success(Unit)
            } else {
                DataResult.Failure(DataError.Storage(IllegalStateException("the range was not stored")))
            }
        }

    /**
     * The stored range, defaulted.
     *
     * An unknown kind — a name written by a build that had one this build does not — falls back to the
     * default rather than throwing. A preference is not worth a broken screen, and storing the enum **by
     * name** is what makes that possible: an ordinal would have been read back as a different range.
     */
    private fun currentStatisticsRange(): StatisticsRange {
        val kind = preferences.getString(KEY_RANGE_KIND, null)
            ?.let { name -> RangeKind.entries.firstOrNull { it.name == name } }
            ?: RangeKind.LAST_7_DAYS

        fun date(key: String): LocalDate? =
            if (preferences.contains(key)) LocalDate.ofEpochDay(preferences.getLong(key, 0L)) else null

        return StatisticsRange(
            kind = kind,
            from = if (kind == RangeKind.CUSTOM) date(KEY_RANGE_FROM) else null,
            to = if (kind == RangeKind.CUSTOM) date(KEY_RANGE_TO) else null,
        )
    }

    /**
     * One flag, committed and confirmed (B158).
     *
     * **The commit runs off the main thread.** `commit = true` is a synchronous write plus fsync, and every
     * caller here is `viewModelScope.launch` on Main — so each toggle in Settings, and each range chip in
     * Statistics, was a disk write on the tap path. The comment below still holds: it is committed rather than
     * applied because the caller reports success, and success has to mean *on disk*.
     */
    private suspend fun writeFlag(key: String, value: Boolean): DataResult<Unit> = withContext(Dispatchers.IO) {
        preferences.edit(commit = true) { putBoolean(key, value) }
        if (preferences.getBoolean(key, !value) == value) {
            DataResult.Success(Unit)
        } else {
            DataResult.Failure(DataError.Storage(IllegalStateException("the setting was not stored")))
        }
    }

    override suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit> =
        if (seconds !in SettingsRepository.VALID_REST_SECONDS) {
            DataResult.Failure(DataError.Invalid("a rest must be between 5 seconds and an hour"))
        } else {
            // Committed, not applied: the caller reports success or failure, and a fire-and-forget write
            // would let it claim a success that never reached disk. The KTX `edit` returns Unit, so the write is
            // confirmed by reading it back — and both happen off the main thread (B158).
            withContext(Dispatchers.IO) {
                preferences.edit(commit = true) { putInt(KEY_DEFAULT_REST_SECONDS, seconds) }
                if (currentRestSeconds() == seconds) {
                    DataResult.Success(Unit)
                } else {
                    DataResult.Failure(
                        DataError.Storage(IllegalStateException("the setting was not stored")),
                    )
                }
            }
        }

    private fun currentRestSeconds(): Int = preferences.getInt(
        KEY_DEFAULT_REST_SECONDS,
        // The value the app shipped with before this screen existed, so an upgrade changes
        // nothing for someone who never opens settings.
        RestTimer.DEFAULT_SECONDS,
    )

    private companion object {
        const val FILE_NAME = SETTINGS_FILE_NAME
        const val KEY_DEFAULT_REST_SECONDS = "default_rest_seconds"
        const val KEY_REST_CUE_ENABLED = "rest_cue_enabled"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_REST_TIMER_ENABLED = "rest_timer_enabled"
        const val KEY_PROGRESSION_MODE = "progression_mode"

        /** The switch P3.17 replaced; read only, so an upgrade keeps the answer it held. */
        const val KEY_PROGRESSION_PROMPT_ENABLED = "progression_prompt_enabled"
        const val KEY_WEIGHT_UNIT = "weight_unit"
        const val KEY_RANGE_KIND = "statistics_range_kind"
        const val KEY_RANGE_FROM = "statistics_range_from"
        const val KEY_RANGE_TO = "statistics_range_to"

        /** The three keys that together are one value. */
        val RANGE_KEYS = setOf(KEY_RANGE_KIND, KEY_RANGE_FROM, KEY_RANGE_TO)
    }
}

/** The settings file's name, which the goals' one-time move also opens (ROADMAP N137). */
internal const val SETTINGS_FILE_NAME = "settings"

/**
 * Goals as one line each, `id=value` (ROADMAP N39) — **the store N137 moved out of this file**.
 *
 * These keys are read once, by [moveLegacyGoals], and emptied by it: a goal is a row in `metric_goals` now,
 * and nothing writes the string again. They are kept rather than deleted because the goals a lifter already
 * had are still sitting in them on every device that upgrades.
 */
private const val KEY_GOALS = "metric_goals"

private fun decodeGoals(text: String?): Map<String, Double> {
    if (text.isNullOrBlank()) return emptyMap()
    return text.lineSequence().mapNotNull { line ->
        val parts = line.split('=', limit = 2)
        val value = parts.getOrNull(1)?.toDoubleOrNull()
        if (parts.size == 2 && value != null) parts[0] to value else null
    }.toMap()
}

/** The kind of the goal each metric has, one line per metric that has one (ROADMAP N135). */
private const val KEY_GOAL_KINDS = "metric_goal_kinds"

private fun decodeGoalKinds(text: String?): Map<String, String> {
    if (text.isNullOrBlank()) return emptyMap()
    return text.lineSequence().mapNotNull { line ->
        val parts = line.split('=', limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
    }.toMap()
}

/**
 * Moves the goals a lifter already had into the rows that hold them now (ROADMAP N137).
 *
 * Runs from `RoomDatabase.Callback.onOpen`, beside the library's own top-up and for the same reason: it is a
 * one-time move of what is already on disk, and the database being opened is the moment to make it. Raw SQL
 * because a Room call on the connection being opened would deadlock, and it is here rather than in the database
 * package because this is the file that knows the settings format — the prefs string is read by the same
 * decoders that wrote it.
 *
 * **The start is written as null**, which is N137's decision: the moment this goal was set is recorded nowhere,
 * and stamping it with the upgrade's own time would be inventing history — the thing periods exist to stop.
 * The ids are derived from the metric rather than random, so a second open inserts nothing.
 *
 * The union of the two maps is moved, not just the values: a **direction-only** goal (N135) is a kind with no
 * number, and dropping it here would lose exactly the goal that has no value to fall back on.
 */
internal fun moveLegacyGoals(db: SupportSQLiteDatabase, context: Context, movedAt: Long) {
    val preferences = context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)
    val goals = decodeGoals(preferences.getString(KEY_GOALS, null))
    val kinds = decodeGoalKinds(preferences.getString(KEY_GOAL_KINDS, null))
    val metrics = goals.keys + kinds.keys
    if (metrics.isEmpty()) return

    db.beginTransaction()
    try {
        metrics.forEach { metricId ->
            db.execSQL(
                INSERT_LEGACY_GOAL,
                arrayOf<Any?>(
                    "legacy:$metricId",
                    metricId,
                    kinds[metricId],
                    goals[metricId],
                    movedAt,
                    movedAt,
                ),
            )
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
    // Emptied only after the rows are in: a crash between the two leaves the goals where they were, and the
    // next open moves them again — which the derived ids make a no-op rather than a duplicate.
    preferences.edit(commit = true) {
        remove(KEY_GOALS)
        remove(KEY_GOAL_KINDS)
    }
}

/** The column list must match [com.example.androidapp.data.local.MetricGoalEntity]; `startedAt` is the null. */
private const val INSERT_LEGACY_GOAL =
    "INSERT OR IGNORE INTO metric_goals " +
        "(id, metricId, kind, value, startedAt, createdAt, updatedAt) VALUES (?, ?, ?, ?, NULL, ?, ?)"
