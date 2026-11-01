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
 * The screen used to pop back with no evidence; this dialog is the evidence, and it counts what the write
 * touched rather than what the picker guessed.
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
}
