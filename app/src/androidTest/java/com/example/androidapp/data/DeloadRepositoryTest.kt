package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.OccurrenceState
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.deloadSetTargets
import com.example.androidapp.domain.repository.DeloadDayInput
import com.example.androidapp.domain.repository.DeloadExerciseInput
import com.example.androidapp.domain.repository.TemplateSetEdit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * Building a program's deload week, end to end through the schema (ROADMAP N147).
 *
 * What needs a device is the part only rows can show: the mark, the templates and the week's days are
 * one transaction, the plan replaces the slots the week would have offered, and a rebuild leaves the
 * old templates in the library. The arithmetic itself is the pure [deloadSetTargets] test.
 */
@RunWith(AndroidJUnit4::class)
class DeloadRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var deloads: RoomDeloadRepository
    private lateinit var programs: RoomProgramRepository
    private lateinit var templates: RoomTemplateRepository
    private lateinit var adherence: RoomAdherenceRepository

    private val clock = TimeSource { Instant.parse("2026-10-07T12:00:00Z") }

    // A Wednesday, so Monday is behind it and the week has started.
    private val today = LocalDate.of(2026, 10, 7)
    private val monday = LocalDate.of(2026, 10, 5)
    private val utc = ZoneOffset.UTC
    private val previousTimeZone: TimeZone = TimeZone.getDefault()

    private lateinit var program: String
    private lateinit var template: String

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        deloads = RoomDeloadRepository(database, clock)
        programs = RoomProgramRepository(database, clock)
        templates = RoomTemplateRepository(database, clock)
        adherence = RoomAdherenceRepository(database, clock)
        runTest {
            database.exerciseDao().insertAll(listOf(exercise("back-squat")))
            program = (programs.createProgram("Upper/Lower") as DataResult.Success).data
            template = createTemplate("Heavy lower")
            programs.addSlot(program, template, DayOfWeek.MONDAY)
            programs.activateProgram(program)
        }
    }

    @After
    fun tearDown() {
        database.close()
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun buildingAWeek_marksIt_writesItsTemplates_andThePlan() = runTest {
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        assertEquals(1, created.dayCount)
        // The mark and the plan are the same act: the week is exempt (P3.10) and its days are what it
        // prescribes (N147).
        assertEquals(1, database.programDeloadDao().countDeload(program, monday.toEpochDay()))
        val plan = deloads.observeWeekPlans(monday).first().single { it.programId == program }
        assertEquals(1, plan.days.size)
        assertEquals("Heavy lower — deload", plan.days.single().templateName)
        assertEquals(DayOfWeek.MONDAY, plan.days.single().weekday)

        val planned = templates.observeExercises(created.templateIds.single()).first()
        assertEquals(1, planned.size)
        assertEquals(3, planned.single().sets.size)
        assertTrue(planned.single().sets.all { it.role == SetType.NORMAL })
        assertEquals(
            "three 100 kg sets at 60%",
            60_000L,
            planned.single().sets.first().targetWeightGrams,
        )
        assertEquals(5, planned.single().sets.first().targetRepsMin)
    }

    @Test
    fun aBuiltWeek_isNotAskedAbout_byTheStartQuestion() = runTest {
        // B163: the week is exempt and a deload day has no slot a skip could record, so the prompt
        // does not ask about a built week at all. The slots it replaced are not the week's schedule.
        createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        val pending = programs.pendingOccurrences(today, utc).getOrNull().orEmpty()

        assertTrue("a built week has nothing to prompt about", pending.isEmpty())
    }

    @Test
    fun rebuildingAWeek_replacesItsDays_andKeepsTheOlderWorkouts() = runTest {
        val first = createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        val second = createDay("Push — deload", DayOfWeek.WEDNESDAY)

        val plan = deloads.observeWeekPlans(monday).first().single { it.programId == program }
        assertEquals("the plan is the one just built", 1, plan.days.size)
        assertEquals("Push — deload", plan.days.single().templateName)
        assertEquals(DayOfWeek.WEDNESDAY, plan.days.single().weekday)
        assertTrue(
            "the first plan's workout is still in the library",
            templates.observeTemplates().first().any { it.id == first.templateIds.single() },
        )
        assertTrue(
            "and the rebuild wrote a workout of its own",
            second.templateIds.single() != first.templateIds.single(),
        )
    }

    @Test
    fun unmarkingTheWeek_putsTheSlotsBack_andLeavesTheWorkouts() = runTest {
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        adherence.setDeloadWeek(program, monday, marked = false)

        assertTrue(deloads.observeWeekPlans(monday).first().isEmpty())
        val pending = programs.pendingOccurrences(today, utc).getOrNull().orEmpty()
        assertEquals("the slot's own Monday is what is missed again", template, pending.single().templateId)
        assertTrue(
            "the template survives the unmark",
            templates.observeTemplates().first().any { it.id == created.templateIds.single() },
        )
    }

    @Test
    fun aPreviousPlan_isReadForItsProgramOnly() = runTest {
        // B168: two programs can build a plan in one week; the reuse must not see the other's days.
        createDay("Heavy lower — deload", DayOfWeek.MONDAY)
        val other = (programs.createProgram("Other") as DataResult.Success).data
        programs.addSlot(other, template, DayOfWeek.MONDAY)
        programs.activateProgram(other)
        (
            deloads.createDeload(
                programId = other,
                weekStart = monday,
                days = listOf(
                    dayInput("Other A", DayOfWeek.MONDAY),
                    dayInput("Other B", DayOfWeek.WEDNESDAY),
                ),
            ) as DataResult.Success
            )

        val last = deloads.findLastPlan(program, monday.plusWeeks(1))!!

        assertEquals(1, last.days.size)
        assertEquals("Heavy lower — deload", last.days.single().templateName)
    }

    @Test
    fun aNextWeekPlan_isAccepted_butTheFollowingWeekIsRefused() = runTest {
        // N158: the creator plans the next week, so the window is current-or-next and no further.
        val next = monday.plusWeeks(1)
        val created = deloads.createDeload(
            programId = program,
            weekStart = next,
            days = listOf(dayInput("Next — deload", DayOfWeek.MONDAY)),
        ) as DataResult.Success
        assertEquals(next, created.data.weekStart)

        val beyond = deloads.createDeload(
            programId = program,
            weekStart = monday.plusWeeks(2),
            days = listOf(dayInput("Beyond — deload", DayOfWeek.MONDAY)),
        ) as DataResult.Failure
        assertEquals(
            DataError.Invalid("A deload week starts this week or next."),
            beyond.error,
        )
    }

    @Test
    fun aNextWeekPlan_isWhatTheCalendarShows() = runTest {
        val next = monday.plusWeeks(1)
        (
            deloads.createDeload(
                programId = program,
                weekStart = next,
                days = listOf(dayInput("Next — deload", DayOfWeek.MONDAY)),
            ) as DataResult.Success
            )

        val report = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()!!

        assertTrue(next in report.adherence.scheduledDays)
        assertEquals(setOf(next), report.deloadWeeks[program])
    }

    @Test
    fun aBuiltWeek_isWhatTheCalendarAndDayDialogShow() = runTest {
        // B161: the plan's own day is what the calendar draws and the day dialog lists, and the slot
        // it replaced is not — on either weekday.
        val created = createDay("Heavy lower — deload", DayOfWeek.WEDNESDAY)
        val wednesday = LocalDate.of(2026, 10, 7)

        val report = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()!!
        assertTrue(wednesday in report.adherence.scheduledDays)
        assertTrue("the slot's Monday is replaced by the plan", monday !in report.adherence.scheduledDays)
        assertEquals(setOf(monday), report.deloadPlans[program])

        val onWednesday = adherence.occurrencesOn(wednesday, today, utc).getOrNull().orEmpty()
        assertEquals(1, onWednesday.size)
        assertEquals(created.templateIds.single(), onWednesday.single().templateId)
        assertEquals(OccurrenceState.DELOAD, onWednesday.single().state)
        assertTrue("a deload day is not correctable", !onWednesday.single().canCorrect)

        assertTrue(
            "the slot's own Monday is not the week's schedule",
            adherence.occurrencesOn(monday, today, utc).getOrNull().orEmpty().isEmpty(),
        )
    }

    @Test
    fun aPreviousPlan_isWhatUseLastDeloadReads() = runTest {
        // N150: the last plan before the week, with the exercises each day kept; the share is not stored.
        createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        assertNull("nothing before this week's plan", deloads.findLastPlan(program, monday))

        val last = deloads.findLastPlan(program, monday.plusWeeks(1))

        assertEquals(monday, last?.weekStart)
        assertEquals(listOf("back-squat"), last?.days?.single()?.exerciseIds)
        assertEquals(1, last?.days?.size)
    }

    @Test
    fun aDayMayKeepTheSameExerciseTwice() = runTest {
        // N157: the template writer and the schema accept the same movement twice on a day.
        val source = templates.observeExercises(template).first().single()
        val input = DeloadExerciseInput(
            exerciseId = source.exerciseId,
            restSeconds = source.restSeconds,
            techniqueNote = source.techniqueNote,
            supersetGroup = source.supersetGroup,
            sets = deloadSetTargets(source.sets, previous = null, stepGrams = 2_500L),
        )
        val created = (
            deloads.createDeload(
                programId = program,
                weekStart = monday,
                days = listOf(
                    DeloadDayInput(
                        name = "Deload A",
                        weekday = DayOfWeek.MONDAY,
                        exercises = listOf(input, input),
                    ),
                ),
            ) as DataResult.Success
            ).data

        val written = templates.observeExercises(created.templateIds.single()).first()

        assertEquals(listOf("back-squat", "back-squat"), written.map { it.exerciseId })
    }

    @Test
    fun aDeloadSession_pausesTheRun_ratherThanAdvancingIt() = runTest {
        // The deload is a break in the block, not a step through it: its workouts name no slot.
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)
        insertSession("s1", "2026-10-06T09:00:00Z", created.templateIds.single(), finished = true)

        val run = programs.observeProgramRun(program).first()

        assertEquals(programs.observeSlots(program).first().single().id, run?.slot?.id)
        assertEquals(true, run?.isAtStart)
    }

    @Test
    fun aWeekBeyondTheNextOrInThePast_isRefused() = runTest {
        // N158 narrows the window to current-or-next: the creator plans the next week, a past week must
        // never be written, and anything past next is refused.
        val beyond = deloads.createDeload(
            programId = program,
            weekStart = monday.plusWeeks(2),
            days = listOf(dayInput("Heavy lower — deload", DayOfWeek.MONDAY)),
        )
        val past = deloads.createDeload(
            programId = program,
            weekStart = monday.minusWeeks(1),
            days = listOf(dayInput("Heavy lower — deload", DayOfWeek.MONDAY)),
        )

        assertTrue(beyond is DataResult.Failure && beyond.error is DataError.Invalid)
        assertTrue(past is DataResult.Failure && past.error is DataError.Invalid)
    }

    @Test
    fun aWeekWithTwoDays_writesBoth_inTheOrderTheyWereListed() = runTest {
        val created = (
            deloads.createDeload(
                programId = program,
                weekStart = monday,
                days = listOf(
                    dayInput("Heavy lower — deload", DayOfWeek.MONDAY),
                    dayInput("Push — deload", DayOfWeek.WEDNESDAY),
                ),
            ) as DataResult.Success
            ).data

        assertEquals(2, created.dayCount)
        assertNotEquals(created.templateIds[0], created.templateIds[1])

        val plan = deloads.observeWeekPlans(monday).first().single { it.programId == program }
        assertEquals(
            listOf("Heavy lower — deload", "Push — deload"),
            plan.days.map { it.templateName },
        )
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), plan.days.map { it.weekday })
        assertEquals(listOf(0, 1), plan.days.map { it.position })
    }

    @Test
    fun aWeekMarkedByHand_getsThePlanAttached_ratherThanASecondMark() = runTest {
        // P3.10 can mark a week with no plan; the creator attaches to that mark rather than writing a
        // second one, so the screen's toggle and the creator cannot disagree about the week (N147).
        adherence.setDeloadWeek(program, monday, marked = true)

        createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        assertEquals(1, database.programDeloadDao().countDeload(program, monday.toEpochDay()))
        val plan = deloads.observeWeekPlans(monday).first().single { it.programId == program }
        assertEquals(1, plan.days.size)
    }

    @Test
    fun aWeekWithNothingInIt_isRefused_andWritesNothing() = runTest {
        val result = deloads.createDeload(program, monday, emptyList())

        assertTrue(result is DataResult.Failure)
        assertEquals(0, database.programDeloadDao().countDeload(program, monday.toEpochDay()))
    }

    /** Builds one day the way the creator's screen would, and returns what the write made. */
    private suspend fun createDay(
        name: String,
        weekday: DayOfWeek,
    ) = (
        deloads.createDeload(
            programId = program,
            weekStart = monday,
            days = listOf(dayInput(name, weekday)),
        ) as DataResult.Success
        ).data

    private suspend fun dayInput(name: String, weekday: DayOfWeek): DeloadDayInput {
        val source = templates.observeExercises(template).first().single()
        return DeloadDayInput(
            name = name,
            weekday = weekday,
            exercises = listOf(
                DeloadExerciseInput(
                    exerciseId = source.exerciseId,
                    restSeconds = source.restSeconds,
                    techniqueNote = source.techniqueNote,
                    supersetGroup = source.supersetGroup,
                    sets = deloadSetTargets(source.sets, previous = null, stepGrams = 2_500L),
                ),
            ),
        )
    }

    private suspend fun createTemplate(name: String): String {
        val id = (templates.createTemplate(name) as DataResult.Success).data
        templates.addExercise(id, "back-squat")
        val planned = templates.observeExercises(id).first().single()
        repeat(3) {
            templates.addSet(
                planned.id,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 5,
                ),
            )
        }
        return id
    }

    private suspend fun insertSession(
        id: String,
        date: String,
        templateId: String,
        finished: Boolean,
    ) {
        val startedAt = Instant.parse(date).toEpochMilli()
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = startedAt,
                finishedAt = if (finished) startedAt + 3_600_000 else null,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                zoneOffsetMinutes = 0,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
                templateId = templateId,
            ),
        )
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id.replace('-', ' ').replaceFirstChar { it.uppercase() },
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        parentId = null,
        rowKind = RowKind.MOVEMENT,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )
}
