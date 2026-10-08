package com.example.androidapp.data

import java.time.DayOfWeek
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.model.rungLoad
import com.example.androidapp.domain.model.rungWeightAt
import com.example.androidapp.domain.model.runAt
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.warmUpRamp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Template writes, end to end through the repository (ROADMAP N3).
 *
 * The order assertions are the point of the feature — a template that comes back in
 * the wrong order produces the wrong workout — and the edge cases are asserted as
 * *successes*, because a Move up button at the top of the list should do nothing,
 * not report a failure the user cannot act on.
 */
@RunWith(AndroidJUnit4::class)
class TemplateRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomTemplateRepository

    /**
     * The clock every write reads, movable so two removals are two moments.
     *
     * A soft delete stamps `deletedAt` with this value, and that stamp is what tells a restore which rows
     * went together (ROADMAP N90): a fixed clock would make every removal on a device one single run.
     */
    private var now = Instant.parse("2026-09-29T08:00:00Z")
    private val clock = TimeSource { now }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomTemplateRepository(database, clock)
        runTest {
            database.exerciseDao().insertAll(listOf(exercise("back-squat"), exercise("bench-press")))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aCreatedTemplate_comesBackTrimmed_andWithNoExercises() = runTest {
        val created = repository.createTemplate("  Push day  ") as DataResult.Success

        val template = repository.observeTemplate(created.data).first()
        assertEquals("Push day", template?.name)
        assertEquals(0, template?.exerciseCount)
    }

    @Test
    fun aBlankName_isRefused_andStoresNothing() = runTest {
        val failure = repository.createTemplate("   ") as DataResult.Failure

        assertTrue(failure.error is DataError.Invalid)
        assertTrue("a refused create must not leave a row", repository.observeTemplates().first().isEmpty())
    }

    @Test
    fun renaming_isTrimmed_andRefusesABlankName() = runTest {
        val id = create("Push day")

        assertTrue(repository.renameTemplate(id, "  Legs  ") is DataResult.Success)
        assertEquals("Legs", repository.observeTemplate(id).first()?.name)

        assertTrue(repository.renameTemplate(id, "  ") is DataResult.Failure)
        assertEquals("a refused rename must leave the name alone", "Legs", repository.observeTemplate(id).first()?.name)
    }

    @Test
    fun renaming_aGoneTemplate_isNotFound() = runTest {
        val failure = repository.renameTemplate("no-such-template", "Legs") as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun deleting_hidesTheTemplate_andASecondDeleteIsNotFound() = runTest {
        val id = create("Push day")

        assertTrue(repository.deleteTemplate(id) is DataResult.Success)
        assertNull(repository.observeTemplate(id).first())
        assertTrue(repository.observeTemplates().first().isEmpty())

        assertEquals(
            DataError.NotFound,
            (repository.deleteTemplate(id) as DataResult.Failure).error,
        )
    }

    @Test
    fun exercises_areAppendedInTheOrderTheyAreAdded() = runTest {
        val id = create("Push day")

        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")

        assertEquals(
            listOf("back-squat", "bench-press"),
            repository.observeExercises(id).first().map { it.exerciseId },
        )
        assertEquals(listOf(0, 1), repository.observeExercises(id).first().map { it.position })
        assertEquals(2, repository.observeTemplate(id).first()?.exerciseCount)
    }

    @Test
    fun addingToAGoneTemplate_isNotFound() = runTest {
        val failure = repository.addExercise("no-such-template", "back-squat") as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun removingAnExercise_takesItOutOfTheOrder() = runTest {
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")
        val first = repository.observeExercises(id).first().first()

        assertTrue(repository.removeExercise(first.id) is DataResult.Success)

        assertEquals(
            listOf("bench-press"),
            repository.observeExercises(id).first().map { it.exerciseId },
        )
    }

    @Test
    fun removingAnUnknownExercise_isNotFound() = runTest {
        val failure = repository.removeExercise("no-such-row") as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun movingDown_swapsWithTheNextExercise() = runTest {
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")
        val first = repository.observeExercises(id).first().first()

        assertTrue(repository.moveExercise(first.id, delta = 1) is DataResult.Success)

        assertEquals(
            listOf("bench-press", "back-squat"),
            repository.observeExercises(id).first().map { it.exerciseId },
        )
    }

    @Test
    fun movingUp_swapsWithThePreviousExercise() = runTest {
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")
        val last = repository.observeExercises(id).first().last()

        assertTrue(repository.moveExercise(last.id, delta = -1) is DataResult.Success)

        assertEquals(
            listOf("bench-press", "back-squat"),
            repository.observeExercises(id).first().map { it.exerciseId },
        )
    }

    @Test
    fun movingPastEitherEnd_isANoOp_success() = runTest {
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")
        val rows = repository.observeExercises(id).first()

        assertTrue(repository.moveExercise(rows.first().id, delta = -1) is DataResult.Success)
        assertTrue(repository.moveExercise(rows.last().id, delta = 1) is DataResult.Success)

        assertEquals(
            "the order must be untouched at the edges",
            listOf("back-squat", "bench-press"),
            repository.observeExercises(id).first().map { it.exerciseId },
        )
    }

    @Test
    fun movingAnUnknownExercise_isNotFound() = runTest {
        val failure = repository.moveExercise("no-such-row", delta = 1) as DataResult.Failure

        assertEquals(DataError.NotFound, failure.error)
    }

    @Test
    fun templates_areListedByName() = runTest {
        create("Push day")
        create("Legs")
        create("Arms")

        assertEquals(
            listOf("Arms", "Legs", "Push day"),
            repository.observeTemplates().first().map { it.name },
        )
    }

    private suspend fun create(name: String): String =
        (repository.createTemplate(name) as DataResult.Success).data

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id,
        primaryMuscle = MuscleGroup.QUADS,
        secondaryMuscles = emptyList(),
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.SQUAT,
        isCustom = false,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
    )

    @Test
    fun aPlannedSet_isAppended_andComesBackWithItsTargets() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)

        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.TOP_SET,
                targetWeightGrams = 140_000L,
                targetRepsMin = 1,
                targetRepsMax = 2,
                targetRpeHalves = 18,
                note = "grind",
            ),
        )

        val stored = repository.observeExercises(template).first().single().sets.single()
        assertEquals(0, stored.setIndex)
        assertEquals(SetType.TOP_SET, stored.role)
        assertEquals(140_000L, stored.targetWeightGrams)
        assertEquals(1, stored.targetRepsMin)
        assertEquals(2, stored.targetRepsMax)
        assertEquals(18, stored.targetRpeHalves)
        assertEquals("grind", stored.note)
    }

    @Test
    fun aPlanWithNoTargets_isValid_ratherThanZeroed() = runTest {
        // "Work up to a heavy single" has no weight to write down (ROADMAP N14).
        val template = create("Legs")
        val exercise = plannedExercise(template)

        val result = repository.addSet(exercise, TemplateSetEdit(role = SetType.WARMUP))

        assertTrue(result is DataResult.Success)
        val stored = repository.observeExercises(template).first().single().sets.single()
        assertNull(stored.targetWeightGrams)
        assertNull(stored.targetRepsMin)
        assertNull(stored.targetRpeHalves)
    }

    @Test
    fun aPlanThatDoesNotMakeSense_isRefused() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)

        val negative = repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = -1))
        val zeroReps = repository.addSet(exercise, TemplateSetEdit(targetRepsMin = 0))
        val backwards = repository.addSet(exercise, TemplateSetEdit(targetRepsMin = 8, targetRepsMax = 3))
        val offScaleRpe = repository.addSet(exercise, TemplateSetEdit(targetRpeHalves = 21))

        assertTrue("a negative weight", negative is DataResult.Failure)
        assertTrue("a zero-rep target", zeroReps is DataResult.Failure)
        assertTrue("a range that runs backwards", backwards is DataResult.Failure)
        assertTrue("an RPE off the scale", offScaleRpe is DataResult.Failure)
        assertTrue(
            "nothing is stored when the plan is refused",
            repository.observeExercises(template).first().single().sets.isEmpty(),
        )
    }

    @Test
    fun updatingAPlannedSet_keepsItsPlace_andCanClearATarget() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        val first = repository.observeExercises(template).first().single().sets.first()

        // A failure set rather than a drop: the first planned set has nothing above it to hang off,
        // so a drop there is refused by N79's rule and this test is about the update, not the group.
        repository.updateSet(first.id, TemplateSetEdit(role = SetType.FAILURE, targetRepsMax = 8))

        val sets = repository.observeExercises(template).first().single().sets
        assertEquals(2, sets.size)
        assertEquals("the order is untouched", listOf(0, 1), sets.map { it.setIndex })
        assertEquals(SetType.FAILURE, sets[0].role)
        assertEquals(8, sets[0].targetRepsMax)
        assertNull("a cleared target is null, not zero", sets[0].targetWeightGrams)
    }

    @Test
    fun removingAPlannedSet_leavesTheOthersAlone() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        val first = repository.observeExercises(template).first().single().sets.first()

        repository.removeSet(first.id)

        val sets = repository.observeExercises(template).first().single().sets
        assertEquals(1, sets.size)
        assertEquals(5, sets.single().targetRepsMax)
    }

    @Test
    fun aPlanForAnExerciseThatIsGone_isNotFound() = runTest {
        assertTrue(repository.addSet("nope", TemplateSetEdit()) is DataResult.Failure)
        assertTrue(repository.removeSet("nope") is DataResult.Failure)
        assertTrue(repository.updateSet("nope", TemplateSetEdit()) is DataResult.Failure)
    }

    @Test
    fun restoringARemovedSet_putsItBackWhereItWas() = runTest {
        // ROADMAP N90: the workout's undo appends, and that is the right answer for a log — "the values
        // come back, the position may not". A plan is the other way round: its order *is* the plan, so a
        // set removed from the middle comes back in the middle, with the survivors shifted down.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 8))
        val middle = repository.observeExercises(template).first().single().sets[1]

        repository.removeSet(middle.id)
        now = now.plusSeconds(5)
        repository.restoreSet(middle.id)

        val back = repository.observeExercises(template).first().single().sets
        assertEquals("the set is where it was", listOf(3, 5, 8), back.map { it.targetRepsMax })
        assertEquals("and the index space is contiguous again", listOf(0, 1, 2), back.map { it.setIndex })
    }

    @Test
    fun restoringARunsAnchor_putsTheWholeRunBackInOrder() = runTest {
        // N90 with N79: a rung's load is read by position (`runAt`, `rungWeightAt`), so a restore that
        // appended would silently rewrite the ladder. The whole run comes back together, because the
        // rungs are not separate sets — one write hid them, and the one timestamp says so.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 60_000L))
        val anchor = repository.observeExercises(template).first().single().sets.first()

        repository.removeSet(anchor.id)
        assertEquals(
            "the run went with the anchor",
            listOf(60_000L),
            repository.observeExercises(template).first().single().sets.map { it.targetWeightGrams },
        )

        now = now.plusSeconds(5)
        repository.restoreSet(anchor.id)

        val back = repository.observeExercises(template).first().single().sets
        assertEquals(
            "the run is back above the set that followed it",
            listOf(SetType.NORMAL, SetType.DROP, SetType.DROP, SetType.NORMAL),
            back.map { it.role },
        )
        assertEquals(listOf(0, 1, 2, 3), back.map { it.setIndex })
        assertEquals(
            "and the ladder derives what it did before",
            listOf(100_000L, 80_000L, 60_000L),
            back.indices.take(3).map { back.rungWeightAt(it) },
        )
    }

    @Test
    fun restoringAfterEarlierRemovals_doesNotResurrectThem() = runTest {
        // `deletedAt` is the identity of the one write that hid a run, so an undo reaches its own
        // removal and nothing else — the alternative, restoring every hidden row, would put back a set
        // the lifter deleted deliberately a minute earlier.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 8))
        val sets = repository.observeExercises(template).first().single().sets

        repository.removeSet(sets[0].id)
        now = now.plusSeconds(5)
        repository.removeSet(sets[2].id)

        repository.restoreSet(sets[2].id)

        assertEquals(
            "only the set this undo names comes back",
            listOf(5, 8),
            repository.observeExercises(template).first().single().sets.map { it.targetRepsMax },
        )
    }

    @Test
    fun restoringASetThatWasNeverRemoved_isRefused() = runTest {
        // The live row is not a removal to take back, and the refusal is the repository's rather than the
        // screen's: N90's undo is offered for one set at a time, and a stale tap has to land nowhere.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        val live = repository.observeExercises(template).first().single().sets.single()

        assertTrue(repository.restoreSet(live.id) is DataResult.Failure)
        assertTrue(repository.restoreSet("nope") is DataResult.Failure)
    }

    @Test
    fun aPlansEffortRestAndCue_reachTheExercise_andBlankBecomesUnset() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)

        repository.setExercisePlan(
            exercise,
            restSeconds = 180,
            techniqueNote = "  Slow descent  ",
            targetRpeHalves = 16,
        )
        val written = repository.observeExercises(template).first().single()
        assertEquals(180, written.restSeconds)
        assertEquals("Slow descent", written.techniqueNote)
        assertEquals("the exercise's one target RPE (N59, amended)", 16, written.targetRpeHalves)

        // Blank is "use the library's" (N5); zero is a value — no rest — and only a negative is
        // refused (N45).
        repository.setExercisePlan(
            exercise,
            restSeconds = null,
            techniqueNote = "   ",
            targetRpeHalves = null,
        )
        val cleared = repository.observeExercises(template).first().single()
        assertNull(cleared.restSeconds)
        assertNull(cleared.techniqueNote)
        assertNull("no effort named is no effort", cleared.targetRpeHalves)

        assertTrue(
            repository.setExercisePlan(
                exercise,
                restSeconds = 0,
                techniqueNote = null,
                targetRpeHalves = null,
            ) is DataResult.Success,
        )
        assertEquals(0, repository.observeExercises(template).first().single().restSeconds)

        assertTrue(
            repository.setExercisePlan(
                exercise,
                restSeconds = -5,
                techniqueNote = null,
                targetRpeHalves = null,
            ) is DataResult.Failure,
        )
        assertTrue(
            "an RPE off the scale is refused like a set's",
            repository.setExercisePlan(
                exercise,
                restSeconds = null,
                techniqueNote = null,
                targetRpeHalves = 21,
            ) is DataResult.Failure,
        )
    }

    /** A template with one exercise added, for the plan tests to write sets against. */
    private suspend fun plannedExercise(templateId: String): String {
        repository.addExercise(templateId, "back-squat")
        return repository.observeExercises(templateId).first().single().id
    }

    @Test
    fun clearingTheExercisesEffort_clearsTheLegacyPerSetValues_too() = runTest {
        // N59: the per-set column is the fallback the reader uses when the exercise names no effort, so
        // a clear that left it behind would resurrect the number the lifter just removed. A plan that
        // arrived with per-set values — a migrated one — is exactly this case.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.setExercisePlan(exercise, restSeconds = null, techniqueNote = null, targetRpeHalves = 16)
        repository.addSet(
            exercise,
            TemplateSetEdit(targetWeightGrams = 100_000L, targetRepsMax = 5, targetRpeHalves = 8),
        )

        repository.setExercisePlan(exercise, restSeconds = null, techniqueNote = null, targetRpeHalves = null)

        val set = repository.observeExercises(template).first().single().sets.single()
        assertNull("the set's legacy value goes with the exercise's", set.targetRpeHalves)
    }

    @Test
    fun savingNoEffortOverAPlanThatNeverNamedOne_keepsTheSetsOwn() = runTest {
        // A plan that came from a backup written before the effort moved to the exercise names none at
        // the exercise level; its sets' values are the only target it has, and an edit that changes the
        // rest must not erase them (N59).
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(targetWeightGrams = 100_000L, targetRepsMax = 5, targetRpeHalves = 8),
        )

        repository.setExercisePlan(exercise, restSeconds = 180, techniqueNote = null, targetRpeHalves = null)

        val set = repository.observeExercises(template).first().single().sets.single()
        assertEquals("the per-set value survives a save that names no effort", 8, set.targetRpeHalves)
    }



    @Test
    fun aWarmUpRamp_writtenToAPlan_comesBackWithItsRolesAndWeights() = runTest {
        // ROADMAP N28, and the point B24 made about reads and writes that only ever meet a fake: the
        // generator's output is asserted as *values passed* everywhere else. This is the same ramp
        // written through the schema and read back, so a column that dropped the role — or a
        // projection that forgot it — fails here rather than at the gym.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
        )

        warmUpRamp(workingWeightGrams = 100_000L).forEach { target ->
            repository.addSet(
                exercise,
                TemplateSetEdit(
                    role = SetType.WARMUP,
                    targetWeightGrams = target.weightGrams,
                    targetRepsMin = target.reps,
                    targetRepsMax = target.reps,
                ),
            )
        }

        val planned = repository.observeExercises(template).first().single().sets
        val warmUps = planned.filter { it.role == SetType.WARMUP }

        assertEquals(
            "the ramp survives the schema, lightest first",
            listOf(40_000L, 60_000L, 75_000L, 85_000L),
            warmUps.map { it.targetWeightGrams },
        )
        assertEquals(
            "and so do its reps",
            listOf(5, 3, 2, 1),
            warmUps.map { it.targetRepsMax },
        )
        assertEquals(
            "the working set is still the only working set",
            1,
            planned.count { it.role != SetType.WARMUP },
        )
        assertEquals("the plan reads as four warm-ups and the work", 5, planned.size)
    }

    @Test
    fun aWarmUpRamp_isWrittenInFrontOfTheWorkingSets() = runTest {
        // ROADMAP B34: the generator's arithmetic was right and it wrote to the wrong end. The plan
        // read working set first and then the warm-ups that exist to prepare for it — and the earlier
        // test could not see it, because it only checked the warm-ups' order among themselves.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.NORMAL, targetWeightGrams = 100_000L, targetRepsMax = 5),
        )

        repository.prependSets(
            exercise,
            (1..3).map { step ->
                TemplateSetEdit(
                    role = SetType.WARMUP,
                    targetWeightGrams = step * 20_000L,
                    targetRepsMax = 5,
                )
            },
        )

        val planned = repository.observeExercises(template).first().single().sets
        assertEquals(
            "the ramp comes first, in the order it was written",
            listOf(20_000L, 40_000L, 60_000L),
            planned.dropLast(1).map { it.targetWeightGrams },
        )
        assertEquals(
            "and the work is still last, unchanged",
            100_000L,
            planned.last().targetWeightGrams,
        )
        assertEquals(SetType.NORMAL, planned.last().role)
    }

    @Test
    fun aDropRun_storesItsValueOnce_andTheLadderReadsFromIt() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L, targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))

        val sets = repository.observeExercises(template).first().single().sets

        assertEquals("the value is held by the run's first rung", 20_000L, sets[1].dropValueGrams)
        assertNull("and a later rung carries none, inheriting it", sets[2].dropValueGrams)
        assertEquals(20_000L, sets.runAt(1)?.dropValueGrams)
        assertEquals(2, sets.runAt(2)?.rung)
        assertEquals(
            "so 100 with a 20 kg value is 80, then 60",
            60_000L,
            rungLoad(Load(100_000L, 0L), SetType.DROP, sets.runAt(2)!!)?.weightGrams,
        )
    }

    @Test
    fun aRungWithNoWorkingSetAboveIt_isRefused() = runTest {
        // The first set of a plan has nothing to hang off, so it cannot be a rung (ROADMAP N79).
        val template = create("Legs")
        val exercise = plannedExercise(template)

        val result = repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L),
        )

        assertTrue(result is DataResult.Failure)
        assertTrue(repository.observeExercises(template).first().single().sets.isEmpty())
    }

    @Test
    fun aDropWithNoValue_isRefused() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))

        val result = repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))

        assertTrue("a drop is the anchor less a value, and there is none", result is DataResult.Failure)
    }

    @Test
    fun aLadderThatRunsPastZero_isRefusedAtTheRungThatWould() = runTest {
        // 100 with a 40 kg value: 60 and 20 are rungs, and the third would be −20 — which in this app
        // is not a small weight but 20 kg of help, so it is refused rather than stored (N15, N79).
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 40_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))

        val third = repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))

        assertTrue(third is DataResult.Failure)
        assertEquals(3, repository.observeExercises(template).first().single().sets.size)
    }

    @Test
    fun aValueBelongsToADrop_andNothingElse() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))

        val onAWorkingSet = repository.addSet(exercise, TemplateSetEdit(dropValueGrams = 20_000L))
        val onACluster = repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.CLUSTER, dropValueGrams = 20_000L),
        )

        assertTrue("a working set takes nothing off the set above it", onAWorkingSet is DataResult.Failure)
        assertTrue("and a cluster repeats it rather than dropping it", onACluster is DataResult.Failure)
    }

    @Test
    fun removingAMiddleSet_keepsTheSurvivorsPositionsContiguous() = runTest {
        // ROADMAP B72: a plan is matched to a running session by position — `setIndex` against the
        // number of sets logged, and a run's continuation against the plan's own index — so a removal
        // that left a gap pointed the prefill and the rest rule at the wrong row. The survivors are
        // renumbered, which is the one index space; `prependSets` holds the same invariant the other way.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 8))
        val middle = repository.observeExercises(template).first().single().sets[1]

        repository.removeSet(middle.id)

        val left = repository.observeExercises(template).first().single().sets
        assertEquals("the survivors hold the positions they now occupy", listOf(0, 1), left.map { it.setIndex })
        assertEquals("and they are the sets that were there, in order", listOf(3, 8), left.map { it.targetRepsMax })

        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 10))

        assertEquals(
            "and a new set appends rather than filling a gap",
            listOf(0, 1, 2),
            repository.observeExercises(template).first().single().sets.map { it.setIndex },
        )
    }

    @Test
    fun anExercisesOwnStep_andAClimb_reachAPlanThroughTheProjections() = runTest {
        // ROADMAP B70: both columns travel through hand-written SQL projections — the exercise's step
        // onto the planned exercise, the climb onto the planned set — and no test read either back
        // through a DAO, so a projection that dropped one stayed green.
        database.exerciseDao().insertAll(listOf(exercise("front-squat").copy(stepGrams = 5_000L)))
        val template = create("Legs")
        repository.addExercise(template, "front-squat")
        val exercise = repository.observeExercises(template).first().single().id
        repository.addSet(
            exercise,
            TemplateSetEdit(targetRepsMin = 5, targetRepsMax = 8, targetRepsCurrent = 7),
        )

        val planned = repository.observeExercises(template).first().single()

        assertEquals(5_000L, planned.stepGrams)
        assertEquals(7, planned.sets.single().targetRepsCurrent)
    }

    @Test
    fun aDropValueThatIsNotAboveZero_isRefused() = runTest {
        // ROADMAP B69: the value's rule has two halves, and only the role half was asserted — the
        // boundary's "more than zero" and the dialog's matching guard were never reached by a test.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))

        val zero = repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 0L))
        val negative = repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.DROP, dropValueGrams = -20_000L),
        )

        assertTrue("zero is no value rather than a small one", zero is DataResult.Failure)
        assertTrue("and a negative value would be assistance, not a drop", negative is DataResult.Failure)
        assertEquals(1, repository.observeExercises(template).first().single().sets.size)
    }

    @Test
    fun aLaterRungCarryingItsOwnValue_isRefused() = runTest {
        // ROADMAP B69: "a run names its value on its first rung" was asserted only from the reading
        // side — the second rung inherits and carries none — so the branch refusing a second value
        // never ran. The value belongs to the run, and a later row's would be a number nothing reads.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))

        val second = repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.DROP, dropValueGrams = 30_000L),
        )

        assertTrue(second is DataResult.Failure)
        assertEquals(
            "and the plan keeps the run it had",
            2,
            repository.observeExercises(template).first().single().sets.size,
        )
    }

    @Test
    fun aClusterRung_needsNoValue_atAll() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))

        val result = repository.addSet(exercise, TemplateSetEdit(role = SetType.CLUSTER))

        assertTrue(result is DataResult.Success)
        val sets = repository.observeExercises(template).first().single().sets
        assertEquals(SetType.CLUSTER, sets.last().role)
        assertNull(sets.last().dropValueGrams)
    }

    @Test
    fun aDropUnderAnAnchorWithNoAddedWeight_isRefused() = runTest {
        // An assisted anchor has no 20 kg to take off — the same absence the progression rule refuses
        // to step, and the reason a drop is not offered there (ROADMAP N15, N79).
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(targetWeightGrams = 0L, targetAssistanceGrams = 20_000L),
        )

        val result = repository.addSet(
            exercise,
            TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L),
        )

        assertTrue(result is DataResult.Failure)
    }

    /**
     * The shape migration 34→35 leaves on disk: a drop run from before the value existed.
     *
     * The migration is the only thing that can produce it, so it is written the way the migration
     * leaves it — a raw update, not a repository call the rules would have refused.
     */
    private fun clearRunValues() {
        database.openHelper.writableDatabase.execSQL(
            "UPDATE template_sets SET dropValueGrams = NULL WHERE role = 'DROP'",
        )
    }

    @Test
    fun aPlanWrittenBeforeTheRunRules_isStillEditable() = runTest {
        // ROADMAP B59: a drop run's value column arrived null, so a plan authored before the rung rules
        // is stored in a shape they refuse. Reading it is fine — the load falls back to what the set
        // carries — and so is every write that does not make it worse, or the lifter could not correct
        // the plan at all.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        clearRunValues()
        val anchor = repository.observeExercises(template).first().single().sets.first()

        val added = repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 90_000L))
        val edited = repository.updateSet(anchor.id, TemplateSetEdit(targetWeightGrams = 105_000L))

        assertTrue("a legacy run must not freeze the plan", added is DataResult.Success)
        assertTrue("nor refuse an edit beside it", edited is DataResult.Success)
        assertEquals(3, repository.observeExercises(template).first().single().sets.size)
    }

    @Test
    fun aRowTheWriteLeavesAsWrongAsItWas_isNotWhatRefusesIt() = runTest {
        // The other half of B59: the same legacy row still refuses a change that makes it *different*,
        // so grandfathering is not a hole. Giving the run's first rung a value it never had is a new
        // state, and it is judged as one.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        clearRunValues()
        val drop = repository.observeExercises(template).first().single().sets[1]

        val result = repository.updateSet(
            drop.id,
            TemplateSetEdit(role = SetType.DROP, targetWeightGrams = 100_000L, dropValueGrams = -10_000L),
        )

        assertTrue("a value is still a positive number", result is DataResult.Failure)
    }

    @Test
    fun editingAnAnchorLightEnoughToStrandARungBelowIt_isRefused() = runTest {
        // ROADMAP B59: the write is judged on what it creates, including on a row it did not touch.
        // 100 kg with a 60 kg value leaves 40 for the rung; an anchor edited to 50 leaves −10, which is
        // assistance rather than a small weight (N15), so the edit is refused rather than stored.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 60_000L))
        val anchor = repository.observeExercises(template).first().single().sets.first()

        val result = repository.updateSet(anchor.id, TemplateSetEdit(targetWeightGrams = 50_000L))

        assertTrue("a rung cannot be left with a negative load", result is DataResult.Failure)
        assertEquals(
            "and the anchor keeps what it had",
            100_000L,
            repository.observeExercises(template).first().single().sets.first().targetWeightGrams,
        )
    }

    @Test
    fun deletingARunsAnchor_takesItsRungsWithIt() = runTest {
        // ROADMAP B60: a rung carries no targets of its own, so a run left standing over a gap can be
        // neither read nor edited. The whole run goes with the anchor.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))
        val anchor = repository.observeExercises(template).first().single().sets.first()

        repository.removeSet(anchor.id)

        assertTrue(repository.observeExercises(template).first().single().sets.isEmpty())
    }

    @Test
    fun deletingALaterRung_leavesTheRunAndItsAnchorAlone() = runTest {
        // The anchor is not the run: removing its last rung shortens the ladder, and removing the
        // first rung leaves the run anchored exactly where it was.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP))
        val sets = repository.observeExercises(template).first().single().sets

        repository.removeSet(sets[2].id)
        val shortened = repository.observeExercises(template).first().single().sets

        assertEquals(2, shortened.size)
        assertEquals(listOf(SetType.NORMAL, SetType.DROP), shortened.map { it.role })

        repository.removeSet(shortened[1].id)

        assertEquals(
            "the anchor stands on its own",
            listOf(SetType.NORMAL),
            repository.observeExercises(template).first().single().sets.map { it.role },
        )
    }
}
