package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight

/**
 * Asks for the lifter's weight before a bodyweight set can be written (ROADMAP N219).
 *
 * Takes a nullable name rather than the pending value itself, so the screen decides *whether* a set is held
 * with one call and this component decides how to ask: a null draws nothing.
 *
 *
 * The body text names the exercise, because this can arrive from a tap on a block the lifter may have scrolled
 * past — "how much do you weigh" alone reads like a question about nothing.
 *
 * **Save is disabled until the field parses to a positive weight.** A zero would write a set whose load is
 * nothing, which is the outcome the prompt exists to prevent; dismissing it writes nothing at all and leaves
 * the set unlogged, which is the honest answer to "I would rather not say".
 */
@Composable
fun BodyweightPromptDialog(
    exerciseName: String?,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (exerciseName == null) return
    val unit = LocalWeightUnit.current
    var text by remember { mutableStateOf("") }
    val grams = Weight.parse(text.trim(), unit)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.bodyweight_prompt_title)) },
        text = {
            Column {
                Text(stringResource(R.string.bodyweight_prompt_body, exerciseName))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag(TestTags.BODYWEIGHT_FIELD),
                    singleLine = true,
                    label = { Text(stringResource(R.string.bodyweight_prompt_field)) },
                    suffix = { Text(unit.label()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { grams?.let(onSave) },
                enabled = grams != null && grams > 0L,
                modifier = Modifier.testTag(TestTags.BODYWEIGHT_SAVE),
            ) {
                Text(stringResource(R.string.bodyweight_prompt_save))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.BODYWEIGHT_DISMISS),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
