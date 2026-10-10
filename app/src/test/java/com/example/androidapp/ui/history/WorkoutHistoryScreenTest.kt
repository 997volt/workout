package com.example.androidapp.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The history list (ROADMAP N48).
 *
 * The repeat action is the part worth pinning down: it is the home screen's repeat button given a
 * new address — the row the user is looking at — so a test has to show that it names *that*
 * workout, that it does not also open it, and that a workout with nothing left to copy offers no
 * action at all.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var opened: String? = null
    private var repeated: String? = null

    private fun show(state: WorkoutHistoryUiState) {
        composeTestRule.setContent {
            AndroidAppTheme {
                WorkoutHistoryScreen(
                    state = state,
                    onOpenWorkout = { opened = it },
                    onRepeatWorkout = { repeated = it },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun aRepeatableWorkout_offersRepeat_addressedToThatRow() {
        show(stateWith(summary("session-1", repeatable = 2)))

        composeTestRule.onNodeWithTag(TestTags.historyRepeat("session-1"))
            .assertIsDisplayed()
            .performClick()

        assertEquals("the action names the row's workout", "session-1", repeated)
        assertNull("and it must not also open it", opened)
    }

    @Test
    fun aWorkoutWithNothingToCopy_offersNoRepeat() {
        // ROADMAP B43's tail: a workout whose exercises have all left the library is history, not a
        // template, so the control is absent rather than present and inert.
        show(stateWith(summary("session-1", repeatable = 0)))

        composeTestRule.onNodeWithTag(TestTags.historyRepeat("session-1")).assertDoesNotExist()
    }

    @Test
    fun aRow_namesTheWorkoutAndItsSets_andStopsThere() {
        // N100: the date, the name and the number of sets, and nothing else. The duration and the volume
        // answer a different question and belong to the workout detail, which is what opening the row goes
        // to — so the whole supporting line is asserted, and either of them reappearing here fails rather
        // than passing quietly.
        show(stateWith(summary("session-1", repeatable = 1).copy(templateName = "Lower A")))

        composeTestRule.onNodeWithText("Lower A · 12 sets").assertIsDisplayed()
    }

    @Test
    fun aPastWorkoutsDate_isRenderedInItsOwnZone() {
        // ROADMAP B33 and B39: grouping was well covered while the rendering was not, and the rendering is
        // where the bug was. The expected string is produced by the same formatter, so this cannot drift
        // with the machine's locale. It moved here from the home screen's tests when the recent list went
        // (N102) and this row became the only one drawing a past workout's date.
        val tokyo = summary("session-1", repeatable = 1).copy(
            startedAt = Instant.parse("2026-09-30T20:00:00Z"),
            zoneOffsetMinutes = 540,
        )
        show(stateWith(tokyo))

        val expected = HistoryFormat.historyHeadline(tokyo.startedAt, zone = ZoneOffset.ofHours(9))

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    private fun stateWith(workout: WorkoutSummary) = WorkoutHistoryUiState(
        isLoading = false,
        groups = listOf(HistoryGroup(month = YearMonth.of(2026, 9), workouts = listOf(workout))),
    )

    private fun summary(id: String, repeatable: Int) = WorkoutSummary(
        id = id,
        startedAt = Instant.parse("2026-09-29T08:00:00Z"),
        finishedAt = Instant.parse("2026-09-29T09:00:00Z"),
        exerciseCount = 3,
        setCount = 12,
        volumeGrams = 1_000_000L,
        repeatableExerciseCount = repeatable,
    )
}
