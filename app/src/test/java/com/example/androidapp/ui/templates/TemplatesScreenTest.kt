package com.example.androidapp.ui.templates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
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
        onFilterChange: (TemplatesFilter) -> Unit = {},
        onUnhide: (String) -> Unit = {},
        onRestore: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TemplatesScreen(
                state = state,
                onCreateTemplate = onCreate,
                onOpenTemplate = onOpen,
                onStartTemplate = onStart,
                onBack = {},
                onOpenPrograms = onOpenPrograms,
                onFilterChange = onFilterChange,
                onUnhide = onUnhide,
                onRestore = onRestore,
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
        // An empty library is not a library whose workouts are merely put away (ROADMAP N217).
        composeTestRule.onNodeWithText("No workouts in this view").assertDoesNotExist()
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
    fun nothingPutAway_offersNoFilter_atAll() {
        // A clean list should not gain a control it never needed (ROADMAP N214).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_FILTER_ALL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_DELETED_FILTER).assertDoesNotExist()
    }

    @Test
    fun theChips_askForTheirOwnGroup() {
        // Modes, not a reveal: each chip names one group (ROADMAP N217), and the deleted one is N216's.
        var asked: TemplatesFilter? = null
        setScreen(
            twoTemplates.copy(hiddenCount = 1, deletedCount = 2),
            onFilterChange = { asked = it },
        )

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_FILTER_ALL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertIsDisplayed().performClick()
        assertEquals("the hidden chip asks for the hidden group", TemplatesFilter.HIDDEN, asked)

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_DELETED_FILTER).assertIsDisplayed().performClick()
        assertEquals("and the deleted chip for its own", TemplatesFilter.DELETED, asked)
    }

    @Test
    fun aChipWithNothingInItsGroup_isNotOffered() {
        // The count is also the control's reason to exist (ROADMAP N214, N217).
        setScreen(twoTemplates.copy(hiddenCount = 1))

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_DELETED_FILTER).assertDoesNotExist()
    }

    @Test
    fun aHiddenRow_carriesNoMarker_onlyItsWayBack() {
        // The selected chip says which group is drawn, so the row needs no badge (ROADMAP N217); its own
        // button is still the way back without opening the editor (N214).
        var unhidden: String? = null
        setScreen(hiddenGroup, onUnhide = { unhidden = it })

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_HIDDEN_FILTER).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.templateUnhide("t2")).performClick()

        assertEquals("t2", unhidden)
    }

    @Test
    fun aDeletedRow_offersRestore_andNoStart() {
        // A deleted workout resolves by no read, so it cannot be started: the row's one action is
        // bringing it back (ROADMAP N216).
        var restored: String? = null
        setScreen(deletedGroup, onRestore = { restored = it })

        composeTestRule.onNodeWithTag(TestTags.TEMPLATES_DELETED_FILTER).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.templateStart("t3")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.templateRestore("t3")).performClick()

        assertEquals("t3", restored)
    }

    @Test
    fun whenTheRestIsPutAway_theListSaysWhereItWent() {
        // "No templates yet" over a library the lifter tidied would read as data loss (ROADMAP N217).
        setScreen(
            TemplatesUiState(
                isLoading = false,
                templates = emptyList(),
                hiddenCount = 2,
                deletedCount = 1,
            ),
        )

        composeTestRule.onNodeWithText("No workouts in this view").assertIsDisplayed()
    }

    private companion object {
        val hiddenGroup = TemplatesUiState(
            isLoading = false,
            filter = TemplatesFilter.HIDDEN,
            hiddenCount = 1,
            templates = listOf(
                WorkoutTemplate(id = "t2", name = "Autumn plan", exerciseCount = 4, hiddenAt = Instant.EPOCH),
            ),
        )

        val deletedGroup = TemplatesUiState(
            isLoading = false,
            filter = TemplatesFilter.DELETED,
            deletedCount = 1,
            templates = listOf(
                WorkoutTemplate(id = "t3", name = "Test", exerciseCount = 5),
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
