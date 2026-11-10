package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.ProgramEntity
import com.example.androidapp.data.local.ProgramSlotEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionTargets
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.repository.ProgressionSetWrite
import com.example.androidapp.domain.repository.TemplateSetEdit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant

/**
 * The auto progression state and its two writes (ROADMAP N299).
 *
 * The baseline is captured with the switch and restored by reset; a step writes the plan and its
 * log in one call, and undo only takes it back while the plan still holds what it wrote. Those are
 * the properties the screens rely on, so they are asserted against the real store rather than fakes.
 */
@RunWith(AndroidJUnit4::class)
class ProgressionRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var progression: RoomProgressionRepository
    private lateinit var templates: RoomTemplateRepository

    private val clock = TimeSource { Instant.ofEpochMilli(NOW) }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        progression = RoomProgressionRepository(database, clock)
        templates = RoomTemplateRepository(database, clock)
        runTest { library() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun enabling_capturesTheBaseline_andResetRestoresIt() = runTest {
        assertTrue(progression.setAutoProgression("p1", true) is DataResult.Success)
        assertTrue(database.programDao().findProgram("p1")?.autoProgression == true)
        assertEquals(NOW, progression.baselineCapturedAt("p1").getOrNull())

        val baseline = database.progressionDao().findBaselines("p1").single()
        assertEquals(100_000L, baseline.weightGrams)
        assertEquals(8, baseline.repsCurrent)

        setTarget(weightGrams = 110_000L, repsCurrent = 5)
        assertEquals(1, (progression.resetToBaseline("p1") as DataResult.Success).data)

        val restored = database.templateSetDao().findTemplateSet(SET_ID)!!
        assertEquals(100_000L, restored.targetWeightGrams)
        assertEquals(8, restored.targetRepsCurrent)
    }

    @Test
    fun aStep_writesThePlanAndItsLog_thenUndoTakesItBack() = runTest {
        val id = (applyStep() as DataResult.Success).data

        val moved = database.templateSetDao().findTemplateSet(SET_ID)!!
        assertEquals(102_500L, moved.targetWeightGrams)
        assertEquals(5, moved.targetRepsCurrent)

        assertTrue(progression.undo(id) is DataResult.Success)
        val back = database.templateSetDao().findTemplateSet(SET_ID)!!
        assertEquals(100_000L, back.targetWeightGrams)
        assertEquals(8, back.targetRepsCurrent)
        assertTrue(database.progressionDao().findWrite(id)?.revertedAt != null)
    }

    @Test
    fun undo_isRefused_afterThePlanMovedAgain() = runTest {
        val id = (applyStep() as DataResult.Success).data
        setTarget(weightGrams = 120_000L, repsCurrent = 5)

        val refused = progression.undo(id)

        assertTrue(refused is DataResult.Failure)
        // The lifter's own edit is left alone: that is the work the check exists to protect.
        assertEquals(120_000L, database.templateSetDao().findTemplateSet(SET_ID)!!.targetWeightGrams)
    }

    @Test
    fun undo_isRefused_twice_andLeavesTheFirstUndoInPlace() = runTest {
        val id = (applyStep() as DataResult.Success).data
        assertTrue(progression.undo(id) is DataResult.Success)

        assertTrue(progression.undo(id) is DataResult.Failure)
        val set = database.templateSetDao().findTemplateSet(SET_ID)!!
        assertEquals(100_000L, set.targetWeightGrams)
        assertEquals(8, set.targetRepsCurrent)
        assertTrue(database.progressionDao().findWrite(id)?.revertedAt != null)
    }

    @Test
    fun aStepWhoseRecordDisagreesWithThePlan_isRefused() = runTest {
        val write = stepWrite(afterWeight = 130_000L)

        assertTrue(templates.applyProgression(SET_ID, stepEdit(), write) is DataResult.Failure)
        assertEquals(
            "nothing was written",
            100_000L,
            database.templateSetDao().findTemplateSet(SET_ID)!!.targetWeightGrams,
        )
        assertTrue(database.progressionDao().allWrites().isEmpty())
    }

    private suspend fun applyStep(): DataResult<String> =
        templates.applyProgression(SET_ID, stepEdit(), stepWrite(afterWeight = 102_500L))

    private fun stepEdit() = TemplateSetEdit(
        role = SetType.NORMAL,
        targetWeightGrams = 102_500L,
        targetRepsMin = 5,
        targetRepsMax = 8,
        targetRepsCurrent = 5,
    )

    private fun stepWrite(afterWeight: Long) = ProgressionSetWrite(
        programId = "p1",
        sessionExerciseId = "se1",
        direction = ProgressionDirection.LOAD,
        before = ProgressionTargets(weightGrams = 100_000L, repsCurrent = 8),
        after = ProgressionTargets(weightGrams = afterWeight, repsCurrent = 5),
    )

    private suspend fun setTarget(weightGrams: Long, repsCurrent: Int) {
        val set = database.templateSetDao().findTemplateSet(SET_ID)!!
        database.templateSetDao().updateTemplateSet(
            set.copy(targetWeightGrams = weightGrams, targetRepsCurrent = repsCurrent),
        )
    }

    private suspend fun library() {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "bench",
                    name = "Bench Press",
                    primaryMuscle = null,
                    secondaryMuscles = emptyList(),
                    equipment = null,
                    movementPattern = null,
                    createdAt = NOW,
                    updatedAt = NOW,
                    deletedAt = null,
                ),
            ),
        )
        database.programDao().insertProgram(
            ProgramEntity(
                id = "p1",
                name = "Upper/Lower",
                createdAt = NOW,
                updatedAt = NOW,
                deletedAt = null,
            ),
        )
        database.templateDao().insertTemplate(
            TemplateEntity(
                id = "t1",
                name = "Day A",
                versionOf = null,
                createdAt = NOW,
                updatedAt = NOW,
                deletedAt = null,
            ),
        )
        database.templateDao().insertTemplateExercise(
            TemplateExerciseEntity(
                id = "te1",
                templateId = "t1",
                exerciseId = "bench",
                position = 0,
                restSeconds = null,
                techniqueNote = null,
                targetRpeHalves = 16,
                supersetGroup = null,
                createdAt = NOW,
                updatedAt = NOW,
                deletedAt = null,
            ),
        )
        database.templateSetDao().insertTemplateSet(
            TemplateSetEntity(
                id = SET_ID,
                templateExerciseId = "te1",
                setIndex = 0,
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetAssistanceGrams = null,
                targetRepsMin = 5,
                targetRepsMax = 8,
                targetRepsCurrent = 8,
                dropValueGrams = null,
                targetDurationSeconds = null,
                note = null,
                createdAt = NOW,
                updatedAt = NOW,
                deletedAt = null,
            ),
        )
        database.programDao().insertSlot(
            ProgramSlotEntity(
                id = "slot1",
                programId = "p1",
                templateId = "t1",
                position = 0,
                weekday = DayOfWeek.MONDAY,
                phaseWeek = null,
                createdAt = NOW,
                updatedAt = NOW,
                deletedAt = null,
            ),
        )
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val SET_ID = "ts1"
    }
}
