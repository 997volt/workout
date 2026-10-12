package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test

/**
 * Which local day an entry belongs to, and whether a day's daily record already exists (ROADMAP N114).
 *
 * The rule the home row is drawn on: the row is offered while today has **no** daily entry, so this answers
 * "has today been written down" for a list of entries. It is a plain JVM test because the rule is a pure
 * function — the day, the zone and the entries in, a boolean out — and it is the seam where a mistake would
 * be invisible: a row that hides itself while the measurements screen shows nothing for today reads as the
 * data being wrong rather than as a bug here.
 */
class BodyMeasurementDayTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val day: LocalDate = LocalDate.of(2026, 10, 12)

    private fun entry(kind: MeasurementKind, at: Instant) =
        BodyMeasurement(id = "m", measuredAt = at, kind = kind)

    private fun noon() = Instant.parse("2026-10-12T12:00:00Z")

    @Test
    fun aDailyEntry_onTheDay_isTheDays() {
        assertThat(entry(MeasurementKind.DAILY, noon()).isDailyOn(day, berlin)).isTrue()
    }

    @Test
    fun aTapeEntry_onTheSameDay_isNot() {
        // N113's split is why: a day keeps one entry of each kind, and a tape day says nothing about soreness,
        // weight, body fat or muscle — so it is no reason to stop offering to write those down.
        assertThat(entry(MeasurementKind.TAPE, noon()).isDailyOn(day, berlin)).isFalse()
    }

    @Test
    fun yesterdaysEntry_isNotTodays() {
        assertThat(entry(MeasurementKind.DAILY, Instant.parse("2026-10-11T12:00:00Z")).isDailyOn(day, berlin))
            .isFalse()
    }

    @Test
    fun theDayIsTheLocalOne_notUtc() {
        // 22:30 UTC on the 11th is already the 12th in Berlin, which is N25's rule for a session's time and
        // N32's for a measurement's date: an entry belongs to the day it was taken on where it was taken.
        val late = Instant.parse("2026-10-11T22:30:00Z")

        assertThat(late.localDay(berlin)).isEqualTo(day)
        assertThat(late.localDay(ZoneOffset.UTC)).isEqualTo(LocalDate.of(2026, 10, 11))
        assertThat(entry(MeasurementKind.DAILY, late).isDailyOn(day, berlin)).isTrue()
        assertThat(entry(MeasurementKind.DAILY, late).isDailyOn(LocalDate.of(2026, 10, 11), berlin)).isFalse()
    }

    @Test
    fun anEmptyList_hasNoDayRecorded() {
        // The reading the home row takes: nothing stored for today, so the offer stands.
        assertThat(
            emptyList<BodyMeasurement>().any { it.isDailyOn(day, berlin) },
        ).isFalse()
    }

    @Test
    fun anyDailyEntryOnTheDay_answersForTheDay() {
        // "Has today been written down" is answered by one entry stating anything, which is N113's rule —
        // a soreness-only morning counts, and the entries beside it are other days or the other kind.
        val entries = listOf(
            entry(MeasurementKind.DAILY, Instant.parse("2026-10-10T12:00:00Z")),
            entry(MeasurementKind.TAPE, noon()),
            BodyMeasurement(
                id = "m2",
                measuredAt = noon(),
                kind = MeasurementKind.DAILY,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            ),
        )

        assertThat(entries.any { it.isDailyOn(day, berlin) }).isTrue()
    }
}
