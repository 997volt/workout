package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.TemplateSetEdit
import java.time.DayOfWeek
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Carrying one program to another device, end to end through the schema (ROADMAP N47).
 *
 * What needs a device is the round trip: a program's templates, their planned work and the
 * definition of the exercises they name have to survive being written to a document and read back
 * into an empty library. The format itself is a pure JVM test.
 */
@RunWith(AndroidJUnit4::class)
class ProgramDocumentTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var programs: RoomProgramRepository
    private lateinit var templates: RoomTemplateRepository

    private val clock = TimeSource { Instant.parse("2026-10-07T12:00:00Z") }

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        programs = RoomProgramRepository(database, clock)
        templates = RoomTemplateRepository(database, clock)
        database.exerciseDao().insertAll(listOf(exercise("back-squat")))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aProgramDocument_roundTripsThroughAnEmptyDevice() = runTest {
        val programId = seedProgram()
        val file = (programs.exportProgramDocument(programId) as DataResult.Success).data

        // Another device: nothing but the seeded library, and not even that for the exercise the
        // document carries — the document is the only thing that can bring it back.
        wipe()

        val summary = importOf(file)

        assertEquals(1, summary.programs)
        assertEquals(1, summary.templates)
        // The exercise the document carried was created, because the device did not have it.
        assertEquals(1, summary.exercises)
        assertEquals(0, summary.droppedMovements)

        val program = programs.observePrograms().first().single()
        assertEquals("Heavy lower", program.name)
        // A document adds a program; it does not decide which one home follows (P3.3).
        assertFalse("a loaded program does not become active", program.isActive)

        val template = templates.observeTemplates().first().single()
        val exercises = templates.observeExercises(template.id).first()
        assertEquals("back-squat", exercises.single().exerciseId)
        assertEquals(1, exercises.single().sets.size)
        assertEquals(SetType.WARMUP, exercises.single().sets.single().role)
        assertEquals(
            "the rest the slot prescribed came too",
            0,
            exercises.single().restSeconds,
        )
        assertEquals(listOf(DayOfWeek.MONDAY), programs.observeSlots(program.id).first().map { it.weekday })
    }

    @Test
    fun aDeletedProgram_comesBackWhenItsDocumentIsLoadedAgain() = runTest {
        // B177: deleting a program soft-deletes it, and an insert-only merge would ignore every row of
        // a document that names it — the example card answered "already here" to a program that was
        // not there. Loading the document again is the request, so its own row is un-hidden, and only
        // its visibility: the follow state the delete cleared stays cleared.
        val file = exportOf(seedProgram())
        wipe()
        importOf(file)
        val programId = programs.observePrograms().first().single().id
        (programs.activateProgram(programId) as DataResult.Success)
        (programs.deleteProgram(programId) as DataResult.Success)
        assertTrue("the program is gone from the list", programs.observePrograms().first().isEmpty())

        val summary = importOf(file)

        assertEquals("the program is back", 1, summary.programs)
        assertEquals("and nothing else was written", 0, summary.templates)
        assertEquals(0, summary.exercises)
        val restored = programs.observePrograms().first().single()
        assertEquals("it is the same program", programId, restored.id)
        assertFalse("a restore does not re-follow what was set down", restored.isActive)
        assertEquals(
            listOf(DayOfWeek.MONDAY),
            programs.observeSlots(programId).first().map { it.weekday },
        )
    }

    @Test
    fun loadingTheSameDocumentTwice_isANoOp() = runTest {
        val file = exportOf(seedProgram())
        wipe()

        importOf(file)
        val second = importOf(file)

        assertEquals("nothing was added the second time", 0, second.programs)
        assertEquals(0, second.templates)
        assertEquals(0, second.exercises)
        assertEquals(1, programs.observePrograms().first().size)
    }

    @Test
    fun aChangedDocument_doesNotEditTheProgramItNames() = runTest {
        // B180: a document's program id is a slug of its name, so a changed sheet keeps it. Merging the
        // document row by row attached its new day to the program the lifter already had — an edit, which
        // "creates and never edits" refuses and "a changed sheet re-imported does nothing" promises cannot
        // happen. The whole document is the unit, so a live id ends the merge there.
        val file = exportOf(seedProgram())
        wipe()
        importOf(file)
        val document = ProgramDocumentCodec.decode(file)
        val changed = ProgramDocumentCodec.encode(
            document.copy(
                templates = document.templates + document.templates.single().copy(
                    id = "tpl-second",
                    name = "Second day",
                ),
                slots = document.slots + document.slots.single().copy(
                    id = "slot-second",
                    templateId = "tpl-second",
                ),
            ),
        )

        val summary = importOf(changed)

        assertEquals("nothing was merged into the program", 0, summary.programs)
        assertEquals(0, summary.templates)
        val program = programs.observePrograms().first().single()
        assertEquals(
            "the program still has the one day it had",
            1,
            programs.observeSlots(program.id).first().size,
        )
        assertEquals("and no second template was written", 1, templates.observeTemplates().first().size)
    }

    @Test
    fun aMovementWhoseExerciseIsMissing_isDropped_whileTheProgramArrives() = runTest {
        val file = exportOf(seedProgram())
        // A hand-edited or truncated document: it names an exercise it does not carry.
        val stripped = ProgramDocumentCodec.encode(
            ProgramDocumentCodec.decode(file).copy(exercises = emptyList()),
        )
        wipe()

        val summary = importOf(stripped)

        assertEquals("the program still arrives", 1, summary.programs)
        assertEquals("one movement could not", 1, summary.droppedMovements)
        val template = templates.observeTemplates().first().single()
        assertTrue(templates.observeExercises(template.id).first().isEmpty())
    }

    @Test
    fun aMovementWhoseExerciseWasDeletedHere_isDroppedRatherThanResurrected() = runTest {
        // B51: the device holds the exercise, but as a soft-deleted row. The insert ignores an id
        // that exists and the presence read filters `deletedAt`, so the movement is dropped — and
        // the exercise stays deleted, because un-deleting a lift is a write to the library that
        // "overwrites nothing" refuses to make.
        val file = exportOf(seedProgram())
        wipe()
        database.exerciseDao().insertAll(listOf(exercise("back-squat").copy(deletedAt = 1L)))

        val summary = importOf(file)

        assertEquals("the program still arrives", 1, summary.programs)
        assertEquals("the exercise the document carried was not resurrected", 0, summary.exercises)
        assertEquals("and its movement could not be placed", 1, summary.droppedMovements)
        assertTrue(
            "the row is still the device's deleted one",
            "back-squat" in database.backupDao().softDeletedExerciseIds(),
        )
    }

    /** A program with one template, one planned set and one slot, ready to export. */
    private suspend fun seedProgram(exerciseId: String = "back-squat"): String {
        val programId = (programs.createProgram("Heavy lower") as DataResult.Success).data
        val templateId = (templates.createTemplate("Squat day") as DataResult.Success).data
        templates.addExercise(templateId, exerciseId)
        val planned = templates.observeExercises(templateId).first().single().id
        templates.addSet(
            planned,
            TemplateSetEdit(role = SetType.WARMUP, targetWeightGrams = 60_000L, targetRepsMax = 5),
        )
        templates.setExercisePlan(planned, restSeconds = 0, techniqueNote = null, targetRpeHalves = null)
        programs.addSlot(programId, templateId, DayOfWeek.MONDAY)
        return programId
    }

    private suspend fun exportOf(programId: String): String =
        (programs.exportProgramDocument(programId) as DataResult.Success).data

    private suspend fun importOf(file: String): ProgramImportSummary =
        (programs.importProgramDocument(file) as DataResult.Success).data

    /** Back to a bare database, which is what "another device" means for this feature. */
    private suspend fun wipe() = withContext(Dispatchers.IO) { database.clearAllTables() }

    private fun exercise(
        id: String,
        name: String = id,
        parentId: String? = null,
        rowKind: RowKind = RowKind.MOVEMENT,
    ) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        parentId = parentId,
        rowKind = rowKind,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    @Test
    fun aDocumentCarryingAFormerVariation_flattensItOntoItsCategory() = runTest {
        // ROADMAP N103, N110: a movement's category is part of its definition, so the whole chain travels and a
        // receiving device has something to file the movement under. What it **writes** is the new shape — a
        // movement is nobody's head — so the link to the movement above is replaced by its category and what
        // the row used to read from that movement is written down first, or the facts would be lost with it.
        database.exerciseDao().insertAll(
            listOf(
                exercise("cat-bench", name = "Bench Press", rowKind = RowKind.CATEGORY),
                exercise("barbell-bench-press", name = "Barbell Bench Press", parentId = "cat-bench"),
                // A variation as an older build wrote one: it states nothing and read every fact from the row
                // above it, which is why the fold has to run before the link moves.
                exercise("paused-bench", name = "Paused Bench", parentId = "barbell-bench-press").copy(
                    primaryMuscle = null,
                    secondaryMuscles = emptyList(),
                    equipment = null,
                    movementPattern = null,
                ),
            ),
        )

        val file = exportOf(seedProgram(exerciseId = "paused-bench"))

        wipe()
        importOf(file)

        val restored = database.exerciseDao().findAllIncludingDeleted().associateBy { it.id }
        // The chain travelled, so the category arrives and both movements are filed under it (N110).
        assertEquals("cat-bench", restored.getValue("barbell-bench-press").parentId)
        assertEquals("cat-bench", restored.getValue("paused-bench").parentId)
        // And what the movement read from the row above it is written onto the row, so nothing the document
        // carried is lost with the link.
        assertEquals(MuscleGroup.QUADS, restored.getValue("paused-bench").primaryMuscle)
        assertEquals(Equipment.BARBELL, restored.getValue("paused-bench").equipment)
        assertEquals(MovementPattern.SQUAT, restored.getValue("paused-bench").movementPattern)
    }
}
