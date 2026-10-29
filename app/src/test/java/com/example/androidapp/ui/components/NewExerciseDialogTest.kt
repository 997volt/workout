package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * The name-only creation dialog (ROADMAP N2).
 *
 * The rule these assert is that a nameless exercise cannot be created: there is
 * no sensible "unspecified" name to fall back to, unlike the taxonomy fields.
 */
@RunWith(AndroidJUnit4::class)
class NewExerciseDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var created: String? = null
    private var createdFamily: String? = null
    private var dismissed = false

    private fun show(error: DataError? = null) {
        composeTestRule.setContent {
            NewExerciseDialog(
                error = error,
                onDismiss = { dismissed = true },
                onCreate = { name, family ->
                    created = name
                    createdFamily = family
                },
            )
        }
    }

    @Test
    fun addIsDisabled_untilANameIsTyped() {
        show()

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_SAVE).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_NAME).performTextInput("Sled Push")

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_SAVE).assertIsEnabled()
    }

    @Test
    fun aWhitespaceOnlyName_doesNotEnableAdd() {
        show()

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_NAME).performTextInput("   ")

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_SAVE).assertIsNotEnabled()
    }

    @Test
    fun adding_reportsTheTrimmedName() {
        show()

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_NAME).performTextInput("  Sled Push  ")
        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_SAVE).performClick()

        assertEquals("Sled Push", created)
    }

    @Test
    fun aFamilyCanBeChosen_whileTheMovementIsMade() {
        // ROADMAP N232: filing happens here rather than after, because an unfiled movement is the shape the
        // library's filters exist for.
        composeTestRule.setContent {
            NewExerciseDialog(
                error = null,
                onDismiss = { dismissed = true },
                onCreate = { name, family ->
                    created = name
                    createdFamily = family
                },
                families = listOf(
                    Exercise(
                        id = "cat-bench",
                        name = "Bench Press",
                        primaryMuscle = MuscleGroup.CHEST,
                        equipment = Equipment.OTHER,
                        movementPattern = MovementPattern.PRESS,
                        rowKind = RowKind.CATEGORY,
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_NAME).performTextInput("Front Squat")
        // Nothing chosen is the default, and it means "no family" rather than "not decided".
        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_FAMILY_NONE).assertIsDisplayed()
        // A family is one tap, and choosing it is what the two writes are for.
        composeTestRule.onNodeWithTag(TestTags.newExerciseFamily("cat-bench")).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_SAVE).performClick()

        assertEquals("Front Squat", created)
        assertEquals("cat-bench", createdFamily)
    }

    @Test
    fun cancel_dismissesWithoutCreating() {
        show()

        composeTestRule.onNodeWithTag(TestTags.NEW_EXERCISE_CANCEL).performClick()

        assertEquals(true, dismissed)
        assertEquals(null, created)
    }

    @Test
    fun aFailedWrite_isShownInsideTheDialog() {
        show(error = DataError.Storage(IOException("disk full")))

        composeTestRule.onNodeWithText("Couldn’t save that. Your last change may not be stored.")
            .assertIsDisplayed()
    }
}
