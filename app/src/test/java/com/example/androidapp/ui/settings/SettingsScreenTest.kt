package com.example.androidapp.ui.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings screen (ROADMAP B23, N43).
 *
 * The assertions are the ones that matter for each half: the screen shows the stored value and a
 * tap sends the choice, and the data actions — export, import and the irreversible clear — reach
 * their callbacks, with the clear behind its typed confirmation rather than a bare tap.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val chosen = mutableListOf<Int>()
    private var backs = 0
    private var exported = 0
    private var imported = 0
    private var cleared = 0

    private fun show(state: SettingsUiState, message: String? = null) {
        composeTestRule.setContent {
            SettingsScreen(
                state = state,
                onSetDefaultRest = { chosen += it },
                onBack = { backs++ },
                onExportData = { exported++ },
                onImportData = { imported++ },
                onClearData = { cleared++ },
                message = message,
            )
        }
    }

    @Test
    fun theStoredValue_isShown_asATime() {
        show(SettingsUiState(defaultRestSeconds = 90))

        composeTestRule.onNodeWithText("Now: 1:30").assertExists()
        composeTestRule.onNodeWithTag(TestTags.settingRest(90)).assertIsSelected()
    }

    @Test
    fun tappingAChoice_sendsIt() {
        show(SettingsUiState(defaultRestSeconds = 90))

        composeTestRule.onNodeWithTag(TestTags.settingRest(30)).performClick()

        assertEquals(listOf(30), chosen)
    }

    @Test
    fun aFailure_isSaidOutLoud() {
        // The stored-vs-tapped rule means a refused choice leaves the old value in force; the
        // screen has to say why, or nothing distinguishes it from a tap that did not register.
        show(
            SettingsUiState(
                defaultRestSeconds = 90,
                error = DataError.Invalid("a rest must be between 5 seconds and an hour"),
            ),
        )

        composeTestRule.onNodeWithText("a rest must be between 5 seconds and an hour").assertExists()
    }

    @Test
    fun theDataSection_offersExportAndImport() {
        // ROADMAP N43: the data actions moved here from the home overflow.
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.DATA_EXPORT).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.DATA_IMPORT).performScrollTo().performClick()

        assertEquals(1, exported)
        assertEquals(1, imported)
    }

    @Test
    fun deleteEverything_asksForTheTypedConfirmation() {
        // N18's guard, now reached from Settings: the row opens the dialog and nothing is deleted
        // until the word is typed and the action pressed.
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_CLEAR_DATA).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.CLEAR_CONFIRM_FIELD).assertExists()
        assertEquals("nothing may be deleted before the confirmation", 0, cleared)

        composeTestRule.onNodeWithTag(TestTags.CLEAR_CONFIRM_FIELD).performTextInput("DELETE")
        composeTestRule.onNodeWithTag(TestTags.CLEAR_CONFIRM_ACTION).performClick()

        assertEquals(1, cleared)
    }

    @Test
    fun aTransferMessage_isShown() {
        show(SettingsUiState(), message = "Backup written.")

        composeTestRule.onNodeWithText("Backup written.").assertExists()
    }

    @Test
    fun back_leaves() {
        show(SettingsUiState())

        // The back control is icon-only, so its label is a content description (N21's rule that
        // accessibility accompanies each screen).
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
