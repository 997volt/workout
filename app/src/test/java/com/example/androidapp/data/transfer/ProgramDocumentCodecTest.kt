package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.shared.ProblemCode
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * The program document's format (ROADMAP N47).
 *
 * Pure, like `BackupCodecTest`: the version gate and the round trip are properties of the format,
 * not of the database, so they are covered without a device.
 */
class ProgramDocumentCodecTest {

    @Test
    fun aDocument_roundTripsThroughTheFormat() {
        val document = document()

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(document))

        assertThat(decoded).isEqualTo(document)
    }

    @Test
    fun aDocument_keepsWhatAMovementIs() {
        // B247: the shared model had no field for either flag, so a program shared with a document
        // recreated a pull-up as an ordinary movement and a plank as a counted lift. The document brings
        // an exercise whole (N47); these two are part of what it is.
        val document = document().copy(
            exercises = listOf(exercise().copy(isBodyweight = true, isTimed = true)),
        )

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(document))

        val movement = decoded.exercises.single()
        assertTrue("a pull-up stays loaded by the body", movement.isBodyweight)
        assertTrue("and a hold stays a hold", movement.isTimed)
    }

    @Test
    fun aProgramDocument_dropsTheAutoProgressionSwitch() {
        // N302 (N299): the switch is device state that travels in the backup, not part of a program's
        // definition — importing a program must not start writing on the recipient's device.
        val withAuto = document().copy(program = document().program.copy(autoProgression = true))

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(withAuto))

        assertThat(decoded.program.autoProgression).isFalse()
    }

    @Test
    fun aPlannedFailure_survivesTheFormat() {
        // B276: the shared role enum listed five of the app's six, so exporting a program whose plan
        // held a failure set threw and was reported as a generic storage failure, while a document
        // naming one was refused as corrupt. It is a role a plan can prescribe.
        val document = document().copy(
            templateSets = listOf(templateSet().copy(role = SetType.FAILURE)),
        )

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(document))

        assertThat(decoded.templateSets.single().role).isEqualTo(SetType.FAILURE)
    }

    @Test
    fun theVersionIsItsOwn_notTheBackupSchemaVersion() {
        // A document written by this build names the document format, not the database schema: the
        // two change for different reasons, and reusing one number for both would make a new backup
        // column look like a new document.
        assertThat(document().formatVersion).isEqualTo(ProgramDocumentCodec.CURRENT_FORMAT_VERSION)
    }

    @Test
    fun aDocumentFromANewerApp_isRefusedWithItsOwnVersion() {
        val fromTheFuture = Json {
            encodeDefaults = true
            prettyPrint = true
        }.encodeToString(
            document().copy(formatVersion = ProgramDocumentCodec.CURRENT_FORMAT_VERSION + 1),
        )

        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode(fromTheFuture)
        }

        assertThat(thrown.message).contains("newer version of the app")
    }

    @Test
    fun aNewerDocumentCarryingAValueThisBuildCannotRead_isRefusedAsNewer() {
        // ROADMAP B62, the program's half: `SetType.CLUSTER` and the split muscle names travel in a
        // document, so the version has to be read before the body or a newer file reads as corrupt.
        val fromTheFuture = ProgramDocumentCodec.encode(document())
            .replace(Regex("\"formatVersion\"\\s*:\\s*\\d+"), "\"formatVersion\": 99")
            .replace(Regex("\"role\"\\s*:\\s*\"[A-Z_]+\""), "\"role\": \"A_ROLE_FROM_THE_FUTURE\"")
        assertThat(fromTheFuture).contains("A_ROLE_FROM_THE_FUTURE")

        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode(fromTheFuture)
        }

        assertThat(thrown.message).contains("newer version of the app")
    }

    @Test
    fun aKnownVersionsDocumentCarryingAnUnreadableValue_isRefusedAsCorrupt() {
        val corrupt = ProgramDocumentCodec.encode(document())
            .replace(Regex("\"role\"\\s*:\\s*\"[A-Z_]+\""), "\"role\": \"A_ROLE_FROM_THE_FUTURE\"")

        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode(corrupt)
        }

        assertThat(thrown.message).isEqualTo("That does not look like a program file.")
    }

    @Test
    fun somethingElseEntirely_isRefusedAsNotAProgramFile() {
        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode("{\"schemaVersion\":1,\"exportedAt\":0}")
        }

        assertThat(thrown.message).isEqualTo("That does not look like a program file.")
    }

    @Test
    fun aPhasedSlot_roundTripsWithItsAuthoredWeek() {
        // ROADMAP N292: the week is part of the day, so a document that carried the day without it
        // would arrive as one repeating week — the wrong schedule the format version exists to refuse.
        val phased = document().copy(
            slots = listOf(document().slots.single().copy(phaseWeek = 2)),
        )

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(phased))

        assertThat(decoded.slots.single().phaseWeek).isEqualTo(2)
    }

    @Test
    fun aDocumentFromBeforePhaseWeeks_readsAsTheSingleWeekItDescribed() {
        // The field is defaulted, so a v7 document is the old program rather than a refusal: every
        // slot is unphased, which means the week repeats (N292).
        val fromBefore = ProgramDocumentCodec.encode(document())
            .replace(Regex("\"formatVersion\"\\s*:\\s*\\d+"), "\"formatVersion\": 7")
            .replace(Regex("\"phaseWeek\"\\s*:\\s*null,?\\s*"), "")

        val decoded = ProgramDocumentCodec.decode(fromBefore)

        assertThat(decoded.slots.single().phaseWeek).isNull()
        assertThat(ProgramDocumentCodec.problems(decoded)).isEmpty()
    }

    @Test
    fun aProgramThatMixesAuthoredWeeksAndASingleWeek_isRefused() {
        // An unphased day inside a phased program is never read, so accepting it would drop a day in
        // silence; the validator refuses the shape instead (N292).
        val slot = document().slots.single()
        val mixed = document().copy(
            slots = listOf(slot.copy(phaseWeek = 1), slot.copy(id = "slot-2", position = 1)),
        )

        assertThat(ProgramDocumentCodec.problems(mixed).map { it.code })
            .contains(ProblemCode.INVALID_PHASE)
    }

    @Test
    fun theSamePositionInTwoAuthoredWeeks_isNotADuplicate() {
        // A position is a day's place *within its week*: week 1 and week 2 both start at zero.
        val slot = document().slots.single()
        val phased = document().copy(
            slots = listOf(
                slot.copy(id = "slot-1", phaseWeek = 1),
                slot.copy(id = "slot-2", phaseWeek = 2),
            ),
        )

        assertThat(ProgramDocumentCodec.problems(phased)).isEmpty()
    }

    @Test
    fun twoDaysOfOneWeekSharingAPosition_areStillRefused() {
        val slot = document().slots.single()
        val duplicate = document().copy(
            slots = listOf(
                slot.copy(id = "slot-1", phaseWeek = 1),
                slot.copy(id = "slot-2", phaseWeek = 1),
            ),
        )

        assertThat(ProgramDocumentCodec.problems(duplicate).map { it.code })
            .contains(ProblemCode.INVALID_POSITION)
    }

    @Test
    fun aWeekBelowOne_isRefused() {
        val slot = document().slots.single()

        val zero = document().copy(slots = listOf(slot.copy(phaseWeek = 0)))

        assertThat(ProgramDocumentCodec.problems(zero).map { it.code })
            .contains(ProblemCode.INVALID_PHASE)
    }

    /** A small but complete document: one program, one template, one planned movement. */
    private fun document() = ProgramDocument(
        formatVersion = ProgramDocumentCodec.CURRENT_FORMAT_VERSION,
        exportedAt = 1_000L,
        program = ProgramDto(
            id = "program-1",
            name = "Heavy lower",
            isActive = true,
            position = 2,
            createdAt = 10L,
            updatedAt = 20L,
        ),
        slots = listOf(
            ProgramSlotDto(
                id = "slot-1",
                programId = "program-1",
                templateId = "template-1",
                position = 0,
                weekday = DayOfWeek.MONDAY,
                createdAt = 10L,
                updatedAt = 20L,
            ),
        ),
        templates = listOf(
            TemplateDto(id = "template-1", name = "Squat day", createdAt = 10L, updatedAt = 20L),
        ),
        templateExercises = listOf(templateExercise()),
        templateSets = listOf(templateSet()),
        exercises = listOf(exercise()),
    )

    private fun templateExercise() = TemplateExerciseDto(
        id = "te-1",
        templateId = "template-1",
        exerciseId = "back-squat",
        position = 0,
        restSeconds = 0,
        createdAt = 10L,
        updatedAt = 20L,
    )

    private fun templateSet() = TemplateSetDto(
        id = "ts-1",
        templateExerciseId = "te-1",
        setIndex = 0,
        role = SetType.WARMUP,
        targetWeightGrams = 60_000L,
        targetRepsMax = 5,
        createdAt = 10L,
        updatedAt = 20L,
    )

    private fun exercise() = ExerciseDto(
        id = "back-squat",
        name = "Back Squat",
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
