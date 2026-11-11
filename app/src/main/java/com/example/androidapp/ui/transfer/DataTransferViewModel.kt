package com.example.androidapp.ui.transfer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.transfer.EncryptedBackup
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.ImportSummary
import com.example.androidapp.platform.DocumentIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What an import did, or why it could not be done. */
sealed interface ImportOutcome {
    data class Imported(val summary: ImportSummary) : ImportOutcome
    data class Failed(val error: DataError) : ImportOutcome
}

/**
 * What a document transfer ended as, as **resources rather than sentences**.
 *
 * The ViewModel decides *what happened*; the screen decides how to say it, because only the screen has the
 * resources — and a plural needs the count, which is why one case carries [count]. Another file carries no
 * `Context` this way, which is what keeps the ViewModel JVM-testable.
 */
sealed interface TransferOutcome {
    /** The file was written, or read and merged: the sentence is a plain string. */
    data object Exported : TransferOutcome

    /** Nothing was written, or nothing was read: the storage failure's own sentence. */
    data object StorageFailure : TransferOutcome

    /** The file was read and refused, or the export could not be produced: the layer's own message. */
    data class Refused(val error: DataError) : TransferOutcome

    /** The merge's own sentence, which needs the summary rather than a count. */
    data class Merged(val summary: ImportSummary) : TransferOutcome
}

/**
 * Backup and restore for the library screen's data menu (ROADMAP P1.12).
 *
 * **The transfer runs in [viewModelScope], and reports through [outcome]** (B159). It used to run in the
 * composable's `rememberCoroutineScope`, which a configuration change disposes: the Storage Access Framework
 * document had already been created by the picker, so a rotation mid-export left a 0-byte `workout-backup.json`
 * behind and said nothing at all — the coroutine was cancelled before the write and before the message, and the
 * lifter found out only when a later import called the file unreadable. A scope that outlives the screen fixes
 * both halves, and it is why the file IO arrives as [documentIo] with the caller's `Context` rather than being
 * held here.
 */
