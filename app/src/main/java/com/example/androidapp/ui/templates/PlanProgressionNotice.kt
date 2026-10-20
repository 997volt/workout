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
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.ui.components.exerciseWeightUnit

/**
 * What this plan will do with the step it earns (P3.17).
 *
 * The app does not choose the step: it is the movement's own (N77, N111), authored in the exercise's editor,
 * and the gate is the plan's target RPE (N50). Both were real rules the plan never stated, so a lifter could
 * not tell why *Done* would or would not ask. The line says the rule in the block it belongs to, and names the
 * missing half when the plan cannot progress at all.
 *
 * Its own file because the editor is at the number of functions this project allows per file; extracted at
 * that point rather than folded into a function it does not belong to.
 */
@Composable
internal fun ProgressionStepNotice(exercise: TemplateExercise, modifier: Modifier = Modifier) {
    val unit = exerciseWeightUnit(exercise.weightUnit)
    val step = Weight.stepGramsFor(exercise.stepGrams, unit)
    val unitLabel = stringResource(
        if (unit == WeightUnit.KILOGRAMS) R.string.suggestion_unit_kg else R.string.suggestion_unit_lb,
    )
    val text = when {
        exercise.targetRpeHalves == null -> stringResource(R.string.plan_progression_no_gate)
        exercise.sets.none { it.targetWeightGrams != null } ->
            stringResource(R.string.plan_progression_no_load)

        else -> stringResource(R.string.plan_progression_step, Weight.format(step, unit), unitLabel)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}
