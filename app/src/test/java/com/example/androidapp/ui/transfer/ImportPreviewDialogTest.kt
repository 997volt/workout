package com.example.androidapp.ui.transfer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.PreviewCounts
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The question a backup asks before it is merged (ROADMAP N246).
 *
 * It has to name what the file holds and that nothing is overwritten, and both answers have to travel.
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
        // The four kinds and the four columns are the question: what the file holds, and what the merge would
        // do with it (N248).
        composeTestRule.onNodeWithText("Sessions").assertExists()
        composeTestRule.onNodeWithText("Measurements").assertExists()
        composeTestRule.onNodeWithText("File").assertExists()
        composeTestRule.onNodeWithText("Back").assertExists()
        composeTestRule.onNodeWithText("509").assertExists()
        composeTestRule.onNodeWithText("506").assertExists()
        composeTestRule.onNodeWithText("2").assertExists()
        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("863").assertExists()
        composeTestRule.onNodeWithText("186").assertExists()

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CONFIRM).performClick()
        assertEquals(1, confirmed)

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CANCEL).performClick()
        assertEquals(1, dismissed)
    }
}
