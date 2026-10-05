package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.TemplateExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pairing a session's rows with the plan's entries (ROADMAP N54).
 *
 * By **movement and occurrence**, never by the slot a row occupies. N54 lets the session's order be
 * edited, so a row that has been moved still has to read its own plan's targets — matching on position
 * is the regression this file exists to catch, because it pointed a moved exercise at its neighbour's
 * plan.
 */
class PlanEntriesTest {

    @Test
    fun aMovedExercise_stillFollowsItsOwnEntry() {
        // bench-press was moved above back-squat, so the session's order no longer matches the plan's.
        val session = listOf(row("row-bench", "bench-press", 0), row("row-squat", "back-squat", 1))
        val plan = listOf(entry("te-squat", "back-squat", 0), entry("te-bench", "bench-press", 1))

        val entries = planEntriesFor(session, plan)

        assertThat(entries["row-bench"]?.id).isEqualTo("te-bench")
        assertThat(entries["row-squat"]?.id).isEqualTo("te-squat")
    }

    @Test
    fun aMovementThePlanNamesTwice_pairsByOccurrence() {
        // A plan may train the same movement twice with different targets: the first row of it follows
        // the plan's first entry and the second row the second.
        val session = listOf(row("row-0", "back-squat", 0), row("row-1", "back-squat", 1))
        val plan = listOf(entry("te-a", "back-squat", 0), entry("te-b", "back-squat", 1))

        val entries = planEntriesFor(session, plan)

        assertThat(entries["row-0"]?.id).isEqualTo("te-a")
        assertThat(entries["row-1"]?.id).isEqualTo("te-b")
    }

    @Test
    fun aRowThePlanHasNothingFor_hasNoEntry() {
        // Added by hand, or the extra row of a movement the plan names once: nothing to follow, which
        // leaves the prefill to the older rule and keeps the control from saying the planned work is
        // done (ROADMAP N52).
        val session = listOf(row("row-0", "back-squat", 0), row("row-1", "bench-press", 1))
        val plan = listOf(entry("te-a", "back-squat", 0))

        val entries = planEntriesFor(session, plan)

        assertThat(entries).containsKey("row-0")
        assertThat(entries).doesNotContainKey("row-1")
    }

    @Test
    fun noPlanAtAll_pairsNothing() {
        assertThat(planEntriesFor(listOf(row("row-0", "back-squat", 0)), emptyList())).isEmpty()
    }

    private fun row(id: String, exerciseId: String, position: Int) = SessionExercise(
        id = id,
        sessionId = "s1",
        exerciseId = exerciseId,
        position = position,
        exerciseName = exerciseId,
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
    )

    @Test
    fun plannedSetsLeft_countsTheRemainder_andIsNullWithoutAPlan() {
        // ROADMAP N70: the line the workout screen draws beside the next-set fields. Null and zero are
        // different statements — "nothing was planned" is not "the plan is finished" — and more logged
        // than planned owes nothing rather than a negative.
        assertThat(loggedRow(planned = 3, logged = 1).plannedSetsLeft).isEqualTo(2)
        assertThat(loggedRow(planned = 3, logged = 3).plannedSetsLeft).isEqualTo(0)
        assertThat(loggedRow(planned = 3, logged = 4).plannedSetsLeft).isEqualTo(0)
        assertThat(loggedRow(planned = null, logged = 2).plannedSetsLeft).isNull()
    }

    private fun loggedRow(planned: Int?, logged: Int) = SessionExerciseRow(
        id = "se1",
        exerciseId = "back-squat",
        name = "Back Squat",
        subtitle = null,
        plannedSetCount = planned,
        sets = List(logged) { index ->
            SetRow(id = "s$index", number = index + 1, reps = 5, weightGrams = 100_000)
        },
    )

    private fun entry(id: String, exerciseId: String, position: Int) = TemplateExercise(
        id = id,
        templateId = "t1",
        exerciseId = exerciseId,
        position = position,
        exerciseName = exerciseId,
        primaryMuscle = MuscleGroup.QUADS,
        equipment = Equipment.BARBELL,
    )
}
