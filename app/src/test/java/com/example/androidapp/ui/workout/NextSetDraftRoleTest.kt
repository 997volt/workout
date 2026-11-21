package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.SetEntryDraft
import com.example.androidapp.ui.components.toEdit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What the role picker does to the next-set draft (ROADMAP N59, N67).
 *
 * The workout screen is the one place the effort field is expected to *open* on a number — the plan's
 * own where it names one, 9.0 otherwise. A planned warm-up is the exception (N67): the field is
 * withheld, so the draft opens blank and the picker is what brings it back. The rule is here rather
 * than inside the composable so it can be read and tested as one decision.
 */
class NextSetDraftRoleTest {

    private fun draft(setType: SetType = SetType.NORMAL, rpeText: String = "") = SetEntryDraft(
        repsText = "5",
        weightText = "100",
        rpeText = rpeText,
        setType = setType,
    )

    @Test
    fun choosingFailedAttempt_zerosTheReps_andLeavesTheEffortBehind() {
        // N329: the role is the whole fact, so a rep typed for the set it used to be does not travel.
        val attempted = draft(setType = SetType.NORMAL, rpeText = "9")
            .withRole(SetType.FAILED_ATTEMPT, planRpeHalves = 16)

        assertThat(attempted.setType).isEqualTo(SetType.FAILED_ATTEMPT)
        assertThat(attempted.repsText).isEqualTo("0")
        assertThat(attempted.toEdit(com.example.androidapp.domain.WeightUnit.KILOGRAMS).rpeHalves).isNull()
    }

    @Test
    fun aPlannedWarmUp_broughtBackToWork_statesThePlansEffort() {
        val warmUp = draft(setType = SetType.WARMUP, rpeText = "")

        val working = warmUp.withRole(SetType.NORMAL, planRpeHalves = 16)

        assertThat(working.setType).isEqualTo(SetType.NORMAL)
        assertThat(working.rpeText).isEqualTo("8")
    }

    @Test
    fun aPlanThatNamesNoEffort_opensOnTheWorkoutScreensDefault() {
        val working = draft(setType = SetType.WARMUP).withRole(SetType.NORMAL, planRpeHalves = null)

        // 9.0, so a set logged from this screen always carries one (N59).
        assertThat(working.rpeText).isEqualTo("9")
    }

    @Test
    fun anEffortTheLifterAlreadyStated_survivesARoundTripThroughWarmUp() {
        val stated = draft(setType = SetType.NORMAL, rpeText = "7.5")

        val roundTripped = stated.withRole(SetType.WARMUP, planRpeHalves = 16)
            .withRole(SetType.NORMAL, planRpeHalves = 16)

        assertThat(roundTripped.rpeText).isEqualTo("7.5")
    }

    @Test
    fun choosingWarmUp_keepsTheText_soTheFieldCanBeWithheldWithoutLosingIt() {
        // The field is absent while the role is a warm-up, not cleared: the text is what the lifter
        // comes back to if they change their mind.
        val working = draft(setType = SetType.NORMAL, rpeText = "8.5")

        assertThat(working.withRole(SetType.WARMUP, planRpeHalves = 16).rpeText).isEqualTo("8.5")
    }
}
