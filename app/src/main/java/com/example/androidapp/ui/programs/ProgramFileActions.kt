package com.example.androidapp.ui.programs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.ui.transfer.JSON_MIME_TYPE
import com.example.androidapp.ui.transfer.documentErrorMessage

/**
 * The two directions a program document travels, wired to the Storage Access Framework (ROADMAP N47).
 *
 * SAF for the backup's reason (P1.12): the user picks where the file goes, and the app declares
 * **no permissions at all**. The file IO itself is `DocumentIo` (ROADMAP B53, B158); what is here is the
 * picker, which needs a `Context` and a user-chosen `Uri`, and the sentences a result is reported with.
 *
 * **The transfer is the ViewModel's** (B159), started here and reported through a `StateFlow` the screen
 * collects. It used to run in `rememberCoroutineScope`, which a configuration change disposes after the
 * picker has already created the document — leaving an empty file and no message at all.
 */

/** Launches a create-document picker and writes the program the editor has open. */
@Composable
fun rememberProgramExport(
    viewModel: ProgramEditorViewModel,
    onMessage: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val exported = stringResource(R.string.program_exported)
    val writeFailed = stringResource(R.string.program_export_failed)
    val storageError = stringResource(R.string.transfer_storage_error)

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // user backed out
        viewModel.writeDocument(context, uri)
    }

    val outcome by viewModel.exported.collectAsStateWithLifecycle()
    // Read through `rememberUpdatedState`: the effect is keyed on the outcome, so a lambda that changed under
    // it would keep reporting into the composition that asked for the transfer.
    val currentOnMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(outcome) {
        when (val reported = outcome) {
            null -> Unit
            is DataResult.Success -> currentOnMessage(exported)
            is DataResult.Failure -> currentOnMessage(
                documentErrorMessage(reported.error, writeFailed).ifEmpty { storageError },
            )
        }
        // Said once and cleared, so a rotation cannot repeat a message about a write that already finished.
        if (outcome != null) viewModel.onExportShown()
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
    // The plural needs the count, so it is resolved as a lambda during composition rather than
    // inside the coroutine — the same shape the backup's import message uses.
    val resources = LocalResources.current
    val readFailed = stringResource(R.string.transfer_read_failed)
    val storageError = stringResource(R.string.transfer_storage_error)
    val loaded = stringResource(R.string.program_loaded)
    val updated = stringResource(R.string.program_updated)
    val alreadyLoaded = stringResource(R.string.program_already_loaded)
    val nameTaken = stringResource(R.string.program_name_taken)

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.readDocument(context, uri)
    }

    val outcome by viewModel.imported.collectAsStateWithLifecycle()
    val edited by viewModel.edited.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(outcome, edited) {
        when (val reported = outcome) {
            null -> Unit
            is DataResult.Success -> currentOnMessage(
                // An edit already had its diff confirmed, so its report is the outcome, not a count.
                if (edited) {
                    updated
                } else {
                    importSentence(reported.data, loaded, alreadyLoaded, nameTaken) { count ->
                        resources.getQuantityString(R.plurals.program_load_dropped, count, count)
                    }
                },
            )

            is DataResult.Failure -> currentOnMessage(
                // A storage failure here is a read that did not happen, which is its own sentence rather
                // than the export's.
                documentErrorMessage(reported.error, readFailed).ifEmpty { storageError },
            )
        }
        if (outcome != null) viewModel.onImportShown()
    }

    return remember(openDocument) {
        {
            openDocument.launch(arrayOf(JSON_MIME_TYPE, "text/plain"))
        }
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
 *
 * **A name taken by another plan is not the idempotent case** (B267). Both add nothing, so both used
 * to say "already here" — but the second is a document this device has never seen, and the lifter
 * needs to know the file is not the program they thought it was. It wins over "already here" because
 * it is the stronger statement, and the load genuinely added nothing in either case.
 */
internal fun importSentence(
    summary: ProgramImportSummary,
    loaded: String,
    alreadyLoaded: String,
    nameTaken: String,
    dropped: (Int) -> String,
): String {
    val droppedText = if (summary.droppedMovements > 0) dropped(summary.droppedMovements) else null
    val first = when {
        summary.nameTakenByAnotherPlan -> nameTaken
        summary.addedNothing -> alreadyLoaded
        else -> loaded
    }
    return listOfNotNull(first, droppedText).joinToString(" ")
}

private const val DEFAULT_PROGRAM_FILE_NAME = "workout-program.json"
