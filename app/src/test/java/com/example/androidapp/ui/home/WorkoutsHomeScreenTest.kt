package com.example.androidapp.ui.home

import com.google.common.truth.Truth.assertThat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutClock
import java.time.DayOfWeek
import java.time.Instant
import org.junit.Rule
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith

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
class WorkoutsHomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Every callback the screen takes, grouped: the helper is a screen setter, and
     * one flat parameter per event stops reading as a call site long before it
     * stops compiling.
     */
    private data class Actions(
        val onStartWorkout: () -> Unit = {},
        val onOpenTemplates: () -> Unit = {},
        val onStartTemplate: (TodayPlan) -> Unit = {},
        val onSubstituteTemplate: (TodayPlan, String?) -> Unit = { _, _ -> },
        val onStartSubstituteTemplate: (TodayPlan, String?) -> Unit = { _, _ -> },
        val onOpenWorkout: (String) -> Unit = {},
        val onOpenPrograms: () -> Unit = {},
        val onOpenPlannedWorkout: (NextUp) -> Unit = {},
    )

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
                    onStartSubstituteTemplate = actions.onStartSubstituteTemplate,
                    onOpenWorkout = actions.onOpenWorkout,
                    onOpenPrograms = actions.onOpenPrograms,
                    message = message,
                )
            }
        }
    }

    @Test
    fun withNothingLogged_theScreenPointsAtStart() {
        var started = false
        setScreen(WorkoutsHomeUiState(isLoading = false), Actions(onStartWorkout = { started = true }))

        // An empty list with no explanation tells a first-run user nothing.
        composeTestRule.onNodeWithTag(TestTags.HOME_FIRST_RUN).assertIsDisplayed()
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
        composeTestRule.onNodeWithTag(TestTags.HOME_START).assertDoesNotExist()
    }

    @Test
    fun theStartAction_offersBothWaysToBegin() {
        // N3: the start action presents the choice — empty, or from a plan set up
        // in advance. With a workout already open there is no choice to make.
        var templates = false
        setScreen(
            WorkoutsHomeUiState(isLoading = false),
            Actions(onOpenTemplates = { templates = true }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_START).assertIsDisplayed()
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
    fun recentWorkouts_areListed_andOpenTheirDetail() {
        var opened: String? = null
        setScreen(
            WorkoutsHomeUiState(isLoading = false, recent = listOf(summary("session-1"))),
            Actions(onOpenWorkout = { opened = it }),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_RECENT_ROW).performClick()

        assertThat(opened).isEqualTo("session-1")
    }

    @Test
    fun theRecentHeading_carriesNoWayOut() {
        // ROADMAP N42: "See all workouts" duplicated the History tab, which is one tap away and
        // always visible; the heading now names the section and nothing else.
        setScreen(WorkoutsHomeUiState(isLoading = false, recent = listOf(summary("session-1"))))

        composeTestRule.onNodeWithText("See all workouts").assertDoesNotExist()
    }

    private fun summary(id: String) = WorkoutSummary(
        id = id,
        startedAt = Instant.parse("2026-09-29T08:00:00Z"),
        finishedAt = Instant.parse("2026-09-29T09:00:00Z"),
        exerciseCount = 3,
        setCount = 12,
        volumeGrams = 1_000_000L,
    )

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

        composeTestRule.onNodeWithText("Today · Friday").assertExists()
        composeTestRule.onNodeWithText("Heavy lower").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-1")).performClick()

        // The row's identity is the slot's; what starts is the template it points at, and the
        // slot travels with it (P3.3, P3.8).
        assertThat(started.single().templateId).isEqualTo("t1")
        assertThat(started.single().slotId).isEqualTo("slot-1")
    }

    @Test
    fun aDayWithNothingScheduled_showsNoTodaySection() {
        // An empty "Today" heading would be a promise the app cannot keep.
        setScreen(WorkoutsHomeUiState(isLoading = false, today = DayOfWeek.MONDAY))

        composeTestRule.onNodeWithText("Today · Monday").assertDoesNotExist()
    }

    @Test
    fun aProgramWithNothingToday_offersWhereItsRunIs() {
        // ROADMAP P3.9: the run gives "which one is next" an answer on a day nothing is scheduled.
        val started = mutableListOf<TodayPlan>()
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                nextUp = listOf(
                    NextUp(
                        plan = TodayPlan(
                            id = "slot-2",
                            templateId = "t2",
                            name = "Push",
                            exerciseCount = 5,
                            slotId = "slot-2",
                        ),
                        programName = "Upper/Lower",
                        isAtStart = true,
                    ),
                ),
            ),
            actions = Actions(onStartTemplate = { started += it }),
        )

        composeTestRule.onNodeWithText("Next up").assertExists()
        // The small Start became the screen's second full-width pill, named apart from the empty one.
        // Addressed by tag: the caption is a user-visible string a translation changes.
        composeTestRule.onNodeWithTag(TestTags.Home.nextUpStart("slot-2")).assertExists()
        // N88: the program and the count are two lines rather than one *Upper/Lower · 5 exercises*.
        composeTestRule.onNodeWithText("Upper/Lower").assertExists()
        composeTestRule.onNodeWithText("5 exercises").assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.nextUpStart("slot-2")).performClick()

        // The slot travels with the start, so its prescription seeds the workout (P3.8).
        assertThat(started.single().slotId).isEqualTo("slot-2")
        assertThat(started.single().templateId).isEqualTo("t2")
    }

    @Test
    fun theNextUpBlock_sitsBelowTheStartPill() {
        // The start bar reads top to bottom: the links, the empty start, then the next-up block, so
        // the app's own suggestion is the last thing the thumb reaches. Checked by position rather
        // than by the order things happen to be composed in, which a rearranged Column would not show.
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                nextUp = listOf(
                    NextUp(
                        plan = TodayPlan(
                            id = "slot-2",
                            templateId = "t2",
                            name = "Push",
                            exerciseCount = 5,
                            slotId = "slot-2",
                        ),
                        programName = "Upper/Lower",
                        isAtStart = true,
                    ),
                ),
            ),
        )

        val start = composeTestRule.onNodeWithTag(TestTags.HOME_START).getUnclippedBoundsInRoot()
        val nextUp = composeTestRule.onNodeWithTag(TestTags.Home.nextUp("slot-2"))
            .getUnclippedBoundsInRoot()

        assertThat(nextUp.top).isGreaterThan(start.bottom)
    }

    @Test
    fun theNextUpField_opensWhatIsPlanned_insteadOfStartingIt() {
        // ROADMAP N55: looking and starting stop being the same gesture.
        val opened = mutableListOf<NextUp>()
        val started = mutableListOf<TodayPlan>()
        val nextUp = NextUp(
            plan = TodayPlan(
                id = "slot-2",
                templateId = "t2",
                name = "Push",
                exerciseCount = 5,
                slotId = "slot-2",
            ),
            programName = "Upper/Lower",
            isAtStart = true,
        )
        setScreen(
            state = WorkoutsHomeUiState(isLoading = false, nextUp = listOf(nextUp)),
            actions = Actions(
                onStartTemplate = { started += it },
                onOpenPlannedWorkout = { opened += it },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Home.nextUp("slot-2")).performClick()

        assertThat(opened.single()).isEqualTo(nextUp)
        assertThat(started).isEmpty()
    }

    @Test
    fun aNextUpRow_givesItsStartRoom_underItsOwnText() {
        // ROADMAP N86. Of the three gaps the entry named, a device showed this one to be the tight one: the
        // row's text ended about 4 dp above its pill, where the space between two rows measured ~24 dp and
        // the block's own top ~14 dp — both of which read fine. A minimum rather than a number, because what
        // matters is that the text is not against the action it offers.
        val nextUp = NextUp(
            plan = TodayPlan(
                id = "slot-2",
                templateId = "t2",
                name = "Push",
                exerciseCount = 5,
                slotId = "slot-2",
            ),
            programName = "Upper/Lower",
            isAtStart = true,
        )
        setScreen(state = WorkoutsHomeUiState(isLoading = false, nextUp = listOf(nextUp)))

        val field = composeTestRule.onNodeWithTag(TestTags.Home.nextUp("slot-2")).getUnclippedBoundsInRoot()
        val pill = composeTestRule.onNodeWithTag(TestTags.Home.nextUpStart("slot-2")).getUnclippedBoundsInRoot()

        assertThat(pill.top - field.bottom >= 8.dp).isTrue()
    }

    @Test
    fun aNextUpRowsCount_isAlwaysOnItsOwnLine() {
        // ROADMAP N88: the point is the shape of the row, not a wrap that happens once the text is long,
        // so a **one-word** program name must not pull the count back up beside it. Asserted by position:
        // the two lines share no vertical overlap, which a separator-joined line could not satisfy.
        val nextUp = NextUp(
            plan = TodayPlan(
                id = "slot-2",
                templateId = "t2",
                name = "Push",
                exerciseCount = 5,
                slotId = "slot-2",
            ),
            // Deliberately short: this is the case a natural wrap would put back on one line.
            programName = "A",
            isAtStart = true,
        )
        setScreen(state = WorkoutsHomeUiState(isLoading = false, nextUp = listOf(nextUp)))

        // Unmerged, because the row is one clickable node to a screen reader: the merged parent carries
        // both lines' text and would answer every lookup with the same box.
        val program = composeTestRule.onNodeWithText("A", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val count = composeTestRule.onNodeWithText("5 exercises", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertThat(count.top >= program.bottom).isTrue()
        // And the joined sentence is gone rather than drawn somewhere else.
        composeTestRule.onNodeWithText("A · 5 exercises").assertDoesNotExist()
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

        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-1")).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Home.startPlan("slot-2")).assertExists()
    }

    @Test
    fun aPastWorkoutsDate_isRenderedInItsOwnZone() {
        // ROADMAP B33 and B39: grouping was well covered while the rendering was not, and the
        // rendering is where the bug was. Home and history now agree — and the expected string is
        // produced by the same formatter, so this cannot drift with the machine's locale.
        val tokyo = summary("session-1").copy(
            startedAt = Instant.parse("2026-09-30T20:00:00Z"),
            zoneOffsetMinutes = 540,
        )
        setScreen(WorkoutsHomeUiState(isLoading = false, recent = listOf(tokyo)))

        val expected = HistoryFormat.date(
            tokyo.startedAt,
            zone = ZoneOffset.ofHours(9),
        )
        composeTestRule.onNodeWithText(expected, substring = true).assertIsDisplayed()
    }

    @Test
    fun whileAWorkoutIsOpen_thePlanLinksAreStillThere() {
        // B43 withheld a second way to *start* a workout, because that is a way to lose one. N78 keeps
        // that and separates it from *looking*: the two links open the plans, and the Start control in
        // the templates list is disabled for as long as the session lasts, which is where the
        // withholding lives now (TemplatesScreenTest holds that half).
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                recent = listOf(summary("session-1")),
                activeWorkout = ActiveWorkoutInfo(
                    startedAt = Instant.parse("2026-10-01T10:00:00Z"),
                    exerciseCount = 2,
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.HOME_PROGRAMS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOME_TEMPLATES).assertIsDisplayed()
    }

    @Test
    fun theSubstituteAction_offersTheTemplates_andReportsThePick() {
        // ROADMAP P3.11: the pick is made at the point of starting, from the row being started.
        var picked: Pair<TodayPlan, String?>? = null
        setScreen(
            state = todayPlanWithTemplates,
            actions = Actions(onSubstituteTemplate = { plan, templateId -> picked = plan to templateId }),
        )

        composeTestRule.onNodeWithTag(TestTags.homeSubstitute("slot-1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.homeSubstituteTemplate("t2")).performClick()

        assertThat(picked?.first?.slotId).isEqualTo("slot-1")
        assertThat(picked?.second).isEqualTo("t2")
    }

    @Test
    fun aNextUpSubstitute_startsThePick_andRecordsNothing() {
        // ROADMAP N85: a substitution is an event keyed by slot *and* week (P3.11), and a next-up row has no
        // week to key one by — the run is calendar-free (P3.9) and `ProgramRun` carries no week — so its pick
        // starts the session and records nothing. That is not the same as the run standing still, which an
        // earlier version of this comment claimed: the session names the template it trained, and P3.9's run
        // follows the last slot trained (see ProgramRunTest.aSessionNamingALaterSlot_advancesPastIt). The
        // recording callback is watched to prove the absence rather than assumed.
        var recorded: Pair<TodayPlan, String?>? = null
        var started: Pair<TodayPlan, String?>? = null
        val nextUp = NextUp(
            plan = TodayPlan(
                id = "slot-2",
                // The row's own plan, which the picker excludes from the list it offers (N55) — so "t2" is
                // still there to be chosen.
                templateId = "t1",
                name = "Push",
                exerciseCount = 5,
                slotId = "slot-2",
            ),
            programName = "Upper/Lower",
            isAtStart = true,
        )
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                nextUp = listOf(nextUp),
                templates = todayPlanWithTemplates.templates,
            ),
            actions = Actions(
                onSubstituteTemplate = { plan, templateId -> recorded = plan to templateId },
                onStartSubstituteTemplate = { plan, templateId -> started = plan to templateId },
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Home.nextUpSubstitute("slot-2")).performClick()
        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.homeSubstituteTemplate("t2")).performClick()

        assertThat(started?.first?.slotId).isEqualTo("slot-2")
        assertThat(started?.second).isEqualTo("t2")
        assertThat(recorded).isNull()
    }

    @Test
    fun aNextUpSubstitutesDialog_offersNothingToRestore() {
        // ROADMAP N85: a next-up row records no substitution, so the picker's *Restore the scheduled
        // workout* row has no pick to clear. It was offered anyway and dismissed without doing anything —
        // a control that cannot do anything is worse than no control (N53, N67) — so it is left out here,
        // while the card, which does write, still offers it (the test below).
        val nextUp = NextUp(
            plan = TodayPlan(
                id = "slot-2",
                templateId = "t1",
                name = "Push",
                exerciseCount = 5,
                slotId = "slot-2",
            ),
            programName = "Upper/Lower",
            isAtStart = true,
        )
        setScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                nextUp = listOf(nextUp),
                templates = todayPlanWithTemplates.templates,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Home.nextUpSubstitute("slot-2")).performClick()

        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.HOME_SUBSTITUTE_CLEAR).assertDoesNotExist()
        // The templates are still offered, so the dialog still does its one job.
        composeTestRule.onNodeWithTag(TestTags.homeSubstituteTemplate("t2")).assertExists()
    }

    @Test
    fun theScheduledWorkout_clearsThePick_ratherThanStandingIn() {
        var picked: Pair<TodayPlan, String?>? = null
        setScreen(
            state = todayPlanWithTemplates,
            actions = Actions(onSubstituteTemplate = { plan, templateId -> picked = plan to templateId }),
        )

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
