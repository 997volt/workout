package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The readiness dialog (ROADMAP N4).
 *
 * One dialog serves the prompt and the later edit, so what is asserted here is the part that matters:
 * *Skip* and *Cancel* both close without writing, an empty field saves as "no note" rather than as an
 * empty string, and the note is trimmed.
 *
 * **The sore-muscle list left this dialog at N113.** Its picker is tested where it lives now, in
 * `com.example.androidapp.ui.measurements.MeasurementSorenessTest`; what is asserted *here* is that a
 * workout no longer asks for it at all.
 */
@RunWith(AndroidJUnit4::class)
class ReadinessNoteDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var savedNote: String? = null
    private var saveCalled = false
    private var dismissed = false

    private fun show(initial: String = "") {
        composeTestRule.setContent {
            ReadinessNoteDialog(
                initialNote = initial,
                onDismiss = { dismissed = true },
                onSave = {
                    savedNote = it
                    saveCalled = true
                },
            )
        }
    }

    @Test
    fun anExistingNote_isPrefilled() {
        show(initial = "Slept badly, legs heavy")

        composeTestRule.onNodeWithTag(TestTags.READINESS_NOTE).assertTextContains("Slept badly, legs heavy")
    }

    @Test
    fun aTypedNote_isTrimmedAndSaved() {
        show()

        composeTestRule.onNodeWithTag(TestTags.READINESS_NOTE).performTextInput("  Slept badly  ")
        composeTestRule.onNodeWithTag(TestTags.READINESS_SAVE).performClick()

        assertEquals("Slept badly", savedNote)
    }

    @Test
    fun savingAnEmptyNote_writesNull() {
        show()

        composeTestRule.onNodeWithTag(TestTags.READINESS_SAVE).performClick()

        assertTrue("save must still be reported", saveCalled)
        assertNull("nothing typed is not an empty note", savedNote)
    }

    @Test
    fun theDialog_canBeDismissed_withoutWriting() {
        // One mode now that nothing prompts it (N140): the row opens it, and dismissing leaves the note as it
        // was rather than writing an empty one.
        show(initial = "Slept badly")

        composeTestRule.onNodeWithTag(TestTags.READINESS_DISMISS).performClick()

        assertTrue(dismissed)
        assertFalse("cancelling must not write", saveCalled)
    }

    @Test
    fun theSoreList_isNotAskedForAnyMore() {
        // N113: soreness is the day's own record now, so a workout's readiness is its note alone.
        show()

        composeTestRule.onNodeWithTag(TestTags.Readiness.SORE_ADD).assertDoesNotExist()
    }
}
