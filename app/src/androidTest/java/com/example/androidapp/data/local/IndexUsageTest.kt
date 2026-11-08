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
    fun aSessionsTemplateIdRead_usesTheTemplateIndex() {
        assertTrue(
            plan("SELECT * FROM workout_sessions WHERE templateId = ?", "t1")
                .contains("index_workout_sessions_templateId"),
        )
    }

    @Test
    fun aMeasurementsDateRead_usesTheDateIndex() {
        assertTrue(
            plan("SELECT * FROM measurements WHERE measuredAt >= ?", 1L)
                .contains("index_measurements_measuredAt"),
        )
    }

    private fun plan(sql: String, argument: Any): String {
        database.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN $sql", arrayOf(argument)).use { cursor ->
            val detail = cursor.getColumnIndexOrThrow("detail")
            return buildString {
                while (cursor.moveToNext()) appendLine(cursor.getString(detail))
            }
        }
    }
}
