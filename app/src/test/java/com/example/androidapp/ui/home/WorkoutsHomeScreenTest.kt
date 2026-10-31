package com.example.androidapp.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutClock
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant

/**
 * The home screen (ROADMAP N1).
 *
 * Two of these moved here from the library's tests: the start/resume button used to
 * live there, and N1 is precisely the change that moved it. Leaving them behind
 * would have tested a button that no longer exists on that screen.
 *
 * Matched by tag, so a translation cannot break them.
 */
@RunWith(AndroidJUnit4::class)
// A window tall enough that the body's rows are composed together: two tests compare a foot row's bounds with
// the first way in, and a recycled top row would make that comparison impossible rather than untrue.
@Config(qualifiers = "w411dp-h1600dp")
class WorkoutsHomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var weeklyOpened = 0

    /**
     * Every callback the screen takes, grouped: the helper is a screen setter, and
     * one flat parameter per event stops reading as a call site long before it
     * stops compiling.
     */
    private data class Actions(
        val onStartWorkout: () -> Unit = {},
        val onOpenTemplates: () -> Unit = {},
        val onStartTemplate: (TodayPlan) -> Unit = {},
        val onSubstituteTemplate: (TodayPlan, String?, String?) -> Unit = { _, _, _ -> },
        val onSkipBlock: (TodayPlan) -> Unit = {},
        val onOpenPrograms: () -> Unit = {},
        val onOpenMeasurements: () -> Unit = {},
        val onMeasureToday: () -> Unit = {},
        val onOpenPlannedWorkout: (TodayPlan) -> Unit = {},
    )

    /**
     * Scrolls the body to a scheduled plan's card (N115).
     *
     * Through the list rather than the row: today's plan is last now, and a lazy list has not composed a row
     * that far down, so `onNodeWithTag(row).performScrollTo()` cannot resolve the node it is asked to scroll.
     */
    private fun scrollToToday(id: String = "slot-1") {
        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.Home.startPlan(id)))
    }

    /**
     * Scrolls the body to the line a day with nothing on it draws (N126).
     *
     * Through the list for the reason [scrollToToday] is: it sits at the foot, below the ways in, and a lazy
     * list has not composed it until the list is scrolled.
     */
    private fun scrollToNoPlan() {
        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.HOME_NO_PLAN))
    }

    /** Scrolls the body to one row, which the lazy list has not composed until asked (B182). */
    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag(TestTags.HOME_LIST).performScrollToNode(hasTestTag(tag))
    }

    private fun setScreen(
        state: WorkoutsHomeUiState,
        actions: Actions = Actions(),
        message: String? = null,
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                WorkoutsHomeScreen(
                    state = state,
                    clock = remember { mutableStateOf(WorkoutClock()) },
                    onStartWorkout = actions.onStartWorkout,
                    onOpenTemplates = actions.onOpenTemplates,
                    onStartTemplate = actions.onStartTemplate,
                    onOpenPlannedWorkout = actions.onOpenPlannedWorkout,
                    onSubstituteTemplate = actions.onSubstituteTemplate,
                    onSkipBlock = actions.onSkipBlock,
                    onOpenPrograms = actions.onOpenPrograms,
                    onOpenMeasurements = actions.onOpenMeasurements,
                    onMeasureToday = actions.onMeasureToday,
                    onOpenWeeklyReview = { weeklyOpened++ },
                    message = message,
                    onDismissMessage = {},
                )
            }
        }
    }

    @Test
    fun theWeekInReviewRow_isAtTheFoot_andOpens() {
        setScreen(WorkoutsHomeUiState(isLoading = false))

        scrollTo(TestTags.HOME_WEEKLY_REVIEW)
        composeTestRule.onNodeWithTag(TestTags.HOME_WEEKLY_REVIEW).performClick()

        assertEquals(1, weeklyOpened)
    }

    @Test
    fun withNothingLogged_theBodyIsTheWaysIn() {
        // N102: with no history and nothing running the screen is never blank — the four ways in *are* the
        // body — so the first-run message that explained an empty list went with the list it explained.
        var started = false
        setScreen(WorkoutsHomeUiState(isLoading = false), Actions(onStartWorkout = { started = true }))

        listOf(
            TestTags.HOME_PROGRAMS,
            TestTags.HOME_TEMPLATES,
            TestTags.HOME_MEASUREMENTS,
            TestTags.HOME_START,
        ).forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(TestTags.HOME_START).performClick()

        assertThat(started).isTrue()
    }

    @Test
    fun withAWorkoutRunning_theButtonOffersToResume() {
        setScreen(
            WorkoutsHomeUiState(
                isLoading = false,
                activeWorkout = ActiveWorkoutInfo(
                    startedAt = Instant.parse("2026-09-29T10:00:00Z"),
                    exerciseCount = 3,
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_RESUME).assertIsDisplayed()
    }

    @Test
    fun withNothingRunning_theBarCarriesNoPill() {
        // N102: starting empty is a row in the body, so the bar is the resume pill and the next-up block —
        // and with neither it is empty, rather than offering a start that already exists above it.
        setScreen(WorkoutsHomeUiState(isLoading = false))

        composeTestRule.onNodeWithTag(TestTags.HOME_RESUME).assertDoesNotExist()
    }

    @Test
    fun theStartAction_offersBothWaysToBegin() {
        // N3, moved by N102: the choice is the body's rows now rather than the bar's — start empty, or go
        // and pick a plan — and both are still one tap from the screen the app opens on.
        var templates = false
        setScreen(
            WorkoutsHomeUiState(isLoading = false),
            Actions(onOpenTemplates = { templates = true }),
        )

        scrollTo(TestTags.HOME_START)
        composeTestRule.onNodeWithTag(TestTags.HOME_START).assertIsDisplayed()
        scrollTo(TestTags.HOME_TEMPLATES)
        composeTestRule.onNodeWithTag(TestTags.HOME_TEMPLATES).performClick()

        assertThat(templates).isTrue()
    }

    @Test
    fun withAWorkoutRunning_thePlanLinksStay() {
        // ROADMAP N78: hiding them was how the app said "you are in a workout", and a lifter checking
        // what is next should not have to finish one to look — so the links stay and the pill below
        // still says the primary act is Resume.
        var templates = false
        setScreen(
            WorkoutsHomeUiState(
                isLoading = false,
                activeWorkout = ActiveWorkoutInfo(
                    startedAt = Instant.parse("2026-09-29T10:00:00Z"),
                    exerciseCount = 3,
                ),
            ),
            Actions(onOpenTemplates = { templates = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_TEMPLATES).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_PROGRAMS).assertIsDisplayed()

        assertThat(templates).isTrue()
    }

    @Test
    fun theActionRow_offersProgramsBesideTemplates() {
        // ROADMAP N42: Programs took the slot the repeat-last link gave up, so the screen the whole
        // scheduling half is edited from is in the action row rather than behind an overflow. The
        // link beside it is Templates, the destination "Start from template" used to open.
        var opened = false
        var templates = false
        setScreen(
            WorkoutsHomeUiState(isLoading = false),
            Actions(onOpenPrograms = { opened = true }, onOpenTemplates = { templates = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_PROGRAMS).assertIsDisplayed().performClick()
        assertThat(opened).isTrue()

        composeTestRule.onNodeWithTag(TestTags.HOME_TEMPLATES).assertIsDisplayed().performClick()
        assertThat(templates).isTrue()
    }

    @Test
    fun theDataActions_areNoLongerOnHome() {
        // ROADMAP N43: export, import and delete-everything act on the whole database, so they
        // moved to Settings. Home must not still offer a second path to them.
        setScreen(WorkoutsHomeUiState(isLoading = false))

        composeTestRule.onNodeWithTag(TestTags.DATA_EXPORT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.DATA_IMPORT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_CLEAR_DATA).assertDoesNotExist()
    }

    @Test
    fun aMessage_isShown() {
        setScreen(WorkoutsHomeUiState(isLoading = false), message = "Exported 42 rows")

        composeTestRule.onNodeWithText("Exported 42 rows").assertIsDisplayed()
    }

    @Test
    fun theWaysIn_openTheirDestinations() {
        // N102: the body's rows are the ways in, and each reports the destination it stands for. They
        // replaced the recent list, which is also why nothing on this screen opens a past workout any
        // more — History is the tab beside this one (N42).
        var programs = false
        var templates = false
        var measurements = false
        setScreen(
            WorkoutsHomeUiState(isLoading = false),
            Actions(
                onOpenPrograms = { programs = true },
                onOpenTemplates = { templates = true },
                onOpenMeasurements = { measurements = true },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_PROGRAMS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_MEASUREMENTS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_TEMPLATES).performClick()

        assertThat(programs).isTrue()
        assertThat(templates).isTrue()
        assertThat(measurements).isTrue()
    }

    @Test
    fun todaysPlan_isShownUnderToday_withAStartAction() {
        // ROADMAP N16: home shows what is scheduled for today and offers to start it.
        val started = mutableListOf<TodayPlan>()
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                today = DayOfWeek.FRIDAY,
                todaysPlan = listOf(
                    TodayPlan(
                        id = "slot-1",
                        templateId = "t1",
                        name = "Heavy lower",
                        exerciseCount = 4,
                        // The slot travels with the row so its prescription seeds the workout
                        // (ROADMAP P3.8).
                        slotId = "slot-1",
                    ),
                ),
            ),
            actions = Actions(onStartTemplate = { started += it }),
        )

        // N115 moved this block below the ways in, so on a short viewport it is reached by scrolling the
        // list to it — the row itself is not composed until that has happened.
        scrollToToday()
        composeTestRule.onNodeWithText("Today · Friday").assertExists()
        composeTestRule.onNodeWithText("Heavy lower").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-1")).performClick()

        // The row's identity is the slot's; what starts is the template it points at, and the
        // slot travels with it (P3.3, P3.8).
        assertThat(started.single().templateId).isEqualTo("t1")
        assertThat(started.single().slotId).isEqualTo("slot-1")
    }

    @Test
    fun aDayWithNothingScheduled_showsNoTodayHeading() {
        // An empty "Today" heading would be a promise the app cannot keep — the line under it names the day.
        setScreen(WorkoutsHomeUiState(isLoading = false, today = DayOfWeek.MONDAY))

        composeTestRule.onNodeWithText("Today · Monday").assertDoesNotExist()
    }

    @Test
    fun withNothingFollowed_theSurfaceSaysNoWorkoutIsPlanned() {
        // ROADMAP N126: the day said nothing at all before this — the list simply ended.
        setScreen(WorkoutsHomeUiState(isLoading = false, hasActiveProgram = false))

        scrollToNoPlan()

        composeTestRule.onNodeWithTag(TestTags.HOME_NO_PLAN).assertExists()
        composeTestRule.onNodeWithText("No workout planned today").assertExists()
        // And it is not the rest-day sentence: there is no schedule to be resting from.
        composeTestRule.onNodeWithText("Rest day").assertDoesNotExist()
    }

    @Test
    fun anActiveProgramWithNothingToday_saysRest_ratherThanNothingPlanned() {
        // ROADMAP N126, and the case P3.9's rows existed for: a program is followed and schedules nothing
        // today. That is a rest day, not a screen with nothing to say.
        setScreen(WorkoutsHomeUiState(isLoading = false, hasActiveProgram = true))

        scrollToNoPlan()

        composeTestRule.onNodeWithTag(TestTags.HOME_NO_PLAN).assertExists()
        composeTestRule.onNodeWithText("Rest day").assertExists()
        composeTestRule.onNodeWithText("Your program schedules nothing today.").assertExists()
        composeTestRule.onNodeWithText("No workout planned today").assertDoesNotExist()
    }

    @Test
    fun withAWorkoutRunning_theBarHoldsOnlyTheResume() {
        // N124 kept the bar to the *Resume* pill, and N125 took the next-up rows out of the body: with a
        // workout open the bar carries the pill alone and the body carries the day's plan, nothing else.
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                activeWorkout = ActiveWorkoutInfo(
                    startedAt = Instant.parse("2026-10-01T10:00:00Z"),
                    exerciseCount = 2,
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_RESUME).assertExists()
    }

    @Test
    fun withNothingRunning_theBarIsNotDrawnAtAll() {
        // N124: the bar was the *Resume* pill plus the run. With neither there is nothing to say, so it is
        // not drawn rather than drawn empty.
        setScreen(state = WorkoutsHomeUiState(isLoading = false))

        composeTestRule.onNodeWithTag(TestTags.HOME_RESUME).assertDoesNotExist()
    }

    @Test
    fun aDeloadDay_saysSo_onItsCard() {
        // ROADMAP N147: the card is still a workout to start, but it is the week's reduced session
        // rather than the slot's full one, and the subtitle is where that is said.
        setScreen(
            WorkoutsHomeUiState(
                isLoading = false,
                todaysPlan = listOf(
                    TodayPlan(
                        id = "d1",
                        templateId = "dt1",
                        name = "Heavy lower — deload",
                        exerciseCount = 3,
                        programName = "Upper/Lower",
                        isDeload = true,
                    ),
                ),
                hasActiveProgram = true,
            ),
        )

        scrollToToday("d1")
        composeTestRule.onNodeWithText("Deload · 3 exercises · Upper/Lower").assertIsDisplayed()
    }

    @Test
    fun todaysCard_opensWhatIsPlanned_insteadOfStartingIt() {
        // ROADMAP N55, N127: looking and starting are two gestures, and the card's own name is the one that
        // looks. Before N127 the preview was reachable only from a next-up row, so the day's own workout was
        // the one plan that could only be started.
        val opened = mutableListOf<TodayPlan>()
        val started = mutableListOf<TodayPlan>()
        val plan = TodayPlan(
            id = "slot-1",
            templateId = "t1",
            name = "Push",
            exerciseCount = 5,
            slotId = "slot-1",
            programName = "Upper/Lower",
        )
        setScreen(
            state = WorkoutsHomeUiState(isLoading = false, todaysPlan = listOf(plan)),
            actions = Actions(
                onStartTemplate = { started += it },
                onOpenPlannedWorkout = { opened += it },
            ),
        )

        scrollToToday()
        composeTestRule.onNodeWithTag(TestTags.Home.planPreview("slot-1")).performClick()

        assertThat(opened.single()).isEqualTo(plan)
        assertThat(started).isEmpty()
    }

    @Test
    fun thePlannedWorkoutDialog_listsWhatIsPlanned_inOrder() {
        // ROADMAP N55: the field opens the plan rather than the editor, so what it shows is the
        // workout's ordered exercises — the answer to "what is in this one".
        var dismissed = false
        composeTestRule.setContent {
            AndroidAppTheme {
                PlannedWorkoutDialog(
                    planned = PlannedWorkout(
                        plan = TodayPlan(
                            id = "slot-2",
                            templateId = "t2",
                            name = "Push",
                            exerciseCount = 2,
                            slotId = "slot-2",
                        ),
                        programName = "Upper/Lower",
                        exercises = listOf("Bench Press", "Overhead Press"),
                        isLoading = false,
                    ),
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Home.PLANNED_WORKOUT).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.PLANNED_WORKOUT_TITLE).assertExists()
        composeTestRule.onNodeWithText("Push").assertExists()
        composeTestRule.onNodeWithText("Upper/Lower").assertExists()
        composeTestRule.onNodeWithText("Bench Press").assertExists()
        composeTestRule.onNodeWithText("Overhead Press").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.PLANNED_WORKOUT_EMPTY).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Home.PLANNED_WORKOUT_CLOSE).performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun aPlanWithNothingInIt_saysSo_ratherThanShowingAnEmptyList() {
        composeTestRule.setContent {
            AndroidAppTheme {
                PlannedWorkoutDialog(
                    planned = PlannedWorkout(
                        plan = TodayPlan(
                            id = "slot-2",
                            templateId = "t2",
                            name = "Push",
                            exerciseCount = 0,
                        ),
                        programName = "Upper/Lower",
                        isLoading = false,
                    ),
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Home.PLANNED_WORKOUT_EMPTY).assertExists()
    }

    @Test
    fun severalPlansOnADay_areAllListed() {
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                today = DayOfWeek.FRIDAY,
                todaysPlan = listOf(
                    TodayPlan(id = "t1", templateId = "t1", name = "Heavy lower", exerciseCount = 0),
                    TodayPlan(id = "t2", templateId = "t2", name = "Push", exerciseCount = 0),
                ),
            ),
        )

        scrollToToday("t1")

        composeTestRule.onNodeWithText("Heavy lower").assertExists()
        composeTestRule.onNodeWithText("Push").assertExists()
    }

    @Test
    fun theSameTemplateTwiceInADay_isTwoRows() {
        // ROADMAP P3.3: a program may schedule one template in two slots, so a row is keyed
        // by the slot rather than by the template — keying by template would collide and crash.
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                today = DayOfWeek.FRIDAY,
                todaysPlan = listOf(
                    TodayPlan(id = "slot-1", templateId = "t1", name = "Heavy lower", exerciseCount = 3),
                    TodayPlan(id = "slot-2", templateId = "t1", name = "Heavy lower", exerciseCount = 3),
                ),
            ),
        )

        scrollToToday()

        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-1")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-2")).assertExists()
    }

    /**
     * While today has no daily entry, home offers to write one (ROADMAP N114).
     *
     * The row is the shortcut the measurements screen's own FAB cannot be: it opens the daily form directly
     * rather than asking which of N113's two records was meant, and it is drawn on the evidence — what is
     * stored for today — rather than on whether the lifter was ever asked.
     *
     * It is the day's block (N138, B239): under the day's own workout when there is one, and under the ways
     * in when there is not — the foot either way, so the scroll below is the same scroll.
     */
    @Test
    fun whileTodayHasNoEntry_theMeasureTodayRow_isOffered_andOpensTheForm() {
        var opened = false
        setScreen(
            state = WorkoutsHomeUiState(isLoading = false, todayIsMeasured = false),
            actions = Actions(onMeasureToday = { opened = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.HOME_MEASURE_TODAY))
        composeTestRule.onNodeWithTag(TestTags.HOME_MEASURE_TODAY).assertIsDisplayed().performClick()

        assertWithMessage("the row opens today's form, not the list").that(opened).isTrue()
    }

    @Test
    fun withTodaysMeasurementsRecorded_theRow_stays_andNamesTheDay() {
        // B182: the row used to disappear once the day was written, which read as the same missing row a
        // compact screen showed. It stays, and its words change — the same destination is an edit now.
        var opened = false
        setScreen(
            state = WorkoutsHomeUiState(isLoading = false, todayIsMeasured = true),
            actions = Actions(onMeasureToday = { opened = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.HOME_MEASURE_TODAY))
        composeTestRule.onNodeWithTag(TestTags.HOME_MEASURE_TODAY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Today’s measurements").assertExists()
        composeTestRule.onNodeWithTag(TestTags.HOME_MEASURE_TODAY).performClick()

        assertWithMessage("the row opens the day's entry, not a blank form").that(opened).isTrue()
    }

    @Test
    fun withNoPlan_theDayBlock_keepsItsPlaceAtTheFoot() {
        // B239: the no-plan line and the record row led the list when nothing was scheduled (B182), so the
        // two rows a lifter checks most swapped ends of the screen with the calendar. The day's block is
        // always last now; with a workout it keeps the same place (todaysPlan_sitsBelowTheWaysIn).
        setScreen(WorkoutsHomeUiState(isLoading = false, todayIsMeasured = false))

        scrollTo(TestTags.HOME_MEASURE_TODAY)
        val row = composeTestRule.onNodeWithTag(TestTags.HOME_MEASURE_TODAY).getUnclippedBoundsInRoot()
        val start = composeTestRule.onNodeWithTag(TestTags.HOME_START).getUnclippedBoundsInRoot()

        assertWithMessage("the day's rows must come after the last way in, plan or no plan")
            .that(row.top >= start.bottom)
            .isTrue()
    }

    @Test
    fun onARestDay_theDayBlock_keepsItsPlaceAtTheFoot_too() {
        // A rest day is the same block, so it keeps the same place: the rest line above the record row,
        // both under the ways in (N115, B239).
        setScreen(WorkoutsHomeUiState(isLoading = false, hasActiveProgram = true))

        scrollTo(TestTags.HOME_MEASURE_TODAY)
        val rest = composeTestRule.onNodeWithTag(TestTags.HOME_NO_PLAN).getUnclippedBoundsInRoot()
        val start = composeTestRule.onNodeWithTag(TestTags.HOME_START).getUnclippedBoundsInRoot()

        assertWithMessage("a rest day's line sits under the ways in, with the record row")
            .that(rest.top >= start.bottom)
            .isTrue()
    }

    @Test
    fun withAWorkoutRunning_theMeasureTodayRow_stays() {
        // B101 withholds *Start empty workout* while a session is open, because the bar's Resume pill is the
        // same action. **Recording is not starting**, so N114's row is not withheld with it: a lifter mid
        // workout is exactly who has something to write down.
        setScreen(
            WorkoutsHomeUiState(
                isLoading = false,
                activeWorkout = ActiveWorkoutInfo(startedAt = Instant.parse("2026-10-12T09:00:00Z"), exerciseCount = 4),
                todayIsMeasured = false,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.HOME_MEASURE_TODAY))
        composeTestRule.onNodeWithTag(TestTags.HOME_MEASURE_TODAY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOME_START).assertDoesNotExist()
    }

    @Test
    fun todaysPlan_sitsBelowTheWaysIn() {
        // N115: the ways in are the body and today's workout is the last thing in the view, where the start
        // bar's own next-up rows already sit (P3.9, N55) rather than at the top pointing the other way. A
        // tag-level assertion cannot see an order, so this is the bounds idiom the workout screen uses for
        // *Done* — and without it an untested reversal drifts back.
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                today = DayOfWeek.FRIDAY,
                todaysPlan = listOf(
                    TodayPlan(id = "slot-1", templateId = "t1", name = "Heavy lower", exerciseCount = 4),
                ),
            ),
        )

        scrollToToday()
        val today = composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-1"))
            .getUnclippedBoundsInRoot()
        val start = composeTestRule.onNodeWithTag(TestTags.HOME_START).getUnclippedBoundsInRoot()

        assertWithMessage("today's plan must come after the last way in, not before the first (N115)")
            .that(today.top >= start.bottom)
            .isTrue()
    }

    @Test
    fun whileAWorkoutIsOpen_theWaysInAreStillThere() {
        // B43 withheld a second way to *start* a workout, because that is a way to lose one. N78 keeps
        // that and separates it from *looking*: the ways in open the plans, and the Start control in
        // the templates list is disabled for as long as the session lasts, which is where the
        // withholding lives now (TemplatesScreenTest holds that half). N102 moved them into the body,
        // which is drawn whatever else is — and B101 extended B43 to the *empty* start, which the bar's
        // Resume pill already performs while a session runs: offering it here as well was the same action
        // twice on one screen, under a label that said "start" about a tap that resumes.
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                activeWorkout = ActiveWorkoutInfo(
                    startedAt = Instant.parse("2026-10-01T10:00:00Z"),
                    exerciseCount = 2,
                ),
            ),
        )

        listOf(TestTags.HOME_PROGRAMS, TestTags.HOME_TEMPLATES, TestTags.HOME_MEASUREMENTS).forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(TestTags.HOME_START).assertDoesNotExist()
    }

    @Test
    fun aBlocksCard_offersSkip_andAWeeklyCardDoesNot() {
        // B195: a block's session happens once, so its card carries its own Skip — the weekly card
        // has no such event to let go, and its Substitute is the only per-card action it offers.
        var skipped: TodayPlan? = null
        val block = TodayPlan(
            id = "slot-9",
            templateId = "t9",
            name = "Peak day 4",
            exerciseCount = 3,
            slotId = "slot-9",
            programName = "Peak",
            isSequence = true,
            sequencePosition = 4,
            sequenceLength = 9,
        )
        setScreen(
            state = WorkoutsHomeUiState(isLoading = false, today = DayOfWeek.FRIDAY, todaysPlan = listOf(block)),
            actions = Actions(onSkipBlock = { skipped = it }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_LIST)
            .performScrollToNode(hasTestTag(TestTags.homeSkipBlock("slot-9")))
        composeTestRule.onNodeWithTag(TestTags.homeSkipBlock("slot-9")).performClick()

        assertThat(skipped?.slotId).isEqualTo("slot-9")
        composeTestRule.onNodeWithTag(TestTags.homeSkipBlock("slot-1")).assertDoesNotExist()
    }

    @Test
    fun theSubstituteAction_offersTheTemplates_andReportsThePickByName() {
        // ROADMAP P3.11: the pick is made at the point of starting, from the row being started. ROADMAP B93:
        // the pick's **name** travels with it, so the start's own question names the workout it is about to
        // begin rather than the scheduled one it stands in for.
        var picked: Triple<TodayPlan, String?, String?>? = null
        setScreen(
            state = todayPlanWithTemplates,
            actions = Actions(
                onSubstituteTemplate = { plan, templateId, templateName ->
                    picked = Triple(plan, templateId, templateName)
                },
            ),
        )

        scrollToToday()
        composeTestRule.onNodeWithTag(TestTags.homeSubstitute("slot-1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.homeSubstituteTemplate("t2")).performClick()

        assertThat(picked?.first?.slotId).isEqualTo("slot-1")
        assertThat(picked?.second).isEqualTo("t2")
        assertThat(picked?.third).isEqualTo("Dumbbell version")
    }

    @Test
    fun theScheduledWorkout_clearsThePick_ratherThanStandingIn() {
        var picked: Pair<TodayPlan, String?>? = null
        setScreen(
            state = todayPlanWithTemplates,
            actions = Actions(
                onSubstituteTemplate = { plan, templateId, _ -> picked = plan to templateId },
            ),
        )

        scrollToToday()
        composeTestRule.onNodeWithTag(TestTags.homeSubstitute("slot-1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_CLEAR).performClick()

        // Null is "restore the slot's own workout", and the route starts nothing for it.
        assertThat(picked?.first?.slotId).isEqualTo("slot-1")
        assertThat(picked?.second).isNull()
    }

    private companion object {
        val todayPlanWithTemplates = WorkoutsHomeUiState(
            isLoading = false,
            today = DayOfWeek.FRIDAY,
            todaysPlan = listOf(
                TodayPlan(
                    id = "slot-1",
                    templateId = "t1",
                    name = "Heavy lower",
                    exerciseCount = 4,
                    slotId = "slot-1",
                ),
            ),
            templates = listOf(WorkoutTemplate(id = "t2", name = "Dumbbell version", exerciseCount = 3)),
        )
    }
}
