package com.example.androidapp.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.data.transfer.EncryptedBackup
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/** The dialog's two fields and its one message, split out so the dialog stays a request and two exits. */
@Composable
private fun PasswordFields(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    showConfirmation: Boolean,
    error: String?,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(description)
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.transfer_password_field)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.testTag(TestTags.PASSWORD_FIELD),
        )
        if (showConfirmation) {
            OutlinedTextField(
                value = confirmation,
                onValueChange = onConfirmationChange,
                label = { Text(stringResource(R.string.transfer_password_confirm_field)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.testTag(TestTags.PASSWORD_CONFIRM_FIELD),
            )
        }
        error?.let { message ->
            Text(
                text = message,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * A password for an encrypted backup (ROADMAP N294, B413).
 *
 * **Controlled**: the text, the confirmation and the refusal are the caller's state, held by the
 * transfer ViewModel so a rotation cannot empty them. The dialog's only job is to draw them and report
 * what was typed; the validation that used to live here is the ViewModel's, because a check that lives
 * in `remember` is a check a rebuilt dialog has forgotten.
 */
@Composable
fun PasswordPromptDialog(
    title: String,
    description: String,
    confirmLabel: String,
    confirmWithSecondField: Boolean,
    password: String,
    confirmation: String,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    issue: PasswordIssue?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    /** True while the answer is being used (a key derived), so neither button starts it twice (B401). */
    busy: Boolean = false,
) {
    val issueText = when (issue) {
        PasswordIssue.TOO_SHORT -> pluralStringResource(
            R.plurals.transfer_password_too_short,
            EncryptedBackup.MIN_PASSWORD_LENGTH,
            EncryptedBackup.MIN_PASSWORD_LENGTH,
        )

        PasswordIssue.MISMATCH -> stringResource(R.string.transfer_password_mismatch)
        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.PASSWORD_DIALOG),
        title = { Text(title) },
        text = {
            PasswordFields(
                password = password,
                onPasswordChange = onPasswordChange,
                confirmation = confirmation,
                onConfirmationChange = onConfirmationChange,
                showConfirmation = confirmWithSecondField,
                error = issueText ?: error,
                description = description,
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                enabled = !busy,
                modifier = Modifier.testTag(TestTags.PASSWORD_CONFIRM),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.testTag(TestTags.PASSWORD_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
