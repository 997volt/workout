package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SlotPrescription
import com.example.androidapp.domain.model.SlotSet
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.repository.SlotSetEdit
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a slot prescribes, authored (ROADMAP P3.8).
 *
 * The dialog is the only way a slot's prescription is written, so each control is asserted through
 * the callback it fires rather than by reading the state back: adding a set, editing one, deleting
 * one, and the rest and cue an exercise carries.
 */
@RunWith(AndroidJUnit4::class)
class SlotPrescriptionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var added: Pair<String, SlotSetEdit>? = null
    private var updated: Pair<String, SlotSetEdit>? = null
    private var removed: String? = null
    private var restCue: SlotExercisePlan? = null
    private var dismissed = false

    /** What the rest/cue/RPE dialog reported for one exercise (P3.8, N59). */
    private data class SlotExercisePlan(
        val exerciseId: String,
        val restSeconds: Int?,
        val techniqueNote: String?,
        val targetRpeHalves: Int?,
    )

    private fun setDialog(prescriptions: List<SlotPrescription> = emptyList()) {
        composeTestRule.setContent {
            SlotPrescriptionDialog(
                editor = SlotPrescriptionEditor(
                    slotId = "slot-1",
                    templateName = "Heavy lower",
                    exercises = listOf(
                        TemplateExercise(
                            id = "te-0",
                            templateId = "t1",
                            exerciseId = "back-squat",
                            position = 0,
                            exerciseName = "Back Squat",
                            primaryMuscle = MuscleGroup.QUADS,
                            equipment = Equipment.BARBELL,
                        ),
                    ),
                    prescriptions = prescriptions,
                ),
                onAddSet = { exerciseId, edit -> added = exerciseId to edit },
                onUpdateSet = { id, edit -> updated = id to edit },
                onRemoveSet = { removed = it },
                onSetRestCue = { exerciseId, rest, cue, rpe ->
                    restCue = SlotExercisePlan(exerciseId, rest, cue, rpe)
                },
                onDismiss = { dismissed = true },
            )
        }
    }

    @Test
    fun theDialog_namesTheSlot_andSaysWhenNothingIsPrescribed() {
        setDialog()

        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Back Squat").assertIsDisplayed()
        // An empty prescription is a legitimate state: the template's targets stand (N14).
        composeTestRule.onNodeWithText("No prescribed sets").assertIsDisplayed()
    }

    @Test
    fun addingASet_reportsEveryTypedTarget() {
        setDialog()

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionAddSet("back-squat")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_ROLE).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionSetRole(SetType.TOP_SET.name)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_WEIGHT).performTextInput("100")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_PERCENT).performTextInput("85")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_REPS_MIN).performTextInput("3")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_REPS_MAX).performTextInput("5")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_NOTE).performTextInput("grind")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_SAVE).performClick()

        assertThat(added?.first).isEqualTo("back-squat")
        val edit = added?.second
        assertThat(edit?.role).isEqualTo(SetType.TOP_SET)
        assertThat(edit?.targetWeightGrams).isEqualTo(100_000L)
        assertThat(edit?.targetPercentOf1Rm).isEqualTo(85)
        assertThat(edit?.targetRepsMin).isEqualTo(3)
        assertThat(edit?.targetRepsMax).isEqualTo(5)
        assertThat(edit?.note).isEqualTo("grind")
    }

    @Test
    fun aLegacyPerSetRpe_isRoundTripped_thoughTheSetFormNoLongerShowsIt() {
        // ROADMAP N59, amended: a slot's target RPE is one number per exercise now, so this form has
        // no RPE field — but a set that still carries a value from before the change must come back
        // from an edit with it intact rather than silently wiped.
        setDialog(
            listOf(
                SlotPrescription(
                    exerciseId = "back-squat",
                    sets = listOf(SlotSet(id = "ps1", setIndex = 0, targetRpeHalves = 19)),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionSet("ps1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_WEIGHT).performTextInput("100")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_SAVE).performClick()

        assertThat(updated?.second?.targetWeightGrams).isEqualTo(100_000L)
        assertWithMessage("the legacy per-set RPE is carried, not cleared")
            .that(updated?.second?.targetRpeHalves)
            .isEqualTo(19)
    }
    @Test
    fun addingASet_startsFromTheLastPrescribedSet() {
        // ROADMAP N46: Add set prefills here too, through this side's own lookup — the sets hang
        // off the editor rather than sitting in local scope.
        setDialog(
            listOf(
                SlotPrescription(
                    exerciseId = "back-squat",
                    sets = listOf(
                        SlotSet(id = "ps1", setIndex = 0, targetWeightGrams = 90_000L, targetRepsMax = 3),
                        SlotSet(id = "ps2", setIndex = 1, targetWeightGrams = 100_000L, targetRepsMax = 5),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionAddSet("back-squat")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_WEIGHT)
            .assertTextContains("100")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_REPS_MAX)
            .assertTextContains("5")
    }

    @Test
    fun anExistingSet_opensForEditing_andItsRowCanRemoveIt() {
        setDialog(
            listOf(
                SlotPrescription(
                    exerciseId = "back-squat",
                    sets = listOf(
                        SlotSet(
                            id = "ps1",
                            setIndex = 0,
                            targetWeightGrams = 100_000L,
                            targetRepsMax = 3,
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionSet("ps1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_WEIGHT).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_WEIGHT).performTextInput("110")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_SAVE).performClick()

        assertThat(updated?.first).isEqualTo("ps1")
        assertThat(updated?.second?.targetWeightGrams).isEqualTo(110_000L)

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionRemoveSet("ps1")).performClick()
        assertThat(removed).isEqualTo("ps1")
    }

    @Test
    fun theRestCueAndOneRpePerExercise_savesWhatWasTyped_andBackingOutSavesNothing() {
        // ROADMAP N59, amended: the effort is one number for the whole exercise, so it is edited
        // beside the rest and cue the slot already carried rather than on each prescribed set.
        setDialog()

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionRestCue("back-squat")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.REST_CUE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.REST_FIELD).performTextInput("150")
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_RPE).performTextInput("8")
        composeTestRule.onNodeWithTag(TestTags.Programs.CUE_FIELD).performTextInput("brace hard")
        composeTestRule.onNodeWithTag(TestTags.Programs.REST_CUE_SAVE).performClick()

        assertThat(restCue).isEqualTo(SlotExercisePlan("back-squat", 150, "brace hard", 16))

        // A second visit that is cancelled reports nothing, so the cancel control is not a write.
        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionRestCue("back-squat")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.REST_CUE_CANCEL).performClick()
        assertThat(restCue).isEqualTo(SlotExercisePlan("back-squat", 150, "brace hard", 16))

        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_CLOSE).performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun theSetForm_canBeDismissedWithNothingWritten() {
        setDialog()

        composeTestRule.onNodeWithTag(TestTags.Programs.prescriptionAddSet("back-squat")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Programs.PRESCRIPTION_SET_CANCEL).performClick()

        assertThat(added).isNull()
    }
}
