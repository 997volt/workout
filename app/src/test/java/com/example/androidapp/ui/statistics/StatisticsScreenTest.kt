package com.example.androidapp.ui.statistics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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

    @Test
    fun theScaleReadsAlongTheFigure_ratherThanStackingAtItsTop() {
        // B259: the gutter asked for fillMaxHeight() inside a scrollable page, where the height constraint is
        // infinite and the modifier resolves to wrap-content — so the row took its height from the canvas and
        // the three numbers sat in a line at the top, naming no height at all.
        setScreen()

        val figure = composeTestRule.onNodeWithTag(TestTags.Statistics.CHART).getUnclippedBoundsInRoot()
        val figureHeight = figure.bottom - figure.top
        val top = composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MAX)
            .getUnclippedBoundsInRoot().top
        val foot = composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MIN)
            .getUnclippedBoundsInRoot().top

        assertWithMessage("the numbers are spread down the figure, not gathered at its top")
            .that(foot - top)
            .isGreaterThan(figureHeight / 2f)
        assertWithMessage("and the smallest one sits at the figure's foot")
            .that(foot)
            .isGreaterThan(figure.top + figureHeight * 0.75f)
    }

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
        onSelectRecent: (StatisticsSelection) -> Unit = {},
        onPinch: (scale: Float, focus: Float) -> Unit = { _, _ -> },
        onOpenMeasurements: (() -> Unit)? = null,
        onOpenAdherence: (() -> Unit)? = null,
        onSetGoal: (Double?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                StatisticsScreen(
                    state = state,
                    onSelectRange = onSelectRange,
                    onSelectMetric = onSelectMetric,
                    onSelectExercise = onSelectExercise,
                    onSelectRecent = onSelectRecent,
                    onPinch = onPinch,
                    onOpenMeasurements = onOpenMeasurements,
                    onOpenAdherence = onOpenAdherence,
                    onSetGoal = onSetGoal,
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
    fun theRange_saysWhichIsSelected_andReportsAChoice() {
        // N197: three ranges stand out and the rest sit behind one control, so choosing a rare range is the
        // control and then the range.
        var chosen: StatisticsRange? = null
        setScreen(onSelectRange = { chosen = it })

        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_MONTH.name)).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).assertIsNotSelected()

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_YEAR.name)).performClick()

        assertThat(chosen?.kind).isEqualTo(RangeKind.LAST_YEAR)
    }

    @Test
    fun theRecentSeries_areAShortcutAboveThePicker_withTheCurrentOneSelected() {
        // N212: the two or three series actually read, one tap away instead of a group and a search away,
        // and one of them reads as the series on screen. A per-lift series keeps its lift (B226).
        val weight = StatisticsSelection(metric = MetricKey.Body(BodyMetric.WEIGHT))
        val volume = StatisticsSelection(
            metric = MetricKey.Exercise(ExerciseTrendMetric.VOLUME),
            exerciseId = "bench",
        )
        val rpe = StatisticsSelection(
            metric = MetricKey.Workout(com.example.androidapp.domain.model.TrendMetric.RPE),
        )
        val bench = Exercise(
            id = "bench",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = emptyList(),
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
            rowKind = RowKind.MOVEMENT,
        )
        var chosen: StatisticsSelection? = null
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                range = StatisticsRange(RangeKind.LAST_MONTH),
                selection = weight,
                recentSeries = listOf(weight, volume, rpe),
                lifts = listOf(bench),
                series = series,
            ),
            onSelectRecent = { chosen = it },
        )

        val recentTop = composeTestRule.onNodeWithTag(TestTags.Statistics.recentSeries(weight.recentTag()))
            .assertIsSelected()
            .getUnclippedBoundsInRoot().top
        val pickerTop = composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC)
            .getUnclippedBoundsInRoot().top
        assertThat(recentTop).isLessThan(pickerTop)

        composeTestRule.onNodeWithTag(TestTags.Statistics.recentSeries(volume.recentTag()))
            .assertIsNotSelected()
            .assertTextEquals("Volume · Bench Press")
            .performClick()

        assertThat(chosen).isEqualTo(volume)
    }

    @Test
    fun theRecentRow_waitsUntilThereAreTwoSeries() {
        // One chip repeating the picker's own label is not a shortcut.
        val weight = StatisticsSelection(metric = MetricKey.Body(BodyMetric.WEIGHT))
        setScreen(
            state = StatisticsUiState(isLoading = false, recentSeries = listOf(weight), series = series),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.recentSeries(weight.recentTag()))
            .assertDoesNotExist()
    }

    @Test
    fun thePicker_canBeSearched_andSaysWhenNothingMatches() {
        // N198: the menu is every series the registry holds, so a word is the fastest way in.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_SEARCH).performTextInput("weight")

        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(MetricKey.Body(BodyMetric.WEIGHT).id))
            .assertExists()
        val rpe = MetricKey.Workout(com.example.androidapp.domain.model.TrendMetric.RPE)
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(rpe.id)).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_SEARCH).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_SEARCH).performTextInput("zzz")

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_NO_MATCHES).assertExists()
    }

    @Test
    fun theSearchReachesAnotherGroup_andTheRowSaysWhichItIsIn() {
        // B210: the menu opens on the selected series' group, and the group chips narrow the browse list — but a
        // query is not scoped to the group on screen. "weight" from the per-lift list used to answer "nothing
        // matched", and the two *Left knee* entries were the same two words without a group to tell them apart.
        setScreen()
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.metricGroupChoice(MetricGroup.EXERCISE.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_SEARCH).performTextInput("weight")

        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(MetricKey.Body(BodyMetric.WEIGHT).id))
            .assertExists()
            // The chips wait while a query narrows, so the row itself has to name the group it came from.
            .assertTextContains("Body", substring = true)
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_NO_MATCHES).assertDoesNotExist()
    }

    @Test
    fun thePicker_offersEveryGroup_andReportsAChoice() {
        // N199: one group's list is on screen at a time, so choosing a series is choosing its group and then
        // the series. The registry's own test proves the list is complete; this is about the picker showing it.
        var chosen: MetricKey? = null
        setScreen(onSelectMetric = { chosen = it })

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()

        // The menu opens on the selected series' group — weight, from the state the screen was given — and a
        // series from another group is not on that list.
        val volume = MetricKey.Exercise(ExerciseTrendMetric.VOLUME)
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(volume.id)).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Statistics.metricGroupChoice(MetricGroup.EXERCISE.name))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(volume.id)).performScrollTo().performClick()

        assertThat(chosen).isEqualTo(volume)
    }

    @Test
    fun thePicker_opensOnAGroup_andTheSitesSitBehindOneRow() {
        // N199: four group choices, then that group's own list; the twenty-four joint sites are one row
        // rather than twenty-four, at either level.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC).performClick()

        // The groups read in the enum's order, and the menu opens on the selected series' group.
        val tops = MetricGroup.entries.map { group ->
            composeTestRule.onNodeWithTag(TestTags.Statistics.metricGroupChoice(group.name))
                .assertExists()
                .getUnclippedBoundsInRoot()
                .top
        }
        assertThat(tops).isInOrder()
        val rpe = MetricKey.Workout(com.example.androidapp.domain.model.TrendMetric.RPE)
        composeTestRule.onNodeWithTag(TestTags.Statistics.metric(rpe.id)).assertDoesNotExist()

        // The workout group lists its own series and one row for the sites.
        composeTestRule.onNodeWithTag(TestTags.Statistics.metricGroupChoice(MetricGroup.WORKOUT.name))
            .performClick()
        composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(MetricKey.JointSite(Joint.KNEE, Side.LEFT).id),
        ).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_JOINT).performClick()

        // The site is two taps behind the row, and the per-lift twin stays in the other group's list.
        val workoutSite = TestTags.Statistics.metric(MetricKey.JointSite(Joint.KNEE, Side.LEFT).id)
        composeTestRule.onNodeWithTag(workoutSite).assertExists()
        // N205: the sites sit under their joint, so the list is a choice rather than a scroll.
        composeTestRule.onNodeWithTag(TestTags.Statistics.metricJointHeading(Joint.KNEE.name)).assertExists()
        composeTestRule.onNodeWithTag(
            TestTags.Statistics.metric(MetricKey.ExerciseJointSite(Joint.KNEE, Side.LEFT).id),
        ).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_JOINT_BACK).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.METRIC_JOINT).assertExists()
    }

    @Test
    fun choosingCustom_asksForTheDatesBeforeItApplies() {
        // A chip that selected an unbounded window would make the chart look broken rather than empty, so
        // custom asks its question first and refuses to apply a window with one end.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).performClick()
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

        // The control carries the active range's own name, so a window chosen from the menu is still said
        // on screen (N197).
        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Statistics.range(RangeKind.LAST_MONTH.name)).assertIsNotSelected()
    }

    @Test
    fun aRangeBehindMore_isNamedWithoutTheWordLast_onTheRow_andInFullInTheMenu() {
        // The row says the window in the shortest form the control can carry: "Last" is understood from the
        // control, and the three common chips already drop it (N197). Six months read longer than any other
        // control in the row, so it is named the same way — while the menu, where there is room, keeps the
        // full name (a request in use).
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                range = StatisticsRange(RangeKind.LAST_6_MONTHS),
                series = series,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE)
            .assertIsSelected()
            .assertTextEquals("6 months")

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).performClick()
        composeTestRule.onNodeWithText("Last 6 months").assertExists()
    }

    @Test
    fun aYearBehindMore_isAlsoNamedWithoutTheWordLast() {
        // B235: the six-month name was shortened but the year kept the word the row had dropped. The menu
        // keeps the full name, as it does for six months.
        setScreen(
            state = StatisticsUiState(
                isLoading = false,
                range = StatisticsRange(RangeKind.LAST_YEAR),
                series = series,
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE)
            .assertIsSelected()
            .assertTextEquals("1 year")

        composeTestRule.onNodeWithTag(TestTags.Statistics.RANGE_MORE).performClick()
        composeTestRule.onNodeWithText("Last year").assertExists()
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

        // N211: one control names the window on screen, and the three windows are behind it.
        val control = composeTestRule.onNodeWithTag(TestTags.Statistics.MOVING_AVERAGE)
        control.performScrollTo().assertExists()
        control.assertTextContains(DAYS.toString(), substring = true)

        control.performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.movingAveragePeriod(14)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.MOVING_AVERAGE)
            .assertTextContains("14", substring = true)
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
        // N196: the scale is a gutter beside the figure, not a row underneath it.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_AXIS).assertExists()
        // N209: and the time axis names a point between its ends.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_MIDDLE_DATE).assertExists()
    }

    @Test
    fun theSummaryNumbers_followTheFigure_ratherThanSittingAboveIt() {
        // N208: the three numbers summarise the window, and the window is what the figure is for.
        setScreen()

        val chart = composeTestRule.onNodeWithTag(TestTags.Statistics.CHART)
            .getUnclippedBoundsInRoot().top
        val overview = composeTestRule.onNodeWithTag(TestTags.Statistics.OVERVIEW_WORKOUTS)
            .getUnclippedBoundsInRoot().top

        assertThat(overview).isGreaterThan(chart)
    }

    @Test
    fun theScaleSitsBesideTheFigure_andCarriesItsMiddleToo() {
        // N196: three numbers against the figure's own top, middle and foot, so "min" and "max" say which end
        // of the line they belong to.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_AXIS)
            .onChildren()
            .assertCountEquals(3)
    }

    @Test
    fun theFigure_canBeOpenedAtFullHeight_andClosedAgain() {
        // N201: the card's height is the screen's choice; expanding is the reader overruling it for a moment
        // without leaving the range and metric they chose.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPAND).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED_FIGURE).assertExists()
        // N206: the dialog is named, so a screen reader announces what it opened.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED_TITLE).assertExists()
        // B213: and the window itself carries the name, rather than only the heading inside it.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
        // B219: the body scrolls, so a short window loses nothing but a swipe.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED_BODY)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        // N204: the expanded view carries the card's extras — its dates are on screen twice now.
        composeTestRule.onAllNodesWithTag(TestTags.Statistics.CHART_FIRST_DATE).assertCountEquals(2)

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_COLLAPSE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART_EXPANDED).assertDoesNotExist()
    }

    @Test
    fun touchingTheFigureReadsTheReadingUnderTheFinger() {
        // N200: a crosshair and a line of text, so a phone-sized figure can be interrogated without leaving it.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART).performTouchInput { down(centerLeft) }

        composeTestRule.onNodeWithTag(TestTags.Statistics.SCRUB_READOUT).assertExists()
        // N206: and it is announced, because a value only a sighted finger can have is half a feature.
        composeTestRule.onNodeWithTag(TestTags.Statistics.SCRUB_READOUT)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))

        // B216: and it goes when the finger does. A readout that outlived its touch would be a value nobody is
        // reading, drawn against a figure that is no longer being pointed at.
        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART).performTouchInput { up() }
        composeTestRule.onNodeWithTag(TestTags.Statistics.SCRUB_READOUT).assertDoesNotExist()
    }

    @Test
    fun pinchingTheFigure_reportsTheScaleAndItsFocus() {
        // N213: the figure reports a two-finger scale and where it is centred; the window it becomes is the
        // ViewModel's business (and is tested there).
        val zooms = mutableListOf<Pair<Float, Float>>()
        setScreen(onPinch = { scale, focus -> zooms += scale to focus })

        composeTestRule.onNodeWithTag(TestTags.Statistics.CHART).performTouchInput {
            down(0, center + Offset(-20f, 0f))
            advanceEventTime(16)
            down(1, center + Offset(20f, 0f))
            advanceEventTime(16)
            moveTo(0, center + Offset(-90f, 0f))
            advanceEventTime(16)
            moveTo(1, center + Offset(90f, 0f))
            advanceEventTime(16)
            up(0)
            up(1)
        }

        // Fingers moving apart is a zoom in, and they move symmetrically about the centre: every magnifying
        // event says a real scale and a focus at the middle, not merely some number in range (B237).
        val pinches = zooms.filter { (scale, _) -> scale > 1f }
        assertThat(pinches).isNotEmpty()
        // A real magnification happened, and once both fingers have moved the event is centred. The fingers
        // move one at a time, so the intermediate events are asymmetric on purpose.
        assertThat(pinches.any { (scale, _) -> scale > 1.5f }).isTrue()
        assertThat(pinches.any { (scale, focus) -> scale > 1f && focus in 0.45f..0.55f }).isTrue()
    }

    @Test
    fun aScreenReaderCanStepThroughTheReadings() {
        // B213: the readout is announced, but only a finger could move it. The figure carries the two steps, so
        // the reader N206 named can read every point on it without a touch.
        setScreen()

        val actions = composeTestRule.onNodeWithTag(TestTags.Statistics.CHART)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
        assertThat(actions).hasSize(2)

        actions[1].action()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.Statistics.SCRUB_READOUT).assertExists()
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

        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET)
            .assertTextEquals("Target reached")
        composeTestRule.onNodeWithTag(TestTags.Statistics.ENERGY_TARGET_DATE).assertDoesNotExist()
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
    fun aMaintenanceGoal_readsAsACorridor_ratherThanARateOfZero() {
        // N135: "hold within ±X" is what makes maintenance a goal; a literal 0.0 kg/week is broken by the
        // first glass of water, so neither the goal row nor the rate row draws one.
        setScreen(state = stateWithGoal(WeightGoal.Maintain(80_000L)))

        // The goal's own row is on the measurements screen now; what stays here is the rate it derives, drawn
        // as the corridor rather than as a zero.
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
