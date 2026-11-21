package com.example.androidapp.data

import com.example.androidapp.domain.model.WorkoutProgram
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Where a block begins when it has no authored start (N350). */
class TrainingReportStartTest {

    private val today = LocalDate.of(2026, 10, 7)
    private val created = Instant.parse("2026-03-02T12:00:00Z")

    private fun program(start: LocalDate?, createdAtMillis: Long) = WorkoutProgram(
        id = "program",
        name = "Block",
        startDate = start,
        createdAtMillis = createdAtMillis,
    )

    @Test
    fun theAuthoredStartWins_thenTheCreationDay_thenToday() {
        val authored = LocalDate.of(2026, 1, 5)

        assertThat(blockStart(program(authored, created.toEpochMilli()), today, ZoneOffset.UTC))
            .isEqualTo(authored)
        assertThat(blockStart(program(null, created.toEpochMilli()), today, ZoneOffset.UTC))
            .isEqualTo(LocalDate.of(2026, 3, 2))
        assertThat(blockStart(program(null, 0L), today, ZoneOffset.UTC)).isEqualTo(today)
    }
}
