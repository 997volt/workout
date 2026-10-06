package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.repository.StartedSession
import com.example.androidapp.domain.repository.WorkoutRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * A hand-written [WorkoutRepository] for the screens that only read a session (ROADMAP N78).
 *
 * Extracted at its second caller: the exercise picker needed one that records what it added, and the
 * template list needs the same repository for one question — is a workout already running. It answers
 * [observeActiveSession] and refuses everything else, which is what keeps a screen from quietly
 * depending on a method its test never set up.
 */
internal class FakeWorkoutRepository : WorkoutRepository {
    val session = MutableStateFlow<WorkoutSession?>(
        WorkoutSession(id = "s1", startedAt = Instant.parse("2026-09-29T07:00:00Z")),
    )
    val added = mutableListOf<Pair<String, String?>>()
    var failAdds = false

    override fun observeActiveSession(): Flow<WorkoutSession?> = session
    override suspend fun addExercise(sessionId: String, exerciseId: String): DataResult<Unit> {
        if (failAdds) return DataResult.Failure(DataError.Storage(IOException("disk full")))
        added += sessionId to exerciseId
        return DataResult.Success(Unit)
    }

    override fun observeSessionExercises(sessionId: String): Flow<List<SessionExercise>> =
        flowOf(emptyList())

    override fun observeSets(sessionId: String): Flow<List<SetEntry>> = flowOf(emptyList())
    override fun observeHistory(): Flow<List<WorkoutSummary>> = flowOf(emptyList())
    override fun observeSession(sessionId: String): Flow<WorkoutSession?> = flowOf(null)
    override suspend fun startOrResumeSession(
        templateId: String?,
    ): DataResult<StartedSession> = unused()

    /** The one fake that models repeating: it records the call, since its tests ask what it did. */
    override suspend fun repeatSession(sessionId: String): DataResult<StartedSession> = unused()
    override suspend fun removeExercise(sessionExerciseId: String): DataResult<Unit> = unused()
    override suspend fun moveExercise(sessionExerciseId: String, delta: Int): DataResult<Unit> = unused()
    override suspend fun finishExercise(sessionExerciseId: String): DataResult<Unit> = unused()
    override suspend fun reopenExercise(sessionExerciseId: String): DataResult<Unit> = unused()
    override suspend fun rateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<JointPain>,
    ): DataResult<Unit> = unused()
    override suspend fun personalRecords(
        exerciseId: String,
        excludingSessionId: String?,
    ): DataResult<PersonalRecords> = DataResult.Success(records)

    /** Every group write, as the ids it covered — one entry per call, which is the point. */
    val supersetGroups = mutableListOf<List<String>>()

    /** What the record read answers with; empty means no records yet. */
    var records: PersonalRecords = PersonalRecords()

    override suspend fun setSupersetGroup(
        sessionExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> {
        supersetGroups += sessionExerciseIds
        return DataResult.Success(Unit)
    }

    override suspend fun finishSession(sessionId: String): DataResult<Unit> = unused()
    override suspend fun setReadiness(
        sessionId: String,
        note: String?,
        soreMuscles: List<com.example.androidapp.domain.model.SoreMuscle>,
    ): DataResult<Unit> = unused()
    override suspend fun setWorkoutNotes(sessionId: String, note: String?): DataResult<Unit> = unused()
    override suspend fun deleteSession(sessionId: String): DataResult<Unit> = unused()
    override suspend fun logSet(
        sessionExerciseId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
    assistanceGrams: Long,
    ): DataResult<Unit> = unused()
    override suspend fun updateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
    assistanceGrams: Long,
    ): DataResult<Unit> = unused()
    override suspend fun deleteSet(setId: String): DataResult<Unit> = unused()
    override suspend fun previousPerformance(
        exerciseId: String,
        currentSessionId: String,
    ): DataResult<PreviousPerformance> = unused()
    override suspend fun startRest(seconds: Int): DataResult<Instant> = unused()
    override suspend fun adjustRest(deltaSeconds: Int): DataResult<Instant> = unused()
    override suspend fun clearRest(): DataResult<Unit> = unused()

    private fun unused(): Nothing = error("the picker must not call this")
}
