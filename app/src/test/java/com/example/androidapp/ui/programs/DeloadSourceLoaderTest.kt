package com.example.androidapp.ui.programs

import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.testing.FakeDeloadRepository
import com.example.androidapp.ui.testing.FakeProgramRepository
import com.example.androidapp.ui.testing.FakeSettingsRepository
import com.example.androidapp.ui.testing.FakeTemplateRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What a deload draft reads as "last time" (ROADMAP B397).
 *
 * The source has to be the **slot's** own normal session. The exercise-wide read includes the last
 * deload, which is already light: a second draft built from it would halve the halving. This is the
 * loader edge, not the arithmetic, so it is asserted here with the fakes.
 */
class DeloadSourceLoaderTest {

    private val week = LocalDate.of(2026, 10, 5)

    @Test
    fun theDraftReadsTheSlotsHistory_notTheMovementsOwn() = runTest {
        val program = FakeProgramRepository()
        val templates = FakeTemplateRepository()
        val deloads = FakeDeloadRepository()
        val settings = FakeSettingsRepository()
        program.program.value = WorkoutProgram(id = "p1", name = "Upper/Lower", isActive = true)
        program.slots.value = listOf(
            ProgramSlot(
                id = "slot1",
                programId = "p1",
                templateId = "t1",
                position = 0,
                templateName = "Heavy lower",
                weekday = DayOfWeek.MONDAY,
            ),
        )
        templates.exercises.value = mapOf(
            "t1" to listOf(
                TemplateExercise(
                    id = "te1",
                    templateId = "t1",
                    exerciseId = "back-squat",
                    position = 0,
                    exerciseName = "Back squat",
                    primaryMuscle = null,
                    equipment = null,
                    targetRpeHalves = 16,
                    sets = listOf(
                        TemplateSet(
                            id = "ts1",
                            templateExerciseId = "te1",
                            setIndex = 0,
                            role = SetType.NORMAL,
                            targetWeightGrams = 100_000L,
                            targetRepsMin = 5,
                            targetRepsMax = 5,
                        ),
                    ),
                ),
            ),
        )
        val slotHistory = PreviousPerformance(
            listOf(
                SetEntry(
                    id = "entry1",
                    sessionExerciseId = "se1",
                    setIndex = 0,
                    reps = 5,
                    weightGrams = 100_000L,
                    assistanceGrams = 0L,
                    setType = SetType.NORMAL,
                ),
            ),
        )
        program.slotPrevious = slotHistory

        val snapshot = RoomDeloadSourceLoader(program, templates, deloads, settings).load("p1", week)

        assertThat(snapshot).isNotNull()
        assertThat(snapshot!!.days.single().exercises.single().previous).isEqualTo(slotHistory)
        assertThat(program.slotPreviousCalls).containsExactly("slot1" to "back-squat")
    }
}
