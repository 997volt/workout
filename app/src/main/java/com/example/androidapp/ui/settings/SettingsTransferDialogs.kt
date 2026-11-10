package com.example.androidapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.transfer.DataTransferViewModel
import com.example.androidapp.ui.transfer.ImportPreviewDialog
import com.example.androidapp.ui.transfer.PasswordPromptDialog

/**
 * The encrypted export's password question, as a trigger the data row can call (ROADMAP N294).
 *
 * The dialog owns its open state so the settings route does not have to; the returned lambda is what the
 * row's callback runs, and the export starts only after a password was chosen.
 */
@Composable
internal fun rememberEncryptedExport(onExport: (String) -> Unit): () -> Unit {
    var show by remember { mutableStateOf(false) }
    if (show) {
        PasswordPromptDialog(
            title = stringResource(R.string.transfer_password_export_title),
            description = stringResource(R.string.transfer_password_export_hint),
            confirmLabel = stringResource(R.string.transfer_password_export_confirm),
            confirmWithSecondField = true,
            onConfirm = { password ->
                show = false
                onExport(password)
            },
            onDismiss = { show = false },
        )
    }
    return { show = true }
}

/**
 * The transfer questions the settings screen can have open (ROADMAP N246, N294).
 *
 * One composable because they are one shape — a sheet over the same screen, at most one at a time — and
 * because the route around them had grown past the length this project allows.
 */
@Composable
internal fun SettingsTransferDialogs(
    pendingImport: DataTransferViewModel.PendingImport?,
    pendingEncryptedImport: String?,
    encryptedImportError: DataError?,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onSubmitEncryptedPassword: (String) -> Unit,
    onCancelEncryptedImport: () -> Unit,
) {
    pendingEncryptedImport?.let {
        PasswordPromptDialog(
            title = stringResource(R.string.transfer_password_import_title),
            description = stringResource(R.string.transfer_password_import_hint),
            confirmLabel = stringResource(R.string.transfer_password_import_confirm),
            confirmWithSecondField = false,
            error = encryptedImportError?.let { dataErrorMessage(it) },
            onConfirm = onSubmitEncryptedPassword,
            onDismiss = onCancelEncryptedImport,
        )
    }
    pendingImport?.let { pending ->
        ImportPreviewDialog(
            preview = pending.preview,
            onConfirm = onConfirmImport,
            onDismiss = onCancelImport,
        )
    }
}
