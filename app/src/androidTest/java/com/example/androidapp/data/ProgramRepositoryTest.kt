package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
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
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Program writes and the missed-day read, end to end through the schema (ROADMAP P3.3).
 *
 * The occurrence arithmetic itself is a pure JVM test; what needs a device is the part that
 * only exists once rows do — the program's order, "one active program only" as one statement,
 * and an occurrence settled by the session that was actually started from its template.
 */
@RunWith(AndroidJUnit4::class)
class ProgramRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomProgramRepository

    /** The adherence reads moved to their own repository at P3.13; the fixtures are shared. */
    private lateinit var adherence: RoomAdherenceRepository

    private val clock = TimeSource { Instant.parse("2026-10-07T12:00:00Z") }

    // A Wednesday, so Tuesday's slot is behind it and Friday's is ahead.
    private val today = LocalDate.of(2026, 10, 7)
    private val monday = LocalDate.of(2026, 10, 5)
    private val utc = ZoneOffset.UTC

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomProgramRepository(database, clock)
        adherence = RoomAdherenceRepository(database, clock)
        runTest {
            database.exerciseDao().insertAll(listOf(exercise("back-squat")))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aCreatedProgram_comesBackTrimmed_andNotActive() = runTest {
        val created = repository.createProgram("  Upper/Lower  ") as DataResult.Success

        val program = repository.observeProgram(created.data).first()
        assertEquals("Upper/Lower", program?.name)
        assertEquals(false, program?.isActive)
        assertTrue(repository.observeActivePrograms().first().isEmpty())
    }

    @Test
    fun aBlankName_isRefused_andStoresNothing() = runTest {
        val failure = repository.createProgram("   ") as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        assertTrue(repository.observePrograms().first().isEmpty())
    }

    @Test
    fun slots_keepTheOrderTheyWereAdded_andMovingSwapsTwo() = runTest {
        val program = create("Upper/Lower")
        val first = createTemplate("Heavy lower")
        val second = createTemplate("Push")
        repository.addSlot(program, first, DayOfWeek.MONDAY)
        repository.addSlot(program, second, DayOfWeek.WEDNESDAY)

        assertEquals(
            listOf("Heavy lower", "Push"),
            repository.observeSlots(program).first().map { it.templateName },
        )

        val pushSlot = repository.observeSlots(program).first().first { it.templateName == "Push" }
        repository.moveSlot(pushSlot.id, delta = -1)

        assertEquals(
            listOf("Push", "Heavy lower"),
            repository.observeSlots(program).first().map { it.templateName },
        )
    }

    @Test
    fun aSlot_carriesItsWeekday_andUnpinningItLeavesItOrderOnly() = runTest {
        val program = create("Upper/Lower")
        repository.addSlot(program, createTemplate("Heavy lower"), DayOfWeek.MONDAY)
        val slot = slot(program)

        assertEquals(DayOfWeek.MONDAY, slot.weekday)

        repository.setSlotWeekday(slot.id, null)
        assertNull(repository.observeSlots(program).first().single().weekday)
    }

    @Test
    fun moreThanOneProgram_canBeActive_atOnce() = runTest {
        // P3.12 amends P3.3's "at most one": a lifting block and a conditioning one are two
        // schedules at once, and both are followed until one is turned off.
        val first = create("Upper/Lower")
        val second = create("PPL")

        repository.activateProgram(first)
        repository.activateProgram(second)

        assertEquals(
            listOf(first, second),
            repository.observeActivePrograms().first().map { it.id },
        )
        assertEquals(2, repository.observePrograms().first().count { it.isActive })
    }

    @Test
    fun deactivatingAProgram_stopsFollowingOnlyThatOne() = runTest {
        val first = create("Upper/Lower")
        val second = create("PPL")
        repository.activateProgram(first)
        repository.activateProgram(second)

        repository.deactivateProgram(first)

        assertEquals(listOf(second), repository.observeActivePrograms().first().map { it.id })

        repository.deactivateProgram(second)
        assertTrue(repository.observeActivePrograms().first().isEmpty())
    }

    @Test
    fun programs_keepTheAuthoredOrder_andMovingReorders() = runTest {
        // The order is authored rather than alphabetical or by creation time, so it has to
        // survive a move (ROADMAP P3.12).
        val first = create("Alpha")
        val second = create("Beta")
        val third = create("Gamma")

        assertEquals(
            listOf("Alpha", "Beta", "Gamma"),
            repository.observePrograms().first().map { it.name },
        )

        repository.moveProgram(third, delta = -1)

        assertEquals(
            listOf(first, third, second),
            repository.observePrograms().first().map { it.id },
        )
    }

    @Test
    fun programsTiedAtOnePosition_areStillReorderable() = runTest {
        // A file written before programs could be ordered carries no position, so a restore brings
        // every program back at 0 and the displayed order is the name order. Swapping two equal
        // values moves nothing; the move has to re-number the list for the controls to work at all.
        val first = create("Alpha")
        val second = create("Beta")
        database.programDao().setProgramPosition(id = first, position = 0, at = 1L)
        database.programDao().setProgramPosition(id = second, position = 0, at = 1L)

        repository.moveProgram(second, delta = -1)

        assertEquals(
            listOf(second, first),
            repository.observePrograms().first().map { it.id },
        )
    }

    @Test
    fun theMissedDayQuestion_walksEveryActiveProgram() = runTest {
        // Two schedules, each with yesterday's slot undone: the union is what the prompt lists,
        // earliest first (ROADMAP P3.12).
        val lifting = create("Upper/Lower")
        val conditioning = create("Conditioning")
        repository.addSlot(lifting, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        repository.addSlot(conditioning, createTemplate("Intervals"), DayOfWeek.TUESDAY)
        repository.activateProgram(lifting)
        repository.activateProgram(conditioning)

        val pending = repository.pendingOccurrences(today, utc).getOrNull()

        assertEquals(
            setOf(
                repository.observeSlots(lifting).first().single().id,
                repository.observeSlots(conditioning).first().single().id,
            ),
            pending?.map { it.slotId }?.toSet(),
        )
    }

    @Test
    fun clearingTheActiveProgram_leavesThePinsToAnswer() = runTest {
        val program = create("Upper/Lower")
        repository.activateProgram(program)

        repository.deactivateProgram(program)

        assertTrue(repository.observeActivePrograms().first().isEmpty())
    }

    @Test
    fun deletingTheActiveProgram_leavesNoneActive() = runTest {
        val program = create("Upper/Lower")
        repository.activateProgram(program)

        repository.deleteProgram(program)

        assertTrue(repository.observePrograms().first().isEmpty())
        assertTrue(repository.observeActivePrograms().first().isEmpty())
    }

    @Test
    fun deletingATemplate_dropsItsSlotFromTheList() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Push")
        repository.addSlot(program, template, DayOfWeek.FRIDAY)

        database.templateDao().softDeleteTemplate(template, clock.now().toEpochMilli())

        assertTrue(repository.observeSlots(program).first().isEmpty())
    }

    @Test
    fun aMissedDay_isPending_untilASessionStartedFromThatTemplateSettlesIt() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id

        assertEquals(
            listOf(slotId),
            repository.pendingOccurrences(today, utc).getOrNull()?.map { it.slotId },
        )

        // Started from the template, and done *late* — on Wednesday, for Tuesday's slot.
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = "s1",
                startedAt = Instant.parse("2026-10-07T09:00:00Z").toEpochMilli(),
                finishedAt = null,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                zoneOffsetMinutes = 0,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
                templateId = template,
            ),
        )

        assertTrue(repository.pendingOccurrences(today, utc).getOrNull().isNullOrEmpty())
    }

    @Test
    fun recordingASkip_settlesTheOccurrence_andASecondOneDoesNotDuplicateIt() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id

        repository.skipOccurrences(listOf(slotId), monday)
        repository.skipOccurrences(listOf(slotId), monday)

        assertTrue(repository.pendingOccurrences(today, utc).getOrNull().isNullOrEmpty())
        assertEquals(1, database.programSkipDao().findSkipsForWeek(monday.toEpochDay()).size)
    }

    @Test
    fun withNoActiveProgram_nothingIsEverPending() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)

        assertTrue(repository.pendingOccurrences(today, utc).getOrNull().isNullOrEmpty())
    }

    @Test
    fun thePerLiftBreakdown_rollsAVariationUpToItsCategory_notToItsMovement() = runTest {
        // ROADMAP N97, B99: the parent map was built from the rows a template names, so a variation whose
        // *movement* no template named stopped at that movement — while the same movement named beside it
        // counted under the category. One family's work then landed in two rows of one breakdown, which is
        // the fragmentation N97 exists to remove.
        database.exerciseDao().insertAll(
            listOf(
                exercise("cat-bench").copy(name = "Bench Press", rowKind = RowKind.CATEGORY),
                exercise("barbell-bench-press").copy(name = "Barbell Bench Press", parentId = "cat-bench"),
                exercise("speed-day").copy(name = "Speed Day", parentId = "barbell-bench-press"),
            ),
        )
        val templates = RoomTemplateRepository(database, clock)
        val template = (templates.createTemplate("Speed") as DataResult.Success).data
        templates.addExercise(template, "speed-day")
        val program = create("Upper/Lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        insertSession(id = "done", date = "2026-10-06T09:00:00Z", finished = true, templateId = template)

        val report = adherence.monthAdherence(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 15),
            zone = utc,
        ).getOrNull()!!

        val rows = report.adherence.byExercise
        assertEquals(
            "the family is one row, under the category it is filed in",
            listOf("cat-bench"),
            rows.map { it.exerciseId },
        )
        assertEquals("Bench Press", rows.single().exerciseName)
        assertEquals(1, rows.single().done)
    }

    @Test
    fun aFinishedSession_isDone_andAnAbandonedStart_isMissed() = runTest {
        // The rule that makes adherence stricter than the prompt: a session that was started
        // silences P3.3's question, but only one that was *finished* counts as training here.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        insertSession(id = "done", date = "2026-10-06T09:00:00Z", finished = true, templateId = template)
        insertSession(id = "abandoned", date = "2026-10-13T09:00:00Z", finished = false, templateId = template)

        val report = adherence.monthAdherence(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 15),
            zone = utc,
        ).getOrNull()

        assertTrue(report!!.hasActiveProgram)
        assertEquals(1, report.adherence.done)
        assertEquals(1, report.adherence.missed)
        // The unfinished session marks no trained day at all.
        assertEquals(setOf(LocalDate.of(2026, 10, 6)), report.adherence.trainedDays)
        assertEquals(
            setOf(
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 27),
            ),
            report.adherence.scheduledDays,
        )
    }

    @Test
    fun aRecordedSkip_reachesTheMonth_asSkipped_notMissed() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        repository.skipOccurrences(listOf(slot(program).id), monday)

        val report = adherence.monthAdherence(
            month = YearMonth.of(2026, 10),
            today = today,
            zone = utc,
        ).getOrNull()

        assertEquals(1, report!!.adherence.skipped)
        assertEquals(0, report.adherence.missed)
    }

    @Test
    fun adherence_scoresTheUnionOfEveryActiveProgram() = runTest {
        // Two schedules at once (P3.12): the month's counts add up across them, and the days they
        // fall on are the union too.
        val lifting = create("Upper/Lower")
        val conditioning = create("Conditioning")
        repository.addSlot(lifting, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        repository.addSlot(conditioning, createTemplate("Intervals"), DayOfWeek.THURSDAY)
        repository.activateProgram(lifting)
        repository.activateProgram(conditioning)

        val report = adherence.monthAdherence(
            month = YearMonth.of(2026, 10),
            today = LocalDate.of(2026, 10, 8),
            zone = utc,
        ).getOrNull()

        assertTrue(report!!.hasActiveProgram)
        // Both programs' elapsed days were missed: Tuesday the 6th and Thursday the 1st.
        assertEquals(2, report.adherence.missed)
        // And the grid draws the union: both programs' scheduled weekdays are in the month.
        assertTrue(report.adherence.scheduledDays.contains(LocalDate.of(2026, 10, 6)))
        assertTrue(report.adherence.scheduledDays.contains(LocalDate.of(2026, 10, 8)))
    }

    @Test
    fun withNoActiveProgram_theDaysTrainedAreStillRead_butNothingIsScored() = runTest {
        // The calendar needs no schedule; the pins home falls back to carry no skip record, so
        // there is deliberately no ratio to compute.
        insertSession(id = "hand-started", date = "2026-10-06T09:00:00Z", finished = true, templateId = null)

        val report = adherence.monthAdherence(
            month = YearMonth.of(2026, 10),
            today = today,
            zone = utc,
        ).getOrNull()

        assertEquals(false, report!!.hasActiveProgram)
        assertEquals(0, report.adherence.scored)
        assertEquals(null, report.adherence.ratio)
        assertEquals(setOf(LocalDate.of(2026, 10, 6)), report.adherence.trainedDays)
    }

    @Test
    fun aSlotsOwnHistory_isWhatThatSlotProgressesFrom() = runTest {
        // ROADMAP P3.8: one template in two slots, and the heavy Monday and the light Friday
        // progress apart. A session names only the template, so which slot it belongs to is
        // settled the way P3.3 settles an occurrence.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.MONDAY)
        repository.addSlot(program, template, DayOfWeek.FRIDAY)
        val slots = repository.observeSlots(program).first()
        val mondaySlot = slots.first { it.weekday == DayOfWeek.MONDAY }
        val fridaySlot = slots.first { it.weekday == DayOfWeek.FRIDAY }

        insertSessionWithSet("mon", "2026-09-28T09:00:00Z", template, weightGrams = 100_000L)
        insertSessionWithSet("fri", "2026-10-02T09:00:00Z", template, weightGrams = 80_000L)

        val fromMonday = repository
            .slotPreviousPerformance(mondaySlot.id, "back-squat", "current", utc)
            .getOrNull()
        val fromFriday = repository
            .slotPreviousPerformance(fridaySlot.id, "back-squat", "current", utc)
            .getOrNull()

        assertEquals(100_000L, fromMonday?.sets?.single()?.weightGrams)
        assertEquals(80_000L, fromFriday?.sets?.single()?.weightGrams)
    }

    /** A finished session from [templateId] with one logged set of `back-squat` (P3.8). */
    private suspend fun insertSessionWithSet(
        id: String,
        date: String,
        templateId: String,
        weightGrams: Long,
    ) {
        val startedAt = Instant.parse(date).toEpochMilli()
        database.workoutDao().insertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = startedAt,
                finishedAt = startedAt + 3_600_000,
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
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "$id-se",
                sessionId = id,
                exerciseId = "back-squat",
                position = 0,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSet(
            SetEntryEntity(
                id = "$id-set",
                sessionExerciseId = "$id-se",
                setIndex = 0,
                reps = 5,
                weightGrams = weightGrams,
                setType = SetType.NORMAL,
                completedAt = null,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
    }

    @Test
    fun theRun_followsTheLastSlotTrained_notTheDay() = runTest {
        // ROADMAP P3.9: an order-only program still runs A -> B, and a day passing moves nothing.
        val program = create("Upper/Lower")
        val first = createTemplate("Workout A")
        val second = createTemplate("Workout B")
        repository.addSlot(program, first)
        repository.addSlot(program, second)
        val slots = repository.observeSlots(program).first()

        val atStart = repository.observeProgramRun(program).first()
        assertEquals(slots[0].id, atStart?.slot?.id)
        assertEquals(true, atStart?.isAtStart)

        insertSessionWithSet("a-session", "2026-10-06T09:00:00Z", first, weightGrams = 100_000L)

        val moved = repository.observeProgramRun(program).first()
        assertEquals(slots[1].id, moved?.slot?.id)
        assertEquals(false, moved?.isAtStart)
    }

    @Test
    fun aSkip_movesTheRunOn_too() = runTest {
        val program = create("Upper/Lower")
        val first = createTemplate("Workout A")
        repository.addSlot(program, first)
        repository.addSlot(program, createTemplate("Workout B"))
        val slots = repository.observeSlots(program).first()

        repository.skipOccurrences(listOf(slots[0].id), monday)

        assertEquals(slots[1].id, repository.observeProgramRun(program).first()?.slot?.id)
    }

    @Test
    fun aDeloadWeek_isNotScored_untilItIsUnmarked() = runTest {
        // ROADMAP P3.10: a deliberate back-off must not read as a failure, and unmarking restores
        // the ordinary reading.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)

        adherence.setDeloadWeek(program, monday, marked = true)
        // Idempotent: marking a marked week must not double-record.
        adherence.setDeloadWeek(program, monday, marked = true)
        assertEquals(1, database.programDeloadDao().countDeload(program, monday.toEpochDay()))

        val marked = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()
        assertEquals(0, marked!!.adherence.scored)
        assertEquals(setOf(monday), marked.deloadWeeks[program])
        assertTrue(marked.programs.any { it.id == program })

        adherence.setDeloadWeek(program, monday, marked = false)

        val unmarked = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()
        assertEquals(1, unmarked!!.adherence.missed)
        assertTrue(unmarked.deloadWeeks[program].isNullOrEmpty())
    }

    @Test
    fun aSessionStartedFromTheSubstitute_settlesTheSlot_andAdherenceScoresItDone() = runTest {
        // ROADMAP P3.11: the picked workout is recorded for one slot and one week, and a session
        // started from it settles the occurrence — or the app would keep asking about a day already
        // trained.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        val substitute = createTemplate("Dumbbell version")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id

        repository.setSubstitution(slotId, monday, substitute)
        insertSessionWithSet("sub", "2026-10-06T09:00:00Z", substitute, weightGrams = 60_000L)

        assertTrue(repository.pendingOccurrences(today, utc).getOrNull().isNullOrEmpty())
        val report = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()
        assertEquals(1, report!!.adherence.done)
        assertEquals(0, report.adherence.missed)
    }

    @Test
    fun clearingASubstitution_restoresTheSlotsOwnWorkout() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        val substitute = createTemplate("Dumbbell version")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id
        repository.setSubstitution(slotId, monday, substitute)

        repository.setSubstitution(slotId, monday, null)
        insertSessionWithSet("sub", "2026-10-06T09:00:00Z", substitute, weightGrams = 60_000L)

        // The substitute no longer stands in, so the Tuesday is unresolved and still pending.
        assertEquals(
            listOf(slotId),
            repository.pendingOccurrences(today, utc).getOrNull()?.map { it.slotId },
        )
    }

    @Test
    fun substitutingAWorkoutThatDoesNotExist_isRefused() = runTest {
        val program = create("Upper/Lower")
        repository.addSlot(program, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        val slotId = slot(program).id

        val result = repository.setSubstitution(slotId, monday, "no-such-template")

        assertTrue(result is DataResult.Failure)
    }

    @Test
    fun aScheduledDay_canBeMarkedSkippedAndUnmarked() = runTest {
        // ROADMAP P3.13: a correction is a second, explicit writer beside the prompt's whole week.
        val program = create("Upper/Lower")
        repository.addSlot(program, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id
        val tuesday = monday.plusDays(1)

        val missed = adherence.occurrencesOn(tuesday, today, utc).getOrNull()!!.single()
        assertEquals(OccurrenceState.MISSED, missed.state)
        assertTrue(missed.canCorrect)

        adherence.setOccurrenceSkipped(slotId, monday, skipped = true)
        assertEquals(
            OccurrenceState.SKIPPED,
            adherence.occurrencesOn(tuesday, today, utc).getOrNull()!!.single().state,
        )

        adherence.setOccurrenceSkipped(slotId, monday, skipped = false)
        assertEquals(
            OccurrenceState.MISSED,
            adherence.occurrencesOn(tuesday, today, utc).getOrNull()!!.single().state,
        )
    }

    @Test
    fun correctingASkip_movesTheMonthBetweenMissedAndSkipped() = runTest {
        val program = create("Upper/Lower")
        repository.addSlot(program, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        val slotId = slot(program).id

        adherence.setOccurrenceSkipped(slotId, monday, skipped = true)
        val skipped = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()!!
        assertEquals(1, skipped.adherence.skipped)
        assertEquals(0, skipped.adherence.missed)

        adherence.setOccurrenceSkipped(slotId, monday, skipped = false)
        val missed = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()!!
        assertEquals(0, missed.adherence.skipped)
        assertEquals(1, missed.adherence.missed)
    }

    @Test
    fun aDoneOccurrence_isNotOfferedForCorrection() = runTest {
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        insertSessionWithSet("done", "2026-10-06T09:00:00Z", template, weightGrams = 100_000L)

        val occurrence = adherence.occurrencesOn(monday.plusDays(1), today, utc).getOrNull()!!.single()
        assertEquals(OccurrenceState.DONE, occurrence.state)
        assertFalse("a finished session is the record", occurrence.canCorrect)
    }

    @Test
    fun theMonthIsBrokenDownBySlotAndByLift() = runTest {
        // ROADMAP P3.14: the same aggregate, read per slot and per lift over the N14 join.
        val program = create("Upper/Lower")
        val lower = createTemplate("Heavy lower")
        repository.addSlot(program, lower, DayOfWeek.MONDAY)
        repository.activateProgram(program)

        val report = adherence.monthAdherence(YearMonth.of(2026, 10), today, utc).getOrNull()!!

        val slotRow = report.adherence.bySlot.single { it.templateId == lower }
        // The parts are the whole.
        assertEquals(report.adherence.missed, report.adherence.bySlot.sumOf { it.missed })
        // createTemplate plans back-squat, so the lift's counts are that one slot's.
        val squat = report.adherence.byExercise.single { it.exerciseId == "back-squat" }
        assertEquals(slotRow.missed, squat.missed)
        assertEquals(slotRow.done, squat.done)
        // Named from the library, not the id: the row is read, not parsed.
        assertTrue(squat.exerciseName.isNotBlank())
        assertTrue(squat.exerciseName != "back-squat")
    }

    @Test
    fun aRunOfDoneOccurrences_isCountedFromTheProgramsFirstWeek() = runTest {
        // ROADMAP P3.15: occurrences, not days. The program was created today, so the walk starts
        // at this week's Monday and Tuesday the 6th is the only occurrence in it.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        insertSessionWithSet("done", "2026-10-06T09:00:00Z", template, weightGrams = 100_000L)

        val streak = adherence.streak(today, utc).getOrNull()

        assertEquals(1, streak?.count)
        assertEquals(monday.plusDays(1), streak?.startedOn)
    }

    @Test
    fun anUntrainedTuesday_leavesNoRunGoing() = runTest {
        val program = create("Upper/Lower")
        repository.addSlot(program, createTemplate("Heavy lower"), DayOfWeek.TUESDAY)
        repository.activateProgram(program)

        val streak = adherence.streak(today, utc).getOrNull()

        assertEquals(0, streak?.count)
        assertNull(streak?.startedOn)
    }

    @Test
    fun withNoActiveProgram_thereIsNoRun() = runTest {
        assertNull(adherence.streak(today, utc).getOrNull())
    }

    @Test
    fun theRatioHistory_endsWithThisMonth_andAgreesWithTheGrid() = runTest {
        // ROADMAP P3.16: a point and the grid it came from cannot disagree.
        val program = create("Upper/Lower")
        val template = createTemplate("Heavy lower")
        repository.addSlot(program, template, DayOfWeek.TUESDAY)
        repository.activateProgram(program)
        insertSessionWithSet("done", "2026-10-06T09:00:00Z", template, weightGrams = 100_000L)

        val history = adherence.ratioHistory(months = 3, today, utc).getOrNull()!!

        assertEquals(3, history.size)
        assertEquals(YearMonth.of(2026, 10), history.last().month)
        history.forEach { point ->
            val grid = adherence.monthAdherence(point.month, today, utc).getOrNull()!!
            assertEquals(grid.adherence.ratio, point.ratio)
        }
    }

    /** A session row, finished or abandoned, started from [templateId] or by hand. */
    private suspend fun insertSession(id: String, date: String, finished: Boolean, templateId: String?) {        val startedAt = Instant.parse(date).toEpochMilli()
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

    private suspend fun create(name: String): String =
        (repository.createProgram(name) as DataResult.Success).data

    /** The single slot of a program the test has already filled, in its order (P3.3). */
    private suspend fun slot(programId: String): ProgramSlot =
        repository.observeSlots(programId).first().single()

    private suspend fun createTemplate(name: String): String {
        val templates = RoomTemplateRepository(database, clock)
        val id = (templates.createTemplate(name) as DataResult.Success).data
        templates.addExercise(id, "back-squat")
        return id
    }

    private fun exercise(
        id: String,
        parentId: String? = null,
        rowKind: RowKind = RowKind.MOVEMENT,
    ) = ExerciseEntity(
        id = id,
        // A display name that is *not* the id: `exerciseAdherence` falls back to the id when the
        // library has no row, so a fixture named after its own id could not tell the two apart.
        name = id.replace('-', ' ').replaceFirstChar { it.uppercase() },
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        isCustom = false,
        parentId = parentId,
        rowKind = rowKind,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )
}
