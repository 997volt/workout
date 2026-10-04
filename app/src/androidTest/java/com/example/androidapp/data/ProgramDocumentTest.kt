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

    /** A program with one template, one planned set and one slot, ready to export. */
    private suspend fun seedProgram(): String {
        val programId = (programs.createProgram("Heavy lower") as DataResult.Success).data
        val templateId = (templates.createTemplate("Squat day") as DataResult.Success).data
        templates.addExercise(templateId, "back-squat")
        val planned = templates.observeExercises(templateId).first().single().id
        templates.addSet(
            planned,
            TemplateSetEdit(role = SetType.WARMUP, targetWeightGrams = 60_000L, targetRepsMax = 5),
        )
        templates.setExercisePlan(planned, restSeconds = 0, techniqueNote = null)
        programs.addSlot(programId, templateId, DayOfWeek.MONDAY)
        return programId
    }

    private suspend fun exportOf(programId: String): String =
        (programs.exportProgramDocument(programId) as DataResult.Success).data

    private suspend fun importOf(file: String): ProgramImportSummary =
        (programs.importProgramDocument(file) as DataResult.Success).data

    /** Back to a bare database, which is what "another device" means for this feature. */
    private suspend fun wipe() = withContext(Dispatchers.IO) { database.clearAllTables() }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        isCustom = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )
}
