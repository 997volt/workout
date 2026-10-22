package com.example.androidapp.shared

/**
 * How one validation failure is reported: a code, a path, and an optional detail (ROADMAP N170).
 *
 * A class rather than a function type, because the checks below are passed this sink and most of
 * them name only the code and the path — a function type loses the defaults, and every call would
 * carry a `null` its reader has to decode.
 */
private class Refusal(private val sink: (ProblemCode, String, String?) -> Unit) {
    operator fun invoke(code: ProblemCode, path: String = "", detail: String? = null) {
        sink(code, path, detail)
    }
}

/** The longest rest a plan may prescribe: an hour (N21's bound, enforced for the shared host too). */
private const val MAX_REST_SECONDS = 3600

/** RPE is half-points, so 1.0–10.0 is 1..20 (N6). */
private const val RPE_HALVES_MIN = 1
private const val RPE_HALVES_MAX = 20

/**
 * Everything a program document must be true before either host acts on it (ROADMAP N170).
 *
 * Pure and shared: the page runs it before offering a download, the app runs it before writing, and
 * the two answers are the same because the code is. A path names where to look — `slots[2].templateId`
 * — so each host can say "the third day" without either one owning the other's words.
 *
 * What is deliberately **not** checked here: whether the document's ids already exist on a device, and
 * whether a template is shared with another program. Those are facts about a device, not about the
 * document, and they belong to the host that can see them.
 *
 * The checks are grouped by the thing they are about rather than written as one function: each group
 * has its own shape — a table of ids, a set of references, a run's order — and a single function of
 * all of them was the longest and most tangled in this module (N179).
 */
fun validate(document: ProgramDocument): List<ProgramProblem> {
    val problems = mutableListOf<ProgramProblem>()
    val refuse = Refusal { code, path, detail -> problems += ProgramProblem(code, path, detail) }

    if (document.program.id.isBlank()) refuse(ProblemCode.MALFORMED, "program.id")
    if (document.program.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "program.name")

    validateDuplicates(document, refuse)
    validateTemplates(document, refuse)
    validateExercises(document, refuse)
    validateSlots(document, refuse)
    validateTemplateExercises(document, refuse)
    validateSets(document, refuse)
    return problems
}

/** Every collection's ids are its own, and a repeated one makes a row ambiguous. */
private fun validateDuplicates(document: ProgramDocument, refuse: Refusal) {
    duplicates(document.templates.map { it.id }).forEach {
        refuse(ProblemCode.DUPLICATE_ID, "templates", it)
    }
    duplicates(document.slots.map { it.id }).forEach {
        refuse(ProblemCode.DUPLICATE_ID, "slots", it)
    }
    duplicates(document.templateExercises.map { it.id }).forEach {
        refuse(ProblemCode.DUPLICATE_ID, "templateExercises", it)
    }
    duplicates(document.templateSets.map { it.id }).forEach {
        refuse(ProblemCode.DUPLICATE_ID, "templateSets", it)
    }
    duplicates(document.exercises.map { it.id }).forEach {
        refuse(ProblemCode.DUPLICATE_ID, "exercises", it)
    }
}

private fun validateTemplates(document: ProgramDocument, refuse: Refusal) {
    document.templates.forEachIndexed { index, template ->
        if (template.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "templates[$index].name")
    }
}

private fun validateExercises(document: ProgramDocument, refuse: Refusal) {
    document.exercises.forEachIndexed { index, exercise ->
        if (exercise.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "exercises[$index].name")
        if (exercise.rowKind != ROW_KIND_MOVEMENT && exercise.rowKind != ROW_KIND_CATEGORY) {
            refuse(ProblemCode.MALFORMED, "exercises[$index].rowKind", exercise.rowKind)
        }
    }
}

/**
 * A slot is a place in the schedule: its program is this one, its workout is in the document, and no
 * two slots share a place.
 */
