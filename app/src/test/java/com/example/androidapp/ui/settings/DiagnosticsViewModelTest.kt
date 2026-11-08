package com.example.androidapp.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.platform.CrashLog
import com.example.androidapp.platform.CrashLogStore
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * Diagnostics' read and retry (ROADMAP N244, B324).
 *
 * `HousekeepingTest` owns what a failed pass records and what a retry does to the store; this is the
 * screen's half — the last crash read once, the shared failure observed, and the retry's own in-flight
 * guard. `Housekeeping` and the database are real rather than faked: neither is a seam the app offers,
 * and inventing one for a test would be a production change N283 explicitly avoids.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var directory: File
    private lateinit var store: CrashLogStore
    private lateinit var database: WorkoutDatabase
    private var nowMillis = 9_000L
    private lateinit var housekeeping: Housekeeping

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        directory = File(context.cacheDir, "diagnostics-${System.nanoTime()}")
        store = CrashLogStore(directory)
        housekeeping = Housekeeping(
            crashLogStore = store,
            timeSource = TimeSource { Instant.ofEpochMilli(nowMillis++) },
            context = context,
        )
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        runCatching { database.close() }
        directory.deleteRecursively()
    }

    @Test
    fun theLastCrash_andTheFailedPass_areShownWhenTheScreenOpens() = runTest(dispatcher) {
        // The repair failure is recorded through Housekeeping, which also writes a crash log; a later,
        // distinct crash is what "last crash" must then mean.
        housekeeping.report(IllegalStateException("boom"), at = 42L)
        val crash = crash(timestamp = 43L)
        assertThat(store.record(crash)).isTrue()

        val viewModel = DiagnosticsViewModel(store, housekeeping, database, context)
        advanceUntilIdle()

        assertThat(viewModel.state.value.crash).isEqualTo(crash)
        assertThat(viewModel.state.value.repair?.at).isEqualTo(42L)
        assertThat(viewModel.state.value.retrying).isFalse()
    }

    @Test
    fun retry_runsThePassAgain_andClearsTheFailure() = runTest(dispatcher) {
        housekeeping.report(IllegalStateException("boom"), at = 42L)
        val viewModel = DiagnosticsViewModel(store, housekeeping, database, context)
        advanceUntilIdle()
        assertThat(viewModel.state.value.repair).isNotNull()

        viewModel.retry()

        assertThat(await { !viewModel.state.value.retrying }).isTrue()
        assertWithMessage("a retry that finished clears the failure")
            .that(viewModel.state.value.repair)
            .isNull()
    }

    @Test
    fun aRetryThatCannotFinish_keepsTheFailureReportable() = runTest(dispatcher) {
        val broken = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            // A store the pass cannot read, so a retry really runs and hits the failure path (B324).
            broken.openHelper.writableDatabase.execSQL("DROP TABLE exercises")
            housekeeping.report(IllegalStateException("boom"), at = 42L)
            val viewModel = DiagnosticsViewModel(store, housekeeping, broken, context)
            advanceUntilIdle()

            viewModel.retry()

            assertThat(await { !viewModel.state.value.retrying }).isTrue()
            assertWithMessage("a failed retry keeps the notice")
                .that(viewModel.state.value.repair)
                .isNotNull()
        } finally {
            broken.close()
        }
    }

    private fun crash(timestamp: Long) = CrashLog(
        timestamp = timestamp,
        exceptionClass = IllegalStateException::class.java.name,
        stackTrace = "${IllegalStateException::class.java.name}\n\tat a.b(C.kt:1)",
        appVersion = "test",
        versionCode = 1,
        androidVersion = "35",
        deviceModel = "Robolectric",
    )

    /**
     * Advances the test scheduler and waits for the real I/O `Housekeeping.retry` moves to its own
     * dispatcher. A retry is a real pass over a real database, so its completion cannot be virtual time;
     * only the settled state is asserted, because the in-flight window is not something a test can pin
     * without a seam in production code.
     */
    private fun TestScope.await(timeoutMillis: Long = 10_000L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            advanceUntilIdle()
            if (condition()) return true
            Thread.sleep(5L)
        }
        return condition()
    }
}
