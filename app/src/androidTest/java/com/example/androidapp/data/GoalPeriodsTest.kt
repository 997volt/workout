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
    fun removingTheOnlyPeriod_leavesTheMetricWithoutAGoal() = runTest {
        save(start = day(2026, 1, 1), goal = WeightGoal.Target(80_000L))
        val only = periods().single().id

        assertTrue(repository.removePeriod(only) is DataResult.Success)

        assertTrue(periods().isEmpty())
        assertNull(repository.observeCurrent().first()[WEIGHT])
    }

    private suspend fun save(
        id: String? = null,
        start: LocalDate,
        end: LocalDate? = null,
        goal: WeightGoal?,
    ): DataResult<Unit> = repository.savePeriod(WEIGHT, id, start, end, goal)

    private suspend fun periods() = repository.observePeriods(WEIGHT).first()

    private fun day(year: Int, month: Int, dayOfMonth: Int): LocalDate = LocalDate.of(year, month, dayOfMonth)

    private companion object {
        /** The registry's id for the only metric with a kind, and the only one with an editor (N135, N215). */
        const val WEIGHT = "BODY:WEIGHT"
    }
}
