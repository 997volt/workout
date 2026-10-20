package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.time.DayOfWeek

/**
 * The app accepts the helper's program sheet (N167).
 *
 * The fixture is the document the tool writes for a two-day sheet: three slots, two templates, five
 * planned exercises, twenty-two sets, one of them a range, one exercise created because the sheet named
 * something the library does not hold. [ProgramDocumentCodec] is the gate the file must pass, and the
 * references are what the import's filters drop silently if they are wrong — so both are pinned here.
 */
class ProgramDocumentFixtureTest {

    @Test
    fun theHelpersProgramSheetIsADocumentTheAppAccepts() {
        val document = ProgramDocumentCodec.decode(
            File(repoRoot, "docs/fixtures/program-document.json").readText(),
        )

        assertThat(document.formatVersion).isEqualTo(4)
        assertThat(document.program.name).isEqualTo("Autumn block")
        assertThat(document.program.isActive).isFalse()

        assertThat(document.templates.map { it.name }).containsExactly("Upper A", "Lower A").inOrder()
        assertThat(document.slots.map { it.weekday })
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).inOrder()
        assertThat(document.slots.map { it.templateId }.toSet())
            .containsExactlyElementsIn(document.templates.map { it.id }.toSet())

        assertThat(document.templateExercises).hasSize(5)
        assertThat(document.templateSets).hasSize(22)
        assertThat(document.templateSets.map { it.role }.toSet()).containsExactly(SetType.NORMAL)

        val templateExerciseIds = document.templateExercises.map { it.id }.toSet()
        assertThat(document.templateSets.map { it.templateExerciseId }.toSet())
            .containsExactlyElementsIn(templateExerciseIds)
        assertThat(document.templateExercises.map { it.exerciseId }.toSet())
            .containsExactlyElementsIn(document.exercises.map { it.id }.toSet())

        // The bench sets: five of them, 82.5 kg for three reps, RPE 8 and three minutes' rest.
        val bench = document.templateExercises.first()
        assertThat(bench.targetRpeHalves).isEqualTo(16)
        assertThat(bench.restSeconds).isEqualTo(180)
        val benchSets = document.templateSets.filter { it.templateExerciseId == bench.id }
        assertThat(benchSets).hasSize(5)
        benchSets.forEach { set ->
            assertThat(set.targetWeightGrams).isEqualTo(82_500L)
            assertThat(set.targetRepsMin).isEqualTo(3)
            assertThat(set.targetRepsMax).isEqualTo(3)
        }

        // The row: four sets, a 6–8 range, and no climb recorded yet.
        val row = document.templateExercises.first { exercise ->
            document.exercises.first { it.id == exercise.exerciseId }.name == "Barbell Row"
        }
        val rowSets = document.templateSets.filter { it.templateExerciseId == row.id }
        assertThat(rowSets).hasSize(4)
        rowSets.forEach { set ->
            assertThat(set.targetRepsMin).isEqualTo(6)
            assertThat(set.targetRepsMax).isEqualTo(8)
            assertThat(set.targetRepsCurrent).isNull()
        }

        val created = document.exercises.first { it.id.startsWith("ex_") }
        assertThat(created.name).isEqualTo("Machine Curl Call")
        assertThat(created.primaryMuscle).isNull()
        assertThat(created.equipment).isNull()
        assertThat(created.deletedAt).isNull()
    }
}
