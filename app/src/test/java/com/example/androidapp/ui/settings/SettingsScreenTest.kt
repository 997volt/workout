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
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

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
    private var diagnosticsOpened = 0
    private val restTimerWrites = mutableListOf<Boolean>()
    private val progressionWrites = mutableListOf<ProgressionMode>()
    private val unitWrites = mutableListOf<WeightUnit>()
    private val backupEnabledWrites = mutableListOf<Boolean>()
    private val backupScheduleWrites = mutableListOf<BackupSchedule>()
    private var folderPicks = 0

    private fun show(state: SettingsUiState, message: String? = null) {
        composeTestRule.setContent {
            SettingsScreen(
                state = state,
                onSetDefaultRest = { chosen += it },
                onBack = { backs++ },
                onSetRestTimer = { restTimerWrites += it },
                onSetWeightUnit = { unitWrites += it },
                onSetProgressionMode = { progressionWrites += it },
                onEnableBackup = { backupEnabledWrites += it },
                onSetBackupSchedule = { backupScheduleWrites += it },
                onChooseBackupFolder = { folderPicks++ },
                onExportData = { exported++ },
                onImportData = { imported++ },
                onClearData = { cleared++ },
                onOpenDiagnostics = { diagnosticsOpened++ },
                message = message,
                onSetRestCue = { _ -> },
                onSetKeepScreenOn = { _ -> },
                onDismissMessage = {},
            )
        }
    }

    @Test
    fun theDiagnosticsRow_opensTheScreen() {
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_DIAGNOSTICS).performScrollTo().performClick()

        assertEquals(1, diagnosticsOpened)
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
    fun theRestTimerSwitch_sendsItsChoice() {
        // ROADMAP N44: the switch is on by default, so a tap turns the countdown off.
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_REST_TIMER)
            .performScrollTo()
            .performClick()

        assertEquals(listOf(false), restTimerWrites)
    }

    @Test
    fun theWeightUnitChips_sendTheChoice_andShowWhatIsInForce() {
        // ROADMAP N64: the app-wide unit, and the screen shows the stored one rather than the tapped.
        show(SettingsUiState(weightUnit = WeightUnit.KILOGRAMS))

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_WEIGHT_UNIT_KG)
            .performScrollTo()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_WEIGHT_UNIT_LB)
            .performScrollTo()
            .performClick()

        assertEquals(listOf(WeightUnit.POUNDS), unitWrites)
    }

    @Test
    fun theProgressionMode_sendsItsChoice() {
        // P3.17: three answers rather than a switch, and the one tapped is the one written.
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.settingsProgressionMode(ProgressionMode.OFF.name))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(ProgressionMode.OFF), progressionWrites)
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

    @Test
    fun theBackupSwitch_sendsItsChoice() {
        // ROADMAP N218: off by default, so the first tap is the one that turns it on.
        show(SettingsUiState())

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_BACKUP_SWITCH).performScrollTo().performClick()

        assertEquals(listOf(true), backupEnabledWrites)
    }

    @Test
    fun aBackupPeriod_showsTheStoredOne_andSendsTheTappedOne() {
        // The period is only offered once the backup will actually run: a period for a backup that never
        // happens would be a control with nothing behind it.
        show(SettingsUiState(backup = BackupSettings(enabled = true, schedule = BackupSchedule.WEEKLY)))

        composeTestRule.onNodeWithTag(TestTags.settingsBackupSchedule(BackupSchedule.WEEKLY.name))
            .performScrollTo()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.settingsBackupSchedule(BackupSchedule.DAILY.name))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(BackupSchedule.DAILY), backupScheduleWrites)
    }

    @Test
    fun theFolderRow_opensThePicker() {
        show(SettingsUiState(backup = BackupSettings(enabled = true)))

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_BACKUP_FOLDER).performScrollTo().performClick()

        assertEquals(1, folderPicks)
    }

    @Test
    fun theLastBackup_isShownAsADate() {
        // The date is formatted for the device's locale and zone, so the assertion is on the label rather than
        // on a literal no other machine would produce.
        show(
            SettingsUiState(
                backup = BackupSettings(
                    enabled = true,
                    lastSuccessAt = Instant.parse("2026-10-26T10:00:00Z"),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.SETTINGS_BACKUP_LAST).assertExists()
        composeTestRule.onNodeWithText("Last backup:", substring = true).assertExists()
    }

    @Test
    fun aFailedBackup_isSaidOutLoud() {
        // The whole reason the row exists: a backup that quietly stopped is worse than none (N218).
        show(SettingsUiState(backup = BackupSettings(enabled = true, failed = true)))

        composeTestRule.onNodeWithText("The last backup failed. Check that the folder still exists.")
            .assertExists()
    }

    @Test
    fun aBackupWithNoFolder_saysThereIsNowhereToWrite() {
        show(SettingsUiState(backup = BackupSettings(enabled = true)))

        composeTestRule.onNodeWithText("Choose a folder — until then a backup has nowhere to go.")
            .assertExists()
    }
}
