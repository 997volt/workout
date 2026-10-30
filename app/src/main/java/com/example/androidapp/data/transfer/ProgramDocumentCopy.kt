package com.example.androidapp.data.transfer

/**
 * A program document as a **copy** under another name (ROADMAP N243).
 *
 * The load path is insert-only and never edits a program (B180), so a document whose program id is already
 * here with different work can only be refused — which is right, and leaves the lifter with nothing to do
 * inside the app. A copy is the way out: the same plan, a name of its own, and an id nothing can collide with.
 *
 * **Every row id is minted, and the references are rewired with it.** A copy that kept the original's workout
 * ids would not be a second program at all: the merge inserts nothing it already holds, so both programs would
 * name the same templates — editing one day would rewrite both, and N171's staleness check would then refuse
 * documents for the other. The **exercise ids are kept**, because those name library movements rather than
 * copying them: a copy of a program trains the movements the library already has.
 *
 * A **slot** may name a workout the device holds without the document describing it, and the copy keeps that
 * reference. A planned exercise cannot: it belongs to its workout, and a workout the copy does not carry is the
 * original's, so those rows are dropped rather than written into the program the copy came from (B302).
 *
 * [newId] is a parameter rather than a call to `UUID` so this is a pure function of its inputs — the caller
 * mints and this decides.
 */
fun programDocumentCopy(
    document: ProgramDocument,
    name: String,
    newId: () -> String,
): ProgramDocument {
    val programId = newId()
    val templateIds = document.templates.associate { it.id to newId() }
    // Only planned work whose workout the document carries is the copy's; the rest named a template the
    // device holds (the original's own), and copying that reference would share rows between two programs.
    val carriedPlanned = document.templateExercises.filter { it.templateId in templateIds }
    val plannedIds = carriedPlanned.associate { it.id to newId() }
    return document.copy(
        // A document is an *add*, so a copy is one too: the edit marker is what turns a load into a proposal
        // to change a program this device holds (N171).
        edit = null,
        program = document.program.copy(
            id = programId,
            name = name.trim(),
            // Following a program is a choice rather than something a file makes (P3.3), and a copy arrives at
            // the end of the list like every other add (P3.12).
            isActive = false,
            position = 0,
        ),
        slots = document.slots.map { slot ->
            slot.copy(
                id = newId(),
                programId = programId,
                templateId = templateIds[slot.templateId] ?: slot.templateId,
            )
        },
        templates = document.templates.map { template ->
            template.copy(
                id = templateIds.getValue(template.id),
                // A copy is a new definition rather than a version of the original, so it starts its own
                // lineage: `versionOf` is what a program document's staleness is measured against (B287).
                versionOf = null,
            )
        },
        templateExercises = carriedPlanned.map { planned ->
            planned.copy(
                id = plannedIds.getValue(planned.id),
                templateId = templateIds.getValue(planned.templateId),
            )
        },
        templateSets = document.templateSets.filter { it.templateExerciseId in plannedIds }.map { set ->
            set.copy(
                id = newId(),
                templateExerciseId = plannedIds.getValue(set.templateExerciseId),
            )
        },
        // **The library's rows are not copied**: a movement a document carries is the same movement, and the
        // merge adds what the device does not have.
        exercises = document.exercises,
    )
}
