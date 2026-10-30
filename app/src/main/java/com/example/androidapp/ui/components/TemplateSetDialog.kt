package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.androidapp.domain.DurationFormat
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.HOLD_STEP_SECONDS
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
     * The step this exercise's ± taps take, or null for the unit's own (ROADMAP N77, N111).
     *
     * Nullable rather than pre-resolved because the unit travels with it, the same shape the
     * workout's fields take: a caller holding what the exercise says and the unit its numbers read
     * in has everything the step needs.
     */
    stepGrams: Long? = null,
    /**
     * Which roles this set may be given (ROADMAP N79, B64).
     *
     * A rung hangs off the set above it, so the caller — which knows the position this row holds, or
     * would hold when it is being added — answers whether one is available here. The default offers
     * everything, which is what a dialog with no position to speak of wants.
     */
    offers: (SetType) -> Boolean = { true },
    /**
     * Whether this row is the one that **opens** its run, which is the only place the value is written
     * (ROADMAP N79, B120).
     *
     * Not the same question as [offers]. A drop appended to an existing run is anchored, so it is offered
     * — and it *inherits* the run's value rather than naming it, which the write boundary enforces ("A
     * run's drop value is written on its first rung"). Deriving this from "is a drop allowed here" made
     * every rung an opener: the dialog demanded a value the boundary then refused on any rung but the
     * first, so a second drop set could not be authored at all.
     */
    holdsTheRunValue: Boolean = true,
) {
    // `remember`, not `rememberSaveable`: a data class is not something a Bundle can
    // hold, and registering one throws when the dialog opens. The set editor's own
    // draft is held the same way for the same reason.
    var draft by remember { mutableStateOf(TemplateSetDraft(initial, unit, holdsTheRunValue)) }

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
        text = {
            TargetFields(
                draft = draft,
                onChange = { draft = it },
                offers = offers,
                stepGrams = stepGrams,
            )
        },
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
    /** The hold this set prescribes, as typed (`m:ss`), or empty for a counted set (ROADMAP N242). */
    val holdText: String = "",
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
        holdText = edit.targetDurationSeconds?.let { DurationFormat.ofSeconds(it.toLong()) }.orEmpty(),
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

    /**
     * The hold as typed, in **seconds** (ROADMAP N242).
     *
     * Read in the form the set's own field shows — `m:ss` where a set is read back — so a plan prescribes a
     * hold the same way a session records one. A number with no colon is read as seconds, which is what
     * somebody typing "45" means.
     */
    val holdSeconds: Int? get() = DurationFormat.parseSeconds(holdText.trim())

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

    /**
     * A set is either counted or held (ROADMAP N242): naming both would prescribe a number the set cannot
     * answer, and the write boundary refuses it for the same reason.
     */
    val prescriptionIsValid: Boolean
        get() = holdText.isBlank() || ((holdSeconds ?: 0) >= 1 && repsMinText.isBlank() && repsMaxText.isBlank())

    val isValid: Boolean get() = weightIsValid && repsAreValid && dropValueIsValid && prescriptionIsValid

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
        dropValueGrams = dropValueGrams.takeIf { writesTheRunValue },
        targetDurationSeconds = holdSeconds.takeIf { !role.isRung },
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
    /**
     * What a rung loads, derived from its anchor (ROADMAP N79); null for a set of its own.
     *
     * The whole [Load] rather than its weight (B153): a cluster rung repeats the anchor exactly, so a family
     * whose anchor is machine-assisted derives *assistance* and no added weight — passing only grams made that
     * read as "0 kg" where the plan gives 20 kg of help.
     */
    rungLoad: Load? = null,
    /** True on the rung that holds the run's value, so the line names what the run takes off (N79). */
    holdsTheRunValue: Boolean = false,
): String {
    // The plan's target RPE is the exercise's one number (N59) and is drawn in the exercise block, so
    // a set's own line states only what belongs to the set.
    return listOfNotNull(
        weightLine(unit, rungLoad),
        dropValueLine(unit, holdsTheRunValue),
        repsLine(),
        // A prescribed hold is the set's prescription, not a detail of it (N242): a 45-second carry that
        // stated only its load read as a counted set with no reps.
        targetDurationSeconds?.let {
            stringResource(R.string.set_hold_value, DurationFormat.ofSeconds(it.toLong()))
        },
        note,
    ).joinToString(" · ")
}

