package com.example.androidapp.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TrainingDay
import com.example.androidapp.domain.model.TrainingExercise
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.model.TrainingSession
import com.example.androidapp.domain.model.TrainingSet
import com.example.androidapp.domain.repository.TrainingBlockChoice
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** The block sheet: everything named, the warning said, and the two actions reported (N331). */
@RunWith(AndroidJUnit4::class)
class TrainingReportSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var downloaded = 0
    private var shared = 0
    private var chosen: String? = null
    private var weeks = 0

    private fun report() = TrainingReport(
        blockName = "Upper/Lower",
        blockStart = LocalDate.of(2026, 9, 4),
        blockEnd = LocalDate.of(2026, 10, 16),
        blockComplete = true,
        exportedAt = Instant.parse("2026-10-17T10:00:00Z"),
        unit = WeightUnit.KILOGRAMS,
        days = listOf(TrainingDay("Push", 1, "s1")),
        sessions = listOf(
            TrainingSession(
                id = "s1",
                workout = "Push",
                dayName = "Push",
                startedAt = Instant.parse("2026-10-01T08:00:00Z"),
                finishedAt = Instant.parse("2026-10-01T09:00:00Z"),
                exercises = listOf(
                    TrainingExercise(
                        name = "Bench Press",
                        sets = listOf(
                            TrainingSet(
                                setIndex = 0,
                                performed = true,
                                role = SetType.NORMAL,
                                unit = WeightUnit.KILOGRAMS,
                                reps = 5,
                                weightGrams = 100_000L,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun show(state: TrainingReportUiState) {
        composeTestRule.setContent {
            AndroidAppTheme {
                TrainingReportSheet(
                    state = state,
                    onDownload = { downloaded++ },
                    onShare = { shared++ },
                    onChoose = { chosen = it },
                    onLastWeeks = { weeks++ },
                    onClose = {},
                )
            }
        }
    }

    @Test
    fun theBlockAndItsIncludesAreNamed_andTheActionsReport() {
        show(
            TrainingReportUiState(
                isOpen = true,
                report = report(),
                blocks = listOf(
                    TrainingBlockChoice("p1", "Upper/Lower", LocalDate.of(2026, 9, 4)),
                ),
                outcome = TrainingReportOutcome.WRITTEN,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HISTORY_BLOCK_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithText("soreness", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Everything in the block").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_BLOCK_OUTCOME).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HISTORY_BLOCK_DOWNLOAD).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_BLOCK_SHARE).performClick()

        assertEquals(1, downloaded)
        assertEquals(1, shared)
    }

    @Test
    fun change_listsTheBlocksAndTheFallback() {
        show(
            TrainingReportUiState(
                isOpen = true,
                report = report(),
                blocks = listOf(TrainingBlockChoice("p1", "Upper/Lower", LocalDate.of(2026, 9, 4))),
            ),
        )

        composeTestRule.onNodeWithText("Change").performClick()
        composeTestRule.onNodeWithText("Last 8 weeks").performClick()

        assertEquals(1, weeks)
    }
}
