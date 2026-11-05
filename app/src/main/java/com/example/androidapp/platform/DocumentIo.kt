package com.example.androidapp.platform

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reading and writing one text document the user chose (ROADMAP B53).
 *
 * Shared because both documents this app hands over are the same thing underneath: JSON the Storage Access
 * Framework gives back as a `Uri`, written and read as a plain string. The backup was the first caller and the
 * program document made the second, which is where this project extracts.
 *
 * An interface so the transfer ViewModel can be tested on the JVM (N273): the Android half is
 * [SafDocumentIo]'s, and a fake is two functions.
 *
 * **Injected rather than a pair of free functions** (B159). The transfer has to run somewhere that outlives the
 * composable that started it — a rotation disposes the composition's scope, and the document the picker already
 * created then stays empty and unreported — so the work moved to a ViewModel, and a ViewModel may not hold a
 * composable's `Context`. This is the seam: the caller passes the `Context` it has, the implementation adds
 * nothing to it, and the dispatcher switch lives in one place so neither caller can get it wrong.
 */
interface DocumentIo {

    /**
     * Writes [text], answering whether it landed.
     *
     * A `false` rather than a thrown failure, because every caller turns it into the same sentence. A cancelled
     * write is rethrown instead: cancellation means the screen is gone rather than that the file could not be
     * written, and reporting the latter would be a lie the coroutine then refuses to finish cancelling over.
     */
    suspend fun write(context: Context, uri: Uri, text: String): Boolean

    /**
     * The document's text, or null when it could not be read.
     *
     * A `null` here is a readable file that is simply empty as much as it is an unreadable one, which is the
     * same failure from the user's side — the same message either way.
     */
    suspend fun read(context: Context, uri: Uri): String?
}

/**
 * The Storage Access Framework implementation.
 *
 * **The switch off the caller's thread is here** (B158): `suspend` alone buys nothing, since
 * `openOutputStream` and `write` execute inline on whatever dispatcher called, and every caller is a UI scope —
 * so choosing a document on a cloud provider blocked the UI thread on a network round trip, and a large history
 * blocked it for as long as the copy took.
 */
class SafDocumentIo @Inject constructor() : DocumentIo {

    override suspend fun write(context: Context, uri: Uri, text: String): Boolean = try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray())
                true
            } ?: false
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        false
    }

    override suspend fun read(context: Context, uri: Uri): String? = try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }
    } catch (cancellation: CancellationException) {
        // Not a failure, for the reason spelled out on [write].
        throw cancellation
    } catch (_: Throwable) {
        null
    }
}
