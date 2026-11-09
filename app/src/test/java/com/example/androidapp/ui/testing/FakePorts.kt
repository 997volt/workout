package com.example.androidapp.ui.testing

import android.content.Context
import android.net.Uri
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.LatestWeight
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.SequenceSchedule
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.StatisticsRange
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.DeloadCreation
import com.example.androidapp.domain.repository.DeloadDayInput
import com.example.androidapp.domain.repository.DeloadRepository
import com.example.androidapp.domain.repository.ExerciseRepository
import com.example.androidapp.domain.repository.MeasurementRepository
import com.example.androidapp.domain.repository.MergeSummary
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.repository.StartedSession
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.repository.WorkoutRepository
import com.example.androidapp.platform.DocumentIo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The ports a ViewModel test hands in, written by hand.
 *
 * There is no mocking framework in this project (AGENTS.md), and these are the narrowest implementations
 * the orchestration tests need: what a test drives is a real [MutableStateFlow], and everything the test
 * does not exercise answers with an error rather than something plausible. A method that quietly returns
 * an empty list would let a ViewModel read a source the test never set up, which is how a wiring test
 * passes without proving the wire.
 *
 * Shared rather than copied into each test because four of these tests read the same four ports; they live
 * here instead of in one test's file so the next ViewModel test can reuse them. Nothing here asserts: a
 * fake only records and answers.
 */

/** The workout port, recording the destructive write the Home handlers own. */
internal class FakeWorkoutRepository : WorkoutRepository {

    val activeSession = MutableStateFlow<WorkoutSession?>(null)
    val sessionExercises = MutableStateFlow<List<SessionExercise>>(emptyList())
    val history = MutableStateFlow<List<WorkoutSummary>>(emptyList())

    /** Every session [deleteSession] was asked to remove, in order. */
    val deletedSessions = mutableListOf<String>()

    /** What [deleteSession] answers, so a test can drive the failure branch. */
    var deleteResult: DataResult<Unit> = DataResult.Success(Unit)

    override fun observeActiveSession(): Flow<WorkoutSession?> = activeSession

    override fun observeSessionExercises(sessionId: String): Flow<List<SessionExercise>> = sessionExercises

    override fun observeHistory(): Flow<List<WorkoutSummary>> = history

    override fun observeSets(sessionId: String): Flow<List<SetEntry>> = unused()

    override fun observeSession(sessionId: String): Flow<WorkoutSession?> = unused()

    override suspend fun startOrResumeSession(templateId: String?): DataResult<StartedSession> = unused()

    override suspend fun repeatSession(sessionId: String): DataResult<StartedSession> = unused()

    override suspend fun addExercise(sessionId: String, exerciseId: String): DataResult<Unit> = unused()

    override suspend fun removeExercise(sessionExerciseId: String): DataResult<Unit> = unused()

    override suspend fun moveExercise(sessionExerciseId: String, delta: Int): DataResult<Unit> = unused()

    override suspend fun finishExercise(sessionExerciseId: String): DataResult<Unit> = unused()

    override suspend fun setWorkoutNotes(sessionId: String, note: String?): DataResult<Unit> = unused()

    override suspend fun setActivityMetrics(

        sessionId: String,

        durationSeconds: Int?,

        distanceMeters: Long?,

    ): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun setSessionActivity(
        sessionId: String,
        activity: com.example.androidapp.domain.model.Activity?,
    ): DataResult<Unit> = unused()

    override suspend fun substituteExercise(sessionExerciseId: String, exerciseId: String): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun reopenExercise(sessionExerciseId: String): DataResult<Unit> = unused()

    override suspend fun declineProgression(
        sessionExerciseId: String,
        directions: Set<ProgressionDirection>,
    ): DataResult<Unit> = unused()

    override suspend fun rateExercise(
        sessionExerciseId: String,
        muscleFeel: Int?,
        joints: List<com.example.androidapp.domain.model.JointPain>,
    ): DataResult<Unit> = unused()

    override suspend fun finishSession(sessionId: String): DataResult<Unit> = unused()

    override suspend fun moveSessionToDay(sessionId: String, day: LocalDate): DataResult<Unit> = unused()

    override suspend fun setReadiness(sessionId: String, note: String?): DataResult<Unit> = unused()

    override suspend fun deleteSession(sessionId: String): DataResult<Unit> {
        deletedSessions += sessionId
        return deleteResult
    }

