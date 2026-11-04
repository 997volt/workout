package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * A body weight is a positive number of grams (B336, B338).
 *
 * The rule's own home, tested where it lives rather than through the screen that gates *Save* or the series that
 * ignores a stored zero — both of which assert it as an effect of something else.
 */
class BodyMeasurementWeightTest {

    @Test
    fun aZeroOrNegativeWeight_statesNone() {
        assertThat(statedBodyWeightGrams(0L)).isNull()
        assertThat(statedBodyWeightGrams(-1L)).isNull()
        assertThat(statedBodyWeightGrams(null)).isNull()
        assertThat(statedBodyWeightGrams(82_400L)).isEqualTo(82_400L)
    }

    @Test
    fun aDailyEntryWhoseOnlyWeightIsZero_statesNothing() {
        // The form's *Save* rule: a typed zero is not a weight, so on its own it does not make an entry worth
        // writing — while the same zero beside a number that was really taken does not hide it.
        assertThat(daily(weight = 0L).statesSomething()).isFalse()
        assertThat(daily(weight = 0L, bodyFat = 183).statesSomething()).isTrue()
        assertThat(daily(weight = 82_400L).statesSomething()).isTrue()
    }

    private fun daily(weight: Long?, bodyFat: Int? = null) = BodyMeasurement(
        id = "",
        measuredAt = Instant.parse("2026-10-01T08:00:00Z"),
        kind = MeasurementKind.DAILY,
        weightGrams = weight,
        bodyFatTenths = bodyFat,
    )
}
