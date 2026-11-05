package com.example.androidapp.ui.components

import androidx.compose.runtime.saveable.SaverScope
import com.example.androidapp.domain.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The set editor's draft survives a rotation (B349).
 *
 * The dialog's open state is saveable, so the values it holds have to be; this pins the round trip of
 * every field rather than relying on a device to rotate.
 */
class SetEntryDraftSaverTest {

    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    @Test
    fun everyField_ofTheDraft_roundTrips() {
        val draft = SetEntryDraft(
            repsText = "5",
            durationText = "45",
            timed = true,
            weightText = "100",
            rpeText = "8.5",
            noteText = "belt",
            setType = SetType.DROP,
        )

        val restored = with(SetEntryDraftSaver) { scope.save(draft) }?.let(SetEntryDraftSaver::restore)

        assertThat(restored).isEqualTo(draft)
    }

    @Test
    fun anEmptyDraft_staysEmpty_andKeepsItsRole() {
        val draft = SetEntryDraft(repsText = "", weightText = "", rpeText = "", setType = SetType.WARMUP)

        val restored = with(SetEntryDraftSaver) { scope.save(draft) }?.let(SetEntryDraftSaver::restore)

        assertThat(restored).isEqualTo(draft)
    }
}
