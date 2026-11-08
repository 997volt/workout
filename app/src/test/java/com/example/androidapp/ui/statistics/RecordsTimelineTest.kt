package com.example.androidapp.ui.statistics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.RecordEvent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * The records timeline as the Statistics page draws it (ROADMAP N290).
 *
 * Addressed by [RecordTimelineTags] rather than by the section's words, so the assertion is about rows,
 * months and order — the parts that can silently break — rather than about English.
 */
@RunWith(AndroidJUnit4::class)
class RecordsTimelineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val octoberFourth = RecordEvent.Load(
        exerciseName = "Back squat",
        performedAt = Instant.parse("2026-10-04T10:00:00Z"),
        zoneOffsetMinutes = 0,
        weightUnit = null,
        weightGrams = 100_000L,
        reps = 5,
    )

    private val octoberFirst = RecordEvent.Load(
        exerciseName = "Deadlift",
        performedAt = Instant.parse("2026-10-01T10:00:00Z"),
        zoneOffsetMinutes = 0,
        weightUnit = null,
        weightGrams = 140_000L,
        reps = 3,
    )

    private val septemberHold = RecordEvent.Hold(
        exerciseName = "Farmer carry",
        performedAt = Instant.parse("2026-09-28T10:00:00Z"),
        zoneOffsetMinutes = 0,
        seconds = 45,
    )

    @Test
    fun theTimelineGroupsRecordsByMonth_newestFirst() {
        show(listOf(septemberHold, octoberFirst, octoberFourth))

        composeTestRule.onNodeWithTag(RecordTimelineTags.TIMELINE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.month(0)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.month(1)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.row(0, 0)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.row(0, 1)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.row(1, 0)).assertIsDisplayed()

        // The newest month is above the older one, and inside October the 4th is above the 1st.
        val newestMonth = composeTestRule.onNodeWithTag(RecordTimelineTags.month(0))
            .getUnclippedBoundsInRoot().top
        val olderMonth = composeTestRule.onNodeWithTag(RecordTimelineTags.month(1))
            .getUnclippedBoundsInRoot().top
        assertWithMessage("October is drawn above September")
            .that(newestMonth).isLessThan(olderMonth)

        val newerRow = composeTestRule.onNodeWithTag(RecordTimelineTags.row(0, 0))
            .getUnclippedBoundsInRoot().top
        val olderRow = composeTestRule.onNodeWithTag(RecordTimelineTags.row(0, 1))
            .getUnclippedBoundsInRoot().top
        assertWithMessage("the 4 October record is drawn above the 1 October one")
            .that(newerRow).isLessThan(olderRow)
    }

    @Test
    fun withNoRecords_theSectionExplainsWhatWouldAppear() {
        show(emptyList())

        composeTestRule.onNodeWithTag(RecordTimelineTags.TIMELINE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RecordTimelineTags.row(0, 0)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(RecordTimelineTags.month(0)).assertDoesNotExist()
    }

    private fun show(records: List<RecordEvent>) {
        composeTestRule.setContent {
            AndroidAppTheme {
                RecordsTimeline(records = records)
            }
        }
    }
}
