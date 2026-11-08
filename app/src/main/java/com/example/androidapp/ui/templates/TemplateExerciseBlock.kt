package com.example.androidapp.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.isAnchoredAt
import com.example.androidapp.domain.model.runAt
import com.example.androidapp.domain.model.rungLoadAt
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.ui.components.TemplateSetDialog
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.WriteOutcome
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.setNumbers
import com.example.androidapp.ui.components.summary
import com.example.androidapp.ui.components.warmUpActionFor

// One planned exercise in the template editor (ROADMAP N3, N14, N81, N91): its folded row, the sets it
// holds, the dialog that edits one, and the plan fields it writes.
/**
 * One exercise in the editor: its row, and — until the row is folded — the plan's sets, the rest and cue
 * the plan prescribes, and the button that adds another set (ROADMAP N14, N81, N91).
 *
 * **Folded until opened** (N91): a plan of five exercises used to be a long scroll of controls with the
 * names — the thing the screen is scanned by — lost among them, so the row is now the whole of a block
 * until its name is tapped. The state is the list's rather than this composable's, because a rotation has
 * to put back every block the lifter opened and the list is where the whole of that answer lives.
 *
 * The sets are lines in the block rather than a count behind a tap (N81): the plan is what this screen is
 * for, and the dialog that held it made reading it a gesture. The one panel that still opens — a set's
 * targets — keeps its state here rather than in the screen, the shape the exercise section in a workout
 * uses: the state that says "this panel is open" belongs next to the row that opens it.
 */
@Composable
internal fun TemplateExerciseBlock(
    exercise: TemplateExercise,
    /** The app's own answer to Done, which this movement may override (ROADMAP N235). */
    appProgressionMode: ProgressionMode,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenExercise: () -> Unit,
    /** The last planned-set write's answer, so the editor closes on success only (B284). */
    setSaveOutcome: WriteOutcome<String>?,
    onDismissSetSave: () -> Unit,
    onAddSet: (TemplateSetEdit) -> Unit,
    onUpdateSet: (String, TemplateSetEdit) -> Unit,
    onRemoveSet: (String) -> Unit,
    onSavePlan: (Int?, String?, Int?) -> Unit,
    onAddSets: (Int, TemplateSetEdit) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    /** Adds a ramp rounded to the unit's step (ROADMAP N64), or null when there is none. */
    onAddWarmUpSets: ((Long) -> Unit)? = null,
    supersetLabels: Map<String, String> = emptyMap(),
) {
    // This exercise's display unit, resolved once: every load it names reads in it (ROADMAP N64).
    val unit = exerciseWeightUnit(exercise.weightUnit)
    // The dialog that is open: a set's id, [ADDING_SET_ID] for a new one, or null for none — one saveable value
    // rather than two flags that could disagree. It closes on the write's answer, not on the tap (B284): a
    // refusal — a rung boundary, a load the plan refuses — keeps the dialog and the values the lifter typed.
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val edited = exercise.sets.firstOrNull { it.id == open }
    val dismissSave by rememberUpdatedState(onDismissSetSave)
    LaunchedEffect(setSaveOutcome) {
        if (open != null && setSaveOutcome == WriteOutcome.Written(open)) {
            open = null
            dismissSave()
        }
    }

    Column(modifier = modifier) {
        TemplateExerciseRow(
            exercise = exercise,
            position = position,
            isFirst = isFirst,
            isLast = isLast,
            isExpanded = isExpanded,
            onToggle = onToggle,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove,
            onOpenExercise = onOpenExercise,
            onToggleSuperset = onToggleSuperset,
            addWarmUps = warmUpActionFor(exercise, unit, onAddWarmUpSets),
            supersetLabels = supersetLabels,
        )
        // Everything below the row folds together (N91): the sets, the control that adds one, and the
        // plan's own fields.
        if (isExpanded) {
            PlannedSets(
                exerciseId = exercise.id,
                sets = exercise.sets,
                unit = unit,
                onEdit = { open = it.id },
                onRemove = onRemoveSet,
            )
            // The plan's quick entry sits with the sets it extends (P3.18): a block that is one shape
            // repeated is one row here, and the per-set dialog below stays for the top set and its back-offs.
            QuickAddSets(
                exerciseId = exercise.id,
                lastSet = exercise.sets.lastOrNull(),
                unit = unit,
                onAdd = onAddSets,
            )
            AddSetButton(exerciseId = exercise.id, onClick = { open = ADDING_SET_ID })
            ExercisePlanFields(exercise, appProgressionMode, onSavePlan)
        }
    }

    if (open != null) {
        TemplateSetEditor(
            exercise = exercise,
            edited = edited,
            unit = unit,
            onDismiss = { open = null },
            onSave = { edit ->
                // No close here (B284): the block's effect above closes on a written set.
                if (edited == null) onAddSet(edit) else onUpdateSet(edited.id, edit)
            },
        )
    }
}

