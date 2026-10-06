package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Reads and writes a program document (ROADMAP N47).
 *
 * Pure, like [BackupCodec]: strings in, strings out, so the format is covered by fast JVM tests and
 * the file IO stays in the UI layer where the Storage Access Framework lives.
 */
object ProgramDocumentCodec {

    /**
     * The version this build writes, and the newest it will read.
     *
     * N75 and N79 moved it to 2: a program document carries the exercise DTOs and the planned sets, so
     * the split muscle names (`LATS`, `UPPER_BACK`, `LOWER_BACK`, `ADDUCTORS`) and `SetType.CLUSTER`
     * travel in it, and a build without those constants cannot represent them. The backup's version
     * moved for the same reason and on the same rule.
     */
    const val CURRENT_FORMAT_VERSION = 2

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
     * The gateway is explicit rather than best-effort and, like the backup's, reads the version before
     * the body: a document whose only fault is being newer must be answered as newer, and an enum name
     * this build lacks makes the decoder throw before a gate behind it could run ([gatedDocument]).
     */
    fun decode(text: String): ProgramDocument {
        val element = gatedDocument(text, "formatVersion", CURRENT_FORMAT_VERSION, "program")
        return try {
            json.decodeFromJsonElement<ProgramDocument>(element)
        } catch (malformed: SerializationException) {
            // A known version whose body this build cannot read is the corrupt case, and says so.
            throw InvalidInputException("That does not look like a program file.", malformed)
        }
    }
}
