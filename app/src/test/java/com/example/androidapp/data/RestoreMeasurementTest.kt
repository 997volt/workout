package com.example.androidapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.platform.CrashLogStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * The two measurement cases a restore has to get right (ROADMAP B269).
 *
 * Both were found by reading the import path rather than by a device: one aborted the whole import on a
 * foreign key, and the other silently dropped the live entry of a day. They live here, on the JVM, because
 * the restore is pure database work and the instrumented suite is not the gate.
 */
@RunWith(AndroidJUnit4::class)
class RestoreMeasurementTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableListOf<WorkoutDatabase>()
    private val names = mutableListOf<String>()
    private val day: Instant = Instant.parse("2026-03-04T09:00:00Z")

    @After
    fun tearDown() {
        databases.forEach { it.close() }
        names.forEach { context.deleteDatabase(it) }
    }

    @Test
    fun aHiddenRowAndTheLiveRowBesideIt_bothTravel_andTheDayStillReads() = runTest {
        val source = database("restore-source")
        val sourceMeasurements = RoomMeasurementRepository(source, clock())
        sourceMeasurements.save(
            BodyMeasurement(id = "", measuredAt = day, kind = MeasurementKind.DAILY, weightGrams = 80_000L),
        )
        val hidden = sourceMeasurements.observeAll().first().single()
        sourceMeasurements.save(
            BodyMeasurement(id = hidden.id, measuredAt = day, kind = MeasurementKind.DAILY, bodyFatTenths = 200),
        )
        sourceMeasurements.delete(hidden.id)
        // A new row for the same day: the pair the export carries, hidden first by rowid order.
        sourceMeasurements.save(
            BodyMeasurement(id = "", measuredAt = day, kind = MeasurementKind.DAILY, bodyFatTenths = 180),
        )
        val exported = (repository(source).export() as DataResult.Success).data

        val target = database("restore-target")
        val imported = repository(target).import(exported)

        assertThat(imported).isInstanceOf(DataResult.Success::class.java)
        val live = RoomMeasurementRepository(target, clock()).observeAll().first()
        assertThat(live).hasSize(1)
        assertThat(live.single().bodyFatTenths).isEqualTo(180)
    }

    @Test
    fun aFileMeasurementThatYieldsToTheDevicesDay_doesNotTakeItsSoreRowsDownWithIt() = runTest {
        val source = database("restore-sore-source")
        RoomMeasurementRepository(source, clock()).save(
            BodyMeasurement(
                id = "",
                measuredAt = day,
                kind = MeasurementKind.DAILY,
                bodyFatTenths = 220,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 7)),
            ),
        )
        val exported = (repository(source).export() as DataResult.Success).data

        val target = database("restore-sore-target")
        val targetMeasurements = RoomMeasurementRepository(target, clock())
        targetMeasurements.save(
            BodyMeasurement(id = "hand-entered", measuredAt = day, kind = MeasurementKind.DAILY, bodyFatTenths = 180),
        )

        // The day the device already has wins, and the file's sore row has no parent to hang off: the
        // import must skip it rather than fail the whole transaction on the foreign key.
        val imported = repository(target).import(exported)

        assertThat(imported).isInstanceOf(DataResult.Success::class.java)
        val live = targetMeasurements.observeAll().first()
        assertThat(live).hasSize(1)
        assertThat(live.single().id).isEqualTo("hand-entered")
        assertThat(live.single().soreMuscles).isEmpty()
    }

    private fun database(name: String): WorkoutDatabase {
        names += name
        return Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .build()
            .also { databases += it }
    }

    private fun repository(database: WorkoutDatabase): RoomBackupRepository {
        val store = File(context.cacheDir, "restore-crash-${names.size}").apply { mkdirs() }
        return RoomBackupRepository(context, database, clock(), CrashLogStore(store))
    }

    private fun clock(): TimeSource = TimeSource { day.plusSeconds(60) }
}
