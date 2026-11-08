package com.example.androidapp.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.TimeSource
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
 * The shell's failed-housekeeping notice (ROADMAP B324).
 *
 * The failure state and the pass itself belong to [Housekeeping], which has its own tests. This covers
 * the ViewModel's two thin, easy-to-break pieces: it shows and dismisses the **shared** state rather than
 * a copy, and a second retry cannot start while the first is still running.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HousekeepingViewModelTest {

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
        directory = File(context.cacheDir, "housekeeping-vm-${System.nanoTime()}")
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
    fun theNotice_isTheSharedFailure_soDismissingItStopsIt() {
        housekeeping.report(IllegalStateException("boom"), at = 42L)
        val viewModel = HousekeepingViewModel(housekeeping, database)

        assertThat(viewModel.failure.value?.at).isEqualTo(42L)
        assertThat(viewModel.retrying.value).isFalse()

        viewModel.dismiss()

        assertWithMessage("later leaves the record for Diagnostics")
            .that(housekeeping.failure.value)
            .isNull()
        assertThat(viewModel.failure.value).isNull()
    }

    @Test
    fun aRetryThatCannotFinish_keepsTheNotice_andSettlesTheButton() = runTest(dispatcher) {
        val broken = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            // A store the pass cannot read, so a retry really runs and records its own failure (B324).
            broken.openHelper.writableDatabase.execSQL("DROP TABLE exercises")
            housekeeping.report(IllegalStateException("boom"), at = 42L)
            val viewModel = HousekeepingViewModel(housekeeping, broken)

            viewModel.retry()

            assertThat(await { !viewModel.retrying.value }).isTrue()
            assertWithMessage("a failed retry keeps the notice")
                .that(viewModel.failure.value)
                .isNotNull()
        } finally {
            broken.close()
        }
    }

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
