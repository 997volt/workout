package com.example.androidapp.ui.workout

import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun theReason_namesTheLastSet_andThePlan() {
        // P3.17: the numbers on screen say where they came from — the same set the prefill read, and the
        // plan's target for the set about to be done.
        val reason = suggestionReason(
            previous = PreviousPerformance(
                listOf(
                    SetEntry(
                        id = "s1",
                        sessionExerciseId = "se1",
                        setIndex = 0,
                        reps = 5,
                        weightGrams = 100_000L,
                        rpeHalves = 17,
                    ),
                ),
            ),
            planned = PlannedTarget(reps = 5, weightGrams = null, rpeHalves = 16),
        )

        assertEquals(5, reason?.lastReps)
        assertEquals(100_000L, reason?.lastWeightGrams)
        assertEquals(17, reason?.lastRpeHalves)
        assertEquals(5, reason?.planReps)
        assertEquals(16, reason?.planRpeHalves)
    }

    @Test
    fun theReason_isNull_whenNeitherSourceSpeaks() {
        assertNull(suggestionReason(previous = null, planned = null))
    }

    @Test
    fun theReason_namesARungsOwnPlace_notTheLastSetOverall() {
        // N79, N168: a rung's reps come from what this same place did last time, so its line must name
        // that set. The session's last set is a different row — for a drop, the wrong one entirely.
        val lastSession = PreviousPerformance(
            listOf(
                SetEntry("s1", "se1", 0, reps = 5, weightGrams = 100_000L),
                SetEntry("s2", "se1", 1, reps = 8, weightGrams = 90_000L),
                SetEntry("s3", "se1", 2, reps = 10, weightGrams = 60_000L, setType = SetType.DROP),
            ),
        )

        val reason = suggestionReason(
            previous = lastSession,
            planned = PlannedTarget(reps = null, weightGrams = null, role = SetType.DROP),
            sameSetLastTime = lastSession.sets[1],
        )

        assertEquals(8, reason?.lastReps)
        assertEquals(90_000L, reason?.lastWeightGrams)
    }

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
    fun withNothingToGoOn_itFallsBackToTheDefault() {
        val suggestion = suggestionForNextSet(loggedSets = emptyList(), previous = null,)

        assertEquals(DEFAULT_REPS, suggestion.reps)
        assertEquals(Weight.DEFAULT_GRAMS, suggestion.weightGrams)
    }

    @Test
    fun aBodyweightMovementWithNothingToGoOn_addsNothing() {
        // B242: the 20 kg a new exercise starts at is an *added* load on a bodyweight movement, so a first
        // pull-up would be logged at 95 kg for a 75 kg lifter without the number ever being typed.
        val suggestion = suggestionForNextSet(
            loggedSets = emptyList(),
            previous = null,
            bodyweight = true,
        )

        assertEquals(DEFAULT_REPS, suggestion.reps)
        assertEquals("nothing was added, so nothing is prefilled", 0L, suggestion.weightGrams)
    }

    @Test
    fun aTimedMovementWithNothingToGoOn_opensUnloaded() {
        // The same for a hold: a plank is not a loaded lift, and 20 kg on the bar would feed the volume and
        // the heaviest-set series with a number nobody entered (B242).
        val suggestion = suggestionForNextSet(loggedSets = emptyList(), previous = null, timed = true)

        assertEquals(0L, suggestion.weightGrams)
    }

    @Test
    fun aBodyweightMovementWithHistory_keepsWhatWasAdded() {
        // Only the fallback changes: last time's belt is the best evidence there is (N219).
        val suggestion = suggestionForNextSet(
            loggedSets = listOf(SetRow(id = "s1", number = 1, reps = 5, weightGrams = 20_000)),
            previous = null,
            bodyweight = true,
        )

        assertEquals(20_000L, suggestion.weightGrams)
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
    fun aTemplatesOneTargetRpe_isTheSetsTarget() {
        // ROADMAP N59, amended: the exercise's number is the plan's target, and a set carries none.
        val target = plannedTargetFor(planned = plannedExercise(exerciseRpe = 18), nextIndex = 0)

        assertEquals(18, target?.rpeHalves)
    }

    /** One planned exercise with one planned set, for the plan's target RPE (N59). */
    private fun plannedExercise(exerciseRpe: Int?) = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "back-squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        targetRpeHalves = exerciseRpe,
        sets = listOf(
            TemplateSet(
                id = "ts1",
                templateExerciseId = "te1",
                setIndex = 0,
                targetWeightGrams = 100_000L,
                targetRepsMax = 5,
            ),
        ),
    )

    @Test
    fun aDropRung_isTheAnchorLessTheValue_andItsRepsComeFromTheSameSetLastTime() {
        // ROADMAP N79: a rung carries no target of its own. Its load is the anchor's — the bar that was
        // actually loaded, not the plan's number for it — less the run's value, and its reps come from
        // what this same set did in the previous training rather than from the set just done.
        val sets = droppingPlan()
        val anchor = logged(weightGrams = 105_000L, reps = 5)

        val suggestion = suggestionForNextSet(
            loggedSets = listOf(anchor),
            previous = PreviousPerformance(sets = listOf(previousSet(setIndex = 1, reps = 10))),
            planned = plannedTargetFor(sets, nextIndex = 1, anchorLoad = Load(105_000L, 0L)),
            sameSetLastTime = previousSet(setIndex = 1, reps = 10),
        )

        assertEquals(
            "105 loaded, 20 off, and not the plan's 100",
            85_000L,
            suggestion.weightGrams,
        )
        assertEquals("last time's ten, not the anchor's five", 10, suggestion.reps)
        assertEquals(SetType.DROP, suggestion.setType)
    }

    @Test
    fun aDropRungNeverPerformed_fallsBackToWhatWasJustDone() {
        val sets = droppingPlan()

        val suggestion = suggestionForNextSet(
            loggedSets = listOf(logged(weightGrams = 100_000L, reps = 5)),
            previous = null,
            planned = plannedTargetFor(sets, nextIndex = 1, anchorLoad = Load(100_000L, 0L)),
            sameSetLastTime = null,
        )

        assertEquals(80_000L, suggestion.weightGrams)
        assertEquals("nothing to copy, so the set just done answers", 5, suggestion.reps)
    }

    @Test
    fun aClusterRung_repeatsTheAnchorsLoad_ratherThanTakingAnythingOff() {
        val sets = TemplateExercise(
            id = "te1",
            templateId = "t1",
            exerciseId = "back-squat",
            position = 0,
            exerciseName = "Back Squat",
            primaryMuscle = MuscleGroup.QUADS,
            equipment = Equipment.BARBELL,
            sets = listOf(
                TemplateSet(id = "ts1", templateExerciseId = "te1", setIndex = 0, targetWeightGrams = 100_000L),
                TemplateSet(id = "ts2", templateExerciseId = "te1", setIndex = 1, role = SetType.CLUSTER),
            ),
        )

        val planned = plannedTargetFor(sets, nextIndex = 1, anchorLoad = Load(100_000L, 0L))

        assertEquals(100_000L, planned?.weightGrams)
        assertNull("and no reps of its own: the group's is the anchor's", planned?.reps)
        assertEquals(SetType.CLUSTER, planned?.role)
    }

    @Test
    fun aRungIsOfferedOnlyWhereTheSetAboveItStandsOnItsOwn() {
        // ROADMAP B63, B64: the picker's question, answered by the rule the write boundary holds. The
        // first set of an exercise has nothing above it and a warm-up is not an anchor, so neither can
        // be armed with a rung; a working set can, and another rung of the same run joins it.
        val nothing = emptyList<SetRow>()
        val ramp = listOf(logged(40_000L, 10).copy(setType = SetType.WARMUP))
        val work = listOf(logged(100_000L, 5))
        val dropRun = work + logged(80_000L, 8).copy(setType = SetType.DROP)

        assertFalse("the first set has nothing above it", nothing.canLogAs(SetType.DROP))
        assertFalse("and a warm-up does not anchor a drop", ramp.canLogAs(SetType.DROP))
        assertFalse("nor a cluster", ramp.canLogAs(SetType.CLUSTER))
        assertTrue("a working set does", work.canLogAs(SetType.DROP))
        assertTrue("and another drop joins its own run", dropRun.canLogAs(SetType.DROP))
        assertFalse("while a different kind of run has no anchor there", dropRun.canLogAs(SetType.CLUSTER))
        assertTrue("a set that is not a rung is always available", nothing.canLogAs(SetType.NORMAL))
    }

    /** A working set at 100 with one 20 kg drop after it, as the plan stores it (N79). */
    private fun droppingPlan() = TemplateExercise(
        id = "te1",
        templateId = "t1",
        exerciseId = "back-squat",
        position = 0,
        exerciseName = "Back Squat",
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
        sets = listOf(
            TemplateSet(
                id = "ts1",
                templateExerciseId = "te1",
                setIndex = 0,
                targetWeightGrams = 100_000L,
                targetRepsMax = 5,
            ),
            TemplateSet(
                id = "ts2",
                templateExerciseId = "te1",
                setIndex = 1,
                role = SetType.DROP,
                dropValueGrams = 20_000L,
            ),
        ),
    )

    private fun logged(weightGrams: Long, reps: Int) = SetRow(
        id = "set1",
        number = 1,
        reps = reps,
        weightGrams = weightGrams,
    )

    private fun previousSet(setIndex: Int, reps: Int) = SetEntry(
        id = "prev$setIndex",
        sessionExerciseId = "se1",
        setIndex = setIndex,
        reps = reps,
        weightGrams = 60_000L,
        setType = SetType.DROP,
    )
}
