package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType

/**
 * The set editor's raw field text, shared by the two places a set is stated (ROADMAP N59).
 *
 * The correction dialog edits a set that exists, and the workout screen's inline fields state the
 * next one; both parse the same four values the same way, so the *text* and the rule that decides
 * whether it is usable live here rather than being written twice.
 */
internal data class SetEntryDraft(
    val repsText: String,
    val weightText: String,
    /**
     * The RPE as text — the halves [values] parses back out of it.
     *
     * It is no longer blank: the field is a stepper now (ROADMAP N59), so it always holds a number
     * and the stepper only ever writes text [Rpe.parse] reads as the halves it stepped to.
     */
    val rpeText: String,
    /** Only the dialog edits the comment; the inline fields leave it empty. */
    val noteText: String = "",
    val setType: SetType = SetType.NORMAL,
)

/**
 * The RPE a set starts at when no plan names one (ROADMAP N59).
 *
 * 9.0. A stepper always shows a number, so this is what a logged set records when the plan asks for
 * nothing and the lifter never touches it — the same direction the muscle-feel stepper took (N8).
 * It is not the middle of the scale but the working end of it, where a hard set that was not taken
 * to failure lands.
 */
internal const val DEFAULT_RPE_HALVES = 18

/** What a [SetEntryDraft]'s text says, and whether each field is a usable value. */
internal data class SetEntryValues(
    val reps: Int?,
    val load: Load?,
    val rpeHalves: Int?,
    val rpeValid: Boolean,
) {
    /** A set needs reps and a load; the comment stays optional, and the RPE is always stated (N6, N59). */
    val isComplete: Boolean get() = reps != null && load != null && rpeValid
}

internal fun SetEntryDraft.values(): SetEntryValues {
    // A set of zero reps is not a set, which is why this floor is 1.
    val reps = repsText.toIntOrNull()?.takeIf { it > 0 }
    // One field, two columns: a leading minus is assistance (ROADMAP N15).
    val load = Weight.parseLoad(weightText)
    // Halves, so 9.5 is a value and 9.3 is not (ROADMAP N6).
    val rpeHalves = rpeText.trim().ifEmpty { null }?.let(Rpe::parse)
    return SetEntryValues(
        reps = reps,
        load = load,
        rpeHalves = rpeHalves,
        // Blank is valid; anything typed has to parse *and* sit on the scale.
        rpeValid = rpeText.isBlank() || rpeHalves != null,
    )
}

/** The set this draft states. Call only where [SetEntryValues.isComplete] gates the action. */
internal fun SetEntryDraft.toEdit(): SetEdit {
    val values = values()
    return SetEdit(
        reps = values.reps ?: 0,
        weightGrams = values.load?.weightGrams ?: 0L,
        assistanceGrams = values.load?.assistanceGrams ?: 0L,
        setType = setType,
        rpeHalves = values.rpeHalves,
        note = noteText.trim().ifEmpty { null },
    )
}

/** The weight and reps steppers, in the order both callers read them (ROADMAP P1.3a, N59). */
@Composable
internal fun SetEntryNumbers(
    draft: SetEntryDraft,
    onDraftChange: (SetEntryDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = draft.values()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberStepper(
            label = stringResource(R.string.set_weight_label),
            testTag = TestTags.SET_WEIGHT_FIELD,
            increaseTag = TestTags.SET_INCREASE_WEIGHT,
            decreaseTag = TestTags.SET_DECREASE_WEIGHT,
            value = draft.weightText,
            onValueChange = { onDraftChange(draft.copy(weightText = it)) },
            keyboardType = KeyboardType.Decimal,
            // Steps the signed number on screen (ROADMAP N15), so + on an assisted
            // set reduces the help rather than deepening it.
            onStep = { delta ->
                val stepped = Weight.stepLoad(
                    signedGrams = values.load?.signedGrams ?: 0L,
                    deltaGrams = delta * Weight.DEFAULT_STEP_GRAMS,
                )
                onDraftChange(
                    draft.copy(
                        weightText = Weight.display(stepped.weightGrams, stepped.assistanceGrams),
                    ),
                )
            },
        )
        NumberStepper(
            label = stringResource(R.string.set_reps_label),
            testTag = TestTags.SET_REPS_FIELD,
            increaseTag = TestTags.SET_INCREASE_REPS,
            decreaseTag = TestTags.SET_DECREASE_REPS,
            value = draft.repsText,
            onValueChange = { onDraftChange(draft.copy(repsText = it)) },
            keyboardType = KeyboardType.Number,
            // A set of zero reps is not a set, so this floor is 1 rather than 0 —
            // unlike weight, where 0 is meaningful (bodyweight).
            onStep = { delta ->
                onDraftChange(
                    draft.copy(repsText = ((values.reps ?: 1) + delta).coerceAtLeast(1).toString()),
                )
            },
        )
    }
}

