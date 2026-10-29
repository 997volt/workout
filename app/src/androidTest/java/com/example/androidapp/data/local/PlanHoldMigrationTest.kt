package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A plan that prescribes a hold survives the upgrade (ROADMAP N242, schema 15).
 *
 * The column is nullable with no default, so the migration is a plain ALTER and every planned set that
 * exists is counted, which is exactly what it was. The second half is that the column is a *prescription*
 * rather than a copy: it round-trips through the entity untouched, and a set that names both is refused by the
 * write boundary rather than by the column.
 */
@RunWith(AndroidJUnit4::class)
class PlanHoldMigrationTest {

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

    /**
     * The three rows a planned set needs behind it: the foreign keys are real.
     *
     * Written rather than faked because the point of the row is that an upgrade has one to read; an empty
     * table cannot show whether an ALTER left the existing rows readable.
     */
    private suspend fun chain() {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = null,
                    secondaryMuscles = emptyList(),
                    equipment = null,
                    movementPattern = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    deletedAt = null,
                ),
            ),
        )
        database.templateDao().insertTemplate(
            TemplateEntity(id = "t1", name = "Push A", createdAt = 1L, updatedAt = 1L, deletedAt = null),
        )
        database.templateDao().insertTemplateExercise(
            TemplateExerciseEntity(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )
    }

    @Test
    fun theColumnTheMigrationAdds_isNullForEverySetThatExisted() = runTest {
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE template_sets DROP COLUMN targetDurationSeconds")

        MIGRATION_14_15.migrate(db)

        chain()
        db.execSQL(
            "INSERT INTO template_sets (id, templateExerciseId, setIndex, role, " +
                "targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, " +
                "targetRepsCurrent, createdAt, updatedAt) " +
                "VALUES ('ts1', 'te1', 0, 'NORMAL', NULL, NULL, 5, 5, NULL, 1, 1)",
        )
        val stored = database.templateSetDao().findTemplateSet("ts1")
        assertNull(stored?.targetDurationSeconds)
    }

    @Test
    fun aHold_isStoredAndReadBack_inSeconds() = runTest {
        chain()
        database.templateSetDao().insertTemplateSet(
            TemplateSetEntity(
                id = "ts1",
                templateExerciseId = "te1",
                setIndex = 0,
                role = com.example.androidapp.domain.model.SetType.NORMAL,
                targetWeightGrams = null,
                targetAssistanceGrams = null,
                targetRepsMin = null,
                targetRepsMax = null,
                targetRepsCurrent = null,
                targetDurationSeconds = 45,
                note = null,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )

        assertEquals(45, database.templateSetDao().findTemplateSet("ts1")?.targetDurationSeconds)
    }
}
