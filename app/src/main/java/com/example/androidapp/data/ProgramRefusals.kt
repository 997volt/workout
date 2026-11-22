package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramDocument
import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.shared.ProblemCode
import com.example.androidapp.shared.ProgramProblem
import kotlinx.coroutines.flow.first

// The sentences a program write refuses with (ROADMAP B308, B328): the document rules checked before
// either import path writes, and the two refusals the screens render.

/**
 * A refusal in the lifter's words.
 *
 * The shared core answers with codes because the page renders its own sentences; this is where the
 * app's are chosen. Stale and missing are the two the lifter can act on, so they get their own.
 */
/**
 * Refuses a document the merge cannot write faithfully, before either import path writes (B328, N358).
 *
 * The shared validator names everything wrong with a document. Four of those are references the merge
 * resolves by design: a slot or exercise naming a workout/movement this device already holds is kept, and
 * one it does not is dropped and counted, which is the `droppedMovements` line the import has always
 * shown. Every other problem describes a row the merge would write as it stands — a repeated id is
 * silently ignored by `INSERT OR IGNORE`, a repeated position is stored, and a value the plan boundary
 * refuses is written straight into `template_sets` — so those are refused here.
 */
internal fun requireValidDocument(document: ProgramDocument) {
    val problems = ProgramDocumentCodec.problems(document)
        .filterNot { it.code in DROPPED_BY_THE_MERGE }
    if (problems.isEmpty()) return
    throw InvalidInputException(
        if (document.edit != null) editRefusal(problems) else loadRefusal(problems),
    )
}

/** The reference failures the merge answers by resolving or dropping, and the summary counts. */
private val DROPPED_BY_THE_MERGE = setOf(
    ProblemCode.DANGLING_SLOT_TEMPLATE,
    ProblemCode.DANGLING_TEMPLATE_EXERCISE,
    ProblemCode.DANGLING_SET,
    ProblemCode.DANGLING_EXERCISE,
)

/** A file being opened, in the same two write-boundary sentences an edit gets (B308, B328). */
private fun loadRefusal(problems: List<ProgramProblem>): String {
    val first = problems.firstOrNull() ?: return "That program file cannot be loaded."
    return when (first.code) {
        ProblemCode.INVALID_HOLD -> "A hold must be at least one second."
        ProblemCode.COUNTED_AND_HELD ->
            "A set is either counted or held: it cannot name both reps and a hold."

        ProblemCode.EMPTY_NAME -> "Give the program a name."
        ProblemCode.DUPLICATE_ID -> "Two rows in that file share an id; it cannot be imported as it stands."
        ProblemCode.INVALID_REPS -> "A rep target must be between 1 and 10,000."
        ProblemCode.INVALID_POSITION -> "Two rows in that file sit in the same place."
        ProblemCode.INVALID_SET_INDEX -> "Two sets in that file share a place."
        ProblemCode.INVALID_LOAD -> "A target load cannot be negative."
        ProblemCode.INVALID_REST -> "A rest must be between 0 seconds and an hour."
        ProblemCode.INVALID_RPE -> "A target effort must be between 1 and 10."
        ProblemCode.INVALID_PHASE -> "That file mixes phased and unphased days."
        else ->
            "That program file does not fit the app's rules " +
                "(${first.code.name.lowercase().replace('_', ' ')})."
    }
}

internal fun editRefusal(problems: List<ProgramProblem>): String {
    val first = problems.firstOrNull() ?: return "That program edit cannot be applied."
    return when (first.code) {
        ProblemCode.STALE_BASE ->
            "This program changed on the phone since you exported it. " +
                "Export it again and redo the edit."

        ProblemCode.PROGRAM_MISSING -> "That program is not on this device."
        ProblemCode.PROGRAM_MISMATCH -> "That edit belongs to a different program."
        ProblemCode.NO_EDIT_MARKER -> "That edit does not name the export it was made from."
        // The same two rules the write boundary refuses, worded the same way (B308).
        ProblemCode.INVALID_HOLD -> "A hold must be at least one second."
        ProblemCode.COUNTED_AND_HELD ->
            "A set is either counted or held: it cannot name both reps and a hold."

        else ->
            "That program edit does not fit the app's rules " +
                "(${first.code.name.lowercase().replace('_', ' ')})."
    }
}

/** A program with no name is a list row nobody can tell apart from the next (ROADMAP P3.3). */
internal fun requireName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) throw InvalidInputException("Give the program a name.")
    return trimmed
}
