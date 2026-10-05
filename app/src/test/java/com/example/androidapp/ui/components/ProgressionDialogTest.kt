package com.example.androidapp.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionOffer
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.ProgressionSource
import com.example.androidapp.domain.model.ProgressionStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The next step a plan earned, as the lifter's decision (ROADMAP N50).
 *
 * What matters here is that the prompt states both halves — what the plan asked and what was done —
 * that each control reports the direction it names rather than one of them, that declining writes
 * nothing, and that the rating the prompt carries is still one tap away. A prompt that offered only
 * the load, or that finished the exercise on a dismiss, would be the app deciding instead of asking.
 */
@RunWith(AndroidJUnit4::class)
class ProgressionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var accepted: ProgressionDirection? = null
    private var notNow = false

    private fun show(prompt: ProgressionPrompt) {
        composeTestRule.setContent {
            ProgressionDialog(
                exerciseName = "Back Squat",
                prompt = prompt,
                onAccept = { accepted = it },
                onNotNow = { notNow = true },
            )
        }
    }

    @Test
    fun thePrompt_statesWhatThePlanAsked_andWhatWasDone() {
        show(prompt())

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN)
            .assertIsDisplayed()
            .assertTextContains(plural(R.plurals.progression_reps_value, 5), substring = true)
            .assertTextContains(text(R.string.progression_weight_value, "100"), substring = true)
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_DONE)
            .assertIsDisplayed()
            .assertTextContains(text(R.string.progression_weight_value, "100"), substring = true)
    }

    @Test
    fun theLoadDirection_reportsItself_andNamesTheWeightItWouldSet() {
        show(prompt())

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_LOAD)
            .assertIsDisplayed()
            .assertTextContains(text(R.string.progression_increase_load, "102.5"))
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_LOAD).performClick()

        assertEquals(ProgressionDirection.LOAD, accepted)
        assertTrue("accepting is not declining", !notNow)
    }

    @Test
    fun theRepDirection_reportsItself_andNamesTheRepsItWouldSet() {
        show(prompt())

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_REPS)
            .assertTextContains(plural(R.plurals.progression_increase_reps, 6))
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_REPS).performClick()

        assertEquals(ProgressionDirection.REPS, accepted)
    }

    @Test
    fun declining_isReported_andAcceptsNothing() {
        show(prompt())

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_NOT_NOW).assertIsDisplayed().performClick()

        assertTrue(notNow)
        assertNull("doing neither writes no direction", accepted)
    }

    @Test
    fun thePrompt_leavesTheRatingToTheExercise() {
        // N8: the rating belongs to the exercise's own row, opened when the lifter reaches for it, so
        // leaving the exercise never asks for one.
        show(prompt())

        composeTestRule.onNodeWithText(text(R.string.rating_edit_title)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_NOT_NOW).assertIsDisplayed()
    }

    @Test
    fun withNothingEarned_thePrompt_stillStatesThePlan_andOffersNoStep() {
        show(prompt(offer = null))

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_LOAD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_REPS).assertDoesNotExist()
        // Declining is still there, which is what keeps Done from being a dead end.
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_NOT_NOW).assertIsDisplayed()
    }

    @Test
    fun anExerciseWithNoPlan_saysSo_ratherThanInventingOne() {
        show(ProgressionPrompt())

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN)
            .assertTextContains(text(R.string.progression_no_plan))
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_DONE).assertDoesNotExist()
    }

    @Test
    fun aPlanWithNoLoadToRaise_offersOnlyTheRep() {
        show(prompt(offer = offer(load = null)))

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_LOAD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_REPS).assertIsDisplayed()
    }

    private fun text(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun plural(id: Int, quantity: Int): String =
        ApplicationProvider.getApplicationContext<Context>().resources
            .getQuantityString(id, quantity, quantity)

    private fun prompt(offer: ProgressionOffer? = offer()) = ProgressionPrompt(
        planned = planSet(),
        performed = ProgressionPerformance(reps = 5, weightGrams = 100_000L, rpeHalves = 7),
        offer = offer,
    )

    private fun planSet() = ProgressionPlanSet(
        setId = "ts-0",
        setIndex = 0,
        source = ProgressionSource.TEMPLATE,
        targetWeightGrams = 100_000L,
        targetRepsMax = 5,
        targetRpeHalves = 8,
    )

    private fun offer(load: ProgressionStep<Long>? = ProgressionStep(100_000L, 102_500L)) = ProgressionOffer(
        set = planSet(),
        reps = ProgressionStep(5, 6),
        load = load,
    )
}
