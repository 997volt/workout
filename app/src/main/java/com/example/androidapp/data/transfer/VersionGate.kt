package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Reads a transfer document's declared version, and refuses the document before anything else is
 * decoded (ROADMAP B62).
 *
 * **The gate runs before the decoder, not after it**, which is the whole point. A document from a
 * newer build routinely holds an enum name this build has no constant for — a split muscle, a new set
 * role — and the decoder throws on the name, so a gate that read the version out of the *decoded*
 * document never got its turn: the lifter was told their file was corrupt, when the truth was that it
 * was newer. Reading the version out of the raw JSON tree first costs one parse of the same text and
 * answers the question the file actually raises.
 *
 * [field] is the format's own version key — `schemaVersion` for a backup, `formatVersion` for a
 * program document, because the two change for different reasons — and [noun] names the file in the
 * two sentences a lifter sees. Text that is not JSON, is not an object, or names no version is the
 * **corrupt** case rather than the newer one: there is nothing to compare it against.
 */
internal fun gatedDocument(
    text: String,
    field: String,
    currentVersion: Int,
    noun: String,
): JsonElement {
    val element = try {
        Json.parseToJsonElement(text)
    } catch (malformed: SerializationException) {
        // The cause is kept, not discarded: the user sees the plain message, while a bug report
        // still has the parser's complaint.
        throw InvalidInputException("That does not look like a $noun file.", malformed)
    }
    val version = ((element as? JsonObject)?.get(field) as? JsonPrimitive)?.intOrNull
    // One refusal, two sentences: text that names no version is corrupt, and a version this build is
    // too old to read is newer. They are decided together so the reader sees the whole answer.
    val refusal = when {
        version == null -> "That does not look like a $noun file."
        version > currentVersion ->
            "That $noun was written by a newer version of the app " +
                "(file v$version, this build reads v$currentVersion)."
        else -> null
    }
    return refusal?.let { throw InvalidInputException(it) } ?: element
}
