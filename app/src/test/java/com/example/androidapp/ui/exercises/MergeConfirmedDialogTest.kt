package com.example.androidapp.ui.exercises

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.repository.MergeSummary
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a completed merge says (ROADMAP N249).
 *
 * The screen used to pop back with no evidence; this dialog is the evidence. Its counts are the picker's reads
 * (B333), and a zero is said rather than printed (N262).
 */
@RunWith(AndroidJUnit4::class)
class MergeConfirmedDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var dismissed = 0

    private fun show(summary: MergeSummary) {
        composeTestRule.setContent {
            MergeConfirmedDialog(summary = summary, onDismiss = { dismissed++ })
        }
    }

    @Test
    fun itSaysWhatMoved_andDoneLeaves() {
        show(MergeSummary(survivorName = "Back Squat", sessions = 12, plans = 3))

        composeTestRule.onNodeWithTag(TestTags.MERGE_CONFIRMED).assertExists()
        composeTestRule.onNodeWithText("Merged into Back Squat").assertExists()
        composeTestRule.onNodeWithText("Moved 12 sessions and 3 plans.").assertExists()

        composeTestRule.onNodeWithTag(TestTags.MERGE_CONFIRMED_OK).performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun aSingleSessionAndPlan_readAsOne() {
        show(MergeSummary(survivorName = "Sled Drag", sessions = 1, plans = 1))

        composeTestRule.onNodeWithText("Moved 1 session and 1 plan.").assertExists()
    }

    @Test
    fun noFinishedHistory_saysSo_ratherThanReportingZeros() {
        // N262: every session the source held was deleted or still open, so the picker's count is zero.
        // "Moved 0 sessions and 0 plans" read as nothing happened, and it could not tell that case from a
        // movement that had no history at all.
        show(MergeSummary(survivorName = "Back Squat", sessions = 0, plans = 0))

        composeTestRule.onNodeWithText("Merged into Back Squat").assertExists()
        composeTestRule.onNodeWithText("No finished session or plan moved.").assertExists()
        composeTestRule.onNodeWithText("0 sessions", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("0 plans", substring = true).assertDoesNotExist()
    }

    @Test
    fun plansOnly_dropsTheZeroAndNamesTheMissingSessions() {
        show(MergeSummary(survivorName = "Sled Drag", sessions = 0, plans = 3))

        composeTestRule.onNodeWithText("Moved 3 plans; no finished session moved.").assertExists()
    }

    @Test
    fun sessionsOnly_dropsTheZeroPlans() {
        show(MergeSummary(survivorName = "Sled Drag", sessions = 12, plans = 0))

        composeTestRule.onNodeWithText("Moved 12 sessions.").assertExists()
    }
}
