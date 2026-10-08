package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Reads and writes the backup file (ROADMAP P1.12).
 *
 * Pure: it takes and returns strings, so the format is covered by fast JVM tests
 * and the file IO stays in the UI layer where the Storage Access Framework lives.
 */
object BackupCodec {

    /**
     * The version this build writes, and the newest it will read.
     *
     * Bump it whenever a [BackupFile] field changes **meaning** or a newer file could otherwise carry
     * data an older build cannot represent, so that build refuses the file rather than silently
     * dropping what it does not understand. Adding a field does not need a bump — an unknown key is
     * ignored and the field is simply absent — and neither does *removing* one: the value is not in
     * the file at all, so an older build's default for it is the truth rather than a loss. N64 added
     * `weightUnit` and N73 removed the two prescription collections on exactly those grounds.
     *
     * **N75 was the first change that needed it.** It added enum *values* — `LATS`, `UPPER_BACK`,
     * `LOWER_BACK`, `ADDUCTORS` — which an older build cannot represent, so this moved to 2. What the
     * bump could not do on its own was deliver its own message: the version used to be read out of the
     * *decoded* document, and the decoder threw on the very enum name the bump exists for, so a newer
     * file was reported as corrupt. [gatedDocument] reads the version before the body now (B62).
     *
     * **N95 moved it to 3.** A library row carries `rowKind` and `parentId`, and `RowKind.CATEGORY` is a
     * value a build without the field cannot represent: that build would accept the file, drop both keys,
     * and turn every head into a loggable lift — flattening every family instead of refusing the file as
     * newer. Adding the two fields is exactly the case the paragraph above excludes (a plain addition), but
     * the *meaning* of a movement row is what changed when a second value appeared (B90).
     */
    const val CURRENT_SCHEMA_VERSION = 3

    private val json = Json {
        prettyPrint = true
        // A file from a newer app may carry fields this build has never heard of.
        // Ignoring them is only safe because schemaVersion is checked first.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    /**
     * Parses [text], or throws [InvalidInputException] with a message worth showing.
     *
     * The version gate is explicit rather than best-effort, and it runs **before** the body is
     * decoded: a file from a newer app may hold data this build cannot represent, and an unknown enum
     * name makes the decoder throw — so a gate behind the decoder would answer "corrupt" to a file
     * whose only fault is being new (B62).
     */
    fun decode(text: String): BackupFile {
        val element = gatedDocument(text, "schemaVersion", CURRENT_SCHEMA_VERSION, "backup")
        return try {
            json.decodeFromJsonElement<BackupFile>(element)
        } catch (malformed: SerializationException) {
            // A known version whose body this build cannot read is the corrupt case, and says so.
            throw InvalidInputException("That does not look like a backup file.", malformed)
        }
    }
}
