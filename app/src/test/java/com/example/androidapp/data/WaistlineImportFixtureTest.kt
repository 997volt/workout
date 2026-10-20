package com.example.androidapp.data

import com.example.androidapp.data.transfer.BackupCodec
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

/**
 * The app's importer accepts the helper's Waistline body data (N166).
 *
 * The fixture is what the import helper writes for a three-day Waistline backup: DAILY rows only,
 * weight in grams, body fat and muscle in tenths of a percent, no tape sites. This test decodes it
 * with the app's own codec — the gate the file actually has to pass — and pins the numbers, so a
 * change on either side that would make the phone reject or misread the file fails here.
 */
class WaistlineImportFixtureTest {

    @Test
    fun theHelpersBodyDataIsABackupTheAppAccepts() {
        val file = BackupCodec.decode(File(repoRoot, "docs/fixtures/waistline-import.json").readText())

        assertThat(file.schemaVersion).isEqualTo(5)
        assertThat(file.measurements).hasSize(3)
        assertThat(file.sessions).isEmpty()

        val days = file.measurements.sortedBy { it.measuredAt }
        assertThat(days.map { it.kind })
            .containsExactly(MeasurementKind.DAILY, MeasurementKind.DAILY, MeasurementKind.DAILY)
            .inOrder()
        assertThat(days.map { it.weightGrams }).containsExactly(83_000L, 82_500L, 80_000L).inOrder()
        assertThat(days.map { it.bodyFatTenths }).containsExactly(184, 182, null).inOrder()
        assertThat(days.map { it.muscleTenths }).containsExactly(null, 413, null).inOrder()

        days.forEach { day ->
            assertThat(day.kind).isEqualTo(MeasurementKind.DAILY)
            assertThat(day.id).startsWith("mes_")
            assertThat(day.neckMm).isNull()
            assertThat(day.waistMm).isNull()
            assertThat(day.deletedAt).isNull()
        }
        assertThat(days.map { it.id }.toSet()).hasSize(3)

        // The day survives the trip: each noon lands on the intended UTC date, not the one before.
        assertThat(
            days.map {
                Instant.ofEpochMilli(it.measuredAt).atZone(ZoneOffset.UTC).toLocalDate().toString()
            }
        ).containsExactly("2026-01-01", "2026-01-02", "2026-01-03").inOrder()
    }
}
