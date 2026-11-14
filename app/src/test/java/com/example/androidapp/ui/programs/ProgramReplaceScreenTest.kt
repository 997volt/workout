package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ReplacementSeed
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The replace sheet's screen (ROADMAP N308): the suggestions, Empty and the confirm. */
@RunWith(AndroidJUnit4::class)
class ProgramReplaceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        state: ProgramReplaceUiState,
        onSelect: (String) -> Unit = {},
        onSetEmpty: (Boolean) -> Unit = {},
        onConfirm: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProgramReplaceScreen(
                state = state,
                onSelect = onSelect,
                onSetEmpty = onSetEmpty,
                onBrowse = {},
                onConfirm = onConfirm,
                onBack = {},
                onDismissMessage = {},
            )
        }
    }

    @Test
    fun theSuggestions_andTheCounts_areShown_andTheConfirmWaitsForAChoice() {
        show(
            ProgramReplaceUiState(
                isLoading = false,
                oldName = "Back Squat",
                suggestions = listOf(Exercise(id = "front-squat", name = "Front Squat")),
                setCountBefore = 5,
                setCountAfter = 3,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.REPLACE_SCREEN).assertExists()
        composeTestRule.onNodeWithText("Front Squat").assertExists()
        composeTestRule.onNodeWithText("Working sets: 5 → 3").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.REPLACE_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun aSuggestion_selects_andTheConfirm_callsBack() {
        var selected: String? = null
        var confirmed = false
        show(
            ProgramReplaceUiState(
                isLoading = false,
                oldName = "Back Squat",
                suggestions = listOf(Exercise(id = "front-squat", name = "Front Squat")),
                selected = Exercise(id = "front-squat", name = "Front Squat"),
                seeds = listOf(ReplacementSeed(80_000L, 0L, 6, null)),
                setCountBefore = 5,
                setCountAfter = 3,
            ),
            onSelect = { selected = it },
            onConfirm = { confirmed = true },
        )

        composeTestRule.onAllNodesWithText("Front Squat").onFirst().performClick()
        assertThat(selected).isEqualTo("front-squat")

        composeTestRule.onNodeWithTag(TestTags.Programs.REPLACE_CONFIRM).performScrollTo().performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun empty_isATargetChoice_andSaysWhatItMeans() {
        var empty = false
        show(
            ProgramReplaceUiState(
                isLoading = false,
                oldName = "Back Squat",
                selected = Exercise(id = "front-squat", name = "Front Squat"),
            ),
            onSetEmpty = { empty = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.REPLACE_EMPTY).performClick()

        assertThat(empty).isTrue()
        composeTestRule.onNodeWithText("clears the loads", substring = true).assertExists()
    }
}
