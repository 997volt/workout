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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
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
    fun aBuiltWeek_isWhatTheMissedDayQuestionAsksAbout() = runTest {
        // The slots are replaced for the week: Monday's own workout is not what was scheduled, the
        // deload day is.
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)

        val pending = programs.pendingOccurrences(today, utc).getOrNull().orEmpty()

        assertEquals(1, pending.size)
        assertEquals(created.templateIds.single(), pending.single().templateId)
        assertEquals("Heavy lower — deload", pending.single().templateName)
        assertNull("a deload day is keyed to the week, not a slot", pending.single().slotId)
    }

    @Test
    fun aStartedDeloadDay_silencesTheQuestion() = runTest {
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)
        insertSession("s1", "2026-10-06T09:00:00Z", created.templateIds.single(), finished = false)

        val pending = programs.pendingOccurrences(today, utc).getOrNull().orEmpty()

        assertTrue("a start silences the day even before it is finished", pending.isEmpty())
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
    fun aDeloadSession_pausesTheRun_ratherThanAdvancingIt() = runTest {
        // The deload is a break in the block, not a step through it: its workouts name no slot.
        val created = createDay("Heavy lower — deload", DayOfWeek.MONDAY)
        insertSession("s1", "2026-10-06T09:00:00Z", created.templateIds.single(), finished = true)

        val run = programs.observeProgramRun(program).first()

        assertEquals(programs.observeSlots(program).first().single().id, run?.slot?.id)
        assertEquals(true, run?.isAtStart)
    }

    @Test
    fun aFutureWeek_isRefused() = runTest {
        val result = deloads.createDeload(
            programId = program,
            weekStart = monday.plusWeeks(1),
            days = listOf(dayInput("Heavy lower — deload", DayOfWeek.MONDAY)),
        )

        assertTrue(result is DataResult.Failure && result.error is DataError.Invalid)
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
