package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The number each set takes (ROADMAP N112).
 *
 * Pure, because the rule is: three lists number sets — the workout's logged rows, the history
 * detail's and the template editor's planned ones — and they have to be numbered by one thing rather
 * than three. A **warm-up takes no number**; a **rung keeps its**, because a run is work the plan
 * asked for even though it is not rated on its own (N79).
 */
class SetNumberTest {

    @Test
    fun aRampLeavesTheWorkingSetsReadingOneTwoThree() {
        val numbers = listOf(
            SetType.WARMUP,
            SetType.WARMUP,
            SetType.NORMAL,
            SetType.WARMUP,
            SetType.NORMAL,
            SetType.NORMAL,
        ).setNumbers { it }

        assertThat(numbers).containsExactly(null, null, 1, null, 2, 3).inOrder()
    }

    @Test
    fun aRungKeepsItsNumber() {
        // N79: a drop is a member of the group above it, but it is work the plan prescribed — so it
        // takes a number where a warm-up does not.
        val numbers = listOf(
            SetType.NORMAL,
            SetType.DROP,
            SetType.NORMAL,
            SetType.DROP,
            SetType.CLUSTER,
            SetType.TOP_SET,
            SetType.FAILURE,
        ).setNumbers { it }

        assertThat(numbers).containsExactly(1, 2, 3, 4, 5, 6, 7).inOrder()
    }

    @Test
    fun aPlanOfNothingButWarmUps_numbersNothing() {
        val numbers = listOf(SetType.WARMUP, SetType.WARMUP).setNumbers { it }

        assertThat(numbers).containsExactly(null, null).inOrder()
    }

    @Test
    fun noSets_numberNothing() {
        assertThat(emptyList<SetType>().setNumbers { it }).isEmpty()
    }
}
