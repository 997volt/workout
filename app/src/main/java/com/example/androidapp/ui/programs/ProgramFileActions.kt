package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.ProgramImportSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The two directions a program document travels, wired to the Storage Access Framework (ROADMAP N47).
 *
 * SAF for the backup's reason (P1.12): the user picks where the file goes, and the app declares
 * **no permissions at all**. The file IO lives here, in the composable, because it needs a `Context`
 * and a user-chosen `Uri`; the ViewModels only ever see strings.
 */

/** Launches a create-document picker and writes the program the editor has open. */
@Composable
fun rememberProgramExport(
    viewModel: ProgramEditorViewModel,
    onMessage: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exported = stringResource(R.string.program_exported)
    val writeFailed = stringResource(R.string.program_export_failed)
    val storageError = stringResource(R.string.transfer_storage_error)

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // user backed out
        scope.launch {
            when (val result = viewModel.exportDocument()) {
                is DataResult.Success -> {
                    onMessage(if (writeText(context, uri, result.data)) exported else writeFailed)
                }

                is DataResult.Failure -> onMessage(programMessageFor(result.error, storageError))
            }
        }
    }

    return remember(createDocument) { { createDocument.launch(DEFAULT_PROGRAM_FILE_NAME) } }
}

/** Launches an open-document picker and merges the program it holds. */
@Composable
fun rememberProgramImport(
    viewModel: ProgramsViewModel,
    onMessage: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The plural needs the count, so it is resolved as a lambda during composition rather than
    // inside the coroutine — the same shape the backup's import message uses.
    val resources = LocalResources.current
    val readFailed = stringResource(R.string.transfer_read_failed)
    val storageError = stringResource(R.string.transfer_storage_error)
    val loaded = stringResource(R.string.program_loaded)
    val alreadyLoaded = stringResource(R.string.program_already_loaded)

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = readText(context, uri)
            if (text == null) {
                onMessage(readFailed)
                return@launch
            }
            when (val result = viewModel.importDocument(text)) {
                is DataResult.Success -> onMessage(
                    importSentence(result.data, loaded, alreadyLoaded) { count ->
                        resources.getQuantityString(R.plurals.program_load_dropped, count, count)
                    },
                )

                is DataResult.Failure -> onMessage(programMessageFor(result.error, storageError))
            }
        }
    }

    return remember(openDocument) {
        { openDocument.launch(arrayOf(JSON_MIME_TYPE, "text/plain")) }
    }
}

/**
 * What a load did, in one sentence.
 *
 * "Already here" is the idempotent case rather than a failure: a document's rows are never written
 * twice, so loading the same file again legitimately adds nothing. It is asked of the whole summary
 * rather than of the program count (B57), because a load can add templates for a program whose id is
 * already present. A dropped movement is appended because it is the one thing a successful load can
 * be missing. [dropped] resolves the plural, which needs the count and so cannot be read during
 * composition.
 */
private fun importSentence(
    summary: ProgramImportSummary,
    loaded: String,
    alreadyLoaded: String,
    dropped: (Int) -> String,
): String {
    val droppedText = if (summary.droppedMovements > 0) dropped(summary.droppedMovements) else null
    val first = if (summary.addedNothing) alreadyLoaded else loaded
    return listOfNotNull(first, droppedText).joinToString(" ")
}

private fun programMessageFor(error: DataError, storageError: String): String = when (error) {
    // Already written for the user by the layer that rejected the file.
    is DataError.Invalid -> error.message
    DataError.NotFound -> storageError
    is DataError.Storage -> storageError
}

private suspend fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray())
        true
    } ?: false
} catch (cancellation: CancellationException) {
    // A cancelled write means "this screen is gone", not "the file could not be written" —
    // reporting the latter would be a lie, and the coroutine would refuse to finish cancelling.
    throw cancellation
} catch (_: Throwable) {
    false
}

private suspend fun readText(context: Context, uri: Uri): String? = try {
    // Null here is a readable file that is simply empty, which is a failed read rather than a
    // failure — the same message either way.
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Throwable) {
    null
}

private const val JSON_MIME_TYPE = "application/json"
private const val DEFAULT_PROGRAM_FILE_NAME = "workout-program.json"
