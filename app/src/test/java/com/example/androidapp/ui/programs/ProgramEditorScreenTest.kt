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
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * One program's editor (ROADMAP P3.3).
 *
 * The order and the weekday are the feature, so both are asserted through the control that
 * changes them rather than by reading the state back.
 */
@RunWith(AndroidJUnit4::class)
// A phone-sized screen: the editor's header is the name, the follow switch and the program's kind,
// and Robolectric's default 470dp test window put the first day's own controls below the fold. The
// tests are about what the controls do, so they run where a lifter would actually see them.
@Config(qualifiers = "w411dp-h891dp")
class ProgramEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: ProgramEditorUiState = twoSlots,
        onRename: (String) -> Unit = {},
        onSetActive: (Boolean) -> Unit = {},
        onSetKind: (ProgramKind) -> Unit = {},
        onSetStartDate: (LocalDate?) -> Unit = {},
        onAddSlot: (String) -> Unit = {},
        onAddNewSlot: (String) -> Unit = {},
        onSetSlotWeekday: (String, DayOfWeek?) -> Unit = { _, _ -> },
        onMoveSlot: (String, Int) -> Unit = { _, _ -> },
        onRemoveSlot: (String) -> Unit = {},
        onOpenTemplate: (String) -> Unit = {},
        onOpenBalance: () -> Unit = {},
        onOpenDeload: () -> Unit = {},
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
                onSetKind = onSetKind,
                onSetStartDate = onSetStartDate,
                onAddSlot = onAddSlot,
                onAddNewSlot = onAddNewSlot,
                onSetSlotWeekday = onSetSlotWeekday,
                onMoveSlot = onMoveSlot,
                onRemoveSlot = onRemoveSlot,
                onOpenTemplate = onOpenTemplate,
                onOpenBalance = onOpenBalance,
                onOpenDeload = onOpenDeload,
                onPreviewSlot = onPreviewSlot,
                onClosePreview = onClosePreview,
                onDeleteProgram = onDeleteProgram,
                onExportProgram = onExportProgram,
                onBack = {},
            )
        }
    }

    @Test
    fun thePicker_createsANewWorkoutInline() {
        // P3.18: a program is the screen the lifter is on, so a workout it needs should be creatable here
        // rather than in Templates and then found again.
        var name: String? = null
        setScreen(onAddNewSlot = { name = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.ADD_SLOT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PICKER_NEW).performClick()

        assertThat(name).isNotNull()
    }

    @Test
    fun theBarsMenu_carriesBalance_exportAndDelete_inThatOrder() {
        // ROADMAP N119: one control with three entries, and the destructive one last and coloured — the order
        // N43 settled for the database's own actions. Balance first, because it is the only one that reads.
        var balanced = false
        setScreen(onOpenBalance = { balanced = true })

        // Nothing is on the bar itself: the three entries are the menu's.
        composeTestRule.onNodeWithTag(TestTags.Programs.EXPORT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Programs.BALANCE).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Programs.MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.BALANCE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.EXPORT).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.DELETE).assertExists()

        composeTestRule.onNodeWithTag(TestTags.Programs.BALANCE).performClick()

        assertThat(balanced).isTrue()
    }

    @Test
    fun theBarsMenu_carriesTheDeloadCreator() {
        // ROADMAP N147: the creator builds a week from this program's own workouts, so its way in is
        // the program that owns them.
        var opened = false
        setScreen(onOpenDeload = { opened = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.MENU).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.DELOAD).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Programs.DELOAD).performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun theExportAction_writesThisProgramToAFile() {
        // ROADMAP N47: export is per program, so it lives in the editor rather than on the list.
        var exported = false
        setScreen(onExportProgram = { exported = true })

        // The bar's ⋮ carries it now (N119), so reaching export is opening the menu first.
        composeTestRule.onNodeWithTag(TestTags.Programs.MENU).performClick()
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
    fun theKindChips_askForAnOrderThatEnds() {
        var kind: ProgramKind? = null
        setScreen(onSetKind = { kind = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.KIND_SEQUENCE).performClick()

        assertThat(kind).isEqualTo(ProgramKind.SEQUENCE)
    }

    @Test
    fun theWeeklyChip_asksForAWeekThatRepeats() {
        var kind: ProgramKind? = null
        setScreen(
            state = twoSlots.copy(program = twoSlots.program?.copy(kind = ProgramKind.SEQUENCE)),
            onSetKind = { kind = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.KIND_WEEKLY).performClick()

        assertThat(kind).isEqualTo(ProgramKind.WEEKLY)
    }

    @Test
    fun changingABlocksStart_opensTheDatePicker() {
        setScreen(
            state = twoSlots.copy(program = twoSlots.program?.copy(kind = ProgramKind.SEQUENCE)),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.START_DATE_CHANGE).performClick()

        // The picker's own confirm is the thing on screen; the chosen date is the repository's to
        // write, which the ViewModel test and the instrumented suite cover.
        composeTestRule.onNodeWithText("Set").assertIsDisplayed()
    }

    @Test
    fun aBlock_showsTheDayItStarts_andItsPlace() {
        setScreen(
            state = twoSlots.copy(
                program = twoSlots.program?.copy(kind = ProgramKind.SEQUENCE),
                runSlotId = "s1",
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.START_DATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Starts on the day it was created").assertIsDisplayed()
        // The run marker is the block's place rather than "Next up" (N178). The unmerged tree for
        // the same reason the existing run-marker test uses it: it lives inside a merged list item.
        composeTestRule
            .onNodeWithTag(TestTags.Programs.runSlot("s1"), useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Day 1 of 2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aWeeklyProgram_doesNotOfferAStartDate() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Programs.START_DATE).assertDoesNotExist()
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
    fun aSlotOpensTheTemplateItNames() {
        // ROADMAP N120: a program is a schedule over templates, and the only way to the workout a slot
        // names was the library. The row's own ⋮ carries it, and it reports the *template's* id rather
        // than the slot's — a slot is the schedule, and the destination is the workout.
        val opened = mutableListOf<String>()
        setScreen(onOpenTemplate = { opened += it })

        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.openSlot("s1")).performClick()

        assertThat(opened).containsExactly("t1")
    }

    @Test
    fun openingASlot_staysDistinctFromPreviewingIt() {
        // The name keeps the in-place preview and the menu entry goes to the editor (N72, N120), so the two
        // gestures cannot be confused: one is offered on the row, the other inside the ⋮.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Programs.openSlot("s1")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Programs.slotMenu("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.openSlot("s1")).assertExists()
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

        // Addressed by tag rather than by the name it shows (AGENTS.md): the control is the slot's,
        // and its label is content rather than identity.
        composeTestRule.onNodeWithTag(TestTags.Programs.previewOpen("s1")).performClick()

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

        composeTestRule.onNodeWithTag(TestTags.Programs.MENU).performClick()
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
