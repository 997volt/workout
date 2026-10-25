package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Counting the records inside a window (ROADMAP N35).
 *
 * The query has to be right about four things a fake would agree with whatever it was told: that matching a
 * best is not beating it, that a warm-up is never a record, that "earlier" means the order they were
 * performed in, and that the window is what bounds the count. Only SQLite can be asked.
 */
class StatisticsDaoTest {

    /**
     * The roles that may be a record, read from the domain rule rather than typed here (B155).
     *
     * The query used to name `'WARMUP'` alone, so a drop or cluster rung was counted as a record the app never
     * announced. It takes the list now, and this is the same expression `RoomStatisticsRepository` passes.
     */
    private val RECORDABLE = SetType.entries.filter { it.recordsEffort }.map { it.name }

    private lateinit var database: WorkoutDatabase
    private val dao get() = database.statisticsDao()

    private val day = 86_400_000L

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        database.exerciseDao().insertAll(
            listOf(exercise("back-squat"), exercise("bench-press")),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theFirstSetAtARepCountIsARecord_andEverythingIsJudgedAgainstIt() = runTest {
        // Day 1: 100 x 5 — the first, so a record.
        session("s1", at = 0L, "back-squat" to listOf(set("s1-0", reps = 5, weight = 100_000L)))
        // Day 2: 100 x 5 again — matching a best does not beat it.
        session("s2", at = day, "back-squat" to listOf(set("s2-0", reps = 5, weight = 100_000L)))
        // Day 3: 105 x 5 — heavier, so a record.
        session("s3", at = 2 * day, "back-squat" to listOf(set("s3-0", reps = 5, weight = 105_000L)))
        // Day 4: 102.5 x 5 — heavier than the first, lighter than the best: not a record.
        session("s4", at = 3 * day, "back-squat" to listOf(set("s4-0", reps = 5, weight = 102_500L)))

        assertEquals(2, dao.countRecordsIn(from = 0L, to = 10 * day, recordableRoles = RECORDABLE))
    }

    @Test
    fun aRungIsNeverARecord_andNeverRaisesTheBarForOne() = runTest {
        // B155. The query excluded `'WARMUP'` alone while the domain rule is `SetType.recordsEffort`, which a
        // rung fails — so a drop or cluster set was counted as a record the live announce never mentioned, and,
        // on the other side of the NOT EXISTS, an earlier rung's weight blocked a later honest working set from
        // a count that *was* announced. A rung's stored weight really is positive (it is the anchor's load less
        // the run's value), which is why the query matched it at all.
        session(
            "s1",
            at = 0L,
            "back-squat" to listOf(
                set("s1-0", index = 0, reps = 5, weight = 80_000L),
                // A **heavier** rung than the anchor, which is what makes the two halves of the bug visible:
                // under the old predicate this 90 was a record the lifter was never told about *and* the bar
                // that stopped the next session's 85 from being one.
                set("s1-1", index = 1, reps = 5, weight = 90_000L, type = SetType.DROP),
            ),
        )
        // The honest set that beats the anchor but not the rung.
        session(
            "s2",
            at = day,
            "back-squat" to listOf(set("s2-0", reps = 5, weight = 85_000L)),
        )

        assertEquals(
            "the anchor, and the 85 the rung was blocking — never the rung itself",
            2,
            dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE),
        )
    }

