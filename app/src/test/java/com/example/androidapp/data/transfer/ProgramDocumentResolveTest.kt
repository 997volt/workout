package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * A program document read as the library the device now holds (ROADMAP N238, B318).
 *
 * The retired map is the only thing that stops a file exported before a merge from minting the twin the merge
 * deleted, and until now no test read through it: the feature schema 16 exists for had no assertion of its own.
 */
class ProgramDocumentResolveTest {

    @Test
    fun aRetiredId_isReadAsItsSurvivor_andItsDefinitionIsDropped() {
        val document = ProgramDocument(
            formatVersion = 1,
            exportedAt = 1L,
            program = ProgramDto(id = "p1", name = "Pull", createdAt = 1L, updatedAt = 1L),
            slots = listOf(
                ProgramSlotDto(
                    id = "sl1",
                    programId = "p1",
                    templateId = "t1",
                    position = 0,
                    weekday = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
            templates = listOf(TemplateDto(id = "t1", name = "Pull A", createdAt = 1L, updatedAt = 1L)),
            templateExercises = listOf(
                planned("pe1", "deadlift"),
                planned("pe2", "conventional-deadlift"),
            ),
            templateSets = listOf(set("s1", "pe1")),
            exercises = listOf(
                movement("deadlift", "Deadlift"),
                movement("conventional-deadlift", "Conventional Deadlift"),
            ),
        )

        val resolved = document.withResolvedExercises(mapOf("deadlift" to "conventional-deadlift"))

        assertThat(resolved.templateExercises.map { it.exerciseId })
            .containsExactly("conventional-deadlift", "conventional-deadlift")
        assertThat(resolved.exercises.map { it.id }).containsExactly("conventional-deadlift")
        assertWithMessage("the set still follows the planned exercise it belonged to")
            .that(resolved.templateSets.map { it.templateExerciseId })
            .containsExactly("pe1")
    }

    private fun planned(id: String, exerciseId: String) = TemplateExerciseDto(
        id = id,
        templateId = "t1",
        exerciseId = exerciseId,
        position = 0,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun set(id: String, plannedId: String) = TemplateSetDto(
        id = id,
        templateExerciseId = plannedId,
        setIndex = 0,
        role = SetType.NORMAL,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun movement(id: String, name: String) = ExerciseDto(
        id = id,
        name = name,
        primaryMuscle = null,
        secondaryMuscles = emptyList(),
        equipment = null,
        movementPattern = null,
        createdAt = 1L,
        updatedAt = 1L,
        parentId = null,
        rowKind = RowKind.MOVEMENT,
    )
}
