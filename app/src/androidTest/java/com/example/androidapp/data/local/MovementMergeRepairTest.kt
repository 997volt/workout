package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.SetType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The merge itself: two rows become one, and nothing is left pointing at the one that went (ROADMAP N238).
 *
 * The interesting half is the re-point. A session's and a plan's sets hang off their own rows rather than off the
 * movement, so a merge that only deleted the twin would take both rows' history with it in the sense that
 * mattered — the sets would still exist but the movement they were performed as would be gone.
 */
@RunWith(AndroidJUnit4::class)
class MovementMergeRepairTest {

    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aBareDuplicateOfASeededMovement_isMerged_andItsHistoryFollows() = runTest {
        // The seed's own row, then the import's leftover beside it.
        val merged = seedBoth()

        assertEquals("the twin is gone", 1, database.exerciseDao().findAllIncludingDeleted().size)
        assertEquals("the seeded row survives", "back-squat", merged.first())
        assertEquals(
            "the session logs the survivor now",
            "back-squat",
            database.sessionExerciseDao().findSessionExercise("se1")?.exerciseId,
        )
        assertEquals(
            "and so does the plan",
            "back-squat",
            database.templateDao().findTemplateExercises("t1").single().exerciseId,
        )
        assertEquals(
            "and the set is still there, at the load it was logged",
            100_000L,
            database.workoutDao().findSetsFor("s1", "back-squat").single().weightGrams,
        )
        assertEquals(
            "the retired id is remembered",
            "back-squat",
            database.movementMergeDao().retiredMovements().single().survivorId,
        )
    }

    @Test
    fun runningTheRepairTwice_changesNothing() = runTest {
        seedBoth()
        val afterFirst = database.exerciseDao().count() to database.movementMergeDao().retiredMovements()

        mergeDuplicateMovements(database.openHelper.writableDatabase, at = 3L)

        assertEquals(afterFirst, database.exerciseDao().count() to database.movementMergeDao().retiredMovements())
    }

    /**
     * The library with the seed's *Back Squat* and an import's bare twin, one session and one plan naming the
     * twin, then the repair. Returns the surviving rows' ids.
     */
    private suspend fun seedBoth(): List<String> {
        database.exerciseDao().insertAll(
            listOf(
                exercise(id = "back-squat", name = "Back Squat", equipment = "BARBELL", parentId = "squat"),
                exercise(id = "custom-back-squat", name = "Back Squat", equipment = null, parentId = null),
            ),
        )
        database.templateDao().insertTemplate(
            TemplateEntity(id = "t1", name = "Push A", createdAt = 1L, updatedAt = 1L, deletedAt = null),
        )
        database.templateDao().insertTemplateExercise(
            TemplateExerciseEntity(
                id = "te1",
                templateId = "t1",
                exerciseId = "custom-back-squat",
                position = 0,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = "s1",
                startedAt = 1L,
                finishedAt = 2L,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                createdAt = 1L,
                updatedAt = 2L,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "se1",
                sessionId = "s1",
                exerciseId = "custom-back-squat",
                position = 0,
                finishedAt = null,
                muscleFeel = null,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSet(
            SetEntryEntity(
                id = "set1",
                sessionExerciseId = "se1",
                setIndex = 0,
                reps = 5,
                weightGrams = 100_000L,
                setType = SetType.NORMAL,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )

        mergeDuplicateMovements(database.openHelper.writableDatabase, at = 5L)

        return database.exerciseDao().findAllIncludingDeleted().map { it.id }
    }

    private fun exercise(id: String, name: String, equipment: String?, parentId: String?) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscle = null,
        secondaryMuscles = emptyList(),
        equipment = equipment?.let { com.example.androidapp.domain.model.Equipment.valueOf(it) },
        movementPattern = null,
        parentId = parentId,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )
}
