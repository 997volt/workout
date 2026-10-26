package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.WeightUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A timed movement's fields state a hold, not a rep count (ROADMAP N220).
 *
 * The point of the feature is that a forty-five-second plank stops reading as "0 × 45", so the assertion
 * that matters first is the negative one: **the reps field is gone** when the movement is timed, and the hold
 * field has taken its place. The ± pair is asserted through its effect, because a tag that draws but does not
 * step is the failure a presence check misses.
 */
@RunWith(AndroidJUnit4::class)
class TimedSetFieldsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(tags: SetFieldTags, durationText: String = "1:30") {
        composeTestRule.setContent {
            TimedFields(tags = tags, durationText = durationText)
        }
    }

    @Composable
    private fun TimedFields(tags: SetFieldTags, durationText: String) {
        var draft by remember {
            mutableStateOf(
                SetEntryDraft(
                    repsText = "5",
                    rpeText = "",
                    weightText = "",
                    durationText = durationText,
                    timed = true,
                ),
            )
        }
        SetEntryNumbers(
            draft = draft,
            onDraftChange = { draft = it },
            unit = WeightUnit.KILOGRAMS,
            tags = tags,
            timed = true,
        )
    }

    @Test
    fun theHold_replacesTheRepCount() {
        // The lie this feature removes: a timed set has no reps to state, so the field must not be offered.
        show(SetFieldTags.Logging)

        composeTestRule.onNodeWithTag(TestTags.SET_DURATION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SET_REPS_FIELD).assertDoesNotExist()
    }

    @Test
    fun theHold_readsBackWhatTheDraftSays() {
        show(SetFieldTags.Logging, durationText = "0:45")

        composeTestRule.onNodeWithTag(TestTags.SET_DURATION_FIELD).assertTextContains("0:45", substring = true)
    }

    @Test
    fun oneStep_movesTheHoldByFifteenSeconds() {
        // A plank is adjusted by a quarter minute, not by a rep: 1:30 + one tap is 1:45.
        show(SetFieldTags.Logging, durationText = "1:30")

        composeTestRule.onNodeWithTag(TestTags.SET_INCREASE_DURATION).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_DURATION_FIELD).assertTextContains("1:45", substring = true)

        composeTestRule.onNodeWithTag(TestTags.SET_DECREASE_DURATION).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_DURATION_FIELD).assertTextContains("1:30", substring = true)
    }

    @Test
    fun theCorrectionDialog_hasItsOwnHoldField() {
        // One tag addresses one control: the dialog restates a set that exists, so it names its own field and
        // its own ± pair (N59's rule, applied to this field).
        show(SetFieldTags.Editing)

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DURATION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_REPS_FIELD).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_INCREASE_DURATION).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DURATION_FIELD).assertTextContains("1:45", substring = true)

        // And back down, which is the half a hold spent past its target needs.
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DECREASE_DURATION).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_EDIT_DURATION_FIELD).assertTextContains("1:30", substring = true)
    }
}
