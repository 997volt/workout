package com.example.androidapp.ui.templates

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.ui.components.exerciseWeightUnit

/**
 * What this plan will do with the step it earns (P3.17, corrected by B181).
 *
 * The app does not choose the step: it is the movement's own (N77, N111), authored in the exercise's editor,
 * and the gate is the plan's target RPE (N50). Both were real rules the plan never stated, so a lifter could
 * not tell why *Done* would or would not ask.
 *
 * **The rule is not the weight.** N74 moves a set's reps while it is below its range's ceiling and the weight
 * only at the ceiling, so a 5–8 plan progresses by one rep per answered set long before it adds 2.5 kg. The
 * sentence states that direction, and which half is missing when the plan cannot progress at all — the
 * version before B181 printed the weight step as though it were the only one.
 *
 * Its own file because the editor is at the number of functions this project allows per file; extracted at
 * that point rather than folded into a function it does not belong to.
 */
internal enum class ProgressionRule {
    /** The plan names no target RPE, so the gate never opens (N50). */
    NO_TARGET_RPE,

    /** The plan names no reps, so there is nothing to measure the work against or to raise (N74). */
    NO_REPS,

    /** Below the ceiling the reps move; at it, the movement's own weight step (N74). */
    REPS_THEN_LOAD,

    /** Reps below the ceiling, and nothing to raise once the ceiling is reached (N74). */
    REPS_ONLY,

    /** A fixed rep target, so the weight is what moves (N74). */
    LOAD_ONLY,

    /** A fixed rep target and no weight: answered work has nowhere to go (N74's TOPPED_OUT). */
    NOTHING_TO_MOVE,
}

/**
 * Which sentence the block's foot can truthfully print for [exercise] (B181).
 *
 * Pure, so each shape is covered by fast JVM tests rather than by reading the screen: the gate, the
 * missing reps, the range that gives the reps room, and the fixed target where the weight is the only step.
 */
internal fun progressionRuleFor(exercise: TemplateExercise): ProgressionRule {
    // Warm-ups are not what a target is measured against (N17, N20, N22), so they cannot decide the sentence.
    val work = exercise.sets.filterNot { it.role == SetType.WARMUP }
    val namesReps = work.any {
        it.targetRepsMin != null || it.targetRepsMax != null || it.targetRepsCurrent != null
    }
    val namesLoad = work.any { it.namesAddedLoad() }
    return when {
        exercise.targetRpeHalves == null -> ProgressionRule.NO_TARGET_RPE
        !namesReps -> ProgressionRule.NO_REPS
        work.any { it.restartsAtFloor } -> if (namesLoad) {
            ProgressionRule.REPS_THEN_LOAD
        } else {
            ProgressionRule.REPS_ONLY
        }

        namesLoad -> ProgressionRule.LOAD_ONLY
        else -> ProgressionRule.NOTHING_TO_MOVE
    }
}

/**
 * The **added** weight this set names, or null (N15).
 *
 * The same rule as `ProgressionPlanSet.addedWeightGrams`: an assisted set stores its number as help with
 * `targetWeightGrams = 0`, and a bodyweight set's `0` is the same absence, so a non-null column alone is not
 * a load a step can raise. Kept here as its own predicate so the sentence and the offer cannot drift.
 */
private fun TemplateSet.namesAddedLoad(): Boolean =
    targetWeightGrams != null && targetWeightGrams > 0L && targetAssistanceGrams == null

/** True where the plan wrote a real range, which is what gives the rep step room (N74). */
private val TemplateSet.restartsAtFloor: Boolean
    get() = targetRepsMin != null && targetRepsMax != null && targetRepsMin < targetRepsMax

/**
 * What this plan will do with the step it earns, in the block it belongs to (P3.17, B181).
 *
 * One line, in the movement's own unit, and never a claim the prompt does not keep: the words come from
 * [ProgressionRule], and the weight is [Weight.stepGramsFor] — the movement's step, not a second default.
 */
@Composable
internal fun ProgressionStepNotice(exercise: TemplateExercise, modifier: Modifier = Modifier) {
    val unit = exerciseWeightUnit(exercise.weightUnit)
    val step = Weight.format(Weight.stepGramsFor(exercise.stepGrams, unit), unit)
    val unitLabel = stringResource(
        if (unit == WeightUnit.KILOGRAMS) R.string.suggestion_unit_kg else R.string.suggestion_unit_lb,
    )
    val text = when (progressionRuleFor(exercise)) {
        ProgressionRule.NO_TARGET_RPE -> stringResource(R.string.plan_progression_no_gate)
        ProgressionRule.NO_REPS -> stringResource(R.string.plan_progression_no_reps)
        ProgressionRule.REPS_THEN_LOAD ->
            stringResource(R.string.plan_progression_reps_then_load, step, unitLabel)

        ProgressionRule.REPS_ONLY -> stringResource(R.string.plan_progression_reps_only)
        ProgressionRule.LOAD_ONLY -> stringResource(R.string.plan_progression_step, step, unitLabel)
        ProgressionRule.NOTHING_TO_MOVE -> stringResource(R.string.plan_progression_nothing_to_move)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}
