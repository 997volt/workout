package com.example.androidapp.ui.weekly

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.DayOccurrence
import com.example.androidapp.domain.model.OccurrenceState
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgressionMiss
import com.example.androidapp.domain.model.ReviewDay
import com.example.androidapp.domain.model.WeekPlan
import com.example.androidapp.domain.model.WeekTotals
import com.example.androidapp.ui.components.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * The week in review's screen (ROADMAP N247).
 *
 * It has to say the plan against the week, the week against the last one, each scheduled day's outcome and the
 * plan that stalled — and say "nothing scheduled" rather than zero when no program is followed.
 */
@RunWith(AndroidJUnit4::class)
class WeeklyReviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val monday = LocalDate.of(2026, 10, 26)

    private fun show(state: WeeklyReviewUiState) {
        composeTestRule.setContent {
            WeeklyReviewScreen(
                state = state,
                onBack = {},
                onRetry = {},
                onOpenDeload = {},
            )
        }
    }

    @Test
    fun theWeek_isShown_withItsPlan_itsTotals_andItsStall() {
        show(
            WeeklyReviewUiState(
                isLoading = false,
                weekStart = monday,
                weekEnd = monday.plusDays(6),
                thisWeek = WeekTotals(sessions = 3, sets = 30, volumeGrams = 30_000),
                lastWeek = WeekTotals(sessions = 2, sets = 20, volumeGrams = 20_000),
                days = listOf(
                    ReviewDay(monday, listOf(occurrence(OccurrenceState.DONE))),
                    ReviewDay(monday.plusDays(1), listOf(occurrence(OccurrenceState.MISSED))),
                ),
                plan = WeekPlan(planned = 4, done = 3),
                stall = PlanStall(
                    exerciseName = "Back Squat",
                    setNumber = 2,
                    kind = ProgressionMiss.NOT_DONE,
                    sessions = 3,
                ),
                stallProgramId = "p1",
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.WEEKLY_REVIEW_SCREEN).assertExists()
        composeTestRule.onNodeWithTag(TestTags.WEEKLY_REVIEW_SUMMARY).assertExists()
        composeTestRule.onNodeWithText("3 of 4 scheduled sessions done").assertExists()
        composeTestRule.onNodeWithText("Squat day · Done", substring = true).assertExists()
        composeTestRule.onNodeWithText("Squat day · Missed", substring = true).assertExists()
        composeTestRule.onNodeWithTag(TestTags.WEEKLY_REVIEW_STALL).performScrollTo().assertExists()
    }

    @Test
    fun noProgramFollowed_saysSo_andNoStallIsStated() {
        show(
            WeeklyReviewUiState(
                isLoading = false,
                weekStart = monday,
                weekEnd = monday.plusDays(6),
            ),
        )

        composeTestRule.onNodeWithText(
            "No program is being followed, so there is nothing scheduled to compare with.",
        ).assertExists()
        composeTestRule.onNodeWithText("No plan has stopped moving.").performScrollTo().assertExists()
    }

    private fun occurrence(state: OccurrenceState) = DayOccurrence(
        slotId = "slot",
        templateId = "template",
        templateName = "Squat day",
        date = monday,
        weekStart = monday,
        state = state,
        canCorrect = false,
    )
}
