package com.example.androidapp.ui.workout

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the exercise picker (ROADMAP N95, N110).
 *
 * The screen under test is the *stateless* composable, so there is no Hilt container and no repository
 * involved — the state is handed in directly.
 *
 * What is worth asserting here rather than on the JVM is the rule the **list** enforces: the picker now draws
 * category heads, and a head folds rather than choosing. That is where N95's "a category is never offered and
 * never logged" lives once the heads are drawn at all, and it is a fact about a tap.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** One family, its movement, and a movement filed under nothing — both row shapes in one list. */
    private val library = listOf(
        category("cat-bench", "Bench Press"),
        movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench"),
        movement("back-squat", "Back Squat"),
    )

    @Test
    fun aMovementFiledUnderACategory_isDrawnUnderItsHead() {
        // N110's shape on the picker: the same grouping the library draws, so browsing for what was just done
        // finds the family a lifter filed it in.
        setScreen(ExercisePickerUiState(isLoading = false, exercises = library))

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).assertIsDisplayed()
        // Folded on arrival, the way the library opens: the count says what is inside without spending room.
        composeTestRule.onNodeWithText("1 exercise").assertExists()
        composeTestRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).performClick()

        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
    }

    @Test
    fun tappingAHead_foldsIt_ratherThanChoosingIt() {
        // N95: a category is never offered and never logged. With the heads drawn, the rule has to hold at the
        // tap — the head's own action is the fold, so nothing is handed to the choice.
        var chosen: String? = null
        setScreen(
            ExercisePickerUiState(isLoading = false, exercises = library),
            onExerciseClick = { chosen = it },
        )

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).performClick()

        assertNull("a head is not a lift, so it cannot be chosen", chosen)
    }

    @Test
    fun tappingAMovement_choosesIt() {
        var chosen: String? = null
        setScreen(
            ExercisePickerUiState(isLoading = false, exercises = library),
            onExerciseClick = { chosen = it },
        )

        composeTestRule.onNodeWithText("Back Squat").performClick()

        assertEquals("back-squat", chosen)
    }

    @Test
    fun aSearch_opensTheFamilyItReachesInto() {
        // The picker is searched mid-workout, so a query must not cost a tap before the lifter can see what
        // matched: the fold is how the list is *browsed*, and a query is not browsing. Without this, searching
        // for a lift would answer with its family and hide the lift itself.
        setScreen(ExercisePickerUiState(isLoading = false, exercises = library))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_SEARCH_FIELD).performTextInput("barbell")

        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
    }

    private fun setScreen(
        state: ExercisePickerUiState,
        onExerciseClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            // The screen is stateless, so the harness owns the query — the only way a test can type into the
            // search box and see the list answer.
            val current = remember { mutableStateOf(state) }
            ExercisePickerScreen(
                state = current.value,
                onQueryChange = { current.value = current.value.copy(query = it) },
                onExerciseClick = onExerciseClick,
                onBack = {},
                onDismissMessage = {},
            )
        }
    }

    private fun category(id: String, name: String) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.OTHER,
        movementPattern = MovementPattern.OTHER,
        rowKind = RowKind.CATEGORY,
    )

    private fun movement(id: String, name: String, parent: String? = null) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        parentId = parent,
    )
}
