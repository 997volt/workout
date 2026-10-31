package com.example.androidapp.ui.transfer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.repository.BackupPreview
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
    fun theCounts_areSaid_andEitherAnswerIsReported() {
        show(
            BackupPreview(
                sessions = 509,
                sets = 13_861,
                measurements = 863,
                exercises = 186,
                templates = 16,
                programs = 2,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_DIALOG).assertExists()
        composeTestRule.onNodeWithText("509 sessions", substring = true).assertExists()
        composeTestRule.onNodeWithText("13861 sets", substring = true).assertExists()
        composeTestRule.onNodeWithText("863 measurements", substring = true).assertExists()

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CONFIRM).performClick()
        assertEquals(1, confirmed)

        composeTestRule.onNodeWithTag(TestTags.IMPORT_PREVIEW_CANCEL).performClick()
        assertEquals(1, dismissed)
    }
}
