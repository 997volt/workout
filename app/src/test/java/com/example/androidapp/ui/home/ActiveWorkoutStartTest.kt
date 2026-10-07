package com.example.androidapp.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Starting a planned workout while one is already running (ROADMAP N89).
 *
 * The route owns the wiring — it is the only place the navigation happens — so what is pinned here is the
 * rule, the question's two answers, and the delete it performs.
 */
@RunWith(AndroidJUnit4::class)
class ActiveWorkoutDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setDialog(
        start: StartIntent?,
        onContinue: () -> Unit = {},
        onDiscardAndStart: (StartIntent) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                ActiveWorkoutDialog(
                    start = start,
                    onContinue = onContinue,
                    onDiscardAndStart = onDiscardAndStart,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    @Test
    fun itNamesTheWorkoutBeingStarted_andOffersBothAnswers() {
        setDialog(StartIntent(templateId = "t1", label = "Heavy lower"))

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG).assertExists()
        // The name, not only the count: the question is "this instead of the one running", so the
        // sentence has to say which one.
        composeTestRule.onNodeWithText("Heavy lower", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_CONTINUE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DISCARD).assertIsDisplayed()
    }

    @Test
    fun continue_opensTheRunningWorkout_andDiscardsNothing() {
        var continued = 0
        var discarded = 0
        setDialog(
            start = StartIntent(templateId = "t1", label = "Heavy lower"),
            onContinue = { continued++ },
            onDiscardAndStart = { discarded++ },
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_CONTINUE).performClick()

        assertThat(continued).isEqualTo(1)
        assertThat(discarded).isEqualTo(0)
    }

    @Test
    fun discard_reportsTheStartTheLifterAskedFor_soItCanBeCarriedOut() {
        // The slot travels with it, so the prescription still seeds the workout once the old one is gone
        // (P3.8) — the same start the gate would have performed without the question.
        var started: StartIntent? = null
        setDialog(
            start = StartIntent(templateId = "t1", slotId = "slot-1", label = "Heavy lower"),
            onDiscardAndStart = { started = it },
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DISCARD).performClick()

        assertThat(started?.templateId).isEqualTo("t1")
        assertThat(started?.slotId).isEqualTo("slot-1")
    }

    @Test
    fun withNoStartWaiting_itDrawsNothing() {
        setDialog(start = null)

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG).assertDoesNotExist()
    }
}

/** The wiring: what the gate does with a start (ROADMAP N89). */
@RunWith(AndroidJUnit4::class)
class ActiveWorkoutGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Composes the gate and hands back the start action it returns, as the route would hold it. */
    private fun setGate(
        isActive: Boolean,
        onStart: (StartIntent) -> Unit = {},
    ): () -> ((StartIntent) -> Unit)? {
        var gate: ((StartIntent) -> Unit)? = null
        composeTestRule.setContent {
            AndroidAppTheme {
                gate = activeWorkoutGate(
                    isActive = { isActive },
                    onStart = onStart,
                    onContinueOngoing = {},
                    discard = { DataResult.Success(Unit) },
                    onFailure = {},
                )
            }
        }
        return { gate }
    }

    @Test
    fun aTemplateStart_whileAWorkoutRuns_asksInsteadOfStarting() {
        var started: StartIntent? = null
        val gate = setGate(isActive = true, onStart = { started = it })

        composeTestRule.runOnIdle { gate()?.invoke(StartIntent(templateId = "t1", label = "Heavy lower")) }

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG).assertExists()
        // Nothing started behind the question: the plan is not dropped, and the running session is not
        // opened until the lifter answers.
        assertThat(started).isNull()
    }

    @Test
    fun aTemplateStart_withNothingRunning_startsStraightAway() {
        var started: StartIntent? = null
        val gate = setGate(isActive = false, onStart = { started = it })

        composeTestRule.runOnIdle { gate()?.invoke(StartIntent(templateId = "t1")) }

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG).assertDoesNotExist()
        assertThat(started?.templateId).isEqualTo("t1")
    }

    @Test
    fun anEmptyStart_withAWorkoutRunning_startsStraightAway() {
        // Home's own pill reads *Resume* in this state, so there is nothing to ask and asking would make
        // the one obvious action a question.
        var started: StartIntent? = null
        val gate = setGate(isActive = true, onStart = { started = it })

        composeTestRule.runOnIdle { gate()?.invoke(StartIntent()) }

        composeTestRule.onNodeWithTag(TestTags.HOME_ACTIVE_WORKOUT_DIALOG).assertDoesNotExist()
        assertThat(started).isNotNull()
    }
}

/** The rule behind the question: which starts have something to choose (ROADMAP N89). */
class NeedsActiveWorkoutChoiceTest {

    @Test
    fun aTemplateStart_asksOnlyWhileAWorkoutIsRunning() {
        assertThat(needsActiveWorkoutChoice(StartIntent(templateId = "t1"), hasActiveWorkout = true))
            .isTrue()
        assertThat(needsActiveWorkoutChoice(StartIntent(templateId = "t1"), hasActiveWorkout = false))
            .isFalse()
    }

    @Test
    fun anEmptyStart_neverAsks() {
        // Home's own pill reads *Resume* while a session is open, so there is nothing to choose: it goes
        // to the running workout either way.
        assertThat(needsActiveWorkoutChoice(StartIntent(), hasActiveWorkout = true)).isFalse()
    }
}

/** The delete behind *Discard and start new* (ROADMAP N89). */
class DiscardActiveSessionTest {

    private val session = WorkoutSession(id = "s1", startedAt = Instant.parse("2026-10-07T07:00:00Z"))

    @Test
    fun withAWorkoutOpen_itDeletesThatSession() = runTest {
        val deleted = mutableListOf<String>()

        val result = discardActiveSession(
            active = flowOf(session),
            delete = { id ->
                deleted += id
                DataResult.Success(Unit)
            },
        )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(deleted).containsExactly("s1")
    }

    @Test
    fun withNothingOpen_itWritesNothing_andSucceeds() = runTest {
        // The start that follows then simply creates the session it asked for; an absent session is not a
        // failure to end one.
        val deleted = mutableListOf<String>()

        val result = discardActiveSession(
            active = flowOf(null),
            delete = { id ->
                deleted += id
                DataResult.Success(Unit)
            },
        )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(deleted).isEmpty()
    }

    @Test
    fun aFailedDelete_isReturned_ratherThanThrown() = runTest {
        // The session is still there, so nothing may start behind it — the caller says so instead (F7).
        val result = discardActiveSession(
            active = flowOf(session),
            delete = { DataResult.Failure(DataError.NotFound) },
        )

        assertThat(result).isInstanceOf(DataResult.Failure::class.java)
        assertThat((result as DataResult.Failure).error).isEqualTo(DataError.NotFound)
    }
}
