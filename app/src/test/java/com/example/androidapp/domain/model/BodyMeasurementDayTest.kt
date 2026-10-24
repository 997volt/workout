package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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

    @Test
    fun aDaysBounds_areTheNextMidnight_notAFixedSpan() {
        // ROADMAP B133. A local day is 23 hours on a spring-forward date and 25 on a fall-back one, so
        // the end of a day is the *next* day's midnight rather than midnight plus 86 400 seconds: the
        // fixed span reached an hour into the next day in March and stopped an hour short in November.
        val newYork = ZoneId.of("America/New_York")
        val (springStart, springEnd) = Instant.parse("2026-03-08T17:00:00Z").localDayBounds(newYork)
        val (fallStart, fallEnd) = Instant.parse("2026-11-01T16:00:00Z").localDayBounds(newYork)

        assertThat(springStart).isEqualTo(Instant.parse("2026-03-08T05:00:00Z").toEpochMilli())
        assertThat(springEnd).isEqualTo(Instant.parse("2026-03-09T04:00:00Z").toEpochMilli())
        assertThat(fallStart).isEqualTo(Instant.parse("2026-11-01T04:00:00Z").toEpochMilli())
        assertThat(fallEnd).isEqualTo(Instant.parse("2026-11-02T05:00:00Z").toEpochMilli())
    }

    @Test
    fun theNextMorningsEarlyHour_isOutsideTheDay_andTheLateEveningIsInside() {
        // The two saves B133 named, at the boundaries the fixed span got wrong: 00:30 the morning after a
        // spring-forward day belongs to the *next* day, and 23:30 on a fall-back day is still its own. A
        // span of 86 400 seconds put the first inside the previous day and the second outside its own.
        val newYork = ZoneId.of("America/New_York")
        val (springStart, springEnd) = Instant.parse("2026-03-08T17:00:00Z").localDayBounds(newYork)
        val (fallStart, fallEnd) = Instant.parse("2026-11-01T16:00:00Z").localDayBounds(newYork)

        val nextMorning = Instant.parse("2026-03-09T04:30:00Z").toEpochMilli() // 00:30 EDT
        val lateEvening = Instant.parse("2026-11-02T04:30:00Z").toEpochMilli() // 23:30 EST

        assertThat(nextMorning >= springStart && nextMorning < springEnd).isFalse()
        assertThat(lateEvening >= fallStart && lateEvening < fallEnd).isTrue()
    }

    @Test
    fun aCorrectedDayKeepsTheHourTheReadingWasTakenAt() {
        // A request in use; no roadmap id covers it. The edit form corrects which day a record belongs to,
        // and that is not a claim that it was taken at a different hour — 23:30 in Berlin is already the next
        // day in UTC, which is why the move is built in the entry's zone rather than in UTC.
        val lateEvening = Instant.parse("2026-10-12T21:30:00Z")
        assertThat(lateEvening.localDay(berlin)).isEqualTo(day)

        val moved = lateEvening.withDay(LocalDate.of(2026, 10, 20), berlin)

        assertThat(moved.localDay(berlin)).isEqualTo(LocalDate.of(2026, 10, 20))
        assertThat(moved.atZone(berlin).toLocalTime())
            .isEqualTo(lateEvening.atZone(berlin).toLocalTime())
    }

    @Test
    fun movingOntoTheSpringForwardDay_landsOnThatDay() {
        // 02:30 does not exist on 2027-03-28 in Berlin. The zone's resolver moves it forward rather than
        // throwing or landing on the 29th, which is what a day field must do.
        val before = Instant.parse("2027-03-20T01:30:00Z")

        val moved = before.withDay(LocalDate.of(2027, 3, 28), berlin)

        assertThat(moved.localDay(berlin)).isEqualTo(LocalDate.of(2027, 3, 28))
    }

    @Test
    fun tomorrowIsAfterToday_andThisMorningIsNot() {
        // B212: a record is of the past. The comparison is between days, so a reading from earlier today is
        // not "after" a later one, and the date is read in the entry's zone.
        val now = Instant.parse("2026-10-12T22:00:00Z") // 00:00 on the 13th in Berlin

        assertThat(Instant.parse("2026-10-13T00:30:00Z").isMeasurementDayAfterToday(now, berlin)).isFalse()
        assertThat(Instant.parse("2026-10-14T08:00:00Z").isMeasurementDayAfterToday(now, berlin)).isTrue()
        assertThat(Instant.parse("2026-10-12T08:00:00Z").isMeasurementDayAfterToday(now, berlin)).isFalse()
    }
}
