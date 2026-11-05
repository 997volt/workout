package com.example.androidapp.ui.components

import androidx.compose.runtime.saveable.SaverScope
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The planned-set editor's draft across a rotation (B376). */
class TemplateSetDraftSaverTest {

    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    @Test
    fun everyField_ofThePlannedDraft_roundTrips() {
        val draft = TemplateSetDraft(
            role = SetType.DROP,
            weightText = "100",
            dropValueText = "10",
            repsMinText = "6",
            repsMaxText = "8",
            holdText = "1:30",
            targetRepsCurrent = 7,
            note = "belt",
            unit = WeightUnit.POUNDS,
            holdsTheRunValue = false,
        )

        val restored = with(TemplateSetDraftSaver) { scope.save(draft) }
            ?.let(TemplateSetDraftSaver::restore)

        assertThat(restored).isEqualTo(draft)
    }
}
