package com.example.androidapp.ui.templates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The template list (ROADMAP N3).
 *
 * The two actions on a row are easy to transpose and impossible to notice by eye —
 * tapping the row edits the plan, tapping Start begins the workout — so both are
 * asserted by the tag that shows which one fired.
 */
@RunWith(AndroidJUnit4::class)
class TemplatesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: TemplatesUiState = twoTemplates,
        onCreate: (String) -> Unit = {},
        onOpen: (String) -> Unit = {},
        onStart: (String) -> Unit = {},
        onOpenPrograms: () -> Unit = {},
        onShowHiddenChange: (Boolean) -> Unit = {},
        onUnhide: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TemplatesScreen(
                state = state,
                onCreateTemplate = onCreate,
                onOpenTemplate = onOpen,
                onStartTemplate = onStart,
                onBack = {},
                onOpenPrograms = onOpenPrograms,
                onShowHiddenChange = onShowHiddenChange,
                onUnhide = onUnhide,
            )
        }
    }

    @Test
    fun theBar_carriesTheWayAcrossToPrograms() {
        // ROADMAP N93: the two plan screens were siblings with a trip home between them, so each one's
        // own bar gains the entry to the other.
        var opened = false
        setScreen(onOpenPrograms = { opened = true })

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_PROGRAMS).performClick()

        assertTrue(opened)
    }

    @Test
    fun eachTemplate_showsItsNameAndExerciseCount() {
        setScreen()

        composeTestRule.onNodeWithText("Push day").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 exercises").assertIsDisplayed()
        composeTestRule.onNodeWithText("Legs").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 exercise").assertIsDisplayed()
    }

    @Test
    fun aRunningWorkout_disablesStart_andSaysWhy() {
        // ROADMAP N78: starting is idempotent, so this button would resume the workout already running
        // rather than begin this one. It is disabled rather than hidden — the plan is still worth
        // looking at — and the row says why, because greying alone leaves that to be guessed.
        setScreen(twoTemplates.copy(hasActiveWorkout = true))

        composeTestRule.onNodeWithTag(TestTags.templateStart("t1")).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.templateStartReason("t1"), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun withNoWorkoutRunning_startIsEnabled_andSaysNothingExtra() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.templateStart("t1")).assertIsEnabled()
        composeTestRule.onNodeWithTag(TestTags.templateStartReason("t1"), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun anEmptyList_explainsWhatATemplateIs() {
        setScreen(TemplatesUiState(isLoading = false, templates = emptyList()))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_EMPTY).assertIsDisplayed()
    }

    @Test
    fun tappingARow_editsThatTemplate() {
        var opened: String? = null
        setScreen(onOpen = { opened = it })

        composeTestRule.onNodeWithTag(TestTags.templateRow("t1")).performClick()

        assertEquals("t1", opened)
    }

    @Test
    fun theStartButton_startsThatTemplate_ratherThanEditingIt() {
        var started: String? = null
        var opened: String? = null
        setScreen(onStart = { started = it }, onOpen = { opened = it })

        composeTestRule.onNodeWithTag(TestTags.templateStart("t2")).performClick()

        assertEquals("t2", started)
        assertEquals("Start must not open the editor", null, opened)
    }

    @Test
    fun theNewTemplateAction_asksForATemplate() {
        var created: String? = null
        setScreen(onCreate = { created = it })

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_NEW).performClick()

        assertTrue("the button must create, not open", created != null)
    }

    @Test
    fun nothingHidden_offersNoFilter_atAll() {
        // A clean list should not gain a control it never needed (ROADMAP N214).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertDoesNotExist()
    }

    @Test
    fun aHiddenWorkout_drawsWithItsMarker_andTheFilterTurnsOff() {
        var shown: Boolean? = null
        setScreen(hiddenTemplate.copy(hiddenCount = 1), onShowHiddenChange = { shown = it })

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(TestTags.templateHidden("t2"), useUnmergedTree = true)
            .assertExists()

        assertEquals("the state was on, so one tap asks for them to be put away", false, shown)
    }

    @Test
    fun aHiddenRowsOwnButton_bringsItBack() {
        var unhidden: String? = null
        setScreen(hiddenTemplate, onUnhide = { unhidden = it })

        composeTestRule.onNodeWithTag(TestTags.templateUnhide("t2")).performClick()

        assertEquals("t2", unhidden)
    }

    @Test
    fun whenEveryWorkoutIsHidden_theListSaysTheFilterBringsOneBack() {
        // "No templates yet" over a library the lifter tidied would read as data loss.
        setScreen(TemplatesUiState(isLoading = false, templates = emptyList(), hiddenCount = 3))

        composeTestRule.onNodeWithText("Every workout is hidden").assertIsDisplayed()
    }

    private companion object {
        val hiddenTemplate = TemplatesUiState(
            isLoading = false,
            showHidden = true,
            templates = listOf(
                WorkoutTemplate(id = "t2", name = "Autumn plan", exerciseCount = 4, hiddenAt = Instant.EPOCH),
            ),
        )

        val twoTemplates = TemplatesUiState(
            isLoading = false,
            templates = listOf(
                WorkoutTemplate(id = "t1", name = "Push day", exerciseCount = 5),
                WorkoutTemplate(id = "t2", name = "Legs", exerciseCount = 1),
            ),
        )
    }
}
