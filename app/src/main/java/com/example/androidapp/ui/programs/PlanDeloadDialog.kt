package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.TestTags

/**
 * The per-lift deload's confirm (ROADMAP N307).
 *
 * The numbers are the same [DeloadPreviewSet]s the write carries, and the shared programs are named only
 * when there are any (decision 5a). Nothing is written from this dialog itself; confirming asks the
 * ViewModel, which is what makes the plan edit and its undo one state transition.
 */
@Composable
internal fun DeloadLiftDialog(
    prompt: DeloadPrompt,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val appUnit = LocalWeightUnit.current
    val unit = prompt.weightUnit ?: appUnit
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag(TestTags.Programs.DELOAD_LIFT_DIALOG),
        title = { Text(stringResource(R.string.program_deload_lift_title)) },
        text = { DeloadPromptBody(prompt, unit) },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.program_deload_lift_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onCancel) {
                Text(stringResource(R.string.program_deload_lift_cancel))
            }
        },
    )
}

/** The confirm's numbers: the counts, each kept set's before → after, the removed ones, and sharing. */
@Composable
private fun DeloadPromptBody(prompt: DeloadPrompt, unit: WeightUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(prompt.exerciseName, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                R.string.program_deload_lift_sets,
                prompt.kept.size + prompt.removedCount,
                prompt.kept.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        prompt.kept.forEach { line ->
            Text(
                text = "${deloadLabel(line.before, unit)} → ${deloadAfterLabel(line, unit)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (prompt.removedCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.program_deload_lift_removed,
                    prompt.removedCount,
                    prompt.removedCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (prompt.sharedProgramNames.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.program_deload_lift_shared,
                    prompt.sharedProgramNames.joinToString(", "),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(R.string.program_deload_lift_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** One planned set's reps/hold and load, as the confirm reads it. */
private fun deloadLabel(set: TemplateSet, unit: WeightUnit): String {
    val load = when {
        set.targetAssistanceGrams != null && set.targetAssistanceGrams > 0L ->
            Weight.display(0L, set.targetAssistanceGrams, unit)
        set.targetWeightGrams != null -> Weight.display(set.targetWeightGrams, 0L, unit)
        else -> null
    }
    val reps = set.targetDurationSeconds?.let { "$it s" }
        ?: (set.targetRepsCurrent ?: set.targetRepsMin ?: set.targetRepsMax)?.toString()
    return listOfNotNull(reps, load).joinToString(" × ").ifEmpty { "—" }
}

private fun deloadAfterLabel(line: DeloadPreviewSet, unit: WeightUnit): String {
    val before = line.before
    val after = line.after
    return deloadLabel(
        before.copy(
            targetWeightGrams = after.weightGrams,
            targetAssistanceGrams = after.assistanceGrams,
            targetRepsCurrent = after.repsCurrent ?: before.targetRepsCurrent,
        ),
        unit,
    )
}
