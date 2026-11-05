package com.example.androidapp.ui.transfer

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.BackupProblem
import com.example.androidapp.domain.repository.PreviewCounts
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The question a backup asks before it is merged (ROADMAP N246, N258).
 *
 * It has to name what the file holds and that nothing is overwritten, both answers have to travel, and each row
 * has to say itself — four bare numbers are four numbers a reader cannot attribute to a column.
 */
@RunWith(AndroidJUnit4::class)
class ImportPreviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    private fun show(preview: BackupPreview) {
        composeTestRule.setContent {
            ImportPreviewDialog(
                preview = preview,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
    }

    @Test
    fun aFileWithAProblem_cannotBeMerged_andNamesTheFirstRow() {
        // B342: the import writes decoded rows straight to entities, so a value no write boundary accepts
        // is refused before the transaction and the preview says what to correct.
        show(
            BackupPreview(
                sessions = PreviewCounts(0, 0, 0, 0),
                sets = PreviewCounts(1, 0, 0, 1),
                measurements = PreviewCounts(0, 0, 0, 0),
                exercises = PreviewCounts(0, 0, 0, 0),
                problems = listOf(
                    BackupProblem("Set 1", "A counted set needs between 1 and 10000 reps."),
                    BackupProblem("Measurement 2", "Body fat must be between 0% and 100%."),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_PROBLEMS).assertExists()
        composeTestRule.onNodeWithText(
            "This file has 2 values the app cannot store, so it cannot be merged.",
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Set 1 — A counted set needs between 1 and 10000 reps.",
        ).assertExists()
        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun aLegacyFile_saysItsOutOfRangeValues_willBeRestoredAsIs() {
        // B374: a file written before the current ceilings may hold a 183% reading; refusing it would
        // lose the only copy, so the preview accepts it and says the values travel as they are.
        show(
            BackupPreview(
                sessions = PreviewCounts(0, 0, 0, 0),
                sets = PreviewCounts(0, 0, 0, 0),
                measurements = PreviewCounts(1, 0, 0, 1),
                exercises = PreviewCounts(0, 0, 0, 0),
                legacyLimits = true,
            ),
        )

        composeTestRule.onNodeWithText("written by an older version", substring = true).assertExists()
        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CONFIRM).performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun theFile_andWhatTheMergeWouldDo_areShown_andEitherAnswerIsReported() {
        show(
            BackupPreview(
                sessions = PreviewCounts(inFile = 509, alreadyHere = 506, restored = 2, added = 1),
                sets = PreviewCounts(inFile = 13_861, alreadyHere = 13_800, restored = 40, added = 21),
                measurements = PreviewCounts(inFile = 864, alreadyHere = 863, restored = 0, added = 0),
                exercises = PreviewCounts(inFile = 187, alreadyHere = 186, restored = 0, added = 0),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_DIALOG).assertExists()
        // The columns are the legend, and each row says itself in one sentence (N248, N258).
        composeTestRule.onNodeWithText("File").assertExists()
        composeTestRule.onNodeWithText("Back").assertExists()
        composeTestRule.onNodeWithContentDescription(
            "Sessions — in the file: 509, already here: 506, would come back: 2, new: 1",
        ).assertExists()
        composeTestRule.onNodeWithContentDescription(
            "Measurements — in the file: 864, already here: 863, would come back: 0, new: 0",
        ).assertExists()

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CONFIRM).performClick()
        assertEquals(1, confirmed)

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CANCEL).performClick()
        assertEquals(1, dismissed)
    }
}
