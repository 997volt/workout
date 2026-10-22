package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.shared.Change
import com.example.androidapp.shared.ChangeKind
import com.example.androidapp.shared.PlanWarning
import com.example.androidapp.shared.WarningCode
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The confirmation an edit stops at (ROADMAP N171).
 *
 * The dialog's job is to state the diff **the planner produced** before anything is written, so the
 * assertions are the diff's own: the program's name, the four counts, the new movement, the workout
 * that stays, and the other program the edit also changes.
 */
@RunWith(AndroidJUnit4::class)
class ProgramEditDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        edit: ProgramDocumentPreview.Edit = preview(),
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProgramEditDialog(edit = edit, onConfirm = onConfirm, onDismiss = onDismiss)
        }
    }

    @Test
    fun theDiff_namesTheProgramAndCountsWhatChanges() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Programs.EDIT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Autumn block", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Days: 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Workouts: 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sets: 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("New movements: Machine Curl", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("These workouts stay in your library: Upper A", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Also used by Conditioning", substring = true).assertIsDisplayed()
    }

    @Test
    fun confirming_reachesItsCallback() {
        var confirmed = false
        show(onConfirm = { confirmed = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.EDIT_CONFIRM).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun cancelling_reachesItsCallback() {
        var dismissed = false
        show(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.EDIT_CANCEL).performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun aRenameOnItsOwn_saysSoRatherThanListingZeroes() {
        show(
            preview(
                changes = listOf(Change(ChangeKind.PROGRAM_RENAMED, "pgm_1", "Autumn block")),
                warnings = emptyList(),
                sharedProgramNames = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("Renamed to “Autumn block”.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Days: 0", substring = true).assertDoesNotExist()
    }

    private fun preview(
        changes: List<Change> = listOf(
            Change(ChangeKind.SLOT_ADDED, "slot_b", "Lower B"),
            Change(ChangeKind.TEMPLATE_UPDATED, "tpl_a", "Upper A"),
            Change(ChangeKind.SET_ADDED, "set_b", null),
        ),
        warnings: List<PlanWarning> = listOf(
            PlanWarning(WarningCode.NEW_MOVEMENT, "ex_new", "Machine Curl"),
            PlanWarning(WarningCode.WORKOUT_KEPT, "tpl_a", "Upper A"),
        ),
        sharedProgramNames: List<String> = listOf("Conditioning"),
    ) = ProgramDocumentPreview.Edit(
        programName = "Autumn block",
        changes = changes,
        warnings = warnings,
        sharedProgramNames = sharedProgramNames,
    )
}
