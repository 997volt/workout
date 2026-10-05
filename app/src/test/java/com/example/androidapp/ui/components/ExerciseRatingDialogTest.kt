package com.example.androidapp.ui.components

import com.example.androidapp.R
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.jointSiteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How an exercise felt — muscle feel and the picked joints (ROADMAP N8, N63).
 *
 * Both halves are steppers, so what matters here is what each records: muscle feel starts at 7, so
 * saving without touching it is still an answer, the buttons stop at the ends of the scale, dismissing
 * the prompt is not a write — and the joint half is picked from the body's joints with left and right
 * apart and a score of its own.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseRatingDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saved: Pair<Int?, List<JointPain>>? = null
    private var saveCalled = false
    private var dismissed = false

    private fun show(
        initialFeel: Int? = null,
        initialJoints: List<JointPain> = emptyList(),
    ) {
        composeTestRule.setContent {
            ExerciseRatingDialog(
                initialMuscleFeel = initialFeel,
                initialJoints = initialJoints,
                onDismiss = { dismissed = true },
                onSave = { feel, joints ->
                    saved = feel to joints
                    saveCalled = true
                },
            )
        }
    }

    /** Adds one joint the way a thumb does: open the picker, tap the site. */
    private fun pick(joint: Joint, side: Side) {
        composeTestRule.onNodeWithTag(TestTags.Rating.JOINT_ADD).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(joint, side))).performClick()
    }

    @Test
    fun savingWithoutTouchingTheStepper_recordsItsStartingPoint() {
        // The stepper always shows a number, so Save carries it: 7 is the value the dialog opens on.
        show()

        composeTestRule.onNodeWithTag(TestTags.RATING_SAVE).performClick()

        assertTrue(saveCalled)
        assertEquals(7 to emptyList<JointPain>(), saved)
    }

    @Test
    fun steppingTheMuscleFeel_isReportedOnSave() {
        show()

        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_INCREASE).performClick()
        composeTestRule.onNodeWithTag(TestTags.RATING_SAVE).performClick()

        assertEquals(8 to emptyList<JointPain>(), saved)
    }

    @Test
    fun theMuscleStepper_stopsAtTheEndsOfTheScale() {
        // A stepper cannot leave the scale, so there is no value to refuse: the end's button is the one
        // that is disabled, the shape the scored picks already use.
        show(initialFeel = 1)

        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_DECREASE).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_INCREASE).assertIsEnabled()
    }

    @Test
    fun existingRatings_arePrefilled() {
        show(
            initialFeel = 8,
            initialJoints = listOf(JointPain(Joint.KNEE, Side.LEFT, 4)),
        )

        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_FIELD).assertTextEquals("8/10")
        val site = jointSiteKey(Joint.KNEE, Side.LEFT)
        composeTestRule.onNodeWithTag(TestTags.Rating.jointRow(site)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointScore(site)).assertTextEquals("4/10")
    }

    @Test
    fun theMuscleScale_saysWhatItsEndsMean() {
        // ROADMAP N12: the number is chosen here, so the meaning of 1 and 10 has to
        // be here — that is what keeps a 7 this month comparable to a 7 next month.
        show()

        composeTestRule.onNodeWithText(muscleAnchors()).assertExists()
    }

    /** Read from resources, so a reworded anchor does not break the test. */
    private fun muscleAnchors(): String =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.rating_muscle_anchors)

    @Test
    fun pickingAJoint_addsIt_atTheBottomOfTheScale() {
        show()

        pick(Joint.SHOULDER, Side.LEFT)

        val site = jointSiteKey(Joint.SHOULDER, Side.LEFT)
        composeTestRule.onNodeWithTag(TestTags.Rating.jointRow(site)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointScore(site)).assertTextEquals("1/10")
    }

    @Test
    fun leftAndRight_areSeparateEntries() {
        // ROADMAP N63: "knee 6" is half a sentence, so the two sides are two picks.
        show(initialJoints = listOf(JointPain(Joint.KNEE, Side.LEFT, 6)))

        composeTestRule.onNodeWithTag(TestTags.Rating.JOINT_ADD).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(Joint.KNEE, Side.LEFT)))
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(Joint.KNEE, Side.RIGHT)))
            .assertExists()
    }

    @Test
    fun aCentralJoint_isOfferedOnlyAsCentre() {
        // NECK and LOWER_BACK have no left and right (ROADMAP N63).
        show()

        composeTestRule.onNodeWithTag(TestTags.Rating.JOINT_ADD).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(Joint.NECK, Side.CENTRE)))
            .assertExists()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(Joint.NECK, Side.LEFT)))
            .assertDoesNotExist()
    }

    @Test
    fun raisingAndLoweringAScore_movesItOnePointAtATime() {
        show(initialJoints = listOf(JointPain(Joint.ANKLE, Side.RIGHT, 8)))

        val site = jointSiteKey(Joint.ANKLE, Side.RIGHT)
        composeTestRule.onNodeWithTag(TestTags.Rating.jointIncrease(site)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointScore(site)).assertTextEquals("9/10")

        composeTestRule.onNodeWithTag(TestTags.Rating.jointDecrease(site)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointDecrease(site)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Rating.jointScore(site)).assertTextEquals("7/10")
    }

    @Test
    fun theScoreStopsAtTheEndsOfTheScale() {
        show(
            initialJoints = listOf(
                JointPain(Joint.WRIST, Side.LEFT, 1),
                JointPain(Joint.HIP, Side.RIGHT, 10),
            ),
        )

        val lowest = jointSiteKey(Joint.WRIST, Side.LEFT)
        val highest = jointSiteKey(Joint.HIP, Side.RIGHT)
        composeTestRule.onNodeWithTag(TestTags.Rating.jointDecrease(lowest)).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointIncrease(lowest)).assertIsEnabled()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointIncrease(highest)).assertIsNotEnabled()
    }

    @Test
    fun removingAJoint_takesItOffTheList() {
        show(
            initialJoints = listOf(
                JointPain(Joint.KNEE, Side.LEFT, 6),
                JointPain(Joint.ELBOW, Side.RIGHT, 3),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Rating.jointRemove(jointSiteKey(Joint.KNEE, Side.LEFT)))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Rating.jointRow(jointSiteKey(Joint.KNEE, Side.LEFT)))
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointRow(jointSiteKey(Joint.ELBOW, Side.RIGHT)))
            .assertExists()
    }

    @Test
    fun saving_reportsTheMuscleFeelAndTheWholeList() {
        show(
            initialFeel = 8,
            initialJoints = listOf(JointPain(Joint.KNEE, Side.LEFT, 6)),
        )

        pick(Joint.SHOULDER, Side.RIGHT)
        composeTestRule.onNodeWithTag(TestTags.RATING_SAVE).performClick()

        assertEquals(
            8 to listOf(
                JointPain(Joint.KNEE, Side.LEFT, 6),
                JointPain(Joint.SHOULDER, Side.RIGHT, 1),
            ),
            saved,
        )
    }

    @Test
    fun dismissing_writesNothing() {
        show()

        composeTestRule.onNodeWithTag(TestTags.RATING_DISMISS).performClick()

        assertTrue(dismissed)
        assertFalse(saveCalled)
    }
}
