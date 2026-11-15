package com.example.androidapp.ui.navigation

import com.example.androidapp.ui.workout.PlanHandOff
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The finish's answer becomes the plan editor's route (ROADMAP N310, N312).
 *
 * The graph itself needs Hilt and every screen, so this is the half that can be read: a deload hand-off
 * must open the deload confirm and a replace the sheet, and neither may carry the other's target. A route
 * with both set would open one question and leave the other waiting behind it.
 */
class PlanEditorRouteTest {

    @Test
    fun aDeloadHandOff_opensTheDeloadConfirm_only() {
        val route = planEditorRouteFor(
            PlanHandOff(
                programId = "p1",
                templateId = "t1",
                templateExerciseId = "te1",
                replace = false,
            ),
        )

        assertThat(route.programId).isEqualTo("p1")
        assertThat(route.deloadTemplateId).isEqualTo("t1")
        assertThat(route.deloadTemplateExerciseId).isEqualTo("te1")
        assertThat(route.replaceTemplateId).isNull()
        assertThat(route.replaceTemplateExerciseId).isNull()
    }

    @Test
    fun aReplaceHandOff_opensTheReplaceSheet_only() {
        val route = planEditorRouteFor(
            PlanHandOff(
                programId = "p1",
                templateId = "t1",
                templateExerciseId = "te1",
                replace = true,
            ),
        )

        assertThat(route.programId).isEqualTo("p1")
        assertThat(route.replaceTemplateId).isEqualTo("t1")
        assertThat(route.replaceTemplateExerciseId).isEqualTo("te1")
        assertThat(route.deloadTemplateId).isNull()
        assertThat(route.deloadTemplateExerciseId).isNull()
    }
}
