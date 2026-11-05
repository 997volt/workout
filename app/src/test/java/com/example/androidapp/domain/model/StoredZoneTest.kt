package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The one total conversion from a stored zone offset to a `ZoneId` (B282, B341).
 *
 * A backup can carry any integer, and `ZoneOffset.ofTotalSeconds` throws outside ±18:00. These
 * cases pin the bounds, the fallback, and the overflow that would otherwise wrap into a legal value.
 */
class StoredZoneTest {

    private val fallback = ZoneId.of("Europe/Berlin")

    @Test
    fun aLegalOffset_readsAsThatZone() {
        assertThat(storedZoneOrNull(120)).isEqualTo(ZoneOffset.ofHours(2))
        assertThat(storedZoneOrNull(-300)).isEqualTo(ZoneOffset.ofHours(-5))
        assertThat(storedZoneOrFallback(120, fallback)).isEqualTo(ZoneOffset.ofHours(2))
    }

    @Test
    fun noOffset_isNull_andTheCallerGetsItsFallback() {
        assertThat(storedZoneOrNull(null)).isNull()
        assertThat(storedZoneOrFallback(null, fallback)).isEqualTo(fallback)
    }

    @Test
    fun theBounds_areInclusive_andOneMinutePastThemIsRefused() {
        assertThat(storedZoneOrNull(18 * 60)).isEqualTo(ZoneOffset.ofHours(18))
        assertThat(storedZoneOrNull(-18 * 60)).isEqualTo(ZoneOffset.ofHours(-18))
        assertThat(storedZoneOrNull(18 * 60 + 1)).isNull()
        assertThat(storedZoneOrNull(-18 * 60 - 1)).isNull()
        assertThat(storedZoneOrFallback(18 * 60 + 1, fallback)).isEqualTo(fallback)
    }

    @Test
    fun anExtremeInteger_isRefusedRatherThanWrappingIntoALegalOffset() {
        // `Int.MAX_VALUE * 60` overflows an Int; the Long conversion and range check keep this total.
        assertThat(storedZoneOrNull(Int.MAX_VALUE)).isNull()
        assertThat(storedZoneOrNull(Int.MIN_VALUE)).isNull()
    }
}
