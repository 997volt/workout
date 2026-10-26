package com.example.androidapp.domain

import com.example.androidapp.ui.workout.WorkoutFormat
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The two formatters are one implementation now (ROADMAP, Simplicity).
 *
 * They used to differ, which is the point of this test: `RestTimer.format` had no
 * hours branch, so a 90-minute rest rendered `90:00` while a workout of the same
 * length rendered `1:30:00`. Asserting they agree is what stops that returning.
 */
class DurationFormatTest {

    @Test
    fun minutesAndSeconds_areZeroPadded() {
        assertEquals("0:00", DurationFormat.ofSeconds(0))
        assertEquals("0:07", DurationFormat.ofSeconds(7))
        assertEquals("1:05", DurationFormat.ofSeconds(65))
        assertEquals("59:59", DurationFormat.ofSeconds(3_599))
    }

    @Test
    fun pastAnHour_growsAnHoursField() {
        assertEquals("1:00:00", DurationFormat.ofSeconds(3_600))
        assertEquals("1:30:00", DurationFormat.ofSeconds(5_400))
    }

    @Test
    fun negativeInput_clamps_toZeroRatherThanRenderingANegativeTime() {
        assertEquals("0:00", DurationFormat.ofSeconds(-90))
    }

    @Test
    fun theRestTimerAndTheWorkoutClockNowAgree() {
        // The regression this file exists for.
        assertEquals("1:30:00", RestTimer.format(5_400))
        assertEquals(RestTimer.format(5_400), WorkoutFormat.elapsed(Duration.ofSeconds(5_400)))
        assertEquals(RestTimer.format(65), WorkoutFormat.elapsed(Duration.ofSeconds(65)))
    }

    @Test
    fun aHold_parsesTheShapesALifterTypes() {
        // ROADMAP N220: the field accepts what the format prints and what a lifter writes.
        assertThat(DurationFormat.parseSeconds("45")).isEqualTo(45)
        assertThat(DurationFormat.parseSeconds("1:30")).isEqualTo(90)
        assertThat(DurationFormat.parseSeconds("1:30:00")).isEqualTo(5_400)
        assertThat(DurationFormat.parseSeconds(" 0:45 ")).isEqualTo(45)
    }

    @Test
    fun anEmptyField_isUnansweredRatherThanZero() {
        // Blank is the same distinction reps makes: nobody has said how long, which is not "no time".
        assertThat(DurationFormat.parseSeconds("")).isNull()
        assertThat(DurationFormat.parseSeconds("   ")).isNull()
    }

    @Test
    fun somethingThatIsNotADuration_parsesToNull() {
        // A refused value must not become a hold, and the save gate reads null as "not yet a set".
        assertThat(DurationFormat.parseSeconds("soon")).isNull()
        assertThat(DurationFormat.parseSeconds("1:2:3:4")).isNull()
        assertThat(DurationFormat.parseSeconds("-30")).isNull()
        assertThat(DurationFormat.parseSeconds("1::30")).isNull()
    }
}
