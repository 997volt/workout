package com.example.androidapp.ui.programs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.TrainedWindow
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A program's balance view (ROADMAP N119, N289).
 *
 * The screen is stateless and the counting is
 * [com.example.androidapp.domain.model.MuscleSetsTest]'s and
 * [com.example.androidapp.domain.model.TrainedMuscleSetsTest]'s, so what is asserted here is the reading: a
 * row per muscle with its own count — halves included — the plan's total, the words for work that states no
 * muscle, the log's total for its window, and the empty states each side deserves. The toggles are asserted
 * by their tags rather than by English, and one test drives them to prove the side and the window change
 * without leaving the destination.
 *
 * The counts are **half-sets**: `18` reads as "9 sets", `3` as "1.5 sets", `2` as "1 set". The trained side
 * is deliberately bars-free and target-free: what is shown is what happened, not what should.
 */
@RunWith(AndroidJUnit4::class)
class ProgramBalanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: ProgramBalanceUiState,
        onSelectView: (BalanceView) -> Unit = {},
        onSelectWindow: (TrainedWindow) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                ProgramBalanceScreen(
                    state = state,
                    onBack = {},
                    onSelectView = onSelectView,
                    onSelectWindow = onSelectWindow,
                )
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
        // The plan is the side a program editor opens on; the trained chips are not shown until asked for.
        composeTestRule.onNodeWithTag(TestTags.programBalanceView(BalanceView.TRAINED.name)).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_7_DAYS.name),
        ).assertDoesNotExist()
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
    fun theTrainedSide_countsWhatWasLogged_inItsWindow_andSaysHowItIsCounted() {
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                balance = listOf(MuscleSets(MuscleGroup.QUADS, 2)),
                plannedSets = 1,
                view = BalanceView.TRAINED,
                window = TrainedWindow.LAST_7_DAYS,
                trained = listOf(
                    MuscleSets(MuscleGroup.QUADS, 18),
                    MuscleSets(MuscleGroup.BICEPS, 3),
                    MuscleSets(MuscleGroup.LATS, 2),
                ),
                trainedSets = 12,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_TOTAL).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.programBalanceTrainedRow(MuscleGroup.QUADS.name),
        ).assertExists()
        composeTestRule.onNodeWithText("9 sets").assertExists()
        composeTestRule.onNodeWithText("1.5 sets").assertExists()
        composeTestRule.onNodeWithText("1 set").assertExists()
        composeTestRule.onNodeWithText("12 working sets logged").assertExists()
        composeTestRule.onNodeWithText(
            "A set counts once for its main muscle, and half for each assisting one. Warm-ups are not counted.",
        ).assertExists()
        // The plan's own total is not repeated on the trained side, and neither are its bars or its rows.
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TOTAL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.programBalanceRow(MuscleGroup.QUADS.name)).assertDoesNotExist()
    }

    @Test
    fun theWindowChips_areOnTheTrainedSide_andReportTheChoice() {
        var chosen: TrainedWindow? = null
        setScreen(
            state = ProgramBalanceUiState(
                isLoading = false,
                view = BalanceView.TRAINED,
                window = TrainedWindow.LAST_7_DAYS,
            ),
            onSelectWindow = { chosen = it },
        )

        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_7_DAYS.name),
        ).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_28_DAYS.name),
        ).assertExists()

        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_28_DAYS.name),
        ).performClick()

        assertThat(chosen).isEqualTo(TrainedWindow.LAST_28_DAYS)
    }

    @Test
    fun switchingSides_dropsThePlanTotal_andShowsTheLogs() {
        // The screen is stateless; this host is the smallest state a real ViewModel would hold, so the click
        // proves the toggle's callback reaches the side that renders counts rather than only being wired.
        val plan = ProgramBalanceUiState(
            isLoading = false,
            balance = listOf(MuscleSets(MuscleGroup.QUADS, 4)),
            plannedSets = 2,
        )
        val trained = plan.copy(
            view = BalanceView.TRAINED,
            trained = listOf(MuscleSets(MuscleGroup.QUADS, 6)),
            trainedSets = 3,
        )
        var state by mutableStateOf(plan)
        composeTestRule.setContent {
            AndroidAppTheme {
                ProgramBalanceScreen(
                    state = state,
                    onBack = {},
                    onSelectView = { selected ->
                        state = if (selected == BalanceView.TRAINED) trained else plan
                    },
                    onSelectWindow = {},
                )
            }
        }

        composeTestRule.onNodeWithText("2 working sets planned").assertExists()

        composeTestRule.onNodeWithTag(TestTags.programBalanceView(BalanceView.TRAINED.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_TOTAL).assertExists()
        composeTestRule.onNodeWithText("3 working sets logged").assertExists()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TOTAL).assertDoesNotExist()
    }

    @Test
    fun aPlanWithNoWorkingSets_saysSo_andStillOffersTheTrainedSide() {
        // N112 is what makes this state reachable with a program full of ramps: a plan of warm-ups alone has
        // no work to spread — and the log may still hold plenty, so the toggle must not disappear with the rows.
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                balance = emptyList(),
                plannedSets = 0,
                trained = listOf(MuscleSets(MuscleGroup.QUADS, 6)),
                trainedSets = 3,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TOTAL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.programBalanceView(BalanceView.TRAINED.name)).assertExists()
    }

    @Test
    fun aFailedLogRead_saysSo_ratherThanSayingNothingWasTrained() {
        // "No work logged" is a claim about the training. A database failure is a claim about the read, and
        // the two must not look the same (B4).
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                view = BalanceView.TRAINED,
                window = TrainedWindow.LAST_7_DAYS,
                trained = emptyList(),
                trainedSets = 0,
                trainedReadFailed = true,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_FAILED).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_EMPTY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_TOTAL).assertDoesNotExist()
    }

    @Test
    fun aWindowWithNoWork_saysSo_ratherThanShowingNothing_andKeepsBothWindows() {
        setScreen(
            ProgramBalanceUiState(
                isLoading = false,
                view = BalanceView.TRAINED,
                window = TrainedWindow.LAST_7_DAYS,
                trained = emptyList(),
                trainedSets = 0,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROGRAM_BALANCE_TRAINED_TOTAL).assertDoesNotExist()
        // Widening the window is the next thing a lifter would try, so neither chip may go with the rows.
        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_7_DAYS.name),
        ).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.programBalanceWindow(TrainedWindow.LAST_28_DAYS.name),
        ).assertExists()
    }
}
