package com.example.androidapp.ui.exercises

import java.io.IOException
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.ui.components.TestTags
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
    fun aFamilyASearchNarrowed_saysWhatItFoundOfWhatItHolds() {
        // ROADMAP N225: the head used to count the matched children, so a family of three read "1 exercise"
        // beside the one row the search found. Both numbers are stated now.
        val wide = listOf(
            bench,
            exercise("barbell-bench-press", "Barbell Bench Press", "Chest", "Barbell", parent = "cat-bench"),
            exercise("dumbbell-bench-press", "Dumbbell Bench Press", "Chest", "Dumbbell", parent = "cat-bench"),
            exercise("incline-bench-press", "Incline Bench Press", "Chest", "Barbell", parent = "cat-bench"),
        )

        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = wide, query = "dumbbell"))

        composeTestRule.onNodeWithText("1 of 3 exercises").assertExists()
    }

    @Test
    fun aCategory_isNotOfferedByThePicker() {
        // The same composable serves both screens, and only the library is given the action: the gap a lifter
        // feels mid-workout is a missing *movement*, and a head can never be logged against (N95).
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = items))

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_CATEGORY).assertDoesNotExist()
    }

    @Test
    fun aFamily_opensFolded_andItsNameOpensIt() {
        // ROADMAP N95: the head is a row that holds others, so the library reads as families rather than
        // as a flat list. **Every family starts folded**, so the screen opens as the list of families it is
        // rather than as a wall of movements — and the head's name is the control, the shape a planned
        // exercise's row already uses (N91), with its state announced because the same name does different
        // things open and folded.
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = groupedLibrary))

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).assertIsDisplayed()
        // The count is what a folded family is *for*: it says something is inside without spending the room.
        composeTestRule.onNodeWithText("1 exercise").assertExists()
        composeTestRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).performClick()

        // Read by existence rather than by display: this test's viewport is short, and what it asserts is
        // that the child is in the list under its head, which is N95's shape.
        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
        // A movement filed under nothing was never behind a fold, and still is not.
        composeTestRule.onNodeWithText("Back Squat").assertExists()

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).performClick()

        composeTestRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bench")).assertIsDisplayed()
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

    /**
     * Holding a family shows the facts its movements inherit (ROADMAP N117).
     *
     * A category head is never offered and never logged (N95), so its own taxonomy has no other screen — and
     * it is the taxonomy that matters, because own-if-stated-else-the-category's is the rule a movement under
     * it is read by (N110). The row's tap already means fold (N91), so the hold is what reaches this.
     */
    @Test
    fun holdingAFamily_showsTheFactsItsMovementsInherit() {
        val chest = Exercise(
            id = "cat-chest",
            name = "Chest",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.QUADS),
            movementPattern = MovementPattern.PRESS,
            rowKind = RowKind.CATEGORY,
        )
        setScreen(
            ExerciseLibraryUiState(
                isLoading = false,
                exercises = listOf(
                    chest,
                    exercise(
                        "barbell-bench-press",
                        "Barbell Bench Press",
                        "Chest",
                        "Barbell",
                        parent = "cat-chest",
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-chest"))
            .performTouchInput { longClick() }

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_CATEGORY_DETAILS).assertExists()
        composeTestRule.onNodeWithText(MovementPattern.PRESS.label).assertExists()
        composeTestRule.onNodeWithText(
            listOf(MuscleGroup.TRICEPS.label, MuscleGroup.QUADS.label).joinToString(", "),
        ).assertExists()
    }

    /**
     * A family that states nothing says so, rather than naming *Other* (ROADMAP N110, N117).
     *
     * Null means **unstated** and `OTHER` is a real answer a lifter gives, so a details view that answered
     * "Other" for a blank row would invent a taxonomy the movement is not read by. Three fields say it here —
     * the pattern, the primary muscle and the secondary list — which is also what makes the count exact.
     */
    @Test
    fun holdingAFamilyThatStatesNothing_saysNone_ratherThanOther() {
        setScreen(
            ExerciseLibraryUiState(
                isLoading = false,
                exercises = listOf(Exercise(id = "cat-bare", name = "Bare", rowKind = RowKind.CATEGORY)),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.libraryCategory("cat-bare"))
            .performTouchInput { longClick() }

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_CATEGORY_DETAILS).assertExists()
        assertEquals(
            3,
            composeTestRule.onAllNodesWithText(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.exercise_detail_none),
            ).fetchSemanticsNodes().size,
        )
    }

    private fun setScreen(
        state: ExerciseLibraryUiState,
        onQueryChange: (String) -> Unit = {},
        onExerciseClick: (String) -> Unit = {},
        onNewCategory: ((String) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            // The screen is stateless, so the harness owns the query **and** reports it: a test can type into
            // the search box and assert both what it wrote back and what the list did with it.
            val current = remember { mutableStateOf(state) }
            ExerciseLibraryScreen(
                state = current.value,
                title = "Exercise library",
                onQueryChange = { typed ->
                    current.value = current.value.copy(query = typed)
                    onQueryChange(typed)
                },
                onExerciseClick = onExerciseClick,
                onNewCategory = onNewCategory,
                onDismissMessage = {},
            )
        }
    }

    @Test
    fun aSearch_opensTheFamilyItReachesInto() {
        // N95: the fold is how the library is browsed, so a query must not answer with a folded head. The
        // library and the picker group the same way, and this is the half that says so on the library's own
        // screen — searching a movement that lives inside a family shows the movement.
        setScreen(ExerciseLibraryUiState(isLoading = false, exercises = groupedLibrary))

        composeTestRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_SEARCH_FIELD).performTextInput("barbell")

        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
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
    fun theLibrary_neverOffersANewMovement() {
        // Making a movement is the in-workout picker's action now (B94): the library is where the library's
        // shape is kept, and its one action is a category.
        setScreen(
            ExerciseLibraryUiState(isLoading = false, exercises = items),
            onNewCategory = {},
        )

        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_EXERCISE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.LIBRARY_NEW_CATEGORY).assertIsDisplayed()
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
