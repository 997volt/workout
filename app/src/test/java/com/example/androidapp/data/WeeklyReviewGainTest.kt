package com.example.androidapp.data

import com.example.androidapp.data.local.WeeklyMovementRow
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which week rows count as movement (ROADMAP N251).
 *
 * The rule is deliberately narrow: a first training, or a strictly heavier set than the best before the week.
 * A repeat is not progress, and a hold's zero weight cannot claim a number it does not have.
 */
class WeeklyReviewGainTest {

    @Test
    fun aFirstTraining_isAgain_evenAtBodyweight() {
        val gain = row(weekTop = 0, beforeTop = 0, priorSets = 0).toGainOrNull()

        assertThat(gain?.firstTime).isTrue()
        assertThat(gain?.previousGrams).isNull()
        assertThat(gain?.topGrams).isEqualTo(0)
    }

    @Test
    fun aHeavierTopSet_isAGain_withTheOldNumber() {
        val gain = row(weekTop = 105_000, beforeTop = 100_000, priorSets = 3).toGainOrNull()

        assertThat(gain?.firstTime).isFalse()
        assertThat(gain?.topGrams).isEqualTo(105_000)
        assertThat(gain?.previousGrams).isEqualTo(100_000)
    }

    @Test
    fun aRepeat_isNotAGain() {
        assertThat(row(weekTop = 100_000, beforeTop = 100_000, priorSets = 3).toGainOrNull()).isNull()
    }

    @Test
    fun aLighterWeek_isNotAGain() {
        assertThat(row(weekTop = 95_000, beforeTop = 100_000, priorSets = 3).toGainOrNull()).isNull()
    }

    @Test
    fun aFirstWeightedWeek_carriesNoPreviousNumber() {
        // B332: the movement was trained before, but only at bodyweight — there is no weight to report as
        // "was", and reporting zero would be a number nobody lifted.
        val gain = row(weekTop = 20_000, beforeTop = 0, priorSets = 3).toGainOrNull()

        assertThat(gain?.firstTime).isFalse()
        assertThat(gain?.previousGrams).isNull()
        assertThat(gain?.topGrams).isEqualTo(20_000)
    }

    private fun row(weekTop: Long, beforeTop: Long, priorSets: Int) = WeeklyMovementRow(
        exerciseId = "back-squat",
        name = "Back Squat",
        weekTop = weekTop,
        beforeTop = beforeTop,
        weekSets = 1,
        priorSets = priorSets,
    )
}
