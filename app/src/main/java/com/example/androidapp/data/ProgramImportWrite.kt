package com.example.androidapp.data

import com.example.androidapp.data.local.BackupDao
import com.example.androidapp.data.local.ProgramDao
import com.example.androidapp.data.local.ProgramEditDao
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.transfer.PlannedDay
import com.example.androidapp.data.transfer.PlannedMovement
import com.example.androidapp.data.transfer.PlannedWork
import com.example.androidapp.data.transfer.ProgramDocument
import com.example.androidapp.data.transfer.counted
import com.example.androidapp.data.transfer.plansTheSameWorkAs
import com.example.androidapp.data.transfer.toEntity
import com.example.androidapp.data.transfer.toShared
import com.example.androidapp.data.transfer.withResolvedExercises
import com.example.androidapp.data.transfer.withValidLibraryShape
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.shared.Change
import com.example.androidapp.shared.ChangeKind
import com.example.androidapp.shared.PlanResult
import com.example.androidapp.shared.plan
import kotlinx.coroutines.flow.map

// Writing a program document into the store (ROADMAP N47, N171, N238): the add that overwrites nothing,
// the edit planned by the shared core, and the writer that applies one change in dependency order.

/** Room reports an ignored insert — a row whose id was already present — as this rowid. */
private const val IGNORED_ROW = -1L

/** The ids this library has merged away, as the import's map (ROADMAP N238). */
internal suspend fun retiredMovementIds(database: WorkoutDatabase): Map<String, String> =
    database.movementMergeDao().retiredMovements().associate { it.id to it.survivorId }

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
internal suspend fun mergeProgramDocument(
    database: WorkoutDatabase,
    programDao: ProgramDao,
    document: ProgramDocument,
    now: Long,
): ProgramImportSummary {
    val backupDao = database.backupDao()
    val programBackup = database.programBackupDao()
    // **An id this library merged away is read as the row it became** (ROADMAP N238). The document may have been
    // exported before the merge, and a movement whose id resolves to nothing is exactly what makes the import
    // mint a duplicate — the twin the merge removed.
    val resolved = document.withResolvedExercises(retiredMovementIds(database))

    // Live means this exact program is already here; nothing in the document is new (B180). The read
    // filters `deletedAt`, so a deleted one falls through to the restore below rather than stopping here.
    if (programDao.findProgram(resolved.program.id) != null) {
        return ProgramImportSummary(
            programs = 0,
            templates = 0,
            exercises = 0,
            droppedMovements = 0,
            // **"Already here" is only true of the same work** (B267). A document whose days or movements
            // differ carries an id the seed produced rather than one this program earned — a pasted table's
            // id is a slug of its name — and the card has to say the name is taken rather than that this
            // program arrived.
            nameTakenByAnotherPlan = !resolved.plansTheSameWorkAs(heldWork(database, resolved.program.id)),
        )
    }

    // One statement, before the inserts: a hidden program comes back, a live or absent one is
    // untouched (only rows with `deletedAt` set are updated), and the count says which happened.
    val programsRestored = programBackup.restoreProgramVisibility(resolved.program.id, now)

    // The library's shape is enforced on the way in (B92): a document can carry a parent link the app would
    // never write, and a movement filed under a head the document does not carry keeps the link (the head may
    // exist here) while an illegal one is cleared.
    val exercisesAdded = backupDao.insertExercises(
        resolved.exercises.withValidLibraryShape().map { it.toEntity() },
    ).count { it != IGNORED_ROW }

    val carriedTemplates = resolved.templates.map { it.id }.toSet()
    val referencedTemplateIds = (carriedTemplates + resolved.slots.map { it.templateId }).toList()
    val liveTemplateIds = database.templateDao().findLiveByIds(referencedTemplateIds).mapTo(mutableSetOf()) { it.id }
    val knownTemplates = referencedTemplateIds.filter { it in carriedTemplates || it in liveTemplateIds }.toSet()
    val referenced = resolved.templateExercises.map { it.exerciseId }.toSet()
    val presentExercises = database.exerciseDao().findLiveByIds(referenced.toList())
        .mapTo(mutableSetOf()) { it.id }

    val templatesAdded = backupDao.insertTemplates(resolved.templates.map { it.toEntity() })
        .count { it != IGNORED_ROW }
    val planned = resolved.templateExercises.filter {
        it.templateId in knownTemplates && it.exerciseId in presentExercises
    }
    backupDao.insertTemplateExercises(planned.map { it.toEntity() })
    val plannedIds = planned.map { it.id }.toSet()
    backupDao.insertTemplateSets(
        resolved.templateSets.filter { it.templateExerciseId in plannedIds }.map { it.toEntity() },
    )

    // Inactive and last: a document adds a program, it does not decide which one home follows
    // (P3.3's "a new program is not made active"), and the authored order stays the user's (P3.12).
    val program = resolved.program.toEntity().copy(
        isActive = false,
        position = programDao.maxProgramPosition() + 1,
    )
    val programsAdded = programBackup.insertPrograms(listOf(program)).count { it != IGNORED_ROW }

    val slots = resolved.slots.filter { it.templateId in knownTemplates }
    programBackup.insertProgramSlots(slots.map { it.toEntity() })

    return ProgramImportSummary(
        programs = programsAdded + programsRestored,
        templates = templatesAdded,
        exercises = exercisesAdded,
        droppedMovements = resolved.templateExercises.size - planned.size,
    )
}

