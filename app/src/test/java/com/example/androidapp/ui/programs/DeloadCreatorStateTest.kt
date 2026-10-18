package com.example.androidapp.ui.programs

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek

/**
 * The creator's two pure readings of one snapshot (ROADMAP N147, N152).
 *
 * [buildDeloadDays] is what the screen shows and [buildDeloadInputs] is what the write is — the same source,
 * the same rule and the same per-day selection, so the preview and the saved plan cannot disagree. The pool is
 * program-wide: a day may keep a lift the program only schedules on a day the deload dropped.
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
        DeloadSourceDay(
            sourceSlotId = "s3",
            name = "Pull",
            weekday = DayOfWeek.FRIDAY,
            exercises = listOf(
                exercise("te4", "row", "Barbell row", List(3) { set(it, 70_000L, 8) }),
            ),
        ),
        DeloadSourceDay(
            sourceSlotId = "s4",
            name = "Lower B",
            weekday = DayOfWeek.SATURDAY,
            exercises = listOf(
                exercise("te5", "front-squat", "Front squat", List(3) { set(it, 70_000L, 6) }),
            ),
        ),
    )

    private val weekdayOf: (DeloadSourceDay) -> DayOfWeek? = { it.weekday }

    @Test
    fun theDayCount_showsOnlyThatManyDayCards() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat", "rdl", "bench", "row", "front-squat")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(days.map { it.sourceSlotId }).containsExactly("s1")
    }

    @Test
    fun everyDayCard_offersTheWholeProgramPool() {
        // N152: the day 1 card is not limited to the source day it starts from.
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        val byExercise = days.single().exercises.associateBy { it.exerciseId }
        assertThat(byExercise.keys).containsExactly("back-squat", "rdl", "bench", "row", "front-squat")
        val squat = byExercise.getValue("back-squat")
        assertThat(squat.sets).hasSize(3)
        assertThat(squat.sets.first().weightGrams).isEqualTo(60_000L)
        assertThat(squat.selected).isTrue()
        assertThat(byExercise.getValue("rdl").selected).isFalse()
        assertThat(byExercise.getValue("row").sourceName).isEqualTo("Pull")
    }

    @Test
    fun theShare_movesEveryShownLoad() {
        val days = buildDeloadDays(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat")),
            loadKeptPercent = 70,
            weekdayOf = weekdayOf,
        )

        val squat = days.single().exercises.single { it.exerciseId == "back-squat" }
        assertThat(squat.sets.first().weightGrams).isEqualTo(70_000L)
    }

    @Test
    fun onlyTheExercisesTickedOnThatDay_areWritten() {
        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(inputs).hasSize(1)
        val day = inputs.single()
        assertThat(day.name).isEqualTo("Deload A")
        assertThat(day.weekday).isEqualTo(DayOfWeek.MONDAY)
        assertThat(day.exercises.map { it.exerciseId }).containsExactly("back-squat")
        val written = day.exercises.single()
        assertThat(written.sets).hasSize(3)
        assertThat(written.sets.first().weightGrams).isEqualTo(60_000L)
        assertThat(written.restSeconds).isEqualTo(180)
        assertThat(written.techniqueNote).isEqualTo("Brace")
    }

    @Test
    fun aTwoDayDeload_canKeepLiftsFromTheSourceDaysItDropped() {
        // The request: four program days, two deload days, a lift from the third and one from the fourth.
        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = 2,
            selectedByDay = listOf(setOf("row"), setOf("front-squat")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(inputs.map { it.name }).containsExactly("Deload A", "Deload B").inOrder()
        assertThat(inputs[0].exercises.map { it.exerciseId }).containsExactly("row")
        assertThat(inputs[1].exercises.map { it.exerciseId }).containsExactly("front-squat")
    }

    @Test
    fun aDayWithNothingTicked_isNotWritten() {
        val inputs = buildDeloadInputs(
            sources = sources,
            dayCount = 2,
            selectedByDay = listOf(emptySet(), setOf("bench")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(inputs.map { it.name }).containsExactly("Deload B")
    }

    @Test
    fun aMovedOrRemovedWeekday_travelsWithTheDay() {
        val moved = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat")),
            loadKeptPercent = 60,
            weekdayOf = { DayOfWeek.FRIDAY },
        )
        val removed = buildDeloadInputs(
            sources = sources,
            dayCount = 1,
            selectedByDay = listOf(setOf("back-squat")),
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
            selectedByDay = listOf(setOf("back-squat"), setOf("bench")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        assertThat(days.sumOf { day -> day.exercises.count { it.selected } }).isEqualTo(2)
    }

    @Test
    fun aMovementOnTwoSourceDays_takesTheHeaviestSourcePlan() {
        // N153: the heaviest stated working load supplies the plan, not simply the first source day.
        val duplicate = listOf(
            DeloadSourceDay(
                sourceSlotId = "a",
                name = "A",
                weekday = DayOfWeek.MONDAY,
                exercises = listOf(exercise("t1", "bench", "Bench press", List(3) { set(it, 60_000L, 8) })),
            ),
            DeloadSourceDay(
                sourceSlotId = "b",
                name = "B",
                weekday = DayOfWeek.WEDNESDAY,
                exercises = listOf(exercise("t2", "bench", "Bench press", List(4) { set(it, 80_000L, 6) })),
            ),
        )

        val days = buildDeloadDays(
            sources = duplicate,
            dayCount = 1,
            selectedByDay = listOf(setOf("bench")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        val bench = days.single().exercises.single()
        assertThat(bench.sourceName).isEqualTo("A, B")
        // Four planned sets, capped at the rule's three; the load is derived from B's 80 kg, not A's 60.
        assertThat(bench.sets).hasSize(3)
        assertThat(bench.sets.first().weightGrams).isEqualTo(47_500L)
    }

    @Test
    fun aMovementOnTwoSourceDays_isOnePoolRow_withBothSources() {
        val duplicate = listOf(
            DeloadSourceDay(
                sourceSlotId = "a",
                name = "A",
                weekday = DayOfWeek.MONDAY,
                exercises = listOf(exercise("t1", "bench", "Bench press", List(3) { set(it, 60_000L, 8) })),
            ),
            DeloadSourceDay(
                sourceSlotId = "b",
                name = "B",
                weekday = DayOfWeek.WEDNESDAY,
                exercises = listOf(exercise("t2", "bench", "Bench press", List(4) { set(it, 55_000L, 10) })),
            ),
        )

        val days = buildDeloadDays(
            sources = duplicate,
            dayCount = 1,
            selectedByDay = listOf(setOf("bench")),
            loadKeptPercent = 60,
            weekdayOf = weekdayOf,
        )

        val bench = days.single().exercises.single()
        assertThat(bench.exerciseId).isEqualTo("bench")
        assertThat(bench.sourceName).isEqualTo("A, B")
        assertThat(bench.sets).hasSize(3)
    }
}
