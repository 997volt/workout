package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.shared.ProblemCode
import com.example.androidapp.shared.ProgramFormatException
import com.example.androidapp.shared.ProgramProblem
import com.example.androidapp.shared.validate
import com.example.androidapp.shared.ProgramDocumentCodec as SharedProgramDocumentCodec

/**
 * Reads and writes a program document (ROADMAP N47, N170).
 *
 * The format itself lives in `:shared` now: the page writes it, the app reads it, and this object is
 * the app's door to that one implementation rather than a second one kept equal by hand. It still
 * takes and returns strings, so the format is covered by fast JVM tests and the file IO stays in the
 * UI layer where the Storage Access Framework lives.
 *
 * The version is checked **before** the body, for the reason the shared gate does it: a newer document
 * may carry an enum name this build lacks, and a decoder that ran first would answer "corrupt" to a
 * file whose only fault is being new.
 */
object ProgramDocumentCodec {

    /** The version this build writes, and the newest it reads. */
    const val CURRENT_FORMAT_VERSION: Int = SharedProgramDocumentCodec.CURRENT_FORMAT_VERSION

    /** The oldest document this build reads: 2.0's own, and nothing earlier (ROADMAP N104). */
    const val MINIMUM_FORMAT_VERSION: Int = SharedProgramDocumentCodec.MINIMUM_FORMAT_VERSION

    fun encode(document: ProgramDocument): String =
        SharedProgramDocumentCodec.encode(document.toShared())

    /**
     * Parses [text], or throws [InvalidInputException] with a message worth showing.
     *
     * The shared gate answers with codes, not English, because the page renders its own sentences.
     * Turning a code into the app's wording is this object's one job.
     */
    /**
     * The shared validator's problems for a decoded document (B328).
     *
     * The document is a write path: the import merges its rows straight into the store, so the rules the page
     * checks before it writes have to run here too. A hand-edited set that is counted and held, a duplicate id
     * or a dangling reference must be refused before the merge rather than written and found later.
     */
    fun problems(document: ProgramDocument): List<ProgramProblem> = validate(document.toShared())

    fun decode(text: String): ProgramDocument =
        try {
            SharedProgramDocumentCodec.decode(text).toApp()
        } catch (refused: ProgramFormatException) {
            throw refused.toInvalidInput()
        } catch (unknownName: IllegalArgumentException) {
            // A known version whose body names an enum value this build lacks: the shared model keeps
            // taxonomy as strings, so the refusal happens on the way into the app's types.
            throw InvalidInputException("That does not look like a program file.", unknownName)
        }
}

private fun ProgramFormatException.toInvalidInput(): InvalidInputException = when (problem.code) {
    ProblemCode.NEWER_VERSION -> InvalidInputException(
        "That program was written by a newer version of the app " +
            "(file v${problem.detail ?: "?"}, this build reads v${ProgramDocumentCodec.CURRENT_FORMAT_VERSION}).",
    )

    ProblemCode.OLDER_VERSION -> InvalidInputException(
        "That program was written by an older version of the app " +
            "(file v${problem.detail ?: "?"}, this build reads " +
            "v${ProgramDocumentCodec.MINIMUM_FORMAT_VERSION} and later).",
    )

    else -> InvalidInputException("That does not look like a program file.")
}
