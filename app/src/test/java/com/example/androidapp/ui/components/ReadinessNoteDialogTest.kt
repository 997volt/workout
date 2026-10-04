package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The readiness dialog (ROADMAP N4), including the sore-muscle list beside the note (N62).
 *
 * One dialog serves the prompt and the later edit, so what is asserted here is the
 * part that matters: *Skip* and *Cancel* both close without writing, an empty
 * field saves as "no note" rather than as an empty string, and the list is picked,
 * scored and removed one muscle at a time.
 */
@RunWith(AndroidJUnit4::class)
class ReadinessNoteDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var savedNote: String? = null
    private var savedSoreMuscles: List<SoreMuscle> = emptyList()
    private var saveCalled = false
    private var dismissed = false

    private fun show(
        initial: String = "",
        initialSoreMuscles: List<SoreMuscle> = emptyList(),
        isPrompt: Boolean = true,
    ) {
        composeTestRule.setContent {
            ReadinessNoteDialog(
                initialNote = initial,
                initialSoreMuscles = initialSoreMuscles,
                isPrompt = isPrompt,
                onDismiss = { dismissed = true },
                onSave = { note, sore ->
                    savedNote = note
                    savedSoreMuscles = sore
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
        assertEquals("and the list rides the same save", emptyList<SoreMuscle>(), savedSoreMuscles)
    }

    @Test
    fun savingAnEmptyNote_writesNull() {
        show()

        composeTestRule.onNodeWithTag(TestTags.READINESS_SAVE).performClick()

        assertTrue("save must still be reported", saveCalled)
        assertNull("nothing typed is not an empty note", savedNote)
    }

    @Test
    fun thePrompt_canBeSkipped_withoutWriting() {
        show(isPrompt = true)

        composeTestRule.onNodeWithTag(TestTags.READINESS_DISMISS).performClick()

        assertTrue(dismissed)
        assertFalse("skipping must not write", saveCalled)
    }

    @Test
    fun theEdit_canBeCancelled_withoutWriting() {
        show(initial = "Slept badly", isPrompt = false)

        composeTestRule.onNodeWithTag(TestTags.READINESS_DISMISS).performClick()

        assertTrue(dismissed)
        assertFalse("cancelling must not write", saveCalled)
    }

    @Test
    fun anExistingList_isShown_withEachMusclesOwnScore() {
        // ROADMAP N62: "quads 8, calves 3" is the fact, and one number for the whole body is not.
        show(
            initial = "Slept badly",
            initialSoreMuscles = listOf(
                SoreMuscle(MuscleGroup.QUADS, 8),
                SoreMuscle(MuscleGroup.CALVES, 3),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRow("QUADS")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreScore("QUADS")).assertTextEquals("8/10")
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRow("CALVES")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreScore("CALVES")).assertTextEquals("3/10")
    }

    @Test
    fun pickingAMuscle_addsIt_inTheMiddleOfTheScale() {
        show()

        composeTestRule.onNodeWithTag(TestTags.Readiness.SORE_ADD).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreOption("CHEST")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRow("CHEST")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreScore("CHEST")).assertTextEquals("5/10")
    }

    @Test
    fun aMuscleAlreadyPicked_isNotOfferedAgain() {
        show(initialSoreMuscles = listOf(SoreMuscle(MuscleGroup.CHEST, 6)))

        composeTestRule.onNodeWithTag(TestTags.Readiness.SORE_ADD).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreOption("CHEST")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreOption("BACK")).assertExists()
    }

    @Test
    fun theTaxonomysOther_isNotOffered() {
        // N62: `OTHER` is "not specified", so it is not a muscle anyone can be sore in.
        show()

        composeTestRule.onNodeWithTag(TestTags.Readiness.SORE_ADD).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreOption("OTHER")).assertDoesNotExist()
    }

    @Test
    fun raisingAndLoweringAScore_movesItOnePointAtATime() {
        show(initialSoreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)))

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreIncrease("QUADS")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreScore("QUADS")).assertTextEquals("9/10")

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreDecrease("QUADS")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreDecrease("QUADS")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreScore("QUADS")).assertTextEquals("7/10")
    }

    @Test
    fun theScoreStopsAtTheEndsOfTheScale() {
        show(initialSoreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 1), SoreMuscle(MuscleGroup.CORE, 10)))

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreDecrease("QUADS")).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreIncrease("QUADS")).assertIsEnabled()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreIncrease("CORE")).assertIsNotEnabled()
    }

    @Test
    fun removingAMuscle_takesItOffTheList() {
        show(
            initialSoreMuscles = listOf(
                SoreMuscle(MuscleGroup.QUADS, 8),
                SoreMuscle(MuscleGroup.CALVES, 3),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRemove("QUADS")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRow("QUADS")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreRow("CALVES")).assertExists()
    }

    @Test
    fun saving_reportsTheNoteAndTheWholeList() {
        show(
            initial = "Travel day",
            initialSoreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8), SoreMuscle(MuscleGroup.CALVES, 3)),
        )

        composeTestRule.onNodeWithTag(TestTags.Readiness.SORE_ADD).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Readiness.soreOption("CHEST")).performClick()
        composeTestRule.onNodeWithTag(TestTags.READINESS_SAVE).performClick()

        assertEquals("Travel day", savedNote)
        assertEquals(
            listOf(
                SoreMuscle(MuscleGroup.QUADS, 8),
                SoreMuscle(MuscleGroup.CALVES, 3),
                SoreMuscle(MuscleGroup.CHEST, 5),
            ),
            savedSoreMuscles,
        )
    }
}