    override suspend fun logSet(
        sessionExerciseId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
        assistanceGrams: Long,
        bodyweightGrams: Long?,
        durationSeconds: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun updateSet(
        setId: String,
        reps: Int,
        weightGrams: Long,
        rpeHalves: Int?,
        note: String?,
        setType: SetType,
        assistanceGrams: Long,
        durationSeconds: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun deleteSet(setId: String): DataResult<Unit> = unused()

    override suspend fun previousPerformance(
        exerciseId: String,
        currentSessionId: String?,
    ): DataResult<PreviousPerformance> = unused()

    override suspend fun startRest(seconds: Int): DataResult<Instant> = unused()

    override suspend fun setSupersetGroup(
        sessionExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun personalRecords(
        exerciseId: String,
        excludingSessionId: String?,
    ): DataResult<PersonalRecords> = unused()

    override suspend fun adjustRest(deltaSeconds: Int): DataResult<Instant> = unused()

    override suspend fun clearRest(): DataResult<Unit> = unused()
}

/** The program port: the reads are flows a test sets, the writes it inspects are recorded. */
internal class FakeProgramRepository : ProgramRepository {

    val program = MutableStateFlow<WorkoutProgram?>(null)
    val activePrograms = MutableStateFlow<List<WorkoutProgram>>(emptyList())
    val slots = MutableStateFlow<List<ProgramSlot>>(emptyList())
    val run = MutableStateFlow<ProgramRun?>(null)
    val sequenceSchedule = MutableStateFlow<SequenceSchedule?>(null)

    var exportResult: DataResult<String> = DataResult.Success("{}")
    var renameResult: DataResult<Unit> = DataResult.Success(Unit)
    var deleteResult: DataResult<Unit> = DataResult.Success(Unit)
    var writeResult: DataResult<Unit> = DataResult.Success(Unit)

    /** Every rename as the id and the new name, in the order the ViewModel asked. */
    val renames = mutableListOf<Pair<String, String>>()
    val deletions = mutableListOf<String>()

    /** Every substitution as the slot, its week and the stand-in (null is the restore). */
    val substitutions = mutableListOf<Triple<String, LocalDate, String?>>()

    /** Every skipped block session and the day the ViewModel dated it. */
    val blockSkips = mutableListOf<Pair<String, LocalDate>>()

    /** Every day added, as the template and the week it was authored in (ROADMAP N292). */
    val addedSlots = mutableListOf<Triple<String, String, Int?>>()

    /** Every removed phase week as the program and the week (ROADMAP N292). */
    val removedWeeks = mutableListOf<Pair<String, Int>>()

    override fun observePrograms(): Flow<List<WorkoutProgram>> = unused()

    override fun observeProgram(programId: String): Flow<WorkoutProgram?> = program

    override fun observeActivePrograms(): Flow<List<WorkoutProgram>> = activePrograms

    override fun observeSlots(programId: String): Flow<List<ProgramSlot>> = slots

    override fun observeProgramRun(programId: String): Flow<ProgramRun?> = run

    override fun observeSequenceSchedule(programId: String): Flow<SequenceSchedule?> = sequenceSchedule

    override suspend fun createProgram(name: String): DataResult<String> = unused()

    override suspend fun renameProgram(programId: String, name: String): DataResult<Unit> {
        renames += programId to name
        return renameResult
    }

    override suspend fun setProgramKind(programId: String, kind: ProgramKind): DataResult<Unit> = unused()

    override suspend fun setProgramStartDate(
        programId: String,
        startDate: LocalDate?,
    ): DataResult<Unit> = unused()

    override suspend fun deleteProgram(programId: String): DataResult<Unit> {
        deletions += programId
        return deleteResult
    }

    override suspend fun activateProgram(programId: String): DataResult<Unit> = unused()

    override suspend fun deactivateProgram(programId: String): DataResult<Unit> = unused()

    override suspend fun moveProgram(programId: String, delta: Int): DataResult<Unit> = unused()

    override suspend fun setSubstitution(
        slotId: String,
        weekStart: LocalDate,
        templateId: String?,
    ): DataResult<Unit> {
        substitutions += Triple(slotId, weekStart, templateId)
        return writeResult
    }

    override suspend fun addSlot(
        programId: String,
        templateId: String,
        weekday: DayOfWeek?,
        phaseWeek: Int?,
    ): DataResult<Unit> {
        addedSlots += Triple(programId, templateId, phaseWeek)
        return writeResult
    }

    override suspend fun removePhaseWeek(programId: String, week: Int): DataResult<Unit> {
        removedWeeks += programId to week
        return writeResult
    }

    override suspend fun setSlotWeekday(slotId: String, weekday: DayOfWeek?): DataResult<Unit> = unused()

    override suspend fun moveSlot(slotId: String, delta: Int): DataResult<Unit> = unused()

    override suspend fun removeSlot(slotId: String): DataResult<Unit> = unused()

    override suspend fun slotPreviousPerformance(
        slotId: String,
        exerciseId: String,
        currentSessionId: String,
        zone: ZoneId,
    ): DataResult<PreviousPerformance> = unused()

    override suspend fun pendingOccurrences(
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<PendingOccurrence>> = unused()

    override suspend fun skipOccurrences(
        slotIds: List<String>,
        weekStart: LocalDate,
    ): DataResult<Unit> = unused()

    override suspend fun skipBlockSession(slotId: String, day: LocalDate): DataResult<Unit> {
        blockSkips += slotId to day
        return writeResult
    }

    override suspend fun exportProgramDocument(programId: String): DataResult<String> = exportResult

    override suspend fun importProgramDocument(text: String): DataResult<ProgramImportSummary> = unused()

    override suspend fun importProgramDocumentCopy(
        text: String,
        name: String,
    ): DataResult<ProgramImportSummary> = unused()

    override suspend fun previewProgramDocument(text: String): DataResult<ProgramDocumentPreview> = unused()
}

/** The template port: names and their planned exercises, each read by template id. */
internal class FakeTemplateRepository : TemplateRepository {

    val templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())

    /** Planned exercises by template id, which is what a balance read flattens. */
    val exercises = MutableStateFlow<Map<String, List<TemplateExercise>>>(emptyMap())

    override fun observeTemplates(includeHidden: Boolean): Flow<List<WorkoutTemplate>> = templates

    override fun observeDeletedTemplates(): Flow<List<WorkoutTemplate>> = unused()

    override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> = unused()

    override fun observeVersions(templateId: String): Flow<List<WorkoutTemplate>> = unused()

    override fun observeTemplateName(templateId: String): Flow<String?> = unused()

    override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> =
        exercises.map { byTemplate -> byTemplate[templateId].orEmpty() }

    override fun observeSets(templateId: String): Flow<List<TemplateSet>> = unused()

    override suspend fun keepVersion(templateId: String): DataResult<Unit> = unused()

    override suspend fun createTemplate(name: String): DataResult<String> = unused()

    override suspend fun createTemplateFromSession(
        sessionId: String,
        name: String,
    ): DataResult<String> = unused()

    override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> = unused()

    override suspend fun setTemplateHidden(templateId: String, hidden: Boolean): DataResult<Unit> = unused()

    override suspend fun deleteTemplate(templateId: String): DataResult<Unit> = unused()

    override suspend fun restoreTemplate(templateId: String): DataResult<Unit> = unused()

    override suspend fun setSupersetGroup(
        templateExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit> = unused()

    override suspend fun removeExercise(templateExerciseId: String): DataResult<Unit> = unused()

    override suspend fun prependSets(
        templateExerciseId: String,
        edits: List<TemplateSetEdit>,
    ): DataResult<Unit> = unused()

    override suspend fun addSet(templateExerciseId: String, edit: TemplateSetEdit): DataResult<Unit> =
        unused()

    override suspend fun updateSet(templateSetId: String, edit: TemplateSetEdit): DataResult<Unit> =
        unused()

    override suspend fun removeSet(templateSetId: String): DataResult<Unit> = unused()

    override suspend fun restoreSet(templateSetId: String): DataResult<Unit> = unused()

    override suspend fun setExercisePlan(
        templateExerciseId: String,
        restSeconds: Int?,
        techniqueNote: String?,
        targetRpeHalves: Int?,
    ): DataResult<Unit> = unused()

    override suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit> = unused()
}

/** The library port, as the [DataResult] the balance read receives. */
internal class FakeExerciseRepository : ExerciseRepository {

    val library = MutableStateFlow<DataResult<List<Exercise>>>(DataResult.Success(emptyList()))

    override fun observeExercises(): Flow<DataResult<List<Exercise>>> = library

    override suspend fun getExercise(id: String): DataResult<Exercise?> = unused()

    override suspend fun getAllIncludingDeleted(): DataResult<List<Exercise>> = unused()

    override suspend fun sessionCounts(): DataResult<Map<String, Int>> = unused()

    override fun observeLastTrained(): Flow<DataResult<Map<String, Instant>>> = unused()

    override suspend fun createCustomExercise(name: String): DataResult<Exercise> = unused()

    override suspend fun createCategory(name: String): DataResult<Exercise> = unused()

    override suspend fun updateExercise(exercise: Exercise): DataResult<Unit> = unused()

    override suspend fun mergeExercises(sourceId: String, survivorId: String): DataResult<MergeSummary> =
        unused()
}

/** The body-measurement port, which home reads to decide whether today has an entry. */
internal class FakeMeasurementRepository : MeasurementRepository {

    val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())

    override fun observeAll(): Flow<List<BodyMeasurement>> = measurements

    override suspend fun save(measurement: BodyMeasurement): DataResult<Unit> = unused()

    override suspend fun latestWeight(): DataResult<LatestWeight?> = unused()

    override suspend fun setDailyWeight(at: Instant, grams: Long): DataResult<Unit> = unused()

    override suspend fun delete(id: String): DataResult<Unit> = unused()

    override suspend fun restore(id: String): DataResult<Unit> = unused()
}

/** The deload port, whose empty answer is the ordinary week home has always shown. */
internal class FakeDeloadRepository : DeloadRepository {

    val weeks = MutableStateFlow<List<DeloadWeek>>(emptyList())

    override fun observeWeekPlans(weekStart: LocalDate): Flow<List<DeloadWeek>> = weeks

    override suspend fun createDeload(
        programId: String,
        weekStart: LocalDate,
        days: List<DeloadDayInput>,
    ): DataResult<DeloadCreation> = unused()

    override suspend fun findLastPlan(programId: String, beforeWeek: LocalDate): DeloadWeek? = unused()
}

/** The settings port, holding the weight unit the shell follows. */
internal class FakeSettingsRepository : SettingsRepository {

    val weightUnit = MutableStateFlow(WeightUnit.KILOGRAMS)

    override fun observeWeightUnit(): Flow<WeightUnit> = weightUnit

    override fun observeDefaultRestSeconds(): Flow<Int> = unused()

    override suspend fun setDefaultRestSeconds(seconds: Int): DataResult<Unit> = unused()

    override fun observeRestCueEnabled(): Flow<Boolean> = unused()

    override suspend fun setRestCueEnabled(enabled: Boolean): DataResult<Unit> = unused()

    override fun observeKeepScreenOn(): Flow<Boolean> = unused()

    override suspend fun setKeepScreenOn(enabled: Boolean): DataResult<Unit> = unused()

    override fun observeRestTimerEnabled(): Flow<Boolean> = unused()

    override suspend fun setRestTimerEnabled(enabled: Boolean): DataResult<Unit> = unused()

    override fun observeProgressionMode(): Flow<ProgressionMode> = unused()

    override suspend fun setProgressionMode(mode: ProgressionMode): DataResult<Unit> = unused()

    override suspend fun setWeightUnit(unit: WeightUnit): DataResult<Unit> = unused()

    override fun observeStatisticsRange(): Flow<StatisticsRange> = unused()

    override suspend fun setStatisticsRange(range: StatisticsRange): DataResult<Unit> = unused()

    override fun observeRecentSeries(): Flow<List<String>> = unused()

    override suspend fun setRecentSeries(entries: List<String>): DataResult<Unit> = unused()

    override fun observeBackupSettings(): Flow<BackupSettings> = unused()

    override suspend fun setBackupEnabled(enabled: Boolean): DataResult<Unit> = unused()

    override suspend fun setBackupSchedule(schedule: BackupSchedule): DataResult<Unit> = unused()

    override suspend fun setBackupFolder(folder: BackupFolder?): DataResult<Unit> = unused()

    override suspend fun recordBackupResult(
        at: Instant?,
        attemptAt: Instant,
        failed: Boolean,
    ): DataResult<Unit> = unused()
}

/** The trends port: the stall finder asks it only for a plan that has slots, which these tests do not. */
internal class FakeTrendsRepository : com.example.androidapp.domain.repository.TrendsRepository {

    override fun observeExerciseTrends(
        exerciseIds: List<String>,
        limit: Int,
    ): Flow<DataResult<List<com.example.androidapp.domain.model.ExerciseTrendPoint>>> = unused()

    override fun observeTrends(
        limit: Int,
    ): Flow<DataResult<List<com.example.androidapp.domain.model.TrendPoint>>> = unused()

    override suspend fun templateSessions(
        templateId: String,
        limit: Int,
    ): DataResult<List<com.example.androidapp.domain.model.TemplateSession>> = unused()
}

/** The document port, proving a write without a content resolver. */
internal class FakeDocumentIo : DocumentIo {

    val written = mutableListOf<String>()
    var writeSucceeds = true

    override suspend fun write(context: Context, uri: Uri, text: String): Boolean {
        written += text
        return writeSucceeds
    }

    override suspend fun read(context: Context, uri: Uri): String? = unused()
}

/** A method the test never sets up is a bug in the test rather than a plausible empty answer. */
private fun unused(): Nothing = error("this port is not used by the orchestration under test")
