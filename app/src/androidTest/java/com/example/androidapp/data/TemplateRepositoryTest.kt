package com.example.androidapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.CalibrationTarget
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MAX_REPS
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ReplacementSeed
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.runAt
import com.example.androidapp.domain.model.rungLoad
import com.example.androidapp.domain.model.rungWeightAt
import com.example.androidapp.domain.model.warmUpRamp
import com.example.androidapp.domain.repository.TemplateSetEdit
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

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
    fun aDeletedTemplate_isListedApart_andRestoringBringsItsPlanBack() = runTest {
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.deleteTemplate(id)

        val deleted = repository.observeDeletedTemplates().first()
        assertEquals(
            "the deleted group holds it, and only it (ROADMAP N216)",
            listOf(id),
            deleted.map { it.id },
        )
        assertEquals("with the plan a delete never touched", 1, deleted.single().exerciseCount)
        assertTrue("while the live list does not", repository.observeTemplates().first().isEmpty())

        assertTrue(repository.restoreTemplate(id) is DataResult.Success)

        assertTrue("nothing is deleted any more", repository.observeDeletedTemplates().first().isEmpty())
        assertEquals(
            "and the workout is live again",
            listOf(id),
            repository.observeTemplates().first().map { it.id },
        )
        assertEquals(1, repository.observeTemplate(id).first()?.exerciseCount)
    }

    @Test
    fun restoring_aWorkoutDeletedWhileHidden_comesBackHidden() = runTest {
        val id = create("Push day")
        repository.setTemplateHidden(id, true)
        repository.deleteTemplate(id)

        repository.restoreTemplate(id)

        assertTrue(
            "the restore writes the delete flag alone, so the lifter's view of the row survives",
            repository.observeTemplates().first().isEmpty(),
        )
        assertEquals(
            "and it is back in the group that shows hidden ones",
            listOf(id),
            repository.observeTemplates(includeHidden = true).first().map { it.id },
        )
    }

    @Test
    fun restoringAWorkoutThatIsNotDeleted_isNotFound() = runTest {
        val id = create("Push day")

        assertEquals(
            DataError.NotFound,
            (repository.restoreTemplate(id) as DataResult.Failure).error,
        )
        assertEquals(
            DataError.NotFound,
            (repository.restoreTemplate("missing") as DataResult.Failure).error,
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
    fun twoMovesInOneFrame_leaveEveryPositionDistinct() = runTest {
        // B468: the read and the swap are one transaction now, so two moves cannot both act on the
        // same order and leave a shared position.
        database.exerciseDao().insertAll(listOf(exercise("deadlift")))
        val id = create("Push day")
        repository.addExercise(id, "back-squat")
        repository.addExercise(id, "bench-press")
        repository.addExercise(id, "deadlift")
        val rows = repository.observeExercises(id).first()

        val first = async { repository.moveExercise(rows[0].id, delta = 1) }
        val second = async { repository.moveExercise(rows[1].id, delta = 1) }
        assertTrue(first.await() is DataResult.Success)
        assertTrue(second.await() is DataResult.Success)

        val after = repository.observeExercises(id).first()
        assertEquals("no row was lost", 3, after.size)
        assertEquals("and no two share a place", 3, after.map { it.position }.distinct().size)
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

    @Test
    fun hiding_takesTheWorkoutOutOfTheOffers_andKeepsItResolvable() = runTest {
        val id = create("Push day")

        assertTrue(repository.setTemplateHidden(id, true) is DataResult.Success)

        assertTrue(
            "a picker no longer offers it",
            repository.observeTemplates().first().isEmpty(),
        )
        assertEquals(
            "the list that shows hidden ones still has it",
            listOf(id),
            repository.observeTemplates(includeHidden = true).first().map { it.id },
        )
        assertNotNull(
            "and it still resolves by id, so a program slot that names it still runs it",
            repository.observeTemplate(id).first(),
        )

        assertTrue(repository.setTemplateHidden(id, false) is DataResult.Success)
        assertEquals(
            "unhiding offers it again",
            listOf(id),
            repository.observeTemplates().first().map { it.id },
        )
    }

    @Test
    fun hiding_doesNotMoveTheWriteClock_aProgramEditReads() = runTest {
        val id = create("Push day")
        val written = database.templateDao().findById(id)?.updatedAt

        now = now.plusSeconds(60)
        repository.setTemplateHidden(id, true)

        assertEquals(
            "hiding changes no plan, so a program that carries it is not stale (N171, N214)",
            written,
            database.templateDao().findById(id)?.updatedAt,
        )
        assertNotNull("while the flag itself is written", database.templateDao().findById(id)?.hiddenAt)
    }

    @Test
    fun hidingAWorkoutThatIsGone_isRefused() = runTest {
        val failure = repository.setTemplateHidden("missing", true) as DataResult.Failure

        assertTrue(failure.error is DataError.NotFound)
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
                note = "grind",
            ),
        )

        val stored = repository.observeExercises(template).first().single().sets.single()
        assertEquals(0, stored.setIndex)
        assertEquals(SetType.TOP_SET, stored.role)
        assertEquals(140_000L, stored.targetWeightGrams)
        assertEquals(1, stored.targetRepsMin)
        assertEquals(2, stored.targetRepsMax)
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
    }

    @Test
    fun twoAddsInFlight_landOnDifferentPositions() = runTest {
        // B449: the position is read and used in one transaction, so two overlapping appends cannot both
        // claim `maxSetIndex + 1` and leave two rows in one place.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        val edit = TemplateSetEdit(
            role = SetType.NORMAL,
            targetWeightGrams = 100_000L,
            targetRepsMin = 5,
            targetRepsMax = 5,
        )

        val first = async { repository.addSet(exercise, edit) }
        val second = async { repository.addSet(exercise, edit) }

        assertTrue(first.await() is DataResult.Success)
        assertTrue(second.await() is DataResult.Success)
        val sets = repository.observeSets(template).first()
        assertEquals(2, sets.size)
        assertEquals("each appended row has its own place", listOf(0, 1), sets.map { it.setIndex })
    }

    @Test
    fun aPlanThatDoesNotMakeSense_isRefused() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)

        val negative = repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = -1))
        val zeroReps = repository.addSet(exercise, TemplateSetEdit(targetRepsMin = 0))
        val backwards = repository.addSet(exercise, TemplateSetEdit(targetRepsMin = 8, targetRepsMax = 3))
        val tooManyReps = repository.addSet(exercise, TemplateSetEdit(targetRepsMax = MAX_REPS + 1))
        val tooLongHold = repository.addSet(exercise, TemplateSetEdit(targetDurationSeconds = 86_401))

        assertTrue("a negative weight", negative is DataResult.Failure)
        assertTrue("a zero-rep target", zeroReps is DataResult.Failure)
        assertTrue("a range that runs backwards", backwards is DataResult.Failure)
        assertTrue("a target above the rep ceiling", tooManyReps is DataResult.Failure)
        assertTrue("a hold above a day", tooLongHold is DataResult.Failure)
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
    fun removingARunsOpener_takesOnlyThatRun_notASecondRunOfTheSameRole() = runTest {
        // B339: a plan may hold two drop ladders separated by a normal set. Removing the first opener
        // must take its own contiguous run, not every later row that happens to share the role.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 100_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 20_000L))
        repository.addSet(exercise, TemplateSetEdit(targetWeightGrams = 60_000L))
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 10_000L))
        val firstDrop = repository.observeExercises(template).first().single().sets[1]

        repository.removeSet(firstDrop.id)

        val left = repository.observeExercises(template).first().single().sets
        assertEquals(
            "only the first ladder goes; the second keeps its opener",
            listOf(SetType.NORMAL, SetType.NORMAL, SetType.DROP),
            left.map { it.role },
        )
        assertEquals("the survivors renumber", listOf(0, 1, 2), left.map { it.setIndex })
        assertEquals("and the second ladder still carries its value", 10_000L, left.last().dropValueGrams)
    }

    @Test
    fun restoringAfterARampWasAdded_putsTheSetAfterItsAnchor() = runTest {
        // B364: the survivors renumber, and a ramp added above moves every index again. The anchor's
        // identity is what puts the undo back after the set it originally followed.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 3))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 5))
        repository.addSet(exercise, TemplateSetEdit(targetRepsMax = 8))
        val middle = repository.observeExercises(template).first().single().sets[1]

        repository.removeSet(middle.id)
        repository.prependSets(exercise, listOf(TemplateSetEdit(role = SetType.WARMUP, targetRepsMax = 5)))
        now = now.plusSeconds(5)
        repository.restoreSet(middle.id)

        assertEquals(
            "the removed set returns after its original anchor, not after the new ramp",
            listOf(5, 3, 5, 8),
            repository.observeExercises(template).first().single().sets.map { it.targetRepsMax },
        )
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
        // The anchor reads its own stored load — `rungWeightAt` answers only for a rung — and the two
        // rungs still derive 80 and 60 off it, which is the ladder the restore had to keep intact.
        assertEquals("the anchor keeps its load", 100_000L, back.first().targetWeightGrams)
        assertEquals(
            "and the ladder derives what it did before",
            listOf(80_000L, 60_000L),
            listOf(1, 2).map { back.rungWeightAt(it) },
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
            "a rest above an hour is refused like a negative one (B374)",
            repository.setExercisePlan(
                exercise,
                restSeconds = 3_601,
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
    @Test
    fun thePerLiftDeload_trimsAndTheUndoRestoresTheWholeEdit() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repeat(5) {
            repository.addSet(
                exercise,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 8,
                ),
            )
        }
        val before = repository.observeSets(template).first()

        val plan = (repository.deloadExercise(exercise, 2_500L) as DataResult.Success).data

        assertEquals(before.drop(3).map { it.id }, plan.removedSetIds)
        val after = repository.observeSets(template).first()
        assertEquals(3, after.size)
        assertEquals(92_500L, after.first().targetWeightGrams)
        assertEquals(5, after.first().targetRepsCurrent)

        // The undo, as the editor performs it: every removed block and every kept target in one write
        // (B432, B451).
        val keptBefore = before.filter { set -> plan.kept.any { it.setId == set.id } }
        assertTrue(repository.undoDeload(plan.removedSetIds, keptBefore) is DataResult.Success)
        val undone = repository.observeSets(template).first()
        assertEquals(5, undone.size)
        assertEquals(100_000L, undone.first().targetWeightGrams)
    }

    @Test
    fun theUndoOfATwoBlockDeload_putsThePlanBackInOrder() = runTest {
        // B451: the trim removes the later working sets, and a warm-up can sit between two of them. The
        // blocks share one write, but the undo must put each back after its own anchor, not both after the
        // first one.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        val work = TemplateSetEdit(
            role = SetType.NORMAL,
            targetWeightGrams = 100_000L,
            targetRepsMin = 5,
            targetRepsMax = 5,
        )
        repository.addSet(exercise, work)
        repository.addSet(exercise, work)
        repository.addSet(exercise, work)
        repository.addSet(exercise, work)
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.WARMUP,
                targetWeightGrams = 60_000L,
                targetRepsMax = 5,
            ),
        )
        repository.addSet(exercise, work)
        val before = repository.observeSets(template).first()
        val keptBefore = before.filter { it.role.recordsEffort }.take(3)

        val plan = (repository.deloadExercise(exercise, 2_500L) as DataResult.Success).data
        assertEquals(2, plan.removedSetIds.size)

        assertTrue(repository.undoDeload(plan.removedSetIds, keptBefore) is DataResult.Success)

        val after = repository.observeSets(template).first()
        assertEquals("every set is back, in the order it was written", before.map { it.id }, after.map { it.id })
        assertEquals(before.map { it.setIndex }, after.map { it.setIndex })
    }

    @Test
    fun theReplace_writesANewRowWithNewSetIds_andTheUndoRestoresTheOld() = runTest {
        val template = create("Legs")
        val old = plannedExercise(template)
        repeat(5) {
            repository.addSet(
                old,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 8,
                ),
            )
        }

        val outcome = (
            repository.replaceExercise(
                old,
                "bench-press",
                listOf(ReplacementSeed(80_000L, 0L, 6, null)),
                empty = false,
            ) as DataResult.Success
            ).data

        val replaced = repository.observeExercises(template).first().single()
        assertEquals(outcome.newTemplateExerciseId, replaced.id)
        assertEquals("bench-press", replaced.exerciseId)
        // Five work sets trim to three; the one seed went to the first place, the others are cleared.
        assertEquals(3, replaced.sets.size)
        assertEquals(80_000L, replaced.sets.first().targetWeightGrams)
        assertEquals(6, replaced.sets.first().targetRepsMin)
        assertNull(replaced.sets[1].targetWeightGrams)
        assertEquals("a new row has new set ids", 3, replaced.sets.map { it.id }.distinct().size)

        assertTrue(repository.undoReplaceExercise(outcome) is DataResult.Success)

        val restored = repository.observeExercises(template).first().single()
        assertEquals(old, restored.id)
        assertEquals(5, restored.sets.size)
    }

    @Test
    fun aSeededReplaceLighterThanTheLadder_keepsTheStrandedRung() = runTest {
        // B434: the new movement's last work can be far lighter than the old plan's drop value. The
        // replace is a deliberate edit; the stranded rung is stored for the editor to show.
        val template = create("Legs")
        val old = plannedExercise(template)
        repository.addSet(
            old,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
            ),
        )
        repository.addSet(old, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 10_000L))

        val outcome = (
            repository.replaceExercise(
                old,
                "bench-press",
                listOf(ReplacementSeed(5_000L, 0L, 6, null)),
                empty = false,
            ) as DataResult.Success
            ).data

        val replaced = repository.observeExercises(template).first().single()
        assertEquals(outcome.newTemplateExerciseId, replaced.id)
        assertEquals("the seed is the new movement's load", 5_000L, replaced.sets.first().targetWeightGrams)
        assertEquals(
            "the ladder is written as the edit asks, even stranded",
            10_000L,
            replaced.sets.last().dropValueGrams,
        )
    }

    @Test
    fun theCalibrationMarker_makesThePlanWeightless_andTheUseAdoptsAndClearsIt() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
            ),
        )

        assertTrue(
            "the plan-editor switch is the same prescription",
            repository.setExercisePlan(
                exercise,
                restSeconds = null,
                techniqueNote = null,
                targetRpeHalves = null,
                findWeight = true,
            ) is DataResult.Success,
        )

        val weightless = repository.observeExercises(template).first().single()
        assertTrue("the plan is finding its weight", weightless.calibrationPending)
        assertNull("the load is cleared", weightless.sets.single().targetWeightGrams)
        assertEquals("the reps stay", 5, weightless.sets.single().targetRepsMin)

        val target = CalibrationTarget(weightless.sets.single().id, 90_000L, 0L)
        assertTrue(repository.calibrateExercise(exercise, listOf(target)) is DataResult.Success)

        val used = repository.observeExercises(template).first().single()
        assertEquals("the number was found", 90_000L, used.sets.single().targetWeightGrams)
        assertNull("no assistance is stored as none, not zero", used.sets.single().targetAssistanceGrams)
        assertFalse("the prompt is one-shot", used.calibrationPending)

        assertTrue(repository.setCalibration(exercise, true) is DataResult.Success)
        assertTrue(repository.observeExercises(template).first().single().calibrationPending)
    }

    @Test
    fun aSecondFindTheWeightSave_keepsAWeightEnteredAfterTheSwitch() = runTest {
        // B426: the switch is a state, not a command. Re-saving the same plan must not wipe a weight
        // entered after the marker was set — the real-store case the ViewModel test's fake cannot prove.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
            ),
        )
        assertTrue(
            repository.setExercisePlan(exercise, null, null, null, findWeight = true) is DataResult.Success,
        )
        val setId = repository.observeExercises(template).first().single().sets.single().id
        assertTrue(
            repository.updateSet(
                setId,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 120_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 8,
                ),
            ) is DataResult.Success,
        )

        assertTrue(
            "the same switch again asks for nothing",
            repository.setExercisePlan(exercise, null, null, null, findWeight = true) is DataResult.Success,
        )

        val after = repository.observeExercises(template).first().single()
        assertEquals("the entered weight survives", 120_000L, after.sets.single().targetWeightGrams)
        assertTrue("the marker is still pending", after.calibrationPending)
    }

    @Test
    fun aFailedDeload_rollsBackTheRemovalsAndTheTargets() = runTest {
        // B345's rule, exercised on N307: a failure after the removals must leave the whole plan as it was.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repeat(3) { index ->
            repository.addSet(
                exercise,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 5,
                ),
            )
        }
        val before = repository.observeExercises(template).first().single().sets
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_deload_update BEFORE UPDATE OF setIndex, targetWeightGrams " +
                "ON template_sets BEGIN SELECT RAISE(ABORT, 'fail'); END",
        )
        try {
            assertTrue(repository.deloadExercise(exercise, stepGrams = 2_500L) is DataResult.Failure)
        } finally {
            database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_deload_update")
        }

        val after = repository.observeExercises(template).first().single().sets
        assertEquals("every set is still live", before.map { it.id }, after.map { it.id })
        assertEquals("at its original load", before.map { it.targetWeightGrams }, after.map { it.targetWeightGrams })
    }

    @Test
    fun aFailedReplace_leavesTheOldMovementAndItsSets() = runTest {
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repeat(2) {
            repository.addSet(
                exercise,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 5,
                ),
            )
        }
        val before = repository.observeExercises(template).first().single()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_replace_update BEFORE UPDATE ON template_exercises " +
                "BEGIN SELECT RAISE(ABORT, 'fail'); END",
        )
        try {
            assertTrue(
                repository.replaceExercise(
                    exercise,
                    newExerciseId = "bench-press",
                    seeds = emptyList(),
                    empty = false,
                ) is DataResult.Failure,
            )
        } finally {
            database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_replace_update")
        }

        val after = repository.observeExercises(template).first().single()
        assertEquals("the old row is still the one live row", before.id, after.id)
        assertEquals("with its own sets", before.sets.map { it.id }, after.sets.map { it.id })
    }

    @Test
    fun theWeightlessAndDeloadWrites_keepADropRun_thatTheAnchorsNewLoadCannotSupport() = runTest {
        // B421: the deliberate transitions may strand a kept rung. Refusing the write made Find the
        // weight unusable on a plan with a ladder and a deload of a light anchor fail.
        val weights = create("Weights")
        val lift = plannedExercise(weights)
        repository.addSet(
            lift,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 5_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
            ),
        )
        repository.addSet(lift, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 1_000L))

        assertTrue(
            "the plan-editor switch is the same prescription",
            repository.setExercisePlan(
                lift,
                restSeconds = null,
                techniqueNote = null,
                targetRpeHalves = null,
                findWeight = true,
            ) is DataResult.Success,
        )
        val weightless = repository.observeExercises(weights).first().single()
        assertNull("the load is gone", weightless.sets.first().targetWeightGrams)
        assertEquals("the ladder is kept", 1_000L, weightless.sets.last().dropValueGrams)

        val light = create("Light")
        val lightLift = plannedExercise(light)
        repository.addSet(
            lightLift,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 5_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
            ),
        )
        repository.addSet(lightLift, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 1_000L))

        assertTrue(repository.deloadExercise(lightLift, 2_500L) is DataResult.Success)
        val deloaded = repository.observeExercises(light).first().single()
        assertEquals("the anchor clamps at zero and the rung survives", 0L, deloaded.sets.first().targetWeightGrams)
        assertEquals(1_000L, deloaded.sets.last().dropValueGrams)
    }

    @Test
    fun thePerLiftDeload_keepsTheRampTheLadderAndTheHold() = runTest {
        // N312: the per-lift deload through the schema, not the pure rule — a warm-up and a rung are
        // untouched, an assisted set gains help, a hold keeps its seconds, and a range moves to its floor.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.setExercisePlan(
            exercise,
            restSeconds = 120,
            techniqueNote = "Brace before the descent",
            targetRpeHalves = 16,
        )
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.WARMUP,
                targetWeightGrams = 60_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
        )
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 8,
                targetRepsCurrent = 7,
            ),
        )
        repository.addSet(exercise, TemplateSetEdit(role = SetType.DROP, dropValueGrams = 10_000L))
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetAssistanceGrams = 20_000L,
                targetRepsMin = 8,
                targetRepsMax = 10,
            ),
        )
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 40_000L,
                targetDurationSeconds = 45,
            ),
        )
        // The fourth working set is the one the trim takes; the rung belongs to a kept anchor.
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
        )

        val plan = (repository.deloadExercise(exercise, 2_500L) as DataResult.Success).data

        assertEquals("one of four working sets goes", 1, plan.removedSetIds.size)
        val after = repository.observeSets(template).first()
        val warmUp = after.single { it.role == SetType.WARMUP }
        assertEquals("a warm-up is not a set of the deload", 60_000L, warmUp.targetWeightGrams)
        assertEquals(5, warmUp.targetRepsMax)

        val ranged = after.single { it.role == SetType.NORMAL && it.targetRepsMin == 5 }
        assertEquals("three load steps off", 92_500L, ranged.targetWeightGrams)
        assertEquals("and a range moves to its floor", 5, ranged.targetRepsCurrent)

        val assisted = after.single { it.targetAssistanceGrams != null }
        assertEquals("more help is lighter", 27_500L, assisted.targetAssistanceGrams)
        assertNull("an assisted set states no weight of its own", assisted.targetWeightGrams)

        val hold = after.single { it.targetDurationSeconds != null }
        assertEquals("a hold keeps its seconds", 45, hold.targetDurationSeconds)
        assertEquals("and still moves its load", 32_500L, hold.targetWeightGrams)
        assertNull("its range is not a rep range", hold.targetRepsCurrent)

        val rung = after.single { it.role == SetType.DROP }
        assertEquals("a kept anchor keeps its ladder", 10_000L, rung.dropValueGrams)

        val row = repository.observeExercises(template).first().single()
        assertEquals("the cue and the rest are the row's, untouched", "Brace before the descent", row.techniqueNote)
        assertEquals(120, row.restSeconds)
    }

    @Test
    fun theEmptyReplace_clearsTheCueAndTheWarmUps_andKeepsThePlansReps() = runTest {
        // N312: N308's Empty through the schema — a new row with new ids, the old movement's cue and every
        // warm-up target gone, and the plan's reps, climbed place and effort kept for the number to find.
        val template = create("Legs")
        val old = plannedExercise(template)
        repository.setExercisePlan(
            old,
            restSeconds = 120,
            techniqueNote = "Pause at the bottom",
            targetRpeHalves = 16,
        )
        repository.addSet(
            old,
            TemplateSetEdit(
                role = SetType.WARMUP,
                targetWeightGrams = 60_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
        )
        repeat(4) {
            repository.addSet(
                old,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 100_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 8,
                    targetRepsCurrent = 6,
                ),
            )
        }

        val outcome = (
            repository.replaceExercise(old, "bench-press", seeds = emptyList(), empty = true)
                as DataResult.Success
            ).data

        val replaced = repository.observeExercises(template).first().single()
        assertEquals(outcome.newTemplateExerciseId, replaced.id)
        assertEquals("the new movement", "bench-press", replaced.exerciseId)
        assertNull("the cue was the old movement's", replaced.techniqueNote)
        assertEquals("the rest is the plan's", 120, replaced.restSeconds)
        assertEquals("and the effort is the plan's", 16, replaced.targetRpeHalves)
        assertTrue("Empty is what asks for the number next session", replaced.calibrationPending)

        val warmUp = replaced.sets.single { it.role == SetType.WARMUP }
        assertNull("the ramp's weight means the old movement", warmUp.targetWeightGrams)
        assertNull(warmUp.targetRepsMax)

        val work = replaced.sets.filter { it.role == SetType.NORMAL }
        assertEquals("four working sets trim to three", 3, work.size)
        assertTrue(
            "Empty keeps the reps and clears the loads",
            work.all {
                it.targetWeightGrams == null &&
                    it.targetAssistanceGrams == null &&
                    it.targetRepsMin == 5 &&
                    it.targetRepsMax == 8 &&
                    it.targetRepsCurrent == 6
            },
        )
        assertEquals("and every kept set is a new row", 4, outcome.newSetIds.size)
    }

    @Test
    fun theCalibrationAdoption_writesOnlyItsOwnSetsAndKeepsAHoldsSeconds() = runTest {
        // N312: the adoption through the schema — a hold keeps its seconds while its load is adopted, an
        // assisted number reaches the assistance column, and another row's set id is not this row's to write.
        val template = create("Legs")
        val lift = plannedExercise(template)
        repository.addSet(
            lift,
            TemplateSetEdit(
                role = SetType.NORMAL,
                targetWeightGrams = 100_000L,
                targetRepsMin = 5,
                targetRepsMax = 5,
            ),
        )
        repository.addSet(lift, TemplateSetEdit(role = SetType.NORMAL, targetDurationSeconds = 45))
        // A second row of the same movement: this adoption must not reach its sets.
        repository.addExercise(template, "back-squat")
        val other = repository.observeExercises(template).first()
            .single { it.exerciseId == "back-squat" && it.id != lift }
            .id
        val otherSet = (
            repository.addSet(
                other,
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = 70_000L,
                    targetRepsMin = 5,
                    targetRepsMax = 5,
                ),
            ) as DataResult.Success
            ).data

        assertTrue(
            "the plan-editor switch is the same prescription",
            repository.setExercisePlan(
                lift,
                restSeconds = null,
                techniqueNote = null,
                targetRpeHalves = null,
                findWeight = true,
            ) is DataResult.Success,
        )
        val plannedSets = repository.observeSets(template).first().filter { it.templateExerciseId == lift }
        val holdId = plannedSets.single { it.targetDurationSeconds == 45 }.id
        val countedId = plannedSets.single { it.targetDurationSeconds == null }.id

        assertTrue(
            repository.calibrateExercise(
                lift,
                listOf(
                    CalibrationTarget(countedId, 90_000L, 5_000L),
                    CalibrationTarget(holdId, 20_000L, 0L),
                    // A stale answer for another row must not reach it.
                    CalibrationTarget(otherSet, 1_000L, 0L),
                ),
            ) is DataResult.Success,
        )

        val after = repository.observeSets(template).first()
        val counted = after.single { it.id == countedId }
        assertEquals("the number found", 90_000L, counted.targetWeightGrams)
        assertEquals("and the help it needed", 5_000L, counted.targetAssistanceGrams)

        val hold = after.single { it.id == holdId }
        assertEquals("a hold adopts its load", 20_000L, hold.targetWeightGrams)
        assertEquals("and keeps its seconds", 45, hold.targetDurationSeconds)

        assertEquals(
            "another row's set is not this adoption's to write",
            70_000L,
            after.single { it.id == otherSet }.targetWeightGrams,
        )
        assertFalse(
            "the prompt is one-shot",
            repository.observeExercises(template).first().first { it.id == lift }.calibrationPending,
        )
    }

    private suspend fun plannedExercise(templateId: String): String {
        repository.addExercise(templateId, "back-squat")
        return repository.observeExercises(templateId).first().single().id
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

    @Test
    fun keepingAVersion_copiesThePlan_asItStands() = runTest {
        // ROADMAP N134: the lifter presses *Keep a version* at the moment they decide to change
        // something, and what is kept is the state as it stands — the exercises, their order and their
        // targets — under a name of its own.
        val template = create("Legs")
        val exercise = plannedExercise(template)
        repository.addSet(
            exercise,
            TemplateSetEdit(
                role = SetType.TOP_SET,
                targetWeightGrams = 140_000L,
                targetRepsMin = 1,
                targetRepsMax = 2,
            ),
        )

        assertTrue(repository.keepVersion(template) is DataResult.Success)

        val version = repository.observeVersions(template).first().single()
        assertEquals("the live plan's name is the version's own", "Legs", version.name)
        assertEquals(template, version.versionOf)
        assertEquals(1, version.exerciseCount)
        assertNotNull("and it is stamped with when it was kept", version.keptAt)

        val copied = repository.observeExercises(version.id).first().single()
        assertEquals("back-squat", copied.exerciseId)
        assertEquals("the targets travel with the copy", listOf(SetType.TOP_SET), copied.sets.map { it.role })
        assertEquals(140_000L, copied.sets.single().targetWeightGrams)
    }

    @Test
    fun aKeptVersion_isNotATemplate_andIsNeverEdited() = runTest {
        val template = create("Legs")
        plannedExercise(template)
        repository.keepVersion(template)
        val version = repository.observeVersions(template).first().single()

        assertEquals(
            "the templates list holds the template and not its version",
            listOf(template),
            repository.observeTemplates().first().map { it.id },
        )
        assertNull("the editor's read refuses a version", repository.observeTemplate(version.id).first())
        assertTrue(
            "and a version cannot be renamed",
            repository.renameTemplate(version.id, "Old legs") is DataResult.Failure,
        )
        assertEquals("Legs", repository.observeVersions(template).first().single().name)
    }

    @Test
    fun aVersionIsUnchanged_whenTheLivePlanChanges() = runTest {
        // The whole point of keeping one: the record does not follow the plan it came from.
        val template = create("Legs")
        plannedExercise(template)
        repository.keepVersion(template)
        val version = repository.observeVersions(template).first().single()

        repository.renameTemplate(template, "Legs (new)")
        repository.addExercise(template, "bench-press")

        assertEquals("the live plan moved on", "Legs (new)", repository.observeTemplate(template).first()?.name)
        assertEquals("and the version did not", "Legs", repository.observeVersions(template).first().single().name)
        assertEquals(1, repository.observeExercises(version.id).first().size)
    }

    @Test
    fun aVersionOfAVersion_isRefused() = runTest {
        val template = create("Legs")
        plannedExercise(template)
        repository.keepVersion(template)
        val version = repository.observeVersions(template).first().single()

        assertTrue(
            "only a live plan can be kept",
            repository.keepVersion(version.id) is DataResult.Failure,
        )
    }

    @Test
    fun twoVersions_areNewestFirst() = runTest {
        val template = create("Legs")
        plannedExercise(template)
        repository.keepVersion(template)
        now = now.plusSeconds(60)
        repository.keepVersion(template)

        val versions = repository.observeVersions(template).first()

        assertEquals(2, versions.size)
        assertTrue("the newest is first", versions[0].keptAt!!.isAfter(versions[1].keptAt!!))
    }
}
