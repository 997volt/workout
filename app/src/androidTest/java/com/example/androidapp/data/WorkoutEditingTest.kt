package com.example.androidapp.data

import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateEntity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneOffsetSource
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import java.time.Instant
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

/**
 * Instrumented tests for correcting a past workout (ROADMAP P1.7).
 *
 * Before this, `Finish` was a one-way door: a mistyped weight appeared in history
 * and could never be fixed. These assert the correction actually lands, and that
 * the history list's SQL aggregates follow it — which is also what keeps the
 * list's volume formula and the detail screen's agreeing.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutEditingTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomWorkoutRepository

    private val clock = TimeSource { Instant.parse("2026-09-28T08:00:00Z") }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
        repository = RoomWorkoutRepository(database, clock, ZoneOffsetSource { 0 })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun correctingASet_changesWhatHistoryReports() = runTest {
        seedFinishedWorkout()
        val setId = database.backupDao().allSets().single().id
        // 100 kg x 5 = 500,000 gram-reps to begin with.
        assertEquals(500_000L, historyVolume())

        val result = repository.updateSet(setId, reps = 8, weightGrams = 100_000L, rpeHalves = null, note = null)

        assertTrue(result is DataResult.Success)
        assertEquals("the list's aggregate must follow the edit", 800_000L, historyVolume())
    }

    @Test
    fun deletingAPastWorkout_removesItFromHistory() = runTest {
        val sessionId = seedFinishedWorkout()
        assertTrue(database.workoutDao().observeHistory().first().isNotEmpty())

        val result = repository.deleteSession(sessionId)

        assertTrue(result is DataResult.Success)
        assertTrue(
            "a deleted workout must not linger in the history list",
            database.workoutDao().observeHistory().first().isEmpty(),
        )
    }

    @Test
    fun correctingASetThatIsGone_failsAsNotFound() = runTest {
        seedFinishedWorkout()
        val setId = database.backupDao().allSets().single().id
        repository.deleteSet(setId)

        val result = repository.updateSet(setId, reps = 5, weightGrams = 1_000L, rpeHalves = null, note = null)

        // A stale screen holding a deleted set's id must not resurrect it.
        assertEquals(DataError.NotFound, (result as DataResult.Failure).error)
    }

    @Test
    fun correctingASet_recordsItsRpeAndComment() = runTest {
        seedFinishedWorkout()
        val setId = database.backupDao().allSets().single().id

        val result = repository.updateSet(
            setId,
            reps = 5,
            weightGrams = 100_000L,
            rpeHalves = 16,
            note = "Felt heavy",
        )

        assertTrue(result is DataResult.Success)
        val stored = database.workoutDao().findSetById(setId)!!
        assertEquals(16, stored.rpeHalves)
        assertEquals("Felt heavy", stored.note)
        // The columns an edit does not touch are preserved, not invented.
        assertEquals(0, stored.setIndex)
        assertEquals("se1", stored.sessionExerciseId)
    }

    @Test
    fun aBlankComment_isStoredAsNull_andRpeCanBeCleared() = runTest {
        seedFinishedWorkout()
        val setId = database.backupDao().allSets().single().id
        repository.updateSet(setId, reps = 5, weightGrams = 100_000L, rpeHalves = 16, note = "Felt heavy")

        repository.updateSet(setId, reps = 5, weightGrams = 100_000L, rpeHalves = null, note = "   ")

        val stored = database.workoutDao().findSetById(setId)!!
        assertNull(stored.note)
        assertNull("clearing an RPE is how a mistyped one is undone", stored.rpeHalves)
    }

    @Test
    fun anRpeOutsideTheScale_isRefused_withoutTouchingTheSet() = runTest {
        seedFinishedWorkout()
        val setId = database.backupDao().allSets().single().id

        val result = repository.updateSet(
            setId,
            reps = 5,
            weightGrams = 100_000L,
            rpeHalves = 21,
            note = null,
        )

        assertTrue((result as DataResult.Failure).error is DataError.Invalid)
        assertNull("a refused write must leave the set alone", database.workoutDao().findSetById(setId)!!.rpeHalves)
    }

    @Test
    fun finishingAnExercise_marksItDone_andClearsTheRest() = runTest {
        val sessionId = seedOpenWorkoutWithASet()
        // Arm the rest, so ending the exercise has something to clear.
        database.workoutDao().updateRestTimer(sessionId, restEndsAt = 9_999L, at = 1_000L)

        val result = repository.finishExercise("se1")

        assertTrue(result is DataResult.Success)
        val exercise = database.workoutDao().observeSessionExerciseDetails(sessionId).first().single()
        assertNotNull("the row must carry its done timestamp", exercise.finishedAt)
        assertNull(
            "ending a lift must not leave a rest armed (N7)",
            database.workoutDao().findSession(sessionId)?.restEndsAt,
        )
    }

    @Test
    fun aDoneExercise_cannotTakeAnotherSet() = runTest {
        seedOpenWorkoutWithASet()
        repository.finishExercise("se1")

        val result = repository.logSet(
            "se1",
            reps = 5,
            weightGrams = 100_000L,
            rpeHalves = null,
            note = null,
        )

        assertEquals(DataError.NotFound, (result as DataResult.Failure).error)
        assertEquals("the refused set must not be written", 1, database.backupDao().allSets().size)
    }

    @Test
    fun reopeningAnExercise_restoresLogging_andClearsTheDoneState() = runTest {
        val sessionId = seedOpenWorkoutWithASet()
        repository.finishExercise("se1")

        val result = repository.reopenExercise("se1")

        assertTrue(result is DataResult.Success)
        assertNull(
            database.workoutDao().observeSessionExerciseDetails(sessionId).first().single().finishedAt,
        )
        assertTrue(
            repository.logSet(
                "se1",
                reps = 5,
                weightGrams = 100_000L,
                rpeHalves = null,
                note = null,
            ) is DataResult.Success,
        )
    }

    @Test
    fun aLoggedSet_keepsTheRpeAndCommentItWasGiven() = runTest {
        // N6, N59: the inline fields are on screen before *Log set*, so what they state is what the
        // write has to carry. It did not — `logSet` had no such parameters, so every set logged from
        // the screen came back with no effort recorded, and undoing a delete lost the comment too.
        seedOpenWorkoutWithASet()

        val result = repository.logSet(
            sessionExerciseId = "se1",
            reps = 8,
            weightGrams = 100_000L,
            rpeHalves = 19,
            note = "  grinder  ",
        )

        assertTrue(result is DataResult.Success)
        val logged = database.backupDao().allSets().single { it.setIndex == 1 }
        assertEquals(19, logged.rpeHalves ?: 0)
        assertEquals("a cleared comment is null, not whitespace", "grinder", logged.note)
    }

    @Test
    fun aWarmUpSet_recordsNoEffort_whateverTheWriteWasGiven() = runTest {
        // ROADMAP N67: a warm-up carries no effort. The editor no longer offers the field, and this is
        // the boundary behind it — a stale screen, or any future caller, cannot put a number on a set
        // the role says was never work.
        seedOpenWorkoutWithASet()

        val result = repository.logSet(
            sessionExerciseId = "se1",
            reps = 5,
            weightGrams = 60_000L,
            rpeHalves = 19,
            note = null,
            setType = SetType.WARMUP,
        )

        assertTrue(result is DataResult.Success)
        val logged = database.backupDao().allSets().single { it.setIndex == 1 }
        assertNull("a warm-up records none", logged.rpeHalves)
    }

    @Test
    fun reRollingASetToWarmUp_dropsTheEffortItCarried() = runTest {
        // The same rule on the correction path (N67): the editor hides the field for a warm-up, so a
        // save has to drop the number rather than keep it behind a control the lifter cannot see.
        seedOpenWorkoutWithASet()

        val result = repository.updateSet(
            setId = "set1",
            reps = 5,
            weightGrams = 60_000L,
            rpeHalves = 19,
            note = null,
            setType = SetType.WARMUP,
        )

        assertTrue(result is DataResult.Success)
        assertNull(
            "the recorded effort is dropped, not hidden",
            database.workoutDao().findSetById("set1")?.rpeHalves,
        )
    }

    @Test
    fun anRpeOffTheScale_isRefusedRatherThanStored() = runTest {
        // The field is the real guard; this is the boundary behind it (N6).
        seedOpenWorkoutWithASet()

        val result = repository.logSet(
            sessionExerciseId = "se1",
            reps = 8,
            weightGrams = 100_000L,
            rpeHalves = 21,
            note = null,
        )

        assertTrue(result is DataResult.Failure)
        assertEquals(
            "nothing half a step off the scale reaches the database",
            1,
            database.backupDao().allSets().size,
        )
    }

    @Test
    fun finishingAGoneExercise_isNotFound() = runTest {
        seedOpenWorkoutWithASet()
        repository.removeExercise("se1")

        assertEquals(
            DataError.NotFound,
            (repository.finishExercise("se1") as DataResult.Failure).error,
        )
    }

    @Test
    fun ratingAnExercise_storesMuscleFeelAndThePickedJoints() = runTest {
        val sessionId = seedOpenWorkoutWithASet()

        val result = repository.rateExercise(
            "se1",
            muscleFeel = 8,
            joints = listOf(
                JointPain(Joint.KNEE, Side.LEFT, 6),
                JointPain(Joint.KNEE, Side.RIGHT, 3),
            ),
        )

        assertTrue(result is DataResult.Success)
        val stored = repository.observeSessionExercises(sessionId).first().single()
        assertEquals(8, stored.muscleFeel)
        // Left and right are two rows with their own scores (ROADMAP N63), in pick order.
        assertEquals(
            listOf(
                JointPain(Joint.KNEE, Side.LEFT, 6),
                JointPain(Joint.KNEE, Side.RIGHT, 3),
            ),
            stored.joints,
        )
    }

    @Test
    fun aJointList_isReplaced_ratherThanMerged() = runTest {
        // ROADMAP N63: the editor shows exactly what is stored, so a joint missing from a save was
        // removed by the lifter and must not survive as a hidden row.
        val sessionId = seedOpenWorkoutWithASet()
        repository.rateExercise(
            "se1",
            muscleFeel = 8,
            joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 6), JointPain(Joint.KNEE, Side.RIGHT, 3)),
        )

        repository.rateExercise(
            "se1",
            muscleFeel = 8,
            joints = listOf(JointPain(Joint.KNEE, Side.RIGHT, 9)),
        )

        assertEquals(
            listOf(JointPain(Joint.KNEE, Side.RIGHT, 9)),
            repository.observeSessionExercises(sessionId).first().single().joints,
        )
    }

    @Test
    fun aRating_retiresTheLegacyColumns_itReplaces() = runTest {
        // ROADMAP N63: the single number and its free text are what an old session recorded, and the
        // row summary and the pain trend fall back to them whenever the picked list is empty. A new
        // rating **replaces** the old one, so it retires them — left behind, a list the lifter cleared
        // would resurrect the number they just removed.
        val sessionId = seedOpenWorkoutWithASet()
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "se-legacy",
                sessionId = sessionId,
                exerciseId = "back-squat",
                position = 1,
                muscleFeel = 4,
                jointPain = 7,
                jointPainNote = "left shoulder",
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )

        repository.rateExercise(
            "se-legacy",
            muscleFeel = 8,
            joints = listOf(JointPain(Joint.SHOULDER, Side.LEFT, 5)),
        )

        val stored = repository.observeSessionExercises(sessionId).first().single { it.id == "se-legacy" }
        assertEquals("the picked list is the new rating", listOf(JointPain(Joint.SHOULDER, Side.LEFT, 5)), stored.joints)
        assertNull("the legacy number is retired by the rating that replaces it", stored.jointPain)
        assertNull("and its free text", stored.jointPainNote)
    }

    @Test
    fun clearingAPickedList_keepsItCleared() = runTest {
        // ROADMAP N63: the resurrection this guards against — a save with every joint removed leaves
        // no live row, and a reader that fell back to the legacy column would show the old pain again.
        val sessionId = seedOpenWorkoutWithASet()
        repository.rateExercise("se1", muscleFeel = 8, joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 6)))

        repository.rateExercise("se1", muscleFeel = 8, joints = emptyList())

        val stored = repository.observeSessionExercises(sessionId).first().single { it.id == "se1" }
        assertEquals("the list is cleared, not resurrected", emptyList<JointPain>(), stored.joints)
    }

    @Test
    fun aRatingOutsideTheScale_isRefused_withoutTouchingTheExercise() = runTest {
        val sessionId = seedOpenWorkoutWithASet()

        val result = repository.rateExercise("se1", muscleFeel = 11, joints = emptyList())

        assertTrue((result as DataResult.Failure).error is DataError.Invalid)
        assertNull(
            "a refused write must leave the exercise unrated",
            database.workoutDao().observeSessionExerciseDetails(sessionId).first().single().muscleFeel,
        )
    }

    @Test
    fun aJointScoreOutsideTheScale_isRefused_beforeAnythingIsWritten() = runTest {
        // N63: the picked list carries its own scores, so the one validator covers them too — and it
        // runs before the muscle feel is stored, so a bad list cannot leave half a rating.
        val sessionId = seedOpenWorkoutWithASet()

        val result = repository.rateExercise(
            "se1",
            muscleFeel = 8,
            joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 11)),
        )

        assertTrue((result as DataResult.Failure).error is DataError.Invalid)
        assertNull(
            "nothing is written when a joint score is off the scale",
            database.workoutDao().observeSessionExerciseDetails(sessionId).first().single().muscleFeel,
        )
    }

    @Test
    fun aWorkoutComment_isStored_andABlankOneClearsIt() = runTest {
        val sessionId = seedOpenWorkoutWithASet()

        assertTrue(repository.setWorkoutNotes(sessionId, "  Felt strong  ") is DataResult.Success)
        assertEquals("Felt strong", repository.observeSession(sessionId).first()?.notes)

        repository.setWorkoutNotes(sessionId, "   ")

        // One representation of "nothing", the same rule the readiness note follows.
        assertNull(repository.observeSession(sessionId).first()?.notes)
    }

    @Test
    fun aCommentOnAGoneWorkout_isNotFound() = runTest {
        assertEquals(
            DataError.NotFound,
            (repository.setWorkoutNotes("no-such-session", "x") as DataResult.Failure).error,
        )
    }

    @Test
    fun ratingsSurviveFinishingAndReopening() = runTest {
        val sessionId = seedOpenWorkoutWithASet()
        repository.rateExercise(
            "se1",
            muscleFeel = 8,
            joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 2)),
        )

        repository.finishExercise("se1")
        repository.reopenExercise("se1")

        // Reopening is for fixing a mis-tap; it must not wipe how it felt.
        val stored = repository.observeSessionExercises(sessionId).first().single()
        assertEquals(8, stored.muscleFeel)
        assertEquals(listOf(JointPain(Joint.KNEE, Side.LEFT, 2)), stored.joints)
    }

    private suspend fun historyVolume(): Long =
        database.workoutDao().observeHistory().first().single().volumeGrams

    private suspend fun seedFinishedWorkout(): String {
        val sessionId = seedOpenWorkoutWithASet()
        database.workoutDao().markFinished(id = sessionId, at = 2_000L)
        return sessionId
    }

    /** An open session with one exercise and one logged set (`s1`, `se1`, `set1`). */

    @Test
    fun aRungCannotBeTheFirstSetOfAnExercise() = runTest {
        // ROADMAP N79: a drop or cluster set hangs off the set above it, so the first set of an
        // exercise has nothing to derive from and nothing to rate. The picker offers only what this
        // allows (B64) and this is the boundary that holds it against a stale screen — and the rule is
        // about the *set above* rather than about rungs, which the third assertion shows.
        seedOpenWorkout()
        val rung: suspend (SetType) -> DataResult<Unit> = { type ->
            repository.logSet(
                "se1",
                reps = 5,
                weightGrams = 80_000L,
                rpeHalves = null,
                note = null,
                setType = type,
            )
        }

        assertTrue("nothing above it to drop from", rung(SetType.DROP) is DataResult.Failure)
        assertTrue("and the same for a cluster", rung(SetType.CLUSTER) is DataResult.Failure)
        assertTrue(
            "a working set is fine",
            repository.logSet(
                "se1",
                reps = 5,
                weightGrams = 100_000L,
                rpeHalves = null,
                note = null,
            ) is DataResult.Success,
        )
        assertTrue("and now a rung has an anchor", rung(SetType.DROP) is DataResult.Success)
    }

    @Test
    fun aRungCannotFollowAWarmUp() = runTest {
        // ROADMAP B63: an anchor is a set that stands on its own — `recordsEffort` is the one rule —
        // so a warm-up cannot anchor a run. The old guard asked only "is there a set above it that is
        // not a rung", which a warm-up satisfied, and a drop logged there derived nothing at all: the
        // app read no run above it and the row was a rung in name only.
        seedOpenWorkout()
        val log: suspend (SetType) -> DataResult<Unit> = { type ->
            repository.logSet(
                "se1",
                reps = 5,
                weightGrams = 80_000L,
                rpeHalves = null,
                note = null,
                setType = type,
            )
        }

        assertTrue("a ramp is a set", log(SetType.WARMUP) is DataResult.Success)
        assertTrue("but it is not an anchor", log(SetType.DROP) is DataResult.Failure)
        assertTrue("for a cluster either", log(SetType.CLUSTER) is DataResult.Failure)
    }

    @Test
    fun reRolingALoggedSetIntoARung_holdsTheSameRule() = runTest {
        // ROADMAP B64: editing a set's role is a write like logging one, and it had no rung guard at
        // all — so the first set of an exercise could be re-roled into a drop that the log path would
        // have refused, and the app would read no run for it.
        seedOpenWorkoutWithASet()

        val result = repository.updateSet(
            setId = "set1",
            reps = 5,
            weightGrams = 100_000L,
            rpeHalves = null,
            note = null,
            setType = SetType.DROP,
        )

        assertTrue("the first set has nothing above it", result is DataResult.Failure)
        assertEquals(
            "and the row keeps the role it had",
            SetType.NORMAL,
            database.workoutDao().findSetById("set1")!!.setType,
        )
    }

    @Test
    fun reRolingALaterSetIntoARung_isAllowed() = runTest {
        // The other half: with a working set above it, the same edit is a legitimate re-role — the
        // guard is about the place in the session, not about editing a set at all.
        seedOpenWorkoutWithASet()
        repository.logSet(
            "se1",
            reps = 5,
            weightGrams = 90_000L,
            rpeHalves = null,
            note = null,
        )
        val logged = database.workoutDao().observeSetsForSession("s1").first().first { it.setIndex == 1 }

        val result = repository.updateSet(
            setId = logged.id,
            reps = 5,
            weightGrams = 80_000L,
            rpeHalves = null,
            note = null,
            setType = SetType.DROP,
        )

        assertTrue(result is DataResult.Success)
        assertEquals(SetType.DROP, database.workoutDao().findSetById(logged.id)!!.setType)
    }

    @Test
    fun anExercisesOwnStep_reachesTheSessionItIsLoggedIn() = runTest {
        // ROADMAP B70: `stepGrams` travels through a hand-written SQL projection — the same shape that
        // dropped a column three times before the round-trip test existed — and nothing read it back
        // from the session. The live ± buttons step by what arrives here (N77).
        seedOpenWorkout(stepGrams = 5_000L)

        val session = repository.observeSessionExercises("s1").first().single()

        assertEquals(5_000L, session.stepGrams)
    }

    /**
     * The same open workout as [seedOpenWorkoutWithASet], before anything has been logged (N79).
     *
     * [stepGrams] is the movement's own weight step (N77), unset by default: the projection that carries
     * it onto the session had no test until B70.
     */
    private suspend fun seedOpenWorkout(stepGrams: Long? = null): String {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.SQUAT,
                    isCustom = false,
                    stepGrams = stepGrams,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
            ),
        )
        val session = database.workoutDao().findOrCreateActiveSession(id = "s1", now = 1_000L).session
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "se1",
                sessionId = session.id,
                exerciseId = "back-squat",
                position = 0,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
        return session.id
    }

    private suspend fun seedOpenWorkoutWithASet(): String {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.SQUAT,
                    isCustom = false,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
            ),
        )

        val session = database.workoutDao().findOrCreateActiveSession(id = "s1", now = 1_000L).session
        database.workoutDao().insertSessionExercise(
            SessionExerciseEntity(
                id = "se1",
                sessionId = session.id,
                exerciseId = "back-squat",
                position = 0,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
        database.workoutDao().insertSet(
            SetEntryEntity(
                id = "set1",
                sessionExerciseId = "se1",
                setIndex = 0,
                reps = 5,
                weightGrams = 100_000L,
                setType = SetType.NORMAL,
                completedAt = 1_000L,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                deletedAt = null,
            ),
        )
        return session.id
    }

    @Test
    fun anAssistedSet_takesNothingOffTheVolume() = runTest {
        // ROADMAP N15: assistance is a separate magnitude, so volume stays
        // `weight × reps`. A signed weight would have made this set subtract, which
        // is the whole reason it is not one.
        seedFinishedWorkout()
        repository.updateSet(
            setId = "set1",
            reps = 8,
            weightGrams = 20_000L,
            rpeHalves = null,
            note = null,
            assistanceGrams = 20_000L,
        )

        val stored = database.workoutDao().findSetById("set1")!!
        assertEquals(20_000L, stored.assistanceGrams)
        assertEquals("the weight is what was added, not netted", 20_000L, stored.weightGrams)

        // 20 kg × 8 is 160 kg; the 20 kg of help is not subtracted from it.
        assertEquals(160_000L, historyVolume())
    }

    @Test
    fun theUiVolumeFormula_agreesWithTheSql() = runTest {
        // `WorkoutDetailViewModel.volumeGrams` says it is "one formula in two places" and
        // that "the instrumented history test asserts they agree" — which no test did
        // (ROADMAP B9). This is that assertion: the sum the detail screen computes over
        // its own rows, against the figure the history query returns for the same rows.
        seedFinishedWorkout()
        repository.logSet(
            sessionExerciseId = "se1",
            reps = 8,
            weightGrams = 0L,
            rpeHalves = null,
            note = null,
            assistanceGrams = 20_000L,
        )

        val sets = database.workoutDao().observeSetsForSession("s1").first()
        val uiFormula = sets.sumOf { it.reps.toLong() * it.weightGrams }

        assertEquals("the assisted set contributes nothing, not a negative", 500_000L, uiFormula)
        assertEquals(uiFormula, historyVolume())
    }

    @Test
    fun personalRecords_ignoreWarmUps_andTheSessionBeingLogged() = runTest {
        // ROADMAP B24: this read met only hand-written fakes, and the SQL behind it has to do two
        // things no fake can prove — leave warm-ups out, and be able to exclude the session in
        // progress, which is what stops a set being compared against itself.
        val sessionId = seedFinishedWorkout()
        // A heavier warm-up at the same rep count: with the role ignored it would set the record.
        database.workoutDao().insertSet(
            SetEntryEntity(
                id = "warm-up",
                sessionExerciseId = "se1",
                setIndex = 1,
                reps = 5,
                weightGrams = 120_000L,
                setType = SetType.WARMUP,
                completedAt = 1L,
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
            ),
        )

        val records = (repository.personalRecords("back-squat") as DataResult.Success).data
        assertEquals("the working set, not the heavier warm-up", 100_000L, records.bestAt(5))

        val excluding = (repository.personalRecords("back-squat", excludingSessionId = sessionId)
            as DataResult.Success).data
        assertTrue("excluding the session in progress leaves nothing", excluding.isEmpty)
    }

    @Test
    fun setSupersetGroup_isReadBackThroughTheProjection() = runTest {
        // The session projection already shipped once without selecting this column, so the row
        // arrived ungrouped while the write succeeded — a defect no fake could show, because a fake
        // never builds a projection from a query string (ROADMAP B24).
        seedOpenWorkoutWithASet()

        val written = repository.setSupersetGroup(listOf("se1"), group = 3)
        assertTrue(written is DataResult.Success)

        val row = repository.observeSessionExercises("s1").first().single()
        assertEquals("the write reaches the read", 3, row.supersetGroup)
    }

    @Test
    fun leavingASuperset_isWrittenToo() = runTest {
        seedOpenWorkoutWithASet()
        repository.setSupersetGroup(listOf("se1"), group = 3)

        repository.setSupersetGroup(listOf("se1"), group = null)

        val row = repository.observeSessionExercises("s1").first().single()
        assertNull("a nullable group is how leaving is expressed", row.supersetGroup)
    }

    @Test
    fun aWorkoutStartedFromAPlan_arrivesWithItsSupersets() = runTest {
        // ROADMAP B16's other half, and the reason the column exists at all: a plan that prescribes
        // a superset must arrive as one. The device walk could only show the editor's labels; this
        // is the seeding path, where the plan meets the session.
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = "back-squat",
                    name = "Back Squat",
                    primaryMuscle = MuscleGroup.QUADS,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.SQUAT,
                    isCustom = false,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
                ExerciseEntity(
                    id = "bench-press",
                    name = "Barbell Bench Press",
                    primaryMuscle = MuscleGroup.CHEST,
                    secondaryMuscles = emptyList(),
                    equipment = Equipment.BARBELL,
                    movementPattern = MovementPattern.PRESS,
                    isCustom = false,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
            ),
        )
        database.templateDao().insertTemplate(
            TemplateEntity(
                id = "t1",
                name = "Pair",
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null,
            ),
        )
        listOf("te1" to "back-squat", "te2" to "bench-press").forEachIndexed { index, (id, exerciseId) ->
            database.templateDao().insertTemplateExercise(
                TemplateExerciseEntity(
                    id = id,
                    templateId = "t1",
                    exerciseId = exerciseId,
                    position = index,
                    supersetGroup = 1,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null,
                ),
            )
        }

        val started = repository.startOrResumeSession(templateId = "t1")
        val sessionId = (started as DataResult.Success).data.id

        val rows = repository.observeSessionExercises(sessionId).first()
        assertEquals("both arrive", 2, rows.size)
        assertEquals(
            "in the plan's group",
            listOf(1, 1),
            rows.map { it.supersetGroup },
        )
    }
}
