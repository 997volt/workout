package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.shared.ChangeKind
import com.example.androidapp.shared.EditMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    /**
     * A clock the new cases can move, because a change *after* an export is what staleness means and
     * one fixed instant cannot express it (B183). The older cases never move it, so they read the
     * fixed instant they always did.
     */
    private var now = Instant.parse("2026-10-07T12:00:00Z")
    private val clock = TimeSource { now }

    private fun aMomentLater() {
        now = now.plusSeconds(1)
    }

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
    @Test
    fun aSequenceDocument_isImportedAsASequenceWithItsStart() = runTest {
        // ROADMAP N174: `kind` and `startDate` are honored when a program is *created*, because a
        // block arrives by document — and ignored by an edit, where the phone's editor owns them.
        // A document that arrived as weekly would put five Monday cards on every Monday.
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val block = original.copy(
            program = original.program.copy(
                id = "pgm_block",
                name = "Peak",
                kind = ProgramKind.SEQUENCE,
                startDate = 20_305L,
            ),
            slots = original.slots.map { it.copy(id = it.id + "_block", programId = "pgm_block") },
            templates = original.templates.map { it.copy(id = it.id + "_block") },
            templateExercises = original.templateExercises.map {
                it.copy(id = it.id + "_block", templateId = it.templateId + "_block")
            },
            templateSets = original.templateSets.map {
                it.copy(id = it.id + "_block", templateExerciseId = it.templateExerciseId + "_block")
            },
        )

        importOf(ProgramDocumentCodec.encode(block))

        val stored = database.programDao().findProgram("pgm_block")
        assertEquals(ProgramKind.SEQUENCE, stored?.kind)
        assertEquals(20_305L, stored?.startDate)
    }

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

    private suspend fun importOf(file: String): ProgramImportSummary {
        val result = programs.importProgramDocument(file)
        if (result is DataResult.Failure) error("import refused: ${result.error}")
        return (result as DataResult.Success).data
    }

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

    @Test
    fun anEditDocument_changesTheProgramItNames() = runTest {
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val template = original.templates.single()
        val planned = original.templateSets.single()
        val edit = original.copy(
            program = original.program.copy(name = "Heavy lower B"),
            templates = listOf(template.copy(name = "Squat day B")),
            templateSets = original.templateSets + planned.copy(
                id = "ts-second",
                setIndex = 2,
                role = SetType.NORMAL,
                targetWeightGrams = 80_000L,
                targetRepsMax = 5,
            ),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )

        val summary = importOf(ProgramDocumentCodec.encode(edit))

        assertEquals(1, summary.programs)
        assertEquals("the name was written", "Heavy lower B", programs.observePrograms().first().single().name)
        val stored = templates.observeTemplates().first().single()
        assertEquals("the workout was renamed", "Squat day B", stored.name)
        assertEquals(
            "the second set was added",
            2,
            templates.observeExercises(stored.id).first().single().sets.size,
        )
    }

    @Test
    fun anEditCannotCarryHiding_soAWorkoutStaysHidden() = runTest {
        val programId = seedProgram()
        val templateId = templates.observeTemplates().first().single().id
        templates.setTemplateHidden(templateId, true)

        val file = exportOf(programId)
        // Hiding is the library's view, not the program's definition (N214): the document says nothing
        // about it, and a document that did would rewrite the receiving device's own choices.
        assertFalse("the document does not carry the flag", file.contains("hiddenAt"))

        val original = ProgramDocumentCodec.decode(file)
        val edit = original.copy(
            templates = listOf(original.templates.single().copy(name = "Squat day B")),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )

        importOf(ProgramDocumentCodec.encode(edit))

        val after = database.templateDao().findById(templateId)
        assertEquals("the edit was applied", "Squat day B", after?.name)
        assertNotNull(
            "and the workout the lifter had hidden is still hidden",
            after?.hiddenAt,
        )
    }

    @Test
    fun anEditOfAProgramThatMovedSinceTheExport_isRefused() = runTest {
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            program = original.program.copy(name = "Changed"),
            edit = EditMarker(baseExportedAt = original.exportedAt - 1),
        )

        val failure = programs.importProgramDocument(ProgramDocumentCodec.encode(edit)) as DataResult.Failure

        val invalid = failure.error as DataError.Invalid
        assertTrue(invalid.message.contains("changed on the phone"))
        assertEquals(
            "nothing was written",
            "Heavy lower",
            programs.observePrograms().first().single().name,
        )
    }

    @Test
    fun anEditOfAProgramWhoseSetWasDeletedSinceTheExport_isRefused() = runTest {
        // B183: the deleted row is absent from the snapshot, so the document cannot show the deletion.
        // The device's own read can, and the write refuses — rather than planning the set as an
        // addition that insert-or-ignore would silently skip while reporting success.
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            program = original.program.copy(name = "Renamed after the deletion"),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )
        aMomentLater()
        val stored = templates.observeTemplates().first().single()
        val planned = templates.observeExercises(stored.id).first().single()
        (templates.removeSet(planned.sets.single().id) as DataResult.Success)

        val failure = programs.importProgramDocument(ProgramDocumentCodec.encode(edit)) as DataResult.Failure

        assertTrue((failure.error as DataError.Invalid).message.contains("changed on the phone"))
        assertEquals(
            "the rename was not written either",
            "Heavy lower",
            programs.observePrograms().first().single().name,
        )
    }

    @Test
    fun anEditOfAProgramWhoseDayWasDeletedSinceTheExport_isRefusedRatherThanCrashing() = runTest {
        // The day is soft-deleted, so the older edit plans it as an addition and the insert would hit
        // the row still holding that id — a constraint failure that rolled the whole edit back. It is
        // refused before anything is written, with the sentence the lifter can act on (B183).
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            program = original.program.copy(name = "Renamed after the deletion"),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )
        aMomentLater()
        val slot = programs.observeSlots(programId).first().single()
        (programs.removeSlot(slot.id) as DataResult.Success)

        val failure = programs.importProgramDocument(ProgramDocumentCodec.encode(edit)) as DataResult.Failure

        assertTrue((failure.error as DataError.Invalid).message.contains("changed on the phone"))
        assertTrue("the day stays removed", programs.observeSlots(programId).first().isEmpty())
    }

    @Test
    fun anEditOfAProgramThisDeviceDoesNotHold_isRefusedAsMissing() = runTest {
        // B189: the snapshot threw NotFoundException first, so the app answered "Couldn't read the
        // file" and the planner's PROGRAM_MISSING branch was unreachable.
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            program = original.program.copy(name = "Changed"),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )
        (programs.deleteProgram(programId) as DataResult.Success)

        val failure = programs.importProgramDocument(ProgramDocumentCodec.encode(edit)) as DataResult.Failure

        assertTrue((failure.error as DataError.Invalid).message.contains("not on this device"))
    }

    @Test
    fun turningAProgramIntoABlock_authorsTodaysStart() = runTest {
        // B196: the toggle is where a block's beginning is chosen. Without it the start falls back to
        // the program's creation, and a program trained for months would arrive already under way.
        val programId = seedProgram()
        assertEquals(
            "a weekly program has no authored start",
            null,
            programs.observePrograms().first().single().startDate,
        )

        (programs.setProgramKind(programId, ProgramKind.SEQUENCE) as DataResult.Success)

        val expected = Instant.parse("2026-10-07T12:00:00Z")
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        assertEquals(expected, programs.observePrograms().first().single().startDate)
    }

    @Test
    fun aBlockIsNotAskedAboutAsAMissedWeek() = runTest {
        // B195: the clock is Wednesday and the seeded slot is Monday, so the weekly program has a
        // miss to ask about. A block's weekday places one session, so it has no weekly occurrence.
        val programId = seedProgram()
        (programs.activateProgram(programId) as DataResult.Success)
        val today = LocalDate.of(2026, 10, 7)
        val zone = ZoneId.of("UTC")
        assertFalse(
            "the weekly program is asked",
            (programs.pendingOccurrences(today, zone) as DataResult.Success).data.isEmpty(),
        )

        (programs.setProgramKind(programId, ProgramKind.SEQUENCE) as DataResult.Success)

        assertTrue(
            "a block is not a missed week",
            (programs.pendingOccurrences(today, zone) as DataResult.Success).data.isEmpty(),
        )
    }

    @Test
    fun aBlockSkip_settlesItsSessionOnTheDayItWasSkipped() = runTest {
        // B195: a block has no week, so the skip is dated the day it was taken and the schedule
        // reads that day as the session's event.
        val programId = seedProgram()
        (programs.setProgramKind(programId, ProgramKind.SEQUENCE) as DataResult.Success)
        val slot = programs.observeSlots(programId).first().single()
        assertEquals(0, programs.observeSequenceSchedule(programId).first()?.settled ?: 0)

        (programs.skipBlockSession(slot.id, LocalDate.of(2026, 10, 9)) as DataResult.Success)

        val schedule = programs.observeSequenceSchedule(programId).first()
        assertEquals("the skip settled the session", 1, schedule?.settled)
        assertTrue("and the block has nothing left", schedule?.isComplete == true)
    }

    @Test
    fun anEditThatRemovesADay_keepsItsWorkout() = runTest {
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            slots = emptyList(),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )

        importOf(ProgramDocumentCodec.encode(edit))

        assertTrue("the day left the schedule", programs.observeSlots(programId).first().isEmpty())
        assertEquals(
            "the workout stayed in the library",
            1,
            templates.observeTemplates().first().size,
        )
    }

    @Test
    fun anEditIsPreviewedWithoutWritingAnything() = runTest {
        val programId = seedProgram()
        val original = ProgramDocumentCodec.decode(exportOf(programId))
        val edit = original.copy(
            program = original.program.copy(name = "Changed"),
            edit = EditMarker(baseExportedAt = original.exportedAt),
        )

        val previewResult = programs.previewProgramDocument(ProgramDocumentCodec.encode(edit))
        if (previewResult is DataResult.Failure) error("preview refused: ${previewResult.error}")
        val preview = (previewResult as DataResult.Success).data
        val editPreview = preview as ProgramDocumentPreview.Edit

        assertTrue(editPreview.changes.any { it.kind == ChangeKind.PROGRAM_RENAMED })
        assertEquals(
            "the preview wrote nothing",
            "Heavy lower",
            programs.observePrograms().first().single().name,
        )
    }
}
