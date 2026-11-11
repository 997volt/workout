package com.example.androidapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.transfer.DataTransferViewModel
import com.example.androidapp.ui.transfer.ImportPreviewDialog
import com.example.androidapp.ui.transfer.PasswordDraft
import com.example.androidapp.ui.transfer.PasswordPromptDialog
import com.example.androidapp.ui.transfer.PasswordPromptKind

/**
 * The transfer questions the settings screen can have open (ROADMAP N246, N294).
 *
 * One composable because they are one shape — a sheet over the same screen, at most one at a time — and
 * because the route around them had grown past the length this project allows. The password question is
 * driven entirely by the draft the transfer ViewModel holds (B413), so its two forms are one dialog and a
 * rotation neither empties it nor closes it.
 */
@Composable
internal fun SettingsTransferDialogs(
    pendingImport: DataTransferViewModel.PendingImport?,
    passwordDraft: PasswordDraft?,
    encryptedImportError: DataError?,
    decrypting: Boolean,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onConfirmPassword: () -> Unit,
    onDismissPassword: () -> Unit,
) {
    passwordDraft?.let { draft ->
        val isExport = draft.kind == PasswordPromptKind.EXPORT
        PasswordPromptDialog(
            title = stringResource(
                if (isExport) R.string.transfer_password_export_title else R.string.transfer_password_import_title,
            ),
            description = stringResource(
                if (isExport) R.string.transfer_password_export_hint else R.string.transfer_password_import_hint,
            ),
            confirmLabel = stringResource(
                if (isExport) R.string.transfer_password_export_confirm else R.string.transfer_password_import_confirm,
            ),
            confirmWithSecondField = isExport,
            password = draft.password,
            confirmation = draft.confirmation,
            onPasswordChange = onPasswordChange,
            onConfirmationChange = onConfirmationChange,
            issue = draft.issue,
            // A refused import is the file's own answer and is said in the same dialog; an export has
            // nothing beyond what the fields state.
            error = if (isExport) null else encryptedImportError?.let { dataErrorMessage(it) },
            onConfirm = onConfirmPassword,
            onDismiss = onDismissPassword,
            busy = !isExport && decrypting,
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
