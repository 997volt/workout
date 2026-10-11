package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The library's shape change, carried across by [MIGRATION_1_2] (ROADMAP N110).
 *
 * **The schema is identical either side of the migration** — only the links change — so a database Room
 * creates today has the tables a schema-1 file had, and the migration can be run against it directly rather
 * than through Room's own machinery.
 *
 * The rows are written through the DAO rather than the repository because **the repository refuses the old
 * shape now** (a movement under a movement), which is exactly what a database written by the previous build
 * holds.
 */
@RunWith(AndroidJUnit4::class)
class LibraryMigrationTest {

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
    fun aMovementFiledUnderAMovement_isRefiledUnderItsCategory_andKeepsWhatItRead() = runTest {
        val dao = database.exerciseDao()
        dao.insertAll(
            listOf(
                category("cat-bench", "Bench Press", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS)),
                movement(
                    id = "barbell-bench-press",
                    parent = "cat-bench",
                    primary = MuscleGroup.CHEST,
                    secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                    equipment = Equipment.BARBELL,
                ),
                // The variation: it stated nothing at all and read every fact from the movement above it
                // (N103), which is why the fold has to run before the link moves.
                movement(
                    id = "paused-bench",
                    parent = "barbell-bench-press",
                    primary = null,
                    secondary = emptyList(),
                    equipment = null,
                ),
            ),
        )

        MIGRATION_1_2.migrate(database.openHelper.writableDatabase)

        val refiled = dao.findAllIncludingDeleted().single { it.id == "paused-bench" }
        assertEquals("filed under the category, not under the bar", "cat-bench", refiled.parentId)
        assertEquals(MuscleGroup.CHEST, refiled.primaryMuscle)
        assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), refiled.secondaryMuscles)
        assertEquals(
            "equipment travels with it — a paused bench still uses a bar",
            Equipment.BARBELL,
            refiled.equipment,
        )
    }

    @Test
    fun aSeededCategory_isFilledFromTheSeed_andALiftersOwnAnswerIsLeftAlone() = runTest {
        val dao = database.exerciseDao()
        dao.insertAll(
            listOf(
                // A seeded category as the old build wrote it: a pattern, and no muscle at all.
                category("bench-press", "Bench Press", null, emptyList()),
                // One the lifter has already answered, which the migration must not overwrite.
                category("curl", "Curl", MuscleGroup.SHOULDERS, listOf(MuscleGroup.CORE)),
            ),
        )

        MIGRATION_1_2.migrate(database.openHelper.writableDatabase)

        val rows = dao.findAllIncludingDeleted().associateBy { it.id }
        assertEquals(MuscleGroup.CHEST, rows.getValue("bench-press").primaryMuscle)
        assertEquals(
            listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            rows.getValue("bench-press").secondaryMuscles,
        )
        assertEquals(
            "an answer the lifter gave is not overwritten",
            MuscleGroup.SHOULDERS,
            rows.getValue("curl").primaryMuscle,
        )
        assertEquals(listOf(MuscleGroup.CORE), rows.getValue("curl").secondaryMuscles)
    }

    @Test
    fun anOrdinaryLibrary_passesThroughUntouched() = runTest {
        // A movement filed under a category — the only legal shape after this change — must not be re-filed,
        // folded, or otherwise rewritten.
        val dao = database.exerciseDao()
        dao.insertAll(
            listOf(
                category("cat-bench", "Bench Press", MuscleGroup.CHEST, emptyList()),
                movement(
                    id = "barbell-bench-press",
                    parent = "cat-bench",
                    primary = MuscleGroup.CHEST,
                    secondary = emptyList(),
                    equipment = Equipment.BARBELL,
                ),
            ),
        )

        MIGRATION_1_2.migrate(database.openHelper.writableDatabase)

        val row = dao.findAllIncludingDeleted().single { it.id == "barbell-bench-press" }
        assertEquals("cat-bench", row.parentId)
        assertEquals(MuscleGroup.CHEST, row.primaryMuscle)
        assertEquals(Equipment.BARBELL, row.equipment)
    }

    private fun category(
        id: String,
        name: String,
        primary: MuscleGroup?,
        secondary: List<MuscleGroup>,
    ) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = null,
        movementPattern = MovementPattern.PRESS,
        parentId = null,
        rowKind = RowKind.CATEGORY,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )

    private fun movement(
        id: String,
        parent: String?,
        primary: MuscleGroup?,
        secondary: List<MuscleGroup>,
        equipment: Equipment?,
    ) = ExerciseEntity(
        id = id,
        name = id,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        movementPattern = null,
        parentId = parent,
        rowKind = RowKind.MOVEMENT,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )
}
