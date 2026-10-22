package com.example.androidapp.shared

/** What an edit would change, one row at a time (ROADMAP N170, N171). */
enum class ChangeKind {
    PROGRAM_RENAMED,

    /** The program becomes a block, or a week that repeats (ROADMAP N186). */
    PROGRAM_KIND,

    /** The day a block starts, or that it goes back to the creation date (ROADMAP N186). */
    PROGRAM_START,
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
data class Change(
    val kind: ChangeKind,
    val id: String,
    val name: String? = null,
    /**
     * What the row becomes, when the row carries a value a host has to word (ROADMAP N186): a kind by
     * name, a start as an epoch day, or null for "the creation date". Never prose — each host renders
     * its own sentence, the rule [ProgramProblem.detail] already follows.
     */
    val detail: String? = null,
)

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
    refusalProblems(snapshot, edit, deviceNewestWrite)?.let { return PlanResult.Refused(it) }

    val warnings = mutableListOf<PlanWarning>()
    val changes = buildList {
        addAll(planProgram(snapshot, edit))
        addAll(planSlots(snapshot, edit, warnings))
        addAll(planTemplates(snapshot, edit))
        addAll(planTemplateExercises(snapshot, edit))
        addAll(planSets(snapshot, edit))
        addAll(planExercises(snapshot, edit, warnings))
    }
    return PlanResult.Ready(changes, warnings)
}

/**
 * Why the edit cannot be planned, or null when it can (N171).
 *
 * The order is the order the answers depend on: the envelope, the two programs, the body, and only
 * then the clock — a document that is not a document is not stale. Validation runs before the
 * staleness check because a refused document's warnings would be about rows that will not be written.
 */
private fun refusalProblems(
    snapshot: ProgramDocument,
    edit: ProgramDocument,
    deviceNewestWrite: Long?,
): List<ProgramProblem>? {
    val marker = edit.edit
    val shape = when {
        marker == null -> ProgramProblem(ProblemCode.NO_EDIT_MARKER, "edit")
        edit.program.id != snapshot.program.id ->
            ProgramProblem(ProblemCode.PROGRAM_MISMATCH, "program.id", edit.program.id)

        snapshot.program.deletedAt != null ->
            ProgramProblem(ProblemCode.PROGRAM_MISSING, "program.id", edit.program.id)

        edit.program.deletedAt != null -> ProgramProblem(ProblemCode.MALFORMED, "program.deletedAt")
        else -> null
    }
    val invalid = validate(edit)
    val stale = marker?.let { staleProblem(snapshot, it, deviceNewestWrite) }
    return shape?.let { listOf(it) }
        ?: invalid.takeIf { it.isNotEmpty() }
        ?: stale?.let { listOf(it) }
}

/** The staleness refusal, or null when the edit's base still matches every row the device holds. */
private fun staleProblem(
    snapshot: ProgramDocument,
    marker: EditMarker,
    deviceNewestWrite: Long?,
): ProgramProblem? {
    val fromDocument = latestWrite(snapshot)
    val newestWrite = if (deviceNewestWrite == null) {
        fromDocument
    } else {
        maxOf(fromDocument, deviceNewestWrite)
    }
    return if (newestWrite > marker.baseExportedAt) {
        ProgramProblem(ProblemCode.STALE_BASE, "edit.baseExportedAt", newestWrite.toString())
    } else {
        null
    }
}

private fun planProgram(snapshot: ProgramDocument, edit: ProgramDocument): List<Change> {
    val changes = mutableListOf<Change>()
    if (snapshot.program.name != edit.program.name) {
        changes += Change(ChangeKind.PROGRAM_RENAMED, edit.program.id, edit.program.name)
    }
    // A block's shape is part of what a document describes, so an edit may author it (N186). It is
    // not placement: `isActive` and `position` stay the device's, because they are the program's
    // place among programs rather than the program's plan.
    if (snapshot.program.kind != edit.program.kind) {
        changes += Change(
            ChangeKind.PROGRAM_KIND,
            edit.program.id,
            edit.program.name,
            edit.program.kind.name,
        )
    }
    if (snapshot.program.startDate != edit.program.startDate) {
        changes += Change(
            ChangeKind.PROGRAM_START,
            edit.program.id,
            edit.program.name,
            edit.program.startDate?.toString(),
        )
    }
    return changes
}

