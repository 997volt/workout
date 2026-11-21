package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneOffsetSource
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.TemplateSetEdit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The last training block, end to end through the schema (ROADMAP N331).
 *
 * The session is started from a template — so the plan snapshot exists — trained, finished, and a
 * measurement is saved; the report must carry the session, the plan beside the performed set, the
 * block day and the measurement in one model.
 */
@RunWith(AndroidJUnit4::class)
class TrainingReportRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomTrainingReportRepository
    private lateinit var workouts: RoomWorkoutRepository
    private lateinit var templates: RoomTemplateRepository
    private lateinit var programs: RoomProgramRepository
    private lateinit var measurements: RoomMeasurementRepository

    private var now = Instant.parse("2026-10-07T12:00:00Z")
    private val clock = TimeSource { now }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        templates = RoomTemplateRepository(database, clock)
        programs = RoomProgramRepository(database, clock)
        workouts = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 })
        measurements = RoomMeasurementRepository(database, clock)
        repository = RoomTrainingReportRepository(
            database = database,
            programs = programs,
            workouts = workouts,
            measurements = measurements,
            settings = PreferencesSettingsRepository(ApplicationProvider.getApplicationContext()),
            timeSource = clock,
        )
        runTest {
            database.exerciseDao().insertAll(
                listOf(
                    ExerciseEntity(
                        id = "back-squat",
                        name = "Back Squat",
                        primaryMuscle = MuscleGroup.QUADS,
                        secondaryMuscles = emptyList(),
                        equipment = Equipment.BARBELL,
                        movementPattern = MovementPattern.SQUAT,
                        createdAt = 0L,
                        updatedAt = 0L,
                        deletedAt = null,
                    ),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theLastBlock_hasItsSessionPlanDayAndMeasurement() = runTest {
        val template = (templates.createTemplate("Push") as DataResult.Success).data
        templates.addExercise(template, "back-squat")
        val planned = templates.observeExercises(template).first().single().id
        templates.addSet(
            planned,
            TemplateSetEdit(role = SetType.NORMAL, targetWeightGrams = 100_000L, targetRepsMax = 5),
        )
        val program = (programs.createProgram("Block") as DataResult.Success).data
        programs.setProgramKind(program, ProgramKind.SEQUENCE)
        programs.setProgramStartDate(program, LocalDate.of(2026, 10, 5))
        programs.addSlot(program, template)

        val started = (workouts.startOrResumeSession(template) as DataResult.Success).data
        val sessionExercise = workouts.observeSessionExercises(started.id).first().single()
        workouts.logSet(
            sessionExercise.id,
            reps = 5,
            weightGrams = 100_000L,
            rpeHalves = 17,
            note = "last set",
        )
        workouts.finishExercise(sessionExercise.id)
        workouts.finishSession(started.id)
        measurements.save(
            BodyMeasurement(
                id = "m1",
                measuredAt = now,
                kind = MeasurementKind.DAILY,
                weightGrams = 80_000L,
                bodyFatTenths = 155,
            ),
        )

        val report = (repository.lastBlock("Last 8 weeks") as DataResult.Success).data

        assertEquals("Block", report.blockName)
        assertEquals(LocalDate.of(2026, 10, 5), report.blockStart)
        // N349: the block's window uses the device's current calendar, as the schedule that produced
        // lastSettled does. Derive the expected day rather than assuming the test device is on UTC.
        assertEquals(now.atZone(ZoneId.systemDefault()).toLocalDate(), report.blockEnd)
        assertTrue(report.blockComplete)

        val session = report.sessions.single()
        assertEquals("Push", session.workout)
        assertEquals("Push", session.dayName)
        assertEquals("last set", session.exercises.single().sets.single().comment)

        val set = report.sessions.single().exercises.single().sets.single()
        assertTrue(set.performed)
        assertEquals(5, set.reps)
        assertEquals(100_000L, set.weightGrams)
        assertEquals(17, set.rpeHalves)
        assertEquals(SetType.NORMAL, set.role)
        // The plan the session started under is beside the performance, not lost.
        assertEquals(SetType.NORMAL, set.plannedRole)
        assertEquals(5, set.plannedReps)
        assertEquals(100_000L, set.plannedWeightGrams)

        val day = report.days.single()
        assertEquals("Push", day.name)
        assertEquals(session.id, day.sessionId)

        val measurement = report.measurements.single()
        assertEquals(80_000L, measurement.weightGrams)
        assertEquals(155, measurement.bodyFatTenths)
    }

    @Test
    fun aCompletedBlock_stopsAtItsLastSettledSession() = runTest {
        // N333: a block is a window, not "everything since the block started". A session trained after
        // the last slot settled does not belong to the block, so it must not stretch the end or add a
        // session — and a measurement after it must not ride along with the block's body data.
        val template = (templates.createTemplate("Push") as DataResult.Success).data
        templates.addExercise(template, "back-squat")
        val planned = templates.observeExercises(template).first().single().id
        templates.addSet(
            planned,
            TemplateSetEdit(role = SetType.NORMAL, targetWeightGrams = 100_000L, targetRepsMax = 5),
        )
        val program = (programs.createProgram("Block") as DataResult.Success).data
        programs.setProgramKind(program, ProgramKind.SEQUENCE)
        programs.setProgramStartDate(program, LocalDate.of(2026, 10, 5))
        programs.addSlot(program, template)

        val first = (workouts.startOrResumeSession(template) as DataResult.Success).data
        val firstExercise = workouts.observeSessionExercises(first.id).first().single()
        workouts.logSet(firstExercise.id, reps = 5, weightGrams = 100_000L, rpeHalves = null, note = null)
        workouts.finishExercise(firstExercise.id)
        workouts.finishSession(first.id)
        measurements.save(
            BodyMeasurement(
                id = "m1",
                measuredAt = now,
                kind = MeasurementKind.DAILY,
                weightGrams = 80_000L,
                bodyFatTenths = 155,
            ),
        )

        val completedOn = now.atZone(ZoneId.systemDefault()).toLocalDate()
        now = Instant.parse("2026-10-20T12:00:00Z")
        val later = (workouts.startOrResumeSession(template) as DataResult.Success).data
        val laterExercise = workouts.observeSessionExercises(later.id).first().single()
        workouts.logSet(laterExercise.id, reps = 5, weightGrams = 110_000L, rpeHalves = null, note = null)
        workouts.finishExercise(laterExercise.id)
        workouts.finishSession(later.id)
        measurements.save(
            BodyMeasurement(
                id = "m2",
                measuredAt = now,
                kind = MeasurementKind.DAILY,
                weightGrams = 81_000L,
                bodyFatTenths = 156,
            ),
        )

        val report = (repository.lastBlock("Last 8 weeks") as DataResult.Success).data

        assertTrue(report.blockComplete)
        assertEquals(completedOn, report.blockEnd)
        assertEquals(first.id, report.sessions.single().id)
        assertEquals(80_000L, report.measurements.single().weightGrams)
    }

    @Test
    fun anUnfinishedBlock_runsToToday_andCarriesWhatWasDoneSoFar() = runTest {
        // N351: only the complete case was covered. An active block's end is today, and the one
        // session trained so far is in the report with its plan snapshot.
        val first = (templates.createTemplate("Push") as DataResult.Success).data
        templates.addExercise(first, "back-squat")
        val planned = templates.observeExercises(first).first().single().id
        templates.addSet(
            planned,
            TemplateSetEdit(role = SetType.NORMAL, targetWeightGrams = 100_000L, targetRepsMax = 5),
        )
        val second = (templates.createTemplate("Pull") as DataResult.Success).data
        val program = (programs.createProgram("Block") as DataResult.Success).data
        programs.setProgramKind(program, ProgramKind.SEQUENCE)
        programs.setProgramStartDate(program, LocalDate.of(2026, 10, 5))
        programs.addSlot(program, first)
        programs.addSlot(program, second)

        val started = (workouts.startOrResumeSession(first) as DataResult.Success).data
        val sessionExercise = workouts.observeSessionExercises(started.id).first().single()
        workouts.logSet(sessionExercise.id, reps = 5, weightGrams = 100_000L, rpeHalves = null, note = null)
        workouts.finishExercise(sessionExercise.id)
        workouts.finishSession(started.id)

        val report = (repository.lastBlock("Last 8 weeks") as DataResult.Success).data

        assertFalse(report.blockComplete)
        assertEquals(LocalDate.of(2026, 10, 5), report.blockStart)
        assertEquals(now.atZone(ZoneId.systemDefault()).toLocalDate(), report.blockEnd)
        assertEquals(started.id, report.sessions.single().id)
        assertEquals("Push", report.sessions.single().dayName)
    }

    @Test
    fun withNoSequenceProgram_lastBlockIsTheEightWeeksItWasNamed() = runTest {
        // N351: with no block the sheet still exports, but under the caller's name and with the
        // eight-week window — not an invented program.
        val template = (templates.createTemplate("Push") as DataResult.Success).data
        templates.addExercise(template, "back-squat")
        val started = (workouts.startOrResumeSession(template) as DataResult.Success).data
        val sessionExercise = workouts.observeSessionExercises(started.id).first().single()
        workouts.logSet(sessionExercise.id, reps = 5, weightGrams = 100_000L, rpeHalves = null, note = null)
        workouts.finishExercise(sessionExercise.id)
        workouts.finishSession(started.id)
        measurements.save(
            BodyMeasurement(
                id = "m1",
                measuredAt = now,
                kind = MeasurementKind.DAILY,
                weightGrams = 80_000L,
                bodyFatTenths = 155,
            ),
        )

        val report = (repository.lastBlock("Last 8 weeks") as DataResult.Success).data

        assertEquals("Last 8 weeks", report.blockName)
        assertFalse(report.blockComplete)
        assertEquals(report.blockEnd.minusWeeks(8), report.blockStart)
        assertEquals(started.id, report.sessions.single().id)
        assertEquals(80_000L, report.measurements.single().weightGrams)
    }
}
