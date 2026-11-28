package com.example.androidapp.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** The two-date window Statistics and the block export share (N35, N332). */
@RunWith(AndroidJUnit4::class)
class CustomRangeDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        from: LocalDate?,
        to: LocalDate?,
        onConfirm: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                CustomRangeDialog(
                    title = "Custom",
                    fromLabel = "From",
                    toLabel = "To",
                    noDateLabel = "Not set",
                    applyLabel = "Apply",
                    fromTestTag = "from",
                    toTestTag = "to",
                    applyTestTag = "apply",
                    dialogTestTag = "dialog",
                    initialFrom = from,
                    initialTo = to,
                    onDismiss = {},
                    onConfirm = onConfirm,
                )
            }
        }
    }

    @Test
    fun aStartAfterTheEnd_isRefusedAndSaid() {
        show(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 1, 10))

        composeTestRule.onNodeWithTag("apply").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Choose a start on or before the end.").assertExists()
    }

    @Test
    fun anOrderedPair_applies() {
        var confirmed: Pair<LocalDate, LocalDate>? = null
        show(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 20)) { from, to -> confirmed = from to to }

        composeTestRule.onNodeWithTag("apply").assertIsEnabled()
        composeTestRule.onNodeWithTag("apply").performClick()

        assertThat(confirmed).isEqualTo(LocalDate.of(2026, 1, 10) to LocalDate.of(2026, 3, 20))
    }
}