/**
 * The RPE a set is logged at, as a −/+ stepper (ROADMAP N6, N59).
 *
 * It opens on the plan's own target where there is one ([targetRpeHalves]) and on
 * [DEFAULT_RPE_HALVES] otherwise, and it always shows a number. N59 first kept the plan's value
 * *beside* an optional field, so a prescription could never be recorded as a measurement; that is
 * reversed here because the two were never in conflict — the lifter reads the plan's number, changes
 * it when the set felt different, and what is recorded is still what the set was.
 *
 * A step is half a point, so the field steps 9 → 9.5 → 10, and the ends are **clamped** to
 * [Rpe.MIN_HALVES]…[Rpe.MAX_HALVES] rather than wrapped, so holding a button down cannot leave the
 * scale (the shape the reps stepper's floor at 1 uses).
 */
@Composable
internal fun SetRpeField(
    draft: SetEntryDraft,
    onDraftChange: (SetEntryDraft) -> Unit,
    modifier: Modifier = Modifier,
    targetRpeHalves: Int? = null,
) {
    // The draft's text stays the source of truth — `values()` parses it — and the stepper only ever
    // writes halves that parse back to the number it is showing.
    val halves = Rpe.parse(draft.rpeText) ?: DEFAULT_RPE_HALVES
    val decreaseDescription = stringResource(R.string.set_stepper_decrease, RPE_STEPPER_NAME)
    val increaseDescription = stringResource(R.string.set_stepper_increase, RPE_STEPPER_NAME)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.set_rpe_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            StepButton(
                glyph = "\u2212",
                description = decreaseDescription,
                testTag = TestTags.SET_DECREASE_RPE,
                onClick = {
                    onDraftChange(
                        draft.copy(rpeText = Rpe.format((halves - 1).coerceAtLeast(Rpe.MIN_HALVES))),
                    )
                },
            )
            Text(
                text = Rpe.format(halves),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag(TestTags.SET_RPE_FIELD),
            )
            StepButton(
                glyph = "+",
                description = increaseDescription,
                testTag = TestTags.SET_INCREASE_RPE,
                onClick = {
                    onDraftChange(
                        draft.copy(rpeText = Rpe.format((halves + 1).coerceAtMost(Rpe.MAX_HALVES))),
                    )
                },
            )
        }
        // Where the number came from, because it is now *in* the field rather than beside it: the
        // plan's own target where it names one, the default it started at otherwise.
        Text(
            text = stringResource(
                if (targetRpeHalves == null) R.string.set_rpe_default else R.string.set_rpe_target,
                Rpe.format(targetRpeHalves ?: DEFAULT_RPE_HALVES),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The name the RPE stepper's buttons announce: "Decrease RPE" / "Increase RPE".
 *
 * "RPE" rather than the field's own label, which carries the scale in brackets — a screen reader
 * saying "Decrease RPE (1–10)" is reading the caption, not naming the control.
 */
private const val RPE_STEPPER_NAME = "RPE"

/**
 * A labelled number field with a −/+ pair either side (ROADMAP P1.3a).
 *
 * The text is the single source of truth: the steppers rewrite it rather than
 * holding a parallel numeric state, so a value the user typed and a value they
 * stepped cannot disagree. Typing stays unrestricted, which is why the keyboard is
 * a *hint* (`KeyboardType`) and validation happens on save.
 *
 * Both buttons are text glyphs carrying an explicit `contentDescription`: an
 * `Icon(…, contentDescription = …)` inside a merged `IconButton` did not answer to
 * `onNodeWithContentDescription`, and material-icons-core ships no `Remove` glyph
 * anyway. Two characters beat an icon-artifact dependency.
 */
@Composable
internal fun NumberStepper(
    label: String,
    testTag: String,
    increaseTag: String,
    decreaseTag: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = stringResource(R.string.set_stepper_decrease, label)
    val increaseDescription = stringResource(R.string.set_stepper_increase, label)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            glyph = "\u2212",
            description = decreaseDescription,
            testTag = decreaseTag,
            onClick = { onStep(-1) },
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).testTag(testTag),
            singleLine = true,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
        StepButton(
            glyph = "+",
            description = increaseDescription,
            testTag = increaseTag,
            onClick = { onStep(1) },
        )
    }
}

@Composable
private fun StepButton(
    glyph: String,
    description: String,
    testTag: String,
    onClick: () -> Unit,
) {
    IconButton(modifier = Modifier.testTag(testTag), onClick = onClick) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}
