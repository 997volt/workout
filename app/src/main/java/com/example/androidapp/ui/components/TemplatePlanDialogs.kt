package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.repository.TemplateSetEdit

/**
 * One planned set's targets (ROADMAP N14).
 *
 * Every field is optional, and the dialog says so by leaving them empty: a plan that
 * says "work up to a heavy single" has no weight to write down, and a zero would be a
 * claim the app cannot check. Reps are a range because a plan writes `(max 2)` and
 * means only the upper bound.
 */
@Composable
fun TemplateSetDialog(
    initial: TemplateSetEdit,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (TemplateSetEdit) -> Unit,
    modifier: Modifier = Modifier,
    /** The unit this target load is typed and shown in (ROADMAP N64). */
    unit: WeightUnit = WeightUnit.KILOGRAMS,
    /**
     * Which roles this set may be given (ROADMAP N79, B64).
     *
     * A rung hangs off the set above it, so the caller — which knows the position this row holds, or
     * would hold when it is being added — answers whether one is available here. It decides two things
     * at once: the roles the picker offers, and whether this row is the place a run's value is authored.
     * The default offers everything, which is what a dialog with no position to speak of wants.
     */
    offers: (SetType) -> Boolean = { true },
) {
    // `remember`, not `rememberSaveable`: a data class is not something a Bundle can
    // hold, and registering one throws when the dialog opens. The set editor's own
    // draft is held the same way for the same reason.
    var draft by remember { mutableStateOf(TemplateSetDraft(initial, unit, offers(SetType.DROP))) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNew) R.string.template_set_title_new else R.string.template_set_title,
                ),
            )
        },
        text = { TargetFields(draft = draft, onChange = { draft = it }, offers = offers) },
        confirmButton = {
            AppTextButton(
                enabled = draft.isValid,
                modifier = Modifier.testTag(TestTags.TEMPLATE_SET_SAVE),
                onClick = { onSave(draft.toEdit()) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.TEMPLATE_SET_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * The draft, as text, with what it parses to.
 *
 * A holder rather than five `remember`s in the dialog: the parsing and the validation
 * are the same thing read twice, and keeping them together is what stops the Save
 * button enabling on a number the repository would refuse.
 *
 * [targetRpeHalves] is carried but **not edited** (N59, amended): a plan's target RPE is one number
 * per exercise now and lives in the editor behind this dialog, while the per-set value a plan may
 * still hold from before the change is a legacy fallback — so the dialog round-trips it rather than
 * inventing one. It *is* dropped when the role is one that records no effort (ROADMAP N67): a
 * planned warm-up carries no target, which is what migration 28→29 cleared, so re-roling a set to
 * *Warm-up* has to be able to clear it as well.
 */
data class TemplateSetDraft(
    val role: SetType = SetType.NORMAL,
    val weightText: String = "",
    /**
     * The value this run takes off the set above it, as typed, or empty (ROADMAP N79).
     *
     * Only a drop run writes one, and only on its first rung: the rest inherit it, so a later rung
     * showing a field would be asking for a number nothing reads.
     */
    val dropValueText: String = "",
    val repsMinText: String = "",
    val repsMaxText: String = "",
    /** The plan's legacy per-set target RPE, passed through unchanged, or null (N59). */
    val targetRpeHalves: Int? = null,
    /**
     * Where in the range the lifter has climbed, passed through and clamped (ROADMAP N74).
     *
     * Not edited here: the range is what the lifter authors, and this only follows it.
     */
    val targetRepsCurrent: Int? = null,
    val note: String = "",
    /** The unit this target is typed and shown in (ROADMAP N64). */
    val unit: WeightUnit = WeightUnit.KILOGRAMS,
    /**
     * Whether this row is the place a run's value may be authored (ROADMAP N79, B64).
     *
     * True only where the caller says a drop is available at this position, which is the same answer
     * that puts **Drop** in the picker. A rung's load is derived from the anchor and the value belongs
     * to the run, so the rung that opens it is the only one that may name it — and with nothing above
     * to hang off, there is no run to name a value for.
     */
    val holdsTheRunValue: Boolean = true,
) {
    constructor(edit: TemplateSetEdit, unit: WeightUnit, holdsTheRunValue: Boolean = true) : this(
        role = edit.role,
        dropValueText = edit.dropValueGrams?.let { Weight.format(it, unit) }.orEmpty(),
        holdsTheRunValue = holdsTheRunValue,
        weightText = if (edit.targetWeightGrams != null || edit.targetAssistanceGrams != null) {
            Weight.display(edit.targetWeightGrams ?: 0L, edit.targetAssistanceGrams ?: 0L, unit)
        } else {
            ""
        },
        unit = unit,
        repsMinText = edit.targetRepsMin?.toString().orEmpty(),
        repsMaxText = edit.targetRepsMax?.toString().orEmpty(),
        targetRpeHalves = edit.targetRpeHalves,
        targetRepsCurrent = edit.targetRepsCurrent,
        note = edit.note.orEmpty(),
    )

    // The plan writes assistance the same way a set does: -20 in the weight field
    // (ROADMAP N15).
    val load: Load? get() = Weight.parseLoad(weightText, unit)
    val weight: Long? get() = load?.weightGrams
    val assistanceGrams: Long? get() = load?.assistanceGrams
    val repsMin: Int? get() = repsMinText.trim().ifEmpty { null }?.toIntOrNull()
    val repsMax: Int? get() = repsMaxText.trim().ifEmpty { null }?.toIntOrNull()

    /** The drop value as typed, or null when the field is empty (ROADMAP N79). */
    val dropValueGrams: Long? get() = dropValueText.trim().ifEmpty { null }?.let { Weight.parse(it, unit) }

    /** True where this set is a drop that opens its run, which is the only place a value is written. */
    val writesTheRunValue: Boolean get() = role == SetType.DROP && holdsTheRunValue

    // Blank is allowed everywhere; anything typed has to be a usable number, and a
    // range that runs backwards is refused rather than silently swapped.
    val weightIsValid: Boolean get() = weightText.isBlank() || load != null
    val repsAreValid: Boolean
        get() {
            // Read once into locals: a computed property has a custom getter, so Kotlin
            // will not smart-cast it at the use site.
            val min = repsMin
            val max = repsMax
            return (repsMinText.isBlank() || (min != null && min >= 1)) &&
                (repsMaxText.isBlank() || (max != null && max >= 1)) &&
                !(min != null && max != null && min > max)
        }
    /**
     * A drop that opens its run has to name the value it takes off (ROADMAP N79): without it there is
     * no ladder to load, and the write boundary refuses the row for the same reason.
     */
    val dropValueIsValid: Boolean
        get() = if (!writesTheRunValue) true else (dropValueGrams ?: 0L) > 0L

    val isValid: Boolean get() = weightIsValid && repsAreValid && dropValueIsValid

    fun toEdit() = TemplateSetEdit(
        role = role,
        // A rung carries no weight of its own and no reps of its own: what it loads is derived from the
        // anchor, and the group's target is the anchor's (ROADMAP N79). Writing either back would put a
        // number on the row that nothing reads.
        targetWeightGrams = weight.takeIf { !role.isRung },
        targetAssistanceGrams = assistanceGrams?.takeIf { it > 0L && !role.isRung },
        targetRepsMin = repsMin.takeIf { !role.isRung },
        targetRepsMax = repsMax.takeIf { !role.isRung },
        targetRepsCurrent = currentRepsWithinRange(),
        // A warm-up carries no effort, whatever the draft still held from before the role changed
        // (ROADMAP N67) — the rule the logged set's own write boundary holds, for the plan side.
        targetRpeHalves = targetRpeHalves.takeIf { role.recordsEffort },
        dropValueGrams = dropValueGrams.takeIf { writesTheRunValue },
        note = note.trim().ifEmpty { null },
    )

    /**
     * The current rep target the save carries (ROADMAP N74).
     *
     * The range is what the editor edits and this only follows it: a target that has been climbed to
     * is clamped into the range on screen, so narrowing the range is the one edit that moves it and
     * widening it leaves the climb where it was. A set that has never been progressed carries nothing
     * — null already means "the range's floor", and materializing it here would write a number on
     * every edit of the note or the load.
     */
    private fun currentRepsWithinRange(): Int? {
        val min = repsMin
        val max = repsMax
        // A set that names no reps, or has never been progressed, carries nothing: null already means
        // "the range's floor", so materializing one here would write a number on every edit of the
        // note or the load.
        val stored = targetRepsCurrent?.takeIf { min != null || max != null }
        return stored?.coerceAtLeast(min ?: 1)?.coerceAtMost(max ?: Int.MAX_VALUE)
    }
}

/**
 * One planned set as a line: what it loads, what it asks for, and anything it notes.
 *
 * `internal` since N72: the program's read-only preview shows the same line, and a second formatter
 * for one plan would be the way two readings of it start. A rung reads differently on purpose (N79):
 * its load is derived rather than written down, it carries no reps of its own, and the value it takes
 * off the anchor is named once, on the rung that holds it.
 */
@Composable
internal fun TemplateSet.summary(
    unit: WeightUnit,
    /** What a rung loads, derived from its anchor (ROADMAP N79); null for a set of its own. */
    rungWeightGrams: Long? = null,
    /** True on the rung that holds the run's value, so the line names what the run takes off (N79). */
    holdsTheRunValue: Boolean = false,
): String {
    // A planned warm-up shows no effort, the rule a logged warm-up already holds (ROADMAP N67), and a
    // rung shows none either (N79): `recordsEffort` is the one rule both read.
    val rpeHalves = if (role.recordsEffort) targetRpeHalves?.let { rpeMarker(it) } else null
    return listOfNotNull(
        weightLine(unit, rungWeightGrams),
        dropValueLine(unit, holdsTheRunValue),
        repsLine(),
        rpeHalves,
        note,
    ).joinToString(" · ")
}

@Composable
private fun TargetFields(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
    /** Which roles this position can take (ROADMAP N79, B64). */
    offers: (SetType) -> Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SetRoleSelector(
            role = draft.role,
            onSelect = { onChange(draft.copy(role = it)) },
            testTag = TestTags.TEMPLATE_SET_ROLE,
            optionTag = TestTags::templateSetRole,
            // A rung is not offered where there is nothing above it to hang off: the boundary would
            // refuse the save, and a control that cannot write is worse than no control (N67, B64).
            offers = offers,
        )
        // A rung has no load of its own to type: a cluster repeats the anchor's and a drop is the
        // anchor less the run's value, so the only field it can hold is that value — and only the rung
        // that opens the run holds it (ROADMAP N79).
        if (draft.writesTheRunValue) DropValueField(draft = draft, onChange = onChange)
        if (!draft.role.isRung) LoadField(draft = draft, onChange = onChange)
        RepsFields(draft = draft, onChange = onChange)
        NoteField(draft = draft, onChange = onChange)
    }
}

/** The one number a drop run is authored with: what each rung takes off the anchor (N79). */
@Composable
private fun DropValueField(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
) {
    OutlinedTextField(
        value = draft.dropValueText,
        onValueChange = { onChange(draft.copy(dropValueText = it)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.TEMPLATE_SET_DROP_VALUE),
        singleLine = true,
        isError = !draft.dropValueIsValid,
        label = { Text(stringResource(R.string.template_set_drop_value_label, draft.unit.label())) },
        supportingText = { Text(stringResource(R.string.template_set_drop_value_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** The load a set of its own names, which a rung does not have (N15, N79). */
@Composable
private fun LoadField(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
) {
    OutlinedTextField(
        value = draft.weightText,
        onValueChange = { onChange(draft.copy(weightText = it)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.TEMPLATE_SET_WEIGHT),
        singleLine = true,
        isError = !draft.weightIsValid,
        label = { Text(stringResource(R.string.template_set_weight, draft.unit.label())) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** The target range, which a rung does not carry either — the group's is the anchor's (N14, N79). */
@Composable
private fun RepsFields(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
) {
    if (draft.role.isRung) return

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft.repsMinText,
            onValueChange = { onChange(draft.copy(repsMinText = it)) },
            modifier = Modifier.weight(1f).testTag(TestTags.TEMPLATE_SET_REPS_MIN),
            singleLine = true,
            isError = !draft.repsAreValid,
            label = { Text(stringResource(R.string.template_set_reps_min)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = draft.repsMaxText,
            onValueChange = { onChange(draft.copy(repsMaxText = it)) },
            modifier = Modifier.weight(1f).testTag(TestTags.TEMPLATE_SET_REPS_MAX),
            singleLine = true,
            isError = !draft.repsAreValid,
            label = { Text(stringResource(R.string.template_set_reps_max)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

/** A note is the one thing every role can carry, a rung included (N14, N79). */
@Composable
private fun NoteField(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
) {
    OutlinedTextField(
        value = draft.note,
        onValueChange = { onChange(draft.copy(note = it)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.TEMPLATE_SET_NOTE),
        label = { Text(stringResource(R.string.template_set_note)) },
        // Two lines, as this field had before the N79 refactor moved it into its own function and
        // dropped the height with it (B71): a note is a sentence, and one line hides most of one.
        minLines = 2,
    )
}

/**
 * The load this set shows, or null (ROADMAP N14, N79, B71).
 *
 * A rung has no weight written down, so it states what it derives — never the stored number the ladder
 * does not read. Where nothing can be derived it **says so** rather than leaving the line blank: the
 * run names no value yet, or the anchor has no added weight to take one off, and a row with no load and
 * no reason reads as a bug rather than as something to fill in.
 */
@Composable
private fun TemplateSet.weightLine(unit: WeightUnit, rungWeightGrams: Long?): String? = when {
    role.isRung -> rungWeightGrams?.let {
        stringResource(R.string.template_set_weight_value, Weight.format(it, unit), unit.label())
    } ?: stringResource(R.string.template_set_rung_no_load)

    targetWeightGrams != null || targetAssistanceGrams != null -> stringResource(
        R.string.template_set_weight_value,
        Weight.display(targetWeightGrams ?: 0L, targetAssistanceGrams ?: 0L, unit),
        unit.label(),
    )

    else -> null
}

/** What the run takes off the anchor, named once, on the rung that holds it (ROADMAP N79). */
@Composable
private fun TemplateSet.dropValueLine(unit: WeightUnit, holdsTheRunValue: Boolean): String? =
    dropValueGrams
        ?.takeIf { holdsTheRunValue && role == SetType.DROP }
        ?.let { stringResource(R.string.template_set_drop_value, Weight.format(it, unit), unit.label()) }

/** The reps this set asks for, or null: a rung carries none, because the group's is the anchor's (N79). */
@Composable
private fun TemplateSet.repsLine(): String? = if (role.isRung) {
    null
} else {
    when {
        targetRepsMin != null && targetRepsMax != null ->
            pluralStringResource(
                R.plurals.template_reps_range,
                targetRepsMax,
                targetRepsMin,
                targetRepsMax,
            )

        targetRepsMin != null -> pluralStringResource(
            R.plurals.template_reps_min_only,
            targetRepsMin,
            targetRepsMin,
        )

        targetRepsMax != null -> pluralStringResource(
            R.plurals.template_reps_max_only,
            targetRepsMax,
            targetRepsMax,
        )
        else -> null
    }
}
