package com.example.androidapp.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionMiss
import com.example.androidapp.domain.model.ProgressionOffer
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionSetPrompt
import com.example.androidapp.domain.model.ProgressionStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The next step each planned set earned, as the lifter's decision (ROADMAP N50, N74).
 *
 * What matters here is that the question states one row per working set — the plan, what was done,
 * and the step that set earned or why it earned none — that a pick reports the set and the direction
 * rather than a bare "increase", that the directions a set offers are the only ones on it, and that
 * declining writes nothing. Picking a step must not itself confirm anything: that is the whole point
 * of N74's one *Done*.
 */
@RunWith(AndroidJUnit4::class)
class ProgressionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var toggled: Pair<String, ProgressionDirection>? = null
    private var selectedAll: ProgressionDirection? = null
    private var confirmed = false
    private var notNow = false

    private fun show(question: PendingProgression) {
        composeTestRule.setContent {
            ProgressionDialog(
                progression = question,
                onToggle = { setId, direction -> toggled = setId to direction },
                onSelectAll = { selectedAll = it },
                onConfirm = { confirmed = true },
                onNotNow = { notNow = true },
            )
        }
    }

    @Test
    fun everyWorkingSet_getsItsOwnRow() {
        show(question(row("ts-0"), row("ts-1")))

        composeTestRule.onNodeWithTag(TestTags.Progression.set("ts-0")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Progression.set("ts-1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Progression.plan("ts-0"))
            .assertTextContains(plural(R.plurals.progression_reps_value, 5), substring = true)
            .assertTextContains(text(R.string.progression_weight_value, "100", "kg"), substring = true)
        composeTestRule.onNodeWithTag(TestTags.Progression.done("ts-1"))
            .assertTextContains(text(R.string.progression_weight_value, "100", "kg"), substring = true)
    }

    @Test
    fun belowTheCeiling_theRepIsOffered_andTheWeightIsNot() {
        // The measure of N74: a set with reps left in its range has no weight step to take.
        show(question(row("ts-0")))

        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0"))
            .assertIsDisplayed()
            .assertTextContains(plural(R.plurals.progression_choose_reps, 6))
        composeTestRule.onNodeWithTag(TestTags.Progression.load("ts-0")).assertDoesNotExist()
    }

    @Test
    fun atTheCeiling_theWeightIsOffered_andNamesTheRestart() {
        // Asked 5 to 8, climbed to 8: the label states both halves of the write — the load and the
        // reps it puts back on the floor — rather than hiding the second behind the first.
        val climbed = planSet("ts-0").copy(targetRepsCurrent = 8)
        val offer = ProgressionOffer(set = climbed, load = ProgressionStep(100_000L, 102_500L))
        show(question(row("ts-0", planned = climbed, offer = offer, performed = done(reps = 8))))

        composeTestRule.onNodeWithTag(TestTags.Progression.load("ts-0"))
            .assertIsDisplayed()
            .assertTextContains(pluralLabel(R.plurals.progression_choose_load_restart, 5, "102.5", "kg", 5))
        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0")).assertDoesNotExist()
    }

    @Test
    fun aSetThatEarnedNothing_statesWhy_andOffersNoStep() {
        show(
            question(
                row(
                    "ts-0",
                    performed = done(reps = 3),
                    offer = null,
                    miss = ProgressionMiss.REPS_SHORT,
                ),
            ),
        )

        composeTestRule.onNodeWithText(plural(R.plurals.progression_miss_reps_short, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Progression.load("ts-0")).assertDoesNotExist()
    }

    @Test
    fun aToppedOutSet_saysSoRatherThanOfferingARepPastTheRange() {
        show(question(row("ts-0", offer = null, miss = ProgressionMiss.TOPPED_OUT)))

        composeTestRule.onNodeWithText(plural(R.plurals.progression_miss_topped_out, 8))
            .assertIsDisplayed()
    }

    @Test
    fun pickingAStep_reportsTheSetAndTheDirection() {
        show(question(row("ts-0")))

        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0")).performClick()

        assertEquals("ts-0" to ProgressionDirection.REPS, toggled)
        assertFalse("a pick is not a write", confirmed)
    }

    @Test
    fun aPickedStep_isAnnouncedAsSelected() {
        // The chip's selected state is what says which step is picked, so a screen reader hears the
        // answer rather than two identical buttons.
        show(question(row("ts-0", direction = ProgressionDirection.REPS)))

        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0")).assertIsSelected()
    }

    @Test
    fun theBulkPicks_appearOnlyWhereTwoSetsWouldTakeTheSameTap() {
        show(question(row("ts-0"), row("ts-1")))

        composeTestRule.onNodeWithTag(TestTags.Progression.REPS_ALL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Progression.LOAD_ALL).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Progression.REPS_ALL).performClick()

        assertEquals(ProgressionDirection.REPS, selectedAll)
    }

    @Test
    fun aSingleOfferingSet_hasNoBulkPick() {
        show(question(row("ts-0")))

        composeTestRule.onNodeWithTag(TestTags.Progression.REPS_ALL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Progression.LOAD_ALL).assertDoesNotExist()
    }

    @Test
    fun aWrittenStep_isStated_andLosesItsControls() {
        val accepted = planSet("ts-0").copy(targetRepsCurrent = 6)
        show(question(row("ts-0", planned = accepted, offer = null, applied = true)))

        composeTestRule.onNodeWithText(text(R.string.progression_applied)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Progression.reps("ts-0")).assertDoesNotExist()
    }

    @Test
    fun confirming_isReported_asWritingEveryPick() {
        show(question(row("ts-0"), row("ts-1", direction = ProgressionDirection.REPS)))

        composeTestRule.onNodeWithTag(TestTags.Progression.CONFIRM).assertIsDisplayed().performClick()

        assertTrue(confirmed)
        assertFalse("accepting is not declining", notNow)
    }

    @Test
    fun declining_isReported_andConfirmsNothing() {
        show(question(row("ts-0", direction = ProgressionDirection.REPS)))

        composeTestRule.onNodeWithTag(TestTags.Progression.NOT_NOW).assertIsDisplayed().performClick()

        assertTrue(notNow)
        assertFalse("doing neither writes nothing", confirmed)
        assertNull(toggled)
    }

    @Test
    fun theQuestion_leavesTheRatingToTheExercise() {
        // N8: the rating belongs to the exercise's own row, opened when the lifter reaches for it, so
        // leaving the exercise never asks for one.
        show(question(row("ts-0")))

        composeTestRule.onNodeWithText(text(R.string.rating_edit_title)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Progression.NOT_NOW).assertIsDisplayed()
    }

    private fun question(vararg sets: PendingProgression.PendingSet) = PendingProgression(
        sessionExerciseId = "se1",
        exerciseName = "Back Squat",
        unit = WeightUnit.KILOGRAMS,
        sets = sets.toList(),
    )

    private fun row(
        setId: String,
        planned: ProgressionPlanSet = planSet(setId),
        performed: ProgressionPerformance? = done(),
        offer: ProgressionOffer? = ProgressionOffer(set = planSet(setId), reps = ProgressionStep(5, 6)),
        miss: ProgressionMiss? = null,
        direction: ProgressionDirection? = null,
        applied: Boolean = false,
    ) = PendingProgression.PendingSet(
        prompt = ProgressionSetPrompt(planned = planned, performed = performed, offer = offer, miss = miss),
        direction = direction,
        applied = applied,
    )

    private fun planSet(setId: String) = ProgressionPlanSet(
        setId = setId,
        setIndex = 0,
        targetWeightGrams = 100_000L,
        targetRepsMin = 5,
        targetRepsMax = 8,
        targetRpeHalves = 8,
    )

    private fun done(reps: Int = 5) = ProgressionPerformance(reps = reps, weightGrams = 100_000L, rpeHalves = 7)

    private fun text(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun plural(id: Int, quantity: Int): String =
        ApplicationProvider.getApplicationContext<Context>().resources
            .getQuantityString(id, quantity, quantity)

    /** A plural whose message takes more than the count, in format order. */
    private fun pluralLabel(id: Int, quantity: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().resources
            .getQuantityString(id, quantity, *args)
}
