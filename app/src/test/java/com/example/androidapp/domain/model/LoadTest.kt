package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What "the load" means on a set (ROADMAP N219).
 *
 * The rule is one line of arithmetic, and this is where it is stated: bodyweight is added, assistance is
 * subtracted, and an ordinary set is its added weight exactly as it always was. Volume, the 1RM estimate and
 * every figure built on them read this one function, so a test here is a test of all of them agreeing.
 */
class LoadTest {

    @Test
    fun anOrdinarySet_isItsAddedWeight() {
        // Assistance on an ordinary set is a fact about the machine, not a deduction from the load: the
        // machine's stack was pulling at what the lifter typed, which is why N15 wrote it that way.
        assertThat(loadGrams(weightGrams = 100_000L, bodyweightGrams = null, assistanceGrams = 0L))
            .isEqualTo(100_000L)
        assertThat(loadGrams(weightGrams = 100_000L, bodyweightGrams = null, assistanceGrams = 30_000L))
            .isEqualTo(100_000L)
    }

    @Test
    fun aBodyweightSet_isTheLifterPlusWhatWasAdded() {
        // A 75 kg lifter with a 20 kg belt is lifting 95 kg, which is what the volume and the 1RM have to say.
        assertThat(loadGrams(weightGrams = 20_000L, bodyweightGrams = 75_000L, assistanceGrams = 0L))
            .isEqualTo(95_000L)
    }

    @Test
    fun aBodyweightSet_isNothingAdded_justTheLifter() {
        assertThat(loadGrams(weightGrams = 0L, bodyweightGrams = 75_000L, assistanceGrams = 0L))
            .isEqualTo(75_000L)
    }

    @Test
    fun assistance_comesOffTheBodyweight() {
        // An assisted pull-up taking 30 kg off a 75 kg lifter is 45 kg of work, not 75 and not -30.
        assertThat(loadGrams(weightGrams = 0L, bodyweightGrams = 75_000L, assistanceGrams = 30_000L))
            .isEqualTo(45_000L)
    }

    @Test
    fun moreAssistanceThanBodyweight_isStillASet_andNeverNegative() {
        // A band that takes more than the lifter weighs is a set somebody did. A negative load would quietly
        // subtract from every total built on it, so the floor is part of the rule rather than a guard here.
        assertThat(loadGrams(weightGrams = 0L, bodyweightGrams = 20_000L, assistanceGrams = 50_000L))
            .isEqualTo(0L)
    }

    @Test
    fun aSet_carriesItsOwnLoad() {
        val set = SetEntry(
            id = "s1",
            sessionExerciseId = "se1",
            setIndex = 0,
            reps = 8,
            weightGrams = 20_000L,
            bodyweightGrams = 75_000L,
            assistanceGrams = 0L,
        )

        assertThat(set.totalLoadGrams).isEqualTo(95_000L)
    }
}
