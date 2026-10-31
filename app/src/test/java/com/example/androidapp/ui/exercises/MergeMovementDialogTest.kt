package com.example.androidapp.ui.exercises

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The question a manual merge asks (ROADMAP N245).
 *
 * The confirm stays off until a movement is chosen, search narrows a library-sized list, and a failed merge
 * says why inside the dialog instead of closing it.
 */
@RunWith(AndroidJUnit4::class)
class MergeMovementDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var confirmed: String? = null
    private var dismissed = 0

    private fun show(error: String? = null) {
        composeTestRule.setContent {
            MergeMovementDialog(
                sourceName = "Sled Push",
                candidates = listOf(squat, bench),
                error = error,
                onConfirm = { confirmed = it },
                onDismiss = { dismissed++ },
            )
        }
    }

    @Test
    fun aMovementIsChosen_andConfirmed() {
        show()

        composeTestRule.onNodeWithTag(TestTags.MERGE_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.MERGE_CONFIRM).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(TestTags.mergeCandidate("barbell-bench-press")).performClick()
        composeTestRule.onNodeWithTag(TestTags.MERGE_CONFIRM).performClick()

        assertEquals("barbell-bench-press", confirmed)
    }

    @Test
    fun searchNarrowsTheList() {
        show()

        composeTestRule.onNodeWithTag(TestTags.MERGE_SEARCH).performTextInput("bench")

        composeTestRule.onNodeWithTag(TestTags.mergeCandidate("barbell-bench-press")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.mergeCandidate("back-squat")).assertDoesNotExist()
    }

    @Test
    fun aFailedMerge_saysWhyInTheDialog_andCancelDismisses() {
        show(error = "Only movements can be merged.")

        composeTestRule.onNodeWithText("Only movements can be merged.").assertExists()
        composeTestRule.onNodeWithTag(TestTags.MERGE_CANCEL).performClick()

        assertEquals(1, dismissed)
    }

    private val squat = Exercise(
        id = "back-squat",
        name = "Back Squat",
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
    )

    private val bench = Exercise(
        id = "barbell-bench-press",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.PRESS,
    )
}