/**
 * The block's foot: the action that adds another planned set (N81).
 *
 * Split out when the second row of fields and the button pushed [TemplateExerciseBlock] past the length
 * this project allows, and it is the piece that reads on its own — the label, the tag, and the shape,
 * which is the workout's *Log set* (N59): full width, and under the values it does not write.
 */
@Composable
internal fun AddSetButton(exerciseId: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TestTags.templatePlanAdd(exerciseId)),
    ) {
        Text(stringResource(R.string.template_plan_add))
    }
}

/**
 * The dialog that adds or edits one planned set (ROADMAP N14, N46).
 *
 * Split out of the block when N64's unit pushed it over the length this project allows, and it is
 * the piece that reads on its own: what a new set starts from, and what a save reports.
 */
@Composable
internal fun TemplateSetEditor(
    exercise: TemplateExercise,
    edited: TemplateSet?,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (TemplateSetEdit) -> Unit,
) {
    // The position this row holds, or would hold when it is being added (which appends), because the
    // rung question is about a place in the plan rather than about a role on its own (ROADMAP N79, B64).
    val position = edited
        ?.let { set -> exercise.sets.indexOfFirst { it.id == set.id } }
        ?: exercise.sets.size

    // Whether this position **opens** the run it would be part of, which is the only place a run's value is
    // written (ROADMAP N79, B120, B121). Neither "is a drop allowed here", which is true of every rung
    // (B120), nor "is this row being added", which says nothing about a row that already exists and is being
    // changed into one (B121).
    //
    // A row that is already a rung is answered by `runAt`. One that is not — an ordinary set being turned
    // into a drop, or a row that does not exist yet — opens its run exactly when the row above is not a drop
    // rung, because a drop placed under one continues that run and inherits its value rather than naming a
    // second.
    val above = exercise.sets.getOrNull(position - 1)
    val opensItsRun = exercise.sets.runAt(position)?.rung == 1 ||
        (exercise.sets.getOrNull(position)?.role?.isRung != true && above?.role != SetType.DROP)

    TemplateSetDialog(
        unit = unit,
        // The step a target's ± pair takes is this exercise's own, exactly as its ramp's is
        // (ROADMAP N77, N111), so the plan is authored in the numbers the machine actually jumps.
        stepGrams = exercise.stepGrams,
        // The plan answers whether a rung may be chosen here, by the same rule the write boundary
        // holds: a run is contiguous, so its anchor is the set above the run's first row and that set
        // has to stand on its own — never a warm-up and never another kind of rung.
        offers = { role -> exercise.sets.isAnchoredAt(position, role) },
        holdsTheRunValue = opensItsRun,
        // Add set starts from the last planned set rather than from nothing (ROADMAP N46): a set is
        // nearly always the one before it again, and Duplicate — which doubled the whole plan and
        // left the odd counts to manual adds — is gone. Blank only while there is no set to start
        // from, and the role prefills too, which is safe because Add warm-ups *prepends*: the last
        // set is the last working set.
        initial = edited?.let { it.toEdit() }
            ?: exercise.sets.lastOrNull()?.toEdit()
            ?: TemplateSetEdit(),
        isNew = edited == null,
        saveKey = edited?.id ?: ADDING_SET_ID,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

/**
 * The plan's sets, as lines in the block (ROADMAP N14, N81).
 *
 * This was a dialog behind *Planned sets · 3*: the count without the plan, and every load, role and
 * rung behind a tap. The lines are the screen's own content now, and each is still the way into that
 * set's targets and the place its delete lives. An exercise with no sets keeps the sentence the dialog
 * carried, because "add your first one" is still the next thing to do and the button below says how.
 */
@Composable
internal fun PlannedSets(
    /** This block's exercise, so the empty note is tagged per row rather than once for the screen (B76). */
    exerciseId: String,
    sets: List<TemplateSet>,
    unit: WeightUnit,
    onEdit: (TemplateSet) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (sets.isEmpty()) {
            Text(
                text = stringResource(R.string.template_plan_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag(TestTags.templatePlanEmpty(exerciseId)),
            )
        }
        // Numbered by the rule every list of sets numbers by, so the plan's ramp does not push its
        // working sets to 5, 6, 7 (N112).
        val numbers = sets.setNumbers { it.role }
        sets.forEachIndexed { index, set ->
            PlanSetRow(
                number = numbers[index],
                set = set,
                unit = unit,
                // A rung has no weight written down, so the row states what it derives from its anchor
                // rather than leaving the reader to do the arithmetic (N79).
                rungLoad = sets.rungLoadAt(index),
                holdsTheRunValue = sets.runAt(index)?.rung == 1,
                onEdit = { onEdit(set) },
                onDelete = { onRemove(set.id) },
            )
        }
    }
}

/**
 * One planned set as a line: its number and role, what it loads and asks for, and its delete
 * (N14, N79, N112).
 *
 * Moved out of the plan dialog and into the block by N81, unchanged but for the padding: the dialog
 * supplied it, the block does now. Its two tags are per set, because a plan of a working set and two
 * rungs is three rows on one screen. A **warm-up has no number** (N112), so its line reads its role
 * alone — and its delete names the role rather than a number nobody can see.
 */
@Composable
private fun PlanSetRow(
    /** The set's place among the sets that stand on their own, or null for a warm-up (N112). */
    number: Int?,
    set: TemplateSet,
    unit: WeightUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** What a rung loads, derived from its anchor, or null (ROADMAP N79). */
    rungLoad: Load? = null,
    /** True on the rung that holds the run's value, which is the one worth naming it on (N79). */
    holdsTheRunValue: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.templatePlanSet(set.id))
            .clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // A warm-up reads its role alone, because it has no number to lead with (N112).
                text = number?.let { stringResource(R.string.template_plan_set, it, set.role.label) }
                    ?: stringResource(R.string.template_plan_set_unnumbered, set.role.label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = set.summary(unit, rungLoad, holdsTheRunValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(TestTags.templatePlanRemove(set.id)),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                // Names the row the way the row reads: by number, or by role where it has none (N112).
                contentDescription = number?.let {
                    stringResource(R.string.template_plan_remove, it)
                } ?: stringResource(R.string.template_plan_remove_unnumbered, set.role.label),
            )
        }
    }
}

/**
 * One planned set as the editor's write takes it (ROADMAP N14, N79).
 *
 * Every field the row can carry has to travel, because `updateSet` overwrites the planned set whole:
 * dropping one here silently erases what the plan stored. `dropValueGrams` is the one that bit — the
 * dialog is the only place a run's value is authored, so a row reopened without it showed an empty
 * field, disabled Save (B61), and read as a number the app had lost.
 */
private fun TemplateSet.toEdit() = TemplateSetEdit(
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    dropValueGrams = dropValueGrams,
    note = note,
)
