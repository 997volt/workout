package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.androidapp.domain.model.PendingProgression
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionMiss
import com.example.androidapp.domain.model.ProgressionPlanSet

/**
 * The next step each planned set earned, as the lifter's decision (ROADMAP N50, N74).
 *
 * *Done* opens this where a plan can answer it, with **one row per working set**: what the plan
 * asked, what was done, and — where there is one — the step that set earned on its own. A set below
 * its rep range's ceiling offers the **rep**, a set at the ceiling offers the **weight** that starts
 * the range again, and a set that earned nothing says why rather than standing empty.
 *
 * **Picking is not writing.** A tap selects a step — drawn as the value it would set — and only
 * *Done* writes them all and closes the exercise; *Not now*, a tap outside and the back gesture all
 * mean "write nothing", which is the shape the skip prompt already uses. That is what makes the
 * question answerable per set: the lifter reads the whole answer before taking it, and a step written
 * the moment it was tapped would re-arm the very offer it just answered.
 *
 * The rating is not here (N8): *How did that feel?* belongs to the exercise's own row, opened when the
 * lifter reaches for it, so leaving the exercise never asks for one.
 *
 * Every control is addressed by a test tag rather than by its English wording, and each chip's label
 * names what accepting it does — the load it would set and the reps it would restart at, or the rep
 * target it would raise — so a screen reader hears the decision rather than a bare "increase". A
 * chip's selected state is what says which step is picked.
 */
@Composable
fun ProgressionDialog(
    progression: PendingProgression,
    onToggle: (String, ProgressionDirection) -> Unit,
    onSelectAll: (ProgressionDirection) -> Unit,
    onConfirm: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        // Dismissing declines rather than choosing: a question that picks for the lifter when they tap
        // away is not a question.
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.progression_title, progression.exerciseName)) },
        text = {
            ProgressionBody(progression = progression, onToggle = onToggle, onSelectAll = onSelectAll)
        },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Progression.CONFIRM),
            ) {
                Text(stringResource(R.string.progression_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onNotNow,
                modifier = Modifier.testTag(TestTags.Progression.NOT_NOW),
            ) {
                Text(stringResource(R.string.progression_not_now))
            }
        },
    )
}

/**
 * Every working set, and the bulk choices above them.
 *
 * Scrollable because a plan may prescribe more sets than fit a dialog, and a set a lifter cannot
 * reach is a set the question does not ask about.
 */
@Composable
private fun ProgressionBody(
    progression: PendingProgression,
    onToggle: (String, ProgressionDirection) -> Unit,
    onSelectAll: (ProgressionDirection) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BulkChoices(progression = progression, onSelectAll = onSelectAll)
        progression.sets.forEachIndexed { index, set ->
            SetRow(
                number = index + 1,
                total = progression.sets.size,
                set = set,
                unit = progression.unit,
                onToggle = onToggle,
            )
        }
    }
}

/**
 * The plan-wide choices, offered only where the same step fits more than one set (N74).
 *
 * A plan whose sets share a target would otherwise ask for the same tap three times, so each
 * direction gets one control that picks it wherever it is offered — and only there: a set below its
 * ceiling has no weight step to take, and a bodyweight set no load at all.
 */
@Composable
private fun BulkChoices(
    progression: PendingProgression,
    onSelectAll: (ProgressionDirection) -> Unit,
) {
    val loads = progression.offering(ProgressionDirection.LOAD)
    val reps = progression.offering(ProgressionDirection.REPS)
    if (loads < 2 && reps < 2) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (loads >= 2) {
            AppTextButton(
                onClick = { onSelectAll(ProgressionDirection.LOAD) },
                modifier = Modifier.testTag(TestTags.Progression.LOAD_ALL),
            ) {
                Text(pluralStringResource(R.plurals.progression_all_load, loads, loads))
            }
        }
        if (reps >= 2) {
            AppTextButton(
                onClick = { onSelectAll(ProgressionDirection.REPS) },
                modifier = Modifier.testTag(TestTags.Progression.REPS_ALL),
            ) {
                Text(pluralStringResource(R.plurals.progression_all_reps, reps, reps))
            }
        }
    }
}

/**
 * One working set: its number, the plan, what was done, and the step it earned or the reason it
 * earned none (N74).
 *
 * The number is here because the old prompt named no set at all: it stated one plan line and one done
 * line and left the lifter to guess which of their three sets it was talking about.
 */
@Composable
private fun SetRow(
    number: Int,
    total: Int,
    set: PendingProgression.PendingSet,
    unit: WeightUnit,
    onToggle: (String, ProgressionDirection) -> Unit,
) {
    val planned = set.prompt.planned
    val planId = set.planId
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(TestTags.Progression.set(planId ?: "extra-$number")),
    ) {
        Text(
            text = stringResource(R.string.progression_set, number, total),
            style = MaterialTheme.typography.titleSmall,
        )
        planned?.let {
            Text(
                text = stringResource(R.string.progression_plan, plannedLine(it, unit)),
                modifier = Modifier.testTag(TestTags.Progression.plan(it.setId)),
            )
        }
        set.prompt.performed?.let { done ->
            Text(
                text = stringResource(
                    R.string.progression_done,
                    setLine(done.reps, done.weightGrams, done.assistanceGrams, done.rpeHalves, unit),
                ),
                modifier = planId?.let { Modifier.testTag(TestTags.Progression.done(it)) } ?: Modifier,
            )
        }
        when {
            set.applied -> Text(stringResource(R.string.progression_applied))
            set.prompt.offer != null && planId != null ->
                ChoiceChips(set = set, planId = planId, unit = unit, onToggle = onToggle)

            else -> set.prompt.miss?.let { miss -> Text(missLine(set, miss)) }
        }
    }
}

