package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The three examples the empty Programs screen offers (P3.19).
 *
 * Each one is a real program document, inserted through the app's own codec and merge — so the test that
 * matters is the one the importer will run: it decodes, and every reference points at something the
 * document carries. The rules the examples keep are pinned here too: seeded movements only, no loads, an
 * RPE the progression can read, and no day left unscheduled.
 */
class StarterProgramsTest {

    private val directory = File(repoRoot, "app/src/main/assets/starter-programs")
    private val seedIds = SeedExercises.all.map { it.id }.toSet()

    @Test
    fun theExamplesAreThreeValidProgramDocuments() {
        val files = directory.listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            .orEmpty()
        assertThat(files.map { it.name }).containsExactly(
            "full-body-3-days.json",
            "push-pull-legs-3-days.json",
            "upper-lower-4-days.json",
        )

        val names = files.map { file ->
            val document = ProgramDocumentCodec.decode(file.readText())

            assertThat(document.formatVersion).isEqualTo(4)
            assertThat(document.program.name).startsWith("Example: ")
            assertThat(document.program.isActive).isFalse()
            assertThat(document.templates).isNotEmpty()
            assertThat(document.slots).isNotEmpty()

            document.templates.forEach { template ->
                val planned = document.templateExercises.filter { it.templateId == template.id }
                assertThat(planned).isNotEmpty()
                planned.forEach { exercise ->
                    assertThat(exercise.exerciseId).isIn(seedIds)
                    assertThat(exercise.targetRpeHalves).isEqualTo(16)
                    val sets = document.templateSets.filter { it.templateExerciseId == exercise.id }
                    assertThat(sets).isNotEmpty()
                    sets.forEach { set ->
                        assertThat(set.role).isEqualTo(SetType.NORMAL)
                        assertThat(set.targetWeightGrams).isNull()
                        assertThat(set.targetRepsMin).isNotNull()
                        assertThat(set.targetRepsMax).isNotNull()
                    }
                }
            }

            document.slots.forEach { slot -> assertThat(slot.weekday).isNotNull() }
            document.slots.forEach { slot ->
                assertThat(document.templates.map { it.id }).contains(slot.templateId)
            }
            document.program.name
        }

        assertThat(names.toSet()).hasSize(3)
    }
}
