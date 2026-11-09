package com.example.androidapp.data

import com.example.androidapp.data.local.ExerciseDao
import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.transfer.ProgramDocument
import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.data.transfer.toDto
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.shared.ProblemCode
import com.example.androidapp.shared.ProgramProblem
import kotlinx.coroutines.flow.map

// Reading one program as a document (ROADMAP N47, N171): the export payload and the edit snapshot,
// assembled from the rows a program already owns.

/**
 * One program's definition as a document (ROADMAP N47).
 *
 * File-level because the repository is at the function ceiling this project enforces, and because
 * this is a read that assembles a value rather than a decision the repository owns.
 */
internal suspend fun buildProgramDocument(
    database: WorkoutDatabase,
    timeSource: TimeSource,
    programId: String,
): ProgramDocument {
    val programDao = database.programDao()
    val program = programDao.findProgram(programId) ?: throw NotFoundException("program $programId")
    // **A day whose workout is hidden is not exported** (B287). A template's delete is a soft delete, so the
    // slot survives it while `findById` hides the row — and a document carrying the slot without its workout is
    // one the app's own validator refuses (`DANGLING_SLOT_TEMPLATE`), which the receiving merge then drops in
    // silence. The screen does not show that day either: the document says what the program is.
    val allSlots = programDao.findSlots(programId)
    val templateDao = database.templateDao()
    val liveTemplates = templateDao.findLiveByIds(allSlots.map { it.templateId }.distinct())
    val kept = liveTemplates.mapTo(mutableSetOf()) { it.id }
    val slots = allSlots.filter { it.templateId in kept }
    val templateIds = slots.map { it.templateId }.distinct()
    val templates = liveTemplates
    val templateExercises = templateDao.findTemplateExercisesForTemplates(templateIds)
    val templateSets = database.templateSetDao().findTemplateSetsForTemplates(templateIds)
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
    var pending = ids.distinct()
    while (pending.isNotEmpty()) {
        // One read per level rather than one per row (B393): a deep library costs the depth, not its size.
        val rows = dao.findLiveByIds(pending).associateBy { it.id }
        val next = mutableListOf<String>()
        for (id in pending) {
            val row = found[id] ?: rows[id]
            val inserted = row != null && found.put(id, row) == null
            if (inserted && row != null) row.parentId?.let(next::add)
        }
        pending = next.distinct()
    }
    return found.values.toList()
}

/**
 * One program's definition as an edit reads it, or the refusal the lifter can act on (B189).
 *
 * [buildProgramDocument] answers a program this device does not hold with `NotFoundException`, which the
 * screens render as a storage failure — "Couldn't read the file" — true of the read and useless to the
 * lifter. An edit names the program it is for, so a missing (or deleted) one is the planner's own
 * `PROGRAM_MISSING` refusal, and the branch that words it stops being dead code.
 */
internal suspend fun editSnapshot(
    database: WorkoutDatabase,
    timeSource: TimeSource,
    programId: String,
): ProgramDocument {
    if (database.programDao().findProgram(programId) == null) {
        throw InvalidInputException(
            editRefusal(
                listOf(ProgramProblem(ProblemCode.PROGRAM_MISSING, "program.id", programId)),
            ),
        )
    }
    return buildProgramDocument(database, timeSource, programId)
}
