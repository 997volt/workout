package com.example.androidapp.ui.programs

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.PreferencesSettingsRepository
import com.example.androidapp.data.RoomDeloadRepository
import com.example.androidapp.data.RoomProgramRepository
import com.example.androidapp.data.RoomTemplateRepository
import com.example.androidapp.data.RoomWorkoutRepository
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.ZoneOffsetSource
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * The creator's one read, against the real repositories (ROADMAP N147).
 *
 * The screen's draft is only as good as this snapshot: a slot's template, its planned sets, the last
 * performance that fills a gap, and the unit the load is shown in. A template that plans no exercises is
 * not a day, and the week's existing plan is what makes the screen say a rebuild replaces it.
 */
@RunWith(AndroidJUnit4::class)
class DeloadSourceLoaderTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var programs: RoomProgramRepository
    private lateinit var templates: RoomTemplateRepository
    private lateinit var deloads: RoomDeloadRepository
    private lateinit var loader: RoomDeloadSourceLoader

    private val clock = TimeSource { Instant.parse("2026-10-07T12:00:00Z") }
    private val monday = LocalDate.of(2026, 10, 5)

    private lateinit var program: String
    private lateinit var template: String

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        programs = RoomProgramRepository(database, clock)
        templates = RoomTemplateRepository(database, clock)
        deloads = RoomDeloadRepository(database, clock)
        val settings = PreferencesSettingsRepository(ApplicationProvider.getApplicationContext())
        runTest { settings.setWeightUnit(WeightUnit.KILOGRAMS) }
        loader = RoomDeloadSourceLoader(
            programRepository = programs,
            templateRepository = templates,
            workoutRepository = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 }),
            deloadRepository = deloads,
            settingsRepository = settings,
        )
        runTest {
            database.exerciseDao().insertAll(listOf(exercise()))
            program = (programs.createProgram("Upper/Lower") as DataResult.Success).data
            template = createTemplate()
            programs.addSlot(program, template, DayOfWeek.MONDAY)
            // A slot whose plan is empty is not a deload day: there is nothing to tick (N147).
            val empty = (templates.createTemplate("Empty") as DataResult.Success).data
            programs.addSlot(program, empty, DayOfWeek.WEDNESDAY)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theDays_areTheSlotsThatPlanSomething_withTheirSetsAndWeekday() = runTest {
        insertSessionWithSet("done", "2026-10-06T09:00:00Z")

        val snapshot = loader.load(program, monday)
            ?: error("the program exists")

        assertEquals("Upper/Lower", snapshot.programName)
        assertEquals("the empty plan is not a day", 1, snapshot.days.size)
        val day = snapshot.days.single()
        assertEquals("Heavy lower", day.name)
        assertEquals(DayOfWeek.MONDAY, day.weekday)

        val exercise = day.exercises.single()
        assertEquals("Back squat", exercise.name)
        assertEquals(2_500L, exercise.stepGrams)
        assertEquals(WeightUnit.KILOGRAMS, exercise.weightUnit)
        assertEquals(3, exercise.sets.size)
        assertEquals(100_000L, exercise.sets.first().targetWeightGrams)
        assertEquals(5, exercise.sets.first().targetRepsMin)
        assertEquals(
            "the last finished session fills the history the rule reads",
            1,
            exercise.previous.sets.size,
        )
        assertFalse("no plan has been built for this week yet", snapshot.hasExistingPlan)
    }

    @Test
    fun aBuiltWeek_isWhatTheSnapshotReports() = runTest {
        val planned = templates.observeExercises(template).first().single()
        deloads.createDeload(
            programId = program,
            weekStart = monday,
            days = listOf(
                DeloadDayInput(
                    name = "Heavy lower — deload",
                    weekday = DayOfWeek.MONDAY,
                    exercises = listOf(
                        DeloadExerciseInput(
                            exerciseId = planned.exerciseId,
                            restSeconds = planned.restSeconds,
                            techniqueNote = planned.techniqueNote,
                            supersetGroup = planned.supersetGroup,
                            sets = deloadSetTargets(planned.sets, previous = null, stepGrams = 2_500L),
                        ),
                    ),
                ),
            ),
        )

        val snapshot = loader.load(program, monday)

        assertTrue("the rebuild hint has something to say", snapshot?.hasExistingPlan == true)
    }

    @Test
    fun aGoneProgram_hasNoSnapshot() = runTest {
        assertNull(loader.load("missing", monday))
    }

    private suspend fun createTemplate(): String {
        val id = (templates.createTemplate("Heavy lower") as DataResult.Success).data
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

    /** A finished workout from the program's template, so the exercise has a history to read. */
    private suspend fun insertSessionWithSet(id: String, date: String) {
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
                templateId = template,
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
                weightGrams = 100_000L,
                setType = SetType.NORMAL,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
    }

    private fun exercise() = ExerciseEntity(
        id = "back-squat",
        name = "Back squat",
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
