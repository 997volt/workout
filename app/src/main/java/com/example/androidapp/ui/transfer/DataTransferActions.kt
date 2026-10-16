package com.example.androidapp.ui.transfer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError

/** The two actions the data menu offers, ready to invoke from `onClick`. */
class DataTransferActions(
    val export: () -> Unit,
    val import: () -> Unit,
)

/**
 * Wires the data menu to the Storage Access Framework (ROADMAP P1.12).
 *
 * SAF rather than a fixed path or `WRITE_EXTERNAL_STORAGE`: the user picks where
 * the file goes, and the app declares **no permissions at all** (ROADMAP N26) — so there
 * is no storage permission to ask for and nothing else to ask for either. It also means the backup can live
 * somewhere that survives uninstalling the app, which is the entire point.
 *
 * **The transfer itself is the ViewModel's, and this only starts it and reads the answer** (B159). The report
 * travels back through [DataTransferViewModel.outcome] rather than through a callback captured here, for the
 * same reason the work moved: a configuration change disposes this composition, and a callback into it would
 * then either be dropped or reach a screen that no longer exists.
 *
 * The file IO lives in `DocumentIo` (ROADMAP B53, B158); what is here is the picker, which needs a
 * `Context` and a user-chosen `Uri`.
 */
@Composable
fun rememberDataTransferActions(
    viewModel: DataTransferViewModel,
    onMessage: (String) -> Unit,
): DataTransferActions {
    val context = LocalContext.current
    val resources = LocalResources.current

    // Resolved during composition, so the outcome below can be said without a context of its own.
    val messages = rememberTransferMessages()

    // Which of the two sheets was answered, so a storage failure is reported in its own direction. Not
    // `rememberSaveable`: a transfer that survives a rotation does so in the ViewModel, and this is re-read only
    // by the outcome that transfer publishes.
    var transferIsAnExport by remember { mutableStateOf(true) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // user backed out
        transferIsAnExport = true
        viewModel.writeBackup(context, uri)
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        transferIsAnExport = false
        viewModel.readBackup(context, uri)
    }

    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    // The callback is read through `rememberUpdatedState`: this effect is keyed on the outcome, so a lambda
    // that changed under it would keep reporting into the composition that asked for the transfer.
    val currentOnMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(outcome) {
        val reported = outcome ?: return@LaunchedEffect
        // Said once and cleared: the outcome is a one-shot event, and `onOutcomeShown` is what stops a rotation
        // from repeating a message about a transfer that finished before it.
        //
        // The direction is remembered rather than carried on the outcome, so the ViewModel stays free of
        // resources: "couldn't write the backup file" and "couldn't read that file" are different sentences and
        // only the sheet that was open knows which was asked for.
        currentOnMessage(
            messages.sentence(
                outcome = reported,
                storageFailure = if (transferIsAnExport) messages.exportFailed else messages.readFailed,
                plural = resources::getQuantityString,
            ),
        )
        viewModel.onOutcomeShown()
    }

    return remember(createDocument, openDocument) {
        DataTransferActions(
            export = { createDocument.launch(DEFAULT_FILE_NAME) },
            import = { openDocument.launch(arrayOf(JSON_MIME_TYPE, "text/plain")) },
        )
    }
}

/** The strings the transfer outcome is reported with, resolved once during composition. */
class TransferMessages(
    val exported: String,
    val exportFailed: String,
    val readFailed: String,
    private val importedFormat: (Int) -> String,
    private val alreadyComplete: String,
    private val storageError: String,
) {
    /**
     * The sentence one outcome is said with.
     *
     * [storageFailure] is the caller's own half: "couldn't write the backup file" and "couldn't read that file"
     * are different sentences, and only the caller knows which direction it asked for.
     */
    fun sentence(
        outcome: TransferOutcome,
        storageFailure: String,
        plural: (Int, Int) -> String,
    ): String = when (outcome) {
        is TransferOutcome.Done -> if (outcome.count > 0) {
            plural(R.plurals.transfer_imported, outcome.count)
        } else {
            exported
        }

        TransferOutcome.StorageFailure -> storageFailure
        is TransferOutcome.Merged -> if (outcome.summary.wasAlreadyComplete) {
            alreadyComplete
        } else {
            importedFormat(outcome.summary.total)
        }

        is TransferOutcome.Refused -> forError(outcome.error)
    }

    fun forError(error: DataError): String = documentErrorMessage(error, storageError)
}

@Composable
private fun rememberTransferMessages(): TransferMessages {
    // LocalResources rather than LocalContext.current.resources: the latter is not
    // invalidated by a Configuration change, so a locale switch would leave these
    // strings stale. The plural also needs the count, which is why this is a lambda
    // rather than a `stringResource` call.
    val resources = LocalResources.current

    val exported = stringResource(R.string.transfer_exported)
    val exportFailed = stringResource(R.string.transfer_export_failed)
    val readFailed = stringResource(R.string.transfer_read_failed)
    val alreadyComplete = stringResource(R.string.transfer_already_complete)
    val storageError = stringResource(R.string.transfer_storage_error)

    return remember(exported, exportFailed, readFailed, alreadyComplete, storageError) {
        TransferMessages(
            exported = exported,
            exportFailed = exportFailed,
            readFailed = readFailed,
            importedFormat = { count ->
                resources.getQuantityString(R.plurals.transfer_imported, count, count)
            },
            alreadyComplete = alreadyComplete,
            storageError = storageError,
        )
    }
}

private const val DEFAULT_FILE_NAME = "workout-backup.json"

/** What both document pickers accept: this app's own JSON, and the `text/plain` some providers report it as. */
internal const val JSON_MIME_TYPE = "application/json"
