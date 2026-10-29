package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.ProgressionMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A movement's own answer to *Done* survives the upgrade (ROADMAP N235, schema 13).
 *
 * The column is nullable with no default, so the migration is a plain ALTER and every row that existed reads
 * as "follow the app setting" — the one answer that is a fact about what the app did before, rather than a
 * guess at what the lifter would have chosen. The second half is the stored name: a value this build does not
 * know must read as unset rather than as a different answer.
 */
@RunWith(AndroidJUnit4::class)
class ProgressionModeMigrationTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theColumnTheMigrationAdds_isNullForEveryRowThatExisted() = runTest {
        // Room creates the file at the current version, so dropping the column's table and re-adding it the
        // way the migration does is what makes this a version-12 store's shape.
        val db = database.openHelper.writableDatabase
        db.execSQL("ALTER TABLE exercises DROP COLUMN progressionMode")

        MIGRATION_12_13.migrate(db)

        db.execSQL(
            // The converters' own separator: a list is stored as its names joined by commas, so an empty
            // list is an empty string rather than "[]".
            "INSERT INTO exercises (id, name, secondaryMuscles, rowKind, createdAt, updatedAt, deletedAt) " +
                "VALUES ('ex1', 'Back Squat', '', 'MOVEMENT', 1, 1, NULL)",
        )

        assertNull(dao.findById("ex1")?.progressionMode)
    }

    @Test
    fun eachAnswer_readsBackByName() = runTest {
        ProgressionMode.entries.forEach { mode ->
            val row = exercise(id = "ex-${mode.name}").copy(progressionMode = mode.name)
            dao.insertAll(listOf(row))

            assertEquals(mode.name, dao.findById(row.id)?.progressionMode)
        }
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = "Back Squat",
        primaryMuscle = null,
        secondaryMuscles = emptyList(),
        equipment = null,
        movementPattern = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )
}
