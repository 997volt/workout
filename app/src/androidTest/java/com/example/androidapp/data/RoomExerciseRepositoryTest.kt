package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Custom exercises, end to end through the repository (ROADMAP N2).
 *
 * The decisions this pins down are the ones a later session is most likely to
 * second-guess: creation asks for the *name only*, so the taxonomy is stored as
 * unspecified rather than invented; and editing fills that taxonomy in without
 * disturbing the row's identity.
 */
@RunWith(AndroidJUnit4::class)
class RoomExerciseRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomExerciseRepository

    private var now: Instant = Instant.parse("2026-09-29T08:00:00Z")
    private val clock = TimeSource { now }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomExerciseRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createCustomExercise_storesTheNameAndStatesNoTaxonomy() = runTest {
        val created = created("  Sled Push  ")

        // A UUID, not a seeded slug: user-created rows must not collide with the
        // seed library's human-readable ids.
        UUID.fromString(created.id)
        assertEquals("Sled Push", created.name)
        // **Null is *unstated*** (ROADMAP N103), not `OTHER`: the row states nothing, and there is a separate
        // `OTHER` for a lifter who means *other*.
        assertNull(created.primaryMuscle)
        assertNull(created.equipment)
        assertNull(created.movementPattern)
        // N5: rest and cue start unset, i.e. "use the app default" and "no cue".
        assertNull(created.restSeconds)
        assertNull(created.techniqueNote)

        // The reads are values now (ROADMAP B4), so a test unwraps them like one.
        val library = (repository.observeExercises().first() as DataResult.Success).data
        assertEquals(listOf("Sled Push"), library.map { it.name })
    }

    @Test
    fun createCustomExercise_refusesABlankName_withoutStoringAnything() = runTest {
        val failure = repository.createCustomExercise("   ") as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        val library = (repository.observeExercises().first() as DataResult.Success).data
        assertTrue("a refused create must store nothing", library.isEmpty())
    }

    @Test
    fun createCustomExercise_refusesANameTheLibraryHolds() = runTest {
        // B298: a second row under one name is the twin N238's merge exists to settle. Creation must not mint
        // one, or the repair deletes the movement the lifter just made.
        created("Sled Push")

        val failure = repository.createCustomExercise("  sled push ") as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        val library = (repository.observeExercises().first() as DataResult.Success).data
        assertEquals("the refused row is not stored", 1, library.size)
    }

    @Test
    fun aRenameToANameTheLibraryHolds_isRefused_andKeepsTheRow() = runTest {
        val first = created("Sled Push")
        val second = created("Sled Drag")

        val refused = repository.updateExercise(second.copy(name = "sled push")) as DataResult.Failure

        assertTrue(refused.error is DataError.Invalid)
        assertEquals("Sled Drag", stored(second.id).name)
        assertTrue("the row it collided with is untouched", repository.getExercise(first.id) is DataResult.Success)
        assertTrue(
            "its own name, in another case, is not a collision",
            repository.updateExercise(first.copy(name = "Sled Push ")) is DataResult.Success,
        )
    }

    @Test
    fun editingAnExistingDuplicate_keepsItsName_andIsAllowed() = runTest {
        // B298's follow-up: a library can already hold two rows under one name — a document's movement the
        // merge did not take away — and the duplicate-name rule must not make either row uneditable.
        created("Sled Push")
        val second = created("Sled Drag")
        database.exerciseDao().update(
            database.exerciseDao().findById(second.id)!!.copy(name = "Sled Push"),
        )

        val duplicate = (repository.getExercise(second.id) as DataResult.Success).data!!
        val edit = duplicate.copy(techniqueNote = "Brace, then push")
        assertTrue(
            "an edit that keeps the name is not a rename",
            repository.updateExercise(edit) is DataResult.Success,
        )
        assertEquals("Brace, then push", stored(second.id).techniqueNote)

        val third = created("Sled Pull")
        val refused = repository.updateExercise(third.copy(name = "Sled Push"))
        assertTrue("a real rename onto a held name is still refused", refused is DataResult.Failure)
    }

    @Test
    fun aCueEdit_keepsTheSeedsMarker_andAFlagChange_stampsTheLifters() = runTest {
        // B327: the repair must not treat every edit as a flag answer, and must treat a flag change as one.
        val created = created("Sled Push")
        assertNull("a custom row starts unmarked", database.exerciseDao().findById(created.id)?.seedFlagsAt)

        val cue = created.copy(techniqueNote = "Brace, then push")
        assertTrue(repository.updateExercise(cue) is DataResult.Success)
        assertNull(
            "an edit that is not a flag answer keeps the marker",
            database.exerciseDao().findById(created.id)?.seedFlagsAt,
        )

        assertTrue(repository.updateExercise(cue.copy(isBodyweight = true)) is DataResult.Success)
        assertEquals(
            "a flag change is the lifter's answer",
            0L,
            database.exerciseDao().findById(created.id)?.seedFlagsAt,
        )
    }

    @Test
    fun updateExercise_fillsInTheTaxonomy_keepingTheRowsIdentity() = runTest {
        val created = created("Sled Push")
        now = now.plusSeconds(60)

        val edited = created.copy(
            name = "Sled Push Heavy",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.SQUAT,
            restSeconds = 180,
            techniqueNote = "Drive the floor away",
        )
        assertTrue(repository.updateExercise(edited) is DataResult.Success)

        val stored = (repository.getExercise(created.id) as DataResult.Success).data!!
        assertEquals("Sled Push Heavy", stored.name)
        assertEquals(MuscleGroup.QUADS, stored.primaryMuscle)
        assertEquals(listOf(MuscleGroup.GLUTES), stored.secondaryMuscles)
        assertEquals(Equipment.MACHINE, stored.equipment)
        assertEquals(MovementPattern.SQUAT, stored.movementPattern)
        assertEquals(180, stored.restSeconds)
        assertEquals("Drive the floor away", stored.techniqueNote)
        // Identity and origin are untouched by an attribute edit.
        assertEquals(created.id, stored.id)

        val row = database.exerciseDao().findById(created.id)!!
        assertNotEquals("an edit must bump updatedAt", row.createdAt, row.updatedAt)
    }

    /** Reads are values now (ROADMAP B4); a test unwraps them in one place. */
    private suspend fun stored(id: String): Exercise =
        (repository.getExercise(id) as DataResult.Success).data
            ?: error("exercise $id should exist")

    @Test
    fun updateExercise_keepsTheFlagsThatChangeWhatASetMeans() = runTest {
        // B240: the flags are the whole of N219 and N220 at the write boundary. A copy that forgot them
        // looked saved on screen and stored the old value, which made both features impossible to switch on.
        val created = created("Pull-up")

        assertTrue(
            repository.updateExercise(created.copy(isBodyweight = true, isTimed = true))
            is DataResult.Success,
        )

        val stored = stored(created.id)
        assertTrue("a movement marked bodyweight stays bodyweight", stored.isBodyweight)
        assertTrue("and a hold stays a hold", stored.isTimed)

        // And a later edit that says nothing about them keeps them, which is what `stored.copy` is for.
        assertTrue(repository.updateExercise(stored.copy(name = "Pull-up heavy")) is DataResult.Success)
        val renamed = stored(created.id)
        assertTrue(renamed.isBodyweight)
        assertTrue(renamed.isTimed)
    }

    @Test
    fun updateExercise_storesAZeroRest_asADeliberateNone() = runTest {
        // ROADMAP N45: zero is a value — "this exercise has no rest" — not input to refuse.
        val created = created("Sled Push")

        assertTrue(repository.updateExercise(created.copy(restSeconds = 0)) is DataResult.Success)
        assertEquals(0, stored(created.id).restSeconds)
    }

    @Test
    fun updateExercise_refusesANegativeRest() = runTest {
        val created = created("Sled Push")

        val failure = repository.updateExercise(created.copy(restSeconds = -1)) as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        // Nothing was written, so the exercise still reads as "use the default".
        assertNull(stored(created.id).restSeconds)
    }

    @Test
    fun updateExercise_storesAClearedCueAsNull_ratherThanAnEmptyString() = runTest {
        val created = created("Sled Push")
        repository.updateExercise(created.copy(techniqueNote = "Brace"))
        assertEquals("Brace", stored(created.id).techniqueNote)

        repository.updateExercise(created.copy(techniqueNote = "   "))

        // Two representations of "nothing" would render differently on screen.
        assertNull(stored(created.id).techniqueNote)
    }

    @Test
    fun updateExercise_storesTheExercisesOwnWeightUnit_andCanClearItBackToTheApp() = runTest {
        // ROADMAP N64: the exercise's own display unit is a property of the row like its rest or its
        // cue, so an edit has to write it. It was the one field a rebuilt row dropped, which made the
        // choice look saved until the screen was reopened and re-read the row.
        val created = created("Lat Pulldown")

        assertTrue(
            repository.updateExercise(created.copy(weightUnit = WeightUnit.POUNDS))
            is DataResult.Success,
        )
        assertEquals(WeightUnit.POUNDS, stored(created.id).weightUnit)

        // Null is a real answer — "follow the app setting" — not a missing one.
        val pounds = stored(created.id)
        assertTrue(repository.updateExercise(pounds.copy(weightUnit = null)) is DataResult.Success)
        assertNull(stored(created.id).weightUnit)
    }

    @Test
    fun updateExercise_missingRow_isNotFound() = runTest {
        val created = created("Sled Push")

        val failure = repository.updateExercise(created.copy(id = "does-not-exist")) as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun updateExercise_aSoftDeletedExercise_isNotFound_ratherThanResurrected() = runTest {
        val created = created("Sled Push")
        database.exerciseDao().softDelete(created.id, deletedAt = 1L)

        val failure = repository.updateExercise(created.copy(name = "Renamed")) as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
        assertNull(database.exerciseDao().findById(created.id))
    }

    @Test
    fun createCategory_storesAHeadThatIsNotALift() = runTest {
        // ROADMAP N95: a category is a full row of the library and differs from a custom exercise in one
        // field, which is what the row kind is for rather than a table of its own.
        val category = (repository.createCategory("  Bench Press  ") as DataResult.Success<Exercise>).data

        UUID.fromString(category.id)
        assertEquals("Bench Press", category.name)
        assertEquals(RowKind.CATEGORY, category.rowKind)
        assertNull("a head hangs under nothing", category.parentId)
        // Unstated for a custom exercise's reason (N103): a family spans equipment and every child sets its
        // own, so a head that claimed `OTHER` would be saying something it does not mean.
        assertNull(category.primaryMuscle)
        assertNull(category.equipment)
        assertNull(category.movementPattern)

        val library = (repository.observeExercises().first() as DataResult.Success).data
        assertEquals(listOf(RowKind.CATEGORY), library.map { it.rowKind })
    }

    @Test
    fun createCategory_refusesABlankName_withoutStoringAnything() = runTest {
        val failure = repository.createCategory("   ") as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        val library = (repository.observeExercises().first() as DataResult.Success).data
        assertTrue("a refused create must store nothing", library.isEmpty())
    }

    @Test
    fun updateExercise_filesARowUnderACategory_andCanUnfileIt() = runTest {
        // *Move to category* is this write and nothing else (N95): the parent is one of the row's own
        // editable attributes. The bug this guards is `stored.copy` keeping every field it is not told,
        // so a field left out of the copy is silently ignored — the screen would show the new head while
        // the row kept the old one.
        val category = (repository.createCategory("Bench Press") as DataResult.Success<Exercise>).data
        val variation = created("Paused Bench Press")

        repository.updateExercise(variation.copy(parentId = category.id))

        val filed = library().single { it.id == variation.id }
        assertEquals(category.id, filed.parentId)
        assertEquals("filing moves no other field", "Paused Bench Press", filed.name)
        assertEquals(RowKind.MOVEMENT, filed.rowKind)

        repository.updateExercise(filed.copy(parentId = null))

        assertNull("and unfiling is the same write with no parent", library().single { it.id == variation.id }.parentId)
    }

    @Test
    fun updateExercise_keepsTheRowKindItWasStoredWith() = runTest {
        // The kind is not the form's to change — a lift does not become a head by being edited — but it
        // travels through the same copy, so a value dropped there would turn every edited category into a
        // lift and offer it in a picker.
        val category = (repository.createCategory("Bench Press") as DataResult.Success<Exercise>).data

        repository.updateExercise(category.copy(name = "Barbell Bench Press"))

        val renamed = library().single()
        assertEquals(RowKind.CATEGORY, renamed.rowKind)
        assertEquals("Barbell Bench Press", renamed.name)
    }

    @Test
    fun updateExercise_filesAMovementUnderACategory() = runTest {
        // N110's shape at the write boundary: a category is the one thing a movement may be filed under.
        val category = (repository.createCategory("Bench Press") as DataResult.Success<Exercise>).data
        val movement = created("Barbell Bench Press")

        assertTrue(repository.updateExercise(movement.copy(parentId = category.id)) is DataResult.Success)
        assertEquals(category.id, library().single { it.id == movement.id }.parentId)
    }

    @Test
    fun updateExercise_refusesToFileAMovementUnderAMovement() = runTest {
        // B82, N110: the variation level is gone, so a movement is nobody's head — and a stale screen that
        // still offered one must not be able to write the link back.
        val parent = created("Barbell Bench Press")
        val child = created("Paused Bench Press")

        val failure = repository.updateExercise(child.copy(parentId = parent.id)) as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
    }

    @Test
    fun updateExercise_refusesToFileACategoryUnderAnything() = runTest {
        // A category sits at the top of the shape (N95), whatever it is offered.
        val category = (repository.createCategory("Bench Press") as DataResult.Success<Exercise>).data
        val other = created("Sled Push")

        val failure = repository.updateExercise(category.copy(parentId = other.id)) as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
    }

    @Test
    fun updateExercise_refusesToFileARowUnderItself() = runTest {
        // B92: the write boundary holds the shape. The screen never offers it; a stale write must not either.
        val row = created("Sled Push")

        val failure = repository.updateExercise(row.copy(parentId = row.id)) as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
    }

    private suspend fun library(): List<Exercise> =
        (repository.observeExercises().first() as DataResult.Success).data

    @Test
    fun merging_movesTheHistory_keepsTheSurvivorsSettings_andRetiresTheSource() = runTest {
        val source = created("Sled Push")
        val survivor = created("Sled Drag")
        // Only the source states the cue, so the merge carries it into the survivor's gap (B298, N245).
        assertTrue(
            repository.updateExercise(source.copy(techniqueNote = "Brace, then push")) is DataResult.Success,
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
                exerciseId = source.id,
                position = 0,
                finishedAt = null,
                muscleFeel = null,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )
        database.templateDao().insertTemplate(
            TemplateEntity(id = "t1", name = "Push", createdAt = 1L, updatedAt = 1L, deletedAt = null),
        )
        database.templateDao().insertTemplateExercise(
            TemplateExerciseEntity(
                id = "te1",
                templateId = "t1",
                exerciseId = source.id,
                position = 0,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )

        val result = repository.mergeExercises(source.id, survivor.id)

        assertTrue(result is DataResult.Success)
        assertEquals(
            "the session follows",
            survivor.id,
            database.sessionExerciseDao().findSessionExercise("se1")?.exerciseId,
        )
        assertEquals(
            "and the plan",
            survivor.id,
            database.templateDao().findTemplateExercises("t1").single().exerciseId,
        )
        assertNull("the source row is gone", database.exerciseDao().findById(source.id))
        assertEquals(
            "the retired id maps to the survivor",
            survivor.id,
            database.movementMergeDao().retiredMovements().single { it.id == source.id }.survivorId,
        )
        assertEquals(
            "the source's cue fills the survivor's gap",
            "Brace, then push",
            database.exerciseDao().findById(survivor.id)?.techniqueNote,
        )
    }

    @Test
    fun mergingAMovementIntoItself_orACategory_isRefused() = runTest {
        val movement = created("Sled Push")
        val category = (repository.createCategory("Sleds") as DataResult.Success<Exercise>).data

        val self = repository.mergeExercises(movement.id, movement.id) as DataResult.Failure
        assertTrue(self.error is DataError.Invalid)
        val intoCategory = repository.mergeExercises(movement.id, category.id) as DataResult.Failure
        assertTrue(intoCategory.error is DataError.Invalid)
        assertTrue("the source is still there", repository.getExercise(movement.id) is DataResult.Success)
    }

    private suspend fun created(name: String): Exercise =
        (repository.createCustomExercise(name) as DataResult.Success<Exercise>).data
}
