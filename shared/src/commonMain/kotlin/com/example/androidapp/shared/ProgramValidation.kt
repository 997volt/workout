package com.example.androidapp.shared

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
 */
fun validate(document: ProgramDocument): List<ProgramProblem> {
    val problems = mutableListOf<ProgramProblem>()
    fun refuse(code: ProblemCode, path: String = "", detail: String? = null) {
        problems += ProgramProblem(code, path, detail)
    }

    if (document.program.id.isBlank()) refuse(ProblemCode.MALFORMED, "program.id")
    if (document.program.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "program.name")

    val templateIds = document.templates.map { it.id }.toSet()
    val exerciseIds = document.exercises.map { it.id }.toSet()
    val exerciseById = document.exercises.associateBy { it.id }
    val templateExerciseIds = document.templateExercises.map { it.id }.toSet()

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

    document.templates.forEachIndexed { index, template ->
        if (template.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "templates[$index].name")
    }

    document.exercises.forEachIndexed { index, exercise ->
        if (exercise.name.isBlank()) refuse(ProblemCode.EMPTY_NAME, "exercises[$index].name")
        if (exercise.rowKind != ROW_KIND_MOVEMENT && exercise.rowKind != ROW_KIND_CATEGORY) {
            refuse(ProblemCode.MALFORMED, "exercises[$index].rowKind", exercise.rowKind)
        }
    }

    // A slot is a place in the schedule: its program is this one, its workout is in the document, and
    // no two slots share a place.
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

    document.templateExercises.forEachIndexed { index, planned ->
        val path = "templateExercises[$index]"
        if (planned.templateId !in templateIds) {
            refuse(ProblemCode.DANGLING_TEMPLATE_EXERCISE, "$path.templateId", planned.templateId)
        }
        if (planned.exerciseId !in exerciseIds) {
            refuse(ProblemCode.DANGLING_EXERCISE, "$path.exerciseId", planned.exerciseId)
        } else if (exerciseById[planned.exerciseId]?.rowKind != ROW_KIND_MOVEMENT) {
            // A category head is never offered and never logged (N95); a plan cannot name one.
            refuse(ProblemCode.DANGLING_EXERCISE, "$path.exerciseId", planned.exerciseId)
        }
        if (planned.position < 0) refuse(ProblemCode.INVALID_POSITION, "$path.position")
        planned.restSeconds?.let { rest ->
            if (rest < 0 || rest > 3600) refuse(ProblemCode.MALFORMED, "$path.restSeconds")
        }
        planned.targetRpeHalves?.let { halves ->
            if (halves !in 1..20) refuse(ProblemCode.INVALID_RPE, "$path.targetRpeHalves")
        }
    }
    // Positions are a template's own order, so uniqueness is per template rather than per document.
    document.templateExercises.groupBy { it.templateId }.forEach { (templateId, rows) ->
        duplicates(rows.map { it.position }).forEach {
            refuse(ProblemCode.INVALID_POSITION, "templateExercises", "$templateId/$it")
        }
    }

    document.templateSets.forEachIndexed { index, set ->
        val path = "templateSets[$index]"
        if (set.templateExerciseId !in templateExerciseIds) {
            refuse(ProblemCode.DANGLING_SET, "$path.templateExerciseId", set.templateExerciseId)
        }
        if (set.setIndex < 1) refuse(ProblemCode.INVALID_SET_INDEX, "$path.setIndex")
        if ((set.targetWeightGrams ?: 0L) < 0L) refuse(ProblemCode.INVALID_LOAD, "$path.targetWeightGrams")
        if ((set.targetAssistanceGrams ?: 0L) < 0L) {
            refuse(ProblemCode.INVALID_LOAD, "$path.targetAssistanceGrams")
        }
        if ((set.dropValueGrams ?: 0L) < 0L) refuse(ProblemCode.INVALID_LOAD, "$path.dropValueGrams")
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

    // A set's index is unique within its exercise, and **not** contiguous: a deleted set keeps its
    // index and re-logging appends at the end (B157), so a live plan legitimately reads 1, 3.
    document.templateSets.groupBy { it.templateExerciseId }.forEach { (plannedId, rows) ->
        duplicates(rows.map { it.setIndex }).forEach {
            refuse(ProblemCode.INVALID_SET_INDEX, "templateSets", "$plannedId/$it")
        }
        validateRuns(rows.sortedBy { it.setIndex }, plannedId, ::refuse)
    }

    return problems
}

/**
 * A run's shape (ROADMAP N79): a rung sits under something, a run's first rung names its value if it
 * is a drop, the rest of the run continues the same kind, and a rung carries no load or reps of its own.
 */
private fun validateRuns(
    ordered: List<TemplateSetDto>,
    plannedId: String,
    refuse: (ProblemCode, String, String?) -> Unit,
) {
    ordered.forEachIndexed { index, set ->
        if (!set.role.isRung) return@forEachIndexed
        val path = "templateSets($plannedId)[$index]"
        if (set.targetWeightGrams != null || set.targetAssistanceGrams != null ||
            set.targetRepsMin != null || set.targetRepsMax != null
        ) {
            // A rung's load and reps are derived from its anchor, so writing either back would put a
            // number on the row that nothing reads (N79).
            refuse(ProblemCode.INVALID_RUNG, path, "a rung carries no targets")
        }
        val above = ordered.getOrNull(index - 1)
        when {
            above == null -> refuse(ProblemCode.INVALID_RUNG, path, "no anchor above")
            above.role == ProgramSetRole.WARMUP -> {
                refuse(ProblemCode.INVALID_RUNG, path, "a warm-up cannot anchor a run")
            }

            !above.role.isRung -> {
                // The run starts here: a drop has to name the value it takes off.
                if (set.role == ProgramSetRole.DROP && (set.dropValueGrams ?: 0L) <= 0L) {
                    refuse(ProblemCode.INVALID_RUNG, path, "a drop run needs its value")
                }
            }

            above.role != set.role -> {
                refuse(ProblemCode.INVALID_RUNG, path, "a run is one kind of rung")
            }
        }
    }
}

private fun duplicates(ids: List<Any>): List<String> =
    ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { it.toString() }
