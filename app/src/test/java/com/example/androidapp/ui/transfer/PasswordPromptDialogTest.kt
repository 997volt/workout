package com.example.androidapp.ui.transfer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The password prompt (ROADMAP N294).
 *
 * The prompt is the only place a password is typed, and the two rules it enforces are the ones a stored
 * file cannot recover from: a minimum length, and a matching confirmation on an export.
 */
@RunWith(AndroidJUnit4::class)
class PasswordPromptDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aShortPassword_isRefusedBeforeTheCallback() {
        var confirmed: String? = null
        setDialog(onConfirm = { confirmed = it })

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD).performTextInput("short")
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM).performClick()

        assertThat(confirmed).isNull()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CANCEL).performClick()
    }

    @Test
    fun anExport_needsTheConfirmationToMatch() {
        var confirmed: String? = null
        setDialog(confirmWithSecondField = true, onConfirm = { confirmed = it })

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD).performTextInput("correct horse")
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD).performTextInput("correct horsf")
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM).performClick()

        assertThat(confirmed).isNull()

        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD).performTextInput("correct horse")
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM).performClick()

        assertThat(confirmed).isEqualTo("correct horse")
    }

    private fun setDialog(
        confirmWithSecondField: Boolean = false,
        onConfirm: (String) -> Unit,
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                PasswordPromptDialog(
                    title = "Open encrypted backup",
                    description = "Enter the password.",
                    confirmLabel = "Open",
                    confirmWithSecondField = confirmWithSecondField,
                    onConfirm = onConfirm,
                    onDismiss = {},
                )
            }
        }
    }
}
