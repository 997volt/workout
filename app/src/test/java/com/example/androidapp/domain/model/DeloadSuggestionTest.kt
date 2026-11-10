package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The recovery-week cadence (ROADMAP N301).
 *
 * It is a count of whole weeks from the last recovery week, or from the program's start when there has
 * not been one; it is a suggestion, so the only thing it can do is make the screen say a week may be due.
 */
class DeloadSuggestionTest {

    private val thisWeek = LocalDate.of(2026, 11, 9)

    @Test
    fun sixWeeksFromTheStart_reachesASixWeekCadence() {
        val suggestion = deloadSuggestion(
            cadenceWeeks = 6,
            lastDeloadWeek = null,
            programStartedWeek = thisWeek.minusWeeks(6),
            thisWeek = thisWeek,
        )

        assertThat(suggestion.weeksSince).isEqualTo(6)
        assertThat(suggestion.sinceLastDeload).isFalse()
        assertThat(suggestion.due).isTrue()
    }

    @Test
    fun aRecentRecoveryWeek_resetsTheCount() {
        val suggestion = deloadSuggestion(
            cadenceWeeks = 6,
            lastDeloadWeek = thisWeek.minusWeeks(3),
            programStartedWeek = thisWeek.minusWeeks(20),
            thisWeek = thisWeek,
        )

        assertThat(suggestion.weeksSince).isEqualTo(3)
        assertThat(suggestion.sinceLastDeload).isTrue()
        assertThat(suggestion.due).isFalse()
    }

    @Test
    fun off_neverSuggests() {
        val suggestion = deloadSuggestion(
            cadenceWeeks = 0,
            lastDeloadWeek = null,
            programStartedWeek = thisWeek.minusWeeks(40),
            thisWeek = thisWeek,
        )

        assertThat(suggestion.due).isFalse()
    }

    @Test
    fun aStartAfterThisWeek_clampsToZeroWeeks() {
        val suggestion = deloadSuggestion(
            cadenceWeeks = 4,
            lastDeloadWeek = null,
            programStartedWeek = thisWeek.plusWeeks(2),
            thisWeek = thisWeek,
        )

        assertThat(suggestion.weeksSince).isEqualTo(0)
        assertThat(suggestion.due).isFalse()
    }
}
