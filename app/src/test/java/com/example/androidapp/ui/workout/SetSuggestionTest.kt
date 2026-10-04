package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SlotPrescription
import com.example.androidapp.domain.model.SlotSet
import com.example.androidapp.domain.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the next set is prefilled with (ROADMAP P1.3, N14).
 *
 * The file that holds the rule says its precedence is covered by fast tests rather
 * than by tapping — this is that file's test. The plan's precedence gets its own cases
 * because it is the one source that can say something about the *second* set of an
 * exercise, which is where a ramp lives.
 */
class SetSuggestionTest {

    @Test
    fun thePlansRole_isCarriedIntoTheArmedSet() {
        // ROADMAP B48: a plan that opens with a ramp arms warm-up, so the logged set is a warm-up
        // rather than a working set that inflates volume and can claim a record.
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
            planned = PlannedTarget(reps = 5, weightGrams = 40_000L, role = SetType.WARMUP),
        )

        assertEquals(SetType.WARMUP, suggestion.setType)
    }

    @Test
    fun withNoPlan_theArmedSetIsAWorkingSet() {
        // The old default stands where there is nothing to follow: N19's role is a choice, and the
        // app does not invent one.
        val suggestion = suggestionForNextSet(loggedSets = emptyList(), previous = null)

        assertEquals(SetType.NORMAL, suggestion.setType)
    }

    @Test
    fun theSlotsRole_winsOverTheTemplates() {
        // The slot overrides only what it says (P3.8), and its role is part of that.
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, role = SetType.TOP_SET)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L, role = SetType.WARMUP),
        )

        assertEquals(SetType.TOP_SET, target?.role)
    }

    @Test
    fun withNothingToGoOn_itFallsBackToTheDefault() {
        val suggestion = suggestionForNextSet(loggedSets = emptyList(), previous = null, )

        assertEquals(DEFAULT_REPS, suggestion.reps)
        assertEquals(Weight.DEFAULT_GRAMS, suggestion.weightGrams)
    }

    @Test
    fun withinASession_itRepeatsTheSetJustLogged() {
        val suggestion = suggestionForNextSet(
            loggedSets = listOf(SetRow(id = "s1", number = 1, reps = 5, weightGrams = 100_000)),
            previous = null,
        )

        assertEquals(5, suggestion.reps)
        assertEquals(100_000L, suggestion.weightGrams)
    }

    @Test
    fun thePlansTargetWins_overRepeatingTheLastSet() {
        // A ramp is the case that needs this: set two is not set one.
        val suggestion = suggestionForNextSet(
            loggedSets = listOf(SetRow(id = "s1", number = 1, reps = 3, weightGrams = 100_000)),
            previous = null,
            planned = PlannedTarget(reps = 3, weightGrams = 105_000),
        )

        assertEquals(3, suggestion.reps)
        assertEquals(105_000L, suggestion.weightGrams)
    }

    @Test
    fun aPlanThatNamesOnlyReps_keepsTheWeightFromTheFallback() {
        // "3 sets of 5" says nothing about the bar; a zero would be a claim.
        val suggestion = suggestionForNextSet(
            loggedSets = listOf(SetRow(id = "s1", number = 1, reps = 5, weightGrams = 80_000)),
            previous = null,
            planned = PlannedTarget(reps = 5, weightGrams = null),
        )

        assertEquals(5, suggestion.reps)
        assertEquals(80_000L, suggestion.weightGrams)
    }

    @Test
    fun aPlanThatNamesOnlyAWeight_keepsTheRepsFromTheFallback() {
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
                        planned = PlannedTarget(reps = null, weightGrams = 120_000),
        )

        assertEquals(DEFAULT_REPS, suggestion.reps)
        assertEquals(120_000L, suggestion.weightGrams)
    }

    @Test
    fun withNoPlan_theOlderPrecedenceStillHolds() {
        // The plan being absent must not change what P1.3 already decided.
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
                        planned = null,
        )

        assertEquals(DEFAULT_REPS, suggestion.reps)
        assertEquals(Weight.DEFAULT_GRAMS, suggestion.weightGrams)
    }

    @Test
    fun aPlanThatPrescribesAssistance_prefillsIt() {
        // ROADMAP N15: the assisted pull-up plan is a column of negative numbers, and
        // the workout has to show them back.
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
                        planned = PlannedTarget(reps = 8, weightGrams = null, assistanceGrams = 20_000L),
        )

        assertEquals(8, suggestion.reps)
        assertEquals(20_000L, suggestion.assistanceGrams)
        // And no weight: an assisted set that also carried the fallback's default
        // would report 20 kg of volume for a set the machine did the work on.
        assertEquals(0L, suggestion.weightGrams)
    }

    @Test
    fun repeatingASet_repeatsItsAssistance() {
        val suggestion = suggestionForNextSet(
            loggedSets = listOf(
                SetRow(
                    id = "s1",
                    number = 1,
                    reps = 8,
                    weightGrams = 0L,
                    assistanceGrams = 20_000L,
                ),
            ),
            previous = null,
        )

        assertEquals(20_000L, suggestion.assistanceGrams)
    }

    @Test
    fun aPlanThatNamesRepsAndNoLoad_takesTheLoadFromHistory() {
        // A plan that says "2 reps" and no bar still has to show a load, and history is the honest
        // answer: the bar is what it was, and stepping it is the lifter's click, not the app's
        // arithmetic (N59 withdrew the proposal N33 used to carry beside it).
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = PreviousPerformance(
                listOf(
                    SetEntry(
                        id = "old",
                        sessionExerciseId = "se-old",
                        setIndex = 0,
                        reps = 2,
                        weightGrams = 100_000,
                    ),
                ),
            ),
            planned = PlannedTarget(reps = 2, weightGrams = null),
        )

        assertEquals("last time's bar, unchanged", 100_000L, suggestion.weightGrams)
        assertEquals("at the reps the plan asked for", 2, suggestion.reps)
    }

    @Test
    fun aPlanThatNamesRepsAndNoLoad_keepsTheRepsThePlanAskedFor() {
        // The same branch, on the other side of the old ceiling: 90 kg for 2 against a plan of 5.
        // The reps are the plan's to decide and the load is history's, unchanged (N59).
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = PreviousPerformance(
                listOf(
                    SetEntry(
                        id = "old",
                        sessionExerciseId = "se-old",
                        setIndex = 0,
                        reps = 2,
                        weightGrams = 90_000,
                    ),
                ),
            ),
            planned = PlannedTarget(reps = 5, weightGrams = null),
        )

        assertEquals("the same bar as last time", 90_000L, suggestion.weightGrams)
        assertEquals("the reps are the plan's to decide", 5, suggestion.reps)
    }

    @Test
    fun thePlansTargetRpe_travelsBesideTheValues() {
        // ROADMAP N59: the plan's RPE is carried for the screen to caption, and it never becomes a
        // value — a suggestion holds no RPE field at all, which is what keeps a prescription out of
        // the record of how hard the set actually was.
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
            planned = PlannedTarget(reps = 5, weightGrams = 100_000L, rpeHalves = 16),
        )

        assertEquals(16, suggestion.targetRpeHalves)
    }

    @Test
    fun withNoPlannedRpe_thereIsNothingToCaption() {
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
            planned = PlannedTarget(reps = 5, weightGrams = 100_000L),
        )

        assertNull(suggestion.targetRpeHalves)
    }

    @Test
    fun aSlotsPercentage_resolvesThroughTheEstimatedOneRepMax() {
        // ROADMAP P3.8: the one load a template's planned set cannot express. 100 kg estimated at
        // 85% is 85 kg, rounded to the loadable step.
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetPercentOf1Rm = 85, targetRepsMin = 3)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = 100_000L,
        )

        assertEquals(3, target?.reps)
        assertEquals(85_000L, target?.weightGrams)
    }

    @Test
    fun aSlotsPercentage_withoutAnEstimate_hasNoNumber() {
        // Nothing estimable means no kilograms rather than a borrowed one; the caller falls back to
        // history, which is the honest answer (P3.8).
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetPercentOf1Rm = 85, targetRepsMin = 3)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
        )

        assertEquals(3, target?.reps)
        assertNull(target?.weightGrams)
    }

    @Test
    fun aSlotsWeight_winsOverItsPercentage() {
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(
                    SlotSet(
                        id = "x",
                        setIndex = 0,
                        targetWeightGrams = 90_000L,
                        targetPercentOf1Rm = 85,
                    ),
                ),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = 100_000L,
        )

        assertEquals(90_000L, target?.weightGrams)
    }

    @Test
    fun aSetTheSlotDoesNotMention_isLeftToTheTemplate() {
        // The slot overrides only what it says: a prescription with no set at this index hands the
        // template's own target straight back (N14, P3.8).
        val template = PlannedTarget(reps = 5, weightGrams = 100_000L)
        val target = prescribedTargetFor(
            prescription = SlotPrescription(exerciseId = "back-squat", sets = emptyList()),
            nextIndex = 0,
            estimatedOneRepMaxGrams = 100_000L,
            template = template,
        )

        assertEquals(template, target)
    }

    @Test
    fun aSlotSetThatNamesNothing_keepsTheTemplatesTarget() {
        // A set carrying only a note speaks about the note, not the load: the dialog promises that
        // anything left alone uses the workout's own targets (P3.8).
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, note = "slow descent")),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = 100_000L,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L),
        )

        assertEquals("the template's reps still stand", 5, target?.reps)
        assertEquals("and its bar", 100_000L, target?.weightGrams)
    }

    @Test
    fun aSlotSetThatNamesRepsOnly_keepsTheTemplatesLoad() {
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetRepsMin = 3, targetRepsMax = 3)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = 100_000L,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L),
        )

        assertEquals("the slot's reps win", 3, target?.reps)
        assertEquals("the slot said nothing about the bar", 100_000L, target?.weightGrams)
    }

    @Test
    fun aSlotsRpe_winsWhereItSpeaks_andTheTemplatesStandsWhereItDoesNot() {
        // ROADMAP N59: the target RPE follows the same "wins where it speaks" rule as reps (N14).
        val slotNamesOne = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetRpeHalves = 18)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L, rpeHalves = 16),
        )
        assertEquals(18, slotNamesOne?.rpeHalves)

        val slotNamesNone = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetRepsMin = 3)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L, rpeHalves = 16),
        )
        assertEquals("the template's RPE stands where the slot is silent", 16, slotNamesNone?.rpeHalves)
    }

    @Test
    fun aSlotSetThatNamesALoad_doesNotAlsoInheritTheTemplatesAssistance() {
        // The load is *one* number (N15): taking the slot's kilograms and the template's assistance
        // would build a set that is both, and count the kilograms as volume on an assisted set.
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "assisted-pull-up",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetWeightGrams = 100_000L)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
            template = PlannedTarget(reps = 5, weightGrams = null, assistanceGrams = 20_000L),
        )

        assertEquals(100_000L, target?.weightGrams)
        assertNull("the template's assistance must not ride along", target?.assistanceGrams)
    }

    @Test
    fun aSlotPercentageWithNoEstimate_fallsBackToTheTemplatesLoad() {
        val target = prescribedTargetFor(
            prescription = SlotPrescription(
                exerciseId = "back-squat",
                sets = listOf(SlotSet(id = "x", setIndex = 0, targetPercentOf1Rm = 85, targetRepsMin = 3)),
            ),
            nextIndex = 0,
            estimatedOneRepMaxGrams = null,
            template = PlannedTarget(reps = 5, weightGrams = 100_000L),
        )

        assertEquals(3, target?.reps)
        assertEquals("no estimate means the template's bar, not an invented one", 100_000L, target?.weightGrams)
    }

    @Test
    fun prescribedWeight_roundsToTheLoadableStep() {
        assertEquals(85_000L, prescribedWeightGrams(85, 100_000L))
        // 82% of 100 kg is 82 kg, which is not a loadable step: it lands on the nearest one.
        assertEquals(82_500L, prescribedWeightGrams(82, 100_000L))
        assertNull("nothing estimable has no number", prescribedWeightGrams(85, null))
        assertNull(prescribedWeightGrams(85, 0L))
    }
}
