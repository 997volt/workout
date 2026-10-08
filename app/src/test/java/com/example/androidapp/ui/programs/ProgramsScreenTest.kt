package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The program list (ROADMAP P3.3).
 *
 * The two actions on a row are easy to transpose and impossible to notice by eye —
 * tapping the row edits the program, tapping Use makes it the one home follows — so both
 * are asserted by the tag that shows which one fired.
 */
@RunWith(AndroidJUnit4::class)
class ProgramsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: ProgramsUiState = twoPrograms,
        onCreate: (String) -> Unit = {},
        onOpen: (String) -> Unit = {},
        onSetActive: (String) -> Unit = {},
        onMoveProgram: (String, Int) -> Unit = { _, _ -> },
        onLoadProgram: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProgramsScreen(
                state = state,
                onCreateProgram = onCreate,
                onOpenProgram = onOpen,
                onBack = {},
                onSetActive = onSetActive,
                onMoveProgram = onMoveProgram,
                onLoadProgram = onLoadProgram,
            )
        }
    }

    @Test
    fun theLoadAction_opensTheProgramFilePicker() {
        // ROADMAP N47: the file's way in, beside the list it adds to.
        var loaded = false
        setScreen(onLoadProgram = { loaded = true })

        composeTestRule.onNodeWithTag(TestTags.Programs.LOAD).performClick()

        assertThat(loaded).isTrue()
    }

    @Test
    fun eachProgram_showsItsNameAndSlotCount() {
        setScreen()

        // Addressed by row tag rather than by text: the active program's name and slot count are on its
        // foot card too (N92), so the same strings are legitimately on screen twice.
        composeTestRule.onNodeWithTag(TestTags.Programs.row("p1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.row("p2")).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("4 workouts").assertCountEquals(2)
        composeTestRule.onNodeWithText("6 workouts").assertIsDisplayed()
    }

    @Test
    fun anEmptyList_explainsWhatAProgramIs() {
        setScreen(ProgramsUiState(isLoading = false, programs = emptyList()))

        composeTestRule.onNodeWithTag(TestTags.Programs.EMPTY).assertIsDisplayed()
    }

    @Test
    fun tappingARow_editsThatProgram() {
        var opened: String? = null
        setScreen(onOpen = { opened = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.row("p1")).performClick()

        assertThat(opened).isEqualTo("p1")
    }

    @Test
    fun theUseButton_makesThatProgramActive_ratherThanEditingIt() {
        var activated: String? = null
        var opened: String? = null
        setScreen(onSetActive = { activated = it }, onOpen = { opened = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.use("p2")).performClick()

        assertThat(activated).isEqualTo("p2")
        assertThat(opened).isNull()
    }

    @Test
    fun theActiveProgram_isLabelled_ratherThanOfferingToUseItself() {
        setScreen()

        // *In use* is on the row and on the foot card (N92), so this counts the label rather than
        // demanding the screen hold it once — the assertion is that no *Use* button stands beside it.
        composeTestRule.onAllNodesWithText("In use").assertCountEquals(2)
        composeTestRule.onNodeWithTag(TestTags.Programs.use("p1")).assertDoesNotExist()
        // The inactive one still offers the choice.
        composeTestRule.onNodeWithTag(TestTags.Programs.use("p2")).assertIsDisplayed()
    }

    @Test
    fun theActiveProgram_isDrawnAtTheFoot_asTheScreensOwnBar() {
        // ROADMAP N92: with one program the row a lifter came to open sat under the app bar — the far end
        // of a modern phone — while the only thing in reach was the action that makes another one. The card
        // is the screen's bottom bar now, so position is the assertion: it is below every list row.
        setScreen()

        val card = composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE_CARD)
        card.assertIsDisplayed()
        val cardTop = card.getUnclippedBoundsInRoot().top
        val lastRowBottom =
            composeTestRule.onNodeWithTag(TestTags.Programs.row("p2")).getUnclippedBoundsInRoot().bottom

        assertThat(cardTop >= lastRowBottom).isTrue()
        // The floating button is placed **above** the bar rather than over it, which is what keeps the two
        // from colliding without either being handed a width that has to match the other.
        val newButtonBottom =
            composeTestRule.onNodeWithTag(TestTags.Programs.NEW).getUnclippedBoundsInRoot().bottom
        assertThat(cardTop >= newButtonBottom).isTrue()
    }

    @Test
    fun theActiveCard_opensThatProgram() {
        var opened: String? = null
        setScreen(onOpen = { opened = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE_CARD).performClick()

        assertThat(opened).isEqualTo("p1")
    }

    @Test
    fun withSeveralActivePrograms_theCardIsOneOfThem_andTheListKeepsTheRest() {
        // P3.12 allows more than one, so the card is the one being acted on rather than "the" program;
        // the list above stays the reference it is, in the authored order, with both still readable.
        setScreen(
            ProgramsUiState(
                isLoading = false,
                programs = listOf(
                    WorkoutProgram(id = "p1", name = "Upper/Lower", slotCount = 4, isActive = true),
                    WorkoutProgram(id = "p2", name = "PPL", slotCount = 6, isActive = true),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.row("p1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Programs.row("p2")).assertIsDisplayed()
    }

    @Test
    fun withNoActiveProgram_thereIsNoCard() {
        setScreen(
            ProgramsUiState(
                isLoading = false,
                programs = listOf(WorkoutProgram(id = "p2", name = "PPL", slotCount = 6)),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.ACTIVE_CARD).assertDoesNotExist()
    }

    @Test
    fun theNewProgramAction_asksForAProgram() {
        var created: String? = null
        setScreen(onCreate = { created = it })

        composeTestRule.onNodeWithTag(TestTags.Programs.NEW).performClick()

        assertThat(created).isNotNull()
    }

    @Test
    fun theMoveButtons_reorderThePrograms_ratherThanEditingThem() {
        // The authored order is P3.12's whole point, so moving is asserted by the tag that fired
        // and by the row's own click not firing with it.
        var moved: Pair<String, Int>? = null
        var opened: String? = null
        setScreen(
            onMoveProgram = { id, delta -> moved = id to delta },
            onOpen = { opened = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Programs.moveProgram("p2", up = true)).performClick()

        assertThat(moved).isEqualTo("p2" to -1)
        assertThat(opened).isNull()
    }

    private companion object {
        val twoPrograms = ProgramsUiState(
            isLoading = false,
            programs = listOf(
                WorkoutProgram(id = "p1", name = "Upper/Lower", slotCount = 4, isActive = true),
                WorkoutProgram(id = "p2", name = "PPL", slotCount = 6),
            ),
        )
    }
}
