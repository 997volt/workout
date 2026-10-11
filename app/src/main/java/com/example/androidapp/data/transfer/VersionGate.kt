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
 * sentences a lifter sees. Text that is not JSON, is not an object, or names no version is the
 * **corrupt** case rather than the newer one: there is nothing to compare it against.
 *
 * **A minimum as well as a maximum** (ROADMAP N104). 2.0 starts a new store and refuses 1.x rather than
 * reading it, so the gate answers both ends of the range: too new is a build that cannot represent the
 * data, too old is a shape this build no longer carries — and both are refused *here*, before the
 * decoder, for the reason above. That is what lets 2.0 delete the four retired pattern names: a 1.x row
 * carrying one is never decoded, where before the decoder was the thing that would have thrown on it.
 */
internal fun gatedDocument(
    text: String,
    field: String,
    currentVersion: Int,
    noun: String,
    minimumVersion: Int = 1,
): JsonElement {
    val element = try {
        Json.parseToJsonElement(text)
    } catch (malformed: SerializationException) {
        // The cause is kept, not discarded: the user sees the plain message, while a bug report
        // still has the parser's complaint.
        throw InvalidInputException("That does not look like a $noun file.", malformed)
    }
    val version = ((element as? JsonObject)?.get(field) as? JsonPrimitive)?.intOrNull
    // One refusal, three sentences: text that names no version is corrupt, a version above the top of the
    // range is newer, and one below the bottom is older. They are decided together so the reader sees the
    // whole answer.
    val refusal = when {
        version == null -> "That does not look like a $noun file."
        version > currentVersion ->
            "That $noun was written by a newer version of the app " +
                "(file v$version, this build reads v$currentVersion)."
        version < minimumVersion ->
            "That $noun was written by an older version of the app " +
                "(file v$version, this build reads v$minimumVersion and later)."
        else -> null
    }
    return refusal?.let { throw InvalidInputException(it) } ?: element
}
