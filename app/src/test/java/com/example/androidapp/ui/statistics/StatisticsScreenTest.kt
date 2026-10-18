package com.example.androidapp.ui.statistics

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ExerciseTrendMetric
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PlanTarget
import com.example.androidapp.domain.model.RangeKind
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** What the Statistics screen offers (ROADMAP N35). */
@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val series = MetricSeries(
        key = MetricKey.Body(BodyMetric.WEIGHT),
        readings = listOf(
            MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 83_000.0),
            MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 82_000.0),
        ),
    )

    private fun setScreen(
        state: StatisticsUiState = StatisticsUiState(
            isLoading = false,
            range = StatisticsRange(RangeKind.LAST_MONTH),
            overview = StatisticsOverview(workouts = 12, volumeGrams = 96_000_000L, personalRecords = 4),
            series = series,
        ),
        onSelectRange: (StatisticsRange) -> Unit = {},
        onSelectMetric: (MetricKey) -> Unit = {},
        onSelectExercise: (String) -> Unit = {},
        onOpenMeasurements: (() -> Unit)? = null,
        onOpenAdherence: (() -> Unit)? = null,
        onSetGoal: (Double?) -> Unit = {},
        onSetWeightGoal: (WeightGoal?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                StatisticsScreen(
                    state = state,
                    onSelectRange = onSelectRange,
                    onSelectMetric = onSelectMetric,
                    onSelectExercise = onSelectExercise,
                    onOpenMeasurements = onOpenMeasurements,
                    onOpenAdherence = onOpenAdherence,
                    onSetGoal = onSetGoal,
                    onSetWeightGoal = onSetWeightGoal,
                )
            }
        }
    }

    @Test
    fun theThreeNumbers_areTheOverview() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.OVERVIEW_WORKOUTS).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.OVERVIEW_VOLUME).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.OVERVIEW_RECORDS).assertExists()
    }

    @Test
    fun aRecordCountThatWasNotAsked_isADashRatherThanAZero() {
        // Zero would claim "you set no records", which is a different statement from "not counted".
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                overview = StatisticsOverview(workouts = 3, volumeGrams = 1_000L, personalRecords = null),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.OVERVIEW_RECORDS)
            .assertTextEquals("—")
    }

    @Test
    fun theRangeChips_sayWhichIsSelected_andReportAChoice() {
        var chosen: StatisticsRange? = null
        setScreen(onSelectRange = { chosen = it })

        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_MONTH.name)).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_YEAR.name)).assertIsNotSelected()

        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_YEAR.name)).performClick()

        assertThat(chosen?.kind).isEqualTo(RangeKind.LAST_YEAR)
    }

    @Test
    fun thePicker_offersEverySeries_andReportsTheChoice() {
        var chosen: MetricKey? = null
        setScreen(onSelectMetric = { chosen = it })

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()

        // One from each group, rather than all twenty-one: the registry's own test proves it is complete,
        // and this is about the picker showing what the registry holds.
        val volume = MetricKey.Exercise(ExerciseTrendMetric.VOLUME)
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(volume.id)).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(
                MetricKey.Workout(
                    com.example.androidapp.domain.model.TrendMetric.RPE,
                ).id
            )
        ).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(
                MetricKey.Tape(
                    com.example.androidapp.domain.model.TapeSite.WAIST,
                ).id
            )
        ).assertExists()

        // Scrolled to rather than clicked where it sits: the registry has grown past a screenful, so the
        // picker scrolls and an off-screen item is a node that exists but takes no click (N130).
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(volume.id))
            .performScrollTo()
            .performClick()

        assertThat(chosen).isEqualTo(volume)
    }

    @Test
    fun thePicker_saysWhichGroup_eachSeriesIsIn() {
        // ROADMAP N130: the list went flat once two series could share a label — *Left knee* is both a
        // workout series (a session's ratings for the lift chosen) and a per-lift one (that lift's own
        // points) — and the trigger shows the label alone, so without headings the two are the same word
        // before and after choosing.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()

        val headingTops = MetricGroup.entries.map { group ->
            composeTestRule.onNodeWithTag(TestTags.Statistics.metricGroup(group.name))
                .assertExists()
                .getUnclippedBoundsInRoot()
                .top
        }
        // In the enum's order, which is the order the menu draws them in: a heading per group, not one
        // label repeated wherever the registry happened to list it.
        assertThat(headingTops).isInOrder()

        // Both sites exist, and each sits after its own group's heading and before the next one — which is
        // what makes the two *Left knee* entries distinguishable rather than the same words twice.
        val workoutSite = composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(MetricKey.JointSite(Joint.KNEE, Side.LEFT).id),
        ).assertExists().getUnclippedBoundsInRoot().top
        val liftSite = composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(MetricKey.ExerciseJointSite(Joint.KNEE, Side.LEFT).id),
        ).assertExists().getUnclippedBoundsInRoot().top
        assertThat(workoutSite).isGreaterThan(headingTops[MetricGroup.WORKOUT.ordinal])
        assertThat(workoutSite).isLessThan(headingTops[MetricGroup.EXERCISE.ordinal])
        assertThat(liftSite).isGreaterThan(headingTops[MetricGroup.EXERCISE.ordinal])
        assertThat(liftSite).isLessThan(headingTops[MetricGroup.BODY.ordinal])
    }

    @Test
    fun choosingCustom_asksForTheDatesBeforeItApplies() {
        // A chip that selected an unbounded window would make the chart look broken rather than empty, so
        // custom asks its question first and refuses to apply a window with one end.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.CUSTOM.name)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CUSTOM_FROM).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CUSTOM_TO).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CUSTOM_APPLY).assertIsNotEnabled()
    }

    @Test
    fun aCustomRange_saysSoInTheChips() {
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                range = StatisticsRange(
                    kind = RangeKind.CUSTOM,
                    from = java.time.LocalDate.of(2026, 8, 1),
                    to = java.time.LocalDate.of(2026, 8, 31),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.CUSTOM.name)).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_MONTH.name)).assertIsNotSelected()
    }

    @Test
    fun aTarget_isShownInTheMetricsOwnUnit_andEditable() {
        // Typed as 82 kilograms and reported as 82000 grams: the row shows what a person reads, and the
        // screen hands over what the app stores (ROADMAP N39).
        var reported: Double? = null
        var called = false
        setScreen(
            state = StatisticsUiState(isLoading = false, series = fatSeries(), goal = 1_800.0),
            onSetGoal = {
                called = true
                reported = it
            },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).performScrollTo()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).assertTextEquals("180.0")

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_FIELD).performTextInput("18")
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_CONFIRM).performClick()

        assertThat(called).isTrue()
        assertThat(reported).isEqualTo(180.0)
    }

    @Test
    fun clearingATarget_isOfferedOnlyWhenThereIsOne() {
        setScreen(state = StatisticsUiState(isLoading = false, series = fatSeries(), goal = 1_800.0))

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_CLEAR).assertExists()
    }

    @Test
    fun withNoTarget_theRowAsksForOne_ratherThanShowingZero() {
        // A target of zero would be a line along the bottom of every chart and a claim nobody made.
        setScreen(state = StatisticsUiState(isLoading = false, series = fatSeries()))

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).performScrollTo()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_SET).assertTextEquals("Set a target")
    }

    @Test
    fun theMovingAveragePeriod_isConfigurable_andSaysWhichIsOn() {
        // Seven by default, which is the reading a daily weigh-in wants: the mean of the last week against the
        // noise of the days (N40).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.MOVING_AVERAGE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.movingAveragePeriod(DAYS)).assertIsSelected()

        composeTestRule.onNodeWithTag(TestTags.Statistics.movingAveragePeriod(14)).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.movingAveragePeriod(14)).assertIsSelected()
    }

    @Test
    fun aQuantityMetric_anchorsItsAxisAtZero() {
        // The visible half of N38: a volume bar starts where volume starts, and the label says so. The bars
        // themselves are a canvas, which the app blanks out for screen readers — so this is the assertion
        // that can see the policy at all.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME)),
                series = MetricSeries(
                    key = MetricKey.Exercise(ExerciseTrendMetric.VOLUME),
                    readings = listOf(
                        MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 4_000_000.0),
                        MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 4_500_000.0),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MIN).assertTextEquals("0")
    }

    @Test
    fun theChartSaysWhenItStartsAndEnds_andOnWhatScale() {
        // A canvas cannot be read by a screen reader, and "when did this start and end" is the first question
        // even for a reader who can see the line (ROADMAP N37).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_FIRST_DATE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_LAST_DATE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MIN).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MAX).assertExists()
    }

    @Test
    fun oneReading_isANumberRatherThanALine() {
        // The chart needs two points to mean anything; the axis labels would be describing a line that is not
        // drawn, so they go with it.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                series = MetricSeries(
                    key = MetricKey.Body(BodyMetric.WEIGHT),
                    readings = listOf(MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 82_000.0)),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_FIRST_DATE).assertDoesNotExist()
    }

    @Test
    fun theReadingsAreCollapsed_untilTheyAreAskedFor() {
        // The chart is what the screen is for; a year of readings is a wall of numbers to anyone who has not
        // asked for one.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_TOGGLE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.reading(0)).assertDoesNotExist()
    }

    @Test
    fun expanding_putsTheAverageOnTop_andTheReadingsNewestFirst() {
        setScreen()

        // The screen scrolls and the chart's labels sit above this, so the control may be below the fold:
        // Compose dispatches a click at coordinates, and a node that is not on screen is not clickable.
        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_TOGGLE).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_AVERAGE).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.reading(0)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.reading(1)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.reading(2)).assertDoesNotExist()
    }

    @Test
    fun expanding_showsTheTrendBesideTheAverage() {
        // The row N36 deliberately left out, arriving with the line that gives it a number (N39).
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_TOGGLE).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_TREND).assertExists()
    }

    @Test
    fun withNothingRecorded_thereIsNoReadingsControl() {
        // A disclosure control that opens onto emptiness is worse than no control.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                series = MetricSeries(
                    key = MetricKey.Body(BodyMetric.WEIGHT),
                    readings = listOf(MetricReading(Instant.parse("2026-09-01T08:00:00Z"), null)),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.READINGS_TOGGLE).assertDoesNotExist()
    }

    @Test
    fun measurementsAreReachable_fromHere() {
        // The regression this action fixes: deleting the trends screen took its measurements link with it,
        // and the screen was registered but reachable from nowhere.
        var opened = false
        setScreen(onOpenMeasurements = { opened = true })

        composeTestRule.onNodeWithTag(TestTags.Statistics.MEASUREMENTS).performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun withoutAWiredAction_thereIsNoMeasurementsButton() {
        // Like every other optional action here: a preview or a test that does not exercise it gets no dead
        // control.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.MEASUREMENTS).assertDoesNotExist()
    }

    @Test
    fun adherenceIsReachable_fromHere() {
        // The aggregate over the session-level plan-versus-actual (ROADMAP P3.5), pushed from the tab that
        // answers how everything is going rather than earning a sixth tab (N34).
        var opened = false
        setScreen(onOpenAdherence = { opened = true })

        composeTestRule.onNodeWithTag(TestTags.Statistics.ADHERENCE).performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun withoutAWiredAdherenceAction_thereIsNoButton() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.ADHERENCE).assertDoesNotExist()
    }

    @Test
    fun theLiftPicker_offersTheLibrary_andReportsTheChoice() {
        var chosen: String? = null
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME)),
                lifts = listOf(
                    lift("back-squat", "Back Squat"),
                    lift("bench-press", "Barbell Bench Press"),
                ),
            ),
            onSelectExercise = { chosen = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.lift("bench-press")).performClick()

        assertThat(chosen).isEqualTo("bench-press")
    }

    @Test
    fun aCategory_readsAsAFamily_whereTheMetricTakesOne() {
        // ROADMAP N97, B98, N110: a **category** heads a family — the movements filed under it — so it reads
        // as the whole family under a metric that takes a head, and the dropdown says so before it is chosen.
        val category = lift("cat-bench", "Bench Press", rowKind = RowKind.CATEGORY)
        val movement = lift("barbell-bench-press", "Barbell Bench Press", parentId = category.id)
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(
                    metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME),
                    exerciseId = category.id,
                ),
                lifts = listOf(category, movement),
                acceptsHead = true,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT)
            .assertTextContains("Bench Press · all exercises")

        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT).performClick()
        composeTestRule.onNodeWithText("All in this family").assertExists()
    }

    @Test
    fun aMovement_readsAsOneLift_whereTheMetricDoesNot() {
        // The other half of B98: a movement is always the one lift it is (N110), so under a metric that reads
        // a single id the label promises exactly that — "all exercises" would promise a family it never draws.
        val category = lift("cat-bench", "Bench Press", rowKind = RowKind.CATEGORY)
        val movement = lift("barbell-bench-press", "Barbell Bench Press", parentId = category.id)
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(
                    metric = MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
                    exerciseId = movement.id,
                ),
                lifts = listOf(category, movement),
                acceptsHead = false,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT).assertTextEquals("Barbell Bench Press")

        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT).performClick()
        composeTestRule.onNodeWithText("All in this family").assertDoesNotExist()
    }

    private fun lift(
        id: String,
        name: String,
        rowKind: RowKind = RowKind.MOVEMENT,
        parentId: String? = null,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        parentId = parentId,
        rowKind = rowKind,
    )

    @Test
    fun aMetricThatNeedsALift_asksForOne() {
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME)),
                series = series,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHOOSE_LIFT).assertExists()
    }

    @Test
    fun aMetricThatNeedsNothing_doesNotAsk() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHOOSE_LIFT).assertDoesNotExist()
    }

    @Test
    fun withALiftAlreadyChosen_thePickerIsStillOffered() {
        // The defect this guards: the picker was drawn only while *nothing* was selected, and every entry
        // point selects a lift, so arriving from the library or from a lift just performed left no way to
        // change it. The prompt is for the empty case; the control is for either.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(
                    metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME),
                    exerciseId = "back-squat",
                ),
                lifts = listOf(lift("back-squat", "Back Squat"), lift("bench-press", "Barbell Bench Press")),
                series = series,
            ),
        )

        // No question, because an answer is already on screen...
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHOOSE_LIFT).assertDoesNotExist()
        // ...but the control that changes the answer is.
        composeTestRule.onNodeWithTag(TestTags.Statistics.LIFT).assertExists()
    }

    @Test
    fun theChosenLift_isTheOneTheControlNames() {
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                selection = StatisticsSelection(
                    metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME),
                    exerciseId = "bench-press",
                ),
                lifts = listOf(lift("back-squat", "Back Squat"), lift("bench-press", "Barbell Bench Press")),
                series = series,
            ),
        )

        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
    }

    @Test
    fun loading_saysSo_ratherThanClaimingZero() {
        // The overview reads "0 workouts · 0 kg" from the default state, which is a claim about data that has
        // not been read yet — and the same zeros used to appear behind a failed read.
        setScreen(state = StatisticsUiState(isLoading = true))

        composeTestRule.onNodeWithText("Loading statistics…").assertExists()
    }

    @Test
    fun aFailedRead_saysSo_ratherThanClaimingZero() {
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                error = DataError.Storage(IOException("disk full")),
            ),
        )

        composeTestRule.onNodeWithText("Couldn’t save that. Your last change may not be stored.").assertExists()
    }

    @Test
    fun withNoGoal_theWeightSectionSaysSo_andSaysNothingElse() {
        // N135: the rate is derived from the goal, so a lifter with no goal has nothing to be short of — and
        // the row that sets the goal is the one above this.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET).assertTextEquals("Set a goal")
        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_STATEMENT).assertDoesNotExist()
    }

    @Test
    fun theDerivedRate_isShown_andThereIsNoFieldToTypeItIn() {
        // The pair that could disagree is gone: the goal sets the rate and this row reads it, so the field the
        // rate used to be typed into does not exist any more.
        setScreen(state = stateWithGoal(WeightGoal.Direction(gain = false)).copy(rateTarget = -262.5))

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET)
            .assertTextEquals("−0.262 kg per week")
    }

    @Test
    fun aTargetWeight_saysAroundWhen() {
        // N151: the date is the rate's implication, not a deadline the rate was derived from.
        setScreen(
            state = stateWithGoal(WeightGoal.Target(75_000L)).copy(
                rateTarget = -520.0,
                rateBand = (-640.0)..(-400.0),
                targetDate = LocalDate.of(2026, 12, 25),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET_DATE)
            .assertTextEquals("Around Dec 25, 2026")
    }

    @Test
    fun aReachedTarget_saysSo_ratherThanDatingIt() {
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_500L)).copy(
                targetReached = true,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET_DATE)
            .assertTextEquals("Target reached")
    }

    @Test
    fun theEnergyStatement_readsTheWholeSeries_ratherThanTheChartWindow() {
        // The chart can be showing seven days while the whole series has the eight readings over 21 days N98
        // requires. The verdict must speak, or the default range silences it by construction (B141).
        val whole = (0L..7L).map { week ->
            MetricReading(
                Instant.parse("2026-09-01T08:00:00Z").plus(Duration.ofDays(week * 7)),
                80_000.0 - 520.0 * week,
            )
        }
        setScreen(
            state = stateWithGoal(WeightGoal.Direction(gain = false)).copy(
                goalReadings = whole,
                rateTarget = -520.0,
                rateBand = (-640.0)..(-400.0),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_STATEMENT)
            .assertTextEquals("Your trend is where you asked it to be")
    }

    @Test
    fun theDerivedRate_isShown_withTheBandItIsJudgedAgainst() {
        // N149: the target is the number the adjustment works from; the band is what says whether the trend
        // is on it, and a loss reads as one negative interval rather than a magnitude to re-sign.
        setScreen(
            state = stateWithGoal(WeightGoal.Direction(gain = false)).copy(
                rateTarget = -520.0,
                rateBand = (-640.0)..(-400.0),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET)
            .assertTextEquals("−0.52 kg per week")
        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_BAND)
            .assertTextEquals("On target between −0.64 and −0.4 kg per week")
    }

    @Test
    fun withAGoal_andTooFewWeighIns_itSaysSoRatherThanGuessing() {
        // The floor N98 sets: two weigh-ins are a number and not a trend, so there is no kcal figure to draw —
        // and the section says that rather than leaving a gap where a number would be.
        setScreen(
            state = stateWithGoal(WeightGoal.Direction(gain = false)).copy(rateTarget = -700.0),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_STATEMENT)
            .assertTextEquals("Not enough weigh-ins yet")
    }

    @Test
    fun aLiftSeries_hasNoEnergySectionAtAll() {
        // The rate is the weight metric's: a lift's trend is not something anybody eats against.
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)).copy(
                series = MetricSeries(
                    key = MetricKey.Exercise(ExerciseTrendMetric.ESTIMATED_1RM),
                    readings = listOf(
                        MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 100_000.0),
                        MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 105_000.0),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY).assertDoesNotExist()
    }

    @Test
    fun aPlansTarget_isDrawn_andNamedUnderTheChart() {
        // N39: what the plan prescribes beside what the lifter did — a prescription, and named so it is not
        // read as the goal the lifter typed.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                range = StatisticsRange(RangeKind.LAST_MONTH),
                series = MetricSeries(
                    key = MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET),
                    readings = listOf(
                        MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 90_000.0),
                        MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 95_000.0),
                    ),
                ),
                planTargets = listOf(
                    PlanTarget(
                        templateId = "t1",
                        templateName = "Push day",
                        weightGrams = 100_000L,
                        reps = 5,
                        assistanceGrams = null,
                        rpeHalves = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Plans: Push day 100 kg").assertExists()
    }

    @Test
    fun withNoPlanPrescribingTheLift_thereIsNoLineUnderTheChart() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.PLAN_TARGETS).assertDoesNotExist()
    }

    @Test
    fun theGoalHistory_listsEachPeriod_withItsVerdict() {
        // N137: keeping the past is only worth it if it can be read — what the goal was in a period and whether
        // it was met.
        val context = ApplicationProvider.getApplicationContext<Context>()
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(
                    GoalPeriod(startedAt = null, kind = "TARGET", value = 75_000.0),
                    GoalPeriod(
                        startedAt = Instant.parse("2026-09-10T08:00:00Z"),
                        kind = "LOSE",
                        value = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(0)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(1)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.statistics_goal_unjudged)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.statistics_goal_missed)).assertExists()
    }

    @Test
    fun theHistory_isOfferedOnlyWhenThereIsAPast() {
        // One open period is what the row already says, so a button opening it would have nothing behind it.
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(GoalPeriod(startedAt = null, kind = "LOSE", value = null)),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).assertDoesNotExist()
    }

    @Test
    fun closingTheHistory_takesItAway() {
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(
                    GoalPeriod(startedAt = null, kind = "LOSE", value = null),
                    GoalPeriod(
                        startedAt = Instant.parse("2026-09-10T08:00:00Z"),
                        kind = "LOSE",
                        value = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_CLOSE).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_DIALOG).assertDoesNotExist()
    }

    @Test
    fun theWeightGoal_isChosenKindFirst_andClearedFromTheSameDialog() {
        // N135: the kind is the question and the weight only where the kind asks for one, so the two are set
        // in one place rather than as two numbers that can disagree.
        var set: WeightGoal? = null
        var cleared = false
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)),
            onSetWeightGoal = { value -> if (value == null) cleared = true else set = value },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.weightGoalKind(WeightGoalKind.MAINTAIN)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).performTextInput("80")
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertThat(set).isEqualTo(WeightGoal.Maintain(80_000L))

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CLEAR).performClick()

        assertThat(cleared).isTrue()
    }

    @Test
    fun aDirectionGoal_offersNoWeight_andInventsNone() {
        // N135's rule: the band decides the rate, and the app never manufactures an end weight the lifter did
        // not state — so the field is not offered at all, and a stale one cannot become a target.
        var set: WeightGoal? = null
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)),
            onSetWeightGoal = { value -> set = value },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.weightGoalKind(WeightGoalKind.LOSE)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertThat(set).isEqualTo(WeightGoal.Direction(gain = false))
    }

    @Test
    fun aMaintenanceGoal_readsAsACorridor_ratherThanARateOfZero() {
        // N135: "hold within ±X" is what makes maintenance a goal; a literal 0.0 kg/week is broken by the
        // first glass of water, so neither the goal row nor the rate row draws one.
        setScreen(state = stateWithGoal(WeightGoal.Maintain(80_000L)))

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).assertTextEquals("80 ±1")
        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET).assertTextEquals("80 ±1")
    }

    private fun stateWithGoal(
        goal: WeightGoal?,
        periods: List<GoalPeriod> = emptyList(),
    ) = StatisticsUiState(
        isLoading = false,
        range = StatisticsRange(RangeKind.LAST_MONTH),
        series = weightSeries(),
        weightGoal = goal,
        goalPeriods = periods,
        // The history judges each period against the readings in it (B141), which in a hand-built state is
        // the series the chart draws.
        goalReadings = weightSeries().readings,
    )
}

/** A pair of weigh-ins: enough for a chart, which is where the target line lives. */
private fun weightSeries() = MetricSeries(
    key = MetricKey.Body(BodyMetric.WEIGHT),
    readings = listOf(
        MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 83_000.0),
        MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 82_000.0),
    ),
)

/**
 * A body series that is **not** weight, for the tests of the plain level target (N135).
 *
 * The weight metric states a *kind* as well, so it has its own row; every other metric still sets one number.
 */
private fun fatSeries() = MetricSeries(
    key = MetricKey.Body(BodyMetric.BODY_FAT),
    readings = listOf(
        MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 1_830.0),
        MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 1_800.0),
    ),
)
