package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.DeloadSuggestion
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The recovery-week choices (ROADMAP N300).
 *
 * The two actions are deliberately different: the exemption is a mark, the build is the creator. The
 * screen says which is which, and the unmark of a built week asks before it returns the week to the
 * ordinary schedule.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ProgramWeeksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val thisWeek = LocalDate.of(2026, 10, 5)
    private val nextWeek = LocalDate.of(2026, 10, 12)

    @Test
    fun exemptingThisWeek_isTheMark() {
        var set: Pair<LocalDate, Boolean>? = null
        setScreen(onSetExempt = { week, exempt -> set = week to exempt })

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_THIS).performClick()

        assertThat(set).isEqualTo(thisWeek to true)
    }

    @Test
    fun exemptingNextWeek_isTheProspectiveMark() {
        // N300: a vacation is known before it starts; the mark is the same row, one week on.
        var set: Pair<LocalDate, Boolean>? = null
        setScreen(onSetExempt = { week, exempt -> set = week to exempt })

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_NEXT).performClick()

        assertThat(set).isEqualTo(nextWeek to true)
    }

    @Test
    fun anExemptWeek_offersTheWayBack() {
        var set: Pair<LocalDate, Boolean>? = null
        setScreen(
            state = state(thisExempt = true),
            onSetExempt = { week, exempt -> set = week to exempt },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_THIS).performClick()

        assertThat(set).isEqualTo(thisWeek to false)
    }

    @Test
    fun aBuiltWeek_asksBeforeTheExemptionIsRemoved() {
        var set: Pair<LocalDate, Boolean>? = null
        setScreen(
            state = state(thisExempt = true, thisHasPlan = true),
            onSetExempt = { week, exempt -> set = week to exempt },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_THIS).performClick()
        assertThat(set).isNull()
        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_UNMARK_DIALOG).assertExists()
        composeTestRule.onNodeWithText(
            "The week returns to the normal schedule and can read as missed.",
            substring = true,
        ).assertExists()

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_UNMARK_CONFIRM).performClick()

        assertThat(set).isEqualTo(thisWeek to false)
    }

    @Test
    fun aDueCadence_isASuggestion_withTheBuilderOneTapAway() {
        // N301: the app counts weeks it can see; it never writes the week on its own.
        var built = false
        setScreen(
            state = state(
                suggestion = DeloadSuggestion(
                    weeksSince = 6,
                    sinceLastDeload = true,
                    due = true,
                ),
            ),
            onBuild = { built = true },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_SUGGESTION).assertExists()
        composeTestRule.onNodeWithText("It has been 6 weeks since your last recovery week.").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_SUGGESTION_BUILD).performClick()

        assertThat(built).isTrue()
    }

    @Test
    fun theBuilder_isTheOtherAction() {
        var built = false
        setScreen(onBuild = { built = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_BUILD).performClick()

        assertThat(built).isTrue()
    }

    @Test
    fun theIntro_namesBothActions() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Programs.WEEKS_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Recovery week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exempt this week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exempt next week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Build a deload week").assertIsDisplayed()
    }

    @Test
    fun theNextCard_namesNextWeek() {
        // B399: one card per week, and each says the week its control acts on — the next-week card used
        // to borrow the this-week words, so two identical controls sat on one screen.
        setScreen(state = state(nextExempt = true, nextHasPlan = true))

        composeTestRule.onNodeWithText("Next week is exempt from scoring.").assertIsDisplayed()
        composeTestRule.onNodeWithText("A deload plan is built for next week.").assertIsDisplayed()
        composeTestRule.onNodeWithText("This week is exempt from scoring.").assertDoesNotExist()
        composeTestRule.onNodeWithText("A deload plan is built for this week.").assertDoesNotExist()
    }

    private fun state(
        thisExempt: Boolean = false,
        thisHasPlan: Boolean = false,
        nextExempt: Boolean = false,
        nextHasPlan: Boolean = false,
        suggestion: DeloadSuggestion? = null,
    ) = ProgramWeeksUiState(
        isLoading = false,
        programName = "Upper/Lower",
        thisWeek = thisWeek,
        nextWeek = nextWeek,
        thisWeekExempt = thisExempt,
        thisWeekHasPlan = thisHasPlan,
        nextWeekExempt = nextExempt,
        nextWeekHasPlan = nextHasPlan,
        suggestion = suggestion,
    )

    private fun setScreen(
        state: ProgramWeeksUiState = state(),
        onBuild: () -> Unit = {},
        onSetExempt: (LocalDate, Boolean) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                ProgramWeeksScreen(
                    state = state,
                    onBack = {},
                    onBuild = onBuild,
                    onSetExempt = onSetExempt,
                    onDismissError = {},
                )
            }
        }
    }
}
