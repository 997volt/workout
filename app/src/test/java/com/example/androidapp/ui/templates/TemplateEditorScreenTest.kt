package com.example.androidapp.ui.templates

import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One template's editor (ROADMAP N3).
 *
 * The reorder buttons are the part worth pinning down: they act on *positions*, so
 * the direction each one sends has to be the direction the user sees.
 */
@RunWith(AndroidJUnit4::class)
class TemplateEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The editor's events, grouped: a Compose screen takes one callback per action,
     * and a screen setter that mirrors all of them stops reading as a call site.
     */
    private data class Actions(
        val onRename: (String) -> Unit = {},
        val onRemoveExercise: (String) -> Unit = {},
        val onMoveExercise: (String, Int) -> Unit = { _, _ -> },
        val onDeleteTemplate: () -> Unit = {},
        val onAddExercise: () -> Unit = {},
        val onAddWarmUpSets: (String) -> Unit = {},
        val onSaveExercisePlan: (String, Int?, String?, Int?) -> Unit = { _, _, _, _ -> },
    )

    private fun setScreen(
        state: TemplateEditorUiState = twoExercises,
        actions: Actions = Actions(),
    ) {
        composeTestRule.setContent {
            TemplateEditorScreen(
                state = state,
                onRename = actions.onRename,
                onRemoveExercise = actions.onRemoveExercise,
                onMoveExercise = actions.onMoveExercise,
                onDeleteTemplate = actions.onDeleteTemplate,
                onAddExercise = actions.onAddExercise,
                onAddWarmUpSets = actions.onAddWarmUpSets,
                onSaveExercisePlan = actions.onSaveExercisePlan,
                onBack = {},
            )
        }
    }

    @Test
    fun theExercises_areListedInOrder_andNumbered() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasText("2. Bench Press"))
        composeTestRule.onNodeWithText("1. Back Squat").assertExists()
        composeTestRule.onNodeWithText("2. Bench Press").assertExists()
        composeTestRule.onNodeWithText("Quads · Barbell").assertExists()
    }

    @Test
    fun anEmptyTemplate_saysWhatToDoNext() {
        setScreen(
            TemplateEditorUiState(
                isLoading = false,
                template = WorkoutTemplate(id = "t1", name = "Push day"),
                exercises = emptyList(),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NO_EXERCISES).assertExists()
    }

    @Test
    fun moveDown_sendsTheExerciseOneSlotDown() {
        var moved: Pair<String, Int>? = null
        setScreen(actions = Actions(onMoveExercise = { id, delta -> moved = id to delta }))

        composeTestRule.onNodeWithTag(TestTags.templateMove("te1", up = false)).performClick()

        assertEquals("te1" to 1, moved)
    }

    @Test
    fun moveUp_sendsTheExerciseOneSlotUp() {
        var moved: Pair<String, Int>? = null
        setScreen(actions = Actions(onMoveExercise = { id, delta -> moved = id to delta }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateMove("te2", up = true)))
        composeTestRule.onNodeWithTag(TestTags.templateMove("te2", up = true)).performClick()

        assertEquals("te2" to -1, moved)
    }

    @Test
    fun atTheEdges_theRelevantMoveButtonIsDisabled() {
        setScreen()

        // There is nowhere for the first exercise to go up, or the last to go down.
        composeTestRule.onNodeWithTag(TestTags.templateMove("te1", up = true)).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateMove("te2", up = false)))
        composeTestRule.onNodeWithTag(TestTags.templateMove("te2", up = false)).assertIsNotEnabled()
    }

    @Test
    fun removingAnExercise_reportsThatRow() {
        var removed: String? = null
        setScreen(actions = Actions(onRemoveExercise = { removed = it }))

        composeTestRule.onNodeWithTag(TestTags.templateRemove("te1")).performClick()

        assertEquals("te1", removed)
    }

    @Test
    fun renaming_commitsTheTrimmedName_onTheSaveAction() {
        var renamed: String? = null
        setScreen(actions = Actions(onRename = { renamed = it }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NAME_FIELD)
            .performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NAME_FIELD)
            .performTextInput("Leg day")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NAME_SAVE).performClick()

        assertEquals("Leg day", renamed)
    }

    @Test
    fun anUnchangedName_cannotBeSavedAgain() {
        setScreen()

        // The field starts at the stored name, so there is nothing to commit.
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NAME_SAVE).assertIsNotEnabled()
    }

    @Test
    fun addingAnExercise_asksForThePicker() {
        var asked = false
        setScreen(actions = Actions(onAddExercise = { asked = true }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_ADD_EXERCISE).performClick()

        assertTrue(asked)
    }

    @Test
    fun deleting_asksForConfirmationFirst() {
        var deleted = false
        setScreen(actions = Actions(onDeleteTemplate = { deleted = true }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_DELETE).performClick()
        assertTrue("the dialog must come before the write", !deleted)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_DELETE_CONFIRM).performClick()

        assertTrue(deleted)
    }

    private companion object {
        val twoExercises = TemplateEditorUiState(
            isLoading = false,
            template = WorkoutTemplate(id = "t1", name = "Push day", exerciseCount = 2),
            exercises = listOf(
                TemplateExercise(
                    id = "te1",
                    templateId = "t1",
                    exerciseId = "back-squat",
                    position = 0,
                    exerciseName = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    equipment = Equipment.BARBELL,
                ),
                TemplateExercise(
                    id = "te2",
                    templateId = "t1",
                    exerciseId = "bench-press",
                    position = 1,
                    exerciseName = "Bench Press",
                    primaryMuscle = MuscleGroup.CHEST,
                    equipment = Equipment.BARBELL,
                ),
            ),
        )
    }

    @Test
    fun oneRpeTargetPerExercise_isEdited_besideTheRestAndCue() {
        // ROADMAP N59, amended: the effort is one number for the exercise, shown next to the rest and
        // cue — not a field on each planned set. 9.5 is 19 half-points (N6).
        var saved: Triple<Int?, String?, Int?>? = null
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(twoExercises.exercises.first().copy(targetRpeHalves = 16)),
            ),
            actions = Actions(onSaveExercisePlan = { _, rest, cue, rpe -> saved = Triple(rest, cue, rpe) }),
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).assertTextContains("8")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).performTextInput("9.5")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE).performClick()

        assertEquals(Triple(null, null, 19), saved)
    }

    @Test
    fun addSet_startsFromTheLastPlannedSet() {
        // ROADMAP N46: Duplicate is gone and Add set prefills from the set before it, so a plan is
        // extended by confirming rather than by retyping — and any count, not just a doubling.
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(
                    twoExercises.exercises.first().copy(
                        sets = listOf(
                            TemplateSet(
                                id = "s1",
                                templateExerciseId = "te1",
                                setIndex = 0,
                                targetWeightGrams = 100_000L,
                                targetRepsMax = 3,
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_PLAN_ROW).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_PLAN_ADD).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("100")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).assertTextContains("3")
    }

}
