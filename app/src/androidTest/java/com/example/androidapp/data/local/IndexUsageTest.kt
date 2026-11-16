package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The N282 indices are actually selected by SQLite, not only declared on the entity (N321, N324).
 *
 * The SQL comes from the same constant the DAO annotation uses, so the test cannot plan a lookalike
 * while the production query changes underneath it.
 */
@RunWith(AndroidJUnit4::class)
class IndexUsageTest {

    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aSessionsTemplateIdRead_usesTheStartedAtIndex() {
        val detail = plan(QuerySql.SESSIONS_STARTED_BETWEEN, 0L, 1L)

        assertTrue(
            "the session read plans its range index: $detail",
            detail.contains("index_workout_sessions_startedAt"),
        )
    }

    @Test
    fun aMeasurementsDateRead_usesTheDateIndex() {
        val detail = plan(QuerySql.FIND_MEASUREMENT_ON_DAY, 0L, 1L, "DAILY")

        assertTrue(
            "the day read plans the measuredAt index: $detail",
            detail.contains("index_measurements_measuredAt"),
        )
    }

    private fun plan(sql: String, vararg arguments: Any): String =
        database.openHelper.readableDatabase
            .query("EXPLAIN QUERY PLAN $sql", arguments)
            .use { cursor ->
                val detail = cursor.getColumnIndexOrThrow("detail")
                buildString {
                    while (cursor.moveToNext()) appendLine(cursor.getString(detail))
                }
            }
}
