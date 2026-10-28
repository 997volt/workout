package com.example.androidapp.ui.workout

import com.example.androidapp.domain.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What the next-set fields hold when the history read lands (ROADMAP B272).
 *
 * The draft is keyed on the row and its set count, and neither changes when the asynchronous
 * previous-performance read returns — so the fields kept their fallback while the reason line beside them
 * named the history. An untouched draft is the suggestion's now; one the lifter has edited stays theirs.
 */
class NextSetReseedTest {

    private val history = SetSuggestion(reps = 5, weightGrams = 100_000)

    @Test
    fun anUntouchedDraft_takesTheHistoryThatArrives() {
        val fallback = SetSuggestion(reps = 8, weightGrams = 20_000)
            .initialDraft(WeightUnit.KILOGRAMS, timed = false)

        val reseeded = reseeded(
            edited = false,
            current = fallback,
            suggestion = history,
            unit = WeightUnit.KILOGRAMS,
            timed = false,
        )

        assertThat(reseeded).isEqualTo(history.initialDraft(WeightUnit.KILOGRAMS, timed = false))
        assertThat(reseeded.weightText).isEqualTo("100")
    }

    @Test
    fun anEditedDraft_isTheLifters_andIsLeftAlone() {
        val typed = history.initialDraft(WeightUnit.KILOGRAMS, timed = false).copy(repsText = "3")

        val reseeded = reseeded(
            edited = true,
            current = typed,
            suggestion = SetSuggestion(reps = 12, weightGrams = 140_000),
            unit = WeightUnit.KILOGRAMS,
            timed = false,
        )

        assertThat(reseeded).isEqualTo(typed)
        assertThat(reseeded.repsText).isEqualTo("3")
    }
}
