package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.WeightGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The goal's past, edited rather than only appended to (ROADMAP N215).
 *
 * What needs a device is the store's arithmetic: a boundary is a day at local midnight, an end is the next
 * boundary, and a correction may not reorder the periods either side of it. The reads that judge a period are a
 * pure JVM test (`GoalOutcomesTest`); this is the rows.
 */
@RunWith(AndroidJUnit4::class)
class GoalPeriodsTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomGoalRepository

    /** A clock the cases can move by a day, because a change at day granularity needs another day (N215). */
    private var now = Instant.parse("2026-09-28T12:00:00Z")
    private val clock = TimeSource { now }
    private val zone = ZoneId.systemDefault()

    private val today: LocalDate get() = now.atZone(zone).toLocalDate()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomGoalRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aPastStretch_endsWhereItSays_andTheGoalThatResumedFollows() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))

        val result = save(
            start = day(2026, 3, 1),
            end = day(2026, 6, 1),
            goal = WeightGoal.Direction(gain = false),
        )

        assertTrue("the stretch was written", result is DataResult.Success)
        val rows = periods()
        assertEquals(3, rows.size)
        assertEquals(
            listOf(day(2026, 1, 1), day(2026, 3, 1), day(2026, 6, 1)),
            rows.map { it.startedAt?.atZone(zone)?.toLocalDate() },
        )
        assertEquals("the cut asked for nothing but a direction", "LOSE", rows[1].kind)
        assertEquals(
            "the goal that covered June resumes there, rather than a gap",
            WeightGoal.Target(80_000L),
            rows[2].weightGoal,
        )
        assertEquals(
            "and that resuming period is the one in force",
            WeightGoal.Target(80_000L),
            repository.observeCurrent().first()[WEIGHT]?.weightGoal,
        )
    }

    @Test
    fun anEndOnADayThatAlreadyStartsAPeriod_writesNoSecondBoundary() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Target(85_000L))

        save(start = day(2026, 2, 1), end = day(2026, 3, 1), goal = WeightGoal.Direction(gain = false))

        assertEquals("March already began there, so nothing is restored", 3, periods().size)
    }

    @Test
    fun theSameDayTwice_isOnePeriod() = runTest {
        save(start = today, goal = WeightGoal.Target(75_000L))

        save(start = today, goal = WeightGoal.Target(75_000L))

        assertEquals("a second set on one day is the same statement", 1, periods().size)
    }

    @Test
    fun theSameGoalOnALaterDay_writesNothing() = runTest {
        // B140 is about the goal, not the day: a new boundary that restates the goal in force is a button
        // press, not a change (B224).
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))

        save(start = day(2026, 2, 1), goal = WeightGoal.Target(80_000L))

        assertEquals(1, periods().size)
    }

    @Test
    fun aSameGoalWithAnEnd_isStillACut() = runTest {
        // The end is the statement, so B140's no-op must not swallow it (B224).
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))

        save(start = day(2026, 2, 1), end = day(2026, 3, 1), goal = WeightGoal.Target(80_000L))

        assertEquals(3, periods().size)
    }

    @Test
    fun aGoalOnAClockBoundaryDay_editsThatRowAndKeepsItsInstant() = runTest {
        // The row the old setGoal path wrote carries 09:15, while N215 asks for 1 March at midnight. The
        // day is the same statement, and a goal-only change keeps the instant it already had (B223).
        val at = day(2026, 3, 1).atTime(9, 15).atZone(zone).toInstant().toEpochMilli()
        insertClockGoal(id = "old", at = at, kind = "TARGET", value = 80_000.0)

        val result = save(start = day(2026, 3, 1), goal = WeightGoal.Target(75_000L))

        assertTrue(result is DataResult.Success)
        val row = periods().single()
        assertEquals("the day's boundary was edited, not duplicated", "old", row.id)
        assertEquals("and its original instant stayed", at, row.startedAt?.toEpochMilli())
        assertEquals(WeightGoal.Target(75_000L), row.weightGoal)
    }

    @Test
    fun theNewestRowOnAClockBoundaryDay_isTheOneAGoalEdits() = runTest {
        // Two rows on one day are a legacy inconsistency. The newest is the goal in force, so that is the
        // row a new statement on the day must change (B223).
        val chosen = day(2026, 3, 1)
        insertClockGoal(
            id = "early",
            at = chosen.atTime(9, 15).atZone(zone).toInstant().toEpochMilli(),
            kind = "TARGET",
            value = 80_000.0,
        )
        insertClockGoal(
            id = "late",
            at = chosen.atTime(15, 0).atZone(zone).toInstant().toEpochMilli(),
            kind = "LOSE",
            value = null,
        )

        save(start = chosen, goal = WeightGoal.Target(75_000L))

        assertEquals(listOf("early", "late"), periods().map { it.id })
        assertEquals(
            "the goal in force changed, rather than the earlier row",
            WeightGoal.Target(75_000L),
            repository.observeCurrent().first()[WEIGHT]?.weightGoal,
        )
    }

    @Test
    fun anOlderSameDayRow_canStillChangeItsGoal() = runTest {
        // The correction of a same-day row is goal-only, so it keeps its place even when a later row shares
        // the day; only a moved start has to find a day of its own (B223).
        val chosen = day(2026, 3, 1)
        insertClockGoal(
            id = "early",
            at = chosen.atTime(9, 15).atZone(zone).toInstant().toEpochMilli(),
            kind = "TARGET",
            value = 80_000.0,
        )
        insertClockGoal(
            id = "late",
            at = chosen.atTime(15, 0).atZone(zone).toInstant().toEpochMilli(),
            kind = "LOSE",
            value = null,
        )

        val result = save(id = "early", start = chosen, goal = WeightGoal.Target(75_000L))

        assertTrue(result is DataResult.Success)
        assertEquals(WeightGoal.Target(75_000L), periods().first().weightGoal)
    }

    @Test
    fun aChangeOnALaterDay_isANewPeriod() = runTest {
        save(start = today.minusDays(10), goal = WeightGoal.Target(75_000L))

        save(start = today, goal = WeightGoal.Target(72_000L))

        assertEquals(2, periods().size)
        assertEquals(WeightGoal.Target(72_000L), repository.observeCurrent().first()[WEIGHT]?.weightGoal)
    }

    @Test
    fun aStartMayNotCrossItsNeighbour() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Target(85_000L))
        val january = periods().first().id

        val refused = save(id = january, start = day(2026, 4, 1), goal = WeightGoal.Target(80_000L))

        assertTrue("a correction keeps the row's place", refused is DataResult.Failure)
        assertEquals(
            listOf(day(2026, 1, 1), day(2026, 3, 1)),
            periods().map { it.startedAt?.atZone(zone)?.toLocalDate() },
        )
    }

    @Test
    fun anEdit_replacesWhatThePeriodAskedFor_andKeepsItsPlace() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val id = periods().single().id

        save(id = id, start = day(2026, 1, 1), goal = WeightGoal.Maintain(81_000L))

        val row = periods().single()
        assertEquals(id, row.id)
        assertEquals(WeightGoal.Maintain(81_000L), row.weightGoal)
        assertEquals(day(2026, 1, 1), row.startedAt?.atZone(zone)?.toLocalDate())
    }

    @Test
    fun anEditThatChangesNothing_writesNothing() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val id = periods().single().id
        val untouched = database.metricGoalDao().findById(id)?.updatedAt

        save(id = id, start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))

        assertEquals(
            "the same statement twice is not a write",
            untouched,
            database.metricGoalDao().findById(id)?.updatedAt,
        )
    }

    @Test
    fun theGoalInForce_hasNoEnd() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val id = periods().single().id

        val refused = save(id = id, start = day(2026, 1, 1), end = day(2026, 6, 1), goal = WeightGoal.Target(80_000L))

        assertTrue("nothing would follow it", refused is DataResult.Failure)
    }

    @Test
    fun anEndMayMoveTheBoundaryAfterIt_butNotSwallowTheOneAfterThat() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Target(85_000L))
        save(start = day(2026, 5, 1), goal = WeightGoal.Target(82_000L))
        val january = periods().first().id

        // April is still before May, so the March period simply shrinks.
        val shortened = save(
            id = january,
            start = day(2026, 1, 1),
            end = day(2026, 4, 1),
            goal = WeightGoal.Target(80_000L),
        )
        assertTrue(shortened is DataResult.Success)
        assertEquals(
            listOf(day(2026, 1, 1), day(2026, 4, 1), day(2026, 5, 1)),
            periods().map { it.startedAt?.atZone(zone)?.toLocalDate() },
        )

        // An end on May's own day would leave the last period starting before its own stretch.
        val swallowed = save(
            id = january,
            start = day(2026, 1, 1),
            end = day(2026, 5, 1),
            goal = WeightGoal.Target(80_000L),
        )
        assertTrue(swallowed is DataResult.Failure)
    }

    @Test
    fun removingAPeriod_mergesItsStretchIntoTheOneBefore() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Direction(gain = true))
        save(start = day(2026, 5, 1), goal = WeightGoal.Target(82_000L))
        val gain = periods()[1].id

        assertTrue(repository.removePeriod(gain) is DataResult.Success)

        assertEquals(
            "the January goal now covers March to May",
            listOf(day(2026, 1, 1), day(2026, 5, 1)),
            periods().map { it.startedAt?.atZone(zone)?.toLocalDate() },
        )
    }

    @Test
    fun removingTheGoalInForce_makesTheOneBeforeItOpen() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Target(85_000L))
        val newest = periods().last().id

        assertTrue(repository.removePeriod(newest) is DataResult.Success)

        assertEquals(1, periods().size)
        assertEquals(WeightGoal.Target(80_000L), repository.observeCurrent().first()[WEIGHT]?.weightGoal)
    }

    @Test
    fun aRemovedPeriod_comesBackWithItsId() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val period = periods().single()

        repository.removePeriod(period.id)
        assertTrue(repository.restorePeriod(WEIGHT, period) is DataResult.Success)

        assertEquals(listOf(period.id), periods().map { it.id })
        assertEquals(WeightGoal.Target(80_000L), periods().single().weightGoal)
    }

    @Test
    fun anUndoOntoADayAlreadyTaken_isRefused() = runTest {
        // A save made while the removal's snackbar was up is newer than the row being put back, and the
        // restore must not write both on one day (B233).
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val removed = periods().single()
        repository.removePeriod(removed.id)
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(75_000L))

        val restored = repository.restorePeriod(WEIGHT, removed)

        assertTrue(restored is DataResult.Failure)
        assertEquals(1, periods().size)
        assertEquals(WeightGoal.Target(75_000L), periods().single().weightGoal)
    }

    @Test
    fun aStartInTheFuture_isRefused() = runTest {
        val refused = save(start = today.plusDays(1), goal = WeightGoal.Target(80_000L))

        assertTrue(refused is DataResult.Failure)
        assertTrue("nothing was written", periods().isEmpty())
    }

    @Test
    fun anEndBeforeTheStart_isRefused() = runTest {
        val refused = save(
            start = day(2026, 3, 1),
            end = day(2026, 2, 1),
            goal = WeightGoal.Target(80_000L),
        )

        assertTrue(refused is DataResult.Failure)
        assertTrue(periods().isEmpty())
    }

    @Test
    fun anEndMayNotPassTheBoundaryThatFollowsIt() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        save(start = day(2026, 3, 1), goal = WeightGoal.Target(85_000L))

        // A period beginning in February ends before March, not after it.
        val refused = save(start = day(2026, 2, 1), end = day(2026, 4, 1), goal = WeightGoal.Direction(gain = false))

        assertTrue(refused is DataResult.Failure)
        assertEquals(2, periods().size)
    }

    @Test
    fun theUndatedFirstPeriod_canBeGivenADate() = runTest {
        database.metricGoalDao().insert(
            MetricGoalEntity(
                id = "legacy:$WEIGHT",
                metricId = WEIGHT,
                kind = "TARGET",
                value = 80_000.0,
                startedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val result = save(id = "legacy:$WEIGHT", start = day(2025, 6, 1), goal = WeightGoal.Target(80_000L))
        assertTrue(result is DataResult.Success)

        val row = periods().single()
        assertNotNull(row.startedAt)
        assertEquals(day(2025, 6, 1), row.startedAt?.atZone(zone)?.toLocalDate())
    }

    @Test
    fun anUndatedPeriodsGoalChangesWithoutInventingAStart() = runTest {
        // B232: the N137 migration leaves the first period undated. Correcting its goal must not invent a
        // beginning the app never had, so the write keeps startedAt null.
        database.metricGoalDao().insert(
            MetricGoalEntity(
                id = "legacy:$WEIGHT",
                metricId = WEIGHT,
                kind = "TARGET",
                value = 80_000.0,
                startedAt = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val result = save(id = "legacy:$WEIGHT", start = null, goal = WeightGoal.Maintain(79_000L))

        assertTrue(result is DataResult.Success)
        val row = periods().single()
        assertNull("the unknown start stays unknown", row.startedAt)
        assertEquals(WeightGoal.Maintain(79_000L), row.weightGoal)
    }

    @Test
    fun aNewPeriodNeedsAStart() = runTest {
        val refused = save(start = null, goal = WeightGoal.Target(80_000L))

        assertTrue(refused is DataResult.Failure)
        assertTrue(periods().isEmpty())
    }

    @Test
    fun aDatedPeriodCannotLoseItsStart() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val id = periods().single().id

        val refused = save(id = id, start = null, goal = WeightGoal.Target(80_000L))

        assertTrue(refused is DataResult.Failure)
        assertEquals(day(2026, 1, 1), periods().single().startedAt?.atZone(zone)?.toLocalDate())
    }

    @Test
    fun removingTheOnlyPeriod_leavesTheMetricWithoutAGoal() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val only = periods().single().id

        assertTrue(repository.removePeriod(only) is DataResult.Success)

        assertTrue(periods().isEmpty())
        assertNull(repository.observeCurrent().first()[WEIGHT])
    }

    private suspend fun save(
        id: String? = null,
        start: LocalDate?,
        end: LocalDate? = null,
        goal: WeightGoal?,
    ): DataResult<Unit> = repository.savePeriod(WEIGHT, id, start, end, goal)

    /** A row the old `setGoal` path wrote: the boundary is a clock instant, not local midnight (B223). */
    private suspend fun insertClockGoal(id: String, at: Long, kind: String?, value: Double?) {
        database.metricGoalDao().insert(
            MetricGoalEntity(
                id = id,
                metricId = WEIGHT,
                kind = kind,
                value = value,
                startedAt = at,
                createdAt = at,
                updatedAt = at,
            ),
        )
    }

    private suspend fun periods() = repository.observePeriods(WEIGHT).first()

    private fun day(year: Int, month: Int, dayOfMonth: Int): LocalDate = LocalDate.of(year, month, dayOfMonth)

    private companion object {
        /** The registry's id for the only metric with a kind, and the only one with an editor (N135, N215). */
        const val WEIGHT = "BODY:WEIGHT"
    }
}