    @Test
    fun aWarmUpIsNeverARecord_howeverHeavy() = runTest {
        // B17: a 120 kg warm-up is not a personal best, and it must not raise the bar for the working sets
        // either — so it is excluded on both sides of the comparison.
        session(
            "s1",
            at = 0L,
            "back-squat" to listOf(set("s1-0", reps = 5, weight = 120_000L, type = SetType.WARMUP)),
        )
        session("s2", at = day, "back-squat" to listOf(set("s2-0", reps = 5, weight = 100_000L)))

        assertEquals(
            "the working set is the first real one, so it is a record",
            1,
            dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE)
        )
    }

    @Test
    fun withinOneSession_theSetBeforeCounts_asEarlier() = runTest {
        // 100 x 5 then 100 x 5: the second matches the first, so one record, not two.
        session(
            "s1",
            at = 0L,
            "back-squat" to listOf(
                set("s1-0", index = 0, reps = 5, weight = 100_000L),
                set("s1-1", index = 1, reps = 5, weight = 100_000L),
            ),
        )
        // And a rising session is two records: 100 then 105.
        session(
            "s2",
            at = day,
            "back-squat" to listOf(
                set("s2-0", index = 0, reps = 5, weight = 100_000L),
                set("s2-1", index = 1, reps = 5, weight = 105_000L),
            ),
        )

        assertEquals(
            "s1-0 is not a record by then, s2-0 and s2-1 are",
            2,
            dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE)
        )
    }

    @Test
    fun aSetWithNoWeight_isNotCountedEitherWay() = runTest {
        // A bodyweight set has nothing to compare, which the domain rule answers with "no claim" rather
        // than with a record (PersonalRecords.isRecord).
        session("s1", at = 0L, "back-squat" to listOf(set("s1-0", reps = 8, weight = 0L)))

        assertEquals(0, dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE))
    }

    @Test
    fun theWindowBoundsTheCount_evenWhenTheSetWasARecord() = runTest {
        session("s1", at = 0L, "back-squat" to listOf(set("s1-0", reps = 5, weight = 100_000L)))
        session("s2", at = 5 * day, "back-squat" to listOf(set("s2-0", reps = 5, weight = 105_000L)))

        assertEquals(
            "the first is outside a window that starts at day five",
            1,
            dao.countRecordsIn(4 * day, 10 * day, recordableRoles = RECORDABLE)
        )
        assertEquals(
            "and both are inside a window that covers everything",
            2,
            dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE)
        )
        assertEquals(
            "the end is exclusive",
            0,
            dao.countRecordsIn(4 * day, 5 * day, recordableRoles = RECORDABLE),
        )
    }

    @Test
    fun twoExercisesAreJudgedSeparately() = runTest {
        // A bench press record says nothing about the squat: the comparison is per exercise.
        session("s1", at = 0L, "back-squat" to listOf(set("s1-0", reps = 5, weight = 100_000L)))
        session("s2", at = day, "bench-press" to listOf(set("s2-0", reps = 5, weight = 60_000L)))

        assertEquals(2, dao.countRecordsIn(0L, 10 * day, recordableRoles = RECORDABLE))
    }

    /** A finished session at [at] (epoch millis), holding each exercise's sets in order. */
    private suspend fun session(
        id: String,
        at: Long,
        vararg exercises: Pair<String, List<SetEntryEntity>>,
    ) {
        val dao = database.workoutDao()
        dao.insertSession(
            WorkoutSessionEntity(
                id = id,
                startedAt = at,
                finishedAt = at + 3_600_000L,
                notes = null,
                restEndsAt = null,
                readinessNote = null,
                createdAt = at,
                updatedAt = at,
                deletedAt = null,
            ),
        )
        exercises.forEachIndexed { position, (exerciseId, rows) ->
            val rowId = "$id-row-$position"
            dao.insertSessionExercise(
                SessionExerciseEntity(
                    id = rowId,
                    sessionId = id,
                    exerciseId = exerciseId,
                    position = position,
                    restSeconds = 120,
                    techniqueNote = null,
                    finishedAt = null,
                    supersetGroup = null,
                    createdAt = at,
                    updatedAt = at,
                    deletedAt = null,
                ),
            )
            rows.forEach { dao.insertSet(it.copy(sessionExerciseId = rowId)) }
        }
    }

    private fun set(
        id: String,
        index: Int = 0,
        reps: Int,
        weight: Long,
        type: SetType = SetType.NORMAL,
    ) = SetEntryEntity(
        // Replaced with the session's row id when the session is seeded.
        sessionExerciseId = "",
        id = id,
        setIndex = index,
        reps = reps,
        weightGrams = weight,
        assistanceGrams = 0L,
        setType = type,
        rpeHalves = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    @Test
    fun onlyTheLivePlansPrescribeATarget() = runTest {
        // N39: a plan's target is read from the plan's own tables, and three kinds of row there are **not** a
        // plan the lifter follows — a kept version (N134), a deleted plan, and a set taken out of the plan.
        // Only SQLite can be asked whether the query says so.
        val templates = database.templateDao()
        val templateSets = database.templateSetDao()
        templates.insertTemplate(template("t-live", "Push day"))
        templates.insertTemplate(template("t-version", "Push day (kept)", versionOf = "t-live"))
        templates.insertTemplate(template("t-deleted", "Gone", deletedAt = 5L))
        listOf("t-live", "t-version", "t-deleted").forEachIndexed { index, templateId ->
            templates.insertTemplateExercise(plannedExercise("e-$index", templateId))
            templateSets.insertTemplateSet(plannedSet("s-$index", "e-$index", weight = 100_000L))
        }
        // A heavier set the lifter took out of the live plan: it is not a prescription any more.
        templates.insertTemplateExercise(plannedExercise("e-removed", "t-live"))
        templateSets.insertTemplateSet(plannedSet("s-removed", "e-removed", weight = 140_000L, deletedAt = 7L))

        val targets = dao.observePlanTargets(listOf("back-squat")).first()

        assertEquals("one live plan, and only it", listOf("t-live"), targets.map { it.templateId })
        assertEquals("and not the set that was taken out", 100_000L, targets.single().weightGrams)
    }

    @Test
    fun planTargets_areReadForEveryLiftInTheList() = runTest {
        // B143: a metric that accepts a head draws its whole family, so its plan lines are read over the same
        // ids — a set never names a category, so the chosen id alone drew no line at all.
        val templates = database.templateDao()
        val templateSets = database.templateSetDao()
        templates.insertTemplate(template("t-live", "Push day"))
        templates.insertTemplateExercise(plannedExercise("e-bench", "t-live", exerciseId = "bench-press"))
        templateSets.insertTemplateSet(plannedSet("s-bench", "e-bench", weight = 100_000L))
        templates.insertTemplateExercise(plannedExercise("e-squat", "t-live", exerciseId = "back-squat"))
        templateSets.insertTemplateSet(plannedSet("s-squat", "e-squat", weight = 120_000L))

        val targets = dao.observePlanTargets(listOf("bench-press", "back-squat")).first()

        assertEquals(
            "both lifts in the list, and nothing outside it",
            setOf(100_000L, 120_000L),
            targets.mapNotNull { it.weightGrams }.toSet(),
        )
    }

    @Test
    fun aRepRange_withoutProgression_readsItsFloor_notItsTop() = runTest {
        // B142: the session asks for the range's floor where progression has not moved it (N74), and the
        // chart's plan line is one reading of the same stored row — a second answer would draw an estimated
        // 1RM the plan never asked for.
        val templates = database.templateDao()
        val templateSets = database.templateSetDao()
        templates.insertTemplate(template("t-live", "Push day"))
        templates.insertTemplateExercise(plannedExercise("e1", "t-live"))
        templateSets.insertTemplateSet(
            plannedSet("s1", "e1", weight = 100_000L, repsMin = 5, repsMax = 8, repsCurrent = null),
        )

        val target = dao.observePlanTargets(listOf("back-squat")).first().single()

        assertEquals(5, target.reps)
    }

    private fun template(
        id: String,
        name: String,
        versionOf: String? = null,
        deletedAt: Long? = null,
    ) = TemplateEntity(
        id = id,
        name = name,
        versionOf = versionOf,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )

    private fun plannedExercise(
        id: String,
        templateId: String,
        exerciseId: String = "back-squat",
    ) = TemplateExerciseEntity(
        id = id,
        templateId = templateId,
        exerciseId = exerciseId,
        position = 0,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    private fun plannedSet(
        id: String,
        templateExerciseId: String,
        weight: Long?,
        deletedAt: Long? = null,
        repsMin: Int? = 5,
        repsMax: Int? = 5,
        repsCurrent: Int? = 5,
    ) = TemplateSetEntity(
        id = id,
        templateExerciseId = templateExerciseId,
        setIndex = 0,
        role = SetType.NORMAL,
        targetWeightGrams = weight,
        targetAssistanceGrams = null,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetRepsCurrent = repsCurrent,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )
}
