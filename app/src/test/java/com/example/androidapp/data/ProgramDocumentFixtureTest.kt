package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramDocument
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
 * planned exercises, twenty-five sets, one of them a range, one exercise created because the sheet named
 * something the library does not hold. [ProgramDocumentCodec] is the gate the file must pass, and the
 * references are what the import's filters drop silently if they are wrong — so both are pinned here.
 * `docs/fixtures/generate.js` writes the fixture and the page's test rebuilds it from the sheet, so this
 * side pins the app's reading rather than the tool's writing. One of the five is two adjacent Back Squat
 * rows the helper writes as one exercise with both prescriptions (N264), so the merged shape is pinned here
 * too.
 */
class ProgramDocumentFixtureTest {

    /**
     * The committed fixture, out of the one-line script assignment it is wrapped in.
     *
     * The file is JavaScript rather than JSON because the page's own test loads it (`window.PROGRAM_SHEET_TEXT`
     * beside `window.PROGRAM_DOCUMENT_FIXTURE`) and rebuilds the document from the sheet — one generated
     * artefact, two readers, so neither side can drift alone (N169). The JSON contains no semicolon, which is
     * what makes the extraction unambiguous.
     */
    private fun fixtureJson(): String =
        File(repoRoot, "docs/fixtures/program-document.js").readText()
            .substringAfter("window.PROGRAM_DOCUMENT_FIXTURE = ")
            .substringBefore(";")
            .trim()

    @Test
    fun theHelpersProgramSheetIsADocumentTheAppAccepts() {
        val document = ProgramDocumentCodec.decode(fixtureJson())

        assertThat(document.formatVersion).isEqualTo(ProgramDocumentCodec.CURRENT_FORMAT_VERSION)
        assertThat(document.program.name).isEqualTo("Autumn block")
        assertThat(document.program.isActive).isFalse()

        assertThat(document.templates.map { it.name }).containsExactly("Upper A", "Lower A").inOrder()
        assertThat(document.slots.map { it.weekday })
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).inOrder()
        assertThat(document.slots.map { it.templateId }.toSet())
            .containsExactlyElementsIn(document.templates.map { it.id }.toSet())

        assertThat(document.templateExercises).hasSize(5)
        assertThat(document.templateSets).hasSize(25)
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

        // Two adjacent Back Squat rows are one exercise with both prescriptions (N264).
        assertMergedSquatIsOneExercise(document)

        val created = document.exercises.first { it.id.startsWith("ex_") }
        assertThat(created.name).isEqualTo("Machine Curl Call")
        assertThat(created.primaryMuscle).isNull()
        assertThat(created.equipment).isNull()
        assertThat(created.deletedAt).isNull()

        // **A plan's indices are places, and the app stores them contiguous from zero** (B257). The
        // workout reads the plan's next row by how many sets are logged, so a document numbered from one
        // gave the first set no target at all and every later set the row above it. Pinned here because
        // this file is the app's reading of what the helper writes — the target values above were pinned
        // and the indices were not, which is how both sides stayed green while disagreeing.
        document.templateExercises.forEach { exercise ->
            val indexes = document.templateSets
                .filter { it.templateExerciseId == exercise.id }
                .map { it.setIndex }
                .sorted()
            assertThat(indexes).isEqualTo(List(indexes.size) { it })
        }
    }

    /**
     * The sheet says Back Squat in two adjacent rows, five sets then three (N264).
     *
     * Same rest, RPE and note, so the helper writes **one** planned exercise with both prescriptions, in
     * the sheet's row order: five sets of five at 100 kg, then three of eight at 80 kg. Two entries here
     * would be the app accepting the split the helper is meant to have removed.
     */
    private fun assertMergedSquatIsOneExercise(document: ProgramDocument) {
        val squats = document.templateExercises.filter { exercise ->
            document.exercises.first { it.id == exercise.exerciseId }.name == "Back Squat"
        }
        assertThat(squats).hasSize(1)
        val sets = document.templateSets
            .filter { it.templateExerciseId == squats.single().id }
            .sortedBy { it.setIndex }
        assertThat(sets.map { it.targetRepsMin }).isEqualTo(List(5) { 5 } + List(3) { 8 })
        assertThat(sets.map { it.targetWeightGrams })
            .isEqualTo(List(5) { 100_000L } + List(3) { 80_000L })
    }
}
