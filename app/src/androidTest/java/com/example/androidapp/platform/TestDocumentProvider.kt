package com.example.androidapp.platform

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * A content URI over one test cache directory (B281/B389).
 *
 * This lives in the instrumented APK, so neither a release nor a debug application carries it. It is
 * deliberately open: the target app opens the URI through this provider, and the provider is the test process
 * itself, so no cross-package URI grant is needed. The path is exactly one cache subdirectory.
 */
class TestDocumentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val name = uri.lastPathSegment
            ?.takeIf { it.isNotBlank() && File(it).name == it }
            ?: throw FileNotFoundException("no file in $uri")
        val directory = File(requireNotNull(context).cacheDir, "document-io-test").apply { mkdirs() }
        return ParcelFileDescriptor.open(File(directory, name), ParcelFileDescriptor.parseMode(mode))
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = "application/json"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