/**
 * The work the device holds for [programId], in the shape [plansTheSameWorkAs] compares (B267).
 *
 * Read only on the path where the document's id is already here, so a first import pays nothing for it. The days
 * come from the program's own slots rather than the whole library for the same reason: this answers a question
 * about one program.
 */
internal suspend fun heldWork(database: WorkoutDatabase, programId: String): PlannedWork {
    val slots = database.programDao().findSlots(programId)
    return PlannedWork(
        days = slots.map { PlannedDay(it.templateId, it.weekday, it.position) }.counted(),
        movements = slots.map { it.templateId }.distinct()
            .flatMap { database.templateDao().findTemplateExercises(it) }
            .map { PlannedMovement(it.templateId, it.exerciseId, it.position) }
            .counted(),
    )
}

/**
 * Applies one program edit, planned by the shared core (ROADMAP N171).
 *
 * The snapshot is read **inside** the transaction and planned against the edit again, so a change
 * made on the phone between the preview and the confirmation is refused rather than overwritten —
 * the preview and the write ask the same function the same question, and the second answer is the one
 * that writes. The device's newest write is read in the same transaction and for the same reason: a
 * row deleted since the export is a change too, and the document cannot carry one (B183).
 *
 * Rows are applied in dependency order: new movements, then workouts, then their exercises, their
 * sets, and the slots that name a workout last. A removal is always a soft delete; the library keeps a
 * workout a day stopped using, and history keeps every session a removed plan pointed at.
 */
