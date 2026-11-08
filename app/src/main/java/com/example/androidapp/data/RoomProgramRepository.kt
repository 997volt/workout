package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.ProgramDao
import com.example.androidapp.data.local.ProgramDeloadDao
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
import com.example.androidapp.data.transfer.plansTheSameWorkAs
import com.example.androidapp.data.transfer.programDocumentCopy
import com.example.androidapp.data.transfer.toShared
import com.example.androidapp.data.transfer.withResolvedExercises
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.RecordedSkip
import com.example.androidapp.domain.model.RecordedSubstitution
import com.example.androidapp.domain.model.SequenceSchedule
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.freeProgramName
import com.example.androidapp.domain.model.programRun
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.shared.PlanResult
import com.example.androidapp.shared.plan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
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

    override suspend fun setProgramKind(programId: String, kind: ProgramKind): DataResult<Unit> =
        dataResultOf {
            val now = timeSource.nowEpochMillis()
            val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
            database.withTransaction {
                val updated = dao.setProgramKind(id = programId, kind = kind, at = now)
                if (updated == 0) throw NotFoundException("program $programId")
                // A block needs a beginning, and the toggle is where one is chosen (B196): without a
                // date the start falls back to the program's creation, which for a program that has
                // been trained for months would make the block arrive already under way.
                if (kind == ProgramKind.SEQUENCE && dao.findProgram(programId)?.startDate == null) {
                    dao.setProgramStartDate(programId, today.toEpochDay(), now)
                }
            }
        }

    override suspend fun setProgramStartDate(programId: String, startDate: LocalDate?): DataResult<Unit> =
        dataResultOf {
            val updated = dao.setProgramStartDate(
                id = programId,
                startDate = startDate?.toEpochDay(),
                at = timeSource.nowEpochMillis(),
            )
            if (updated == 0) throw NotFoundException("program $programId")
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
        combine(
            dao.observeProgram(programId),
            dao.observeSlotDetails(programId),
        ) { program, rows -> program to rows }
            .flatMapLatest { (program, rows) ->
                val slots = rows.map { it.toDomain() }
                if (program == null || slots.isEmpty()) {
                    flowOf(null)
                } else {
                    val slotIds = slots.map { it.id }
                    // The substitutions drive the session read: a session started from a stand-in
                    // template trains that slot, so its template has to be read too (P3.11).
                    substitutionDao.observeSubstitutionsForSlots(slotIds).flatMapLatest { substitutions ->
                        val templates =
                            (slots.map { it.templateId } + substitutions.map { it.templateId }).distinct()
                        combine(
                            runDao.observeFinishedSessions(templates),
                            runDao.observeSkipsForSlots(slotIds),
                        ) { sessions, skips ->
                            // The zone is read when the data arrives rather than captured (B45); it is
                            // the fallback only for a session recorded before N25.
                            val zone = ZoneId.systemDefault()
                            val programSessions = sessions.mapNotNull { it.toRunSession(zone) }
                            val recordedSkips = skips.map {
                                RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
                            }
                            // A sequence's place is its own derivation (N175): a run that wrapped
                            // would put the first session of the block back on the schedule.
                            if (program.kind == ProgramKind.SEQUENCE) {
                                sequenceRun(program, slots, programSessions, recordedSkips, zone)
                            } else {
                                programRun(
                                    slots = slots,
                                    sessions = programSessions,
                                    skips = recordedSkips,
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
            }

    override fun observeSequenceSchedule(programId: String): Flow<SequenceSchedule?> =
        combine(
            dao.observeProgram(programId),
            dao.observeSlotDetails(programId),
        ) { program, rows -> program to rows }
            .flatMapLatest { (program, rows) ->
                val slots = rows.map { it.toDomain() }
                if (program == null || slots.isEmpty() || program.kind != ProgramKind.SEQUENCE) {
                    flowOf(null)
                } else {
                    combine(
                        runDao.observeFinishedSessions(slots.map { it.templateId }),
                        runDao.observeSkipsForSlots(slots.map { it.id }),
                    ) { sessions, skips ->
                        val zone = ZoneId.systemDefault()
                        scheduleOf(
                            program = program,
                            slots = slots,
                            sessions = sessions.mapNotNull { it.toRunSession(zone) },
                            skips = skips.map {
                                RecordedSkip(slotId = it.slotId, weekStart = LocalDate.ofEpochDay(it.weekStart))
                            },
                            zone = zone,
                        )
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
            // A block's weekday places one session rather than scheduling a recurring one (N174), so
            // there is no weekly occurrence to miss — and asking on its behalf recorded a week-keyed
            // skip that settled a session weeks before it was due (B195).
            if (program.kind == ProgramKind.SEQUENCE) return@forEach
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

    override suspend fun skipBlockSession(slotId: String, day: LocalDate): DataResult<Unit> =
        dataResultOf {
            val epochDay = day.toEpochDay()
            val now = timeSource.nowEpochMillis()
            database.withTransaction {
                // Idempotent for the day, like the weekly skip is for its week: two taps must not
                // settle two sessions.
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
            // The document's own rules run here, not just in the page (B328): the merge writes its rows
            // straight into the store, bypassing the template write boundary.
            requireValidDocument(document)
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

    override suspend fun importProgramDocumentCopy(
        text: String,
        name: String,
    ): DataResult<ProgramImportSummary> = dataResultOf {
        val document = ProgramDocumentCodec.decode(text)
        // The copy is an add: an edit is a proposal to change a program this device holds, and the question a
        // rename answers is which program the arriving document is (N243).
        if (document.edit != null) throw InvalidInputException("An edit cannot be loaded as a copy.")
        requireValidDocument(document)
        val wanted = requireName(name)
        // One timestamp inside the transaction, like the add it is (B177).
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            // Read inside the transaction (B320): a name taken between the question and the answer is still
            // refused, and this read cannot see a list the write would not.
            if (dao.names().any { it.equals(wanted, ignoreCase = true) }) {
                throw InvalidInputException("A program is already called \"$wanted\".")
            }
            mergeProgramDocument(
                database = database,
                programDao = dao,
                document = programDocumentCopy(document, wanted) { UUID.randomUUID().toString() },
                now = now,
            )
        }
    }

    override suspend fun previewProgramDocument(text: String): DataResult<ProgramDocumentPreview> =
        dataResultOf {
            val document = ProgramDocumentCodec.decode(text)
            if (document.edit == null) {
                return@dataResultOf addPreview(document)
            }
            val snapshot = editSnapshot(database, timeSource, document.program.id)
            // The device's own newest write, soft-deleted rows included: a deletion after the export
            // is not in the snapshot at all, so only this read can see it (B183).
            val newest = database.programEditDao().newestWriteForProgram(document.program.id)
            when (val planned = plan(snapshot.toShared(), document.toShared(), newest)) {
                is PlanResult.Refused -> throw InvalidInputException(editRefusal(planned.problems))
                is PlanResult.Ready -> ProgramDocumentPreview.Edit(
                    // The name the list shows, not the one the edit would give it: the lifter picks a
                    // program they know, and the rename is stated separately (B192).
                    programName = snapshot.program.name,
                    changes = planned.changes,
                    warnings = planned.warnings,
                    sharedProgramNames = otherProgramsUsing(database, document),
                )
            }
        }

    /**
     * What adding this document would do, without writing it (N243).
     *
     * A document whose id is already here is only a clash when the plan under that id is a **different** one
     * (B267): loading the same file twice is the idempotent case the merge already answers. When it is a
     * different plan there is nothing the load can do — the merge never edits a program (B180) — so the question
     * becomes the lifter's, and the suggestion is a name the list does not hold.
     */
    private suspend fun addPreview(document: ProgramDocument): ProgramDocumentPreview {
        val held = dao.findProgram(document.program.id) ?: return ProgramDocumentPreview.Add
        // The merge resolves a retired movement id before it compares and writes (N238), and the preview has to
        // answer the same question: a file exported before the merge names the id this device no longer holds,
        // so comparing it raw would read the program already here as a different one (B301).
        val resolved = document.withResolvedExercises(retiredMovementIds(database))
        val sameWork = resolved.plansTheSameWorkAs(heldWork(database, resolved.program.id))
        return if (sameWork) {
            ProgramDocumentPreview.Add
        } else {
            ProgramDocumentPreview.NameTaken(
                // The name the device actually holds under this id (B320): the document's own name may be
                // free, because the program here was renamed, and the question then names a program that is
                // not called that.
                name = held.name,
                suggestion = freeProgramName(resolved.program.name, dao.names()),
            )
        }
    }

    private companion object {
        /** One day of slack at each end of the week, for sessions in another zone. */
        const val SLACK_DAYS = 1L

        const val DAYS_IN_WEEK = 7L
    }
}
