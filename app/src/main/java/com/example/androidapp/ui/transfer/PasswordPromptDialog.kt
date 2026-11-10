package com.example.androidapp.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * A password for an encrypted backup (ROADMAP N294).
 *
 * The password is never stored or remembered; the dialog hands it to the caller and forgets it. An export
 * asks twice because a typo in a password nobody stored makes the file unreadable; an import asks once and
 * keeps the caller's error (a wrong password) visible until the next try.
 */
@Composable
fun PasswordPromptDialog(
    title: String,
    description: String,
    confirmLabel: String,
    confirmWithSecondField: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val tooShort = pluralStringResource(
        R.plurals.transfer_password_too_short,
        EncryptedBackup.MIN_PASSWORD_LENGTH,
        EncryptedBackup.MIN_PASSWORD_LENGTH,
    )
    val mismatch = stringResource(R.string.transfer_password_mismatch)
    val shownError = localError ?: error

    fun submit() {
        localError = when {
            password.length < EncryptedBackup.MIN_PASSWORD_LENGTH -> tooShort
            confirmWithSecondField && password != confirmation -> mismatch
            else -> null
        }
        if (localError == null) onConfirm(password)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.PASSWORD_DIALOG),
        title = { Text(title) },
        text = {
            PasswordFields(
                password = password,
                onPasswordChange = {
                    password = it
                    localError = null
                },
                confirmation = confirmation,
                onConfirmationChange = {
                    confirmation = it
                    localError = null
                },
                showConfirmation = confirmWithSecondField,
                error = shownError,
                description = description,
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { submit() },
                modifier = Modifier.testTag(TestTags.PASSWORD_CONFIRM),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.PASSWORD_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
