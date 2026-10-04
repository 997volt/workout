package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

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
    fun theVersionIsItsOwn_notTheBackupSchemaVersion() {
        // A document written by this build names the document format, not the database schema: the
        // two change for different reasons, and reusing one number for both would make a new backup
        // column look like a new document.
        assertThat(document().formatVersion).isEqualTo(ProgramDocumentCodec.CURRENT_FORMAT_VERSION)
    }

    @Test
    fun aDocumentFromANewerApp_isRefusedWithItsOwnVersion() {
        val fromTheFuture = Json { encodeDefaults = true; prettyPrint = true }.encodeToString(
            document().copy(formatVersion = ProgramDocumentCodec.CURRENT_FORMAT_VERSION + 1),
        )

        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode(fromTheFuture)
        }

        assertThat(thrown.message).contains("newer version of the app")
    }

    @Test
    fun somethingElseEntirely_isRefusedAsNotAProgramFile() {
        val thrown = assertThrows(InvalidInputException::class.java) {
            ProgramDocumentCodec.decode("{\"schemaVersion\":1,\"exportedAt\":0}")
        }

        assertThat(thrown.message).isEqualTo("That does not look like a program file.")
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
        slotExercises = listOf(slotExercise()),
        slotSets = listOf(slotSet()),
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

    private fun slotExercise() = ProgramSlotExerciseDto(
        id = "se-1",
        slotId = "slot-1",
        exerciseId = "back-squat",
        restSeconds = 0,
        createdAt = 10L,
        updatedAt = 20L,
    )

    private fun slotSet() = ProgramSlotSetDto(
        id = "ss-1",
        slotExerciseId = "se-1",
        setIndex = 0,
        role = SetType.TOP_SET,
        targetPercentOf1Rm = 85,
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
        isCustom = false,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
