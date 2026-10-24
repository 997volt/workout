package com.example.androidapp.ui.measurements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.androidapp.domain.model.withDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The day an edited record belongs to, and the one operation the form applies to it (B212, B214).
 *
 * A state holder rather than a `var` in the dialog so the path the calendar feeds — a picked day becomes the
 * draft's day, keeping the reading's hour — can be asserted without driving a Material calendar under
 * Robolectric. The dialog builds its draft from [measuredAt], so this is the whole of the pick → save path that
 * is not Compose.
 */
internal class MeasurementDayState(initial: Instant?, private val zone: ZoneId) {
    /** The record's instant; the day is read from it in [zone], as it is everywhere else. */
    var measuredAt by mutableStateOf(initial ?: Instant.now())
        private set

    /** The day the record now belongs to, in [zone]. */
    val day: LocalDate get() = measuredAt.atZone(zone).toLocalDate()

    /** Moves the record to [picked], keeping the hour it was taken at. */
    fun pick(picked: LocalDate) {
        measuredAt = measuredAt.withDay(picked, zone)
    }
}
