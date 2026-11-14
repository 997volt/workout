package com.example.androidapp.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.taxonomySubtitle
import com.example.androidapp.ui.components.ExerciseActionsMenu
import com.example.androidapp.ui.components.ExerciseMenuTags
import com.example.androidapp.ui.components.NumberStepper
import com.example.androidapp.ui.components.OpenExerciseAction
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.WarmUpAction
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.steppedRpeText

// A planned exercise's row and its own controls (ROADMAP N71, N81, N91, N116): the fold, the overflow,
// the plan's rest and cue, and the effort field that shares their line.
/**
 * The effort, the rest, the accept and the cue this exercise's plan prescribes (N14, N59, N80, N111, N116).
 *
 * The **target RPE is one number for the exercise**, beside the rest the plan already carried rather
 * than on every planned set. All three blank means "the plan says nothing" — the library's rest and
 * cue show through, and the workout's RPE stepper opens on its default — so the fields are empty
 * rather than zero.
 *
 * **The cue has a line of its own** (N80) — see [PlanCueField] — and it is read *first*, above the
 * numbers (N99): the block reads in the order it is filled in, and the cue is the thing performed
 * differently rather than a number the plan already had.
 *
 * **The effort, the rest and the save share one line, in that order** (N116). The number the plan
 * wants, the rest it prescribes and the control that writes both are one statement about one set, and
 * splitting them made the effort look like a different kind of fact from the rest beside it. This
 * supersedes the row of its own that N111 gave the RPE, and N68's squeeze behind it — but only here:
 * the workout screen's own RPE stepper is untouched. What N111 priced was a label, a value and two
 * buttons beside another number, and the arithmetic has changed: the label is the field's own floating
 * one rather than a leading word, and both numbers are one or two characters. So the stepper takes
 * `weight(1f)` and the fixed rest field narrows to [REST_FIELD_WIDTH] — and the two 48 dp step buttons
 * and the save's own 48 dp target do not shrink, because that is the app's touch-target floor. If a
 * narrower device runs out of room, a label gives way rather than a control. The field stays typeable
 * beside the buttons, because every planned field is optional and a stepper alone could never put a
 * target back to none (N14).
 */
@Suppress("LongMethod") // The plan's own form; the find-the-weight switch (N309) is the line at the ceiling.
@Composable
internal fun ExercisePlanFields(
    exercise: TemplateExercise,
    appProgressionMode: ProgressionMode,
    onSave: (Int?, String?, Int?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rest by rememberSaveable(exercise.id) { mutableStateOf(exercise.restSeconds?.toString().orEmpty()) }
    var cue by rememberSaveable(exercise.id) { mutableStateOf(exercise.techniqueNote.orEmpty()) }
    var rpe by rememberSaveable(exercise.id) {
        mutableStateOf(exercise.targetRpeHalves?.let(Rpe::format).orEmpty())
    }
    var findWeight by rememberSaveable(exercise.id) { mutableStateOf(exercise.calibrationPending) }
    val restSeconds = rest.trim().ifEmpty { null }?.toIntOrNull()
    val restIsValid = rest.isBlank() || (restSeconds?.let(RestTimer::isValidPrescription) == true)
    val rpeHalves = rpe.trim().ifEmpty { null }?.let(Rpe::parse)
    val rpeIsValid = rpe.isBlank() || rpeHalves != null
    val changed = restSeconds != exercise.restSeconds ||
        cue.trim().ifEmpty { null } != exercise.techniqueNote ||
        rpeHalves != exercise.targetRpeHalves ||
        findWeight != exercise.calibrationPending

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressionStepNotice(exercise = exercise, appProgressionMode = appProgressionMode)
        PlanCueField(cue = cue, onCueChange = { cue = it })
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.template_find_weight))
                Text(
                    text = stringResource(R.string.template_find_weight_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = findWeight,
                onCheckedChange = { findWeight = it },
                modifier = Modifier.testTag(TestTags.TEMPLATE_FIND_WEIGHT),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The effort, then the rest, then the control that writes both (N116). Reading them on one line
            // is the point: they are one statement about one set, and the stepper is the flexible member —
            // its field takes what is left, which is what makes room for the two 48 dp buttons beside a rest
            // field and a save.
            NumberStepper(
                label = stringResource(R.string.set_rpe_label),
                testTag = TestTags.TEMPLATE_EXERCISE_RPE,
                increaseTag = TestTags.TEMPLATE_EXERCISE_INCREASE_RPE,
                decreaseTag = TestTags.TEMPLATE_EXERCISE_DECREASE_RPE,
                value = rpe,
                onValueChange = { rpe = it },
                keyboardType = KeyboardType.Decimal,
                isError = !rpeIsValid,
                // The workout's own RPE rule (N59): a blank target states the working end a logged set
                // opens on rather than stepping a number that was never there, and neither button can
                // leave 1–10.
                onStep = { delta -> rpe = steppedRpeText(Rpe.parse(rpe), delta) },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = rest,
                onValueChange = { rest = it },
                modifier = Modifier.width(REST_FIELD_WIDTH).testTag(TestTags.TEMPLATE_REST_FIELD),
                singleLine = true,
                isError = !restIsValid,
                label = { Text(stringResource(R.string.template_rest_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            // Last by construction now that there are three controls on the line, so the spacer that held
            // it at the right margin when the rest field was the only thing here (N80) is gone.
            IconButton(
                onClick = { onSave(restSeconds, cue.trim().ifEmpty { null }, rpeHalves, findWeight) },
                enabled = changed && restIsValid && rpeIsValid,
                modifier = Modifier.testTag(TestTags.TEMPLATE_REST_CUE_SAVE),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.template_rest_cue_save),
                )
            }
        }
    }
}

/**
 * How wide the rest field is on the shared line (N116).
 *
 * Narrower than the 110 dp it held alone: a three- or four-digit rest still fits, and the 14 dp it gives
 * back is what lets the stepper's field keep a readable width beside it on the smallest phone this app
 * targets. Fixed rather than weighted because the *rest* is the one of the three whose width does not
 * depend on its content — the stepper's field and the save are already the flexible and the fixed members.
 */
private val REST_FIELD_WIDTH = 96.dp

/**
 * The cue, on a full-width line of its own (N80).
 *
 * Split out of [ExercisePlanFields] when N80's second row pushed that function past the length this
 * project allows, and it is the piece that changed: it took `weight(1f)` between the two fixed-width
 * number fields and the save, which on a phone left it about a word wide with `singleLine` on top — the
 * same squeeze N68 fixed for the workout's RPE stepper by giving it a row to itself.
 */
@Composable
private fun PlanCueField(
    cue: String,
    onCueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = cue,
        onValueChange = onCueChange,
        modifier = modifier.fillMaxWidth().testTag(TestTags.TEMPLATE_CUE_FIELD),
        singleLine = true,
        label = { Text(stringResource(R.string.template_cue_label)) },
    )
}

