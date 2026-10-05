package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The plan dialog's summary line (ROADMAP B6).
 *
 * A planned set stores its RPE in half-points, and this dialog was the one place that
 * passed the count straight to the marker: a plan saying 9.5 rendered as **"RPE 19"** —
 * the exact confusion the halves representation exists to prevent. The formatting now
 * lives in one shared `rpeMarker`, and this is the test that would have caught it.
 */
@RunWith(AndroidJUnit4::class)
class TemplatePlanDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(set: TemplateSet, onAddWarmUpSets: (() -> Unit)? = null) {
        composeTestRule.setContent {
            TemplatePlanDialog(
                exerciseName = "Back Squat",
                sets = listOf(set),
                onAddSet = {},
                onEditSet = {},
                onDeleteSet = {},
                onDismiss = {},
                onAddWarmUpSets = onAddWarmUpSets,
            )
        }
    }

    @Test
    fun aHalfStepTargetRpe_readsAsALifterWritesIt() {
        show(plannedSet(targetRpeHalves = 19))

        composeTestRule.onNodeWithText("RPE 9.5", substring = true).assertIsDisplayed()
    }

    @Test
    fun aWholeTargetRpe_hasNoTrailingZero() {
        show(plannedSet(targetRpeHalves = 16))

        composeTestRule.onNodeWithText("RPE 8", substring = true).assertIsDisplayed()
    }

    private fun plannedSet(
        targetRpeHalves: Int? = null,
        targetWeightGrams: Long? = 140_000L,
        role: SetType = SetType.TOP_SET,
    ) = TemplateSet(
        id = "ts1",
        templateExerciseId = "te1",
        setIndex = 0,
        role = role,
        targetWeightGrams = targetWeightGrams,
        targetRepsMax = 2,
        targetRpeHalves = targetRpeHalves,
    )

    @Test
    fun aPlannedWarmUp_printsNoEffort_evenThoughTheRowStillCarriesOne() {
        // ROADMAP N67: a warm-up records no effort, and a plan written before that rule can still
        // carry a legacy per-set target. Printing it beside *Warm-up* read as a number the ramp had
        // been judged against, so the line leaves it off; the editor is what clears the stored value
        // on the next write.
        show(plannedSet(targetRpeHalves = 17, role = SetType.WARMUP))

        composeTestRule.onNodeWithText("RPE 8.5", substring = true).assertDoesNotExist()
    }

    @Test
    fun theWarmUpRamp_isOffered_whenTheCallerSaysThereIsAWeight() {
        // ROADMAP N28. The dialog does not decide whether a ramp makes sense — the screen does, since
        // it knows the plan — so this holds the one thing the dialog owns: it draws the control when
        // it is given one.
        show(plannedSet(targetWeightGrams = 100_000L), onAddWarmUpSets = {})

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_ADD_WARMUPS).assertIsDisplayed()
    }

    @Test
    fun theWarmUpRamp_isNotOffered_whenThereIsNoWeightToRampFrom() {
        // A bodyweight exercise gets no control rather than one that would write nothing.
        show(plannedSet(targetWeightGrams = null), onAddWarmUpSets = null)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_ADD_WARMUPS).assertDoesNotExist()
    }
}
