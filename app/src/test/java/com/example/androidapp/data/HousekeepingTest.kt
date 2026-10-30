package com.example.androidapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.platform.CrashLogStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * Housekeeping that cannot finish is recorded and said once, and the app stays usable (ROADMAP B324).
 *
 * The 2.19 upgrade proved the alternative: one data-dependent foreign key in the seed's merge threw out of
 * Room's `onOpen`, so the app died on its first screen with every workout intact. These tests pin the three
 * behaviours the fix rests on — the failure is kept and recorded, a retry that finishes clears it, and a
 * retry that fails keeps it.
 */
@RunWith(AndroidJUnit4::class)
class HousekeepingTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var store: CrashLogStore
    private lateinit var housekeeping: Housekeeping
    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        directory = File(context.cacheDir, "crash-${System.nanoTime()}")
        store = CrashLogStore(directory)
        housekeeping = Housekeeping(store, TimeSource { Instant.ofEpochMilli(9_000L) }, context)
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        runCatching { database.close() }
        directory.deleteRecursively()
    }

    @Test
    fun aFailedPass_isRecordedWithoutItsMessage_andSaidOnce() {
        housekeeping.report(IllegalStateException("weight 102.5 kg is not a number"), at = 42L)

        val failure = housekeeping.failure.value!!
        assertEquals(42L, failure.at)
        assertEquals(IllegalStateException::class.java.name, failure.exceptionClass)
        assertTrue("the frames name the fault", failure.frames.contains("HousekeepingTest"))

        val logged = store.latest()!!
        assertEquals(IllegalStateException::class.java.name, logged.exceptionClass)
        assertFalse(
            "a message can carry a value, and the record never keeps one",
            logged.stackTrace.contains("102.5"),
        )

        housekeeping.dismiss()
        assertNull("later means the notice goes away", housekeeping.failure.value)
    }

    @Test
    fun aRetryThatFinishes_clearsTheFailure() = runTest {
        housekeeping.report(IllegalStateException("boom"), at = 1L)
        assertNotNull(housekeeping.failure.value)

        val outcome = housekeeping.retry(database)

        assertTrue(outcome is DataResult.Success)
        assertNull("the pass finished, so there is nothing to say", housekeeping.failure.value)
    }

    @Test
    fun aRetryThatDoesNotFinish_keepsTheFailure() = runTest {
        // A store the pass cannot read: the retry's failure path, without a test-only seam in production code.
        val broken = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            broken.openHelper.writableDatabase.execSQL("DROP TABLE exercises")

            val outcome = housekeeping.retry(broken)

            assertTrue(outcome is DataResult.Failure)
            assertNotNull("the notice stays, with the new failure", housekeeping.failure.value)
        } finally {
            runCatching { broken.close() }
        }
    }
}
