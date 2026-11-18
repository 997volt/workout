package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.AutoProgressionCheck
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.ProgressionRule
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/**
 * Auto progression's switch, preflight and reset (ROADMAP N299).
 *
 * The screen is the consent: what will move is named before the switch is turned on, and the reset
 * asks before it rewrites a plan. Both are asserted through the control a lifter presses.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ProgramProgressionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theTitle_isSetAsAScreenTitle() {
        // B407: every pushed screen's title goes through the component that clamps it and marks it as a
        // heading (B369), which is why the rendered text is the eyebrow's uppercase.
        setScreen()

        composeTestRule.onNodeWithText("AUTO PROGRESSION").assertIsDisplayed()
    }

    @Test
    fun theSwitchReportsTheStateTheLifterAskedFor() {
        var requested: Boolean? = null
        setScreen(onSetAuto = { requested = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_SWITCH).performClick()

        assertThat(requested).isTrue()
    }

    @Test
    fun turningItOnAsksFirst_andTheConfirmationIsTheWrite() {
        var confirmed = false
        setScreen(state = state(confirmEnable = true), onConfirmEnable = { confirmed = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_ENABLE).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun thePreflightNamesWhatWillMove_andWhatNeedsThePlanFixed() {
        setScreen(
            state = state(
                checks = listOf(
                    AutoProgressionCheck("Day A", "Bench Press", ProgressionRule.REPS_THEN_LOAD),
                    AutoProgressionCheck("Day A", "Curl", ProgressionRule.NO_TARGET_RPE),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_PROGRESSION_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeTestRule.onNodeWithText("Day A · Reps, then load").assertIsDisplayed()
        composeTestRule.onNodeWithText("Curl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set a target effort on this exercise.").assertIsDisplayed()
    }

    @Test
    fun aProgramWhoseMovementsAnswerForThemselves_saysTheSwitchHasNothingToDecide() {
        // B417: the old line told the lifter to fix "the plans below" while the only list below was
        // "answers for themselves" — nothing was broken.
        setScreen(
            state = state(
                checks = listOf(
                    AutoProgressionCheck(
                        "Day A",
                        "Bench Press",
                        ProgressionRule.LOAD_ONLY,
                        ProgressionMode.OFF,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText(
            "Every movement here answers for itself, so this switch has nothing to decide.",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Nothing in this program can progress yet. Fix the plans below.",
        ).assertDoesNotExist()
    }

    @Test
    fun aProgramWithAPlanToFix_isToldToFixIt() {
        setScreen(
            state = state(
                checks = listOf(
                    AutoProgressionCheck("Day A", "Curl", ProgressionRule.NO_TARGET_RPE),
                ),
            ),
        )

        composeTestRule.onNodeWithText(
            "Nothing in this program can progress yet. Fix the plans below.",
        ).assertIsDisplayed()
    }

    @Test
    fun theSwitchCard_statesWhatTheSwitchDecides() {
        // N325: the consent's own words, not just the control's presence.
        setScreen(state = state())

        composeTestRule.onNodeWithText("Adjust this program's targets").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("A movement's own answer still decides for that movement.")
            .assertIsDisplayed()
    }

    @Test
    fun theEnableDialog_statesWhatTheAppWillWrite() {
        setScreen(state = state(confirmEnable = true))

        composeTestRule.onNodeWithText("Turn on auto progression?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "The app will write the next step into your plan after you complete the work. " +
                    "Every write is recorded and can be undone. Nothing is written until you finish an exercise.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun theBaselineLine_namesWhenItStarted() {
        val at = Instant.ofEpochMilli(1_700_000_000_000L)
        setScreen(state = state(auto = true, baselineAt = at))

        val date = HistoryFormat.date(at, ZoneId.systemDefault())
        composeTestRule.onNodeWithText("Started from the targets on $date").assertIsDisplayed()
    }

    @Test
    fun aBlock_saysTheSwitchIsNotOffered() {
        // B487: the route is not normally reachable for a block, but a stale one must not draw a switch
        // the repository now refuses.
        setScreen(state = state(isSequence = true))

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_SWITCH).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("This program is a block. Its weeks are its own, so auto progression is not offered.")
            .assertIsDisplayed()
    }

    @Test
    fun resetIsOfferedOnlyWhileAutoIsOn_andAsksBeforeRestoring() {
        var reset = false
        setScreen(
            state = state(auto = true, baselineAt = Instant.ofEpochMilli(1_700_000_000_000L)),
            onReset = { reset = true },
        )
        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_RESET).performClick()

        assertThat(reset).isTrue()
    }

    @Test
    fun resetIsNotDrawn_whileAutoIsOff() {
        // B477: the retained snapshot had no reader because the screen draws Reset only while the
        // switch is on; the state now drops it, but the control's absence is the screen's own rule.
        setScreen(state = state(auto = false, baselineAt = Instant.ofEpochMilli(1_700_000_000_000L)))

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_RESET).assertDoesNotExist()
    }

    @Test
    fun resetIsDisabled_whileAResetWrites() {
        // B414: the button used to stay live, so a second confirm started a second reset.
        setScreen(
            state = state(
                auto = true,
                baselineAt = Instant.ofEpochMilli(1_700_000_000_000L),
                writing = true,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_RESET).assertIsNotEnabled()
    }

    @Test
    fun theResetConfirmationIsTheWrite() {
        var confirmed = false
        setScreen(
            state = state(auto = true, baselineAt = Instant.ofEpochMilli(1_700_000_000_000L), confirmReset = true),
            onConfirmReset = { confirmed = true },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.AUTO_RESET_CONFIRM).performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun theResetConfirmation_namesAnotherProgramSharingThePlan() {
        // B476: the plan is shared, so the dialog names the program the same write rewinds.
        setScreen(
            state = state(
                auto = true,
                baselineAt = Instant.ofEpochMilli(1_700_000_000_000L),
                confirmReset = true,
                sharedProgramNames = listOf("PPL"),
            ),
        )

        composeTestRule.onNodeWithText("Also rewinds the plan used by PPL.").assertExists()
    }

    private fun setScreen(
        state: ProgramProgressionUiState = state(),
        onSetAuto: (Boolean) -> Unit = {},
        onConfirmEnable: () -> Unit = {},
        onDismissEnable: () -> Unit = {},
        onReset: () -> Unit = {},
        onConfirmReset: () -> Unit = {},
        onDismissReset: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                ProgramProgressionScreen(
                    state = state,
                    onBack = {},
                    onSetAuto = onSetAuto,
                    onConfirmEnable = onConfirmEnable,
                    onDismissEnable = onDismissEnable,
                    onReset = onReset,
                    onConfirmReset = onConfirmReset,
                    onDismissReset = onDismissReset,
                    onDismissError = {},
                    onDismissRestoreMessage = {},
                )
            }
        }
    }

    private fun state(
        auto: Boolean = false,
        checks: List<AutoProgressionCheck> = emptyList(),
        baselineAt: Instant? = null,
        confirmEnable: Boolean = false,
        confirmReset: Boolean = false,
        writing: Boolean = false,
        sharedProgramNames: List<String> = emptyList(),
        isSequence: Boolean = false,
    ) = ProgramProgressionUiState(
        isLoading = false,
        programName = "Upper/Lower",
        autoProgression = auto,
        isSequence = isSequence,
        checks = checks,
        baselineAt = baselineAt,
        confirmEnable = confirmEnable,
        confirmReset = confirmReset,
        writing = writing,
        sharedProgramNames = sharedProgramNames,
    )
}
