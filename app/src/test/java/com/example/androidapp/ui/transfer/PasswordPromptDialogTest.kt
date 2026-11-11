package com.example.androidapp.ui.transfer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The password prompt (ROADMAP N294, B413).
 *
 * The dialog is **controlled**: its text and its refusal are the transfer ViewModel's, so this test
 * drives a local stand-in for that state and asserts what a lifter sees — the sentence a refusal prints
 * and the fact that neither button starts a second derivation while one runs. The validation itself is
 * tested where it lives, in [DataTransferViewModelTest].
 */
@RunWith(AndroidJUnit4::class)
class PasswordPromptDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theConfirmButton_reportsTheQuestionWasAnswered() {
        var confirmed = false
        setDialog(onConfirm = { confirmed = true })

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD).performTextInput("correct horse battery")
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun anExport_asksForTheConfirmation() {
        setDialog(confirmWithSecondField = true)

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD).assertExists()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD).performTextInput("correct horse battery")
    }

    @Test
    fun aShortPassword_showsItsSentence() {
        setDialog(issue = PasswordIssue.TOO_SHORT)

        composeTestRule.onNodeWithText("Use a password of at least 8 characters.").assertExists()
    }

    @Test
    fun aMismatch_showsItsSentence() {
        setDialog(issue = PasswordIssue.MISMATCH)

        composeTestRule.onNodeWithText("The passwords do not match.").assertExists()
    }

    @Test
    fun aBusyDialog_leavesNeitherButtonAbleToStartAgain() {
        // B401: 600,000 PBKDF2 iterations are a visible pause, and a second tap would stack another pass.
        setDialog(busy = true)

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CANCEL).assertIsNotEnabled()
    }

    private fun setDialog(
        confirmWithSecondField: Boolean = false,
        busy: Boolean = false,
        issue: PasswordIssue? = null,
        onConfirm: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            // The stand-in for the ViewModel's draft: the dialog owns nothing itself.
            var password by remember { mutableStateOf("") }
            var confirmation by remember { mutableStateOf("") }
            AndroidAppTheme {
                PasswordPromptDialog(
                    title = "Open encrypted backup",
                    description = "Enter the password.",
                    confirmLabel = "Open",
                    confirmWithSecondField = confirmWithSecondField,
                    password = password,
                    confirmation = confirmation,
                    onPasswordChange = { password = it },
                    onConfirmationChange = { confirmation = it },
                    issue = issue,
                    onConfirm = onConfirm,
                    onDismiss = {},
                    busy = busy,
                )
            }
        }
    }
}
