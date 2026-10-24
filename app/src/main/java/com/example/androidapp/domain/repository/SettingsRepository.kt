package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.StatisticsRange
import kotlinx.coroutines.flow.Flow

/**
 * The choices that apply to the whole app rather than to one workout (ROADMAP N21).
 *
 * Small on purpose: the app is local-only and has no account, so "settings" here means the
 * handful of preferences that change how it behaves for this person on this phone. The
 * default rest is the first, because it was a hardcoded 90 seconds with no way to change it.
 */
interface SettingsRepository {

    /**
     * The rest the app starts when an exercise prescribes none, in seconds (ROADMAP N5).
     *
     * A `Flow` rather than a value: a change made in settings must reach a workout that is
     * already open, and the workout screen is not restarted to pick it up.
     */
    fun observeDefaultRestSeconds(): Flow<Int>

    /** Stores the default rest for future sets. Rejects a value outside [VALID_REST_SECONDS]. */
    suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit>

    /**
     * Whether a finished rest should be **heard** as well as seen (ROADMAP N27).
     *
     * With the background alert gone this is the cue that replaces it, so it is on by default: the
     * app would otherwise be quieter than it was, which is a regression dressed as a preference.
     * It plays a tone in-process and uses view-level haptics — neither asks for a permission, which
     * is what N26 bought and DECISIONS.md records.
     */
    fun observeRestCueEnabled(): Flow<Boolean>

    suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit>

    /**
     * Whether a workout keeps the screen awake (ROADMAP N27).
     *
     * A window flag rather than a wake lock: the screen stays on while the app is in front, and
     * nothing is held once it is not.
     */
    fun observeKeepScreenOn(): Flow<Boolean>

    suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit>

    /**
     * Whether a rest between sets is counted down at all (ROADMAP N44).
     *
     * On by default: a rest *is* a countdown, and the switch is for someone who rests by feel.
     * Off means the end instant is never written, so the timer is genuinely not running rather
     * than a countdown hidden behind a static number; the workout screen shows the exercise's own
     * prescription as a fixed label instead, and nothing chimes.
     */
    fun observeRestTimerEnabled(): Flow<Boolean>

    suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit>

    /**
     * What *Done* does with the step a plan earned (ROADMAP N50, N66, P3.17).
     *
     * Three states rather than a switch: ask (the app's behaviour since N50), answer with every earned step
     * already picked so one *Done* writes them, or do not ask at all. A mode, because "on" and "off" could
     * not express the middle — the lifter who wants the steps taken without tapping each one.
     */
    fun observeProgressionMode(): Flow<ProgressionMode>

    suspend fun setProgressionMode(mode: ProgressionMode): DataResult<Unit>

    /**
     * The unit weights are shown and typed in, app-wide (ROADMAP N64).
     *
     * A preference rather than a per-screen choice, and **presentation only**: every weight is stored
     * as whole grams, so switching this changes nothing on disk. An exercise may override it for
     * itself ([com.example.androidapp.domain.model.Exercise.weightUnit]).
     *
     * Kilograms by default, which is the unit the app shipped in: an upgrade changes nothing for
     * someone who never opens settings.
     */
    fun observeWeightUnit(): Flow<WeightUnit>

    suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit>

    /**
     * The window the Statistics screen is showing (ROADMAP N35).
     *
     * A preference rather than screen state: coming back to the tab should find the range that was chosen,
     * and "the last 7 days" has to keep meaning seven days from whenever it is read — which is why what is
     * stored is the range's *kind*, not the dates it resolved to.
     */
    fun observeStatisticsRange(): Flow<StatisticsRange>

    suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit>

    /**
     * The series most recently read on the Statistics screen, newest first (N212).
     *
     * Ids rather than the screen's keys: the store stays a domain type, and an id written by a build that knew
     * a series this one does not is dropped by the reader rather than breaking the screen — the same rule
     * [observeStatisticsRange] follows for an unknown range name. The caller keeps the list short (three); the
     * store keeps what it is given.
     */
    fun observeRecentSeries(): Flow<List<String>>

    /** Stores [ids] newest first. An empty list clears the row. */
    suspend fun setRecentSeries(ids: List<String>): DataResult<Unit>

    companion object {
        /**
         * What the setting accepts, in seconds.
         *
         * Bounded rather than free-form: zero or a negative rest is not a setting, it is a bug waiting
         * to be entered, and a rest longer than the session itself is a mistake rather than a
         * preference. (This used to argue from the value becoming an alarm; the alert is gone
         * (ROADMAP N26), and the bounds are still right — they are about what a rest means.)
         */
        val VALID_REST_SECONDS = 5..3600
    }
}
