package com.example.androidapp.ui.exercises

import java.io.IOException
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.TestTags
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the library screen.
 *
 * The screen under test is the *stateless* composable, so there is no Hilt
 * container and no repository involved — the state is handed in directly.
 * Testing the stateful `ExerciseLibraryRoute` would need `hilt-android-testing`
 * and a custom test runner; that belongs with the ViewModel-wiring tests later.
 *
 *   ./gradlew connectedDebugAndroidTest
 */



@RunWith(AndroidJUnit4::class)
class ExerciseLibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Two movements, one in a family (ROADMAP N95), so a test can assert both row shapes.
     *
     * The screen holds the library and groups it, so a test hands in exercises and reads rows — the
     * grouping itself is covered by `ExerciseLibraryTest` on the JVM, where the interesting cases are
     * cheap to arrange.
     */
    private val bench = category("cat-bench", "Bench Press")

    private val items = listOf(
        exercise("back-squat", "Back Squat", "Quads", "Barbell"),
        exercise("hammer-curl", "Hammer Curl", "Biceps", "Dumbbell"),
    )

    private val groupedLibrary = listOf(
        bench,
        exercise("barbell-bench-press", "Barbell Bench Press", "Chest", "Barbell", parent = "cat-bench"),
        exercise("back-squat", "Back Squat", "Quads", "Barbell"),
    )

    private fun exercise(
        id: String,
        name: String,
        muscle: String,
        equipment: String,
        parent: String? = null,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.valueOf(muscle.uppercase()),
        equipment = Equipment.valueOf(equipment.uppercase()),
        movementPattern = MovementPattern.OTHER,
        parentId = parent,
    )

    private fun category(id: String, name: String) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.OTHER,
        movementPattern = MovementPattern.OTHER,
        rowKind = RowKind.CATEGORY,
    )

            @Test
    fun aCategory_canBeMadeFromTheLibrary() {
        // ROADMAP N95: the seeded families are a starting set rather than a closed one, so without this a
        // custom movement could only ever be filed under a head the seed happened to ship.
        var created: String? = null
        setScreen(
            ExerciseLibraryUiState(isLoading = false, exercises = items),
            onNewCategory = { created = it },
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_CATEGORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_CATEGORY_NAME).performTextInput("Press")
        composeTestRule.onNodeWithTag(TestTags.NEW_CATEGORY_SAVE).performClick()

        assertEquals("Press", created)
    }

    @Test
    fun aCategory_isNotOfferedByThePicker() {
        // The same composable serves both screens, and only the library is given the action: the gap a lifter
        // feels mid-workout is a missing *movement*, and a head can never be logged against (N95).
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_CATEGORY).assertDoesNotExist()
    }

    @Test
    fun aFamily_isAHeadWithItsChildUnderIt_andFoldsOnItsName() {
        // ROADMAP N95: the head is a row that holds others, so the library reads as families rather than
        // as a flat list. Its name is the control — the shape a planned exercise's row already uses (N91) —
        // and its state is announced, because the same name does different things open and folded.
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = groupedLibrary))

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 exercise").assertExists()
        // Read by existence rather than by display: this test's viewport is short, and what it asserts is
        // that the child is in the list under its head, which is N95's shape.
        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).performClick()

        composeTestRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()
        // The head stays, and so does everything filed under nothing.
        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).assertIsDisplayed()
        composeTestRule.onNodeWithText("Back Squat").assertExists()
    }

    @Test
    fun aMovementInNoFamily_isATopLevelRow() {
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = groupedLibrary))

        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quads · Barbell").assertIsDisplayed()
    }

    @Test
    fun anEmptyLibrary_saysSo_ratherThanBlamingTheSearch() {
        setScreen(
            ExerciseLibraryUiState(isLoading = false, exercises = emptyList(), libraryIsEmpty = true),
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_EMPTY_LIBRARY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NO_MATCH).assertDoesNotExist()
    }

    @Test
    fun aSearchWithNoHits_namesTheQuery() {
        setScreen(
            ExerciseLibraryUiState(
                isLoading = false,
                exercises = emptyList(),
                libraryIsEmpty = false,
                query = "zzz",
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NO_MATCH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LIBRARY_EMPTY_LIBRARY).assertDoesNotExist()
    }

    private fun setScreen(
        state: ExerciseLibraryUiState,
        onQueryChange: (String) -> Unit = {},
        onExerciseClick: (String) -> Unit = {},
        onNewExercise: (() -> Unit)? = null,
        onNewCategory: ((String) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            ExerciseLibraryScreen(
                state = state,
                title = "Exercise library",
                onQueryChange = onQueryChange,
                onExerciseClick = onExerciseClick,
                onNewExercise = onNewExercise,
                onNewCategory = onNewCategory,
            )
        }
    }

    @Test
    fun rendersTitleAndRows() {
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quads · Barbell").assertIsDisplayed()
    }

    @Test
    fun anUneditedCustomExercise_rendersItsNameWithoutAnOtherSubtitle() {
        setScreen(
            ExerciseLibraryUiState(
                isLoading = false,
                exercises = listOf(exercise("custom-1", "Sled Push", "Other", "Other")),
            ),
        )

        composeTestRule.onNodeWithText("Sled Push").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other · Other").assertDoesNotExist()
    }

    @Test
    fun withNoCreateCallback_thereIsNoNewExerciseAction() {
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_EXERCISE).assertDoesNotExist()
    }

    @Test
    fun tappingNewExercise_reportsIt() {
        var started = false
        setScreen(
            ExerciseLibraryUiState(isLoading = false, exercises = items),
            onNewExercise = { started = true },
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_EXERCISE).performClick()

        assertTrue("the picker's create action should be wired through", started)
    }

    @Test
    fun typingInSearchField_reportsTheQuery() {
        var typed: String? = null
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items), onQueryChange = { typed = it })

        composeTestRule.onNode(hasSetTextAction()).performTextInput("squat")

        assertEquals("squat", typed)
    }

    @Test
    fun tappingRow_reportsThatExerciseId() {
        var clicked: String? = null
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items), onExerciseClick = { clicked = it })

        composeTestRule.onNodeWithText("Hammer Curl").performClick()

        assertEquals("hammer-curl", clicked)
    }

    @Test
    fun emptyResult_showsMessageInsteadOfSpinner() {
        setScreen(ExerciseLibraryUiState(query = "zzz", isLoading = false, exercises = emptyList()))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NO_MATCH).assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading exercises…").assertDoesNotExist()
    }

    @Test
    fun theLibrary_offersNoExportImportOrHistoryLink() {
        // ROADMAP B1: export and import moved out of here, and leaving a second copy is the drift
        // the fix exists to remove — one path, not two. The overflow went with the history link it
        // last held: History is the tab beside this screen, so a menu whose one item opens a room you
        // are already standing next to is a second door rather than a shortcut.
        setScreen(ExerciseLibraryUiState(isLoading = false))

        composeTestRule.onNodeWithTag(TestTags.DATA_EXPORT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.DATA_IMPORT).assertDoesNotExist()
        // Read from resources rather than matched as English: the assertion is "the History screen is
        // not reachable from here", and a reworded title must not make it pass by accident.
        val historyTitle =
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.history_title)
        composeTestRule.onNodeWithText(historyTitle).assertDoesNotExist()
    }

    @Test
    fun aFailedRead_isShown_inPlaceOfTheList() {
        // ROADMAP B4: the failure is rendered where the data would have been, so an
        // empty list cannot be mistaken for an empty library.
        setScreen(
            ExerciseLibraryUiState(
                isLoading = false,
                error = DataError.Storage(IOException("database is locked")),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_READ_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LIBRARY_EMPTY_LIBRARY).assertDoesNotExist()
    }
}