@Composable
private fun TargetFields(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
    /** Which roles this position can take (ROADMAP N79, B64). */
    offers: (SetType) -> Boolean,
    /** The step this exercise's ± taps take, or null for the unit's own (ROADMAP N77, N111). */
    stepGrams: Long?,
) {
    // The step once, for every field that moves a load: the exercise's own, or the unit's (N77).
    val step = Weight.stepGramsFor(stepGrams, draft.unit)

    Column(
        // The dialog grew a row when the ± pairs arrived (N111), and a plan is authored on whatever
        // screen the lifter has, so the fields scroll rather than the note being clipped off the end.
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        if (draft.writesTheRunValue) DropValueField(draft = draft, onChange = onChange, step = step)
        if (!draft.role.isRung) LoadField(draft = draft, onChange = onChange, step = step)
        RepsFields(draft = draft, onChange = onChange)
        NoteField(draft = draft, onChange = onChange)
    }
}

/**
 * The one number a drop run is authored with: what each rung takes off the anchor (N79).
 *
 * Stepped by the exercise's own step like the load it comes off (N111), and floored at zero the way a
 * weight is rather than the way assistance is: a drop value is a magnitude, so stepping down from
 * nothing states 0 — which this field already refuses — rather than inventing a drop nobody named.
 */
@Composable
private fun DropValueField(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
    step: Long,
) {
    NumberStepper(
        label = stringResource(R.string.template_set_drop_value_label, draft.unit.label()),
        supportingText = stringResource(R.string.template_set_drop_value_hint),
        testTag = TestTags.TEMPLATE_SET_DROP_VALUE,
        increaseTag = TestTags.TEMPLATE_SET_INCREASE_DROP_VALUE,
        decreaseTag = TestTags.TEMPLATE_SET_DECREASE_DROP_VALUE,
        value = draft.dropValueText,
        onValueChange = { onChange(draft.copy(dropValueText = it)) },
        keyboardType = KeyboardType.Decimal,
        isError = !draft.dropValueIsValid,
        onStep = { delta ->
            val stepped = ((draft.dropValueGrams ?: 0L) + delta * step).coerceAtLeast(0L)
            onChange(draft.copy(dropValueText = Weight.format(stepped, draft.unit)))
        },
    )
}

/**
 * The load a set of its own names, which a rung does not have (N15, N79).
 *
 * The ± pair holds the workout's own rule (N111): the field is one *signed* number, so a target of
 * `-20` is 20 kg of assistance and `+` makes the target harder rather than adding weight to a bar
 * that is not there.
 */
@Composable
private fun LoadField(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
    step: Long,
) {
    val load = draft.load

    NumberStepper(
        label = stringResource(R.string.template_set_weight, draft.unit.label()),
        testTag = TestTags.TEMPLATE_SET_WEIGHT,
        increaseTag = TestTags.TEMPLATE_SET_INCREASE_WEIGHT,
        decreaseTag = TestTags.TEMPLATE_SET_DECREASE_WEIGHT,
        value = draft.weightText,
        onValueChange = { onChange(draft.copy(weightText = it)) },
        keyboardType = KeyboardType.Decimal,
        isError = !draft.weightIsValid,
        onStep = { delta ->
            val stepped = Weight.stepLoad(
                signedGrams = load?.signedGrams ?: 0L,
                deltaGrams = delta * step,
            )
            onChange(
                draft.copy(
                    weightText = Weight.display(
                        weightGrams = stepped.weightGrams,
                        assistanceGrams = stepped.assistanceGrams,
                        unit = draft.unit,
                    ),
                ),
            )
        },
    )
}

