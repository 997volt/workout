package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

    private fun show(
        initial: TemplateSetEdit = TemplateSetEdit(),
        isNew: Boolean = true,
        stepGrams: Long? = null,
    ) {
        composeTestRule.setContent {
            TemplateSetDialog(
                initial = initial,
                isNew = isNew,
                onDismiss = {},
                onSave = { saved = it },
                stepGrams = stepGrams,
            )
        }
    }

    @Test
    fun theDialogOpens_andSavesWhatWasTyped() {
        show()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).performTextInput("140")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MIN).performTextInput("1")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).performTextInput("2")
        // The ± pairs gave every number a row of its own (N111), so the note is scrolled to: a node
        // below Robolectric's window takes no text.
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_NOTE).performScrollTo().performTextInput("grind")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(140_000L, saved?.targetWeightGrams)
        assertEquals(1, saved?.targetRepsMin)
        assertEquals(2, saved?.targetRepsMax)
        assertEquals("grind", saved?.note)
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

    @Test
    fun steppingTheLoad_movesByTheUnitsStep() {
        // ROADMAP N111: a target is stated the way a logged set is. With no step of its own this
        // exercise moves by the unit's, which is the 2.5 kg a pair of plates gives.
        show(initial = TemplateSetEdit(targetWeightGrams = 100_000L), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_WEIGHT).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("102.5")
    }

    @Test
    fun steppingTheLoad_usesTheExercisesOwnStep() {
        // N77 reaches the plan too: a machine that jumps 5 kg is authored in 5 kg, not in the unit's.
        show(
            initial = TemplateSetEdit(targetWeightGrams = 100_000L),
            isNew = false,
            stepGrams = 5_000L,
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_WEIGHT).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("105")
    }

    @Test
    fun steppingATargetDown_pastZero_becomesAssistance() {
        // One signed field, exactly as a logged set's is (N15, N111): 1 kg down once is 1 − 2.5, and a
        // negative weight is still impossible — it is the machine's help instead, which the save
        // writes to its own column.
        show(initial = TemplateSetEdit(targetWeightGrams = 1_000L), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DECREASE_WEIGHT).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("-1.5")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).performClick()

        assertEquals(0L, saved?.targetWeightGrams)
        assertEquals(1_500L, saved?.targetAssistanceGrams)
    }

    @Test
    fun theRepsRange_stepsEachEndOnItsOwn() {
        // N111: a plan writes a range where a logged set is one number, so each end keeps its own pair.
        show(initial = TemplateSetEdit(targetRepsMin = 8, targetRepsMax = 10), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_REPS_MIN).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_REPS_MAX).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_REPS_MAX).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MIN).assertTextContains("9")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).assertTextContains("12")
    }

    @Test
    fun steppingRepsDown_stopsAtOne() {
        // The workout's floor, on the plan's fields: a set of zero reps is not a set.
        show(initial = TemplateSetEdit(targetRepsMin = 1), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DECREASE_REPS_MIN).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MIN).assertTextContains("1")
    }

    @Test
    fun aRangeSteppedBackwards_isRefused_ratherThanSwapped() {
        // The two ends do not police each other (N111): a max stepped under the min is the invalid
        // state typing it already was, and the save is what refuses it — swapping the ends here would
        // silently change what the lifter asked for.
        show(initial = TemplateSetEdit(targetRepsMin = 5, targetRepsMax = 5), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DECREASE_REPS_MAX).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).assertIsNotEnabled()
    }

    @Test
    fun steppingTheDropValue_movesByTheSameStep() {
        // A run's value is a load, so it moves like one (N111) — both ways, and the restore is what
        // says the two buttons step the same amount.
        show(initial = TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L), isNew = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DECREASE_DROP_VALUE).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertTextContains("17.5")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_INCREASE_DROP_VALUE).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertTextContains("20")
    }
}
