package com.example.androidapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The week's per-movement aggregate against a real database (ROADMAP N251).
 *
 * The boundaries are raw milliseconds here because the repository, not the query, owns the zone; this pins
 * that "before the week" and "this week" are two halves of the same row rather than two reads.
 */
@RunWith(AndroidJUnit4::class)
class WeeklyReviewDaoTest {

    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theWeekTop_meetsTheBeforeTop_andAFirstTrainingHasNoPriorSets() = runTest {
        database.exerciseDao().insertAll(
            listOf(exercise("squat", "Back Squat"), exercise("press", "Overhead Press")),
        )
        set(session = "last", at = 500L, exercise = "squat", weightGrams = 100_000)
        set(session = "this", at = 1_500L, exercise = "squat", weightGrams = 105_000)
        set(session = "press", at = 1_600L, exercise = "press", weightGrams = 60_000)

        val rows = database.weeklyReviewDao().movementGains(from = 1_000L, to = 2_000L)

        val squat = rows.single { it.exerciseId == "squat" }
        assertEquals(105_000L, squat.weekTop)
        assertEquals(100_000L, squat.beforeTop)
        assertEquals(1, squat.priorSets)
        val press = rows.single { it.exerciseId == "press" }
        assertEquals(60_000L, press.weekTop)
        assertEquals(0, press.priorSets)
        assertEquals("a movement not trained this week is not in the read", 2, rows.size)
    }

    @Test
    fun aDeletedSessionOrSet_isNotCounted() = runTest {
        database.exerciseDao().insertAll(listOf(exercise("squat", "Back Squat")))
        set(session = "deleted", at = 1_500L, exercise = "squat", weightGrams = 200_000, deletedAt = 1L)
        set(session = "live", at = 1_600L, exercise = "squat", weightGrams = 100_000)

        val rows = database.weeklyReviewDao().movementGains(from = 1_000L, to = 2_000L)

        assertEquals(100_000L, rows.single().weekTop)
    }

    private suspend fun set(
        session: String,
        at: Long,
        exercise: String,
        weightGrams: Long,
        deletedAt: Long? = null,
    ) {
        val sessionExerciseId = "se-$session-$exercise"
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = "session-$session",
                startedAt = at,
                finishedAt = at + 10,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                createdAt = at,
                updatedAt = at,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = sessionExerciseId,
                sessionId = "session-$session",
                exerciseId = exercise,
                position = 0,
                finishedAt = null,
                muscleFeel = null,
                createdAt = at,
                updatedAt = at,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSet(
            SetEntryEntity(
                id = "set-$sessionExerciseId",
                sessionExerciseId = sessionExerciseId,
                setIndex = 0,
                reps = 5,
                weightGrams = weightGrams,
                setType = SetType.TOP_SET,
                createdAt = at,
                updatedAt = at,
                deletedAt = deletedAt,
            ),
        )
    }

    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )
}