private fun validateSlots(document: ProgramDocument, refuse: Refusal) {
    val templateIds = document.templates.map { it.id }.toSet()
    document.slots.forEachIndexed { index, slot ->
        if (slot.programId != document.program.id) {
            refuse(ProblemCode.MALFORMED, "slots[$index].programId", slot.programId)
        }
        if (slot.templateId !in templateIds) {
            refuse(ProblemCode.DANGLING_SLOT_TEMPLATE, "slots[$index].templateId", slot.templateId)
        }
        if (slot.position < 0) refuse(ProblemCode.INVALID_POSITION, "slots[$index].position")
    }
    duplicates(document.slots.map { it.position }).forEach {
        refuse(ProblemCode.INVALID_POSITION, "slots", it)
    }
}

private fun validateTemplateExercises(document: ProgramDocument, refuse: Refusal) {
    val templateIds = document.templates.map { it.id }.toSet()
    val exerciseById = document.exercises.associateBy { it.id }
    document.templateExercises.forEachIndexed { index, planned ->
        val path = "templateExercises[$index]"
        if (planned.templateId !in templateIds) {
            refuse(ProblemCode.DANGLING_TEMPLATE_EXERCISE, "$path.templateId", planned.templateId)
        }
        // A category head is never offered and never logged (N95), so a plan cannot name one. An id
        // the document does not carry is the same refusal: nothing on a device can name it either.
        if (exerciseById[planned.exerciseId]?.rowKind != ROW_KIND_MOVEMENT) {
            refuse(ProblemCode.DANGLING_EXERCISE, "$path.exerciseId", planned.exerciseId)
        }
        if (planned.position < 0) refuse(ProblemCode.INVALID_POSITION, "$path.position")
        planned.restSeconds?.let { rest ->
            // Its own code: the number is a value the host can word, not a file that is not a file
            // (B186). A 4000-second rest used to answer "this is not shaped like a program".
            if (rest < 0 || rest > MAX_REST_SECONDS) {
                refuse(ProblemCode.INVALID_REST, "$path.restSeconds")
            }
        }
        planned.targetRpeHalves?.let { halves ->
            if (halves !in RPE_HALVES_MIN..RPE_HALVES_MAX) {
                refuse(ProblemCode.INVALID_RPE, "$path.targetRpeHalves")
            }
        }
    }
    // Positions are a template's own order, so uniqueness is per template rather than per document.
    document.templateExercises.groupBy { it.templateId }.forEach { (templateId, rows) ->
        duplicates(rows.map { it.position }).forEach {
            refuse(ProblemCode.INVALID_POSITION, "templateExercises", "$templateId/$it")
        }
    }
}

private fun validateSets(document: ProgramDocument, refuse: Refusal) {
    validateSetTargets(document, refuse)
    validateSetIndexes(document, refuse)
}

/** A set's anchors, its load, and the reps it is prescribed — the values a host can show and word. */
private fun validateSetTargets(document: ProgramDocument, refuse: Refusal) {
    val plannedIds = document.templateExercises.map { it.id }.toSet()
    document.templateSets.forEachIndexed { index, set ->
        val path = "templateSets[$index]"
        if (set.templateExerciseId !in plannedIds) {
            refuse(ProblemCode.DANGLING_SET, "$path.templateExerciseId", set.templateExerciseId)
        }
        if (set.setIndex < 0) refuse(ProblemCode.INVALID_SET_INDEX, "$path.setIndex")
        if ((set.targetWeightGrams ?: 0L) < 0L) {
            refuse(ProblemCode.INVALID_LOAD, "$path.targetWeightGrams")
        }
        if ((set.targetAssistanceGrams ?: 0L) < 0L) {
            refuse(ProblemCode.INVALID_LOAD, "$path.targetAssistanceGrams")
        }
        if ((set.dropValueGrams ?: 0L) < 0L) refuse(ProblemCode.INVALID_LOAD, "$path.dropValueGrams")
        validateReps(set, path, refuse)
    }
}

/** The range's ends and where the lifter is in it: a current rung outside its own range is a plan no
 * session can read. */
