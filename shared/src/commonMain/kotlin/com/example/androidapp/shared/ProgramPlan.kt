package com.example.androidapp.shared

/** What an edit would change, one row at a time (ROADMAP N170, N171). */
enum class ChangeKind {
    PROGRAM_RENAMED,
    SLOT_ADDED,
    SLOT_UPDATED,
    SLOT_REMOVED,
    TEMPLATE_ADDED,
    TEMPLATE_UPDATED,
    TEMPLATE_EXERCISE_ADDED,
    TEMPLATE_EXERCISE_UPDATED,
    TEMPLATE_EXERCISE_REMOVED,
    SET_ADDED,
    SET_UPDATED,
    SET_REMOVED,
    EXERCISE_ADDED,
}

/**
 * One change, by row id.
 *
 * [name] is what the row is called — a template's name, a movement's, the program's own — so each host
 * can describe the change without holding the document, and neither has to guess which row an id means.
 */
data class Change(val kind: ChangeKind, val id: String, val name: String? = null)

/** A consequence of an edit that is worth stating but is not a refusal (ROADMAP N170). */
enum class WarningCode {
    /** A movement the device does not hold, which the edit brings with its definition. */
    NEW_MOVEMENT,

    /** A day left the schedule and its workout stays in the library, which is the app's own rule. */
    WORKOUT_KEPT,
}

data class PlanWarning(val code: WarningCode, val id: String? = null, val name: String? = null)

/** What [plan] decided (ROADMAP N170). */
sealed interface PlanResult {
    data class Ready(val changes: List<Change>, val warnings: List<PlanWarning>) : PlanResult
    data class Refused(val problems: List<ProgramProblem>) : PlanResult
}

/**
 * What applying [edit] to [snapshot] would do, without doing it (ROADMAP N170, N171).
 *
 * This is the one decision the page previews and the app executes. It is pure and total: every refusal
 * is a [PlanResult.Refused] carrying codes, and every acceptance is the exact list of rows to add,
 * update or remove — so the page cannot promise one thing while the app writes another.
 *
 * **The program is the unit.** Slots are the exact set the edit names (an absent one leaves the
 * schedule). Templates are upserted but never removed: the app's own removal keeps the workout in the
 * library, and a workout another program also uses must not disappear because this edit dropped a day.
 * Template exercises and sets belonging to a template the edit carries are similarly exact; rows of a
 * template the edit does not carry are untouched, because they are not part of this program's edit.
 *
 * **The device owns some fields.** `isActive` and `position` are the program's place among programs,
 * not the program's plan, and the edit's values are ignored — the same line B177 drew for a restore.
 * Existing library rows are never rewritten, so a movement's name and taxonomy stay the device's.
 *
 * **Staleness is a refusal, not a merge.** The edit names the export it was made from
 * ([ProgramDocument.edit]); if the program's newest row is later than that, the lifter changed it on
 * the phone after exporting and the app refuses rather than guessing which change wins.
 *
 * [deviceNewestWrite] is that newest write as only a device can read it: a document holds live rows,
 * and deleting one is exactly what takes it out of them, so a document alone cannot show a deletion
 * (B183). The app passes the timestamp it reads from its own tables, soft-deleted rows included; the
 * page passes nothing, because it has no device to ask and its preview is the app's decision anyway.
 */
