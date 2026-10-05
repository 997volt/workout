package com.example.androidapp.data

import kotlinx.coroutines.flow.first
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PerformedSetSpec
import com.example.androidapp.data.local.ExerciseTrendRowEntity
import androidx.room.withTransaction
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SessionExerciseJointEntity
import com.example.androidapp.data.local.SessionSoreMuscleEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.WorkoutDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.ZoneOffsetSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.TenPointScale
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.StartedSession
import com.example.androidapp.domain.repository.WorkoutRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Room-backed [WorkoutRepository] (ROADMAP P1.2, P1.3, P1.4, P1.8).
 *
 * Every write goes through [dataResultOf], so a database failure surfaces to the
 * caller as a [DataResult.Failure] instead of an exception that could disappear
 * inside a coroutine.
 */
@Singleton
class RoomWorkoutRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
    private val zoneOffsetSource: ZoneOffsetSource,
) : WorkoutRepository {

    private val dao = database.workoutDao()

    /** The session-*exercise* queries, split off `WorkoutDao` at its function ceiling (N24, N54). */
    private val sessionExerciseDao = database.sessionExerciseDao()

    /** Read only when a workout is started from a template (ROADMAP N3). */
    private val templateDao = database.templateDao()

    /** The session's sore-muscle rows (ROADMAP N62), and their half of the backup. */
    private val soreMuscleDao = database.sessionSoreMuscleDao()

    /** The joints a session exercise reported painful (ROADMAP N63), and their half of the backup. */
    private val jointDao = database.sessionExerciseJointDao()

    /**
     * The active session with its sore-muscle list (ROADMAP N62).
     *
     * `flatMapLatest` because the rows belong to whichever session is open, which changes when a
     * workout starts or finishes; `combine` would need the id before the query exists.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                soreMuscleDao.observeForSession(entity.id).map { entity.toDomain(it) }
            }
        }

    override fun observeSessionExercises(sessionId: String): Flow<List<SessionExercise>> =
        combine(
            dao.observeSessionExerciseDetails(sessionId),
            jointDao.observeForSession(sessionId),
        ) { rows, joints ->
            // Grouped once rather than filtered per row: the joints arrive whole for the session,
            // and an exercise with none is the common case.
            val byExercise = joints.groupBy { it.sessionExerciseId }
            rows.map { row -> row.toDomain(byExercise[row.id].orEmpty()) }
        }

    override fun observeSets(sessionId: String): Flow<List<SetEntry>> =
        dao.observeSetsForSession(sessionId).map { rows -> rows.map { it.toDomain() } }

    override fun observeHistory(): Flow<List<WorkoutSummary>> =
        dao.observeHistory().map { rows -> rows.map { it.toDomain() } }

    override fun observeSession(sessionId: String): Flow<WorkoutSession?> =
        combine(
            dao.observeSession(sessionId),
            soreMuscleDao.observeForSession(sessionId),
        ) { entity, soreMuscles -> entity?.toDomain(soreMuscles) }

    override suspend fun startOrResumeSession(
        templateId: String?,
        slotId: String?,
    ): DataResult<StartedSession> =
        dataResultOf {
            // find-or-create and any template seeding are one transaction, so two
            // taps cannot open two sessions — and a failure cannot leave a session
            // holding half a template. The same transaction reports whether this
            // call is the one that opened it (ROADMAP N4).
            database.withTransaction {
                val start = dao.findOrCreateActiveSession(
                    id = UUID.randomUUID().toString(),
                    now = timeSource.nowEpochMillis(),
                    // Where this workout is happening, captured once (ROADMAP N25).
                    zoneOffsetMinutes = zoneOffsetSource.offsetMinutes(),
                    // Written only if this call opens the session, so a resumed workout keeps the
                    // template it was created from (ROADMAP P3.3).
                    templateId = templateId,
                )
                if (start.created && templateId != null) {
                    // What the slot prescribes for each exercise, if it was started from one
                    // (ROADMAP P3.8). Read here rather than stored on the session, so the slot
                    // stays a living thing the session merely followed.
                    val prescribed = slotId
                        ?.let { slot -> database.programPrescriptionDao().findSlotExercises(slot) }
                        .orEmpty()
                        .associateBy { it.exerciseId }
                    templateDao.findPlannedExercises(templateId).forEach { planned ->
                        val fromSlot = prescribed[planned.exerciseId]
                        // The plan's rest and cue come with it (ROADMAP N14): a plan
                        // that says "3m break" and a workout counting 90 seconds is
                        // the plan being ignored. Null leaves the library's showing
                        // through, which is the fallback N5 established. A slot's own
                        // prescription wins where it speaks (P3.8).
                        appendExercise(
                            dao = dao,
                            now = timeSource.nowEpochMillis(),
                            sessionId = start.session.id,
                            exerciseId = planned.exerciseId,
                            restSeconds = fromSlot?.restSeconds ?: planned.restSeconds,
                            techniqueNote = fromSlot?.techniqueNote ?: planned.techniqueNote,
                            // A plan that prescribes a superset must arrive as one, or the
                            // grouping can only ever be made by hand (ROADMAP B16).
                            supersetGroup = planned.supersetGroup,
                        )
                    }
                }
                StartedSession(id = start.session.id, isNew = start.created)
            }
        }

    override suspend fun repeatSession(sessionId: String): DataResult<StartedSession> =
        dataResultOf {
            database.withTransaction {
                val source = dao.findSession(sessionId)
                    ?: throw NotFoundException("session $sessionId")
                val start = dao.findOrCreateActiveSession(
                    id = UUID.randomUUID().toString(),
                    now = timeSource.nowEpochMillis(),
                    zoneOffsetMinutes = zoneOffsetSource.offsetMinutes(),
                    // The repeat carries its source's provenance (ROADMAP N58): a repeated *Push A* is
                    // still a Push A, so history goes on saying which workout it was instead of showing
                    // an unnamed session. It is provenance and not prescription, exactly as it is for a
                    // template start (P3.3), and the targets are not copied with it.
                    templateId = source.templateId,
                )
                if (start.created) {
                    // The same append path every other start uses (ROADMAP N3, N29), so a repeated
                    // exercise is an ordinary one — its rest, note and grouping rules included.
                    database.sessionExerciseDao().findLiveSessionExercises(sessionId)
                        .forEach { past ->
                            appendExercise(
                                dao = dao,
                                now = timeSource.nowEpochMillis(),
                                sessionId = start.session.id,
                                exerciseId = past.exerciseId,
                                // The three the comment above promises, now actually carried: a
                                // repeated superset stays a superset (ROADMAP B41).
                                restSeconds = past.restSeconds,
                                techniqueNote = past.techniqueNote,
                                supersetGroup = past.supersetGroup,
                            )
                        }
                }
                StartedSession(id = start.session.id, isNew = start.created)
            }
        }

    override suspend fun addExercise(sessionId: String, exerciseId: String): DataResult<Unit> =
        dataResultOf {
            if (dao.findSession(sessionId) == null) {
                throw NotFoundException("session $sessionId is not open")
            }
            appendExercise(dao, timeSource.nowEpochMillis(), sessionId, exerciseId)
        }

    override suspend fun removeExercise(sessionExerciseId: String): DataResult<Unit> =
        dataResultOf {
            val now = timeSource.nowEpochMillis()
            val updated = dao.softDeleteSessionExercise(id = sessionExerciseId, at = now)
            if (updated == 0) throw NotFoundException("session exercise $sessionExerciseId")
            // The joint rows belong to the exercise, so they go with it (N63) — the reader already
            // hides a removed exercise's joints, and this keeps the stored state saying the same.
            jointDao.softDeleteForExercise(sessionExerciseId = sessionExerciseId, at = now)
        }

    override suspend fun moveExercise(sessionExerciseId: String, delta: Int): DataResult<Unit> =
        dataResultOf {
            // The row's own session is read from the row, so the sibling list and the swap cannot
            // disagree about which list is being reordered (ROADMAP N54).
            val exercise = sessionExerciseDao.findSessionExercise(sessionExerciseId)
                ?: throw NotFoundException("session exercise $sessionExerciseId")
            val ordered = sessionExerciseDao.findSessionExercisesInOrder(exercise.sessionId)
            val index = ordered.indexOfFirst { it.id == sessionExerciseId }
            val neighbour = ordered.getOrNull(index + delta)
            // At the top or the bottom: nothing to do, and not an error.
            if (index >= 0 && neighbour != null) {
                sessionExerciseDao.swapPositions(
                    firstId = exercise.id,
                    firstPosition = neighbour.position,
                    secondId = neighbour.id,
                    secondPosition = exercise.position,
                    at = timeSource.nowEpochMillis(),
                )
            }
        }

    override suspend fun finishExercise(sessionExerciseId: String): DataResult<Unit> = dataResultOf {
        // The session is read first so the rest can be cleared on the same row that
        // owns the exercise — ending a lift must not leave a timer armed for a break
        // the user has finished with (ROADMAP N7).
        val sessionId = dao.findSessionIdForSessionExercise(sessionExerciseId)
            ?: throw NotFoundException("session exercise $sessionExerciseId")
        val now = timeSource.nowEpochMillis()
        if (dao.setSessionExerciseFinished(id = sessionExerciseId, finishedAt = now, at = now) == 0) {
            throw NotFoundException("session exercise $sessionExerciseId")
        }
        dao.updateRestTimer(id = sessionId, restEndsAt = null, at = now)
    }

    override suspend fun setWorkoutNotes(sessionId: String, note: String?): DataResult<Unit> =
        dataResultOf {
            // Blank is stored as null rather than "": two representations of
            // "nothing" would show up differently on screen, the same rule the
            // readiness note follows.
            val updated = dao.setWorkoutNotes(
                id = sessionId,
                notes = note?.trim()?.ifEmpty { null },
                at = timeSource.nowEpochMillis(),
            )
            if (updated == 0) throw NotFoundException("session $sessionId")
        }

    override suspend fun reopenExercise(sessionExerciseId: String): DataResult<Unit> = dataResultOf {
        val updated = dao.setSessionExerciseFinished(
            id = sessionExerciseId,
            finishedAt = null,
            at = timeSource.nowEpochMillis(),
        )
        if (updated == 0) throw NotFoundException("session exercise $sessionExerciseId")
    }

    override suspend fun rateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<JointPain>,
    ): DataResult<Unit> = dataResultOf {
        // Both halves sit on the same 1–10 scale as a set's RPE, and both are skippable, so the one
        // validator covers them (ROADMAP N8, N63). Checked before any write, so a bad score cannot
        // leave a save half-landed.
        if (!TenPointScale.isValid(muscleFeel) || joints.any { !TenPointScale.isValid(it.score) }) {
            throw InvalidInputException(
                "Ratings must be between ${TenPointScale.MIN} and ${TenPointScale.MAX}.",
            )
        }
        val now = timeSource.nowEpochMillis()
        // The write retires the legacy `jointPain` / `jointPainNote` columns (N63). A new rating
        // replaces the old one, and both readers fall back to those columns when the picked list is
        // empty — so leaving them would resurrect the number a cleared list just removed. A session
        // nobody re-rates keeps them, which is what "history still reads them" means.
        val updated = dao.setSessionExerciseRating(
            id = sessionExerciseId,
            muscleFeel = muscleFeel,
            at = now,
        )
        if (updated == 0) throw NotFoundException("session exercise $sessionExerciseId")

        // A save **replaces** the list (N63): the editor shows exactly what is stored, so a joint
        // missing from it was removed by the lifter. The same shape the readiness write uses — the
        // exercise write above has already refused a dead row, so the only failure window hides the
        // old list and shows the new one empty rather than mixing the two.
        jointDao.softDeleteForExercise(sessionExerciseId = sessionExerciseId, at = now)
        if (joints.isNotEmpty()) {
            jointDao.insertAll(
                joints.mapIndexed { index, joint ->
                    SessionExerciseJointEntity(
                        id = UUID.randomUUID().toString(),
                        sessionExerciseId = sessionExerciseId,
                        joint = joint.joint,
                        side = joint.side,
                        score = joint.score,
                        position = index,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                    )
                },
            )
        }
    }

    override suspend fun finishSession(sessionId: String): DataResult<Unit> = dataResultOf {
        // Finishing also stops any running rest: leaving a timer armed on a closed
        // session would fire a notification for a workout that is over.
        val now = timeSource.nowEpochMillis()
        if (dao.markFinished(id = sessionId, at = now) == 0) {
            throw NotFoundException("session $sessionId")
        }
        dao.updateRestTimer(id = sessionId, restEndsAt = null, at = now)
    }

    override suspend fun setReadiness(
        sessionId: String,
        note: String?,
        soreMuscles: List<SoreMuscle>,
    ): DataResult<Unit> = dataResultOf {
        // The same 1–10 scale as a set's RPE and the per-exercise ratings, so the one validator
        // covers them (ROADMAP N8, N62). Checked before any write, so a bad score cannot leave a
        // save half-landed.
        if (soreMuscles.any { !TenPointScale.isValid(it.score) }) {
            throw InvalidInputException(
                "A sore-muscle score must be between ${TenPointScale.MIN} and ${TenPointScale.MAX}.",
            )
        }
        // Blank is stored as null rather than "": two representations of "nothing
        // written" would show up differently on the workout header.
        val cleaned = note?.trim()?.ifEmpty { null }
        val now = timeSource.nowEpochMillis()
        val updated = dao.updateReadinessNote(id = sessionId, note = cleaned, at = now)
        if (updated == 0) throw NotFoundException("session $sessionId")

        // A save **replaces** the list (N62): the editor shows exactly what is stored, so a muscle
        // missing from it was removed by the lifter. The two statements are not wrapped in a
        // transaction, the shape `deleteSession` already uses — the session write above has already
        // refused a dead session, so the only failure window hides the old list and shows the new
        // one empty rather than mixing the two.
        soreMuscleDao.softDeleteForSession(sessionId = sessionId, at = now)
        if (soreMuscles.isNotEmpty()) {
            soreMuscleDao.insertAll(
                soreMuscles.mapIndexed { index, sore ->
                    SessionSoreMuscleEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        muscle = sore.muscle,
                        score = sore.score,
                        position = index,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                    )
                },
            )
        }
    }

    override suspend fun deleteSession(sessionId: String): DataResult<Unit> = dataResultOf {
        if (dao.findSession(sessionId) == null) {
            throw NotFoundException("session $sessionId")
        }
        // Children first, then the parent, so a partial failure cannot leave the
        // session invisible but its exercises still live.
        val now = timeSource.nowEpochMillis()
        // The sore-muscle rows go with the session they describe (N62): leaving them live would
        // show a deleted workout's soreness to the restore the export offers.
        soreMuscleDao.softDeleteForSession(sessionId = sessionId, at = now)
        // And the joint rows go with the exercise they describe (N63), for the same reason.
        jointDao.softDeleteForSession(sessionId = sessionId, at = now)
        dao.softDeleteSessionExercises(sessionId = sessionId, at = now)
        dao.softDeleteSession(id = sessionId, at = now)
    }

    override suspend fun logSet(
        sessionExerciseId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
        assistanceGrams: Long,
    ): DataResult<Unit> = dataResultOf {
        // The screen's field is the real guard; this is the boundary that keeps an
        // out-of-range value from reaching the database (ROADMAP N6).
        if (!Rpe.isValid(rpeHalves)) {
            throw InvalidInputException("RPE must be between 1 and 10, in half steps.")
        }
        // A stale screen can hold an id for an exercise that was removed, or whose
        // session was already finished. Writing anyway would file the set under
        // history the user cannot reach, so this fails as NotFound instead.
        if (dao.countLoggableSessionExercise(sessionExerciseId) == 0) {
            throw NotFoundException("session exercise $sessionExerciseId is not loggable")
        }
        val now = timeSource.nowEpochMillis()
        dao.insertSet(
            SetEntryEntity(
                id = UUID.randomUUID().toString(),
                sessionExerciseId = sessionExerciseId,
                setIndex = dao.maxSetIndex(sessionExerciseId) + 1,
                // A set of zero reps is not a set. Clamping here rather than
                // trusting the caller keeps a mistyped field out of the history.
                reps = reps.coerceAtLeast(1),
                weightGrams = weightGrams.coerceAtLeast(0L),
                // A magnitude, so a negative arriving here becomes none rather than
                // a weight that subtracts (ROADMAP N15).
                assistanceGrams = assistanceGrams.coerceAtLeast(0L),
                setType = setType,
                // What the set felt like, written with the set rather than after it: the
                // inline fields are on screen before *Log set*, so they are part of what it
                // commits (N59), and an undo puts back what the deleted row carried (N6).
                rpeHalves = rpeHalves,
                // A cleared comment is null, not "": one representation of nothing.
                note = note?.trim()?.ifEmpty { null },
                completedAt = now,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    override suspend fun updateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
        assistanceGrams: Long,
    ): DataResult<Unit> =
        dataResultOf {
            // The editor's field is the real guard; this is the boundary that keeps
            // an out-of-range value from reaching the database (ROADMAP N6).
            // RPE takes halves; the feel and pain ratings above do not (ROADMAP N6).
            if (!Rpe.isValid(rpeHalves)) {
                throw InvalidInputException("RPE must be between 1 and 10, in half steps.")
            }

            // Read the stored row first, so the columns an edit does not touch
            // (setIndex, completedAt, createdAt) are preserved rather than invented,
            // and so a soft-deleted set cannot be resurrected by an edit.
            val stored = dao.findSetById(setId) ?: throw NotFoundException("set $setId")
            val updated = stored.copy(
                reps = reps.coerceAtLeast(1),
                weightGrams = weightGrams.coerceAtLeast(0L),
                rpeHalves = rpeHalves,
                // The role is part of what the set was (ROADMAP N14), and so is
                // what the machine took off (N15).
                setType = setType,
                assistanceGrams = assistanceGrams.coerceAtLeast(0L),
                // A cleared comment is null, not "": one representation of nothing.
                note = note?.trim()?.ifEmpty { null },
                updatedAt = timeSource.nowEpochMillis(),
            )
            if (dao.updateSet(updated) == 0) throw NotFoundException("set $setId")
        }

    override suspend fun deleteSet(setId: String): DataResult<Unit> = dataResultOf {
        val updated = dao.softDeleteSet(id = setId, at = timeSource.nowEpochMillis())
        if (updated == 0) throw NotFoundException("set $setId")
    }

    override suspend fun previousPerformance(
        exerciseId: String,
        currentSessionId: String,
    ): DataResult<PreviousPerformance> = dataResultOf {
        // Two plain queries composed here rather than one correlated subquery:
        // this is the read a user waits on before their first set, and it is far
        // easier to reason about — and to test — as two obvious steps.
        val previousSessionId = dao.findPreviousSessionIdFor(exerciseId, currentSessionId)
            ?: return@dataResultOf PreviousPerformance(emptyList())

        PreviousPerformance(
            sets = dao.findSetsFor(previousSessionId, exerciseId).map { it.toDomain() },
        )
    }

    override suspend fun personalRecords(
        exerciseId: String,
        excludingSessionId: String?,
    ): DataResult<PersonalRecords> = dataResultOf {
        // Every session, because a record is against all of them — the window that suits a
        // chart would silently forget an old best, which is the one thing a record is for.
        val rows = database.trendsDao().observeExerciseTrendRows(exerciseId, limit = ALL_SESSIONS).first()
        PersonalRecords.from(
            rows.filter { it.sessionId != excludingSessionId }
                .mapNotNull { it.toPerformedSetSpec() },
        )
    }

    override suspend fun setSupersetGroup(
        sessionExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> = dataResultOf {
        if (sessionExerciseIds.isEmpty()) return@dataResultOf

        // One statement, so the group moves together or not at all (ROADMAP B27). A row-per-call
        // loop could fail or be killed between writes and leave half a superset — the state the
        // screen's own comment says nobody asked for.
        val updated = database.sessionExerciseDao()
            .setSupersetGroup(sessionExerciseIds, group, timeSource.nowEpochMillis())
        if (updated != sessionExerciseIds.size) {
            // Reported rather than skipped: the caller asked for all of them, and a partial write
            // that looks successful is worse than a failure it can show.
            throw NotFoundException("$updated of ${sessionExerciseIds.size} session exercises were written")
        }
    }

    override suspend fun startRest(seconds: Int): DataResult<Instant> = dataResultOf {
        val session = dao.findActiveSession() ?: throw NotFoundException("no active session")
        val now = timeSource.now()
        val endsAt = now.plusSeconds(seconds.coerceAtLeast(0).toLong())
        if (dao.updateRestTimer(session.id, endsAt.toEpochMilli(), now.toEpochMilli()) == 0) {
            throw NotFoundException("session ${session.id}")
        }
        endsAt
    }

    override suspend fun adjustRest(deltaSeconds: Int): DataResult<Instant> = dataResultOf {
        val session = dao.findActiveSession() ?: throw NotFoundException("no active session")
        val now = timeSource.now()
        // Adding time to a rest that already ended restarts it from now rather
        // than computing a moment in the past.
        val base = session.restEndsAt?.let(Instant::ofEpochMilli) ?: now
        val moved = base.plusSeconds(deltaSeconds.toLong())
        val endsAt = if (moved.isAfter(now)) moved else now
        if (dao.updateRestTimer(session.id, endsAt.toEpochMilli(), now.toEpochMilli()) == 0) {
            throw NotFoundException("session ${session.id}")
        }
        endsAt
    }

    override suspend fun clearRest(): DataResult<Unit> = dataResultOf {
        val session = dao.findActiveSession() ?: throw NotFoundException("no active session")
        if (dao.updateRestTimer(session.id, null, timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("session ${session.id}")
        }
    }
}

/** A stand-in for "all of them": no exercise has anywhere near this many sessions. */
private const val ALL_SESSIONS = 100_000

/**
 * A history row as the record rule's input, or null when the row recorded no set.
 *
 * The projection keeps `setType` as its stored name (a database column, not an entity), so
 * the name is resolved here; an unknown name is read as a working set rather than dropped,
 * because silently discarding a set would make a record easier to beat than it is.
 */
private fun ExerciseTrendRowEntity.toPerformedSetSpec(): PerformedSetSpec? {
    val reps = reps ?: return null
    return PerformedSetSpec(
        role = SetType.entries.firstOrNull { it.name == setType } ?: SetType.NORMAL,
        weightGrams = weightGrams ?: 0L,
        assistanceGrams = assistanceGrams ?: 0L,
        reps = reps,
    )
}

/**
 * The one place a session exercise is appended, shared by a manual "add exercise", a template and a
 * repeat of the last workout (ROADMAP N3, N29) — so a repeated exercise is an ordinary one, carrying
 * the same defaults and the same position arithmetic.
 *
 * A file-level function rather than a private member: `RoomWorkoutRepository` is at detekt's ceiling
 * for class functions, and the next thing added to it should force a real split rather than another
 * helper moving out.
 */
private suspend fun appendExercise(
    dao: WorkoutDao,
    now: Long,
    sessionId: String,
    exerciseId: String,
    restSeconds: Int? = null,
    techniqueNote: String? = null,
    supersetGroup: Int? = null,
) {
    dao.insertSessionExercise(
        SessionExerciseEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            exerciseId = exerciseId,
            position = dao.maxPosition(sessionId) + 1,
            restSeconds = restSeconds,
            techniqueNote = techniqueNote,
            supersetGroup = supersetGroup,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        ),
    )
}
