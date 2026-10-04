package com.example.androidapp.ui.transfer

import android.content.Context
import android.net.Uri
import com.example.androidapp.domain.DataError
import kotlinx.coroutines.CancellationException

/**
 * Reading and writing one text document the user chose (ROADMAP B53).
 *
 * Shared because both documents this app hands over are the same thing underneath: JSON the Storage
 * Access Framework gives back as a `Uri`, written and read as a plain string. The backup was the
 * first caller and the program document made the second, which is where this project extracts — and
 * the alternative was the same three helpers, and the same `DataError`-to-sentence mapping, written
 * twice and free to drift.
 *
 * Here rather than in either screen's composable because it needs a `Context` and a `Uri` and
 * nothing else; the ViewModels never see either.
 */

/** What both document pickers accept: this app's own JSON, and the `text/plain` some providers report it as. */
internal const val JSON_MIME_TYPE = "application/json"

/**
 * Writes [text], answering whether it landed.
 *
 * A `false` rather than a thrown failure, because every caller turns it into the same sentence. A
 * cancelled write is rethrown instead: this runs in the composable's scope, so cancellation means
 * the screen is gone rather than that the file could not be written, and reporting the latter would
 * be a lie the coroutine then refuses to finish cancelling over.
 */
internal suspend fun writeDocument(context: Context, uri: Uri, text: String): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray())
        true
    } ?: false
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Throwable) {
    false
}

/**
 * The document's text, or null when it could not be read.
 *
 * A `null` here is a readable file that is simply empty as much as it is an unreadable one, which is
 * the same failure from the user's side — the same message either way.
 */
internal suspend fun readDocument(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
} catch (cancellation: CancellationException) {
    // Not a failure, for the reason spelled out on [writeDocument].
    throw cancellation
} catch (_: Throwable) {
    null
}

/**
 * The one sentence a rejected document is reported with (ROADMAP B53).
 *
 * An [DataError.Invalid] carries its own message because the layer that rejected the file wrote it
 * for the user; everything else is the storage failure the caller's own string names.
 */
internal fun documentErrorMessage(error: DataError, storageError: String): String = when (error) {
    is DataError.Invalid -> error.message
    DataError.NotFound -> storageError
    is DataError.Storage -> storageError
}