fun plan(
    snapshot: ProgramDocument,
    edit: ProgramDocument,
    deviceNewestWrite: Long? = null,
): PlanResult {
    val marker = edit.edit ?: return PlanResult.Refused(
        listOf(ProgramProblem(ProblemCode.NO_EDIT_MARKER, "edit")),
    )
    if (edit.program.id != snapshot.program.id) {
        return PlanResult.Refused(
            listOf(ProgramProblem(ProblemCode.PROGRAM_MISMATCH, "program.id", edit.program.id)),
        )
    }
    if (snapshot.program.deletedAt != null) {
        return PlanResult.Refused(
            listOf(ProgramProblem(ProblemCode.PROGRAM_MISSING, "program.id", edit.program.id)),
        )
    }
    if (edit.program.deletedAt != null) {
        return PlanResult.Refused(
            listOf(ProgramProblem(ProblemCode.MALFORMED, "program.deletedAt")),
        )
    }
    val invalid = validate(edit)
    if (invalid.isNotEmpty()) return PlanResult.Refused(invalid)

    // The document's own rows are the page's whole answer; the device's read adds the rows it has
    // deleted, which the document cannot carry at all (B183).
    val fromDocument = latestWrite(snapshot)
    val newestWrite = if (deviceNewestWrite == null) fromDocument else maxOf(fromDocument, deviceNewestWrite)
    if (newestWrite > marker.baseExportedAt) {
        return PlanResult.Refused(
            listOf(ProgramProblem(ProblemCode.STALE_BASE, "edit.baseExportedAt", newestWrite.toString())),
        )
    }

    val changes = mutableListOf<Change>()
    val warnings = mutableListOf<PlanWarning>()

    if (snapshot.program.name != edit.program.name) {
        changes += Change(ChangeKind.PROGRAM_RENAMED, edit.program.id, edit.program.name)
    }

    val snapshotSlots = snapshot.slots.associateBy { it.id }
    val editSlotIds = edit.slots.map { it.id }.toSet()
    edit.slots.forEach { slot ->
        val old = snapshotSlots[slot.id]
        val name = edit.templates.firstOrNull { it.id == slot.templateId }?.name
        when {
            old == null -> changes += Change(ChangeKind.SLOT_ADDED, slot.id, name)
            !sameSlot(old, slot) -> changes += Change(ChangeKind.SLOT_UPDATED, slot.id, name)
        }
    }
    snapshot.slots.filter { it.id !in editSlotIds }.forEach { slot ->
        val name = snapshot.templates.firstOrNull { it.id == slot.templateId }?.name
        changes += Change(ChangeKind.SLOT_REMOVED, slot.id, name)
    }

    val snapshotTemplates = snapshot.templates.associateBy { it.id }
    edit.templates.forEach { template ->
        val old = snapshotTemplates[template.id]
        when {
            old == null -> changes += Change(ChangeKind.TEMPLATE_ADDED, template.id, template.name)
            !sameTemplate(old, template) ->
                changes += Change(ChangeKind.TEMPLATE_UPDATED, template.id, template.name)
        }
    }

    // A removed day's workout stays, and saying so is the difference between a deletion and a loss.
    val editSlotTemplateIds = edit.slots.map { it.templateId }.toSet()
    snapshot.slots.filter { it.id !in editSlotIds }
        .map { it.templateId }
        .filter { it !in editSlotTemplateIds }
        .distinct()
        .forEach { templateId ->
            warnings += PlanWarning(
                WarningCode.WORKOUT_KEPT,
                templateId,
                snapshot.templates.firstOrNull { it.id == templateId }?.name,
            )
        }

    val editTemplateIds = edit.templates.map { it.id }.toSet()
    val editTemplateExerciseIds = edit.templateExercises.map { it.id }.toSet()
    val snapshotTemplateExercises = snapshot.templateExercises.associateBy { it.id }
    edit.templateExercises.forEach { planned ->
        val old = snapshotTemplateExercises[planned.id]
        when {
            old == null -> changes += Change(ChangeKind.TEMPLATE_EXERCISE_ADDED, planned.id, null)
            !sameTemplateExercise(old, planned) ->
                changes += Change(ChangeKind.TEMPLATE_EXERCISE_UPDATED, planned.id, null)
        }
    }
    snapshot.templateExercises
        .filter { it.id !in editTemplateExerciseIds && it.templateId in editTemplateIds }
        .forEach { changes += Change(ChangeKind.TEMPLATE_EXERCISE_REMOVED, it.id, null) }

    val snapshotSets = snapshot.templateSets.associateBy { it.id }
    val editSetIds = edit.templateSets.map { it.id }.toSet()
    edit.templateSets.forEach { set ->
        val old = snapshotSets[set.id]
        when {
            old == null -> changes += Change(ChangeKind.SET_ADDED, set.id, null)
            !sameSet(old, set) -> changes += Change(ChangeKind.SET_UPDATED, set.id, null)
        }
    }
    // A set removed with its exercise is covered by that exercise's removal; only a set removed from a
    // plan that keeps the exercise is its own change.
    snapshot.templateSets
        .filter { it.id !in editSetIds && it.templateExerciseId in editTemplateExerciseIds }
        .forEach { changes += Change(ChangeKind.SET_REMOVED, it.id, null) }

    val snapshotExerciseIds = snapshot.exercises.map { it.id }.toSet()
    edit.exercises.filter { it.id !in snapshotExerciseIds }.forEach { exercise ->
        changes += Change(ChangeKind.EXERCISE_ADDED, exercise.id, exercise.name)
        warnings += PlanWarning(WarningCode.NEW_MOVEMENT, exercise.id, exercise.name)
    }

    return PlanResult.Ready(changes, warnings)
}

/** The newest write among the program's own rows — not the library's, which an edit never rewrites. */
private fun latestWrite(document: ProgramDocument): Long {
    var latest = document.program.updatedAt
    fun consider(time: Long) {
        if (time > latest) latest = time
    }
    document.slots.forEach { consider(it.updatedAt) }
    document.templates.forEach { consider(it.updatedAt) }
    document.templateExercises.forEach { consider(it.updatedAt) }
    document.templateSets.forEach { consider(it.updatedAt) }
    return latest
}

private fun sameSlot(a: ProgramSlotDto, b: ProgramSlotDto): Boolean =
    a.programId == b.programId && a.templateId == b.templateId &&
        a.position == b.position && a.weekday == b.weekday

private fun sameTemplate(a: TemplateDto, b: TemplateDto): Boolean =
    a.name == b.name && a.versionOf == b.versionOf

private fun sameTemplateExercise(a: TemplateExerciseDto, b: TemplateExerciseDto): Boolean =
    a.templateId == b.templateId && a.exerciseId == b.exerciseId && a.position == b.position &&
        a.restSeconds == b.restSeconds && a.techniqueNote == b.techniqueNote &&
        a.targetRpeHalves == b.targetRpeHalves && a.supersetGroup == b.supersetGroup

private fun sameSet(a: TemplateSetDto, b: TemplateSetDto): Boolean =
    a.templateExerciseId == b.templateExerciseId && a.setIndex == b.setIndex && a.role == b.role &&
        a.targetWeightGrams == b.targetWeightGrams &&
        a.targetAssistanceGrams == b.targetAssistanceGrams &&
        a.targetRepsMin == b.targetRepsMin && a.targetRepsMax == b.targetRepsMax &&
        a.targetRepsCurrent == b.targetRepsCurrent &&
        a.dropValueGrams == b.dropValueGrams && a.note == b.note
