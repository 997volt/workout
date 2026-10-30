package com.example.androidapp.ui.settings

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.platform.CrashLog
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The screen that makes a crash reportable (ROADMAP N244).
 *
 * It has to show both records, offer the retry only when there is a failed pass, and hand the whole thing
 * over as text — the copy is what the person fixing the app actually receives.
 */
@RunWith(AndroidJUnit4::class)
class DiagnosticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var copied: String? = null
    private var retried = 0

    private fun show(crash: CrashLog?, repair: Housekeeping.Failure?) {
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(
                    appVersion = "2.19.1",
                    versionCode = 40,
                    crash = crash,
                    repair = repair,
                ),
                onBack = {},
                onCopy = { copied = it },
                onRetry = { retried++ },
            )
        }
    }

    @Test
    fun aCrashAndAFailedPass_areShown_andCopied_andRetried() {
        show(
            crash = CrashLog(
                timestamp = 1L,
                exceptionClass = "android.database.sqlite.SQLiteConstraintException",
                stackTrace = "android.database.sqlite.SQLiteConstraintException\n\tat a.b(C.kt:1)",
                appVersion = "2.19",
                versionCode = 39,
                androidVersion = "16",
                deviceModel = "Pixel 6",
            ),
            repair = Housekeeping.Failure(
                at = 2L,
                exceptionClass = "android.database.sqlite.SQLiteConstraintException",
                frames = "android.database.sqlite.SQLiteConstraintException\n\tat a.b(C.kt:2)",
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_SCREEN).assertExists()
        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_CRASH).assertExists()
        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_REPAIR).assertExists()
        composeTestRule.onNodeWithText(
            text(R.string.diagnostics_version, "2.19.1", 40),
        ).assertExists()

        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_COPY).performScrollTo().performClick()
        val report = copied
        assertNotNull("copy details hands over the record", report)
        assertTrue(report!!.contains("SQLiteConstraintException"))
        assertTrue("the crash's frames travel", report.contains("C.kt:1"))
        assertTrue("and the failed pass's", report.contains("C.kt:2"))

        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_RETRY).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun noRecords_saysSo_andOffersNoRetry() {
        show(crash = null, repair = null)

        composeTestRule.onNodeWithText(text(R.string.diagnostics_crash_none)).assertExists()
        composeTestRule.onNodeWithText(text(R.string.diagnostics_repair_none)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_RETRY).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.DIAGNOSTICS_COPY).performScrollTo().performClick()
        assertTrue(copied!!.contains("No crash recorded."))
    }

    private fun text(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)
}
