package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.BackupDao
import com.example.androidapp.data.local.ExerciseDao
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.ProgramDao
import com.example.androidapp.data.local.ProgramDeloadDao
import com.example.androidapp.data.local.ProgramEditDao
import com.example.androidapp.data.local.ProgramEntity
import com.example.androidapp.data.local.ProgramRunDao
import com.example.androidapp.data.local.ProgramSkipDao
import com.example.androidapp.data.local.ProgramSkipEntity
import com.example.androidapp.data.local.ProgramSlotEntity
import com.example.androidapp.data.local.ProgramSubstitutionDao
import com.example.androidapp.data.local.ProgramSubstitutionEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toProgramSession
import com.example.androidapp.data.local.toRunSession
import com.example.androidapp.data.transfer.ProgramDocument
import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.data.transfer.toDto
import com.example.androidapp.data.transfer.toEntity
import com.example.androidapp.data.transfer.toShared
import com.example.androidapp.data.transfer.withValidLibraryShape
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RecordedSkip
import com.example.androidapp.domain.model.RecordedSubstitution
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.programRun
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.shared.Change
import com.example.androidapp.shared.ChangeKind
import com.example.androidapp.shared.PlanResult
import com.example.androidapp.shared.ProblemCode
import com.example.androidapp.shared.ProgramProblem
import com.example.androidapp.shared.plan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [ProgramRepository] (ROADMAP P3.3).
 *
 * The occurrence arithmetic lives in [ProgramSchedule], which is pure: this class only
 * gathers the three sources it needs — the active program's slots, the week's sessions
 * started from a template, and the recorded skips — and hands them over. That keeps the
 * part with a real answer testable without a database.
 */
@Singleton
class RoomProgramRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : ProgramRepository {

    private val dao: ProgramDao = database.programDao()

    private val runDao: ProgramRunDao = database.programRunDao()

    private val skipDao: ProgramSkipDao = database.programSkipDao()

    private val substitutionDao: ProgramSubstitutionDao = database.programSubstitutionDao()

    private val deloadDao: ProgramDeloadDao = database.programDeloadDao()

    override fun observePrograms(): Flow<List<WorkoutProgram>> =
        dao.observePrograms().map { rows -> rows.map { it.toDomain() } }

    override fun observeProgram(programId: String): Flow<WorkoutProgram?> =
        dao.observeProgram(programId).map { it?.toDomain() }

    override fun observeActivePrograms(): Flow<List<WorkoutProgram>> =
        dao.observeActivePrograms().map { rows -> rows.map { it.toDomain() } }