/** The two directions one set can offer, each drawn as the value it would write (N74). */
@Composable
private fun ChoiceChips(
    set: PendingProgression.PendingSet,
    planId: String,
    unit: WeightUnit,
    onToggle: (String, ProgressionDirection) -> Unit,
) {
    val offer = set.prompt.offer ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        offer.reps?.let { step ->
            AppFilterChip(
                selected = set.direction == ProgressionDirection.REPS,
                onClick = { onToggle(planId, ProgressionDirection.REPS) },
                label = pluralStringResource(R.plurals.progression_choose_reps, step.to, step.to),
                testTag = TestTags.Progression.reps(planId),
            )
        }
        offer.load?.let { step ->
            val restart = set.prompt.planned?.takeIf { it.restartsAtFloor }?.targetRepsMin
            val weight = Weight.format(step.to, unit)
            AppFilterChip(
                selected = set.direction == ProgressionDirection.LOAD,
                onClick = { onToggle(planId, ProgressionDirection.LOAD) },
                label = if (restart == null) {
                    stringResource(R.string.progression_choose_load, weight, unit.label())
                } else {
                    pluralStringResource(
                        R.plurals.progression_choose_load_restart,
                        restart,
                        weight,
                        unit.label(),
                        restart,
                    )
                },
                testTag = TestTags.Progression.load(planId),
            )
        }
    }
}

/**
 * Why a set earned nothing, as a sentence a lifter can act on (N74).
 *
 * The alternatives are stated with the number the app actually measured — how many reps short, which
 * effort the set went over, where the range tops out — because "no step" on its own reads as a bug
 * rather than as an answer.
 */
@Composable
private fun missLine(set: PendingProgression.PendingSet, miss: ProgressionMiss): String {
    val planned = set.prompt.planned
    return when (miss) {
        ProgressionMiss.NOT_DONE -> stringResource(R.string.progression_miss_not_done)
        ProgressionMiss.NOT_IN_PLAN -> stringResource(R.string.progression_miss_not_in_plan)
        ProgressionMiss.NO_REP_TARGET -> stringResource(R.string.progression_miss_no_reps)
        ProgressionMiss.NO_TARGET_RPE -> stringResource(R.string.progression_miss_no_rpe)
        ProgressionMiss.UNRATED -> stringResource(R.string.progression_miss_unrated)
        ProgressionMiss.REPS_SHORT -> {
            val asked = planned?.targetReps ?: 0
            val short = (asked - (set.prompt.performed?.reps ?: 0)).coerceAtLeast(1)
            pluralStringResource(R.plurals.progression_miss_reps_short, short, short)
        }

        ProgressionMiss.OVER_TARGET_RPE ->
            stringResource(R.string.progression_miss_over_rpe, rpeMarker(planned?.targetRpeHalves ?: 0))

        ProgressionMiss.TOPPED_OUT -> {
            val top = planned?.targetRepsMax ?: 0
            pluralStringResource(R.plurals.progression_miss_topped_out, top, top)
        }
    }
}

/** One planned set as a line: "5 reps · 100 kg · RPE 8", with whichever parts the plan wrote. */
@Composable
private fun plannedLine(planned: ProgressionPlanSet, unit: WeightUnit): String = setLine(
    reps = planned.targetReps,
    weightGrams = planned.targetWeightGrams,
    assistanceGrams = planned.targetAssistanceGrams,
    rpeHalves = planned.targetRpeHalves,
    unit = unit,
)

/**
 * One set as a line: "5 reps · 100 kg · RPE 8", with whichever parts are there.
 *
 * Parts rather than a fixed sentence because the plan and the session can each be missing something —
 * a bodyweight movement names no load, an unrated session no RPE — and a placeholder would read as a
 * value nobody entered.
 */
@Composable
private fun setLine(
    reps: Int?,
    weightGrams: Long?,
    assistanceGrams: Long?,
    rpeHalves: Int?,
    unit: WeightUnit,
): String {
    val repsPart = reps?.let { pluralStringResource(R.plurals.progression_reps_value, it, it) }
    val weightPart = weightGrams?.let {
        stringResource(
            R.string.progression_weight_value,
            Weight.display(it, assistanceGrams ?: 0L, unit),
            unit.label(),
        )
    }
    val rpePart = rpeHalves?.let { rpeMarker(it) }
    return listOfNotNull(repsPart, weightPart, rpePart).joinToString(" · ")
}
