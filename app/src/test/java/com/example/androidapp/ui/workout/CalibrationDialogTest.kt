package com.example.androidapp.ui.workout

import androidx.compose.ui.test.assertContentDescriptionEquals
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
        errorNotice: CalibrationNotice? = null,
        onClose: (CalibrationNotice) -> Unit = {},
    ) = showAll(listOf(notice), onUse, onSkip, errorNotice, onClose)

    private fun showAll(
        notices: List<CalibrationNotice>,
        onUse: (CalibrationNotice) -> Unit = {},
        onSkip: (CalibrationNotice) -> Unit = {},
        errorNotice: CalibrationNotice? = null,
        onClose: (CalibrationNotice) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CalibrationDialog(
                notices,
                onUse = onUse,
                onSkip = onSkip,
                errorNotice = errorNotice,
                onClose = onClose,
            )
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

        composeTestRule.onNodeWithTag(TestTags.calibrationUse(notice.templateExerciseId)).performClick()
        composeTestRule.onNodeWithTag(TestTags.calibrationSkip(notice.templateExerciseId)).performClick()

        assertThat(used).isEqualTo(notice)
        assertThat(skipped).isEqualTo(notice)
    }

    @Test
    fun eachMarkedPlan_hasItsOwnNamedAnswers() {
        // B447: one line per marked plan; the second line's answers must be addressable and named.
        val bench = CalibrationNotice(
            templateExerciseId = "te1",
            exerciseName = "Bench Press",
            setCount = 3,
            topWeightGrams = 65_000L,
            topAssistanceGrams = 0L,
            weightUnit = WeightUnit.KILOGRAMS,
            targets = emptyList(),
        )
        val squat = bench.copy(templateExerciseId = "te2", exerciseName = "Back Squat", topWeightGrams = 110_000L)
        var used: CalibrationNotice? = null
        var skipped: CalibrationNotice? = null
        showAll(listOf(bench, squat), onUse = { used = it }, onSkip = { skipped = it })

        composeTestRule.onNodeWithTag(TestTags.calibrationUse("te2"))
            .assertContentDescriptionEquals("Use today's weights for Back Squat")
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.calibrationSkip("te2"))
            .assertContentDescriptionEquals("Not now for Back Squat")
            .performClick()

        assertThat(used).isEqualTo(squat)
        assertThat(skipped).isEqualTo(squat)
    }

    @Test
    fun aBodyweightPlan_saysOnlyTheCount_noTop() {
        // B450: a bodyweight or hold plan has no load to be "top" of, so the prompt must not claim 0 kg.
        val notice = CalibrationNotice(
            templateExerciseId = "te1",
            exerciseName = "Plank",
            setCount = 3,
            topWeightGrams = 0L,
            topAssistanceGrams = 0L,
            weightUnit = WeightUnit.KILOGRAMS,
            targets = emptyList(),
        )
        show(notice)

        composeTestRule.onNodeWithText("3 sets").assertExists()
        composeTestRule.onNodeWithText("top", substring = true).assertDoesNotExist()
    }

    @Test
    fun aFailedWrite_statesIt_andCloseMovesOnWithoutWriting() {
        // B422: the dialog used to have no exit when a write failed, so a stored workout could not close.
        val notice = CalibrationNotice(
            templateExerciseId = "te1",
            exerciseName = "Bench Press",
            setCount = 3,
            topWeightGrams = 65_000L,
            topAssistanceGrams = 0L,
            weightUnit = WeightUnit.KILOGRAMS,
            targets = emptyList(),
        )
        var closed: CalibrationNotice? = null
        show(notice, errorNotice = notice, onClose = { closed = it })

        composeTestRule.onNodeWithText("The plan could not be saved", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.calibrationClose(notice.templateExerciseId)).performClick()

        assertThat(closed).isEqualTo(notice)
    }
}
