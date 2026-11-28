package com.example.androidapp.ui.navigation

import com.example.androidapp.ui.workout.PlanHandOff
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The navigation decision a plan hand-off makes (N310, N377). */
class PlanHandOffNavTest {

    private fun handOff(returnsForNext: Boolean) = PlanHandOff(
        programId = "p1",
        templateId = "t1",
        templateExerciseId = "te1",
        replace = false,
        returnsForNext = returnsForNext,
    )

    @Test
    fun aPainHandOff_keepsTheWorkoutBehind() {
        assertThat(replacesWorkout(handOff(returnsForNext = true))).isFalse()
    }

    @Test
    fun aStallHandOff_replacesTheWorkout() {
        assertThat(replacesWorkout(handOff(returnsForNext = false))).isTrue()
    }
}
