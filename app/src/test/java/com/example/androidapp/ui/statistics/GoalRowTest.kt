package com.example.androidapp.ui.statistics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** The weight goal row's editor keeps what was typed until the write succeeds (B234). */
@RunWith(AndroidJUnit4::class)
class GoalRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 2)

    @Test
    fun aFailedSave_keepsTheEditor_andASuccessClosesIt() {
        var outcome by mutableStateOf<Boolean?>(null)
        composeTestRule.setContent {
            AndroidAppTheme {
                WeightGoalRow(
                    goal = null,
                    metric = MetricRegistry.entryFor(MetricKey.Body(BodyMetric.WEIGHT)),
                    history = emptyList(),
                    today = today,
                    onSavePeriod = { _, _, _, _ -> },
                    saveOutcome = outcome,
                    onDismissSave = { outcome = null },
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        // Nothing is closed until the write answers.
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_DIALOG).assertExists()

        composeTestRule.runOnIdle { outcome = false }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_DIALOG).assertExists()

        composeTestRule.runOnIdle { outcome = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_DIALOG).assertDoesNotExist()
    }

    @Test
    fun theCardEditor_isCalledWeightGoal() {
        // B230: the title travels with the editor, not from a null id.
        composeTestRule.setContent {
            AndroidAppTheme {
                WeightGoalRow(
                    goal = null,
                    metric = MetricRegistry.entryFor(MetricKey.Body(BodyMetric.WEIGHT)),
                    history = emptyList(),
                    today = today,
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performClick()

        // The row's own eyebrow says "Weight goal" too; the dialog adds the second one.
        composeTestRule.onAllNodesWithText("Weight goal").assertCountEquals(2)
        composeTestRule.onNodeWithText("Add a past goal").assertDoesNotExist()
    }
}
