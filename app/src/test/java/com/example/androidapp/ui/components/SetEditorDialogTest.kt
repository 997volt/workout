package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.SetType
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the number steppers (ROADMAP P1.3a) and for the set's
 * RPE and comment (N6).
 *
 * `Weight.step` already had unit tests and no callers. These are about the wiring
 * the unit tests cannot see: that a tap actually moves the field, that the floors
 * hold at the extremes, and that the RPE stepper records the number it shows — and,
 * for a set that recorded none, that a correction leaves it that way rather than
 * inventing the default (N59).
 */
@RunWith(AndroidJUnit4::class)
class SetEditorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saved: SetEdit? = null

    private fun show(
        reps: Int = 5,
        weightGrams: Long = 100_000L,
        rpe: Int? = null,
        setType: SetType = SetType.NORMAL,
    ) {
        composeTestRule.setContent {
            SetEditorDialog(
                initialReps = reps,
                initialWeightGrams = weightGrams,
                initialRpe = rpe,
                initialSetType = setType,
                onDismiss = {},
                onSave = { saved = it },
            )
        }
    }

    @Test
    fun steppingWeight_addsTheDefaultStep() {
        show(weightGrams = 100_000L)

        // 100 kg, and the default step is 2.5 kg.
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_INCREASE_WEIGHT).performClick()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).assertTextContains("102.5")
    }

    @Test
    fun steppingRepsDown_stopsAtOne() {
        // A set of zero reps is not a set, so this floor is 1 — unlike weight,
        // where 0 is meaningful.
        show(reps = 1)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_REPS).performClick()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_REPS_FIELD).assertTextContains("1")
    }

    @Test
    fun steppingRepsUp_raisesTheField() {
        show(reps = 5)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_INCREASE_REPS).performClick()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_REPS_FIELD).assertTextContains("6")
    }

    @Test
    fun anUnusableWeight_disablesSave_ratherThanGuessing() {
        show()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).performTextInput("not a number")

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).assertIsNotEnabled()
    }

    @Test
    fun aZeroWeightIsAccepted_becauseBodyweightSetsHaveNoExternalLoad() {
        // This is the bodyweight decision, asserted rather than assumed: a
        // push-up is reps at 0 kg, and it must be savable today.
        show(weightGrams = 0L)

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).assertIsEnabled().performClick()

        assertEquals(SetEdit(reps = 5, weightGrams = 0L, rpeHalves = null, note = null), saved)
    }

    @Test
    fun steppingWeightDown_pastZero_becomesAssistance() {
        // The field is one signed number (ROADMAP N15): 1 kg down twice is 1 − 2.5,
        // and a *negative* weight is still impossible — it is assistance instead.
        // Stepping is how a machine's help is reached without typing a minus.
        show(weightGrams = 1_000L)

        // One step down from 1 kg: 1 − 2.5.
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_WEIGHT).performClick()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).assertTextContains("-1.5")
    }

    @Test
    fun typingAMinus_storesAssistance_againstAZeroWeight() {
        // ROADMAP N15: one field, two columns.
        show(weightGrams = 0L)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).performTextInput("-20")
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(0L, saved?.weightGrams)
        assertEquals(20_000L, saved?.assistanceGrams)
    }

    @Test
    fun anAssistedSet_opensShowingTheMinus() {
        composeTestRule.setContent {
            SetEditorDialog(
                initialReps = 8,
                initialWeightGrams = 0L,
                initialAssistanceGrams = 20_000L,
                onDismiss = {},
                onSave = { saved = it },
            )
        }

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_WEIGHT_FIELD).assertTextContains("-20")
    }

    @Test
    fun theRpeStepperAndCommentField_areOfferedForAWorkingSet() {
        // N6's decision: RPE is visible on every edit — except a warm-up, which records no effort
        // and is not offered the field at all (N67). It is a stepper now (N59), and a set that
        // recorded no effort says so rather than opening on a number nobody gave; the comment is the
        // other field that may stay empty.
        //
        // Existence rather than "displayed": the editor is taller than
        // Robolectric's default window, and what matters is that the fields are
        // always part of it, not where they land on a small screen.
        show()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertExists()
        composeTestRule.onNodeWithTag(TestTags.SET_NOTE_FIELD).assertExists()
    }

    @Test
    fun aWarmUpSet_isNotOfferedTheRpeField_andSavesWithoutOne() {
        // ROADMAP N67: a warm-up carries no effort. The editor does not show the field, and a save
        // drops the number the set happened to carry rather than keeping it behind a control the
        // lifter cannot see. The comment stays: what a warm-up *was* is still worth writing down.
        show(rpe = 18, setType = SetType.WARMUP)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.SET_NOTE_FIELD).assertExists()

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(SetType.WARMUP, saved?.setType)
        assertEquals("the recorded effort is dropped, not hidden", null, saved?.rpeHalves)
    }

    @Test
    fun aSetThatRecordedNoRpe_keepsNone_andTheFirstTapStatesTheDefault() {
        // This is the correction path's decision, asserted rather than assumed: editing a set's weight
        // must not turn an unrecorded effort into a 9.0 measurement (N59). The stepper's first tap is
        // how a lifter states one, and it states the default 9.0 rather than stepping a number that
        // was never there.
        show()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("Not recorded")

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()
        assertEquals(SetEdit(reps = 5, weightGrams = 100_000L, rpeHalves = null, note = null), saved)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_RPE).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("9")

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()
        assertEquals(18, saved?.rpeHalves)
    }

    @Test
    fun anRpeAndComment_areReportedOnSave() {
        show(rpe = 18)

        // Two steps down from 9.0 is 8.
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_RPE).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_RPE).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_NOTE_FIELD).performTextInput("Felt heavy")
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals("8 is 16 halves", 16, saved?.rpeHalves)
        assertEquals("Felt heavy", saved?.note)
    }

    @Test
    fun steppingTheRpeUp_movesByAHalfPoint() {
        // ROADMAP N6 extended: the step is half a point, so 9 → 9.5 (19 halves).
        show(rpe = 18)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_INCREASE_RPE).performClick()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("9.5")
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(19, saved?.rpeHalves)
    }

    @Test
    fun theRpeStepper_stopsAtBothEndsOfTheScale() {
        // Clamped rather than wrapped: holding either button cannot leave 1–10, so there is no
        // off-scale value left to refuse (the shape the reps stepper's floor at 1 uses).
        show(rpe = 18)

        repeat(20) { composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_RPE).performClick() }
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("1")

        repeat(20) { composeTestRule.onNodeWithTag(TestTags.SET_EDIT_INCREASE_RPE).performClick() }
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("10")
    }

    @Test
    fun theRoleSelector_changesWhatIsSaved() {
        // ROADMAP N14: `SetType` carried warm-up, drop and failure with no way to
        // reach them from the UI until this selector existed.
        show()

        composeTestRule.onNodeWithTag(TestTags.SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.setRole("TOP_SET")).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(SetType.TOP_SET, saved?.setType)
    }

    @Test
    fun aSetLeftAlone_isAPlainWorkingSet() {
        // The common case must not need a tap: nothing selected is NORMAL (N14).
        show()

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(SetType.NORMAL, saved?.setType)
    }

    @Test
    fun anExistingSetsRole_isShownRatherThanReset() {
        // Editing a set you logged as a warm-up must keep it one.
        composeTestRule.setContent {
            SetEditorDialog(
                initialReps = 5,
                initialWeightGrams = 60_000L,
                initialSetType = SetType.WARMUP,
                onDismiss = {},
                onSave = { saved = it },
            )
        }

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(SetType.WARMUP, saved?.setType)
        composeTestRule.onNodeWithText("Role: Warm-up").assertIsDisplayed()
    }

    @Test
    fun anExistingHalfStepRpe_opensWhereItWasRecorded() {
        // An existing set keeps the RPE it recorded (N59), halves included: 19 halves is 9.5.
        composeTestRule.setContent {
            SetEditorDialog(
                initialReps = 5,
                initialWeightGrams = 100_000L,
                initialRpe = 19,
                onDismiss = {},
                onSave = { saved = it },
            )
        }

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_RPE_FIELD).assertTextEquals("9.5")

        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).performClick()

        assertEquals(19, saved?.rpeHalves)
    }
}
