package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Whether a document is the plan the device holds under its id (B267).
 *
 * The question exists because an id that is already here has two causes — the same document loaded twice, or a
 * different document that the seed happened to give the same id — and the import card used to call both "already
 * here". Plain JUnit: it reads two values and compares them, which is the whole decision.
 */
class ProgramPlanShapeTest {

    private fun plan(vararg rows: Triple<String, String, Int>): ProgramDocument = document(
        rows.map { (templateId, exerciseId, position) ->
            TemplateExerciseDto(
                id = "te_$templateId$exerciseId$position",
                templateId = templateId,
                exerciseId = exerciseId,
                position = position,
                createdAt = 1,
                updatedAt = 1,
            )
        },
    )

    private fun document(templateExercises: List<TemplateExerciseDto>) = ProgramDocument(
        formatVersion = ProgramDocumentCodec.CURRENT_FORMAT_VERSION,
        exportedAt = 1,
        program = ProgramDto(
            id = "pgm_1",
            name = "Push Pull Legs",
            isActive = false,
            position = 0,
            createdAt = 1,
            updatedAt = 1,
        ),
        templateExercises = templateExercises,
    )

    private fun List<Triple<String, String, Int>>.stored(): Map<PlannedMovement, Int> =
        map { PlannedMovement(it.first, it.second, it.third) }.counted()

    @Test
    fun theSameDaysAndMovements_areTheSameWork() {
        val rows = listOf(
            Triple("tpl_push", "bench-press", 0),
            Triple("tpl_push", "overhead-press", 1),
            Triple("tpl_pull", "deadlift", 0),
        )

        assertThat(plan(*rows.toTypedArray()).plansTheSameWorkAs(rows.stored())).isTrue()
    }

    @Test
    fun aDifferentMovementInTheSameDay_isNotTheSameWork() {
        // The case a same-named paste produces: one program says Push/Pull/Legs too, with different lifts.
        val document = plan(
            Triple("tpl_push", "bench-press", 0),
            Triple("tpl_pull", "deadlift", 0),
        )
        val stored = listOf(
            Triple("tpl_push", "incline-press", 0),
            Triple("tpl_pull", "deadlift", 0),
        )

        assertThat(document.plansTheSameWorkAs(stored.stored())).isFalse()
    }

    @Test
    fun aMovementInAnotherOrder_isNotTheSameWork() {
        val document = plan(
            Triple("tpl_push", "bench-press", 0),
            Triple("tpl_push", "overhead-press", 1),
        )
        val stored = listOf(
            Triple("tpl_push", "overhead-press", 0),
            Triple("tpl_push", "bench-press", 1),
        )

        assertThat(document.plansTheSameWorkAs(stored.stored())).isFalse()
    }

    @Test
    fun aDayTheDeviceDoesNotHave_isNotTheSameWork() {
        val document = plan(
            Triple("tpl_push", "bench-press", 0),
            Triple("tpl_legs", "back-squat", 0),
        )

        assertThat(document.plansTheSameWorkAs(listOf(Triple("tpl_push", "bench-press", 0)).stored())).isFalse()
        assertThat(document.plansTheSameWorkAs(emptyMap())).isFalse()
    }

    @Test
    fun aRepeatedRow_countsAsADifference() {
        // A malformed document can name the same movement twice; one row is not two.
        val document = plan(
            Triple("tpl_push", "bench-press", 0),
            Triple("tpl_push", "bench-press", 0),
        )

        assertThat(document.plansTheSameWorkAs(listOf(Triple("tpl_push", "bench-press", 0)).stored())).isFalse()
    }

    @Test
    fun aDifferentLoadOnTheSameWork_isNotReportedAsAnotherProgram() {
        // Stated boundary: the comparison is the days and their movements, not what they load. A weight the
        // import cannot merge over a live program must not turn a re-import of the same plan into a warning.
        val stored = listOf(Triple("tpl_push", "bench-press", 0)).stored()
        val document = document(
            listOf(
                TemplateExerciseDto(
                    id = "te_a",
                    templateId = "tpl_push",
                    exerciseId = "bench-press",
                    position = 0,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            ),
        ).copy(
            templateSets = listOf(
                TemplateSetDto(
                    id = "set_a",
                    templateExerciseId = "te_a",
                    setIndex = 0,
                    role = SetType.NORMAL,
                    targetWeightGrams = 120_000,
                    targetRepsMin = 3,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            ),
        )

        assertThat(document.plansTheSameWorkAs(stored)).isTrue()
    }
}