@Composable
internal fun TemplateExerciseRow(
    exercise: TemplateExercise,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    /** The warm-up entry this row's ⋮ offers, or null where no ramp can be built (N28, B50, N81). */
    addWarmUps: WarmUpAction? = null,
    supersetLabels: Map<String, String> = emptyMap(),
) {
    // Read here rather than inside the semantics lambda below, which is not a composable context (N91).
    val openLabel = stringResource(R.string.template_exercise_open)
    val foldedLabel = stringResource(R.string.template_exercise_folded)
    ListItem(
        headlineContent = {
            // The name is the control (ROADMAP N91): tapping it opens the block and tapping it again
            // folds it. The label names the *action* — "Open Back Squat", "Fold Back Squat" — and the
            // state is announced beside it, because the same name behaves differently depending on state
            // the screen reader cannot otherwise see (B21's rule, applied to a fold).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(
                            if (isExpanded) R.string.template_close_exercise else R.string.template_open_exercise,
                            exercise.exerciseName,
                        ),
                        onClick = onToggle,
                    )
                    // Announced beside the action, because the same name behaves differently depending on a
                    // state a reader cannot see (B21's rule, applied to a fold).
                    .semantics { stateDescription = if (isExpanded) openLabel else foldedLabel }
                    .testTag(TestTags.templateFold(exercise.id)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        // `A1 · ` in front of a grouped exercise, or nothing (ROADMAP B16).
                        text = "$position. " +
                            (supersetLabels[exercise.id]?.let { "$it · " }.orEmpty()) +
                            exercise.exerciseName,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        supportingContent = {
            // Stated facts only (ROADMAP B103, N103): a custom exercise has a name and nothing else, so
            // the row leads with it rather than with "Other · Other".
            taxonomySubtitle(exercise.primaryMuscle, exercise.equipment)?.let { Text(it) }
        },
        trailingContent = {
            ExerciseMenu(
                exercise = exercise,
                canMoveUp = !isFirst,
                canMoveDown = !isLast,
                canToggleSuperset = onToggleSuperset != null,
                addWarmUps = addWarmUps,
                onOpenExercise = onOpenExercise,
                onToggleSuperset = { onToggleSuperset?.invoke() },
                onMove = { delta -> if (delta < 0) onMoveUp() else onMoveDown() },
                onRemove = onRemove,
            )
        },
        modifier = modifier.testTag(TestTags.templateExerciseRow(exercise.id)),
    )
}

/**
 * The one control a planned exercise's row offers, and the rare entries behind it (ROADMAP N71, N81).
 *
 * The superset toggle, the two arrows and the delete icon became the same ⋮ the live workout uses, and
 * removal asks first because it takes the planned sets with it (B2). N91 leaves it where it is: the fold
 * is the row's own name, so opening a block is never a tap on a menu.
 */
@Composable
private fun ExerciseMenu(
    exercise: TemplateExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    /** False for the first planned exercise, which has nothing above it to pair with (B28). */
    canToggleSuperset: Boolean,
    addWarmUps: WarmUpAction?,
    onOpenExercise: () -> Unit,
    onToggleSuperset: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    ExerciseActionsMenu(
        contentDescription = stringResource(R.string.exercise_menu_more, exercise.exerciseName),
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        // Not offered rather than writing a group that rewrites every ungrouped row.
        supersetGrouped = if (canToggleSuperset) exercise.supersetGroup != null else null,
        addWarmUps = addWarmUps,
        // Only this screen supplies it: the live workout's menu must not grow a way out of a running
        // session (N121).
        openExercise = OpenExerciseAction(
            tag = TestTags.templateOpenExercise(exercise.id),
            onClick = onOpenExercise,
        ),
        removeTitle = stringResource(R.string.template_remove_confirm_title),
        removeText = stringResource(R.string.template_remove_confirm_text, exercise.exerciseName),
        onToggleSuperset = onToggleSuperset,
        onMove = onMove,
        onRemove = onRemove,
        tags = ExerciseMenuTags(
            menu = TestTags.templateMenu(exercise.id),
            moveUp = TestTags.templateMove(exercise.id, up = true),
            moveDown = TestTags.templateMove(exercise.id, up = false),
            superset = TestTags.supersetToggle(exercise.id),
            remove = TestTags.templateRemove(exercise.id),
        ),
    )
}