private fun planSlots(
    snapshot: ProgramDocument,
    edit: ProgramDocument,
    warnings: MutableList<PlanWarning>,
): List<Change> {
    val changes = mutableListOf<Change>()
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
    warnings += keptWorkoutWarnings(snapshot, edit, editSlotIds)
    return changes
}

/**
 * The workouts whose day left the schedule: the template stays in the library, and saying so is the
 * difference between a deletion and a loss (N73).
 */
private fun keptWorkoutWarnings(
    snapshot: ProgramDocument,
    edit: ProgramDocument,
    editSlotIds: Set<String>,
): List<PlanWarning> {
    val keptTemplateIds = edit.slots.map { it.templateId }.toSet()
    return snapshot.slots
        .filter { it.id !in editSlotIds }
        .map { it.templateId }
        .filter { it !in keptTemplateIds }
        .distinct()
        .map { templateId ->
            PlanWarning(
                WarningCode.WORKOUT_KEPT,
                templateId,
                snapshot.templates.firstOrNull { it.id == templateId }?.name,
            )
        }
}

private fun planTemplates(snapshot: ProgramDocument, edit: ProgramDocument): List<Change> {
    val snapshotTemplates = snapshot.templates.associateBy { it.id }
    return edit.templates.mapNotNull { template ->
        val old = snapshotTemplates[template.id]
        when {
            old == null -> Change(ChangeKind.TEMPLATE_ADDED, template.id, template.name)
            !sameTemplate(old, template) ->
                Change(ChangeKind.TEMPLATE_UPDATED, template.id, template.name)

            else -> null
        }
    }
}

private fun planTemplateExercises(snapshot: ProgramDocument, edit: ProgramDocument): List<Change> {
    val editTemplateIds = edit.templates.map { it.id }.toSet()
    val editIds = edit.templateExercises.map { it.id }.toSet()
    val snapshotRows = snapshot.templateExercises.associateBy { it.id }
    val changed = edit.templateExercises.mapNotNull { planned ->
        val old = snapshotRows[planned.id]
        when {
            old == null -> Change(ChangeKind.TEMPLATE_EXERCISE_ADDED, planned.id, null)
            !sameTemplateExercise(old, planned) ->
                Change(ChangeKind.TEMPLATE_EXERCISE_UPDATED, planned.id, null)

            else -> null
        }
    }
    val removed = snapshot.templateExercises
        .filter { it.id !in editIds && it.templateId in editTemplateIds }
        .map { Change(ChangeKind.TEMPLATE_EXERCISE_REMOVED, it.id, null) }
    return changed + removed
}

private fun planSets(snapshot: ProgramDocument, edit: ProgramDocument): List<Change> {
    val editExerciseIds = edit.templateExercises.map { it.id }.toSet()
    val editIds = edit.templateSets.map { it.id }.toSet()
    val snapshotSets = snapshot.templateSets.associateBy { it.id }
    val changed = edit.templateSets.mapNotNull { set ->
        val old = snapshotSets[set.id]
        when {
            old == null -> Change(ChangeKind.SET_ADDED, set.id, null)
            !sameSet(old, set) -> Change(ChangeKind.SET_UPDATED, set.id, null)
            else -> null
        }
    }
    // A set removed with its exercise is covered by that exercise's removal; only a set removed from a
    // plan that keeps the exercise is its own change.
    val removed = snapshot.templateSets
        .filter { it.id !in editIds && it.templateExerciseId in editExerciseIds }
        .map { Change(ChangeKind.SET_REMOVED, it.id, null) }
    return changed + removed
}

private fun planExercises(
    snapshot: ProgramDocument,
    edit: ProgramDocument,
    warnings: MutableList<PlanWarning>,
): List<Change> {
    val known = snapshot.exercises.map { it.id }.toSet()
    val added = edit.exercises.filter { it.id !in known }
    warnings += added.map { PlanWarning(WarningCode.NEW_MOVEMENT, it.id, it.name) }
    return added.map { Change(ChangeKind.EXERCISE_ADDED, it.id, it.name) }
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
