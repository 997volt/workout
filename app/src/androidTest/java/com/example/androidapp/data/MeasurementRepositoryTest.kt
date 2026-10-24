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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    /** Morning and evening of the same local day, whatever zone the device is in. */
    private val today = LocalDate.now(ZoneId.systemDefault())

    /**
     * A clock that advances a second per call (ROADMAP N133).
     *
     * The suite's writes used to share one instant, which is fine until an undo has to tell "before the
     * delete" from "after it" — and the whole refusal is that comparison. One second per call is also
     * what the real clock does, so the suite exercises the ordering the device will.
     *
     * It starts on **today**, because a record dated after the clock's day is refused (B212). The suite's
     * entries are today's, so a clock fixed in the past would make every one of them a future record.
     */
    private var now = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant()
    private val clock = TimeSource {
        now.also { now = it.plusSeconds(1) }
    }

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
        // A day after today is refused now (B212), so "another day" is taken from the past.
        val yesterday = localHour(8).minusSeconds(86_400L)
        repository.save(entry(at = yesterday, weightGrams = 81_000L))

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

    @Test
    fun aDeletedEntry_comesBackWithItsSoreRows() = runTest {
        // N133: the delete is soft, so the way back is clearing `deletedAt` — the same mechanism N90's
        // restore uses — and the rows the delete hid return with the entry that owns them.
        repository.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            ),
        )
        val saved = repository.observeAll().first().single()
        repository.delete(saved.id)

        assertTrue(repository.restore(saved.id) is DataResult.Success)

        val restored = repository.observeAll().first().single()
        assertEquals(saved.id, restored.id)
        assertEquals(82_000L, restored.weightGrams)
        assertEquals(
            "the sore list comes back whole, not empty",
            listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            restored.soreMuscles,
        )
    }

    @Test
    fun restore_isRefused_whenTheDayHasBeenReEnteredSince() = runTest {
        // A measurement's identity is its day and its kind (N32, N113), so an undo that landed on a day
        // re-entered since would silently replace the newer reading — the one thing an undo must not do.
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val deleted = repository.observeAll().first().single()
        repository.delete(deleted.id)
        repository.save(entry(at = localHour(20), weightGrams = 81_000L))

        val failure = repository.restore(deleted.id) as DataResult.Failure

        assertTrue("the refusal is a sentence, not a storage error", failure.error is DataError.Invalid)
        assertEquals(
            "and the newer reading is left exactly as the lifter wrote it",
            81_000L,
            repository.observeAll().first().single().weightGrams,
        )
    }

    @Test
    fun restore_isRefused_onlyForTheSameKindAsTheReEntry() = runTest {
        // The identity an undo has to respect is the day **and** the kind (N32, N113), so a tape entry
        // written after the daily one went is a different record: it does not occupy the daily one's
        // place, and the undo is not refused on its account.
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val deleted = repository.observeAll().first().single()
        repository.delete(deleted.id)
        repository.save(entry(at = localHour(20), kind = MeasurementKind.TAPE, waistMm = 860L))

        assertTrue(repository.restore(deleted.id) is DataResult.Success)
        assertEquals(
            setOf(MeasurementKind.DAILY, MeasurementKind.TAPE),
            repository.observeAll().first().map { it.kind }.toSet(),
        )
    }

    @Test
    fun aSaveAndADelete_inOneMillisecond_doNotConfuseTheUndo() = runTest {
        // B135. The restore correlates the rows a delete hid by the timestamp that delete wrote, and the
        // save path writes the same field when it replaces a list — so a save and a delete sharing a
        // millisecond would be one marker, and the undo would restore the soreness the save replaced. The
        // clock is frozen on purpose: only a unique write timestamp can tell the two apart.
        val frozen = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant()
        val frozenClock = RoomMeasurementRepository(database, TimeSource { frozen })

        frozenClock.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 8)),
            ),
        )
        frozenClock.save(
            entry(
                at = localHour(8),
                weightGrams = 82_000L,
                soreMuscles = listOf(SoreMuscle(MuscleGroup.CALVES, 7)),
            ),
        )
        val saved = frozenClock.observeAll().first().single()
        frozenClock.delete(saved.id)

        assertTrue(frozenClock.restore(saved.id) is DataResult.Success)
        assertEquals(
            "only what the delete hid comes back, not what the save replaced",
            listOf(SoreMuscle(MuscleGroup.CALVES, 7)),
            frozenClock.observeAll().first().single().soreMuscles,
        )
    }

    @Test
    fun aCorrectionCanMoveAnEntryToAnotherDay() = runTest {
        // A request in use; no roadmap id covers it. The day was fixed when the record was saved, so a morning
        // typed onto the wrong date had no way back. The row keeps its id: this is the same entry on another
        // day rather than a second one written beside it.
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val saved = repository.observeAll().first().single()
        // A move is a correction of a past record (B212), so the other day is three days back, not forward.
        val otherDay = saved.measuredAt.minusSeconds(3 * 86_400L)

        repository.save(saved.copy(measuredAt = otherDay, weightGrams = 81_500L))

        val all = repository.observeAll().first()
        assertEquals("moved, not copied", 1, all.size)
        assertEquals("and it is the same entry", saved.id, all.single().id)
        assertEquals(otherDay, all.single().measuredAt)
        assertEquals(81_500L, all.single().weightGrams)
    }

    @Test
    fun movingAnEntryOntoADayThatAlreadyHasOne_isRefused_andNothingChanges() = runTest {
        // A record's identity is its day and its kind (N32, N113), and the restore path already refuses to
        // resurrect one onto a day re-entered since (N133). A move is the other way to make that collision,
        // and it is refused rather than letting a day hold two live entries.
        repository.save(entry(at = localHour(8), weightGrams = 82_000L))
        val first = repository.observeAll().first().single()
        // The second entry is yesterday's, so both days are in the past and the collision is the only refusal.
        repository.save(entry(at = localHour(8).minusSeconds(86_400L), weightGrams = 83_000L))
        val second = repository.observeAll().first().single { it.id != first.id }

        val failure = repository.save(second.copy(measuredAt = first.measuredAt)) as DataResult.Failure

        assertTrue("the refusal is a sentence, not a storage error", failure.error is DataError.Invalid)
        assertEquals(
            "the refused move changed neither row",
            listOf(first, second).sortedBy { it.measuredAt },
            repository.observeAll().first().sortedBy { it.measuredAt },
        )
    }

    @Test
    fun aReadingDatedAfterToday_isRefused_andNothingIsWritten() = runTest {
        // B212: a record is of the past. The picker does not offer a future day, and this is the boundary for
        // anything that did not come through the form — a chart plotting a reading that has not happened is a
        // claim about the future, and the rate verdict would read it as the newest one.
        val failure = repository.save(
            entry(at = localHour(8).plusSeconds(86_400L), weightGrams = 82_000L),
        ) as DataResult.Failure

        assertTrue("the refusal is a sentence, not a storage error", failure.error is DataError.Invalid)
        assertEquals("and nothing was written", 0, repository.observeAll().first().size)
    }

    @Test
    fun twoSavesOnADayThatArriveTogether_leaveOneEntry() = runTest {
        // B151. The day's read used to happen **outside** the save's transaction, so two saves racing for the
        // same day and kind both found nothing, both inserted, and the day held two live entries — which every
        // read then lists and every chart then plots twice. `find` and `findOnDay` both filter
        // `deletedAt IS NULL`, so neither is any help against it, and there is no unique index to catch it
        // either. The read is inside the transaction now, so the pair is one critical section.
        //
        // The race is what is being pinned, not the interleaving: with the read outside the transaction the
        // second save's `find` runs while the first has not committed, which is exactly what launching both
        // without awaiting does here.
        val first = async { repository.save(entry(at = localHour(8), weightGrams = 82_000L)) }
        val second = async { repository.save(entry(at = localHour(8), weightGrams = 83_000L)) }
        first.await()
        second.await()

        assertEquals(
            "one entry of a kind per day, whichever save arrived second",
            1,
            repository.observeAll().first().size,
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
