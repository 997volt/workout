package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.PersonalRecordMoment
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionOffer
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.ProgressionSource
import com.example.androidapp.domain.model.ProgressionStep
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.jointSiteKey
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The active workout screen (closing the testing gap the roadmap names).
 *
 * The stateless composable with a fixed state, so this covers what the ViewModel
 * tests cannot see: that "Done" and "Reopen" are the right way round, that a done
 * exercise really loses its Log set button, and that its sets stop being tappable.
 */
@RunWith(AndroidJUnit4::class)
class ActiveWorkoutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The screen's events, grouped: one flat parameter per callback stops reading as
     * a call site long before it stops compiling.
     */
    private data class Actions(
        val onFinishExercise: (String) -> Unit = {},
        val onAcceptProgression: (String, ProgressionDirection) -> Unit = { _, _ -> },
        val onMoveExercise: (String, Int) -> Unit = { _, _ -> },
        val onReopenExercise: (String) -> Unit = {},
        val onRemoveExercise: (String) -> Unit = {},
        val onRateExercise: (String, Int?, List<JointPain>) -> Unit = { _, _, _ -> },
        val onFinish: (String?) -> Unit = {},
        val onLogSet: (String, SetEdit) -> Unit = { _, _ -> },
        val onToggleSuperset: (String) -> Unit = {},
        val onDiscard: () -> Unit = {},
    )

    private fun setScreen(
        state: ActiveWorkoutUiState,
        actions: Actions = Actions(),
        personalRecord: PersonalRecordMoment? = null,
        countsAgainstProgram: Boolean = false,
        restTimerEnabled: Boolean = true,
        defaultRestSeconds: Int = 90,
        /**
         * The superset callback, passed directly rather than through [Actions] because the screen
         * silences it for the first row (B28): a test that needs the entry on row 0 has to be able to
         * say so, which is what N53's move into the overflow made worth asserting.
         */
        onToggleSuperset: (String) -> Unit = actions.onToggleSuperset,
        /** Whether *Done* asks about the next step a plan earned (ROADMAP N66). */
        progressionPromptEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            ActiveWorkoutScreen(
                state = state,
                clock = remember { mutableStateOf(WorkoutClock()) },
                onAddExercise = {},
                onLogSet = actions.onLogSet,
                onToggleSuperset = onToggleSuperset,
                personalRecord = personalRecord,
                onUpdateSet = { _, _, _, _, _, _, _ -> },
                onRemoveExercise = actions.onRemoveExercise,
                onMoveExercise = actions.onMoveExercise,
                onRateExercise = actions.onRateExercise,
                onFinish = actions.onFinish,
                onFinishExercise = actions.onFinishExercise,
                onAcceptProgression = actions.onAcceptProgression,
                onReopenExercise = actions.onReopenExercise,
                onDeleteSet = {},
                onUndoDelete = {},
                onDismissUndo = {},
                onSkipRest = {},
                onAdjustRest = {},
                onSaveReadinessNote = { _, _ -> },
                onDismissReadinessPrompt = {},
                onUndoFinishExercise = {},
                onDismissFinishUndo = {},
                onDiscard = actions.onDiscard,
                onBack = {},
                countsAgainstProgram = countsAgainstProgram,
                restTimerEnabled = restTimerEnabled,
                defaultRestSeconds = defaultRestSeconds,
                progressionPromptEnabled = progressionPromptEnabled,
            )
        }
    }

    @Test
    fun withTheRestTimerOff_theRestIsAFixedPrescription() {
        // ROADMAP N44: off is not "hide the number" — the exercise's own rest is shown as a fact,
        // with no countdown behind it.
        setScreen(state(isFinished = false), restTimerEnabled = false, defaultRestSeconds = 120)

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REST_PRESCRIPTION).assertExists()
        composeTestRule.onNodeWithText("Rest 2:00").assertExists()
    }

    @Test
    fun withTheRestTimerOn_thePrescriptionLabelIsNotDrawn() {
        // On, the countdown bar is the display; a second static copy of the same number would be
        // two answers to one question.
        setScreen(state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REST_PRESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun anOpenExercise_offersDone_andItsLogSetButton() {
        setScreen(state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REOPEN).assertDoesNotExist()
        // The fields stating the next set, and the button that writes them (N59).
        composeTestRule.onNodeWithTag(TestTags.SET_WEIGHT_FIELD).assertExists()
        composeTestRule.onNodeWithText("Log set").assertExists()
        // N5's cue, on the screen it was added for.
        composeTestRule.onNodeWithText("Brace, sit back").assertIsDisplayed()
    }

    @Test
    fun aDoneExercise_hidesLogSet_andOffersReopen() {
        setScreen(state(isFinished = true))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REOPEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_FINISHED_LABEL).assertIsDisplayed()
        // The accident N7 exists to prevent: no way to add another set, and no fields stating one.
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.SET_WEIGHT_FIELD).assertDoesNotExist()
    }

    @Test
    fun withAPlan_tappingDone_opensTheProgressionPrompt_andWritesNothingUntilItIsAnswered() {
        // ROADMAP N50: where a plan can answer it, Done opens the progression prompt, and answering
        // it is what finishes the exercise.
        var finished: String? = null
        setScreen(
            state(isFinished = false, progression = plannedPrompt()),
            actions = Actions(onFinishExercise = { finished = it }),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).performClick()

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN).assertExists()
        assertEquals(null, finished)

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_NOT_NOW).performClick()

        assertEquals("se1", finished)
    }

    @Test
    fun withNoPlan_tappingDone_onlyFinishesTheExercise() {
        // N50: no plan means no next step to decide. N8: leaving the exercise never asks how it felt,
        // so nothing opens at all — the rating is the exercise's own row.
        var rated: Rounding? = null
        var finished: String? = null
        setScreen(
            state(isFinished = false),
            actions = Actions(
                onRateExercise = { id, feel, joints -> rated = Rounding(id, feel, joints) },
                onFinishExercise = { finished = it },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).performClick()

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_FIELD).assertDoesNotExist()
        assertEquals("Done writes nothing", null, rated)
        assertEquals("se1", finished)
    }

    @Test
    fun withThePromptTurnedOff_tappingDone_onlyFinishesTheExercise() {
        // ROADMAP N66: the question is a preference, so a plan that could answer it is not asked
        // when the switch is off — Done finishes and nothing is written to the plan.
        var accepted: Pair<String, ProgressionDirection>? = null
        var finished: String? = null
        setScreen(
            state(isFinished = false, progression = earnedPrompt()),
            actions = Actions(
                onAcceptProgression = { id, direction -> accepted = id to direction },
                onFinishExercise = { finished = it },
            ),
            progressionPromptEnabled = false,
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).performClick()

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN).assertDoesNotExist()
        assertEquals("se1", finished)
        assertEquals("nothing was written to the plan", null, accepted)
    }

    @Test
    fun aPlanThatEarnedAStep_offersIt_andReportsTheDirectionAccepted() {
        var accepted: Pair<String, ProgressionDirection>? = null
        setScreen(
            state(isFinished = false, progression = earnedPrompt()),
            actions = Actions(onAcceptProgression = { id, direction -> accepted = id to direction }),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_REPS).performClick()

        assertEquals("se1" to ProgressionDirection.REPS, accepted)
    }

    @Test
    fun theProgressionPrompt_doesNotOfferTheRating() {
        // N8: the rating belongs to the exercise's own row, so the prompt asks about the plan and
        // nothing else — nothing in it opens the rating dialog.
        var rated: Rounding? = null
        setScreen(
            state(isFinished = false, progression = earnedPrompt()),
            actions = Actions(
                onRateExercise = { id, feel, joints -> rated = Rounding(id, feel, joints) },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_DONE).performClick()

        composeTestRule.onNodeWithTag(TestTags.PROGRESSION_PLAN).assertExists()
        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_FIELD).assertDoesNotExist()
        assertEquals("nothing in the prompt rates the exercise", null, rated)
    }

    @Test
    fun anExerciseCanBeRated_beforeItIsDone() {
        // ROADMAP N10: the ratings used to be reachable only through the Done
        // prompt, which means recording how a set felt from memory, afterwards.
        var rated: Rounding? = null
        setScreen(
            state(isFinished = false),
            Actions(
                onRateExercise = { id, feel, joints ->
                    rated = Rounding(id, feel, joints)
                },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_RATING_ROW, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        // Muscle feel is left at the 7 it opens on: the inline rating is what this test is about.
        composeTestRule.onNodeWithTag(TestTags.Rating.JOINT_ADD).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Rating.jointOption(jointSiteKey(Joint.ELBOW, Side.RIGHT)))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.RATING_SAVE).performClick()

        assertEquals(Rounding("se1", 7, listOf(JointPain(Joint.ELBOW, Side.RIGHT, 1))), rated)
    }

    @Test
    fun anExerciseAlreadyRated_showsWhatItSaid() {
        setScreen(state(isFinished = false).copy(exercises = listOf(finishedRow(rated = true))))

        // N63: the picked joint and its own score ride in the same summary.
        composeTestRule.onNodeWithText("Muscle feel 8 · Left knee 2/10").assertExists()
    }

    @Test
    fun aDoneExercise_canStillBeRated() {
        // "At any time" includes after Done: the row is not tied to the prompt.
        var rated: Rounding? = null
        setScreen(
            state(isFinished = false).copy(exercises = listOf(finishedRow(rated = false))),
            Actions(
                onRateExercise = { id, feel, joints ->
                    rated = Rounding(id, feel, joints)
                },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_RATING_ROW, useUnmergedTree = true)
            .performClick()
        // Two steps down from the 7 the stepper opens on.
        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_DECREASE).performClick()
        composeTestRule.onNodeWithTag(TestTags.RATING_MUSCLE_DECREASE).performClick()
        composeTestRule.onNodeWithTag(TestTags.RATING_SAVE).performClick()

        assertEquals(Rounding("se1", 5, emptyList()), rated)
    }

    /** What the screen reports when a rating is saved outside the Done prompt. */
    private data class Rounding(
        val id: String,
        val muscleFeel: Int?,
        val joints: List<JointPain>,
    )

    private fun finishedRow(rated: Boolean) = SessionExerciseRow(
        id = "se1",
        exerciseId = "back-squat",
        name = "Back Squat",
        subtitle = "Quads · Barbell",
        isFinished = true,
        muscleFeel = if (rated) 8 else null,
        joints = if (rated) listOf(JointPain(Joint.KNEE, Side.LEFT, 2)) else emptyList(),
        sets = listOf(SetRow(id = "set1", number = 1, reps = 5, weightGrams = 100_000)),
        suggestion = SetSuggestion(reps = 5, weightGrams = 100_000),
    )

    @Test
    fun tappingReopen_reportsThatExercise() {
        var reopened: String? = null
        setScreen(state(isFinished = true), actions = Actions(onReopenExercise = { reopened = it }))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REOPEN).performClick()

        assertEquals("se1", reopened)
    }

    @Test
    fun finishing_asksForAComment_andPassesItOn() {
        // ROADMAP N11: the moment of finishing is when the reason is remembered.
        var finished: String? = null
        setScreen(state(isFinished = false), actions = Actions(onFinish = { note -> finished = note }))

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_FINISH).performClick()
        composeTestRule.onNodeWithTag(TestTags.WORKOUT_NOTE).performTextInput("Legs felt heavy")
        composeTestRule.onNodeWithTag(TestTags.WORKOUT_NOTE_SAVE).performClick()

        assertEquals("Legs felt heavy", finished)
    }

    @Test
    fun skippingTheComment_stillFinishesTheWorkout() {
        // A prompt, not a gate: the user asked to finish.
        var finished: String? = null
        var called = false
        setScreen(state(isFinished = false), actions = Actions(onFinish = { note -> finished = note; called = true }))

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_FINISH).performClick()
        composeTestRule.onNodeWithTag(TestTags.WORKOUT_NOTE_SKIP).performClick()

        assertTrue(called)
        assertNull(finished)
    }

    @Test
    fun removingAnExercise_asksFirst_andWritesNothingUntilConfirmed() {
        // ROADMAP B2: the removal has no undo, so the dialog is the guard — and N53 moved the action
        // into the exercise's own overflow without changing that.
        var removed: String? = null
        setScreen(state(isFinished = false), actions = Actions(onRemoveExercise = { removed = it }))

        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE).performClick()

        assert(removed == null) { "the tap removed the exercise before asking" }
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CONFIRM).assertExists()

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CONFIRM).performClick()

        assert(removed == "se1") { "confirming did not remove the exercise, got $removed" }
    }

    @Test
    fun dismissingTheRemovalDialog_leavesTheExerciseInPlace() {
        var removed: String? = null
        setScreen(state(isFinished = false), actions = Actions(onRemoveExercise = { removed = it }))

        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE).performClick()
        // The dialog's other button: backing out must not remove anything.
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CANCEL).performClick()

        assert(removed == null) { "cancelling removed the exercise anyway" }
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE_CONFIRM).assertDoesNotExist()
    }

    @Test
    fun aWorkoutWithSets_discardsOnlyAfterAConfirmedPrompt() {
        // ROADMAP N41: a workout holding logged sets had no exit but Finish, which files it in
        // history. The destructive path now asks first and says what goes.
        var discarded = false
        setScreen(state(isFinished = false), actions = Actions(onDiscard = { discarded = true }))

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD).performClick()

        assertTrue("the tap discarded the workout before asking", !discarded)
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_TEXT).assertExists()

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_CONFIRM).performClick()

        assertTrue("confirming did not discard the workout", discarded)
    }

    @Test
    fun dismissingTheDiscardPrompt_keepsTheWorkout() {
        var discarded = false
        setScreen(state(isFinished = false), actions = Actions(onDiscard = { discarded = true }))

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD).performClick()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_CANCEL).performClick()

        assertTrue("cancelling discarded the workout anyway", !discarded)
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_CONFIRM).assertDoesNotExist()
    }

    @Test
    fun discardingAProgramWorkout_saysItCountsAsAMiss() {
        // P3.5: only a finished session settles an occurrence, so the prompt has to be honest that
        // dropping out is counted against the program rather than as no workout at all.
        setScreen(state = state(isFinished = false), countsAgainstProgram = true)

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD).performClick()

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_PROGRAM).assertExists()
    }

    @Test
    fun discardingANonProgramWorkout_doesNotClaimAMiss() {
        setScreen(state = state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD).performClick()

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_PROGRAM).assertDoesNotExist()
    }

    @Test
    fun theEmptyWorkout_discardsWithoutAPrompt() {
        // N41 deliberately leaves the empty state alone: there is nothing to lose, so the
        // prompt-free discard keeps behaving as it does.
        var discarded = false
        setScreen(
            ActiveWorkoutUiState(isLoading = false, sessionId = "s1", exercises = emptyList()),
            actions = Actions(onDiscard = { discarded = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_MENU).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_TEXT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.ACTIVE_WORKOUT_DISCARD_EMPTY).performClick()

        assertTrue("the empty workout's discard did not reach the action", discarded)
    }

    @Test
    fun aDoneExercisesSet_cannotBeTapped() {
        setScreen(state(isFinished = true))

        // useUnmergedTree: the tag is on a child of a merging parent.
        composeTestRule.onNodeWithTag(TestTags.SET_ROW, useUnmergedTree = true)
            .assertHasNoClickAction()
    }

    @Test
    fun anOpenExercisesSet_isStillEditable() {
        setScreen(state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.SET_ROW, useUnmergedTree = true)
            .assertHasClickAction()
    }

    @Test
    fun aWarmUpSet_isNotOfferedTheRpeField() {
        // ROADMAP N67: a warm-up carries no effort, so the field is absent rather than disabled — a
        // control that cannot write is worse than no control — and the role picker is what brings it
        // back.
        setScreen(
            state(
                isFinished = false,
                suggestion = SetSuggestion(reps = 5, weightGrams = 60_000, setType = SetType.WARMUP),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.SET_INCREASE_RPE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).assertExists()
    }

    @Test
    fun aWorkingSet_isStillOfferedTheRpeField() {
        setScreen(state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertExists()
    }

    @Test
    fun theSetEditor_opensOnALoggedSet_andCancelClosesIt() {
        // The editor is the *correction* path now that logging has its own fields (N59): tapping a
        // logged set still opens it, and Cancel still means nothing is written.
        setScreen(state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.SET_ROW, useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).assertExists()

        composeTestRule.onNodeWithTag(TestTags.SET_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_SAVE).assertDoesNotExist()
    }

    private fun state(
        isFinished: Boolean,
        /**
         * What *Done* opens with (ROADMAP N50). Empty by default, which is the no-plan case: Done is
         * then the rating prompt (N8). A test that wants the progression prompt passes one with a plan
         * — [plannedPrompt] or [earnedPrompt].
         */
        progression: ProgressionPrompt = ProgressionPrompt(),
        /** What the next-set fields open on (N59); a warm-up role withholds the RPE field (N67). */
        suggestion: SetSuggestion = SetSuggestion(reps = 5, weightGrams = 100_000),
    ) = ActiveWorkoutUiState(
        isLoading = false,
        sessionId = "s1",
        startedAt = "07:42",
        exercises = listOf(
            SessionExerciseRow(
                id = "se1",
                exerciseId = "back-squat",
                name = "Back Squat",
                subtitle = "Quads · Barbell",
                techniqueNote = "Brace, sit back",
                isFinished = isFinished,
                sets = listOf(SetRow(id = "set1", number = 1, reps = 5, weightGrams = 100_000)),
                suggestion = suggestion,
                progression = progression,
            ),
        ),
    )

    /**
     * One exercise whose plan exists but earned no step: the prompt states it and offers nothing, so a
     * test that wants the prompt can pass this rather than the earned one (N50).
     */
    private fun plannedPrompt(): ProgressionPrompt = ProgressionPrompt(
        planned = ProgressionPlanSet(
            setId = "ts-0",
            setIndex = 0,
            source = ProgressionSource.TEMPLATE,
            targetWeightGrams = 100_000L,
            targetRepsMax = 5,
            targetRpeHalves = 8,
        ),
        performed = ProgressionPerformance(reps = 5, weightGrams = 100_000L, rpeHalves = 8),
    )

    /** One exercise whose plan asked 5 reps at RPE 8 and whose session answered at 7 (N50). */
    private fun earnedPrompt(): ProgressionPrompt {
        val planned = ProgressionPlanSet(
            setId = "ts-0",
            setIndex = 0,
            source = ProgressionSource.TEMPLATE,
            targetWeightGrams = 100_000L,
            targetRepsMax = 5,
            targetRpeHalves = 8,
        )
        return ProgressionPrompt(
            planned = planned,
            performed = ProgressionPerformance(reps = 5, weightGrams = 100_000L, rpeHalves = 7),
            offer = ProgressionOffer(
                set = planned,
                reps = ProgressionStep(5, 6),
                load = ProgressionStep(100_000L, 102_500L),
            ),
        )
    }

    @Test
    fun theNextSet_isStatedByFields_andLogSetWritesThem() {
        // ROADMAP N59: the values are on screen before anything is written, and the button beside
        // them writes exactly those values — B7's display-agrees-with-storage, back on this path.
        val logged = mutableListOf<Pair<String, SetEdit>>()
        setScreen(
            state = state(isFinished = false),
            actions = Actions(onLogSet = { id, edit -> logged += id to edit }),
        )

        // Stated before any tap, and nothing is written by looking at them.
        composeTestRule.onNodeWithTag(TestTags.SET_WEIGHT_FIELD).assertExists()
        composeTestRule.onNodeWithTag(TestTags.SET_REPS_FIELD).assertExists()
        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertExists()
        assertEquals(emptyList<Pair<String, SetEdit>>(), logged)

        // The button is the whole of the commit, so the click is sent as its own action rather than
        // as a touch: the fields put it at the fold of this test's small surface, where a touch at
        // its centre lands outside the list's viewport and is dropped.
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, logged.size)
        assertEquals("se1", logged.single().first)
        assertEquals(5, logged.single().second.reps)
        assertEquals(100_000L, logged.single().second.weightGrams)
    }

    @Test
    fun editingAField_beforeLogging_writesTheEditedValue() {
        // The point of stating the set on the screen: a set that differs from the prefill is changed
        // *before* the write, so nothing is logged and then corrected (N59).
        val logged = mutableListOf<SetEdit>()
        setScreen(
            state = state(isFinished = false),
            actions = Actions(onLogSet = { _, edit -> logged += edit }),
        )

        composeTestRule.onNodeWithTag(TestTags.SET_REPS_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.SET_REPS_FIELD).performTextInput("7")
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(7), logged.map { it.reps })
    }

    @Test
    fun theInlineSteppers_moveTheFieldsTheyState() {
        // The logging path's own controls (N59), each addressed by its own tag: the correction dialog
        // states a set that exists and no longer shares these names, so one tag names one control (D4).
        setScreen(state = state(isFinished = false))

        // Sent as semantics actions rather than touches: the fields sit at the fold of this test's
        // small surface, where a touch at a control's centre can land outside the list's viewport.
        composeTestRule.onNodeWithTag(TestTags.SET_INCREASE_WEIGHT)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.SET_WEIGHT_FIELD).assertTextContains("102.5")
        composeTestRule.onNodeWithTag(TestTags.SET_DECREASE_WEIGHT)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.SET_WEIGHT_FIELD).assertTextContains("100")

        composeTestRule.onNodeWithTag(TestTags.SET_DECREASE_REPS)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.SET_REPS_FIELD).assertTextContains("4")

        // No plan names an effort here, so the stepper opens on the default 9.0.
        composeTestRule.onNodeWithTag(TestTags.SET_DECREASE_RPE)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertTextEquals("8.5")
    }

    @Test
    fun theRole_isChosenInline_beforeTheWrite() {
        // N19's picker moved out of the dialog rather than away: the role is still one set's
        // decision, still made before the set is written, and still cleared by the write.
        val logged = mutableListOf<Pair<String, SetType>>()
        setScreen(
            state = state(isFinished = false),
            actions = Actions(onLogSet = { id, edit -> logged += id to edit.setType }),
        )

        composeTestRule.onNodeWithTag(TestTags.exercisePendingRole("se1")).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.exercisePendingRole("se1", "WARMUP")).performClick()
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf("se1" to SetType.WARMUP), logged)
    }

    @Test
    fun pastThePlannedWork_theControlStillLogs_theExtraSet() {
        // N52 changes the label and adds a notice; writing an extra set is what the control still
        // does, so nothing about it closes.
        var logged = false
        setScreen(
            state(isFinished = false).copy(exercises = listOf(plannedRow(logged = 3, planned = 3))),
            actions = Actions(onLogSet = { _, _ -> logged = true }),
        )

        composeTestRule.onNodeWithText("Log extra set").assertExists()
        composeTestRule.onNodeWithTag(TestTags.SET_LOG).performSemanticsAction(SemanticsActions.OnClick)

        assertTrue("the extra set was never written", logged)
    }

    @Test
    fun thePlansTargetRpe_prefillsTheStepper() {
        // ROADMAP N59 reversed: the plan's number is what the stepper opens on, so the lifter reads
        // the plan and changes it when the set felt different rather than starting from a blank field.
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises.map {
                    it.copy(suggestion = it.suggestion.copy(targetRpeHalves = 16))
                },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertTextEquals("8")
        composeTestRule.onNodeWithText("Plan: RPE 8").assertExists()
    }

    @Test
    fun withNoPlannedRpe_theStepperStartsAtNine() {
        // A stepper always shows a number, so an exercise with no plan target opens on 9.0, and that
        // is what a logged set records if it is never touched (N59). Nothing captions it: the field
        // already shows what it opens on.
        setScreen(state = state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertTextEquals("9")
        composeTestRule.onNodeWithText("Starts at 9").assertDoesNotExist()
    }

    @Test
    fun aLoggedSet_showsTheRpeItCarries() {
        // N6, N59: effort is part of what a set was, so the row says it on the set's own line rather
        // than leaving it to the small marker a glance can miss.
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises.map { row ->
                    row.copy(
                        sets = listOf(
                            SetRow(
                                id = "set1",
                                number = 1,
                                reps = 8,
                                weightGrams = 100_000L,
                                rpeHalves = 19,
                            ),
                        ),
                    )
                },
            ),
        )

        composeTestRule.onNodeWithText("100 kg × 8").assertExists()
        composeTestRule.onNodeWithText("RPE 9.5").assertExists()
    }

    @Test
    fun steppingTheRpe_isWhatTheLoggedSetCarries() {
        // Half-point stepping on the screen (N6): 9.0 → 9.5, and the button beside the stepper
        // writes the value it shows.
        val logged = mutableListOf<SetEdit>()
        setScreen(
            state = state(isFinished = false),
            actions = Actions(onLogSet = { _, edit -> logged += edit }),
        )

        composeTestRule.onNodeWithTag(TestTags.SET_INCREASE_RPE)
            .performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag(TestTags.SET_RPE_FIELD).assertTextEquals("9.5")
        composeTestRule.onNodeWithTag(TestTags.SET_LOG)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(19), logged.map { it.rpeHalves })
    }

    @Test
    fun aRecord_isShownAboveTheWork() {
        setScreen(
            state = state(isFinished = false),
            personalRecord = PersonalRecordMoment(
                exerciseName = "Back Squat",
                reps = 5,
                weightGrams = 100_000L,
                previousBestGrams = 95_000L,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.PERSONAL_RECORD).assertExists()
        composeTestRule.onNodeWithText("Personal record: 100 kg × 5").assertExists()
        composeTestRule.onNodeWithText("Back Squat — your best before was 95 kg").assertExists()
    }

    @Test
    fun theFirstTimeAtARepCount_saysThatInsteadOfClaimingABest() {
        // There was no bar to clear, and claiming one would be a lie the data does not support.
        setScreen(
            state = state(isFinished = false),
            personalRecord = PersonalRecordMoment(
                exerciseName = "Back Squat",
                reps = 3,
                weightGrams = 60_000L,
                previousBestGrams = null,
            ),
        )

        composeTestRule.onNodeWithText("Back Squat — the first time at this rep count").assertExists()
    }

    @Test
    fun withoutARecord_thereIsNoBanner() {
        setScreen(state = state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.PERSONAL_RECORD).assertDoesNotExist()
    }

    @Test
    fun editingALoggedSet_opensOnWhatWasStored() {
        // ROADMAP B14: the editor was never told the role or the assistance, so it opened on a
        // plain working set with no help — and saving wrote that over the stored row. A one-rep
        // correction silently destroyed both, which is what this assertion exists to prevent.
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises.map { row ->
                    row.copy(
                        sets = listOf(
                            SetRow(
                                id = "set1",
                                number = 1,
                                reps = 8,
                                weightGrams = 0L,
                                setType = SetType.WARMUP,
                                assistanceGrams = 20_000L,
                            ),
                        ),
                    )
                },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.SET_ROW).performClick()

        composeTestRule.onNodeWithText("Role: Warm-up").assertExists()
        composeTestRule.onNodeWithText("-20").assertExists()
    }

    @Test
    fun aGroupedExercise_isLabelledWithItsPlaceInTheSuperset() {
        // ROADMAP B24's gap: N24's label and toggle had no test. Giant-set notation is the only
        // way the screen says two exercises are performed together.
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises.map {
                    it.copy(supersetGroup = 1, supersetLabel = "A1")
                },
            ),
        )

        composeTestRule.onNodeWithText("A1 · Back Squat").assertExists()
    }

    @Test
    fun theSupersetTap_isNotDrawnOnTheFirstExercise() {
        // ROADMAP B28: the first exercise has nothing above it to pair with, and drawing the control
        // there was a no-op whose write rewrote every ungrouped row. N53 moved it into the overflow,
        // and the exclusion moves with it: the entry is not offered rather than writing nothing.
        setScreen(state = state(isFinished = false))

        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()

        composeTestRule.onNodeWithTag(TestTags.supersetToggle("se1")).assertDoesNotExist()
        // The menu itself is still there: the exclusion is the entry, not the whole overflow.
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_REMOVE).assertExists()
    }

    @Test
    fun aFinishedExercise_isNotOfferedTheSupersetTap() {
        // Same rule as its Log set button: a done exercise is out of the round (N7).
        setScreen(state = state(isFinished = true))

        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()

        composeTestRule.onNodeWithTag(TestTags.supersetToggle("se1")).assertDoesNotExist()
    }

    @Test
    fun aGroupedExercise_isOfferedTheWayOut_ofItsSuperset() {
        // The entry moved into the overflow rather than changing (N53), so the word still follows the
        // state — and finding it here is what proves the menu is where the action went. It is the
        // *second* row: B28 silences the action on the first, which has nothing above it to pair with.
        var toggled: String? = null
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises.map { it.copy(supersetGroup = 1, supersetLabel = "A1") } +
                    base.exercises.map {
                        it.copy(id = "se2", name = "Bench Press", supersetGroup = 1, supersetLabel = "A2")
                    },
            ),
            actions = Actions(onToggleSuperset = { toggled = it }),
        )

        // The list is the scrollable: the second row, and its menu with it, is only composed once it
        // is brought into view.
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.exerciseMenu("se2")))
        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se2")).performClick()
        composeTestRule.onNodeWithText("Leave the superset").assertExists()
        composeTestRule.onNodeWithTag(TestTags.supersetToggle("se2")).performClick()

        assertEquals("se2", toggled)
    }

    @Test
    fun anExerciseCanBeMovedUpAndDown_fromItsMenu() {
        // ROADMAP N54: the session's own order is editable, and both directions live in the exercise's
        // overflow beside the other rare actions.
        val moved = mutableListOf<Pair<String, Int>>()
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises +
                    base.exercises.map { it.copy(id = "se2", name = "Bench Press") },
            ),
            actions = Actions(onMoveExercise = { id, delta -> moved += id to delta }),
        )

        // Each direction is taken from the row that has one: the first moves down, the second moves up.
        // The second row is below the fold, so its menu only exists once the list is scrolled to it.
        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se1", up = false)).performClick()
        composeTestRule.onNodeWithTag(TestTags.EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.exerciseMenu("se2")))
        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se2", up = true)).performClick()

        assertEquals(listOf("se1" to 1, "se2" to -1), moved)
    }

    @Test
    fun aMoveWithNowhereToGo_isNotOffered() {
        // The first row has nothing above it and the last nothing below. An entry that writes nothing
        // is the control B28's row-0 exclusion exists to avoid, so it is not drawn at all (N54).
        val base = state(isFinished = false)
        setScreen(
            state = base.copy(
                exercises = base.exercises +
                    base.exercises.map { it.copy(id = "se2", name = "Bench Press") },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se1", up = true)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se1", up = false)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se1", up = false)).performClick()

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_LIST)
            .performScrollToNode(hasTestTag(TestTags.exerciseMenu("se2")))
        composeTestRule.onNodeWithTag(TestTags.exerciseMenu("se2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se2", up = false)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.exerciseMove("se2", up = true)).assertExists()
    }

    @Test
    fun pastTheLastPlannedSet_theControlSaysTheNextOneIsExtra() {
        // ROADMAP N52: the plan is the template the workout was started from, so the moment its last
        // set is written is the moment the app can say the planned work is done — rather than only in
        // the review, after Finish.
        setScreen(state(isFinished = false).copy(exercises = listOf(plannedRow(logged = 3, planned = 3))))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_PLAN_DONE).assertExists()
        composeTestRule.onNodeWithText("Log extra set").assertExists()
        // The label no longer promises the plan's next set, because the plan does not name one: it
        // drops back to the plain label rather than naming the last planned one again.
        composeTestRule.onNodeWithText("Log set").assertDoesNotExist()
    }

    @Test
    fun withAPlannedSetStillToWrite_theControlHasNothingToNotice() {
        // One short of the plan is not done: a notice here would appear a set early.
        setScreen(state(isFinished = false).copy(exercises = listOf(plannedRow(logged = 2, planned = 3))))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_PLAN_DONE).assertDoesNotExist()
        composeTestRule.onNodeWithText("Log set").assertExists()
    }

    @Test
    fun anExerciseWithNoPlan_isNeverCalledDone() {
        // An empty workout's exercise has no plan behind it, so there is no work to have finished —
        // and a null count is not zero.
        setScreen(state(isFinished = false).copy(exercises = listOf(plannedRow(logged = 9, planned = null))))

        composeTestRule.onNodeWithTag(TestTags.EXERCISE_PLAN_DONE).assertDoesNotExist()
        composeTestRule.onNodeWithText("Log set").assertExists()
    }

    /** One exercise with a plan behind it and [logged] sets already written. */
    private fun plannedRow(logged: Int, planned: Int?): SessionExerciseRow = SessionExerciseRow(
        id = "se1",
        exerciseId = "back-squat",
        name = "Back Squat",
        subtitle = "Quads · Barbell",
        plannedSetCount = planned,
        sets = List(logged) { index ->
            SetRow(id = "set$index", number = index + 1, reps = 5, weightGrams = 100_000)
        },
        suggestion = SetSuggestion(reps = 5, weightGrams = 100_000),
    )
}
