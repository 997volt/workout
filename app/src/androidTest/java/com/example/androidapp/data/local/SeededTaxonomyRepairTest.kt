package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A movement the seed took over from a device's library gains its taxonomy (ROADMAP N239).
 *
 * The rows are already on that device under the ids the seed now ships — which is why their history did not have
 * to move — so the repair is the whole of what makes them searchable and sortable. The interesting cases are the
 * two it must **not** touch: a row a lifter filled in, and the seed's own rows, which were written with their
 * taxonomy in the first place.
 */
@RunWith(AndroidJUnit4::class)
class SeededTaxonomyRepairTest {

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
    fun aBareRowTheSeedTookOver_gainsItsEquipmentAndFamily() = runTest {
        // The import's shape: the id and name the seed now ships, and nothing else.
        dao.insertAll(listOf(exercise(id = LEG_RAISE, name = "Leg Raise")))

        seedMissingExercises(database.openHelper.writableDatabase, seededAt = 100L)

        val row = dao.findById(LEG_RAISE)
        assertEquals(Equipment.BODYWEIGHT, row?.equipment)
        assertEquals("the family the seed files it under", "core", row?.parentId)
        assertNull("the family holds the muscle, so the movement states none", row?.primaryMuscle)
    }

    @Test
    fun aRowALifterFilledIn_isLeftAlone() = runTest {
        // The same id, already stating facts of its own: the repair's guard is the shape, not the id.
        dao.insertAll(
            listOf(
                exercise(id = LEG_RAISE, name = "Leg Raise").copy(equipment = Equipment.CABLE),
            ),
        )

        seedMissingExercises(database.openHelper.writableDatabase, seededAt = 100L)

        assertEquals("their answer stands", Equipment.CABLE, dao.findById(LEG_RAISE)?.equipment)
    }

    @Test
    fun theSeedsOwnMovements_areNotRewritten() = runTest {
        seedMissingExercises(database.openHelper.writableDatabase, seededAt = 100L)
        val before = dao.findById("barbell-bench-press")

        seedMissingExercises(database.openHelper.writableDatabase, seededAt = 200L)

        assertEquals("a second open changes nothing", before, dao.findById("barbell-bench-press"))
    }

    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscle = null,
        secondaryMuscles = emptyList(),
        equipment = null,
        movementPattern = null,
        createdAt = 1L,
        updatedAt = 1L,
        deletedAt = null,
    )

    private companion object {
        const val LEG_RAISE = "imp_cba361b70e024188"
    }
}
