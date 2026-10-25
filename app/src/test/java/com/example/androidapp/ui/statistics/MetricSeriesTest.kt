package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.ExerciseTrendMetric
import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.TrendMetric
import com.example.androidapp.domain.model.TrendPoint
import com.example.androidapp.domain.model.jointSiteKey
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.time.Instant

/**
 * Reading a series out of whatever the metric is stored in (ROADMAP N35).
 *
 * No database and no repositories: the twenty-one series come from three queries, and the part worth
 * testing is which field each one reads and what it does with a moment that recorded nothing.
 */
class MetricSeriesTest {

    private val first = Instant.parse("2026-09-01T07:00:00Z")
    private val second = Instant.parse("2026-09-08T07:00:00Z")

    @Test
    fun aWorkoutMetricTakesItsValueFromThePoint() {
        val series = metricSeries(
            key = MetricKey.Workout(TrendMetric.RPE),
            workouts = listOf(
                TrendPoint(startedAt = first, averageRpe = 7.5),
                TrendPoint(startedAt = second, averageRpe = 8.0),
            ),
        )

        assertThat(series.readings.map { it.value }).isEqualTo(listOf(7.5, 8.0))
        assertThat(series.readings.map { it.at }).isEqualTo(listOf(first, second))
    }

    @Test
    fun aJointSiteSeriesReadsItsOwnSite_notTheSessionsAverage() {
        // ROADMAP N130: the dimension the average drops. One session, two knees, and each series takes
        // only its own — a chart for the left knee must not be drawn from the 4 that is about neither.
        val series = metricSeries(
            key = MetricKey.JointSite(Joint.KNEE, Side.LEFT),
            workouts = listOf(
                TrendPoint(
                    startedAt = first,
                    averageJointPain = 6.0,
                    jointPainBySite = mapOf(
                        jointSiteKey(Joint.KNEE, Side.LEFT) to 6.0,
                        jointSiteKey(Joint.KNEE, Side.RIGHT) to 2.0,
                    ),
                ),
                TrendPoint(
                    startedAt = second,
                    averageJointPain = 2.0,
                    jointPainBySite = mapOf(jointSiteKey(Joint.KNEE, Side.RIGHT) to 2.0),
                ),
            ),
        )

        assertThat(series.readings.map { it.value }).isEqualTo(listOf(6.0, null))
    }

    @Test
    fun anExerciseJointSiteSeriesReadsThatLiftsOwnSite() {
        // ROADMAP N130's per-lift half. The same session reports a left knee in two ways: the workout's
        // own point carries it, and the chosen lift's point carries it — and a lift that named no site
        // is a gap, not the workout's number borrowed.
        val series = metricSeries(
            key = MetricKey.ExerciseJointSite(Joint.KNEE, Side.LEFT),
            exercises = listOf(
                ExerciseTrendPoint(
                    startedAt = first,
                    jointPainBySite = mapOf(jointSiteKey(Joint.KNEE, Side.LEFT) to 7.0),
                ),
                ExerciseTrendPoint(
                    startedAt = second,
                    jointPainBySite = mapOf(jointSiteKey(Joint.KNEE, Side.RIGHT) to 4.0),
                ),
            ),
        )

        assertThat(series.readings.map { it.value }).isEqualTo(listOf(7.0, null))
    }

    @Test
    fun aMomentThatRecordedNothing_isKeptAsAGap() {
        // The assertion that matters for N37: three sessions, one of which rated nothing, must not come
        // back as two readings a week apart with the gap quietly closed.
        val series = metricSeries(
            key = MetricKey.Workout(TrendMetric.RPE),
            workouts = listOf(
                TrendPoint(startedAt = first, averageRpe = 7.5),
                TrendPoint(startedAt = second),
            ),
        )

        assertWithMessage("both moments, not just the one with a value").that(series.readings.size).isEqualTo(2)
        assertWithMessage("the second recorded nothing").that(series.readings[1].value).isNull()
    }

    @Test
    fun theExerciseMetricsReadTheirOwnFields() {
        val point = ExerciseTrendPoint(
            startedAt = first,
            heaviestSetGrams = 102_500L,
            volumeGrams = 4_100_000L,
            totalReps = 40,
            leastAssistanceGrams = 20_000L,
        )

        assertThat(
            metricSeries(MetricKey.Exercise(ExerciseTrendMetric.HEAVIEST_SET), exercises = listOf(point))
                .readings.single().value
        ).isEqualTo(102_500.0)
        assertThat(
            metricSeries(MetricKey.Exercise(ExerciseTrendMetric.TOTAL_REPS), exercises = listOf(point))
                .readings.single().value
        ).isEqualTo(40.0)
        assertThat(
            metricSeries(MetricKey.Exercise(ExerciseTrendMetric.ASSISTANCE), exercises = listOf(point))
                .readings.single().value
        ).isEqualTo(20_000.0)
    }

    @Test
    fun theBodyMetricsReadTheirOwnFields() {
        val entry = measurement(weight = 82_400L, bodyFat = 183, muscle = 421)

        assertThat(
            metricSeries(MetricKey.Body(BodyMetric.WEIGHT), measurements = listOf(entry))
                .readings.single().value
        ).isEqualTo(82_400.0)
        assertThat(
            metricSeries(MetricKey.Body(BodyMetric.BODY_FAT), measurements = listOf(entry))
                .readings.single().value
        ).isEqualTo(183.0)
        assertThat(
            metricSeries(MetricKey.Body(BodyMetric.MUSCLE), measurements = listOf(entry))
                .readings.single().value
        ).isEqualTo(421.0)
    }

