package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One program's editor (ROADMAP P3.3).
 *
 * The order and the weekday are the feature, so both are asserted through the control that
 * changes them rather than by reading the state back.
 */
@RunWith(AndroidJUnit4::class)
class ProgramEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: ProgramEditorUiState = twoSlots,
        onRename: (String) -> Unit = {},
        onSetActive: (Boolean) -> Unit = {},
        onAddSlot: (String) -> Unit = {},
        onSetSlotWeekday: (String, DayOfWeek?) -> Unit = { _, _ -> },
        onMoveSlot: (String, Int) -> Unit = { _, _ -> },
        onRemoveSlot: (String) -> Unit = {},
        onPreviewSlot: (String, String) -> Unit = { _, _ -> },
        onClosePreview: () -> Unit = {},
        onDeleteProgram: () -> Unit = {},
        onExportProgram: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProgramEditorScreen(
                state = state,
                onRename = onRename,
                onSetActive = onSetActive,
                onAddSlot = onAddSlot,
                onSetSlotWeekday = onSetSlotWeekday,
                onMoveSlot = onMoveSlot,
                onRemoveSlot = onRemoveSlot,
                onPreviewSlot = onPreviewSlot,
                onClosePreview = onClosePreview,
                onDeleteProgram = onDeleteProgram,
                onExportProgram = onExportProgram,
                onBack = {},
            )
        }
    }

    @Test
    fun theExportAction_writesThisProgramToAFile() {
        // ROADMAP N47: export is per program, so it lives in the editor rather than on the list.
        var exported = false
        setScreen(onExportProgram = { exported = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.EXPORT).performClick()

        assertThat(exported).isTrue()
    }

    @Test
    fun aSlot_showsItsTemplateAndExerciseCount() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Programs.slot("s1")).assertIsDisplayed()
        composeTestRule.onNodeWithText("Heavy lower").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 exercises").assertIsDisplayed()
    }

    @Test
    fun theRunPlace_isMarked_onItsOwnSlot() {
        // ROADMAP P3.9: the order is a run, and the editor is where the order is authored.
        setScreen(twoSlots.copy(runSlotId = "s2"))

        // The unmerged tree: the marker lives inside the row, whose semantics are merged into the
        // list item, so the default finder cannot see it. Which slot it is on is the behaviour.
        composeTestRule.onNodeWithTag(TestTags.Programs.runSlot("s2"), useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.runSlot("s1"), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun anEmptyProgram_explainsWhatASlotIs() {
        setScreen(twoSlots.copy(slots = emptyList()))

        composeTestRule.onNodeWithTag(TestTags.Programs.NO_SLOTS).assertIsDisplayed()
    }

    @Test
    fun theNameField_commitsTheTypedName() {
        var renamed: String? = null
        setScreen(onRename = { renamed = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.NAME_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Programs.NAME_FIELD).performTextInput("Push/Pull")
        composeTestRule.onNodeWithTag(TestTags.Programs.NAME_SAVE).performClick()

        assertThat(renamed).isEqualTo("Push/Pull")
    }

    @Test
    fun theActiveSwitch_makesThisProgramTheOneHomeFollows() {
        var active: Boolean? = null
        setScreen(onSetActive = { active = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE).performClick()

        assertThat(active).isTrue()
    }

    @Test
    fun turningTheActiveSwitchOff_fallsBackToThePins() {
        var active: Boolean? = null
        setScreen(
            state = twoSlots.copy(program = twoSlots.program?.copy(isActive = true)),
            onSetActive = { active = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE).performClick()

        assertThat(active).isFalse()
    }

    @Test
    fun addingAWorkout_offersTheTemplates_andPickingOneAddsIt() {
        var added: String? = null
        setScreen(onAddSlot = { added = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.ADD_SLOT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PICKER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.pickTemplate("t2")).performClick()

        assertThat(added).isEqualTo("t2")
    }

    @Test
    fun thePicker_saysWhenThereAreNoTemplatesToAdd() {
        setScreen(twoSlots.copy(templates = emptyList()))

        composeTestRule.onNodeWithTag(TestTags.Programs.ADD_SLOT).performClick()

        composeTestRule.onNodeWithTag(TestTags.Programs.PICKER_EMPTY).assertIsDisplayed()
    }

    @Test
    fun movingASlot_carriesItsDirection() {
        // ROADMAP N72: the order moved behind the row's ⋮, so reaching a direction means opening it.
        val moved = mutableListOf<Pair<String, Int>>()
        setScreen(onMoveSlot = { id, delta -> moved += id to delta })

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.move("s1", up = false)).performClick()

        assertThat(moved).containsExactly("s1" to 1)
    }

    @Test
    fun aDirectionWithNowhereToGo_isNotOffered() {
        // The first slot has nothing above it and the last nothing below, so the entry is absent
        // rather than present-and-inert — the shape the workout's menu already uses (N54, N72).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.move("s1", up = true)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Programs.move("s1", up = false)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.move("s2", up = false)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Programs.move("s2", up = true)).assertExists()
    }

    @Test
    fun removingASlot_asksFirst_andReportsThatOneOnConfirm() {
        // N72: the schedule loses a day, so it is a question rather than a tap.
        var removed: String? = null
        setScreen(onRemoveSlot = { removed = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.removeSlot("s2")).performClick()
        assertThat(removed).isNull()

        composeTestRule.onNodeWithTag(TestTags.Programs.SLOT_REMOVE_CONFIRM).performClick()

        assertThat(removed).isEqualTo("s2")
    }

    @Test
    fun cancellingARemoval_writesNothing() {
        var removed: String? = null
        setScreen(onRemoveSlot = { removed = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.removeSlot("s2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.SLOT_REMOVE_CANCEL).performClick()

        assertThat(removed).isNull()
    }

    @Test
    fun tappingASlotsName_opensWhatItsTemplateTrains() {
        // ROADMAP N72: the name is the way in, and what it opens is read-only — the template is
        // edited in the template editor, not here.
        var asked: Pair<String, String>? = null
        setScreen(onPreviewSlot = { id, name -> asked = id to name })

        composeTestRule.onNodeWithText("Heavy lower").performClick()

        assertThat(asked).isEqualTo("t1" to "Heavy lower")
    }

    @Test
    fun thePreview_listsTheTemplatesExercises_andItsSets() {
        var closed = false
        setScreen(
            onClosePreview = { closed = true },
            state = twoSlots.copy(
                preview = SlotPreview(
                    templateName = "Heavy lower",
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
                                    targetRepsMax = 3,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.PREVIEW).assertExists()
        composeTestRule.onNodeWithText("Back Squat").assertExists()
        // The same line the template editor draws, in the exercise's own unit (N64, N72).
        composeTestRule.onNodeWithText("100 kg", substring = true).assertExists()

        composeTestRule.onNodeWithTag(TestTags.Programs.PREVIEW_CLOSE).performClick()
        assertThat(closed).isTrue()
    }

    @Test
    fun thePreview_saysSo_whenTheTemplatePlansNothing() {
        setScreen(state = twoSlots.copy(preview = SlotPreview(templateName = "Empty day", exercises = emptyList())))

        composeTestRule.onNodeWithTag(TestTags.Programs.PREVIEW_EMPTY).assertExists()
    }

    @Test
    fun tappingTheChosenWeekday_makesTheSlotOrderOnly() {
        val chosen = mutableListOf<Pair<String, DayOfWeek?>>()
        setScreen(onSetSlotWeekday = { id, day -> chosen += id to day })

        composeTestRule.onNodeWithTag(TestTags.Programs.slotWeekday("s1", "MONDAY")).performClick()

        assertThat(chosen).containsExactly("s1" to null)
    }

    @Test
    fun tappingAnotherWeekday_pinsTheSlotToIt() {
        val chosen = mutableListOf<Pair<String, DayOfWeek?>>()
        // One slot only, so its chips are on screen: the second slot's row sits below the
        // fold in the two-slot fixture.
        setScreen(
            state = twoSlots.copy(slots = listOf(twoSlots.slots[1])),
            onSetSlotWeekday = { id, day -> chosen += id to day },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.slotWeekday("s2", "FRIDAY")).performClick()

        assertThat(chosen).containsExactly("s2" to DayOfWeek.FRIDAY)
    }

    @Test
    fun deletingAProgram_asksBeforeItDeletes() {
        var deleted = false
        setScreen(onDeleteProgram = { deleted = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.DELETE_CONFIRM).performClick()

        assertThat(deleted).isTrue()
    }

    private companion object {
        val twoSlots = ProgramEditorUiState(
            isLoading = false,
            program = WorkoutProgram(id = "p1", name = "Upper/Lower", slotCount = 2),
            slots = listOf(
                ProgramSlot(
                    id = "s1",
                    programId = "p1",
                    templateId = "t1",
                    position = 0,
                    weekday = DayOfWeek.MONDAY,
                    templateName = "Heavy lower",
                    exerciseCount = 4,
                ),
                ProgramSlot(
                    id = "s2",
                    programId = "p1",
                    templateId = "t2",
                    position = 1,
                    templateName = "Push",
                    exerciseCount = 5,
                ),
            ),
            templates = listOf(
                WorkoutTemplate(id = "t1", name = "Heavy lower", exerciseCount = 4),
                WorkoutTemplate(id = "t2", name = "Push", exerciseCount = 5),
            ),
        )
    }
}
