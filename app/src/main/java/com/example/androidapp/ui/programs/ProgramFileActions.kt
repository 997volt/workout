package com.example.androidapp.ui.programs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.ui.transfer.JSON_MIME_TYPE
import com.example.androidapp.ui.transfer.documentErrorMessage
import com.example.androidapp.ui.transfer.readDocument
import com.example.androidapp.ui.transfer.writeDocument
import kotlinx.coroutines.launch

/**
 * The two directions a program document travels, wired to the Storage Access Framework (ROADMAP N47).
 *
 * SAF for the backup's reason (P1.12): the user picks where the file goes, and the app declares
 * **no permissions at all**. The file IO itself is [writeDocument] and [readDocument], shared with
 * the backup (ROADMAP B53); what is here is the picker, which needs a `Context` and a user-chosen
 * `Uri`, and the sentences a result is reported with.
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
                    onMessage(
                        if (writeDocument(context, uri, result.data)) exported else writeFailed,
                    )
                }

                is DataResult.Failure -> onMessage(
                    documentErrorMessage(result.error, storageError),
                )
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
            val text = readDocument(context, uri)
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

                is DataResult.Failure -> onMessage(
                    documentErrorMessage(result.error, storageError),
                )
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

private const val DEFAULT_PROGRAM_FILE_NAME = "workout-program.json"
