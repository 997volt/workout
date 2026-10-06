package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.TemplateSetEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The planned-set dialog (ROADMAP N14).
 *
 * The first test is the one that matters: the draft is held in a `remember`, not a
 * `rememberSaveable`, because a data class is not Bundle-saveable and registering one
 * throws as the dialog opens. That crash reached a device and no test could see it,
 * because every other test drove the ViewModel instead of the dialog.
 */
@RunWith(AndroidJUnit4::class)
class TemplateSetDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saved: TemplateSetEdit? = null

    private fun show(initial: TemplateSetEdit = TemplateSetEdit(), isNew: Boolean = true) {
        composeTestRule.setContent {
            TemplateSetDialog(
                initial = initial,
                isNew = isNew,
                onDismiss = {},
                onSave = { saved = it },
            )
        }
    }

    @Test
    fun theDialogOpens_andSavesWhatWasTyped() {
        show()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).performTextInput("140")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MIN).performTextInput("1")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).performTextInput("2")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_NOTE).performTextInput("grind")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(140_000L, saved?.targetWeightGrams)
        assertEquals(1, saved?.targetRepsMin)
        assertEquals(2, saved?.targetRepsMax)
        assertEquals("grind", saved?.note)
    }

    @Test
    fun aLegacyPerSetRpe_isRoundTripped_thoughTheDialogNoLongerShowsIt() {
        // ROADMAP N59, amended: the effort is one number per exercise now, so this form has no RPE
        // field — but a set that still carries a value from before the change must come back from an
        // edit with it intact rather than silently wiped.
        show(initial = TemplateSetEdit(targetWeightGrams = 60_000L, targetRpeHalves = 19), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).performTextInput("65")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(65_000L, saved?.targetWeightGrams)
        assertEquals("the legacy per-set RPE is carried, not cleared", 19, saved?.targetRpeHalves)
    }

    @Test
    fun reRolingASetToWarmUp_clearsTheLegacyPerSetEffort() {
        // ROADMAP N67: a planned warm-up carries no effort. Migration 28→29 cleared the value off the
        // rows already stored, so the one way it could come back is an edit that re-roles a set — and
        // there the dialog has to drop it rather than round-trip it (the case above still holds for a
        // role that records an effort).
        show(
            initial = TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 60_000L,
                targetRpeHalves = 19,
            ),
            isNew = false,
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole("WARMUP")).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(SetType.WARMUP, saved?.role)
        assertEquals("a planned warm-up carries no target effort", null, saved?.targetRpeHalves)
    }

    @Test
    fun everyFieldCanBeLeftEmpty() {
        // A plan may say "work up to a heavy single" and mean it (N14).
        show()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(TemplateSetEdit(role = SetType.NORMAL), saved)
    }

    @Test
    fun theRoleCanBeChanged_fromTheDialog() {
        show()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole("TOP_SET")).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(SetType.TOP_SET, saved?.role)
    }

    @Test
    fun anExistingTargets_areShownRatherThanReset() {
        show(
            initial = TemplateSetEdit(
                role = SetType.TOP_SET,
                targetWeightGrams = 60_000L,
                targetRepsMax = 8,
            ),
            isNew = false,
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(SetType.TOP_SET, saved?.role)
        assertEquals(60_000L, saved?.targetWeightGrams)
        assertEquals(8, saved?.targetRepsMax)
    }

    @Test
    fun aDropsValue_isShown_andIsWhatTheSaveCarries() {
        // ROADMAP N79: a drop rung is authored by the value it takes off the anchor, so that is what
        // the dialog opens on and what a save without edits writes back — a rung carries no weight or
        // reps of its own for this test to have been about.
        show(
            initial = TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L),
            isNew = false,
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE)
            .assertTextContains("20", substring = true)
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(SetType.DROP, saved?.role)
        assertEquals(20_000L, saved?.dropValueGrams)
        assertNull("and nothing it does not have", saved?.targetWeightGrams)
    }
}
