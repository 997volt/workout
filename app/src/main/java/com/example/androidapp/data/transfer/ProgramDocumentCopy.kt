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
 * A reference the document does not carry is left alone, which is the merge's own rule: a slot may name a
 * workout the device holds, and a document never describes one it does not.
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
    val plannedIds = document.templateExercises.associate { it.id to newId() }
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
        templateExercises = document.templateExercises.map { planned ->
            planned.copy(
                id = plannedIds.getValue(planned.id),
                templateId = templateIds[planned.templateId] ?: planned.templateId,
            )
        },
        templateSets = document.templateSets.map { set ->
            set.copy(
                id = newId(),
                templateExerciseId = plannedIds[set.templateExerciseId] ?: set.templateExerciseId,
            )
        },
        // **The library's rows are not copied**: a movement a document carries is the same movement, and the
        // merge adds what the device does not have.
        exercises = document.exercises,
    )
}
