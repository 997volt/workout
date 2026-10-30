package com.example.androidapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.seedMissingExercises
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.platform.CrashLog
import com.example.androidapp.platform.CrashLogStore
import com.example.androidapp.platform.androidCrashMetadata
import com.example.androidapp.platform.framesWithTypes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The housekeeping the app runs when the database opens, and what happens when it cannot finish (ROADMAP
 * B324).
 *
 * The seed's repairs and the goals move are **housekeeping**, not the lifter's data: they settle shapes an
 * older build left behind. A throw in them used to propagate out of Room's `onOpen` and kill the process on
 * the first screen after an update — a data-dependent foreign key in 2.19.1's predecessor did exactly that,
 * on a store where every workout was intact.
 *
 * So the pass is run through this class, which records a failure, keeps the store as the transaction left it
 * and hands the app a state to say so once with a retry. The pass itself is [runSeedRepairs], uncaught, so the
 * migration-chain test still fails on a broken repair instead of having the catch hide it (B325).
 */
@Singleton
class Housekeeping @Inject constructor(
    private val crashLogStore: CrashLogStore,
    private val timeSource: TimeSource,
    @ApplicationContext private val context: Context,
) {

    /**
     * A pass that did not finish.
     *
     * [frames] is the same message-free frame list a [CrashLog] keeps: a repair's exception message can quote
     * a value, and this state is shown on a screen.
     */
    data class Failure(
        val at: Long,
        val exceptionClass: String,
        val frames: String,
    )

    private val _failure = MutableStateFlow<Failure?>(null)

    /** The most recent failed pass, or null once a retry succeeds or the lifter dismisses it. */
    val failure: StateFlow<Failure?> = _failure.asStateFlow()

    /**
     * The pass, uncaught: the caller decides whether a throw is survivable.
     *
     * The seed opens its own transaction, so a failure leaves nothing half-written; the goals move does the
     * same and only empties the preferences after its rows are in.
     */
    internal fun runRepairs(db: SupportSQLiteDatabase, at: Long = timeSource.nowEpochMillis()) {
        runSeedRepairs(db, context, at)
    }

    /** Records a failed pass to disk and to [failure]; never throws — a recorder that throws records nothing. */
    fun report(error: Throwable, at: Long = timeSource.nowEpochMillis()) {
        val frames = error.framesWithTypes()
        val metadata = androidCrashMetadata(context)
        runCatching {
            crashLogStore.record(
                CrashLog(
                    timestamp = at,
                    exceptionClass = error.javaClass.name,
                    stackTrace = frames,
                    appVersion = metadata.appVersion,
                    versionCode = metadata.versionCode,
                    androidVersion = metadata.androidVersion,
                    deviceModel = metadata.deviceModel,
                ),
            )
        }
        _failure.value = Failure(at = at, exceptionClass = error.javaClass.name, frames = frames)
    }

    /** The lifter said "later": the next open tries again, and Diagnostics still holds the record. */
    fun dismiss() {
        _failure.value = null
    }

    /** Runs the pass again on the open store, clearing the failure only if it finishes. */
    suspend fun retry(database: WorkoutDatabase): DataResult<Unit> = withContext(Dispatchers.IO) {
        runCatching { runRepairs(database.openHelper.writableDatabase) }.fold(
            onSuccess = {
                _failure.value = null
                DataResult.Success(Unit)
            },
            onFailure = { error ->
                report(error)
                DataResult.Failure(DataError.Storage(error))
            },
        )
    }
}

/**
 * The one-time moves, as a plain function so the app's callback, the retry and the migration-chain test all
 * run the same code (B324, B325). It throws; whether that is fatal is the caller's decision.
 */
internal fun runSeedRepairs(db: SupportSQLiteDatabase, context: Context, at: Long) {
    seedMissingExercises(db, at)
    moveLegacyGoals(db, context, at)
}