@HiltViewModel
class DataTransferViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val documentIo: DocumentIo,
) : ViewModel() {

    /**
     * Where PBKDF2 and AES run.
     *
     * A mutable seam rather than a constructor dependency: Hilt has no binding for a dispatcher, and a
     * JVM test wants the work on its own scheduler. Production keeps the default.
     */
    internal var cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _outcome = MutableStateFlow<TransferOutcome?>(null)

    /** The last transfer's result, or null while there is nothing to say. Cleared by [onOutcomeShown]. */
    val outcome: StateFlow<TransferOutcome?> = _outcome.asStateFlow()

    /** The JSON to write, or null if it could not be produced. */
    suspend fun export(): ExportOutcome = when (val result = backupRepository.export()) {
        is DataResult.Success -> ExportOutcome.Ready(result.data)
        is DataResult.Failure -> ExportOutcome.Failed(result.error)
    }

    /**
     * Deletes everything the user made (ROADMAP N18).
     *
     * Irreversible, which is why the screen asks for a typed confirmation before calling
     * it: "Delete everything" is a button someone presses exactly once by accident.
     */
    suspend fun clearEverything(): ClearOutcome =
        when (val result = backupRepository.clearAllUserData()) {
            is DataResult.Success -> ClearOutcome.Cleared
            is DataResult.Failure -> ClearOutcome.Failed(result.error)
        }

    /** Merges an imported file. Additive, so it can never cost the user data. */
    suspend fun import(text: String): ImportOutcome = when (val result = backupRepository.import(text)) {
        is DataResult.Success -> ImportOutcome.Imported(result.data)
        is DataResult.Failure -> ImportOutcome.Failed(result.error)
    }

    /** The file waiting for the lifter's confirmation, with what the preview read (ROADMAP N246). */
    data class PendingImport(val text: String, val preview: BackupPreview)

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)

    /** The file the picker returned, held until the sheet is answered. */
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _pendingEncryptedImport = MutableStateFlow<String?>(null)

    /** The encrypted file the picker returned, held while its password is asked for (N294). */
    val pendingEncryptedImport: StateFlow<String?> = _pendingEncryptedImport.asStateFlow()

    private val _encryptedImportError = MutableStateFlow<DataError?>(null)

    /** Why the last password did not open the pending file; the dialog stays open on it. */
    val encryptedImportError: StateFlow<DataError?> = _encryptedImportError.asStateFlow()

    private val _decrypting = MutableStateFlow(false)

    /** True while the password is being turned into a key, so the dialog can refuse a second tap (B401). */
    val decrypting: StateFlow<Boolean> = _decrypting.asStateFlow()

    /** Merges the file that was previewed, after the lifter confirmed what it holds. */
    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        if (pending.preview.problems.isNotEmpty()) {
            // The dialog disables this action, but a direct caller must not bypass the same refusal (B342).
            _outcome.value = TransferOutcome.Refused(
                DataError.Invalid("That backup has values the app cannot store."),
            )
            return
        }
        viewModelScope.launch {
            _outcome.value = when (val merged = import(pending.text)) {
                is ImportOutcome.Imported -> TransferOutcome.Merged(merged.summary)
                is ImportOutcome.Failed -> TransferOutcome.Refused(merged.error)
            }
        }
    }

    /** The picker's answer was no: forget the file without reading further. */
    fun cancelImport() {
        _pendingImport.value = null
    }

    /** Writes the backup to the document the picker returned, and reports what happened. */
    fun writeBackup(context: Context, uri: Uri, password: String? = null) {
        viewModelScope.launch {
            val produced = when (val exported = export()) {
                is ExportOutcome.Ready -> exported
                is ExportOutcome.Failed -> {
                    _outcome.value = TransferOutcome.Refused(exported.error)
                    return@launch
                }
            }
            val text = if (password == null) {
                produced.json
            } else {
                when (
                    val encrypted = withContext(cryptoDispatcher) {
                        EncryptedBackup.encrypt(produced.json, password)
                    }
                ) {
                    is DataResult.Success -> encrypted.data
                    is DataResult.Failure -> {
                        _outcome.value = TransferOutcome.Refused(encrypted.error)
                        return@launch
                    }
                }
            }
            _outcome.value = if (documentIo.write(context, uri, text)) {
                TransferOutcome.Exported
            } else {
                TransferOutcome.StorageFailure
            }
        }
    }

    /**
     * Reads the document the picker returned and **asks** before merging it (ROADMAP N246).
     *
     * The read is the risky half — the file is someone else's, and the merge is the app's biggest write — so
     * the counts come first. A file that cannot be read or decoded is still reported at once; only a file with
     * something to merge becomes a question.
     */
    fun readBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val text = documentIo.read(context, uri)
            if (text == null) {
                _outcome.value = TransferOutcome.StorageFailure
                return@launch
            }
            if (EncryptedBackup.isEncrypted(text)) {
                _encryptedImportError.value = null
                _pendingEncryptedImport.value = text
                return@launch
            }
            preview(text)
        }
    }

    /**
     * Opens the pending encrypted file with [password], or keeps it for another try (N294).
     *
     * One derivation at a time (B401): the dialog's buttons are disabled while it runs, and a second call
     * that arrives anyway is dropped rather than stacking a second PBKDF2 pass. The answer is only used
     * while the same file is still pending — a question dismissed during the derivation must not open the
     * import preview behind the lifter's back.
     */
    fun submitEncryptedImport(password: String) {
        if (_decrypting.value) return
        val text = _pendingEncryptedImport.value ?: return
        _decrypting.value = true
        viewModelScope.launch {
            val opened = withContext(cryptoDispatcher) {
                EncryptedBackup.decrypt(text, password)
            }
            _decrypting.value = false
            if (_pendingEncryptedImport.value != text) return@launch
            when (opened) {
                is DataResult.Success -> {
                    _encryptedImportError.value = null
                    _pendingEncryptedImport.value = null
                    preview(opened.data)
                }

                is DataResult.Failure -> _encryptedImportError.value = opened.error
            }
        }
    }

    /** Backs out of the password question and forgets the file. */
    fun cancelEncryptedImport() {
        _pendingEncryptedImport.value = null
        _encryptedImportError.value = null
    }

    private suspend fun preview(text: String) {
        when (val preview = backupRepository.preview(text)) {
            is DataResult.Success -> _pendingImport.value = PendingImport(text, preview.data)
            is DataResult.Failure -> _outcome.value = TransferOutcome.Refused(preview.error)
        }
    }

    /** The outcome has been put in front of the lifter, so it is not shown twice. */
    fun onOutcomeShown() {
        _outcome.value = null
    }
}

/** The backup document, or the reason there isn't one. */
sealed interface ExportOutcome {
    data class Ready(val json: String) : ExportOutcome
    data class Failed(val error: DataError) : ExportOutcome
}

/** What came of the one irreversible action. */
sealed interface ClearOutcome {
    data object Cleared : ClearOutcome

    data class Failed(val error: DataError) : ClearOutcome
}
