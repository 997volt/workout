package com.example.androidapp.ui.history

import com.example.androidapp.ui.programs.StartIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Both answers the missed-day question can hand the history screen (ROADMAP B271).
 *
 * The host handled only the repeat, so *Do it now* — which returns the missed day's template instead —
 * closed the question and started nothing. The branch is a pure function so this can be asserted
 * without a device.
 */
class WorkoutHistoryStartTest {

    @Test
    fun repeatIntent_repeatsTheWorkoutItNames() {
        val start = historyStartFor(StartIntent(repeatSessionId = "session-1"))

        assertThat(start).isEqualTo(HistoryStart.Repeat("session-1"))
    }

    @Test
    fun doItNowIntent_startsTheMissedDaysTemplate() {
        // The shape ProgramStartGateViewModel.onDoItNow builds: a template, its slot, no repeat.
        val start = historyStartFor(
            StartIntent(templateId = "template-1", label = "Heavy lower", slotId = "slot-1"),
        )

        assertThat(start).isEqualTo(HistoryStart.Template("template-1", "slot-1"))
    }

    @Test
    fun anIntentWithNeitherId_doesNothingRatherThanGuessing() {
        assertThat(historyStartFor(StartIntent())).isEqualTo(HistoryStart.None)
    }
}
