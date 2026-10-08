package com.example.androidapp.ui.exercises

import java.io.IOException
import com.example.androidapp.domain.DataError
import com.example.androidapp.R
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The detail screen's edit mode (ROADMAP N2, N5).
 *
 * The stateless composable is driven with a fixed state, so these are about the
 * wiring a unit test cannot see: which exercises offer Edit, and whether the form
 * reports what was chosen.
 *
 * Pinned to a phone-sized window: the form is taller than Robolectric's default
 * display, and a control pushed off-screen is a test artifact rather than a
 * behaviour change.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ExerciseDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        state: ExerciseDetailUiState,
        onEdit: () -> Unit = {},
        onCancelEdit: () -> Unit = {},
        onSave: (ExerciseEdit) -> Unit = {},
        onNewVariation: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ExerciseDetailScreen(
                state = state,
                onBack = {},
                onEdit = onEdit,
                onCancelEdit = onCancelEdit,
                onSave = onSave,
                onNewVariation = onNewVariation,
            )
        }
    }

    private fun customState(isEditing: Boolean) = ExerciseDetailUiState(
        isLoading = false,
        isEditing = isEditing,
        exercise = custom,
    )

    @Test
    fun aSeededExercise_isAlsoEditable() {
        // N5 reversed N2's custom-only rule: a seeded exercise must be editable
        // too, and that is safe because the seeder never updates an existing row.
        var edit = false
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded), onEdit = { edit = true })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT).performClick()

        assertTrue("the edit action should be wired through", edit)
    }

    @Test
    fun theWeightUnit_isShownAsTheInForceOne() {
        // ROADMAP N64: the row says what is in force and where it comes from — "App default (kg)" —
        // rather than a bare unit that would read as this exercise's own setting.
        show(customState(isEditing = false))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_WEIGHT_UNIT).assertExists()
    }

    @Test
    fun theWeightUnit_isATripleChoice_whenEditing() {
        // Null is a real answer here — "follow the app" — so the form offers three chips, which is
        // why it is not the two-way control the settings screen uses (ROADMAP N64).
        show(customState(isEditing = true))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_WEIGHT_UNIT_DEFAULT).assertExists()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_WEIGHT_UNIT_KG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_WEIGHT_UNIT_LB).assertExists()
    }

    @Test
    fun aCustomExercise_offersEdit() {
        var edit = false
        show(customState(isEditing = false), onEdit = { edit = true })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT).performClick()

        assertTrue("the edit action should be wired through", edit)
    }

    @Test
    fun theEditForm_isPrefilledWithTheCurrentName() {
        show(customState(isEditing = true))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_NAME).assertTextContains("Sled Push")
    }

    @Test
    fun theEditForm_isPrefilledWithTheExercisesRestAndCue() {
        show(
            ExerciseDetailUiState(
                isLoading = false,
                isEditing = true,
                exercise = seeded.copy(restSeconds = 180, techniqueNote = "Brace, sit back"),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_REST).assertTextContains("180")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CUE).assertTextContains("Brace, sit back")
    }

    @Test
    fun saving_reportsTheEditedName() {
        var saved: ExerciseEdit? = null
        show(customState(isEditing = true), onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_NAME).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_NAME).performTextInput("Sled Push Heavy")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).performScrollTo().performClick()

        assertEquals("Sled Push Heavy", saved?.name)
        assertEquals(MuscleGroup.OTHER, saved?.primaryMuscle)
    }

    @Test
    fun saving_reportsTheRestAndCue() {
        var saved: ExerciseEdit? = null
        show(ExerciseDetailUiState(isLoading = false, isEditing = true, exercise = seeded), onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_REST).performTextInput("180")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CUE).performTextInput("Brace, sit back")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).performScrollTo().performClick()

        assertEquals(180, saved?.restSeconds)
        assertEquals("Brace, sit back", saved?.techniqueNote)
    }

    @Test
    fun anUnparseableRest_disablesSave_ratherThanGuessingASecondsValue() {
        show(ExerciseDetailUiState(isLoading = false, isEditing = true, exercise = seeded))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_REST).performTextInput("soon")

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).assertIsNotEnabled()
    }

    @Test
    fun choosingAMuscle_reportsItOnSave() {
        var saved: ExerciseEdit? = null
        show(customState(isEditing = true), onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.Muscle.FIELD).performClick()
        composeTestRule.onNodeWithText("Chest").performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).performScrollTo().performClick()

        assertEquals(MuscleGroup.CHEST, saved?.primaryMuscle)
    }

    @Test
    fun theWeightStep_isTypedInTheExercisesUnit_andSaved() {
        // ROADMAP N77: 5 in the step field, with this exercise read in kilograms, is 5000 g on disk.
        var saved: ExerciseEdit? = null
        show(customState(isEditing = true), onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_STEP).performTextInput("5")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).performScrollTo().performClick()

        assertEquals(5_000L, saved?.stepGrams)
    }

    @Test
    fun switchingTheUnit_carriesTheTypedStepRatherThanReinterpretingIt() {
        // ROADMAP B66: the step is a load in grams and the field's text is only how it reads, so a unit
        // change while the form is open has to carry the number. "5" typed in kilograms and then read
        // as pounds would store 2268 g — a different step from the one the lifter asked for.
        var saved: ExerciseEdit? = null
        show(customState(isEditing = true), onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_STEP).performTextInput("5")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_WEIGHT_UNIT_LB).performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).performScrollTo().performClick()

        // The field shows a tenth of a unit, so re-expressing 5 kg in pounds lands within one of those
        // tenths rather than exactly — 4990 g, which reads as "5 kg" and as "11 lb" either way. What
        // must not happen is the text being read as 5 lb, which is 2268 g: a different step entirely.
        val step = requireNotNull(saved?.stepGrams) { "the form must save a step" }
        assertTrue("re-expressed, not reinterpreted: $step g", step in 4_955L..5_045L)
    }

    @Test
    fun aStepOfZero_disablesSave() {
        // Zero is not a small step, it is no step: the ± buttons would do nothing and the warm-up
        // ramp divides by it, so the form refuses it rather than storing it (N77).
        show(customState(isEditing = true))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_STEP).performTextInput("0")

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).assertIsNotEnabled()
    }

    @Test
    fun aStoredStep_isShownAsThisExercisesOwn() {
        // The default names the step in force too, the shape the unit row uses: a bare number would
        // read as this exercise's own setting when it is the unit's (N77).
        show(ExerciseDetailUiState(isLoading = false, exercise = custom.copy(stepGrams = 5_000L)))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_WEIGHT_STEP).assertExists()
    }

    @Test
    fun theRetiredBack_isNotAnOption_andTheSplitGroupsAre() {
        // ROADMAP N75: a new exercise is never tagged with the group the taxonomy split three ways. The
        // seeded rows that carried it are moved by a migration; the value itself stays readable.
        // Addressed by the enum's name rather than its label (B71): the labels are enum literals today
        // and a translated one would otherwise read as a missing option.
        show(customState(isEditing = true))

        composeTestRule.onNodeWithTag(TestTags.Muscle.FIELD).performClick()

        composeTestRule.onNodeWithTag(TestTags.Muscle.option("BACK")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Muscle.option("LATS")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Muscle.option("UPPER_BACK")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Muscle.option("LOWER_BACK")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Muscle.option("ADDUCTORS")).assertExists()
    }

    @Test
    fun clearingTheName_disablesSave() {
        show(customState(isEditing = true))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_NAME).performTextClearance()

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).assertIsNotEnabled()
    }

    @Test
    fun cancel_reportsThatEditingStopped() {
        var cancelled = false
        show(customState(isEditing = true), onCancelEdit = { cancelled = true })

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CANCEL).performScrollTo().performClick()

        assertTrue("cancel should leave the form", cancelled)
    }

    @Test
    fun theReadOnlyView_showsEachAttributeOnItsOwnRow() {
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded))

        composeTestRule.onNodeWithText("Primary muscle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quads").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Barbell").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun anUnsetRest_readsAsTheAppDefault_ratherThanNothing() {
        // N5: unset means the 90 s default will actually run, so saying so beats a
        // bare dash the user has to interpret.
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded.copy(restSeconds = null)))

        composeTestRule.onNodeWithText("Default (1:30)").assertIsDisplayed()
    }

    @Test
    fun anExercisesOwnRest_isShownInItsPlace() {
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded.copy(restSeconds = 180)))

        composeTestRule.onNodeWithText("3:00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun anExercisesZeroRest_readsAsNone_ratherThanARestThatRanOut() {
        // ROADMAP N45: a stored zero is a deliberate "no rest", and 0:00 would read as one that
        // ended instead — two intentions, two words.
        // A cue is set so the only "None" on screen is the rest's: the cue's own empty state uses
        // the same word.
        show(
            ExerciseDetailUiState(
                isLoading = false,
                exercise = seeded.copy(restSeconds = 0, techniqueNote = "Brace"),
            ),
        )

        composeTestRule.onNodeWithText("None").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aZeroRest_isAccepted_asNoRest() {
        // ROADMAP N45: the field stops calling zero an error. It is a value, and Save takes it.
        var saved: ExerciseEdit? = null
        show(
            ExerciseDetailUiState(isLoading = false, isEditing = true, exercise = seeded),
            onSave = { saved = it },
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_REST).performTextInput("0")
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE).assertIsEnabled().performScrollTo().performClick()

        assertEquals(0, saved?.restSeconds)
    }

    @Test
    fun anUnsetCue_readsAsNone() {
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded.copy(techniqueNote = null)))

        composeTestRule.onNodeWithText("Technique cue").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("None").performScrollTo().assertIsDisplayed()
    }

    private companion object {
        val seeded = Exercise(
            id = "back-squat",
            name = "Back Squat",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
        )

        val custom = Exercise(
            id = "custom-1",
            name = "Sled Push",
            primaryMuscle = MuscleGroup.OTHER,
            equipment = Equipment.OTHER,
            movementPattern = MovementPattern.OTHER,
            isCustom = true,
        )

        val bench = Exercise(
            id = "cat-bench",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.OTHER,
            movementPattern = MovementPattern.HORIZONTAL_PUSH,
            rowKind = RowKind.CATEGORY,
        )
    }

    @Test
    fun aFiledMovement_namesItsFamily_andTakesTheMuscleFromIt() {
        // ROADMAP N95: the child reads the head's primary muscle rather than holding a copy, and a head that
        // has been removed still names it (N58's rule). Both are one row on the detail screen because they
        // answer one question — what is this filed under.
        show(
            ExerciseDetailUiState(
                isLoading = false,
                exercise = seeded.copy(parentId = "cat-bench"),
                head = bench,
                headName = "Bench Press",
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DETAIL_CATEGORY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        // The head says Chest and the row itself says Quads, so reading Chest is the inheritance working.
        composeTestRule.onNodeWithText("Chest").assertIsDisplayed()
    }

    @Test
    fun anUnfiledMovement_namesNoFamily() {
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DETAIL_CATEGORY).assertDoesNotExist()
        composeTestRule.onNodeWithText("Quads").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aVariation_isOfferedForAMovement() {
        // ROADMAP N95: a variation hangs under an exercise, so a movement offers one.
        show(ExerciseDetailUiState(isLoading = false, exercise = seeded))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_NEW_VARIATION).assertIsDisplayed()
    }

    @Test
    fun aVariation_isNotOfferedForACategory() {
        // It would be the third level the shape does not have: a variation hangs under an exercise.
        show(ExerciseDetailUiState(isLoading = false, exercise = bench))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_NEW_VARIATION).assertDoesNotExist()
    }

    @Test
    fun theVariationAction_isWiredThrough() {
        var variation = false
        show(
            ExerciseDetailUiState(isLoading = false, exercise = seeded),
            onNewVariation = { variation = true },
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_NEW_VARIATION).performClick()

        assertTrue(variation)
    }

    @Test
    fun theEditor_filesAMovementUnderAHead_andCanUnfileIt() {
        // *Move to category* is one field of the row rather than a flow of its own (N95), so the picker is
        // in the form and what it writes travels out with the save.
        var saved: ExerciseEdit? = null
        show(
            ExerciseDetailUiState(
                isLoading = false,
                isEditing = true,
                exercise = seeded,
                categoryOptions = listOf(bench),
            ),
            onSave = { saved = it },
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CATEGORY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CATEGORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.exerciseCategoryOption("cat-bench")).performClick()

        // Addressed by the semantics action rather than by a tap: the form is scrolled to the bottom in this
        // viewport, and what this asserts is the write the button performs rather than where it sits.
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_SAVE)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals("cat-bench", saved?.parentId)
    }

    @Test
    fun aCategorysEditor_offersNoCategoryPicker() {
        // A head sits at the top level, so there is nowhere to file it (N95).
        show(
            ExerciseDetailUiState(isLoading = false, isEditing = true, exercise = bench),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_EDIT_CATEGORY).assertDoesNotExist()
    }

    @Test
    fun aFailedRead_isShownAsAFailure_notAsAMissingExercise() {
        // ROADMAP B4 and the distinction that matters: "we could not read it" and
        // "it is not there" are different sentences, and the state says which.
        show(ExerciseDetailUiState(isLoading = false, error = DataError.Storage(IOException("locked"))))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_READ_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText(notFoundMessage()).assertDoesNotExist()
    }

    /** The "no such exercise" line, read from resources so wording can change. */
    private fun notFoundMessage(): String =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.exercise_detail_not_found)
}
