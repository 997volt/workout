package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Reads and writes a program document (ROADMAP N47).
 *
 * Pure, like [BackupCodec]: strings in, strings out, so the format is covered by fast JVM tests and
 * the file IO stays in the UI layer where the Storage Access Framework lives.
 */
object ProgramDocumentCodec {

    /** The version this build writes, and the newest it will read. */
    const val CURRENT_FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        // Same rule as the backup: a newer document may carry fields this build has not heard of,
        // which is only safe because the version is checked first.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(document: ProgramDocument): String = json.encodeToString(document)

    /**
     * Parses [text], or throws [InvalidInputException] with a message worth showing.
     *
     * The version gate is explicit rather than best-effort, for [BackupCodec.decode]'s reason: a
     * document from a newer app may hold something this build cannot represent, and a partial read
     * would look like success while quietly dropping it.
     */
    fun decode(text: String): ProgramDocument {
        val document = try {
            json.decodeFromString<ProgramDocument>(text)
        } catch (malformed: SerializationException) {
            // The cause is kept, not discarded: the user sees the plain message, while a bug
            // report still has the parser's complaint.
            throw InvalidInputException("That does not look like a program file.", malformed)
        }

        if (document.formatVersion > CURRENT_FORMAT_VERSION) {
            throw InvalidInputException(
                "That program was written by a newer version of the app " +
                    "(file v${document.formatVersion}, this build reads v$CURRENT_FORMAT_VERSION).",
            )
        }
        return document
    }
}
