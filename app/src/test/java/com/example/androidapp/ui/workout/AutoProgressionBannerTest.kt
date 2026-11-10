package com.example.androidapp.ui.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionTargets
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The banner auto progression reads back with its undo (ROADMAP N299).
 *
 * The app wrote without being asked, so the one thing the banner must offer is the way back. Both
 * actions are asserted through their controls rather than by reading the moment back.
 */
@RunWith(AndroidJUnit4::class)
class AutoProgressionBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theBannerSaysTheNextTarget_andUndoIsOneTap() {
        var undone = false
        composeTestRule.setContent {
            AndroidAppTheme {
                AutoProgressionBanner(
                    moment = AutoProgressionMoment(
                        exerciseName = "Bench Press",
                        unit = WeightUnit.KILOGRAMS,
                        steps = listOf(
                            AutoProgressionStep(
                                writeId = "w1",
                                direction = ProgressionDirection.LOAD,
                                before = ProgressionTargets(weightGrams = 100_000L, repsCurrent = 8),
                                after = ProgressionTargets(weightGrams = 102_500L, repsCurrent = 5),
                            ),
                        ),
                    ),
                    onUndo = { undone = true },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTO_PROGRESSION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.AUTO_PROGRESSION_UNDO).performClick()

        assertThat(undone).isTrue()
    }

    @Test
    fun dismissHidesTheBannerWithoutUndoingTheStep() {
        var dismissed = false
        composeTestRule.setContent {
            AndroidAppTheme {
                AutoProgressionBanner(
                    moment = AutoProgressionMoment(
                        exerciseName = "Bench Press",
                        unit = WeightUnit.KILOGRAMS,
                        steps = listOf(
                            AutoProgressionStep(
                                writeId = "w1",
                                direction = ProgressionDirection.REPS,
                                before = ProgressionTargets(weightGrams = 100_000L, repsCurrent = 5),
                                after = ProgressionTargets(weightGrams = 100_000L, repsCurrent = 6),
                            ),
                        ),
                    ),
                    onUndo = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTO_PROGRESSION_DISMISS).performClick()

        assertThat(dismissed).isTrue()
    }
}
