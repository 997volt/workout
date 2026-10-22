package com.example.androidapp.shared

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

/**
 * Why a document cannot be read, as a code both hosts can turn into their own sentence (ROADMAP N170).
 *
 * The shared core deliberately carries no English: the app renders `strings.xml` and the page renders
 * its own copy, and a message shared across both would be the first thing to disagree with either.
 * [detail] is data for that sentence — a version number, an id — never prose.
 */
enum class ProblemCode {
    /** The text is not a JSON object, or the body does not decode. */
    MALFORMED,

    /** The root names no `formatVersion`. */
    MISSING_VERSION,

    /** Written by a newer build than this one reads. */
    NEWER_VERSION,

    /** Written before the oldest version this build reads. */
    OLDER_VERSION,

    /** The document is a proposal to add, but an edit was expected. */
    NO_EDIT_MARKER,

    /** The device changed the program after the edit's base. */
    STALE_BASE,

    /** The edit names a different program than the one it was made from. */
    PROGRAM_MISMATCH,

    /** The snapshot does not hold the program the edit names. */
    PROGRAM_MISSING,

    /** One id is used twice in the collection that must be unique. */
    DUPLICATE_ID,

    /** A slot names a template the document does not carry. */
    DANGLING_SLOT_TEMPLATE,

    /** A template exercise names a template the document does not carry. */
    DANGLING_TEMPLATE_EXERCISE,

    /** A planned set names a template exercise the document does not carry. */
    DANGLING_SET,

    /** A planned exercise names a library row the document does not carry. */
    DANGLING_EXERCISE,

    /** A program, template or exercise row has no name. */
    EMPTY_NAME,

    /** A position is negative, or two rows of one collection share it. */
    INVALID_POSITION,

    /** A set's index is not a contiguous 1..n within its exercise. */
    INVALID_SET_INDEX,

    /** A rep target is below one, or a range runs backwards. */
    INVALID_REPS,

    /** A load or assistance is negative. */
    INVALID_LOAD,

    /** A target effort is outside 1..10. */
    INVALID_RPE,

    /** A rest is below zero or above an hour. */
    INVALID_REST,

    /** A rung is the first set of its plan, so nothing above it says what it takes off. */
    RUNG_WITHOUT_ANCHOR,

    /** A warm-up is the set above the rung, and a warm-up is not a working set to run down from. */
    RUNG_UNDER_WARMUP,

    /** A run's first rung is a drop and names no value to take off. */
    RUNG_WITHOUT_VALUE,

    /** A run changes kind part-way: a drop cannot continue as a cluster, or the reverse. */
    RUN_MIXES_KINDS,

    /** A rung carries a load or a rep target, which it has to take from the set above it instead. */
    RUNG_CARRIES_TARGETS,
}

/** One refusal, located where the reader can act on it. */
data class ProgramProblem(
    val code: ProblemCode,
    /** A dotted path into the document, e.g. `templateSets[3].setIndex`. */
    val path: String = "",
    /** Data for the host's sentence — a version number, an id — never prose. */
    val detail: String? = null,
)

/** Thrown by [ProgramDocumentCodec.decode] when the text is not a document this build reads. */
class ProgramFormatException(val problem: ProgramProblem) : Exception(problem.code.name)

/**
 * Reads and writes a program document (ROADMAP N47, N170).
 *
 * The same codec the app has used since 2.0, moved into the shared core so the page writes what the
 * app reads. The version is checked **before** the body, for the reason the app's gate is: a newer
 * document may carry an enum name this build lacks, and a decoder that ran first would answer
 * "corrupt" to a file whose only fault is being new.
 */
object ProgramDocumentCodec {

    /** The version this build writes, and the newest it reads. */
    const val CURRENT_FORMAT_VERSION: Int = PROGRAM_FORMAT_VERSION

    /** The oldest document this build reads: 2.0's own, and nothing earlier (N104). */
    const val MINIMUM_FORMAT_VERSION: Int = PROGRAM_FORMAT_VERSION

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(document: ProgramDocument): String = json.encodeToString(document)

    fun decode(text: String): ProgramDocument {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (malformed: SerializationException) {
            throw ProgramFormatException(ProgramProblem(ProblemCode.MALFORMED))
        }
        val version = ((element as? JsonObject)?.get("formatVersion") as? JsonPrimitive)?.intOrNull
        when {
            version == null -> throw ProgramFormatException(ProgramProblem(ProblemCode.MISSING_VERSION))
            version > CURRENT_FORMAT_VERSION -> throw ProgramFormatException(
                ProgramProblem(ProblemCode.NEWER_VERSION, detail = version.toString()),
            )

            version < MINIMUM_FORMAT_VERSION -> throw ProgramFormatException(
                ProgramProblem(ProblemCode.OLDER_VERSION, detail = version.toString()),
            )
        }
        return try {
            json.decodeFromJsonElement<ProgramDocument>(element)
        } catch (malformed: SerializationException) {
            throw ProgramFormatException(ProgramProblem(ProblemCode.MALFORMED))
        }
    }
}
