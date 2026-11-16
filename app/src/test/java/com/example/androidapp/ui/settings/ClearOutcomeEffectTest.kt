package com.example.androidapp.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.transfer.ClearOutcome
import com.example.androidapp.ui.transfer.DataClearStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The clear's one-shot answer, and the message a recreated composition keeps (B456, N323). */
@RunWith(AndroidJUnit4::class)
class ClearOutcomeEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aPublishedOutcome_isSaidOnce_andAcknowledged() {
        val store = DataClearStore().apply { publish(ClearOutcome.Cleared) }
        var said = 0

        composeTestRule.setContent {
            val outcome by store.outcome.collectAsState()
            ClearOutcomeEffect(
                outcome = outcome,
                onClear = { said++ },
                onFail = {},
                onAcknowledge = store::forget,
            )
        }
        composeTestRule.waitForIdle()

        assertThat(said).isEqualTo(1)
        assertThat(store.outcome.value).isNull()
    }

    @Test
    fun theMessage_survivesARecreatedComposition() {
        // The route keeps `message` in rememberSaveable; this host mirrors that contract, including the
        // effect acknowledging the store before the screen is recreated.
        val store = DataClearStore().apply { publish(ClearOutcome.Cleared) }
        val tester = StateRestorationTester(composeTestRule)
        tester.setContent { MessageHost(store) }

        composeTestRule.onNodeWithTag(TAG).assertTextEquals("Cleared")

        tester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(TAG).assertTextEquals("Cleared")
    }

    @Composable
    private fun MessageHost(store: DataClearStore) {
        var message by rememberSaveable { mutableStateOf<String?>(null) }
        val outcome by store.outcome.collectAsState()
        ClearOutcomeEffect(
            outcome = outcome,
            onClear = { message = "Cleared" },
            onFail = { message = "Failed" },
            onAcknowledge = store::forget,
        )
        Text(message ?: "none", modifier = Modifier.testTag(TAG))
    }

    private companion object {
        const val TAG = "clear-message"
    }
}