/**
 * The target range, which a rung does not carry either — the group's is the anchor's (N14, N79).
 *
 * One pair per end (N111): a plan writes `(max 2)` or a range, and each end moves on its own by the
 * rule the workout's reps field holds — one rep a tap, floored at 1. A range that runs backwards is
 * left for this dialog's own validity to refuse rather than being swapped here, because swapping
 * would silently change what the lifter asked for.
 */
@Composable
private fun RepsFields(
    draft: TemplateSetDraft,
    onChange: (TemplateSetDraft) -> Unit,
) {
    if (draft.role.isRung) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberStepper(
            label = stringResource(R.string.template_set_reps_min),
            testTag = TestTags.TEMPLATE_SET_REPS_MIN,
            increaseTag = TestTags.TEMPLATE_SET_INCREASE_REPS_MIN,
            decreaseTag = TestTags.TEMPLATE_SET_DECREASE_REPS_MIN,
            value = draft.repsMinText,
            onValueChange = { onChange(draft.copy(repsMinText = it)) },
            keyboardType = KeyboardType.Number,
            isError = !draft.repsAreValid,
            onStep = { delta -> onChange(draft.copy(repsMinText = steppedReps(draft.repsMin, delta))) },
        )
        NumberStepper(
            label = stringResource(R.string.template_set_reps_max),
            testTag = TestTags.TEMPLATE_SET_REPS_MAX,
            increaseTag = TestTags.TEMPLATE_SET_INCREASE_REPS_MAX,
            decreaseTag = TestTags.TEMPLATE_SET_DECREASE_REPS_MAX,
            value = draft.repsMaxText,
            onValueChange = { onChange(draft.copy(repsMaxText = it)) },
            keyboardType = KeyboardType.Number,
            isError = !draft.repsAreValid,
            onStep = { delta -> onChange(draft.copy(repsMaxText = steppedReps(draft.repsMax, delta))) },
        )
        // The hold this set prescribes, if it is held rather than counted (ROADMAP N242). One field with
        // its own ± pair, the same shape a hold's own field has on the session's set.
        NumberStepper(
            label = stringResource(R.string.template_set_hold),
            testTag = TestTags.TEMPLATE_SET_HOLD,
            increaseTag = TestTags.TEMPLATE_SET_INCREASE_HOLD,
            decreaseTag = TestTags.TEMPLATE_SET_DECREASE_HOLD,
            value = draft.holdText,
            onValueChange = { onChange(draft.copy(holdText = it)) },
            keyboardType = KeyboardType.Number,
            isError = !draft.prescriptionIsValid,
            onStep = { delta ->
                val stepped = ((draft.holdSeconds ?: HOLD_STEP_SECONDS) + delta * HOLD_STEP_SECONDS)
                onChange(draft.copy(holdText = DurationFormat.ofSeconds(stepped.coerceAtLeast(1).toLong())))
            },
        )
    }
}

/**
 * The plan's rep count after one ± tap (ROADMAP N111).
 *
 * The workout's reps rule (N59) for a field that may be empty: blank is where a count starts, so the
 * first tap states 1 rather than stepping a number that was never there, and no tap can reach 0 —
 * a set of zero reps is not a set.
 */
private fun steppedReps(current: Int?, delta: Int): String =
    ((current ?: 1) + delta).coerceAtLeast(1).toString()

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
private fun TemplateSet.weightLine(unit: WeightUnit, rungLoad: Load?): String? = when {
    role.isRung -> rungLoad?.let {
        stringResource(
            R.string.template_set_weight_value,
            Weight.display(it.weightGrams, it.assistanceGrams, unit),
            unit.label(),
        )
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
