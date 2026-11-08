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
 * Refuses a document whose **set targets** break the write boundary, before either import path writes (B328).
 *
 * Only the two set rules are refused here, not every problem the shared validator can name: a document that
 * names an exercise this device does not hold is a case the merge handles by dropping the movement, and the
 * import has always accepted it (and says so in its summary). A set that is counted and held, or held for no
 * time, is different — the merge writes it straight into `template_sets`, bypassing the boundary that refuses
 * it, and the row is invalid the moment it exists.
 */
internal fun requireValidDocument(document: ProgramDocument) {
    val problems = ProgramDocumentCodec.problems(document)
        .filter { it.code == ProblemCode.INVALID_HOLD || it.code == ProblemCode.COUNTED_AND_HELD }
    if (problems.isEmpty()) return
    throw InvalidInputException(
        if (document.edit != null) editRefusal(problems) else loadRefusal(problems),
    )
}

/** A file being opened, in the same two write-boundary sentences an edit gets (B308, B328). */
private fun loadRefusal(problems: List<ProgramProblem>): String {
    val first = problems.firstOrNull() ?: return "That program file cannot be loaded."
    return when (first.code) {
        ProblemCode.INVALID_HOLD -> "A hold must be at least one second."
        ProblemCode.COUNTED_AND_HELD ->
            "A set is either counted or held: it cannot name both reps and a hold."

        ProblemCode.EMPTY_NAME -> "Give the program a name."
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
