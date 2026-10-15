package com.example.androidapp.ui.measurements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.R
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        pendingUndo: BodyMeasurement? = null,
        onUndo: () -> Unit = {},
        onDismissUndo: () -> Unit = {},
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
                    pendingUndo = pendingUndo,
                    onUndo = onUndo,
                    onDismissUndo = onDismissUndo,
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
        // Correcting a reading is a different question (N132), so there is no field here — and the one
        // control takes the dialog away.
        setScreen()

        composeTestRule.onNodeWithTag(TestTags.Measurements.row("m1")).performClick()
        composeTestRule.onNodeWithTag(TestTags.Measurements.WEIGHT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS_CLOSE).performClick()

        composeTestRule.onNodeWithTag(TestTags.Measurements.DETAILS).assertDoesNotExist()
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
}
