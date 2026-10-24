package com.example.androidapp.ui.measurements

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

/**
 * The day an edit form holds, and what a picked day does to it (B212, B214).
 *
 * The one part of the pick → save path that does not need a Material calendar: the dialog builds its draft
 * from [MeasurementDayState.measuredAt], so a day that becomes this value becomes the saved day.
 */
class MeasurementDayStateTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun pickingADay_movesTheRecordToIt_andKeepsItsHour() {
        // 08:30 in Berlin; the picker returns a calendar day, so the hour has to survive the move.
        val state = MeasurementDayState(Instant.parse("2026-10-12T06:30:00Z"), berlin)

        state.pick(LocalDate.of(2026, 10, 20))

        assertThat(state.day).isEqualTo(LocalDate.of(2026, 10, 20))
        assertThat(state.measuredAt.atZone(berlin).toLocalTime()).isEqualTo(LocalTime.of(8, 30))
    }

    @Test
    fun aNewEntry_startsOnTheDayItWasOpened() {
        val state = MeasurementDayState(initial = null, zone = berlin)

        assertThat(state.day).isEqualTo(Instant.now().atZone(berlin).toLocalDate())
    }
}