    override fun observeSlots(programId: String): Flow<List<ProgramSlot>> =
        dao.observeSlotDetails(programId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun createProgram(name: String): DataResult<String> = dataResultOf {
        val trimmed = requireName(name)
        val id = UUID.randomUUID().toString()
        val now = timeSource.nowEpochMillis()
        dao.insertProgram(
            ProgramEntity(
                id = id,
                name = trimmed,
                // Following a program is a deliberate choice made in its editor, not a
                // side effect of creating one.
                isActive = false,
                // Appended, so the authored order is the order they were created in until
                // the user moves one (P3.12).
                position = dao.maxProgramPosition() + 1,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        id
    }

    override suspend fun renameProgram(programId: String, name: String): DataResult<Unit> =
        dataResultOf {
            val updated = dao.renameProgram(
                id = programId,
                name = requireName(name),
                at = timeSource.nowEpochMillis(),
            )
            if (updated == 0) throw NotFoundException("program $programId")
        }

    override suspend fun deleteProgram(programId: String): DataResult<Unit> = dataResultOf {
        if (dao.softDeleteProgram(id = programId, at = timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("program $programId")
        }
    }

    override suspend fun activateProgram(programId: String): DataResult<Unit> = dataResultOf {
        // No transaction and no clearing: more than one program may be active (P3.12), and
        // "which others are followed" is not this write's business.
        if (dao.setProgramActive(programId, isActive = true, at = timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("program $programId")
        }
    }

    override suspend fun deactivateProgram(programId: String): DataResult<Unit> = dataResultOf {
        if (dao.setProgramActive(programId, isActive = false, at = timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("program $programId")
        }
    }

    override suspend fun moveProgram(programId: String, delta: Int): DataResult<Unit> = dataResultOf {
        if (dao.findProgram(programId) == null) throw NotFoundException("program $programId")
        val ordered = dao.findPrograms()
        val index = ordered.indexOfFirst { it.id == programId }
        val target = index + delta
        // At the top or the bottom: nothing to do, and not an error.
        if (index >= 0 && target in ordered.indices) {
            // Re-number the whole list rather than swap two rows: a restore from a file written
            // before programs could be ordered leaves every position at 0, where a swap of two
            // equal values moves nothing (P3.12).
            val reordered = ordered.map { it.id }.toMutableList().apply { add(target, removeAt(index)) }
            dao.resequencePrograms(
                order = reordered.mapIndexed { position, id -> id to position },
                at = timeSource.nowEpochMillis(),
            )
        }
    }

    override suspend fun addSlot(
        programId: String,
        templateId: String,
        weekday: DayOfWeek?,
    ): DataResult<Unit> = dataResultOf {
        if (dao.findProgram(programId) == null) {
            throw NotFoundException("program $programId")
        }
        val template = database.templateDao().findById(templateId)
            ?: throw NotFoundException("template $templateId")
        val now = timeSource.nowEpochMillis()
        dao.insertSlot(
            ProgramSlotEntity(
                id = UUID.randomUUID().toString(),
                programId = programId,
                templateId = template.id,
                // Appended, so a program is built in the order it is trained.
                position = dao.maxSlotPosition(programId) + 1,
                weekday = weekday,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    override suspend fun setSlotWeekday(slotId: String, weekday: DayOfWeek?): DataResult<Unit> =
        dataResultOf {
            val updated = dao.setSlotWeekday(
                id = slotId,
                weekday = weekday,
                at = timeSource.nowEpochMillis(),
            )
            if (updated == 0) throw NotFoundException("program slot $slotId")
        }

    override suspend fun moveSlot(slotId: String, delta: Int): DataResult<Unit> = dataResultOf {
        val row = dao.findSlot(slotId) ?: throw NotFoundException("program slot $slotId")
        val ordered = dao.findSlotDetails(row.programId)
        val index = ordered.indexOfFirst { it.id == slotId }
        val neighbour = ordered.getOrNull(index + delta)
        // At the top or the bottom: nothing to do, and not an error.
        if (index >= 0 && neighbour != null) {
            dao.swapSlotPositions(
                firstId = row.id,
                firstPosition = neighbour.position,
                secondId = neighbour.id,
                secondPosition = row.position,
                at = timeSource.nowEpochMillis(),
            )
        }
    }

    override suspend fun removeSlot(slotId: String): DataResult<Unit> = dataResultOf {
        if (dao.softDeleteSlot(id = slotId, at = timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("program slot $slotId")
        }
    }

    override suspend fun slotPreviousPerformance(
        slotId: String,
        exerciseId: String,
        currentSessionId: String,
        zone: ZoneId,
    ): DataResult<PreviousPerformance> = dataResultOf {
        val slot = dao.findSlot(slotId) ?: throw NotFoundException("program slot $slotId")
        val slots = dao.findSlotDetails(slot.programId).map { it.toDomain() }
        val candidates = dao.finishedSessionsForTemplate(slot.templateId, currentSessionId)
            .map { it.toProgramSession(zone) }

        // A slot's own history is the P3.3 assignment: a session settles one occurrence, so two
        // slots naming one template each progress from the sessions that settled them.
        val settled = ProgramSchedule.sessionAssignments(slots, candidates)
            .lastOrNull { (_, occurrence) -> occurrence.slotId == slotId }
            ?: return@dataResultOf PreviousPerformance(emptyList())

        PreviousPerformance(
            sets = database.workoutDao().findSetsFor(settled.first.sessionId, exerciseId)
                .map { it.toDomain() },
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProgramRun(programId: String): Flow<ProgramRun?> =
        dao.observeSlotDetails(programId).flatMapLatest { rows ->
            val slots = rows.map { it.toDomain() }
            if (slots.isEmpty()) {
                flowOf(null)
            } else {
                val slotIds = slots.map { it.id }
                // The substitutions drive the session read: a session started from a stand-in
                // template trains that slot, so its template has to be read too (P3.11).
                substitutionDao.observeSubstitutionsForSlots(slotIds).flatMapLatest { substitutions ->
                    val templates = (slots.map { it.templateId } + substitutions.map { it.templateId }).distinct()
                    combine(
                        runDao.observeFinishedSessions(templates),
                        runDao.observeSkipsForSlots(slotIds),
                    ) { sessions, skips ->
                        // The zone is read when the data arrives rather than captured (B45); it is
                        // the fallback only for a session recorded before N25.
                        val zone = ZoneId.systemDefault()
                        programRun(
                            slots = slots,
                            sessions = sessions.mapNotNull { it.toRunSession(zone) },
                            skips = skips.map {
                                RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
                            },
                            substitutions = substitutions.map {
                                RecordedSubstitution(
                                    slotId = it.slotId,
                                    weekStart = LocalDate.ofEpochDay(it.weekStart),
                                    templateId = it.templateId,
                                )
                            },
                        )
                    }
                }
            }
        }

    override suspend fun pendingOccurrences(
        today: LocalDate,
        zone: ZoneId,
    ): DataResult<List<PendingOccurrence>> = dataResultOf {
        val active = dao.findActivePrograms()
        if (active.isEmpty()) return@dataResultOf emptyList()

        val weekStart = ProgramSchedule.weekStartOf(today)
        // A day of slack at each end: a session's week is taken in its own zone (N25), so a
        // workout performed near midnight elsewhere can fall in a week the device clock does
        // not name. The exact bucketing happens in ProgramSchedule, per session.
        val from = weekStart.minusDays(SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = weekStart.plusDays(DAYS_IN_WEEK + SLACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = dao.sessionsStartedBetween(from, to).map { it.toProgramSession(zone) }
        val skips = skipDao.findSkipsForWeek(weekStart.toEpochDay()).map {
            RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
        }
        val substitutions = substitutionDao
            .findSubstitutionsBetween(weekStart.toEpochDay(), weekStart.toEpochDay())
            .map {
                RecordedSubstitution(
                    slotId = it.slotId,
                    weekStart = LocalDate.ofEpochDay(it.weekStart),
                    templateId = it.templateId,
                )
            }

        // A built deload week has its own days, which are not asked about (B163): the week is exempt
        // and a day keyed to it has no slot a skip could be recorded against. A marked week with no plan
        // is P3.10's own shape and keeps asking about the slots.
        val builtWeeks = deloadDao.findDeloadDaysForWeek(weekStart.toEpochDay()).map { it.programId }.toSet()

        // The union of every active program's misses (P3.12). Each program is resolved on its
        // own slots — a session settles an occurrence by its template, so the same session list
        // answers for all of them — and the misses are merged earliest first, with the programs'
        // own order breaking a tie on the same day.
        val pending = mutableListOf<PendingOccurrence>()
        active.forEach { program ->
            if (program.id in builtWeeks) return@forEach
            pending += ProgramSchedule.pendingOccurrences(
                slots = dao.findSlotDetails(program.id).map { it.toDomain() },
                sessions = sessions,
                skips = skips,
                today = today,
                substitutions = substitutions,
            )
        }
        pending.sortedBy { it.date }
    }

    override suspend fun skipOccurrences(
        slotIds: List<String>,
        weekStart: LocalDate,
    ): DataResult<Unit> = dataResultOf {
        if (slotIds.isEmpty()) return@dataResultOf
        val epochDay = weekStart.toEpochDay()
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            slotIds.distinct().forEach { slotId ->
                // Idempotent: a second Continue on the same week must not record twice.
                if (skipDao.countSkips(slotId, epochDay) == 0) {
                    skipDao.insertSkip(
                        ProgramSkipEntity(
                            id = UUID.randomUUID().toString(),
                            slotId = slotId,
                            weekStart = epochDay,
                            createdAt = now,
                            updatedAt = now,
                            deletedAt = null,
                        ),
                    )
                }
            }
        }
    }

    override suspend fun setSubstitution(
        slotId: String,
        weekStart: LocalDate,
        templateId: String?,
    ): DataResult<Unit> = dataResultOf {
        val slot = dao.findSlot(slotId) ?: throw NotFoundException("program slot $slotId")
        val epochDay = weekStart.toEpochDay()
        val now = timeSource.nowEpochMillis()

        // Clearing restores the slot's own workout. Doing it to a week with nothing recorded is a
        // no-op rather than a failure: the state asked for is the state it is in.
        if (templateId == null || templateId == slot.templateId) {
            substitutionDao.softDeleteSubstitution(slotId, epochDay, now)
            return@dataResultOf
        }

        // The stand-in has to be a live template, or it would be invisible on every screen the
        // moment it was chosen — the same trap `addSlot` guards a slot against.
        if (database.templateDao().findById(templateId) == null) {
            throw NotFoundException("template $templateId")
        }

        val existing = substitutionDao.findSubstitution(slotId, epochDay)
        if (existing == null) {
            substitutionDao.insertSubstitution(
                ProgramSubstitutionEntity(
                    id = UUID.randomUUID().toString(),
                    slotId = slotId,
                    weekStart = epochDay,
                    templateId = templateId,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
        } else {
            substitutionDao.setSubstitutionTemplate(existing.id, templateId, now)
        }
    }

    override suspend fun exportProgramDocument(programId: String): DataResult<String> = dataResultOf {
        ProgramDocumentCodec.encode(buildProgramDocument(database, timeSource, programId))
    }

    override suspend fun importProgramDocument(text: String): DataResult<ProgramImportSummary> =
        dataResultOf {
            // Throws InvalidInputException for a file we cannot use, before touching anything: a
            // rejected document must leave the database exactly as it was.
            val document = ProgramDocumentCodec.decode(text)
            // One timestamp inside the transaction, so a restore and the rows it counts agree (B177).
            val now = timeSource.nowEpochMillis()
            database.withTransaction {
                // An edit is a different promise from a load, and the document says which it is (N171).
                if (document.edit != null) {
                    applyProgramEdit(database, timeSource, document, now)
                } else {
                    mergeProgramDocument(database, dao, document, now)
                }
            }
        }

    override suspend fun previewProgramDocument(text: String): DataResult<ProgramDocumentPreview> =
        dataResultOf {
            val document = ProgramDocumentCodec.decode(text)
            if (document.edit == null) {
                return@dataResultOf ProgramDocumentPreview.Add
            }
            val snapshot = buildProgramDocument(database, timeSource, document.program.id)
            when (val planned = plan(snapshot.toShared(), document.toShared())) {
                is PlanResult.Refused -> throw InvalidInputException(editRefusal(planned.problems))
                is PlanResult.Ready -> ProgramDocumentPreview.Edit(
                    programName = document.program.name,
                    changes = planned.changes,
                    warnings = planned.warnings,
                    sharedProgramNames = otherProgramsUsing(database, document),
                )
            }
        }

    private companion object {
        /** One day of slack at each end of the week, for sessions in another zone. */
        const val SLACK_DAYS = 1L

        const val DAYS_IN_WEEK = 7L
    }
}

/** Room reports an ignored insert — a row whose id was already present — as this rowid. */
private const val IGNORED_ROW = -1L

/**
 * One program's definition as a document (ROADMAP N47).
 *
 * File-level because the repository is at the function ceiling this project enforces, and because
 * this is a read that assembles a value rather than a decision the repository owns.
 */
private suspend fun buildProgramDocument(
    database: WorkoutDatabase,
    timeSource: TimeSource,
    programId: String,
): ProgramDocument {
    val programDao = database.programDao()
    val program = programDao.findProgram(programId) ?: throw NotFoundException("program $programId")
    val slots = programDao.findSlots(programId)
    val templateIds = slots.map { it.templateId }.distinct()
    val templates = templateIds.mapNotNull { database.templateDao().findById(it) }
    val templateExercises = templateIds.flatMap { database.templateDao().findTemplateExercises(it) }
    val templateSets = templateIds.flatMap { database.templateDao().findTemplateSets(it) }
    // The definition of every exercise the plan names, so a receiving device can create the ones it
    // has never seen — a user's own exercise id means nothing anywhere else (N47) — **and of every row above
    // them** (N103, N110). A movement's category is part of its definition: a document naming a movement
    // without its family would arrive with nothing to file it under, and once a row reads rather than copies
    // its taxonomy, nothing to read from.
    val exercises = withAncestors(
        ids = templateExercises.map { it.exerciseId }.distinct(),
        dao = database.exerciseDao(),
    )

    return ProgramDocument(
        formatVersion = ProgramDocumentCodec.CURRENT_FORMAT_VERSION,
        exportedAt = timeSource.nowEpochMillis(),
        program = program.toDto(),
        slots = slots.map { it.toDto() },
        templates = templates.map { it.toDto() },
        templateExercises = templateExercises.map { it.toDto() },
        templateSets = templateSets.map { it.toDto() },
        exercises = exercises.map { it.toDto() },
    )
}

/**
 * [ids] and every row above them in the library (ROADMAP N103, N110).
 *
 * The walk goes to the top rather than one level, because a **file** is not bound by the shape this app
 * writes and a chain that stopped early would be the same silent loss in a shorter file. A cycle is not a
 * shape this app writes either, but a document is a place one can arrive, so an id already collected ends the
 * walk (B92). A row the device does not hold is simply absent, which is what a receiving device's own miss
 * already means.
 */
private suspend fun withAncestors(ids: List<String>, dao: ExerciseDao): List<ExerciseEntity> {
    val found = linkedMapOf<String, ExerciseEntity>()
    val pending = ArrayDeque(ids.distinct())
    while (pending.isNotEmpty()) {
        val id = pending.removeFirst()
        // `found[id]` first, so a cycle costs no second read and no jump out of the loop: a row already
        // collected has a non-null value here and adds nothing.
        val row = found[id] ?: dao.findById(id)
        if (row != null && found.put(id, row) == null) {
            row.parentId?.let { pending.add(it) }
        }
    }
    return found.values.toList()
}

/**
 * Adds what [document] holds and overwrites nothing (ROADMAP N47).
 *
 * Every insert ignores a row whose id is already present, so loading one file twice is a no-op
 * rather than a second copy and a document can never cost the user a program they wrote. Two
 * defences are deliberate: the imported program arrives **inactive** at the **end** of the list,
 * because following a program is a choice rather than something a file makes, and a movement whose
 * exercise is neither carried nor present is dropped rather than allowed to fail the foreign key
 * and roll the whole document back — one missing exercise is not a reason to refuse a program.
 *
 * **A row the device holds but has soft-deleted counts as absent** (B51). The presence read filters
 * `deletedAt` and the insert ignores an id that is already there, so such an exercise is neither
 * carried nor found and its movements are dropped. Dropping is the deliberate answer rather than
 * restoring it: un-deleting a lift is a write to the user's library, which is what "overwrites
 * nothing" refuses, and the load's sentence says "not in your library" so that it is true whether
 * the row is absent or merely hidden.
 *
 * **The program itself is the exception** (B177). A deleted program keeps its id, so loading its
 * document again would ignore every row of it and the card would answer "already here" to a program
 * that is not there. The document is a request for *that program*, so its own row is un-hidden —
 * visibility alone, never the name, position or follow state — and the workouts it points at were
 * never deleted with it. A deleted *exercise* is still not resurrected: it is shared library content,
 * not the program the document is about.
 *
 * **A live program under the document's id ends the merge there** (B180). A sheet-derived document's
 * id is a slug of its name, so a changed sheet keeps the id; merging it row by row would attach the
 * new day's template and slot to the program the lifter already has — an edit, which "creates and
 * never edits" refuses, and precisely what P3.18's "a changed sheet re-imported does nothing" says
 * cannot happen. The program is the unit of a document, not each row.
 */
private suspend fun mergeProgramDocument(
    database: WorkoutDatabase,
    programDao: ProgramDao,
    document: ProgramDocument,
    now: Long,
): ProgramImportSummary {
    val backupDao = database.backupDao()
    val programBackup = database.programBackupDao()

    // Live means this exact program is already here; nothing in the document is new (B180). The read
    // filters `deletedAt`, so a deleted one falls through to the restore below rather than stopping here.
    if (programDao.findProgram(document.program.id) != null) {
        return ProgramImportSummary(programs = 0, templates = 0, exercises = 0, droppedMovements = 0)
    }

    // One statement, before the inserts: a hidden program comes back, a live or absent one is
    // untouched (only rows with `deletedAt` set are updated), and the count says which happened.
    val programsRestored = programBackup.restoreProgramVisibility(document.program.id, now)

    // The library's shape is enforced on the way in (B92): a document can carry a parent link the app would
    // never write, and a movement filed under a head the document does not carry keeps the link (the head may
    // exist here) while an illegal one is cleared.
    val exercisesAdded = backupDao.insertExercises(
        document.exercises.withValidLibraryShape().map { it.toEntity() },
    ).count { it != IGNORED_ROW }

    val carriedTemplates = document.templates.map { it.id }.toSet()
    val knownTemplates = (carriedTemplates + document.slots.map { it.templateId }).filter {
        it in carriedTemplates || database.templateDao().findById(it) != null
    }.toSet()
    val referenced = document.templateExercises.map { it.exerciseId }.toSet()
    val presentExercises = referenced.filter { database.exerciseDao().findById(it) != null }.toSet()

    val templatesAdded = backupDao.insertTemplates(document.templates.map { it.toEntity() })
        .count { it != IGNORED_ROW }
    val planned = document.templateExercises.filter {
        it.templateId in knownTemplates && it.exerciseId in presentExercises
    }
    backupDao.insertTemplateExercises(planned.map { it.toEntity() })
    val plannedIds = planned.map { it.id }.toSet()
    backupDao.insertTemplateSets(
        document.templateSets.filter { it.templateExerciseId in plannedIds }.map { it.toEntity() },
    )

    // Inactive and last: a document adds a program, it does not decide which one home follows
    // (P3.3's "a new program is not made active"), and the authored order stays the user's (P3.12).
    val program = document.program.toEntity().copy(
        isActive = false,
        position = programDao.maxProgramPosition() + 1,
    )
    val programsAdded = programBackup.insertPrograms(listOf(program)).count { it != IGNORED_ROW }

    val slots = document.slots.filter { it.templateId in knownTemplates }
    programBackup.insertProgramSlots(slots.map { it.toEntity() })

    return ProgramImportSummary(
        programs = programsAdded + programsRestored,
        templates = templatesAdded,
        exercises = exercisesAdded,
        droppedMovements = document.templateExercises.size - planned.size,
    )
}

/**
 * Applies one program edit, planned by the shared core (ROADMAP N171).
 *
 * The snapshot is read **inside** the transaction and planned against the edit again, so a change
 * made on the phone between the preview and the confirmation is refused rather than overwritten —
 * the preview and the write ask the same function the same question, and the second answer is the one
 * that writes.
 *
 * Rows are applied in dependency order: new movements, then workouts, then their exercises, their
 * sets, and the slots that name a workout last. A removal is always a soft delete; the library keeps a
 * workout a day stopped using, and history keeps every session a removed plan pointed at.
 */
private suspend fun applyProgramEdit(
    database: WorkoutDatabase,
    timeSource: TimeSource,
    document: ProgramDocument,
    now: Long,
): ProgramImportSummary {
    val snapshot = buildProgramDocument(database, timeSource, document.program.id)
    val planned = plan(snapshot.toShared(), document.toShared())
    val ready = when (planned) {
        is PlanResult.Refused -> throw InvalidInputException(editRefusal(planned.problems))
        is PlanResult.Ready -> planned
    }

    val writer = ProgramEditWriter(
        document = document,
        changes = ready.changes,
        now = now,
        backupDao = database.backupDao(),
        editDao = database.programEditDao(),
        programDao = database.programDao(),
    )
    if (writer.renamed) writer.rename()
    val exercises = writer.writeExercises()
    val templates = writer.writeTemplates()
    writer.writeTemplateExercises()
    writer.writeSets()
    writer.writeSlots()

    return ProgramImportSummary(
        programs = 1,
        templates = templates,
        exercises = exercises,
        droppedMovements = 0,
    )
}

/**
 * The writes one accepted edit needs, in dependency order (ROADMAP N171).
 *
 * Split from the planner so each method stays small and says one thing: movements before the plans
 * that name them, plans before their sets, and the slots that schedule a plan last. The insert-only
 * [BackupDao] adds; [ProgramEditDao] is the half that updates and soft-deletes.
 */
private class ProgramEditWriter(
    private val document: ProgramDocument,
    changes: List<Change>,
    private val now: Long,
    private val backupDao: BackupDao,
    private val editDao: ProgramEditDao,
    private val programDao: ProgramDao,
) {
    private val byKind = changes.groupBy { it.kind }

    val renamed: Boolean get() = byKind.containsKey(ChangeKind.PROGRAM_RENAMED)

    suspend fun rename() {
        programDao.renameProgram(document.program.id, document.program.name, now)
    }

    /** New movements first: every planned exercise below names one. */
    suspend fun writeExercises(): Int {
        val added = ids(ChangeKind.EXERCISE_ADDED)
        if (added.isEmpty()) return 0
        backupDao.insertExercises(
            document.exercises
                .filter { it.id in added }
                .withValidLibraryShape()
                .map { it.toEntity() },
        )
        return added.size
    }

    suspend fun writeTemplates(): Int {
        val added = document.templates.filter { it.id in ids(ChangeKind.TEMPLATE_ADDED) }
        if (added.isNotEmpty()) {
            backupDao.insertTemplates(
                added.map {
                    it.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null)
                },
            )
        }
        val updated = document.templates.filter { it.id in ids(ChangeKind.TEMPLATE_UPDATED) }
        if (updated.isNotEmpty()) {
            editDao.updateTemplates(
                updated.map { it.toEntity().copy(updatedAt = now, deletedAt = null) },
            )
        }
        return added.size + updated.size
    }

    suspend fun writeTemplateExercises() {
        val added = document.templateExercises
            .filter { it.id in ids(ChangeKind.TEMPLATE_EXERCISE_ADDED) }
        if (added.isNotEmpty()) {
            backupDao.insertTemplateExercises(
                added.map {
                    it.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null)
                },
            )
        }
        val updated = document.templateExercises
            .filter { it.id in ids(ChangeKind.TEMPLATE_EXERCISE_UPDATED) }
        if (updated.isNotEmpty()) {
            editDao.updateTemplateExercises(
                updated.map { it.toEntity().copy(updatedAt = now, deletedAt = null) },
            )
        }
        val removed = ids(ChangeKind.TEMPLATE_EXERCISE_REMOVED).toList()
        if (removed.isNotEmpty()) {
            editDao.softDeleteTemplateExercises(removed, now)
            editDao.softDeleteSetsOfTemplateExercises(removed, now)
        }
    }

    suspend fun writeSets() {
        val added = document.templateSets.filter { it.id in ids(ChangeKind.SET_ADDED) }
        if (added.isNotEmpty()) {
            backupDao.insertTemplateSets(
                added.map {
                    it.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null)
                },
            )
        }
        val updated = document.templateSets.filter { it.id in ids(ChangeKind.SET_UPDATED) }
        if (updated.isNotEmpty()) {
            editDao.updateTemplateSets(
                updated.map { it.toEntity().copy(updatedAt = now, deletedAt = null) },
            )
        }
        val removed = ids(ChangeKind.SET_REMOVED).toList()
        if (removed.isNotEmpty()) {
            editDao.softDeleteTemplateSets(removed, now)
        }
    }

    suspend fun writeSlots() {
        document.slots
            .filter { it.id in ids(ChangeKind.SLOT_ADDED) }
            .forEach { slot ->
                programDao.insertSlot(
                    slot.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null),
                )
            }
        val updated = document.slots.filter { it.id in ids(ChangeKind.SLOT_UPDATED) }
        if (updated.isNotEmpty()) {
            editDao.updateProgramSlots(
                updated.map { it.toEntity().copy(updatedAt = now, deletedAt = null) },
            )
        }
        val removed = ids(ChangeKind.SLOT_REMOVED).toList()
        if (removed.isNotEmpty()) {
            editDao.softDeleteProgramSlots(removed, now)
        }
    }

    private fun ids(kind: ChangeKind): Set<String> = byKind[kind].orEmpty().map { it.id }.toSet()
}

/** Other live programs that schedule a workout this edit names, empty when none does (N73, N171). */
private suspend fun otherProgramsUsing(
    database: WorkoutDatabase,
    document: ProgramDocument,
): List<String> {
    val templateIds = document.templates.map { it.id }
    if (templateIds.isEmpty()) return emptyList()
    return database.programDao().otherProgramNamesUsingTemplates(templateIds, document.program.id)
}

/**
 * A refusal in the lifter's words.
 *
 * The shared core answers with codes because the page renders its own sentences; this is where the
 * app's are chosen. Stale and missing are the two the lifter can act on, so they get their own.
 */
private fun editRefusal(problems: List<ProgramProblem>): String {
    val first = problems.firstOrNull() ?: return "That program edit cannot be applied."
    return when (first.code) {
        ProblemCode.STALE_BASE ->
            "This program changed on the phone since you exported it. " +
                "Export it again and redo the edit."

        ProblemCode.PROGRAM_MISSING -> "That program is not on this device."
        ProblemCode.PROGRAM_MISMATCH -> "That edit belongs to a different program."
        ProblemCode.NO_EDIT_MARKER -> "That edit does not name the export it was made from."
        else ->
            "That program edit does not fit the app's rules " +
                "(${first.code.name.lowercase().replace('_', ' ')})."
    }
}

/** A program with no name is a list row nobody can tell apart from the next (ROADMAP P3.3). */
private fun requireName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) throw InvalidInputException("Give the program a name.")
    return trimmed
}
