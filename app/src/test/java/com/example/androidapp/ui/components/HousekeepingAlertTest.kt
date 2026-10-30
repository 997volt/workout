package com.example.androidapp.ui.components

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.data.Housekeeping
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one-time notice a failed housekeeping pass raises (ROADMAP B324).
 *
 * It has to be loud enough to be seen and quiet enough not to accuse the lifter's data: the sentence says
 * the workouts are untouched, and the two actions are retry and later.
 */
@RunWith(AndroidJUnit4::class)
class HousekeepingAlertTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aFailedPass_isSaidOnce_withRetryAndLater() {
        var retried = 0
        var dismissed = 0
        composeTestRule.setContent {
            HousekeepingAlert(
                failure = Housekeeping.Failure(
                    at = 1L,
                    exceptionClass = "android.database.sqlite.SQLiteConstraintException",
                    frames = "android.database.sqlite.SQLiteConstraintException",
                ),
                retrying = false,
                onRetry = { retried++ },
                onDismiss = { dismissed++ },
            )
        }

        composeTestRule.onNodeWithTag(TestTags.HOUSEKEEPING_ALERT).assertExists()
        composeTestRule.onNodeWithText(text(R.string.housekeeping_failed_title)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.housekeeping_failed_text)).assertExists()

        composeTestRule.onNodeWithTag(TestTags.HOUSEKEEPING_RETRY).performClick()
        assertEquals(1, retried)

        composeTestRule.onNodeWithTag(TestTags.HOUSEKEEPING_LATER).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun nothingIsShown_whenThePassFinished() {
        composeTestRule.setContent {
            HousekeepingAlert(failure = null, retrying = false, onRetry = {}, onDismiss = {})
        }

        composeTestRule.onNodeWithTag(TestTags.HOUSEKEEPING_ALERT).assertDoesNotExist()
    }

    private fun text(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)
}
