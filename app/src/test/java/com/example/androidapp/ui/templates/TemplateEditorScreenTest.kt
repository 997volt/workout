package com.example.androidapp.ui.templates

import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One template's editor (ROADMAP N3).
 *
 * The reorder entries are the part worth pinning down: they act on *positions*, so the direction each
 * one sends has to be the direction the user sees. Since N71 they live behind the row's ⋮, the same
 * menu the live workout draws, so reaching one means opening it first.
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
        val onAddWarmUpSets: (String, Long) -> Unit = { _, _ -> },
        val onSaveExercisePlan: (String, Int?, String?, Int?) -> Unit = { _, _, _, _ -> },
    )

    /**
     * The block's own *Add set* (N81), reached the way a thumb reaches it.
     *
     * It is the foot of a block that already carries the plan's lines and the exercise's fields, so with
     * a set planned it sits below the window's fold in this test — the list is scrolled to it rather than
     * clicked where it is not.
     */
    private fun addSet() {
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanAdd("te1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).performClick()
    }

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

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateMove("te1", up = false)).performClick()

        assertEquals("te1" to 1, moved)
    }

    @Test
    fun moveUp_sendsTheExerciseOneSlotUp() {
        var moved: Pair<String, Int>? = null
        setScreen(actions = Actions(onMoveExercise = { id, delta -> moved = id to delta }))

        // The second row's ⋮ is under the extended FAB after the minimal scroll, so the menu is opened
        // through its semantics action: a swallowed tap would read as a missing entry (N71).
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateMenu("te2")))
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te2"))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.templateMove("te2", up = true)).performClick()

        assertEquals("te2" to -1, moved)
    }

    @Test
    fun atTheEdges_theRelevantMoveEntryIsNotOffered() {
        setScreen()

        // There is nowhere for the first exercise to go up, or the last to go down, so the entry is
        // absent rather than present-and-inert — B28's shape, now through the shared menu (N71).
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateMove("te1", up = true)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateMove("te1", up = false)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateMenu("te2")))
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te2"))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.templateMove("te2", up = false)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateMove("te2", up = true)).assertExists()
    }

    @Test
    fun removingAnExercise_asksFirst_andReportsThatRowOnConfirm() {
        // ROADMAP N71: a template's removal keeps the guard the workout's has (B2) — the planned sets
        // go with the row and there is no undo to reach for.
        var removed: String? = null
        setScreen(actions = Actions(onRemoveExercise = { removed = it }))

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateRemove("te1")).performClick()
        assertNull("the dialog must come before the write", removed)

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CONFIRM).performClick()

        assertEquals("te1", removed)
    }

    @Test
    fun cancellingARemoval_writesNothing() {
        var removed: String? = null
        setScreen(actions = Actions(onRemoveExercise = { removed = it }))

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateRemove("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CANCEL).performClick()

        assertNull(removed)
    }

    @Test
    fun theFirstExercise_isNotOfferedTheSupersetEntry() {
        // B28's row-0 exclusion, through the shared menu (N71): nothing above it to pair with.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()

        composeTestRule.onNodeWithTag(TestTags.supersetToggle("te1")).assertDoesNotExist()
    }

    @Test
    fun theSecondExercise_isOfferedTheSupersetEntry() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateMenu("te2")))
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te2"))
            .performSemanticsAction(SemanticsActions.OnClick)

        composeTestRule.onNodeWithTag(TestTags.supersetToggle("te2")).assertExists()
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

    /**
     * One exercise with a working weight, so the plan dialog can offer its warm-up ramp (N28), and a
     * step the movement may or may not name (N77).
     */
    private fun rampedExercise(stepGrams: Long?) = TemplateEditorUiState(
        isLoading = false,
        template = WorkoutTemplate(id = "t1", name = "Legs", exerciseCount = 1),
        exercises = listOf(
            TemplateExercise(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                exerciseName = "Back Squat",
                primaryMuscle = MuscleGroup.QUADS,
                equipment = Equipment.BARBELL,
                stepGrams = stepGrams,
                sets = listOf(
                    TemplateSet(
                        id = "ts1",
                        templateExerciseId = "te1",
                        setIndex = 0,
                        targetWeightGrams = 100_000L,
                        targetRepsMin = 5,
                        targetRepsMax = 5,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun theWarmUpRamp_isBuiltFromTheMovementsOwnStep() {
        // ROADMAP N77: the ramp rounds to the step the exercise actually loads in, so a machine that
        // jumps 5 kg gets a ramp of 5 kg steps rather than the unit's 2.5 kg ones.
        var askedStep: Long? = null
        setScreen(
            state = rampedExercise(stepGrams = 5_000L),
            actions = Actions(onAddWarmUpSets = { _, step -> askedStep = step }),
        )

        // The ramp moved into the row's ⋮ (N81): the plan is on the block now, and this is the one
        // action that is not — it writes rather than reads, and it is the template's alone.
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateAddWarmUps("te1")).performClick()

        assertEquals(5_000L, askedStep)
    }

    @Test
    fun theWarmUpRamp_fallsBackToTheUnitsStep_whenTheMovementNamesNone() {
        var askedStep: Long? = null
        setScreen(
            state = rampedExercise(stepGrams = null),
            actions = Actions(onAddWarmUpSets = { _, step -> askedStep = step }),
        )

        // The ramp moved into the row's ⋮ (N81): the plan is on the block now, and this is the one
        // action that is not — it writes rather than reads, and it is the template's alone.
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateAddWarmUps("te1")).performClick()

        assertEquals(Weight.DEFAULT_STEP_GRAMS, askedStep)
    }

    /** A working set at 100 kg with two 20 kg drops after it, so the ladder is 80 then 60 (N79). */
    private fun droppingExercise() = TemplateEditorUiState(
        isLoading = false,
        template = WorkoutTemplate(id = "t1", name = "Legs", exerciseCount = 1),
        exercises = listOf(
            TemplateExercise(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                exerciseName = "Back Squat",
                primaryMuscle = MuscleGroup.QUADS,
                equipment = Equipment.BARBELL,
                sets = listOf(
                    TemplateSet(
                        id = "ts1",
                        templateExerciseId = "te1",
                        setIndex = 0,
                        targetWeightGrams = 100_000L,
                        targetRepsMax = 5,
                    ),
                    TemplateSet(
                        id = "ts2",
                        templateExerciseId = "te1",
                        setIndex = 1,
                        role = SetType.DROP,
                        dropValueGrams = 20_000L,
                    ),
                    TemplateSet(
                        id = "ts3",
                        templateExerciseId = "te1",
                        setIndex = 2,
                        role = SetType.DROP,
                    ),
                ),
            ),
        ),
    )

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
    fun oneRpeTargetPerExercise_isEdited_aboveTheCue() {
        // ROADMAP N59, amended by N80: the effort is one number for the exercise, shown next to the
        // rest — not a field on each planned set, and no longer in the cue's row. 9.5 is 19
        // half-points (N6).
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
    fun theCue_getsALineOfItsOwn_insteadOfASliverOfTheNumbersRow() {
        // ROADMAP N80. The cue took `weight(1f)` between two fixed-width number fields and the save,
        // so a phone left it about a word wide with `singleLine` on top. Both halves are asserted,
        // because the width is the point and the position is the decision: it is wider than the two
        // numbers together, it sits below them, and the save stayed up on their row rather than
        // following the cue down — which keeps the block's foot free for the sets' own Add set.
        setScreen(
            state = twoExercises.copy(exercises = listOf(twoExercises.exercises.first())),
        )

        val rest = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_FIELD).getUnclippedBoundsInRoot()
        val rpe = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).getUnclippedBoundsInRoot()
        val cue = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_CUE_FIELD).getUnclippedBoundsInRoot()
        val save = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE).getUnclippedBoundsInRoot()

        assertTrue("the cue shares the numbers' row", cue.top >= rest.bottom)
        assertTrue(
            "the cue is no wider than the two numbers it used to sit between",
            cue.right - cue.left >= (rest.right - rest.left) + (rpe.right - rpe.left),
        )
        assertTrue("the save followed the cue down out of the numbers' row", save.bottom <= cue.top)
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

        addSet()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("100")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).assertTextContains("3")
    }
    @Test
    fun addingADropSet_asksForTheValueItTakesOff_theAnchor() {
        // ROADMAP N79: a drop rung has no weight of its own to type — its load is the anchor less the
        // run's value — so the dialog swaps the weight field for the one number a run is authored with.
        setScreen(state = rampedExercise(stepGrams = null))

        addSet()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.DROP.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertDoesNotExist()
    }

    @Test
    fun aPlansFirstSet_cannotBeArmedWithARung() {
        // ROADMAP B64: a rung hangs off the set above it, so the plan's picker does not offer one where
        // there is nothing above — the write boundary would refuse the save, and the project's rule is
        // that a control which cannot write is worse than no control (N67). The exercise here has no
        // planned sets, so the set being added is its first — and since the block's Add set is tagged per
        // exercise (B76), a second exercise can stay on screen while this one is the one addressed.
        setScreen(state = twoExercises)

        addSet()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()

        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.DROP.name)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.CLUSTER.name)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.NORMAL.name)).assertExists()
    }

    @Test
    fun aClusterRung_asksForNeitherAWeight_norReps() {
        // A cluster repeats the anchor's load and answers to the anchor's reps, so it carries neither
        // (ROADMAP N79).
        setScreen(state = rampedExercise(stepGrams = null))

        addSet()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.CLUSTER.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MIN).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertDoesNotExist()
    }

    @Test
    fun aRungWithNoDerivableLoad_saysSo_insteadOfShowingNothing() {
        // ROADMAP B71: where a rung's load cannot be derived — the run names no value yet, or the anchor
        // has nothing to take one off — the row says so rather than standing empty, which reads as a bug
        // and leaves an assisted-anchor drop indistinguishable from a cluster.
        val runWithoutAValue = droppingExercise().let { state ->
            state.copy(
                exercises = listOf(
                    state.exercises.first().copy(
                        sets = state.exercises.first().sets.map {
                            if (it.role == SetType.DROP) it.copy(dropValueGrams = null) else it
                        },
                    ),
                ),
            )
        }
        setScreen(state = runWithoutAValue)

        // Nothing is opened: N81 put the plan's lines on the block, which is the point of the change.
        // Addressed by the row, not by the sentence: every rung of the run derives nothing, so the text
        // alone would match more than one row.
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts2"))
            .assertTextContains("no load to drop", substring = true)
    }

    @Test
    fun editingADropSet_opensOnTheValueItAlreadyTakesOff() {
        // ROADMAP B61: the dialog is the only place a run's value is authored, so reopening a drop row
        // has to show what the plan stored. An empty field is not a cosmetic loss: the dialog's own
        // guard disables Save on a first rung with no value, so the row becomes uneditable — note
        // included — and the number reads as one the app dropped.
        setScreen(state = droppingExercise())

        // Tapping the line is the way in, exactly as it was in the dialog the line used to live in.
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts2")).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertTextContains("20")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).assertIsEnabled()
    }

    @Test
    fun eachRungOfThePlan_statesWhatItLoads() {
        // The ladder is derived, so the plan says what it comes to rather than leaving the reader to
        // subtract: 100 with a 20 kg value is 80, then 60 (ROADMAP N79) — and since N81 that reads on
        // the block itself, with nothing to open.
        setScreen(state = droppingExercise())

        // The row's line joins its parts, so each is asserted as part of it.
        composeTestRule.onNodeWithText("80 kg", substring = true).assertExists()
        composeTestRule.onNodeWithText("60 kg", substring = true).assertExists()
        composeTestRule.onNodeWithText("20 kg drop", substring = true).assertExists()
    }

    @Test
    fun thePlansSets_readOnTheBlock_withNothingOpened() {
        // This is N81's whole change: the sets were a count behind *Planned sets · 3*, so the plan's
        // roles, loads and deletes were all behind a tap. Nothing is clicked here, and the line, the
        // rung it derives and the delete that takes it are all *readable* — which is why these are
        // `assertIsDisplayed` rather than `assertExists` (B77): a node composed below the fold exists too.
        setScreen(state = droppingExercise())

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanSet("ts1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts1")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanSet("ts2")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts2")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.templatePlanRemove("ts2")).assertIsDisplayed()
    }

    @Test
    fun anExerciseWithNoSets_saysSoUnderItsHeader() {
        // The dialog's sentence is the block's now (N81), and the button that answers it is at the
        // foot of the same block: an empty plan is the one place the two are read together.
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(twoExercises.exercises.first().copy(sets = emptyList())),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertIsDisplayed()
    }

    @Test
    fun eachExerciseBlock_carriesItsOwnAddSet_andEmptyNote() {
        // ROADMAP B76: the block's controls belong to the exercise, so two planned exercises answer to two
        // distinct tags. Before, both blocks applied one tag — two nodes under it — so neither could be
        // addressed, and `performScrollToNode` throws when a matcher finds more than one node.
        setScreen(state = twoExercises)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanAdd("te1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertExists()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanAdd("te2")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te2")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te2")).assertExists()
    }

    @Test
    fun theWarmUpRamp_isNotOffered_whenThereIsNothingToRampFrom() {
        // ROADMAP N28, B50, and N53's rule the entry now follows: a bodyweight set has no weight to take
        // a fraction of, so the menu omits the entry rather than offering one that writes nothing. The
        // action is supplied here, so what suppresses the entry is the ramp and not the caller.
        val bodyweight = rampedExercise(stepGrams = null).let { state ->
            state.copy(
                exercises = listOf(
                    state.exercises.first().copy(
                        sets = state.exercises.first().sets.map { it.copy(targetWeightGrams = null) },
                    ),
                ),
            )
        }
        setScreen(state = bodyweight, actions = Actions(onAddWarmUpSets = { _, _ -> }))

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()

        // The menu really opened, so the absence below is the entry's and not a popup that never drew (B77).
        composeTestRule.onNodeWithTag(TestTags.templateRemove("te1")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.templateAddWarmUps("te1")).assertDoesNotExist()
    }

    @Test
    fun aHalfStepTargetRpe_readsAsALifterWritesIt() {
        // ROADMAP B6, carried over from the plan dialog's own test when N81 deleted that dialog: a
        // planned set stores its effort in half-points, and handing the count straight to the marker
        // rendered a plan saying 9.5 as *RPE 19*. Displayed, not merely composed (B77) — the guard is
        // that a lifter can read it, which `assertExists` does not say.
        setScreen(state = twoExercises.copy(exercises = listOf(targetRpeSet(19))))

        composeTestRule.onNodeWithText("RPE 9.5", substring = true).assertIsDisplayed()
    }

    @Test
    fun aWholeTargetRpe_hasNoTrailingZero() {
        // The third guard the deleted plan-dialog test held, re-homed because the review found it had gone
        // (B77): a whole effort prints as *RPE 8*, not *RPE 8.0* — 16 half-points — for the same reason the
        // half-point case exists.
        setScreen(state = twoExercises.copy(exercises = listOf(targetRpeSet(16))))

        composeTestRule.onNodeWithText("RPE 8", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("RPE 8.0", substring = true).assertDoesNotExist()
    }

    @Test
    fun aPlannedWarmUp_printsNoEffort_evenThoughTheRowStillCarriesOne() {
        // ROADMAP N67, also carried over: a warm-up records no effort, and a plan written before that
        // rule can still carry a legacy per-set target. Printing it beside *Warm-up* read as a number
        // the ramp had been judged against, so the line leaves it off. The row is anchored first, or a
        // regression that dropped the line altogether would satisfy the absence below (B77).
        setScreen(state = twoExercises.copy(exercises = listOf(targetRpeSet(17, role = SetType.WARMUP))))

        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts1")).assertIsDisplayed()
        composeTestRule.onNodeWithText("RPE 8.5", substring = true).assertDoesNotExist()
    }

    /** A first exercise carrying one planned set, so a summary line can be read on its own. */
    private fun targetRpeSet(halves: Int, role: SetType = SetType.NORMAL) =
        twoExercises.exercises.first().copy(
            sets = listOf(
                TemplateSet(
                    id = "ts1",
                    templateExerciseId = "te1",
                    setIndex = 0,
                    role = role,
                    targetWeightGrams = 140_000L,
                    targetRepsMax = 2,
                    targetRpeHalves = halves,
                ),
            ),
        )
}
