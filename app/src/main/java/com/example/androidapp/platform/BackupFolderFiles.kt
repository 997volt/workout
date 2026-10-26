package com.example.androidapp.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The folder operations an automatic backup needs (ROADMAP N218).
 *
 * An interface rather than a class with a `Context`, for the reason [DocumentIo] states: the caller has the
 * `Context`, and a folder that can be listed and pruned has to be replaceable in a test without a provider.
 * Everything below the interface is a tree URI and a `ContentResolver`; everything above it is "write this
 * file, tell me what is there, delete that one".
 */
interface BackupFolderFiles {

    /**
     * Writes [text] as [fileName] in the tree, answering the name it landed under, or null on failure.
     *
     * The answer is a name rather than a boolean because a name may already be taken (B248): the folder is
     * shared, and reopening an existing document would truncate the copy that is already there. The
     * implementation picks a free name beside it and says which one it used.
     */
    suspend fun write(treeUri: String, fileName: String, text: String): String?

    /** Every display name in the tree, in provider order. */
    suspend fun names(treeUri: String): List<String>

    /** Deletes one file by display name, answering whether it went. */
    suspend fun delete(treeUri: String, fileName: String): Boolean
}

/**
 * The Storage Access Framework implementation (ROADMAP N218).
 *
 * A tree the user picked and granted **persistable** access to, so no permission is declared and the app can
 * write into it while closed — which is the whole reason the folder is a setting rather than an export prompt.
 * `createDocument` makes the file when it does not exist and throws when it does, so an existing name is
 * opened instead: two backups within one second are the same file, not a failure.
 */
class SafBackupFolderFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupFolderFiles {

    override suspend fun write(treeUri: String, fileName: String, text: String): String? = try {
        withContext(Dispatchers.IO) {
            val tree = treeUri.toUri()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
            // **Never reopen a backup** (B248). An existing document is not the one to write: opening it
            // would truncate the copy already in the folder, and two writers would interleave. The name is
            // taken, so the copy lands beside it.
            val name = freeName(children, fileName)
            val document = DocumentsContract.createDocument(
                context.contentResolver,
                children,
                JSON_MIME_TYPE,
                name,
            )
            val landed = document?.let { uri ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(text.toByteArray())
                        true
                    } ?: false
                } catch (_: Throwable) {
                    // A half-written file must not be left looking like a backup. Deleting it is the only
                    // cleanup this port can do, and failing to delete is the failure it already reports.
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    false
                }
            } ?: false
            if (landed) name else null
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        // A provider that went away, a folder the user deleted, a revoked grant: all one answer here, and the
        // settings row says which, rather than three failure types nobody can act on differently.
        null
    }

    /**
     * A name in [children] that nothing holds: [fileName], or the same name with `-2`, `-3` … appended.
     *
     * A second-resolution stamp made two writes in one second the same file; the same-instant case is still
     * possible across devices and clock changes, so the folder is asked rather than assumed (B248).
     */
    private fun freeName(children: Uri, fileName: String): String {
        val taken = namesIn(children).toSet()
        if (fileName !in taken) return fileName
        val base = fileName.removeSuffix(JSON_SUFFIX)
        var attempt = 2
        while ("$base-$attempt$JSON_SUFFIX" in taken) attempt++
        return "$base-$attempt$JSON_SUFFIX"
    }

    /** Every display name in one children collection. */
    private fun namesIn(children: Uri): List<String> =
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { add(it) }
                }
            }
        }.orEmpty()

    override suspend fun names(treeUri: String): List<String> = try {
        withContext(Dispatchers.IO) {
            val tree = treeUri.toUri()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
            context.contentResolver
                .query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            cursor.getString(0)?.let { add(it) }
                        }
                    }
                }
                .orEmpty()
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        emptyList()
    }

    override suspend fun delete(treeUri: String, fileName: String): Boolean = try {
        withContext(Dispatchers.IO) {
            val tree = treeUri.toUri()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
            documentNamed(children, fileName)?.let { uri ->
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } ?: false
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        false
    }

    /**
     * The document called [fileName] in [children], or null.
     *
     * The id is needed as well as the name: a tree document is addressed by id, and the display name is only
     * how it is shown — the two are separate columns for exactly this reason.
     */
    private fun documentNamed(children: Uri, fileName: String): Uri? =
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == fileName) {
                    val id = cursor.getString(0) ?: return@use null
                    return@use DocumentsContract.buildDocumentUriUsingTree(children, id)
                }
            }
            null
        }

    private companion object {
        const val JSON_MIME_TYPE = "application/json"
        const val JSON_SUFFIX = ".json"
    }
}
