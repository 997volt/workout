package com.example.androidapp.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.ui.components.TestTags

/** The most sets one quick entry writes, and the most reps it will prescribe. */
internal const val MAX_QUICK_SETS = 20
private const val MAX_QUICK_REPS = 100

/** The load field is a little wider than the two numbers beside it. */
private const val LOAD_FIELD_WEIGHT = 1.2f

/**
 * The plan's quick entry (P3.18): a count, reps and a load in one row, written as that many sets.
 *
 * A five-set block is nearly always one shape repeated, so the shape gets one row and the per-set dialog
 * stays for the top set and its back-offs. The values start from the last planned set — the common case is
 * *three more like that one* — and the button is disabled rather than writing a half-parsed target.
 *
 * Extracted into its own file at the point the editor reached the functions this project allows per file.
 */
@Composable
internal fun QuickAddSets(
    exerciseId: String,
    lastSet: TemplateSet?,
    unit: WeightUnit,
    onAdd: (Int, TemplateSetEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var setsText by rememberSaveable(exerciseId, lastSet?.id) { mutableStateOf("3") }
    var repsText by rememberSaveable(exerciseId, lastSet?.id) {
        mutableStateOf(lastSet?.targetRepsMin?.toString().orEmpty())
    }
    var loadText by rememberSaveable(exerciseId, lastSet?.id) {
        mutableStateOf(lastSet?.targetWeightGrams?.let { Weight.format(it, unit) }.orEmpty())
    }
    val entry = quickEntry(setsText, repsText, loadText, unit)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickField(
            value = setsText,
            onValueChange = { setsText = it },
            label = stringResource(R.string.template_quick_sets),
            tag = TestTags.templateQuickSets(exerciseId),
            modifier = Modifier.weight(1f),
        )
        QuickField(
            value = repsText,
            onValueChange = { repsText = it },
            label = stringResource(R.string.template_quick_reps),
            tag = TestTags.templateQuickReps(exerciseId),
            modifier = Modifier.weight(1f),
        )
        QuickField(
            value = loadText,
            onValueChange = { loadText = it },
            label = stringResource(R.string.template_quick_load),
            tag = TestTags.templateQuickLoad(exerciseId),
            modifier = Modifier.weight(LOAD_FIELD_WEIGHT),
        )
        FilledTonalButton(
            onClick = { entry?.let { onAdd(it.first, it.second) } },
            enabled = entry != null,
            modifier = Modifier.testTag(TestTags.templateQuickAdd(exerciseId)),
        ) {
            Text(stringResource(R.string.template_quick_add))
        }
    }
}

/**
 * The row's values as a write, or null while any of them is unusable.
 *
 * A blank load is valid — a plan may prescribe reps without a weight — but a typo is not, so the parsed
 * text and the parse have to agree before the button exists.
 */
private fun quickEntry(
    setsText: String,
    repsText: String,
    loadText: String,
    unit: WeightUnit,
): Pair<Int, TemplateSetEdit>? {
    val count = setsText.trim().toIntOrNull()?.takeIf { it in 1..MAX_QUICK_SETS }
    val reps = repsText.trim().toIntOrNull()?.takeIf { it in 1..MAX_QUICK_REPS }
    val blankLoad = loadText.trim().isEmpty()
    val load = if (blankLoad) null else Weight.parse(loadText, unit)
    val usable = count != null && reps != null && (blankLoad || load != null)
    return if (usable) {
        count to TemplateSetEdit(
            role = SetType.NORMAL,
            targetWeightGrams = load,
            targetRepsMin = reps,
            targetRepsMax = reps,
        )
    } else {
        null
    }
}

@Composable
private fun QuickField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.testTag(tag),
    )
}
