package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.data.local.MeasurementSoreMuscleEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.at
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Measurements against a real database (ROADMAP N32, two records since N113).
 *
 * The rules worth this suite are the ones DECISIONS.md settled, and they are the kind a fake would agree
 * with whatever it was told: a day holds **one entry of each kind**, an unmeasured site stays blank rather
 * than being carried forward by a correction, a save **replaces** the sore list, and a score off the scale
 * is refused before anything is written.
 */
class MeasurementRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomMeasurementRepository

    private val clock = TimeSource { Instant.parse("2026-10-02T09:00:00Z") }

    /** Morning and evening of the same local day, whatever zone the device is in. */
    private val today = LocalDate.now(ZoneId.systemDefault())
    private fun localHour(hour: Int): Instant =
        today.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomMeasurementRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savingTwiceOnOneDay_editsTheFirstEntry() = runTest {
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        repository.save(entry(at = localHour(20), weightGrams = 81_500L))

        val all = repository.observeAll().first()
        assertEquals("one daily entry for the day, not two", 1, all.size)
        assertEquals("and it holds the later reading", 81_500L, all.single().weightGrams)
    }

    @Test
    fun savingOnAnotherDay_addsAnEntry() = runTest {
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val tomorrow = localHour(8).plusSeconds(86_400L)
        repository.save(entry(at = tomorrow, weightGrams = 81_000L))

        assertEquals(2, repository.observeAll().first().size)
    }

    @Test
    fun aDailyAndATapeEntry_onOneDay_areOneOfEach() = runTest {
        // N113's keying: the day is the local one and the kind is part of it, so a tape day edits its
        // own row rather than the one the scale wrote.
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        repository.save(entry(at = localHour(8), kind = MeasurementKind.TAPE, waistMm = 860L))

        val all = repository.observeAll().first()
        assertEquals("the two records are two rows", 2, all.size)
        assertEquals(
            setOf(MeasurementKind.DAILY, MeasurementKind.TAPE),
            all.map { it.kind }.toSet(),
        )

        val daily = all.single { it.kind == MeasurementKind.DAILY }
        val tape = all.single { it.kind == MeasurementKind.TAPE }
        assertEquals(82_000L, daily.weightGrams)
        assertNull("the tape save left the daily record's own numbers alone", daily.at(TapeSite.WAIST))
        assertEquals(860L, tape.at(TapeSite.WAIST))
        assertNull("and the tape record claims no weight", tape.weightGrams)
    }

    @Test
    fun aSiteTheCorrectionDidNotMeasure_staysBlank() = runTest {
        repository.save(
            entry(at = localHour(8), kind = MeasurementKind.TAPE, waistMm = 860L, chestMm = 900L),
        )
        // The same day's tape record, this time taking the chest and not the waist: the waist is
        // corrected away rather than surviving from the entry this replaces.
        repository.save(entry(at = localHour(20), kind = MeasurementKind.TAPE, chestMm = 900L))

        val stored = repository.observeAll().first().single()
        assertEquals(900L, stored.at(TapeSite.CHEST))
        assertNull(
            "a site nobody measured draws no line",
            stored.at(TapeSite.WAIST),
        )
    }

    @Test
    fun savingTheSoreList_replacesWhatWasThere() = runTest {
        // N62's rule, on the daily record since N113: the form shows exactly what is stored, so a muscle
        // missing from a save was removed by the lifter.
        repository.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8), SoreMuscle(MuscleGroup.CALVES, 3)),
            ),
        )
        repository.save(
            entry(
                at = localHour(20),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 9)),
            ),
        )

        assertEquals(
            "the list is replaced, so the calf is gone rather than kept",
            listOf(SoreMuscle(MuscleGroup.QUADS, 9)),
            repository.observeAll().first().single().soreMuscles,
        )
    }

    @Test
    fun aSoreScoreOffTheScale_isRefused_andNothingIsWritten() = runTest {
        val failure = repository.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 11)),
            ),
        ) as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        assertEquals(
            "a refused save must not write the entry either",
            emptyList<BodyMeasurement>(),
            repository.observeAll().first(),
        )
    }

    @Test
    fun deleting_removesItFromTheList() = runTest {
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val saved = repository.observeAll().first().single()

        repository.delete(saved.id)

        assertEquals(emptyList<BodyMeasurement>(), repository.observeAll().first())
    }

    @Test
    fun deleting_alsoHidesTheSoreRows() = runTest {
        repository.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            ),
        )
        val saved = repository.observeAll().first().single()

        repository.delete(saved.id)

        assertEquals(
            "the rows hang off the entry, so they go with it",
            emptyList<MeasurementSoreMuscleEntity>(),
            database.measurementSoreMuscleDao().observeAll().first(),
        )
    }

    private fun entry(
        at: Instant,
        kind: MeasurementKind = MeasurementKind.DAILY,
        weightGrams: Long? = null,
        waistMm: Long? = null,
        chestMm: Long? = null,
        soreMuscles: List<SoreMuscle> = emptyList(),
    ) = BodyMeasurement(
        id = "",
        measuredAt = at,
        kind = kind,
        weightGrams = weightGrams,
        tape = listOfNotNull(
            waistMm?.let { TapeSite.WAIST to it },
            chestMm?.let { TapeSite.CHEST to it },
        ).toMap(),
        soreMuscles = soreMuscles,
    )
}
