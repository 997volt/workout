package com.example.androidapp.ui.measurements

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.statistics.EnergyAdjustment
import com.example.androidapp.ui.statistics.MetricReading
import com.example.androidapp.ui.statistics.goalOutcomes
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The measurements screen (ROADMAP N32, two records since N113).
 *
 * The screen's own wiring: that *Add* asks which of the two records before it opens a form, that each
 * form states only its own kind's numbers, and that *Save* is offered once the entry states something
 * rather than once it states a weight.
 */
@RunWith(AndroidJUnit4::class)
class MeasurementsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = BodyMeasurement(
        id = "m1",
        measuredAt = Instant.parse("2026-10-02T08:00:00Z"),
        kind = MeasurementKind.DAILY,
        weightGrams = 82_400L,
        bodyFatTenths = 183,
    )

    private fun setScreen(
        state: MeasurementsUiState = MeasurementsUiState(isLoading = false, entries = listOf(today)),
        onSave: (BodyMeasurement) -> Unit = {},
        onDelete: (String) -> Unit = {},
        error: DataError? = null,
        onDismissError: () -> Unit = {},
        initialAdd: MeasurementKind? = null,
        initialEdit: BodyMeasurement? = null,
        pendingUndo: BodyMeasurement? = null,
        onUndo: () -> Unit = {},
        onDismissUndo: () -> Unit = {},
        rateVerdict: EnergyAdjustment? = null,
        onDismissRateVerdict: () -> Unit = {},
        onSaveGoalPeriod: (String?, LocalDate, LocalDate?, WeightGoal?) -> Unit = { _, _, _, _ -> },
        onRemoveGoalPeriod: (String) -> Unit = {},
        goalPeriodUi: GoalPeriodUi = GoalPeriodUi(),
    ) {
        composeTestRule.setContent {
            AndroidAppTheme {
                MeasurementsScreen(
                    state = state,
                    onSave = onSave,
                    onDelete = onDelete,
                    onBack = {},
                    error = error,
                    onDismissError = onDismissError,
                    initialAdd = initialAdd,
                    initialEdit = initialEdit,
                    pendingUndo = pendingUndo,
                    onUndo = onUndo,
                    onDismissUndo = onDismissUndo,
                    rateVerdict = rateVerdict,
                    onDismissRateVerdict = onDismissRateVerdict,
                    onSaveGoalPeriod = onSaveGoalPeriod,
                    onRemoveGoalPeriod = onRemoveGoalPeriod,
                    goalPeriodUi = goalPeriodUi,
                )
            }
        }
    }

    @Test
    fun arrivingWithTheDailyFormOpen_skipsTheQuestion() {
        // ROADMAP N114: home's row opens the *daily* form directly, rather than leaving the lifter to find the
        // FAB and answer which of N113's two records they meant. The list is behind it, so dismissing lands on
        // what was recorded rather than back on home.
        setScreen(initialAdd = MeasurementKind.DAILY)

        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD_DAILY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun arrivingOnADayAlreadyWritten_editsTheDay_ratherThanOpeningABlankForm() {
        // B182: the shortcut's blank form would replace the day's numbers on save, because the repository
        // keys a daily entry by its day rather than by id — so an entry that exists is opened as the edit
        // it is, holding what the day already says.
        setScreen(
            state = MeasurementsUiState(
                isLoading = false,
                entries = listOf(today),
                todayEntry = today,
            ),
            initialEdit = today,
        )

        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertTextContains("82.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.BODY_FAT).assertTextContains("18.3")
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsEnabled()
    }

    @Test
    fun theEditForm_carriesTheDay_andOffersToChangeIt() {
        // A request in use; no roadmap id covers it. A record's day was fixed when it was saved, so a morning
        // typed onto the wrong date could not be corrected anywhere — and the edit form is the one that
        // corrects a record.
        setScreen(initialEdit = today)

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE_CHANGE).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE_CONFIRM).assertIsDisplayed()
    }

    @Test
    fun theWeightGoal_sitsAtTheTopOfTheList() {
        // The row the statistics screen used to carry under its chart is this screen's first item now — and
        // "top" is the claim, so the test compares it with the entry it sits above (B216).
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)).copy(entries = listOf(today)),
        )

        val goalTop = composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot().top
        val entryTop = composeTestRule.onNodeWithTag(TestTags.Measurements.row(today.id))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot().top

        assertTrue("the goal sits above the entries it is about", goalTop < entryTop)
    }

    @Test
    fun theGoalHistory_listsEachPeriod_withItsVerdict() {
        // N137, moved here with the row it opens: keeping the past is only worth it if it can be read.
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(
                    GoalPeriod(id = "p0", startedAt = null, kind = "TARGET", value = 75_000.0),
                    GoalPeriod(
                        id = "p1",
                        startedAt = Instant.parse("2026-09-10T08:00:00Z"),
                        kind = "LOSE",
                        value = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(0)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(1)).assertExists()
    }

    @Test
    fun theGoalCard_waitsForTheFirstEmission() {
        // B225: the default state's `today` is LocalDate.MIN, and formatting it throws. The card is not
        // reachable until the real day is known.
        setScreen(state = MeasurementsUiState())

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL).assertDoesNotExist()
    }

    @Test
    fun theHistory_isOfferedAsSoonAsThereIsAPeriod() {
        // N215: the list is also where a past is added, so it cannot wait for a past to exist.
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(GoalPeriod(id = "p0", startedAt = null, kind = "LOSE", value = null)),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).assertExists()
    }

    @Test
    fun aPeriodRow_opensItsEditor_andSavesTheRowItNames() {
        // N215: the past is corrected by naming the row, not by laying a new boundary over it.
        var savedId: String? = "not called"
        setScreen(
            state = stateWithGoal(goal = WeightGoal.Direction(gain = false), periods = twoPeriods()),
            onSaveGoalPeriod = { id, _, _, _ -> savedId = id },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(0)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_DIALOG).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_START).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertEquals("the newest period, which the first row is", "p1", savedId)
    }

    @Test
    fun aPastGoal_canBeAddedFromTheList() {
        var savedId: String? = "not called"
        setScreen(
            state = stateWithGoal(goal = WeightGoal.Direction(gain = false), periods = twoPeriods()),
            onSaveGoalPeriod = { id, _, _, _ -> savedId = id },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_ADD).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertNull("an added period is a new row, not an edit", savedId)
    }

    @Test
    fun aPeriod_canBeRemovedFromItsEditor() {
        var removed: String? = null
        setScreen(
            state = stateWithGoal(goal = WeightGoal.Direction(gain = false), periods = twoPeriods()),
            onRemoveGoalPeriod = { removed = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(0)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_REMOVE).performClick()

        assertEquals("p1", removed)
    }

    @Test
    fun onlyAPeriodThatHasASuccessor_offersAnEnd() {
        // The end of a period is the next one's start, so the period still in force has none (N215).
        setScreen(
            state = stateWithGoal(goal = WeightGoal.Direction(gain = false), periods = twoPeriods()),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(0)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_END).assertDoesNotExist()
    }

    @Test
    fun anEarlierPeriod_offersTheEndThatClosesIt() {
        setScreen(
            state = stateWithGoal(goal = WeightGoal.Direction(gain = false), periods = twoPeriods()),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.goalOutcome(1)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_PERIOD_END).assertExists()
    }

    /** A goal with a past: the undated first period, then one that began in September. */
    private fun twoPeriods() = listOf(
        GoalPeriod(id = "p0", startedAt = null, kind = "TARGET", value = 75_000.0),
        GoalPeriod(id = "p1", startedAt = Instant.parse("2026-09-10T08:00:00Z"), kind = "LOSE", value = null),
    )

    @Test
    fun closingTheHistory_takesItAway() {
        setScreen(
            state = stateWithGoal(
                goal = WeightGoal.Direction(gain = false),
                periods = listOf(
                    GoalPeriod(id = "p0", startedAt = null, kind = "LOSE", value = null),
                    GoalPeriod(
                        id = "p1",
                        startedAt = Instant.parse("2026-09-10T08:00:00Z"),
                        kind = "LOSE",
                        value = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_CLOSE).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_HISTORY_DIALOG).assertDoesNotExist()
    }

    @Test
    fun theWeightGoal_isChosenKindFirst_andNoGoalIsOneOfTheKinds() {
        var saved: Pair<String?, WeightGoal?>? = null
        var started: LocalDate? = null
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)),
            onSaveGoalPeriod = { id, start, _, value ->
                saved = id to value
                started = start
            },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.weightGoalKind(WeightGoalKind.MAINTAIN)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).performTextInput("80")
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertEquals("the card opens a new period", null, saved?.first)
        assertEquals(WeightGoal.Maintain(80_000L), saved?.second)
        assertEquals("and it starts on the day the screen calls today", LocalDate.of(2026, 10, 2), started)

        // N215: clearing is a period of No goal, one of the kinds rather than a second button.
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.GOAL_KIND_NONE).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertEquals("a period that asks for nothing", null, saved?.second)
    }

    @Test
    fun aDirectionGoal_offersNoWeight_andInventsNone() {
        // N135's rule, moved here with the row: the band decides the rate, and the app never manufactures an end
        // weight the lifter did not state — so the field is not offered at all, and a stale one cannot become a
        // target. The test went with the statistics row and was not re-added (B215); it is back beside the row.
        var set: WeightGoal? = null
        setScreen(
            state = stateWithGoal(WeightGoal.Target(80_000L)),
            onSaveGoalPeriod = { _, _, _, value -> set = value },
        )

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_SET).performClick()
        composeTestRule.onNodeWithTag(TestTags.Statistics.weightGoalKind(WeightGoalKind.LOSE)).performClick()

        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM).performClick()

        assertEquals(WeightGoal.Direction(gain = false), set)
    }

    /** A state whose goal the row can read: the readings are the two the history is judged against. */
    private fun stateWithGoal(
        goal: WeightGoal?,
        periods: List<GoalPeriod> = emptyList(),
    ) = MeasurementsUiState(
        isLoading = false,
        today = LocalDate.of(2026, 10, 2),
        weightGoal = goal,
        goalHistory = goalOutcomes(
            periods,
            listOf(
                MetricReading(Instant.parse("2026-09-01T08:00:00Z"), 83_000.0),
                MetricReading(Instant.parse("2026-09-15T08:00:00Z"), 82_000.0),
            ),
        ),
    )

    @Test
    fun theAddForm_doesNotOfferTheDay() {
        // A new entry belongs to today: the day is what an edit corrects, not something to choose while
        // adding.
        setScreen(initialAdd = MeasurementKind.DAILY)

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE).assertDoesNotExist()
    }

    /** The two taps every add takes now: the button, then which of the two records (N113). */
    private fun startAdding(kind: MeasurementKind) {
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).performClick()
        composeTestRule.onNodeWithTag(
            if (kind == MeasurementKind.DAILY) {
                TestTags.Measurements.ADD_DAILY
            } else {
                TestTags.Measurements.ADD_TAPE
            },
        ).performClick()
    }

    @Test
    fun aFailedWrite_isSurfaced_ratherThanSilent() {
        // B102: the ViewModel reported a failure, but nothing read it, so a save or delete that did not
        // land said nothing at all. Every other screen shows the same message through a snackbar.
        val context = ApplicationProvider.getApplicationContext<Context>()
        setScreen(error = DataError.Storage(IllegalStateException()))

        composeTestRule.onNodeWithText(context.getString(R.string.workout_error_storage)).assertExists()
    }

    @Test
    fun aLongList_canBeScrolledToItsOldestEntry() {
        // B130: the list was a `Column` that could not scroll, so the first screenful was all there was —
        // a lifter with a few weeks of daily entries could not see, edit or delete anything older.
        val stale = (1..30).map { day ->
            BodyMeasurement(
                id = "m$day",
                measuredAt = Instant.parse("2026-10-02T08:00:00Z").minusSeconds(day * 86_400L),
                kind = MeasurementKind.DAILY,
                weightGrams = 82_000L,
            )
        }
        setScreen(state = MeasurementsUiState(isLoading = false, entries = stale))

        // The last entry is the oldest, which is the one a non-scrolling list could never reach.
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.LIST)
            .performScrollToNode(hasTestTag(TestTags.Measurements.delete("m30")))
        composeTestRule.onNodeWithTag(TestTags.Measurements.delete("m30")).assertIsDisplayed()
    }

    @Test
    fun withNothingRecorded_itSaysSo() {
        setScreen(state = MeasurementsUiState(isLoading = false))

        // The empty state is the whole point of the list being empty rather than blank — and it is an
        // item of the list now (B130), so it scrolls with everything else.
        composeTestRule.onNodeWithTag(TestTags.Measurements.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).assertIsEnabled()
    }

    @Test
    fun anEntryCanBeDeleted_byItsOwnId_onceTheModeIsEntered() {
        // N132: the delete is behind the toggle, so the same button that was on every row of a reading
        // screen is now two deliberate taps — enter the mode, then delete.
        var deleted: String? = null
        setScreen(onDelete = { deleted = it })

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.delete("m1")).performClick()

        assertEquals("m1", deleted)
    }

    @Test
    fun theDelete_isNotOnTheScreenTheLifterOpenedToRead() {
        // The complaint N132 answers: a delete button on every row of a screen opened to read or add, one
        // tap from a fact that cannot be re-derived.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.delete("m1")).assertDoesNotExist()
    }

    @Test
    fun leavingTheMode_takesTheDeleteAwayAgain() {
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.delete("m1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.delete("m1")).assertDoesNotExist()
    }

    @Test
    fun theAddButton_isOutOfTheWayWhileDeleting() {
        // The default view is scroll and add, the edit mode is delete (N132): the two are not offered at
        // once, so a finger aiming at a row cannot land on the button that writes a new one.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).assertDoesNotExist()
    }

    @Test
    fun tappingARecord_opensItInFull_withEachNumberNamed() {
        // A request in use: the supporting line is a summary — "Daily · 82.4 kg · 18.3%" — and says
        // nothing about which number is which, so the row itself opens the record.
        setScreen()
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS_TITLE).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.measurements_weight)).assertExists()
        composeTestRule.onNodeWithText(Weight.kilograms(82_400L)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.measurements_body_fat)).assertExists()
    }

    @Test
    fun theDetails_drawOnlyWhatTheRecordStates() {
        // A daily entry that states soreness and nothing else is a real entry (N113), so the details show
        // the sore list and no weight: an absent number is not a zero, so it is not a row either.
        val soreOnly = BodyMeasurement(
            id = "m1",
            measuredAt = Instant.parse("2026-10-02T08:00:00Z"),
            kind = MeasurementKind.DAILY,
            soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
        )
        setScreen(state = MeasurementsUiState(isLoading = false, entries = listOf(soreOnly)))
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.readiness_sore_title)).assertExists()
        composeTestRule.onNodeWithText(MuscleGroup.QUADS.label).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.scored_pick_score, 8)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.measurements_weight)).assertDoesNotExist()
    }

    @Test
    fun theDetails_areReadOnly_andCloseIsHowTheyGo() {
        // Correcting a reading is the edit mode's job — its row opens the form there — so the details
        // dialog has no field, and its one control takes it away.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS_CLOSE).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS).assertDoesNotExist()
    }

    @Test
    fun inTheEditMode_tappingARecord_opensItsForm_holdingWhatItStates() {
        // The mode is where the writes live, so the row opens the form rather than the read-only details —
        // and it opens **on the record**, so a wrong number can be changed and a missing one filled in.
        setScreen(state = MeasurementsUiState(isLoading = false, entries = listOf(today)))

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertTextContains("82.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.BODY_FAT).assertTextContains("18.3")
        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS).assertDoesNotExist()
    }

    @Test
    fun inTheEditMode_aNumberTheRecordNeverStated_canBeAdded() {
        // The request this answers: a record that left a number out — muscle here — can be completed later
        // rather than being fixed the moment it was written.
        var saved: BodyMeasurement? = null
        setScreen(
            state = MeasurementsUiState(isLoading = false, entries = listOf(today)),
            onSave = { saved = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.MUSCLE).performTextInput("41.5")
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        assertEquals("the record it came from is the one corrected", "m1", saved?.id)
        assertEquals("its own day is kept", today.measuredAt, saved?.measuredAt)
        assertEquals("what it already stated is kept", 82_400L, saved?.weightGrams)
        assertEquals("and what was blank is now stated", 415, saved?.muscleTenths)
    }

    @Test
    fun inTheEditMode_aNumberThatIsWrong_canBeChanged() {
        var saved: BodyMeasurement? = null
        setScreen(
            state = MeasurementsUiState(isLoading = false, entries = listOf(today)),
            onSave = { saved = it },
        )

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextInput("81.2")
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        assertEquals("m1", saved?.id)
        assertEquals(81_200L, saved?.weightGrams)
        assertEquals("and the number beside it was not disturbed", 183, saved?.bodyFatTenths)
    }

    @Test
    fun aDeletedEntry_isOfferedBack_andUndoTakesIt() {
        // N133: a snackbar naming what went, with *Undo* — the shape the workout and template screens use.
        var undone = false
        setScreen(pendingUndo = today, onUndo = { undone = true })

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.measurements_deleted),
        ).assertExists()
        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.measurements_undo),
        ).performClick()

        assertTrue("the snackbar's action is the undo", undone)
    }

    @Test
    fun anOffTargetWeighIn_namesTheMove_ratherThanOpeningADialog() {
        // N136: the answer arrives with the weighing, as a line that states and is dismissible — never a
        // dialog, which would pursue (P4.7), and never a number to eat (N98).
        setScreen(rateVerdict = EnergyAdjustment(lowKcal = -300, highKcal = -200))

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.statistics_energy_less, "200–300"),
        ).assertExists()
    }

    @Test
    fun anOnTargetWeighIn_saysSo() {
        setScreen(rateVerdict = EnergyAdjustment(lowKcal = -40, highKcal = 30))

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.statistics_energy_on_target),
        ).assertExists()
    }

    @Test
    fun theTwoRecords_areOfferedSeparately() {
        // N113: one form for everything is what made a tape-only day invent a weight to save.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD_DAILY).assertIsEnabled()
        composeTestRule.onNodeWithTag(TestTags.Measurements.ADD_TAPE).assertIsEnabled()
    }

    @Test
    fun theSaveIsGatedOnTheEntryStatingSomething() {
        setScreen()
        startAdding(MeasurementKind.DAILY)

        // Nothing stated yet: an entry that says nothing is the one thing the form refuses.
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextInput("82.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsEnabled()
    }

    @Test
    fun aSorenessOnlyMorning_canBeSaved() {
        // The rule the split exists for (N113): the weight stopped being the one thing an entry cannot
        // be without, because a rest day reporting sore muscles is exactly what the record is for.
        var saved: BodyMeasurement? = null
        setScreen(onSave = { saved = it })
        startAdding(MeasurementKind.DAILY)

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.add).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.SORE.option(MuscleGroup.QUADS.name))
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsEnabled().performClick()

        assertNull("no weight was stated, and none is invented", saved?.weightGrams)
        assertEquals(listOf(MuscleGroup.QUADS), saved?.soreMuscles?.map { it.muscle })
        assertEquals(MeasurementKind.DAILY, saved?.kind)
    }

    @Test
    fun theOptionalNumbers_stayOptional() {
        var saved: BodyMeasurement? = null
        setScreen(onSave = { saved = it })
        startAdding(MeasurementKind.DAILY)
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextInput("82.4")

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        val entry = saved
        assertTrue("the entry reached the screen's callback", entry != null)
        assertEquals(82_400L, entry!!.weightGrams)
        // Nothing else was asked for, so nothing else is claimed: null, not zero.
        assertNull("body fat was not measured", entry.bodyFatTenths)
        assertNull("muscle was not measured", entry.muscleTenths)
        assertTrue("no tape site was measured", entry.tape.isEmpty())
    }

    @Test
    fun aTapeEntry_statesItsSites_andAsksForNoWeight() {
        var saved: BodyMeasurement? = null
        setScreen(onSave = { saved = it })
        startAdding(MeasurementKind.TAPE)

        // The daily numbers are not on this form at all: a tape day has no weight to state (N113).
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(TestTags.Measurements.tape(TapeSite.WAIST)).performTextInput("86.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        assertEquals(MeasurementKind.TAPE, saved?.kind)
        assertEquals(864L, saved?.tape?.get(TapeSite.WAIST))
        assertNull("and no weight is claimed by the tape record", saved?.weightGrams)
    }

    @Test
    fun aFieldThatIsNotARealNumber_isNotStored() {
        // `toDoubleOrNull` accepts "NaN" and "Infinity", and `Math.round(NaN * 10)` is 0 — so a typed "NaN"
        // became a real 0.0% body-fat reading, a measurement nobody took, in a series that then draws it.
        var saved: BodyMeasurement? = null
        setScreen(onSave = { saved = it })
        startAdding(MeasurementKind.DAILY)
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextInput("82.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.BODY_FAT).performTextInput("NaN")

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()
        assertNull("a NaN body fat is not a measurement", saved?.bodyFatTenths)
    }

    @Test
    fun aTapeEntryWhoseOnlySiteIsNotANumber_statesNothing() {
        setScreen(state = MeasurementsUiState(isLoading = false))
        startAdding(MeasurementKind.TAPE)

        composeTestRule.onNodeWithTag(TestTags.Measurements.tape(TapeSite.WAIST))
            .performTextInput("Infinity")

        // Save stays refused: an entry whose only number is not a number states nothing at all.
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun aNegativeMeasurement_isNotStored() {
        // Nothing this screen collects is below zero, and a negative would draw as a real reading.
        var saved: BodyMeasurement? = null
        setScreen(onSave = { saved = it })
        startAdding(MeasurementKind.DAILY)
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).performTextInput("82.4")
        composeTestRule.onNodeWithTag(TestTags.Measurements.BODY_FAT).performTextInput("-5")

        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        assertNull("no body fat below zero", saved?.bodyFatTenths)
    }

    @Test
    fun aRowSaysWhichOfTheTwoRecords_itIs() {
        // N113: the kind leads the supporting line, so the list tells a tape day from a daily one.
        val tape = BodyMeasurement(
            id = "m2",
            measuredAt = Instant.parse("2026-10-03T08:00:00Z"),
            kind = MeasurementKind.TAPE,
            tape = mapOf(TapeSite.WAIST to 860L),
        )
        setScreen(state = MeasurementsUiState(isLoading = false, entries = listOf(today, tape)))
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.onNodeWithText(context.getString(R.string.measurements_kind_daily), substring = true)
            .assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.measurements_kind_tape), substring = true)
            .assertExists()
    }

    @Test
    fun theEditForm_savesTheDayItWasCorrectedTo() {
        // B214: opening the calendar was proved; that the day picked in it reaches the saved draft was not.
        // The record is in 2020 so the day is never "in the future" for the picker's bound (B212).
        val record = BodyMeasurement(
            id = "m-date",
            measuredAt = Instant.parse("2020-02-04T08:00:00Z"),
            kind = MeasurementKind.DAILY,
            weightGrams = 80_000L,
        )
        var saved: BodyMeasurement? = null
        setScreen(initialEdit = record, onSave = { saved = it })

        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE_CHANGE).performClick()
        // The calendar opens on the record's own month; a day cell is named by its whole date.
        composeTestRule.onNodeWithText("February 20, 2020", substring = true).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.EDIT_DATE_CONFIRM).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.CONFIRM).performClick()

        val zone = ZoneId.systemDefault()
        assertEquals(LocalDate.of(2020, 2, 20), saved!!.measuredAt.atZone(zone).toLocalDate())
        assertEquals(
            "the reading keeps the hour it was taken at",
            record.measuredAt.atZone(zone).toLocalTime(),
            saved!!.measuredAt.atZone(zone).toLocalTime(),
        )
    }
}
