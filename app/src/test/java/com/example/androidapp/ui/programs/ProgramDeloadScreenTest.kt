package com.example.androidapp.ui.programs

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.DeloadSetTarget
import com.example.androidapp.domain.repository.DeloadCreation
import com.example.androidapp.ui.components.TestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The deload creator's screen (ROADMAP N147).
 *
 * The controls are the feature: how many days, which exercises, which share of the load, and the day
 * each workout is offered on. Asserted through the control that changes them rather than by reading the
 * state back, because the state is the ViewModel's and this is the surface.
 */
@RunWith(AndroidJUnit4::class)
class ProgramDeloadScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val week: LocalDate = LocalDate.of(2026, 10, 5)

    private fun exercise(
        templateExerciseId: String = "te1",
        exerciseId: String = "back-squat",
        name: String = "Back squat",
        selected: Boolean = true,
        sets: List<DeloadSetTarget> = List(2) {
            DeloadSetTarget(weightGrams = 60_000L, assistanceGrams = null, reps = 5)
        },
    ) = DeloadExerciseUi(
        templateExerciseId = templateExerciseId,
        exerciseId = exerciseId,
        name = name,
        selected = selected,
        sets = sets,
        weightUnit = WeightUnit.KILOGRAMS,
    )

    private fun day(
        slotId: String = "s1",
        name: String = "Deload A",
        weekday: DayOfWeek? = DayOfWeek.MONDAY,
        exercises: List<DeloadExerciseUi> = listOf(
            exercise(),
            exercise(
                templateExerciseId = "te2",
                exerciseId = "romanian-deadlift",
                name = "Romanian deadlift",
                sets = List(3) { DeloadSetTarget(weightGrams = 80_000L, assistanceGrams = null, reps = 8) },
            ),
        ),
    ) = DeloadDayUi(
        sourceSlotId = slotId,
        sourceName = name,
        weekday = weekday,
        exercises = exercises,
    )

    private val ready = DeloadCreatorUiState(
        isLoading = false,
        programName = "Upper/Lower",
        weekStart = week,
        days = listOf(day()),
        dayCount = 1,
        maxDays = 2,
        selectedExerciseCount = 2,
    )

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(TestTags.Deload.LIST)
            .performScrollToNode(hasTestTag(tag))
    }

    private fun setScreen(
        state: DeloadCreatorUiState = ready,
        onBack: () -> Unit = {},
        onToggleExercise: (Int, String) -> Unit = { _, _ -> },
        onDayCount: (Int) -> Unit = {},
        onLoadPercentChange: (Int) -> Unit = {},
        onWeekday: (String, DayOfWeek?) -> Unit = { _, _ -> },
        onCreate: () -> Unit = {},
        onUseLastDeload: () -> Unit = {},
        onOpenWorkouts: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProgramDeloadScreen(
                state = state,
                onBack = onBack,
                onToggleExercise = onToggleExercise,
                onDayCount = onDayCount,
                onLoadPercentChange = onLoadPercentChange,
                onWeekday = onWeekday,
                onCreate = onCreate,
                onUseLastDeload = onUseLastDeload,
                onOpenWorkouts = onOpenWorkouts,
                onDismissError = {},
            )
        }
    }

    @Test
    fun aLastDeload_isOffered_asAnAction() {
        // N150: the choices are one tap away when a previous plan exists; the share hint is explicit.
        var reused = false
        setScreen(
            state = ready.copy(lastDeloadWeek = week.minusWeeks(2)),
            onUseLastDeload = { reused = true },
        )

        composeTestRule.onNodeWithTag(TestTags.Deload.USE_LAST).performClick()
        assertThat(reused).isTrue()
    }

    @Test
    fun thePool_listsAnExerciseOnce_withADayChipForEachDay() {
        // N156: the pool used to repeat inside every day card, so a two-day week listed every exercise twice.
        setScreen(
            state = ready.copy(
                days = listOf(day(), day(slotId = "s2", name = "Deload B")),
                dayCount = 2,
                maxDays = 2,
            ),
        )

        scrollTo(TestTags.Deload.poolExercise("te1"))
        composeTestRule.onAllNodesWithTag(TestTags.Deload.poolExercise("te1")).assertCountEquals(1)
        scrollTo(TestTags.Deload.poolDay("te1", 1))
        composeTestRule.onNodeWithTag(TestTags.Deload.poolDay("te1", 1)).assertIsDisplayed()
    }

    @Test
    fun aDayChip_keepsThatExerciseOnThatDay() {
        var toggled: Pair<Int, String>? = null
        setScreen(
            state = ready.copy(
                days = listOf(day(), day(slotId = "s2", name = "Deload B")),
                dayCount = 2,
                maxDays = 2,
            ),
            onToggleExercise = { day, exerciseId -> toggled = day to exerciseId },
        )

        scrollTo(TestTags.Deload.poolDay("te2", 1))
        composeTestRule.onNodeWithTag(TestTags.Deload.poolDay("te2", 1)).performClick()

        assertThat(toggled).isEqualTo(1 to "te2")
    }

    @Test
    fun aDayHeader_showsItsNameAndNumber_andThePool_rowShowsTheDerivedPrescription() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Deload.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("Upper/Lower").assertIsDisplayed()
        scrollTo(TestTags.Deload.day("s1"))
        composeTestRule.onNodeWithText("Deload A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Day 1").assertIsDisplayed()

        scrollTo(TestTags.Deload.poolExercise("te1"))
        composeTestRule.onNodeWithText("2 × 5 · 60 kg").assertIsDisplayed()
    }

    @Test
    fun theDayCount_stepsWithinTheProgram() {
        var count: Int? = null
        setScreen(onDayCount = { count = it })

        composeTestRule.onNodeWithTag(TestTags.Deload.DAYS_INCREASE).performClick()
        assertThat(count).isEqualTo(2)

        composeTestRule.onNodeWithTag(TestTags.Deload.DAYS_VALUE).assertIsDisplayed()
    }

    @Test
    fun theIncrease_isOffAtTheProgramsLastDay() {
        setScreen(ready.copy(dayCount = 2))

        composeTestRule.onNodeWithTag(TestTags.Deload.DAYS_INCREASE).assertIsNotEnabled()
    }

    @Test
    fun theDecrease_isOffAtOneDay() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Deload.DAYS_DECREASE).assertIsNotEnabled()
    }

    @Test
    fun theLoadShare_isAChoice() {
        var share: Int? = null
        setScreen(onLoadPercentChange = { share = it })

        scrollTo(TestTags.Deload.load(70))
        composeTestRule.onNodeWithTag(TestTags.Deload.load(70)).performClick()

        assertThat(share).isEqualTo(70)
    }

    @Test
    fun theWeekdayControl_saysWhatItChanges() {
        setScreen()

        scrollTo(TestTags.Deload.day("s1"))
        composeTestRule.onNodeWithTag(TestTags.Deload.weekday("s1"))
            .assertContentDescriptionEquals("Change the day for Deload A")
    }

    @Test
    fun aDay_canBeMovedToAnotherWeekday() {
        var picked: Pair<String, DayOfWeek?>? = null
        setScreen(onWeekday = { slot, day -> picked = slot to day })

        scrollTo(TestTags.Deload.day("s1"))
        composeTestRule.onNodeWithTag(TestTags.Deload.weekday("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Deload.weekdayOption("s1", "TUESDAY")).performClick()

        assertThat(picked).isEqualTo("s1" to DayOfWeek.TUESDAY)
    }

    @Test
    fun aDay_canBeMadeOrderOnly() {
        var picked: Pair<String, DayOfWeek?>? = null
        setScreen(onWeekday = { slot, day -> picked = slot to day })

        scrollTo(TestTags.Deload.day("s1"))
        composeTestRule.onNodeWithTag(TestTags.Deload.weekday("s1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Deload.weekdayOption("s1", "NONE")).performClick()

        assertThat(picked).isEqualTo("s1" to null)
    }

    @Test
    fun create_isOffWhenNothingIsTicked() {
        setScreen(ready.copy(selectedExerciseCount = 0))

        composeTestRule.onNodeWithTag(TestTags.Deload.CREATE).assertIsNotEnabled()
    }

    @Test
    fun create_isOnWhenThereIsSomethingToWrite() {
        var created = false
        setScreen(onCreate = { created = true })

        composeTestRule.onNodeWithTag(TestTags.Deload.CREATE).assertIsEnabled().performClick()

        assertThat(created).isTrue()
    }

    @Test
    fun aBuiltWeek_saysWhatItWrote_andOffersDone_andTheWorkouts() {
        var done = false
        var opened = false
        setScreen(
            ready.copy(
                created = DeloadCreation(
                    weekStart = week,
                    templateIds = listOf("t1", "t2"),
                ),
            ),
            onBack = { done = true },
            onOpenWorkouts = { opened = true },
        )

        composeTestRule.onNodeWithTag(TestTags.Deload.CREATED).assertIsDisplayed()
        composeTestRule.onNodeWithText("2 workouts", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Deload.CREATE).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Deload.VIEW_WORKOUTS).performClick()
        assertThat(opened).isTrue()

        composeTestRule.onNodeWithTag(TestTags.Deload.DONE).performClick()
        assertThat(done).isTrue()
    }

    @Test
    fun aFailedRead_saysSo_ratherThanClaimingTheProgramIsEmpty() {
        // ROADMAP B164: a storage failure is not an empty program.
        setScreen(
            ready.copy(
                isLoading = false,
                loadFailed = true,
                days = emptyList(),
                maxDays = 0,
                dayCount = 0,
                error = DataError.Storage(IOException("disk")),
            ),
        )

        composeTestRule.onNodeWithText("Couldn’t read this program’s plan.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Deload.CREATE).assertDoesNotExist()
    }

    @Test
    fun aProgramWithNothingToDeload_saysSo() {
        setScreen(ready.copy(isLoading = false, days = emptyList(), maxDays = 0, dayCount = 0))

        composeTestRule.onNodeWithText("This program has no workouts to deload.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Deload.CREATE).assertDoesNotExist()
    }
}
