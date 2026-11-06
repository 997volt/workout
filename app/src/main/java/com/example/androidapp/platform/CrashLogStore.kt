package com.example.androidapp.platform

import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * One recorded crash (ROADMAP F11).
 *
 * **There is deliberately no exception message here.** A message is the one field
 * that can carry user data: a parse failure quotes the value it choked on, so
 * "cannot parse 102.5" would put a weight in the crash log — exactly what the
 * quality bar forbids. Class name plus stack trace is enough to find the fault;
 * frames carry types and methods, not values.
 *
 * No notes, no set values, no body measurements, and no `INTERNAL` permission story
 * to reason about: this never leaves the device unless the user exports it.
 */
/** How many crash files the store keeps (B361); enough to diagnose a loop, bounded on disk. */
internal const val MAX_CRASH_LOGS = 50

@Serializable
data class CrashLog(
    val timestamp: Long,
    val exceptionClass: String,
    val stackTrace: String,
    val appVersion: String,
    val versionCode: Int,
    val androidVersion: String,
    val deviceModel: String,
)

/** The device and build facts worth attaching to a crash, kept injectable for tests. */
data class CrashMetadata(
    val appVersion: String,
    val versionCode: Int,
    val androidVersion: String,
    val deviceModel: String,
)

/**
 * Crash logs on disk, one small JSON file each.
 *
 * Files rather than the database on purpose: the crash may *be* a database failure,
 * and a recorder that needs Room to record a Room crash records nothing. One file
 * per crash also means a partial write can only lose the newest record.
 *
 * Takes a directory rather than a `Context` so the whole thing is testable on the
 * JVM with a temp folder — no device, no Robolectric.
 */
class CrashLogStore(private val directory: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /** Best-effort: a recorder that throws while recording is worse than none. */
    fun record(log: CrashLog): Boolean = runCatching {
        if (!directory.exists() && !directory.mkdirs()) return false
        File(directory, "crash-${log.timestamp}-${log.exceptionClass.hashCode()}.json")
            .writeText(json.encodeToString(log))
        prune()
        true
    }.getOrDefault(false)

    /**
     * Newest first. Unreadable files are skipped rather than throwing.
     *
     * The store is bounded (B361): a launch-time crash loop must not grow the directory for as long as it
     * repeats, and every export carries these files.
     */
    fun all(): List<CrashLog> = runCatching {
        crashFiles()
            ?.mapNotNull { file ->
                runCatching { json.decodeFromString<CrashLog>(file.readText()) }.getOrNull()
            }
            ?.sortedByDescending { it.timestamp }
            .orEmpty()
    }.getOrDefault(emptyList())

    /**
     * The most recent crash, or null.
     *
     * The filename carries the timestamp the record was written with, so the newest file is chosen without
     * parsing the rest (B361); a corrupt newest file falls through to the next valid one rather than making
     * "did we crash last time?" answer null.
     */
    fun latest(): CrashLog? = runCatching {
        crashFiles()
            ?.sortedByDescending(::timestampOf)
            ?.firstNotNullOfOrNull { file ->
                runCatching { json.decodeFromString<CrashLog>(file.readText()) }.getOrNull()
            }
    }.getOrNull()

    /** The app's own files, newest-name first; other JSON in the directory is not ours to prune. */
    private fun crashFiles(): List<File>? =
        directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash-") && it.name.endsWith(".json") }

    private fun timestampOf(file: File): Long =
        file.name.removePrefix("crash-").substringBefore('-').toLongOrNull() ?: file.lastModified()

    /** Keep only the newest [MAX_CRASH_LOGS]; a malformed name is old enough to go first. */
    private fun prune() {
        crashFiles()
            ?.sortedByDescending(::timestampOf)
            ?.drop(MAX_CRASH_LOGS)
            ?.forEach { it.delete() }
    }

    fun clear(): Boolean = runCatching {
        directory.listFiles()?.forEach { it.delete() }
        true
    }.getOrDefault(false)
}

/** The device and build facts a crash -- or a repair that failed -- is recorded with. */
internal fun androidCrashMetadata(context: Context): CrashMetadata {
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()

    @Suppress("DEPRECATION") // versionCode: fine on minSdk 26, longVersionCode is 28+
    val code = info?.versionCode ?: 0
    return CrashMetadata(
        appVersion = info?.versionName ?: "unknown",
        versionCode = code,
        androidVersion = Build.VERSION.RELEASE ?: "unknown",
        deviceModel = Build.MODEL ?: "unknown",
    )
}
