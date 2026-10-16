package com.example.androidapp.ui.templates

import com.google.common.truth.Truth.assertThat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
        val onOpenExercise: (String) -> Unit = {},
        val onMoveExercise: (String, Int) -> Unit = { _, _ -> },
        val onDeleteTemplate: () -> Unit = {},
        val onAddExercise: () -> Unit = {},
        val onAddWarmUpSets: (String, Long) -> Unit = { _, _ -> },
        val onSaveExercisePlan: (String, Int?, String?, Int?) -> Unit = { _, _, _, _ -> },
        val onRemoveSet: (String) -> Unit = {},
        val onUndoRemoveSet: () -> Unit = {},
        /** Keeping a version, and reading one back (ROADMAP N134). */
        val onKeepVersion: () -> Unit = {},
        val onDismissVersionNotice: () -> Unit = {},
        val onOpenVersion: (WorkoutTemplate) -> Unit = {},
        val onDismissVersion: () -> Unit = {},
    )

    /**
     * Opens one block (ROADMAP N91), which every read of its sets, fields or foot now needs first.
     *
     * The tap is the control the lifter uses — the exercise's own name — so a test that reads a block's
     * contents goes through the same door rather than past it.
     */
    private fun openBlock(exerciseId: String) {
        composeTestRule.onNodeWithTag(TestTags.templateFold(exerciseId)).performClick()
    }

    /**
     * The block's own *Add set* (N81), reached the way a thumb reaches it.
     *
     * It sits with the sets it extends (N99) rather than at the foot of the block; the list is scrolled to
     * it, so the test does not depend on where in the viewport it falls.
     */
    private fun addSet() {
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanAdd("te1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).performClick()
    }

    private fun setScreen(
        state: TemplateEditorUiState = twoExercises,
        actions: Actions = Actions(),
        openVersion: TemplateVersionPlan? = null,
        versionKept: Boolean = false,
    ) {
        composeTestRule.setContent {
            TemplateEditorScreen(
                state = state,
                onRename = actions.onRename,
                onRemoveExercise = actions.onRemoveExercise,
                onOpenExercise = actions.onOpenExercise,
                onMoveExercise = actions.onMoveExercise,
                onDeleteTemplate = actions.onDeleteTemplate,
                onAddExercise = actions.onAddExercise,
                onAddWarmUpSets = actions.onAddWarmUpSets,
                onSaveExercisePlan = actions.onSaveExercisePlan,
                onRemoveSet = actions.onRemoveSet,
                onUndoRemoveSet = actions.onUndoRemoveSet,
                versionUi = TemplateVersionUi(
                    versions = state.versions,
                    openVersion = openVersion,
                    versionKept = versionKept,
                    onKeepVersion = actions.onKeepVersion,
                    onDismissVersionNotice = actions.onDismissVersionNotice,
                    onOpenVersion = actions.onOpenVersion,
                    onDismissVersion = actions.onDismissVersion,
                ),
                onBack = {},
            )
        }
    }

    @Test
    fun theEditorsMenu_keepsAVersion() {
        // ROADMAP N134: *Keep a version* is pressed at the moment the lifter decides to change
        // something, so it sits in the editor's own overflow rather than on a surface of its own.
        var kept = false
        setScreen(actions = Actions(onKeepVersion = { kept = true }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_KEEP_VERSION).performClick()

        assertTrue("the menu's item is the write", kept)
    }

    @Test
    fun theVersionsItem_opensTheList_andAnEmptyOneSaysWhatToDo() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS_DIALOG).assertExists()
        composeTestRule.onNodeWithText("No versions yet. Keep one before you change this plan.")
            .assertIsDisplayed()
    }

    @Test
    fun aKeptVersion_isOpenedFromTheList_andReportsWhichOne() {
        val version = WorkoutTemplate(id = "v1", name = "Push day", versionOf = "t1")
        var opened: WorkoutTemplate? = null
        setScreen(
            state = twoExercises.copy(versions = listOf(version)),
            actions = Actions(onOpenVersion = { opened = it }),
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSIONS).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateVersion("v1")).performClick()

        assertEquals(version, opened)
    }

    @Test
    fun theReadOnlyPlan_showsTheExercisesTheVersionPlanned() {
        val plan = TemplateVersionPlan(
            version = WorkoutTemplate(id = "v1", name = "Push day", versionOf = "t1"),
            exercises = listOf(twoExercises.exercises.first()),
            isLoading = false,
        )
        setScreen(openVersion = plan)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSION_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSION_TITLE).assertTextContains("Push day")
        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        // Nothing on it writes: the editor's own *Add set* for that exercise is not here.
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertDoesNotExist()
    }

    @Test
    fun aVersionWhosePlanIsEmpty_saysSo_ratherThanDrawingNothing() {
        val plan = TemplateVersionPlan(
            version = WorkoutTemplate(id = "v1", name = "Push day", versionOf = "t1"),
            exercises = emptyList(),
            isLoading = false,
        )
        setScreen(openVersion = plan)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSION_EMPTY).assertExists()
    }

    @Test
    fun theReadOnlyPlansClose_isTheDismissal() {
        var dismissed = false
        val plan = TemplateVersionPlan(
            version = WorkoutTemplate(id = "v1", name = "Push day", versionOf = "t1"),
            exercises = emptyList(),
            isLoading = false,
        )
        setScreen(openVersion = plan, actions = Actions(onDismissVersion = { dismissed = true }))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_VERSION_CLOSE).performClick()

        assertTrue("closing the plan is the one control it has", dismissed)
    }

    @Test
    fun aPlannedExercisesBlock_startsFolded_andTheNameOpensIt() {
        // ROADMAP N91: a plan of five exercises was a long scroll of controls with the names — the thing
        // the screen is scanned by — lost among them. The row is the whole of a block until its name is
        // tapped, and everything below it folds together: the sets, the control that adds one, and the
        // plan's own fields.
        setScreen(state = twoExercises)

        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Save rest, RPE and cue").assertDoesNotExist()

        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertIsDisplayed()
        // Read by existence rather than by display: the test's viewport is short, and what this asserts is
        // that the fields came with the block, not that they fit on this screen. Addressed by the save's
        // own content description, which is what a screen reader announces for it.
        composeTestRule.onNodeWithContentDescription("Save rest, RPE and cue").assertExists()

        // And tapping the name again folds it back.
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertDoesNotExist()
    }

    @Test
    fun aBlocksName_statesWhetherItIsOpen() {
        // The row is a control now, so its state has to be announced (B21's rule): without it a screen
        // reader reads the same name whether the block is open or folded, and the tap does a different
        // thing in each case.
        setScreen(state = twoExercises)

        composeTestRule.onNodeWithTag(TestTags.templateFold("te1"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Folded"))

        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templateFold("te1"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Open"))
    }

    @Test
    fun anExerciseAddedAfterTheScreenOpened_opens() {
        // N91's other half: only the blocks that were already there start folded, so the exercise just
        // added can take its first set without a second tap. The state is the list's, so this hands the
        // screen a second exercise after it has drawn the first.
        val state = mutableStateOf(
            twoExercises.copy(exercises = listOf(twoExercises.exercises.first())),
        )
        composeTestRule.setContent {
            TemplateEditorScreen(
                state = state.value,
                onRename = {},
                onRemoveExercise = {},
                onOpenExercise = {},
                onMoveExercise = { _, _ -> },
                onDeleteTemplate = {},
                onAddExercise = {},
                onAddWarmUpSets = { _, _ -> },
                onSaveExercisePlan = { _, _, _, _ -> },
                onBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertDoesNotExist()

        state.value = twoExercises
        composeTestRule.waitForIdle()

        // It opens on arrival: the block is fully drawn, foot and all, with no tap on it. Asserted as
        // existing rather than displayed, because the foot of a just-added block can sit below the fold.
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te2")).assertExists()
        // And the one that was already there stays folded.
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertDoesNotExist()
    }

    @Test
    fun theFirstExerciseAddedToAnEmptyTemplate_opens() {
        // B89: the seed was keyed on `ids.isEmpty()`, so when an empty editor gained its first exercise the
        // key flipped, the seed was rebuilt to include that id, and the one row N91 says opens was folded.
        val state = mutableStateOf(twoExercises.copy(exercises = emptyList()))
        composeTestRule.setContent {
            TemplateEditorScreen(
                state = state.value,
                onRename = {},
                onRemoveExercise = {},
                onOpenExercise = {},
                onMoveExercise = { _, _ -> },
                onDeleteTemplate = {},
                onAddExercise = {},
                onAddWarmUpSets = { _, _ -> },
                onSaveExercisePlan = { _, _, _, _ -> },
                onBack = {},
            )
        }
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_NO_EXERCISES).assertIsDisplayed()

        state.value = twoExercises.copy(exercises = listOf(twoExercises.exercises.first()))
        composeTestRule.waitForIdle()

        // Its *Add set* is drawn, which is the whole point of the row opening.
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertExists()
    }

    @Test
    fun removingAPlannedSet_offersTheWayBack() {
        // ROADMAP N90: a mis-tap on a set's delete used to be unrecoverable from the screen, with
        // nothing said. The ViewModel holds the set that went, and the box names it and offers Undo,
        // which reports the tap upward.
        var undone = false
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(exerciseWithAPlannedSet()),
                pendingUndo = TemplateSet(
                    id = "ts1",
                    templateExerciseId = "te1",
                    setIndex = 0,
                ),
            ),
            actions = Actions(onUndoRemoveSet = { undone = true }),
        )

        composeTestRule.onNodeWithText("Set deleted").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()

        assertTrue(undone)
    }

    @Test
    fun aPlannedSetsDelete_asksForThatSetToGo() {
        // The other half of N90: what the tap reports upward is the set's own id, which is what the
        // ViewModel turns into a removal and then into an undo.
        var removed: String? = null
        setScreen(
            state = twoExercises.copy(exercises = listOf(exerciseWithAPlannedSet())),
            actions = Actions(onRemoveSet = { removed = it }),
        )
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanSet("ts1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanRemove("ts1")).performClick()

        assertEquals("ts1", removed)
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

    /**
     * Two ordinary working sets, so the second can be turned into the drop that opens a run (B121).
     *
     * [droppingExercise] cannot serve: its second row is already a rung, and the case that went wrong is a
     * row that **exists**, is not a rung, and is being changed into one.
     */
    private fun twoOrdinarySets() = droppingExercise().let { state ->
        state.copy(
            exercises = listOf(
                state.exercises.first().copy(
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
                            targetWeightGrams = 80_000L,
                            targetRepsMax = 5,
                        ),
                    ),
                ),
            ),
        )
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
    fun oneRpeTargetPerExercise_isEdited_inTheBlocksFoot() {
        // ROADMAP N59, amended by N80 and laid out by N111: the effort is one number for the exercise,
        // in the block's foot rather than a field on each planned set. 9.5 is 19 half-points (N6). It
        // stays **typeable** beside its ± pair, because every planned field is optional and a stepper
        // alone could never put a target back to none (N14).
        var saved: Triple<Int?, String?, Int?>? = null
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(twoExercises.exercises.first().copy(targetRpeHalves = 16)),
            ),
            actions = Actions(onSaveExercisePlan = { _, rest, cue, rpe -> saved = Triple(rest, cue, rpe) }),
        )
        openBlock("te1")

        // The cue is read above the numbers now (N99), so this row — and the save on it — sits at the foot of
        // a block taller than this test's viewport. The save is therefore reached through its semantics
        // action, the way the second row's ⋮ already is (N71), because a swallowed tap would read as a save
        // that did nothing. The field is scrolled to first, so it takes the text.
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.TEMPLATE_EXERCISE_RPE))
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).assertTextContains("8")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).performTextInput("9.5")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(Triple(null, null, 19), saved)
    }

    @Test
    fun theTargetRpe_stepsByAHalfPoint_andStopsAtTheScale() {
        // ROADMAP N111: the plan's effort moves the way a logged set's does — half a point a tap, and
        // clamped at both ends of the scale rather than wrapped, so holding a button down cannot leave
        // 1–10 (the shape N59 gave the workout's own stepper).
        var saved: Triple<Int?, String?, Int?>? = null
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(twoExercises.exercises.first().copy(targetRpeHalves = 18)),
            ),
            actions = Actions(onSaveExercisePlan = { _, rest, cue, rpe -> saved = Triple(rest, cue, rpe) }),
        )
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.TEMPLATE_EXERCISE_INCREASE_RPE))
        // The ± pair sits low in a block taller than this test's viewport, so the buttons are reached
        // through their semantics action the way the save below them already is (N71): a swallowed tap
        // would read as a step that did nothing, which is the one thing this test is about.
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_INCREASE_RPE)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).assertTextContains("9.5")

        // Held down past the bottom of the scale: 9.5, then 1.0 and no further. `substring = false`
        // because the field's own label carries a 1 and a 10, and a substring match would pass on
        // either of them rather than on the value.
        repeat(20) {
            composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_DECREASE_RPE)
                .performSemanticsAction(SemanticsActions.OnClick)
        }
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).assertTextContains("1", substring = false)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(Triple(null, null, 2), saved)
    }

    /**
     * The effort, the rest and the save are one line, in that order (ROADMAP N116).
     *
     * The three are one statement about one set, so a tag-level assertion cannot see the thing worth
     * protecting: that they are on the same line rather than stacked. Bounds can — the idiom the workout
     * screen uses for *Done* — and the failure to catch is the row silently wrapping.
     */
    @Test
    fun theCue_getsALineOfItsOwn_andIsReadAboveTheNumbers() {
        // ROADMAP N80, moved by N99, re-laid-out by N111 and re-laid-out again by N116. The cue took
        // `weight(1f)` between two fixed-width number fields and the save, so a phone left it about a word wide
        // with `singleLine` on top; it is read *above* the numbers because the block reads in the order it is
        // filled in. Below it the effort, the rest and the save share one line in that order (N116), which is
        // where the row of its own N111 gave the RPE — and N68's squeeze behind it — were superseded: the label
        // is the field's own floating one rather than a leading word, so the stepper fits beside the rest once
        // it takes the flexible width and the rest field narrows.
        setScreen(
            state = twoExercises.copy(exercises = listOf(twoExercises.exercises.first())),
        )
        openBlock("te1")

        val rest = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_FIELD).getUnclippedBoundsInRoot()
        val rpe = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE).getUnclippedBoundsInRoot()
        val cue = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_CUE_FIELD).getUnclippedBoundsInRoot()
        val save = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE).getUnclippedBoundsInRoot()

        assertTrue("the cue sits above the numbers, not on their row", cue.bottom <= rest.top)
        assertTrue(
            "the cue is as wide as the number row below it, not squeezed between them",
            cue.right - cue.left >= rpe.right - rpe.left,
        )
        assertTrue(
            "the effort and the rest share a line, the effort first (N116)",
            rpe.top < rest.bottom && rest.top < rpe.bottom && rpe.right <= rest.left,
        )
        assertTrue(
            "and the save is on that same line, at its end",
            save.top < rest.bottom && rest.top < save.bottom && save.left >= rest.right,
        )
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
        openBlock("te1")

        addSet()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertTextContains("100")
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_REPS_MAX).assertTextContains("3")
    }
    @Test
    fun addingADropSet_asksForTheValueItTakesOff_theAnchor() {
        // ROADMAP N79: a drop rung has no weight of its own to type — its load is the anchor less the
        // run's value — so the dialog swaps the weight field for the one number a run is authored with.
        setScreen(state = rampedExercise(stepGrams = null))
        openBlock("te1")

        addSet()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.DROP.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_WEIGHT).assertDoesNotExist()
    }

    @Test
    fun addingASecondDropSet_doesNotAskForTheValueAgain() {
        // B120: a run names its value on its **first** rung (N79), so a drop appended to the run continues
        // it. The dialog used to be told only that a drop was *allowed* at this position — which is true of
        // every rung of a run — and so treated the new row as an opener: it demanded a value the write
        // boundary then refused ("A run's drop value is written on its first rung"), and a drop run could
        // never grow past one rung.
        setScreen(state = droppingExercise())
        openBlock("te1")

        addSet()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.DROP.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).assertIsEnabled()
    }

    @Test
    fun editingAnOrdinarySetIntoADrop_asksForTheValueItTakesOff() {
        // B121: the row that **exists** and is being changed into a run's opener is neither a rung — so
        // `runAt` answers null — nor a row being added, which is all the fallback covered. The dialog hid
        // the field, Save was enabled, and the write boundary then refused the null value it wrote ("A drop
        // run needs the value it takes off the set above it"), so an ordinary set could not be turned into
        // the drop that opens a run at all. The position does open a run — the row above is not a drop — so
        // the value has to be asked for here, exactly as it is when that row is being added instead.
        setScreen(state = twoOrdinarySets())
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateSetRole(SetType.DROP.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).assertExists()
        // And the guard an opener is authored under: an empty value is refused, because the run would have
        // no ladder to load — where the continuing rung of B120's test inherits one and saves without it.
        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_DROP_VALUE).performTextInput("20")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_SET_SAVE).assertIsEnabled()
    }

    @Test
    fun aPlansFirstSet_cannotBeArmedWithARung() {
        // ROADMAP B64: a rung hangs off the set above it, so the plan's picker does not offer one where
        // there is nothing above — the write boundary would refuse the save, and the project's rule is
        // that a control which cannot write is worse than no control (N67). The exercise here has no
        // planned sets, so the set being added is its first — and since the block's Add set is tagged per
        // exercise (B76), a second exercise can stay on screen while this one is the one addressed.
        setScreen(state = twoExercises)
        openBlock("te1")

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
        openBlock("te1")

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
        openBlock("te1")

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
        openBlock("te1")

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
        openBlock("te1")

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
        openBlock("te1")

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
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertIsDisplayed()
    }

    @Test
    fun eachExerciseBlock_carriesItsOwnAddSet_andEmptyNote() {
        // ROADMAP B76: the block's controls belong to the exercise, so two planned exercises answer to two
        // distinct tags. Before, both blocks applied one tag — two nodes under it — so neither could be
        // addressed, and `performScrollToNode` throws when a matcher finds more than one node.
        setScreen(state = twoExercises)
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanAdd("te1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanAdd("te1")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.templatePlanEmpty("te1")).assertExists()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templateFold("te2")))
        openBlock("te2")

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

    /**
     * A plan's ramp does not number its working sets (ROADMAP N112).
     *
     * One rule numbers the plan, the workout and the history, and it is the rule asserted here: a
     * **warm-up takes no number**, so the working set after it reads 1 rather than 2 — which is what a
     * lifter's own sheet says, where the ramp is `w1, w2` and the work is `s1`.
     */
    @Test
    fun aPlannedWarmUp_readsItsRole_whereTheWorkingSetAfterItIsOne() {
        setScreen(
            state = twoExercises.copy(
                exercises = listOf(
                    twoExercises.exercises.first().copy(
                        sets = listOf(
                            TemplateSet(
                                id = "ts0",
                                templateExerciseId = "te1",
                                setIndex = 0,
                                role = SetType.WARMUP,
                                targetWeightGrams = 60_000L,
                                targetRepsMax = 5,
                            ),
                            TemplateSet(
                                id = "ts1",
                                templateExerciseId = "te1",
                                setIndex = 1,
                                role = SetType.NORMAL,
                                targetWeightGrams = 140_000L,
                                targetRepsMax = 2,
                            ),
                        ),
                    ),
                ),
            ),
        )
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanSet("ts0")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts0"))
            .assertTextContains("Warm-up", substring = true)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.templatePlanSet("ts1")))
        composeTestRule.onNodeWithTag(TestTags.templatePlanSet("ts1"))
            .assertTextContains("1 · Working", substring = true)
    }

    /**
     * A planned exercise's ⋮ opens the lift itself (ROADMAP N121).
     *
     * Planning is where a lifter notices the lift's facts matter — its unit, step, rest and cue — and the only
     * way to them was to leave the template, go to the library and find it. It reports the **exercise's** id
     * rather than the row's: a planned row is the plan's copy, and the destination is the lift.
     */
    @Test
    fun aPlannedExercisesMenu_opensTheLiftItself() {
        val opened = mutableListOf<String>()
        setScreen(actions = Actions(onOpenExercise = { opened += it }))
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateOpenExercise("te1")).performClick()

        assertThat(opened).containsExactly("back-squat")
    }

    @Test
    fun theOpenEntry_isOnlyInsideTheMenu() {
        // N91 leaves the row's own tap as the fold, so the way to the lift is the ⋮ and nothing on the row.
        setScreen()
        openBlock("te1")

        composeTestRule.onNodeWithTag(TestTags.templateOpenExercise("te1")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateMenu("te1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.templateOpenExercise("te1")).assertExists()
    }

    @Test
    fun theEffortTheRest_andTheSave_shareOneLine_inThatOrder() {
        setScreen(state = twoExercises)
        openBlock("te1")

        val rpe = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_EXERCISE_RPE)
            .getUnclippedBoundsInRoot()
        val rest = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_FIELD)
            .getUnclippedBoundsInRoot()
        val save = composeTestRule.onNodeWithTag(TestTags.TEMPLATE_REST_CUE_SAVE)
            .getUnclippedBoundsInRoot()

        assertThat(rest.left >= rpe.right).isTrue()
        assertThat(save.left >= rest.right).isTrue()
        // And within one line's height, so the order is read across rather than down.
        assertThat(rpe.top < rest.bottom && rest.top < rpe.bottom).isTrue()
        assertThat(rest.top < save.bottom && save.top < rest.bottom).isTrue()
    }

    /**
     * A first exercise carrying one planned set, so a summary line can be read on its own.
     *
     * The plan's target RPE is the exercise's own number (N59) and is covered by
     * [oneRpeTargetPerExercise_isEdited_inTheBlocksFoot]; a set carries no effort of its own.
     */
    private fun exerciseWithAPlannedSet() =
        twoExercises.exercises.first().copy(
            sets = listOf(
                TemplateSet(
                    id = "ts1",
                    templateExerciseId = "te1",
                    setIndex = 0,
                    targetWeightGrams = 140_000L,
                    targetRepsMax = 2,
                ),
            ),
        )
}