    @Test
    fun aTapeEntry_isNotAWeighIn() {
        // B221: every measurement row became a weight reading, so a tape day was a null point and the line
        // stopped and restarted there. A tape entry is not a moment of the weight series at all.
        val series = metricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            measurements = listOf(
                measurement(at = first, weight = 82_400L),
                measurement(at = second, waist = 864L, kind = MeasurementKind.TAPE),
            ),
        )

        assertThat(series.readings.map { it.value }).isEqualTo(listOf(82_400.0))
    }

    @Test
    fun aDailyEntryThatLeftTheWeightBlank_isNotAPointInTheWeightSeries() {
        // The other half of B221: the day recorded soreness and no weight, and the weigh-ins either side are
        // not in question — so the line runs between them rather than stopping at a day that weighed nothing.
        val series = metricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            measurements = listOf(
                measurement(at = first, weight = 82_400L),
                measurement(at = second, bodyFat = 183),
            ),
        )

        assertThat(series.readings.map { it.value }).isEqualTo(listOf(82_400.0))
    }

    @Test
    fun aBodySeries_holdsNoGaps_becauseAReadingsAbsenceIsNotAMoment() {
        // What the chart reads: with no null in the series there is nothing to break the line at. A skipped
        // weigh-in is a longer segment between two readings, which is the shape the chart already drew for a
        // day with no entry at all. The scope is pinned by the two tests above it — a tape **site** and a
        // soreness score still leave their gap (N37), because there the entry is a moment of that series.
        val series = metricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            measurements = listOf(
                measurement(at = first, weight = 82_400L),
                measurement(at = second, kind = MeasurementKind.TAPE, waist = 864L),
            ),
        )

        assertThat(series.readings.none { it.value == null }).isTrue()
    }

    @Test
    fun sorenessReadsTheMuscleItNames_andLeavesTheOthersGaps() {
        // N118: the daily record states soreness one muscle at a time (N113), so one series per muscle is
        // what the data already is. A muscle the day did not mention is a **gap**, not a zero — a line at
        // zero would claim the lifter was not sore rather than that they did not say.
        val entry = BodyMeasurement(
            id = "m",
            measuredAt = first,
            kind = MeasurementKind.DAILY,
            soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
        )

        val quads = metricSeries(MetricKey.Soreness(MuscleGroup.QUADS), measurements = listOf(entry))
        val calves = metricSeries(MetricKey.Soreness(MuscleGroup.CALVES), measurements = listOf(entry))

        assertThat(quads.readings.single().value).isEqualTo(8.0)
        assertThat(calves.readings.single().value).isNull()
    }

    @Test
    fun aTapeSiteThatWasNotMeasured_isAGapRatherThanAnAbsence() {
        // "I weighed myself but did not take a waist" is a real entry, and it has to read as a gap at that
        // moment rather than as an entry that does not exist (ROADMAP N32).
        val series = metricSeries(
            key = MetricKey.Tape(TapeSite.WAIST),
            measurements = listOf(measurement(waist = null, kind = MeasurementKind.TAPE)),
        )

        assertThat(series.readings.size).isEqualTo(1)
        assertThat(series.readings.single().value).isNull()
    }

    @Test
    fun aTapeSiteReadsItsOwnValue_whenItWasMeasured() {
        val series = metricSeries(
            key = MetricKey.Tape(TapeSite.WAIST),
            measurements = listOf(measurement(waist = 864L, kind = MeasurementKind.TAPE)),
        )

        assertThat(series.readings.single().value).isEqualTo(864.0)
    }

    @Test
    fun theSeriesIsOldestFirst_whateverOrderItArrivedIn() {
        // Measurements arrive newest-first and the training series oldest-first. One of those would have
        // been wrong to trust, so the series sorts rather than assuming.
        val series = metricSeries(
            key = MetricKey.Body(BodyMetric.WEIGHT),
            measurements = listOf(
                measurement(at = second, weight = 82_000L),
                measurement(at = first, weight = 83_000L),
            ),
        )

        assertThat(series.readings.map { it.at }).isEqualTo(listOf(first, second))
        assertThat(series.readings.map { it.value }).isEqualTo(listOf(83_000.0, 82_000.0))
    }

    @Test
    fun aMetricWithNoSource_hasNoReadings() {
        val series = metricSeries(MetricKey.Workout(TrendMetric.RPE))

        assertThat(series.readings).isEqualTo(emptyList<MetricReading>())
    }

    private fun measurement(
        at: Instant = first,
        weight: Long? = null,
        bodyFat: Int? = null,
        muscle: Int? = null,
        waist: Long? = null,
        kind: MeasurementKind = MeasurementKind.DAILY,
    ) = BodyMeasurement(
        id = "m",
        measuredAt = at,
        kind = kind,
        weightGrams = weight,
        bodyFatTenths = bodyFat,
        muscleTenths = muscle,
        tape = listOfNotNull(waist?.let { TapeSite.WAIST to it }).toMap(),
    )
}
