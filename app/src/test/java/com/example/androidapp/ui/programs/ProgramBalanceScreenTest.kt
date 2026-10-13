package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A program's balance view (ROADMAP N119, N122).
 *
 * The screen is stateless and the counting is [com.example.androidapp.domain.model.MuscleSetsTest]'s, so what
 * is asserted here is the reading: a row per muscle with its own count — halves included — the plan's total,
 * the words for work that states no muscle, and the empty state a plan with no working sets deserves.
 *
 * The counts are **half-sets**: `18` reads as "9 sets", `3` as "1.5 sets", `2` as "1 set".
 */
@RunWith(AndroidJUnit4::class)
class ProgramBalanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(state: ProgramBalanceUiState) {
        composeTestRule.setContent {
            AndroidAppTheme {
                ProgramBalanceScreen(state = state, onBack = {})
            }
        }
    }

    @Test
    fun eachMuscleReadsItsOwnCount_besideThePlansTotal() {
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                programName = "Upper/Lower",
                balance = listOf(
                    MuscleSets(MuscleGroup.QUADS, 18),
                    MuscleSets(MuscleGroup.CHEST, 6),
                ),
                plannedSets = 12,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.programBalanceRow(MuscleGroup.QUADS.name)).assertExists()
        composeTestRule.onNodeWithText("9 sets").assertExists()
        composeTestRule.onNodeWithText("3 sets").assertExists()
        composeTestRule.onNodeWithText("12 working sets planned").assertExists()
    }

    @Test
    fun aSecondarysHalf_readsWithItsHalf_andTheWeightingIsSaid() {
        // N122: a secondary's share is half a set, so the rows can sum past the plan's size — which the screen
        // says in words rather than leaving a lifter to reverse-engineer why 1.5 + 6 is not 6.
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                balance = listOf(
                    MuscleSets(MuscleGroup.LATS, 12),
                    MuscleSets(MuscleGroup.BICEPS, 3),
                    MuscleSets(MuscleGroup.HAMSTRINGS, 2),
                ),
                plannedSets = 6,
            ),
        )

        composeTestRule.onNodeWithText("6 sets").assertExists()
        composeTestRule.onNodeWithText("1.5 sets").assertExists()
        composeTestRule.onNodeWithText("1 set").assertExists()
        composeTestRule.onNodeWithText(
            "A set counts once for its main muscle, and half for each assisting one.",
        ).assertExists()
    }

    @Test
    fun workThatStatesNoMuscle_isNamed_ratherThanCountedAsNothing() {
        // The bucket is a caveat rather than a muscle, so it says so — dropping it would make the rest of the
        // plan look more balanced than it is.
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                balance = listOf(MuscleSets(MuscleGroup.QUADS, 8), MuscleSets(null, 4)),
                plannedSets = 6,
            ),
        )

        composeTestRule.onNodeWithText("No muscle stated").assertExists()
        composeTestRule.onNodeWithTag(TestTags.programBalanceRow("unstated")).assertExists()
    }

    @Test
    fun aPlanWithNoWorkingSets_saysSo_ratherThanShowingNothing() {
        // N112 is what makes this state reachable with a program full of ramps: a plan of warm-ups alone has
        // no work to spread.
        setScreen(ProgramBalanceUiState(isLoading = false, balance = emptyList(), plannedSets = 0))

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TOTAL).assertDoesNotExist()
    }
}
