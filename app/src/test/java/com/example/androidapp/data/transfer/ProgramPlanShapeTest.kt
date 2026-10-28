package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import org.junit.Test

/**
 * Whether a document is the work the device holds under its id (B267).
 *
 * The question exists because an id that is already here has two causes — the same document loaded twice, or a
 * different document the seed gave the same id — and the import card used to call both "already here". Both halves
 * matter: movements alone miss a day a second program has, days alone miss a changed movement inside one. Plain
 * JUnit: it compares two values, which is the whole decision.
 */
class ProgramPlanShapeTest {

    private fun day(templateId: String, weekday: DayOfWeek? = null, position: Int = 0) =
        PlannedDay(templateId = templateId, weekday = weekday, position = position)

    private fun movement(templateId: String, exerciseId: String, position: Int = 0) =
        PlannedMovement(templateId = templateId, exerciseId = exerciseId, position = position)

    private fun document(
        days: List<PlannedDay> = emptyList(),
        movements: List<PlannedMovement> = emptyList(),
    ) = ProgramDocument(
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
        slots = days.mapIndexed { index, slot ->
            ProgramSlotDto(
                id = "slot_$index",
                programId = "pgm_1",
                templateId = slot.templateId,
                position = slot.position,
                weekday = slot.weekday,
                createdAt = 1,
                updatedAt = 1,
            )
        },
        templateExercises = movements.map { planned ->
            TemplateExerciseDto(
                id = "te_${planned.templateId}_${planned.exerciseId}_${planned.position}",
                templateId = planned.templateId,
                exerciseId = planned.exerciseId,
                position = planned.position,
                createdAt = 1,
                updatedAt = 1,
            )
        },
    )

    private fun held(
        days: List<PlannedDay> = emptyList(),
        movements: List<PlannedMovement> = emptyList(),
    ) = PlannedWork(days = days.counted(), movements = movements.counted())

    @Test
    fun theSameDaysAndMovements_areTheSameWork() {
        val days = listOf(day("tpl_push", DayOfWeek.MONDAY, 0), day("tpl_pull", DayOfWeek.WEDNESDAY, 1))
        val movements = listOf(
            movement("tpl_push", "bench-press", 0),
            movement("tpl_push", "overhead-press", 1),
            movement("tpl_pull", "deadlift", 0),
        )

        assertThat(document(days, movements).plansTheSameWorkAs(held(days, movements))).isTrue()
    }

    @Test
    fun aDifferentMovementInTheSameDay_isNotTheSameWork() {
        val days = listOf(day("tpl_push", DayOfWeek.MONDAY))
        val stored = held(days, listOf(movement("tpl_push", "incline-press")))

        assertThat(document(days, listOf(movement("tpl_push", "bench-press"))).plansTheSameWorkAs(stored))
            .isFalse()
    }

    @Test
    fun aMovementInAnotherOrder_isNotTheSameWork() {
        val stored = held(
            listOf(day("tpl_push", DayOfWeek.MONDAY)),
            listOf(movement("tpl_push", "overhead-press", 0), movement("tpl_push", "bench-press", 1)),
        )
        val document = document(
            listOf(day("tpl_push", DayOfWeek.MONDAY)),
            listOf(movement("tpl_push", "bench-press", 0), movement("tpl_push", "overhead-press", 1)),
        )

        assertThat(document.plansTheSameWorkAs(stored)).isFalse()
    }

    @Test
    fun aDayTheDeviceDoesNotHave_isNotTheSameWork() {
        // The shape that got a first cut of this wrong: a second program can add a day and no movement at all,
        // so a comparison that read only movements called it the same plan.
        val stored = held(listOf(day("tpl_push", DayOfWeek.MONDAY)), listOf(movement("tpl_push", "bench-press")))
        val document = document(
            listOf(day("tpl_push", DayOfWeek.MONDAY), day("tpl_second", DayOfWeek.FRIDAY, 1)),
            listOf(movement("tpl_push", "bench-press")),
        )

        assertThat(document.plansTheSameWorkAs(stored)).isFalse()
    }

    @Test
    fun theSameDayOnAnotherWeekday_isNotTheSameWork() {
        val stored = held(listOf(day("tpl_push", DayOfWeek.MONDAY)), emptyList())

        assertThat(document(listOf(day("tpl_push", DayOfWeek.TUESDAY)), emptyList()).plansTheSameWorkAs(stored))
            .isFalse()
    }

    @Test
    fun anEmptyProgram_isNotTheSameWorkAsANamedDay() {
        assertThat(document().plansTheSameWorkAs(held(listOf(day("tpl_push"))))).isFalse()
        assertThat(document().plansTheSameWorkAs(held())).isTrue()
    }

    @Test
    fun aRepeatedRow_countsAsADifference() {
        // A malformed document can name the same movement twice; one row is not two.
        val stored = held(emptyList(), listOf(movement("tpl_push", "bench-press")))
        val document = document(
            emptyList(),
            listOf(movement("tpl_push", "bench-press"), movement("tpl_push", "bench-press")),
        )

        assertThat(document.plansTheSameWorkAs(stored)).isFalse()
    }

    @Test
    fun aDifferentLoadOnTheSameWork_isNotReportedAsAnotherProgram() {
        // Stated boundary: the comparison is the days and their movements, not what they load. A weight the
        // import cannot merge over a live program must not turn a re-import of the same plan into a warning.
        val days = listOf(day("tpl_push", DayOfWeek.MONDAY))
        val movements = listOf(movement("tpl_push", "bench-press"))
        val document = document(days, movements).copy(
            templateSets = listOf(
                TemplateSetDto(
                    id = "set_a",
                    templateExerciseId = "te_tpl_push_bench-press_0",
                    setIndex = 0,
                    role = SetType.NORMAL,
                    targetWeightGrams = 120_000,
                    targetRepsMin = 3,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            ),
        )

        assertThat(document.plansTheSameWorkAs(held(days, movements))).isTrue()
    }
}
