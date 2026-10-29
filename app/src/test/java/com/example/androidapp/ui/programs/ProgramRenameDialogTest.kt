package com.example.androidapp.ui.programs

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The question a refused load asks (ROADMAP N243).
 *
 * Three things have to hold for the answer to be usable: the field opens on a name the list does not hold, a
 * name that IS held cannot be confirmed — two programs a lifter cannot tell apart are worse than the refusal
 * this dialog exists to get past — and confirming reports exactly the name that was typed.
 */
@RunWith(AndroidJUnit4::class)
class ProgramRenameDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var confirmed: String? = null
    private var dismissed = false

    private fun show(takenNames: List<String> = listOf("Push Pull Legs")) {
        composeTestRule.setContent {
            ProgramRenameDialog(
                rename = ProgramDocumentPreview.NameTaken(
                    name = "Push Pull Legs",
                    suggestion = "Push Pull Legs (2)",
                ),
                takenNames = takenNames,
                onConfirm = { confirmed = it },
                onDismiss = { dismissed = true },
            )
        }
    }

    @Test
    fun theField_opensOnANameTheListDoesNotHold() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_DIALOG).assertExists()
        // The body says which name is taken rather than leaving the lifter to guess.
        composeTestRule.onNodeWithText(text(R.string.program_rename_text, "Push Pull Legs")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).assertTextContains("Push Pull Legs (2)")
        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_CONFIRM).assertIsEnabled()
    }

    @Test
    fun confirming_reportsTheNameThatWasTyped() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).performTextInput("PPL B")
        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_CONFIRM).performClick()

        assertEquals("PPL B", confirmed)
    }

    @Test
    fun aNameTheListHolds_cannotBeConfirmed() {
        // The dialog's own guard, so the refusal arrives while the lifter is looking at the field rather than
        // after the write it would have cost.
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).performTextInput("push pull legs")

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithText(text(R.string.program_rename_taken)).assertExists()
    }

    @Test
    fun anEmptyName_cannotBeConfirmed() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_NAME).performTextClearance()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun cancel_reportsTheDismissal_andWritesNothing() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.RENAME_CANCEL).performClick()

        assertTrue(dismissed)
        assertEquals(null, confirmed)
    }

    private fun text(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)
}
