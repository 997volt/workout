package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** B384: a message in a lazy item fills the page it is in, not just its text. */
@RunWith(AndroidJUnit4::class)
class CenteredMessageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aMessageInALazyItem_fillsTheViewport() {
        composeTestRule.setContent {
            AndroidAppTheme {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        CenteredMessage(
                            text = "Nothing here",
                            modifier = Modifier.testTag("centered-message"),
                        )
                    }
                }
            }
        }

        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val message = composeTestRule.onNodeWithTag("centered-message").getUnclippedBoundsInRoot()

        assertThat((message.bottom - message.top).value)
            .isWithin(1f)
            .of((root.bottom - root.top).value)
        assertThat((message.right - message.left).value).isAtLeast((root.right - root.left).value - 1f)
    }
}
