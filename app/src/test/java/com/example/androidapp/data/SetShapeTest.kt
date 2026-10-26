package com.example.androidapp.data

import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.loadGrams
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a set's reps and duration are allowed to be together (ROADMAP N220, B256).
 *
 * The rule is one decision at the write boundary, and the cases that matter are its refusals: a hold of zero
 * seconds, one longer than a day, and a row that states both answers. The counted case clamps, which is the
 * older rule and stays — a zero-rep set is not a set.
 */
class SetShapeTest {

    @Test
    fun aCountedSet_keepsItsReps_andCarriesNoDuration() {
        assertThat(heldShape(reps = 8, durationSeconds = null)).isEqualTo(8 to null)
    }

    @Test
    fun aSetOfZeroReps_isClampedToTheOneRepItAlreadyWas() {
        assertThat(heldShape(reps = 0, durationSeconds = null)).isEqualTo(1 to null)
    }

    @Test
    fun aHold_statesItsSeconds_andNoReps() {
        assertThat(heldShape(reps = 0, durationSeconds = 45)).isEqualTo(0 to 45)
    }

    @Test
    fun aHoldOfZeroSeconds_isRefused() {
        val refused = runCatching { heldShape(reps = 0, durationSeconds = 0) }
        assertThat(refused.exceptionOrNull()).isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    fun aHoldLongerThanADay_isRefused() {
        val refused = runCatching { heldShape(reps = 0, durationSeconds = MAX_SET_SECONDS + 1) }
        assertThat(refused.exceptionOrNull()).isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    fun aRowThatStatesBoth_isRefused() {
        // The one shape the write boundary exists for: two answers to "how many".
        val refused = runCatching { heldShape(reps = 5, durationSeconds = 45) }
        assertThat(refused.exceptionOrNull()).isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    fun aBodyweightSet_carriesTheLoadTheHistoryAndTheTrendCount() {
        // The rule the review, the detail screen and the SQL share (N219): a bodyweight set's load is the
        // lifter plus what was added, less the machine, floored at zero.
        val set = SetEntry(
            id = "s1",
            sessionExerciseId = "se1",
            setIndex = 0,
            reps = 8,
            weightGrams = 20_000L,
            bodyweightGrams = 75_000L,
            assistanceGrams = 0L,
        )

        assertThat(loadGrams(set.weightGrams, set.bodyweightGrams, set.assistanceGrams)).isEqualTo(95_000L)
    }
}
