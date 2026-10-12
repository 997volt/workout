package com.example.androidapp.ui.measurements

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction

/**
 * The soreness list, where it lives now (ROADMAP N62, moved to the day's record by N113).
 *
 * The picker's own behaviour: one muscle at a time off a dropdown, a 1–10 score that steps, a removal
 * that sticks, and the taxonomy answering what may be picked. These are the tests the readiness note
 * used to carry — the interaction is unchanged, its home is the daily measurement.
 */
@RunWith(AndroidJUnit4::class)
class MeasurementSorenessTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var saved: BodyMeasurement? = null

    /** Opens the daily form, which is the only place the sore list is asked for now (N113). */
    private fun openDailyForm() {
        composeTestRule.setContent {
            AndroidAppTheme {
                MeasurementsScreen(
                    state = MeasurementsUiState(isLoading = false),
                    onSave = { saved = it },
                    onDelete = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD_DAILY).performClick()
    }

    /**
     * Opens the dropdown and takes one muscle.
     *
     * The button is reached through its semantics action rather than a tap: the form is taller than the
     * dialog once a muscle is on it, and a tap outside the visible area lands on the scrim and dismisses
     * the whole dialog — the same reason the template editor's foot is reached this way (N71).
     */
    private fun openTheMenu() {
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.add)
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun pick(muscle: MuscleGroup) {
        openTheMenu()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(muscle.name)).performClick()
    }

    @Test
    fun pickingAMuscle_addsIt_atTheBottomOfTheScale() {
        openDailyForm()

        pick(MuscleGroup.CHEST)

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.row(MuscleGroup.CHEST.name)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.score(MuscleGroup.CHEST.name))
            .assertTextEquals("1/10")
    }

    @Test
    fun aMuscleAlreadyPicked_isNotOfferedAgain() {
        openDailyForm()
        pick(MuscleGroup.CHEST)

        openTheMenu()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.CHEST.name))
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.LATS.name)).assertExists()
    }

    @Test
    fun adductorsAndTheBackGroupsAreOffered() {
        // N75: adductors joined the taxonomy, so it is a muscle a lifter can be sore in, and the three
        // groups *Back* split into are each their own answer.
        openDailyForm()

        openTheMenu()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.ADDUCTORS.name))
            .assertExists()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.LATS.name)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.UPPER_BACK.name))
            .assertExists()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.LOWER_BACK.name))
            .assertExists()
    }

    @Test
    fun theTaxonomysOther_isNotOffered() {
        // N62: `OTHER` is "not specified", so it is not a muscle anyone can be sore in.
        openDailyForm()

        openTheMenu()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.OTHER.name))
            .assertDoesNotExist()
    }

    @Test
    fun raisingAndLoweringAScore_movesItOnePointAtATime() {
        openDailyForm()
        pick(MuscleGroup.QUADS)
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.score(MuscleGroup.QUADS.name))
            .assertTextEquals("3/10")

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.decrease(MuscleGroup.QUADS.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.score(MuscleGroup.QUADS.name))
            .assertTextEquals("2/10")
    }

    @Test
    fun theScoreStopsAtTheEndsOfTheScale() {
        openDailyForm()
        pick(MuscleGroup.QUADS)

        // 1 is the floor: a muscle that is not sore is a muscle that is not on the list.
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.decrease(MuscleGroup.QUADS.name))
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
            .assertIsEnabled()

        // And 10 is the ceiling, whichever button is held.
        repeat(9) {
            composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
                .performClick()
        }
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.score(MuscleGroup.QUADS.name))
            .assertTextEquals("10/10")
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
            .assertIsNotEnabled()
    }

    @Test
    fun removingAMuscle_takesItOffTheList() {
        openDailyForm()
        pick(MuscleGroup.QUADS)

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.remove(MuscleGroup.QUADS.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.row(MuscleGroup.QUADS.name))
            .assertDoesNotExist()
        // Taken off, so it is on offer again — which is what makes a removal a removal.
        openTheMenu()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.QUADS.name))
            .assertExists()
    }

    @Test
    fun saving_reportsTheWholeList_onTheDaysOwnRecord() {
        openDailyForm()
        pick(MuscleGroup.QUADS)
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.increase(MuscleGroup.QUADS.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        assertEquals(listOf(SoreMuscle(MuscleGroup.QUADS, 2)), saved?.soreMuscles)
        assertEquals(MeasurementKind.DAILY, saved?.kind)
    }
}
