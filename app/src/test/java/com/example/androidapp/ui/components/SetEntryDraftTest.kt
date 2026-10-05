package com.example.androidapp.ui.components

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a set draft states (ROADMAP N67).
 *
 * The RPE rule is a property of the role, and this is the half of it the UI owns: a draft whose role
 * is a warm-up states no effort even while its text still holds the number it opened on — which is
 * exactly what happens when a lifter changes the role picker from *Working* to *Warm-up* after the
 * stepper already showed 9. The write boundary holds the same rule, so this is the first of two.
 */
class SetEntryDraftTest {

    @Test
    fun aWarmUpDraft_statesNoEffort_whateverItsTextHolds() {
        val draft = SetEntryDraft(
            repsText = "5",
            weightText = "60",
            rpeText = "9",
            setType = SetType.WARMUP,
        )

        assertThat(draft.values(WeightUnit.KILOGRAMS).rpeHalves).isEqualTo(18)
        assertThat(draft.toEdit(WeightUnit.KILOGRAMS).rpeHalves).isNull()
    }

    @Test
    fun aWorkingDraft_statesTheEffortItShows() {
        val draft = SetEntryDraft(
            repsText = "5",
            weightText = "100",
            rpeText = "8.5",
            setType = SetType.NORMAL,
        )

        assertThat(draft.toEdit(WeightUnit.KILOGRAMS).rpeHalves).isEqualTo(17)
    }
}
