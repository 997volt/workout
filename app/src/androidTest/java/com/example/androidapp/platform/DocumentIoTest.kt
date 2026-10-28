package com.example.androidapp.platform

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The document seam, against a real `ContentResolver` (ROADMAP B158, B159).
 *
 * Why this exists rather than a JVM test: [DocumentIo] is the one piece of the transfer that is *about* Android —
 * `openOutputStream`, `openInputStream` and the dispatch they run on — and the coroutine that had to move out of
 * the composition's scope (B159) is only safe if this half behaves. A `FileProvider` URI is a content URI the app
 * has write access to, which is exactly the shape the Storage Access Framework hands over.
 *
 * The instrumented job builds the **debug** variant, so this is not a check that R8 kept anything: the
 * minified build is exercised by the signed release smoke test in RELEASING.md.
 */
@RunWith(AndroidJUnit4::class)
class DocumentIoTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val io = DocumentIo()

    /** A `content://` URI for one file in this app's own cache, through the provider the manifest declares. */
    private fun uriFor(name: String): Uri {
        val directory = File(context.cacheDir, "document-io-test").apply { mkdirs() }
        val file = File(directory, name)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    @Test
    fun aWrittenDocument_readsBackByteForByte() = runTest {
        val uri = uriFor("round-trip.json")
        val text = """{"schemaVersion":6,"notes":"a diacritic: é, a quote: \", a newline: \n"}"""

        assertTrue("the write must report success", io.write(context, uri, text))
        assertEquals("and the same bytes must come back", text, io.read(context, uri))
    }

    @Test
    fun anUnreadableDocument_answersNull_ratherThanThrowing() = runTest {
        // The failure a picker can hand over: a URI whose provider is gone, or a file that cannot be opened. Every
        // caller turns null into one sentence, so a throw here would be an unhandled crash on the import path.
        val missing = Uri.parse("content://com.example.androidapp.fileprovider/not/a/real/file.json")

        assertNull(io.read(context, missing))
    }

    @Test
    fun anUnwritableDocument_answersFalse_ratherThanThrowing() = runTest {
        val missing = Uri.parse("content://com.example.androidapp.fileprovider/not/a/real/file.json")

        assertFalse(io.write(context, missing, "anything"))
    }

    @Test
    fun anEmptyDocument_isReadable_andIsNotTheSameThingAsAnUnreadableOne() = runTest {
        // Both answer null to the *import* (the same sentence either way, as DocumentIo's KDoc says), but the
        // write has to be able to produce a readable empty document so a test can tell the two apart.
        val uri = uriFor("empty.json")

        assertTrue(io.write(context, uri, ""))
        assertEquals("", io.read(context, uri))
    }
}