private fun validateReps(set: TemplateSetDto, path: String, refuse: Refusal) {
    set.targetRepsMin?.let { if (it < 1) refuse(ProblemCode.INVALID_REPS, "$path.targetRepsMin") }
    set.targetRepsMax?.let { if (it < 1) refuse(ProblemCode.INVALID_REPS, "$path.targetRepsMax") }
    val min = set.targetRepsMin
    val max = set.targetRepsMax
    if (min != null && max != null && min > max) refuse(ProblemCode.INVALID_REPS, path)
    set.targetRepsCurrent?.let { current ->
        if (current < 1) refuse(ProblemCode.INVALID_REPS, "$path.targetRepsCurrent")
        if (max != null && current > max) refuse(ProblemCode.INVALID_REPS, "$path.targetRepsCurrent")
        if (min != null && current < min) refuse(ProblemCode.INVALID_REPS, "$path.targetRepsCurrent")
    }
}

private fun validateSetIndexes(document: ProgramDocument, refuse: Refusal) {
    // A set's index is unique within its exercise, and **not** contiguous: a deleted set keeps its
    // index and re-logging appends at the end (B157), so a live plan legitimately reads 0, 2. The
    // app's indices are **0-based** — `addSet` appends `max + 1` and a ramp takes 0..n-1 (B34) — so
    // the floor is zero, not one.
    document.templateSets.groupBy { it.templateExerciseId }.forEach { (plannedId, rows) ->
        duplicates(rows.map { it.setIndex }).forEach {
            refuse(ProblemCode.INVALID_SET_INDEX, "templateSets", "$plannedId/$it")
        }
        validateRuns(rows.sortedBy { it.setIndex }, plannedId, refuse)
    }
}

/**
 * A run's shape (ROADMAP N79): a rung sits under something, a run's first rung names its value if it
 * is a drop, the rest of the run continues the same kind, and a rung carries no load or reps of its own.
 *
 * **Each cause is its own code** (B186): one `INVALID_RUNG` carried all five and an English `detail`
 * that no host read, so the page had to guess a sentence and guessed wrong for three of them. A code
 * per cause is what lets each host say which set is wrong and why.
 */
private fun validateRuns(
    ordered: List<TemplateSetDto>,
    plannedId: String,
    refuse: Refusal,
) {
    ordered.forEachIndexed { index, set ->
        if (!set.role.isRung) return@forEachIndexed
        val path = "templateSets($plannedId)[$index]"
        if (set.carriesTargets()) {
            // A rung's load and reps are derived from its anchor, so writing either back would put a
            // number on the row that nothing reads (N79).
            refuse(ProblemCode.RUNG_CARRIES_TARGETS, path, null)
        }
        val above = ordered.getOrNull(index - 1)
        when {
            above == null -> refuse(ProblemCode.RUNG_WITHOUT_ANCHOR, path, null)
            above.role == ProgramSetRole.WARMUP -> {
                refuse(ProblemCode.RUNG_UNDER_WARMUP, path, null)
            }

            !above.role.isRung -> {
                // The run starts here: a drop has to name the value it takes off.
                if (set.role == ProgramSetRole.DROP && (set.dropValueGrams ?: 0L) <= 0L) {
                    refuse(ProblemCode.RUNG_WITHOUT_VALUE, path, null)
                }
            }

            above.role != set.role -> {
                refuse(ProblemCode.RUN_MIXES_KINDS, path, null)
            }
        }
    }
}

/** True when a rung names a load or a rep target of its own, which it must derive from its anchor. */
private fun TemplateSetDto.carriesTargets(): Boolean = carriesLoad() || carriesReps()

private fun TemplateSetDto.carriesLoad(): Boolean =
    targetWeightGrams != null || targetAssistanceGrams != null

private fun TemplateSetDto.carriesReps(): Boolean = targetRepsMin != null || targetRepsMax != null

private fun duplicates(ids: List<Any>): List<String> =
    ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { it.toString() }
