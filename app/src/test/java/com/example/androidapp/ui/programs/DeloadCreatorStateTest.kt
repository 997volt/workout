package com.example.androidapp.ui.programs

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek

/**
 * The creator's two pure readings of one snapshot (ROADMAP N147).
 *
 * [buildDeloadDays] is what the screen shows and [buildDeloadInputs] is what the write is — the same
 * source, the same rule, and the same selection, so the preview and the saved plan cannot disagree.
 */
class DeloadCreatorStateTest {

    private fun set(index: Int, weightGrams: Long, reps: Int) = TemplateSet(
        id = "ts$index",
        templateExerciseId = "te",
        setIndex = index,
        role = SetType.NORMAL,
        targetWeightGrams = weightGrams,
        targetAssistanceGrams = null,
        targetRepsMin = reps,
        targetRepsMax = reps,
        targetRepsCurrent = null,
        note = null,
    )

    private fun exercise(
        id: String,
        exerciseId: String,
        name: String,
        sets: List<TemplateSet>,
    ) = DeloadSourceExercise(
        templateExerciseId = id,
        exerciseId = exerciseId,
        name = name,
        restSeconds = 180,
        techniqueNote = "Brace",
        supersetGroup = null,
        sets = sets,
        weightUnit = WeightUnit.KILOGRAMS,
        stepGrams = 2_500L,
        previous = PreviousPerformance(emptyList()),
    )

    private val sources = listOf(
        DeloadSourceDay(
            sourceSlotId = "s1",
            name = "Heavy lower",
            weekday = DayOfWeek.MONDAY,
            exercises = listOf(
                exercise("te1", "back-squat", "Back squat", List(3) { set(it, 100_000L, 5) }),
                exercise("te2", "rdl", "Romanian deadlift", List(3) { set(it, 80_000L, 8) }),
            ),
        ),
        DeloadSourceDay(
            sourceSlotId = "s2",
            name = "Push",
            weekday = DayOfWeek.WEDNESDAY,
            exercises = listOf(
                exercise("te3", "bench", "Bench press", List(4) { set(it, 60_000L, 10) }),
            ),
        ),
    )

    private val weekdayOf: (DeloadSourceDay) -> DayOfWeek? = { it.weekday }

    @Test
    fun theDayCount_showsOnlyThatManyDays() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1", "te2", "te3"),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(days.map { it.sourceSlotId }).containsExactly("s1")
    }

    @Test
    fun theScreen_showsTheRuleAndTheSelection() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1"),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        val squat = days.single().exercises.single { it.templateExerciseId == "te1" }
        assertThat(squat.sets).hasSize(3)
        assertThat(squat.sets.first().weightGrams).isEqualTo(60_000L)
        assertThat(squat.selected).isTrue()
        val rdl = days.single().exercises.single { it.templateExerciseId == "te2" }
        assertThat(rdl.selected).isFalse()
    }

    @Test
    fun theShare_movesEveryShownLoad() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1"),
            loadKeptPercent = 70,
            weekdayOf = weekdayOf,
        )

        val squat = days.single().exercises.single { it.templateExerciseId == "te1" }
        assertThat(squat.sets.first().weightGrams).isEqualTo(70_000L)
    }

    @Test
    fun onlyTheTickedExercises_areWritten() {
        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1"),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(inputs).hasSize(1)
        val day = inputs.single()
        assertThat(day.name).isEqualTo("Heavy lower — deload")
        assertThat(day.weekday).isEqualTo(DayOfWeek.MONDAY)
        assertThat(day.exercises.map { it.exerciseId }).containsExactly("back-squat")
        val written = day.exercises.single()
        assertThat(written.sets).hasSize(3)
        assertThat(written.sets.first().weightGrams).isEqualTo(60_000L)
        assertThat(written.restSeconds).isEqualTo(180)
        assertThat(written.techniqueNote).isEqualTo("Brace")
    }

    @Test
    fun aDayWithNothingTicked_isNotWritten() {
        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = 2,
            selected = setOf("te3"),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(inputs.map { it.name }).containsExactly("Push — deload")
    }

    @Test
    fun aMovedOrRemovedWeekday_travelsWithTheDay() {
        val moved = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1"),
            loadKeptPercent = 60,
            weekdayOf = { DayOfWeek.FRIDAY },
        )
        val removed = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selected = setOf("te1"),
            loadKeptPercent = 60,
            weekdayOf = { null },
        )

        assertThat(moved.single().weekday).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(removed.single().weekday).isNull()
    }

    @Test
    fun theVisibleSelection_isTheCountTheScreenReports() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 2,
            selected = setOf("te1", "te3"),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(days.sumOf { day -> day.exercises.count { it.selected } }).isEqualTo(2)
    }
}