internal suspend fun applyProgramEdit(
    database: WorkoutDatabase,
    timeSource: TimeSource,
    document: ProgramDocument,
    now: Long,
): ProgramImportSummary {
    val editDao = database.programEditDao()
    val snapshot = editSnapshot(database, timeSource, document.program.id)
    val planned = plan(
        snapshot.toShared(),
        document.toShared(),
        editDao.newestWriteForProgram(document.program.id),
    )
    val ready = when (planned) {
        is PlanResult.Refused -> throw InvalidInputException(editRefusal(planned.problems))
        is PlanResult.Ready -> planned
    }

    val writer = ProgramEditWriter(
        document = document,
        changes = ready.changes,
        now = now,
        backupDao = database.backupDao(),
        editDao = editDao,
        programDao = database.programDao(),
    )
    if (writer.renamed) writer.rename()
    writer.applySchedule()
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

    /**
     * A block's shape, when the edit names it (ROADMAP N186).
     *
     * The document's own values, not the change's detail: the detail is for wording the preview, and
     * parsing it back into a kind or a date would be a second reading of what the document already
     * says. `isActive` and `position` stay the device's — they are placement, not the plan.
     */
    suspend fun applySchedule() {
        if (byKind.containsKey(ChangeKind.PROGRAM_KIND)) {
            programDao.setProgramKind(
                id = document.program.id,
                kind = document.program.kind,
                at = now,
            )
        }
        if (byKind.containsKey(ChangeKind.PROGRAM_START)) {
            programDao.setProgramStartDate(
                id = document.program.id,
                startDate = document.program.startDate,
                at = now,
            )
        }
    }

    /** New movements first: every planned exercise below names one. */
    suspend fun writeExercises(): Int {
        val added = ids(ChangeKind.EXERCISE_ADDED)
        if (added.isEmpty()) return 0
        // **Count what landed, not what was asked for** (B363): a locally hidden movement has the id the
        // document carries, so its insert is ignored and the summary must not claim it was added.
        return backupDao.insertExercises(
            document.exercises
                .filter { it.id in added }
                .withValidLibraryShape()
                .map { it.toEntity() },
        ).count { it != IGNORED_ROW }
    }

    suspend fun writeTemplates(): Int {
        val added = document.templates.filter { it.id in ids(ChangeKind.TEMPLATE_ADDED) }
        if (added.isNotEmpty()) {
            backupDao.insertTemplates(
                added.map {
                    // A workout arriving with a program is offered: nothing the app creates starts hidden.
                    it.toEntity().copy(
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        hiddenAt = null,
                    )
                },
            )
        }
        // A targeted write rather than the whole row: the document cannot carry `hiddenAt`, so a
        // full-row update would unhide a workout the lifter hid (ROADMAP N214).
        val updated = document.templates.filter { it.id in ids(ChangeKind.TEMPLATE_UPDATED) }
        updated.forEach {
            editDao.updateTemplatePlan(id = it.id, name = it.name, versionOf = it.versionOf, at = now)
        }
        return added.size + updated.size
    }

    suspend fun writeTemplateExercises() {
        // A movement the document names but this library does not hold live cannot carry a plan row
        // (B363): the row would be written against a hidden id and every plan read would filter it out.
        val liveExercises = backupDao.allExercises()
            .filter { it.deletedAt == null }
            .mapTo(mutableSetOf()) { it.id }
        val added = document.templateExercises
            .filter { it.id in ids(ChangeKind.TEMPLATE_EXERCISE_ADDED) && it.exerciseId in liveExercises }
        if (added.isNotEmpty()) {
            backupDao.insertTemplateExercises(
                added.map {
                    it.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null)
                },
            )
        }
        val updated = document.templateExercises
            .filter { it.id in ids(ChangeKind.TEMPLATE_EXERCISE_UPDATED) && it.exerciseId in liveExercises }
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
        // A set whose planned exercise is hidden (or was dropped above) would be an orphan row (B363).
        // The live rows are read back after `writeTemplateExercises`, so a newly inserted plan row is
        // included and a hidden movement's row is not.
        val livePlanned = backupDao.allTemplateExercises()
            .filter { it.deletedAt == null }
            .mapTo(mutableSetOf()) { it.id }
        val added = document.templateSets.filter {
            it.id in ids(ChangeKind.SET_ADDED) && it.templateExerciseId in livePlanned
        }
        if (added.isNotEmpty()) {
            backupDao.insertTemplateSets(
                added.map {
                    it.toEntity().copy(createdAt = now, updatedAt = now, deletedAt = null)
                },
            )
        }
        val updated = document.templateSets.filter {
            it.id in ids(ChangeKind.SET_UPDATED) && it.templateExerciseId in livePlanned
        }
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
internal suspend fun otherProgramsUsing(
    database: WorkoutDatabase,
    document: ProgramDocument,
): List<String> {
    val templateIds = document.templates.map { it.id }
    if (templateIds.isEmpty()) return emptyList()
    return database.programDao().otherProgramNamesUsingTemplates(templateIds, document.program.id)
}
