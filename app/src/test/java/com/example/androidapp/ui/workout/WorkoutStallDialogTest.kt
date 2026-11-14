package com.example.androidapp.ui.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one thing a finished workout still says (ROADMAP N305).
 *
 * A stall is the exercise's, so the dialog names the lift and how long nothing has moved — and it is
 * information, so *Done* is the only thing it offers.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutStallDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theDialogNamesTheExerciseAndItsRun() {
        show(
            listOf(
                PlanStall(
                    exerciseName = "Back Squat",
                    sessions = 3,
                    templateId = "t1",
                    templateExerciseId = "te1",
                    exerciseId = "squat"
                )
            )
        )

        composeTestRule.onNodeWithTag(TestTags.STALL_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("No progress in 3 sessions running.").assertIsDisplayed()
    }

    @Test
    fun everyStalledLiftIsListed() {
        show(
            listOf(
                PlanStall(
                    exerciseName = "Back Squat",
                    sessions = 3,
                    templateId = "t1",
                    templateExerciseId = "te1",
                    exerciseId = "squat"
                ),
                PlanStall(
                    exerciseName = "Bench Press",
                    sessions = 4,
                    templateId = "t1",
                    templateExerciseId = "te2",
                    exerciseId = "bench"
                ),
            ),
        )

        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeTestRule.onNodeWithText("No progress in 4 sessions running.").assertIsDisplayed()
    }

    @Test
    fun done_dismisses() {
        var done = false
        show(
            listOf(
                PlanStall(
                    exerciseName = "Back Squat",
                    sessions = 3,
                    templateId = "t1",
                    templateExerciseId = "te1",
                    exerciseId = "squat"
                )
            ),
            onDone = { done = true }
        )

        composeTestRule.onNodeWithTag(TestTags.STALL_DONE).performClick()

        assertThat(done).isTrue()
    }

    private fun show(
        stalls: List<PlanStall>,
        onDone: () -> Unit = {},
        onDeload: (PlanStall) -> Unit = {},
        onReplace: (PlanStall) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                WorkoutStallDialog(
                    stalls = stalls,
                    onDone = onDone,
                    onDeload = onDeload,
                    onReplace = onReplace,
                )
            }
        }
    }

    @Test
    fun everyStalledLift_offersBothPlanEdits_andDoneDoesNothing() {
        // N310: the finish is a decision now; both answers hand off to the plan, which owns the edit.
        val stall = PlanStall(
            exerciseName = "Back Squat",
            sessions = 3,
            templateId = "t1",
            templateExerciseId = "te1",
            exerciseId = "squat",
        )
        var deloaded: PlanStall? = null
        var replaced: PlanStall? = null
        var done = false
        show(
            listOf(stall),
            onDone = { done = true },
            onDeload = { deloaded = it },
            onReplace = { replaced = it },
        )

        composeTestRule.onNodeWithTag(TestTags.STALL_DIALOG_DELOAD).performClick()
        composeTestRule.onNodeWithTag(TestTags.STALL_DIALOG_REPLACE).performClick()
        composeTestRule.onNodeWithTag(TestTags.STALL_DONE).performClick()

        assertThat(deloaded).isEqualTo(stall)
        assertThat(replaced).isEqualTo(stall)
        assertThat(done).isTrue()
    }
}
