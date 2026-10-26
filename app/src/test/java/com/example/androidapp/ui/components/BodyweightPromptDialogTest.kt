package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.runtime.CompositionLocalProvider
import com.example.androidapp.domain.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The prompt a bodyweight set raises when no weight is on record (ROADMAP N219).
 *
 * The rule the control has to hold is that **nothing is written at zero**: the save action is disabled until
 * a positive weight is typed, and dismissing writes nothing at all. This is the one place the app asks the
 * lifter a question mid-workout, so it says which exercise it is about rather than asking in the abstract.
 */
@RunWith(AndroidJUnit4::class)
class BodyweightPromptDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val saved = mutableListOf<Long>()
    private var dismissed = 0

    private fun show() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalWeightUnit provides WeightUnit.KILOGRAMS) {
                BodyweightPromptDialog(
                    exerciseName = "Pull-up",
                    onSave = { saved += it },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    @Test
    fun theExercise_isNamedInTheQuestion() {
        // "How much do you weigh" alone reads like a question about nothing when it arrives from a block the
        // lifter has scrolled past.
        show()

        composeTestRule.onNodeWithText("Pull-up", substring = true).assertExists()
    }

    @Test
    fun theSaveAction_isRefusedUntilAWeightIsTyped() {
        // A zero would write a set whose load is nothing, which is exactly the outcome the prompt prevents.
        show()

        composeTestRule.onNodeWithTag(TestTags.BODYWEIGHT_SAVE).assertIsNotEnabled()
    }

    @Test
    fun aTypedWeight_isSavedInGrams() {
        show()

        composeTestRule.onNodeWithTag(TestTags.BODYWEIGHT_FIELD).performTextInput("75")
        composeTestRule.onNodeWithTag(TestTags.BODYWEIGHT_SAVE).assertIsEnabled().performClick()

        assertEquals(listOf(75_000L), saved)
    }

    @Test
    fun dismissing_writesNothing() {
        show()

        composeTestRule.onNodeWithTag(TestTags.BODYWEIGHT_DISMISS).performClick()

        assertEquals(1, dismissed)
        assertEquals("a dismissed prompt saves no weight", emptyList<Long>(), saved)
    }
}
