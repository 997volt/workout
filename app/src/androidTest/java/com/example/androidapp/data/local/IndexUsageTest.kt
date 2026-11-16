package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The N282 indices are actually selected by SQLite, not only declared on the entity. */
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
    fun aSessionsTemplateIdRead_usesAnIndex() {
        // The query `ProgramDao.sessionsStartedBetween` runs, not a lookalike (N321).
        val detail = plan(
            """
            SELECT id AS sessionId,
                   templateId AS templateId,
                   startedAt AS startedAt,
                   zoneOffsetMinutes AS zoneOffsetMinutes
            FROM workout_sessions
            WHERE templateId IS NOT NULL
              AND deletedAt IS NULL
              AND startedAt >= ?
              AND startedAt < ?
            ORDER BY startedAt ASC
            """,
            0L,
            1L,
        )
        assertTrue(
            "the session read plans an index: $detail",
            detail.contains("index_workout_sessions_templateId") ||
                detail.contains("index_workout_sessions_startedAt"),
        )
    }

    @Test
    fun aMeasurementsDateRead_usesTheDateIndex() {
        // The query `MeasurementDao.findOnDay` runs (N321).
        val detail = plan(
            """
            SELECT * FROM measurements
            WHERE measuredAt >= ? AND measuredAt < ? AND deletedAt IS NULL AND kind = ?
            ORDER BY measuredAt DESC LIMIT 1
            """,
            0L,
            1L,
            "DAILY",
        )
        assertTrue(
            "the day read plans the measuredAt index: $detail",
            detail.contains("index_measurements_measuredAt"),
        )
    }

    private fun plan(sql: String, vararg arguments: Any): String {
        database.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN $sql", arguments).use { cursor ->
            val detail = cursor.getColumnIndexOrThrow("detail")
            return buildString {
                while (cursor.moveToNext()) appendLine(cursor.getString(detail))
            }
        }
    }
}
