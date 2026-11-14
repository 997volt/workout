package com.example.androidapp.ui.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The finish's find-the-weight prompt (ROADMAP N309). */
@RunWith(AndroidJUnit4::class)
class CalibrationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        notice: CalibrationNotice,
        onUse: (CalibrationNotice) -> Unit = {},
        onSkip: (CalibrationNotice) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CalibrationDialog(listOf(notice), onUse = onUse, onSkip = onSkip)
        }
    }

    @Test
    fun theLine_saysTheQuestionTheCountAndTheTop_andBothAnswersCallBack() {
        val notice = CalibrationNotice(
            templateExerciseId = "te1",
            exerciseName = "Bench Press",
            setCount = 3,
            topWeightGrams = 65_000L,
            topAssistanceGrams = 0L,
            weightUnit = WeightUnit.KILOGRAMS,
            targets = emptyList(),
        )
        var used: CalibrationNotice? = null
        var skipped: CalibrationNotice? = null
        show(notice, onUse = { used = it }, onSkip = { skipped = it })

        composeTestRule.onNodeWithTag(TestTags.CALIBRATION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Use today's weights for Bench Press?", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("3 sets, top 65 kg").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.CALIBRATION_USE).performClick()
        composeTestRule.onNodeWithTag(TestTags.CALIBRATION_SKIP).performClick()

        assertThat(used).isEqualTo(notice)
        assertThat(skipped).isEqualTo(notice)
    }
}
