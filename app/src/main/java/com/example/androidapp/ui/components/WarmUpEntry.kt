package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.warmUpRampFor

/**
 * The row's *Add warm-ups* entry, or null where no ramp can be built (ROADMAP N28, B50, N81).
 *
 * The entry is offered only where the action would actually write something — the predicate the action
 * itself reads: asking whether a weight was merely *typed* offered the button for an assisted set (0 kg)
 * and for one too light to load, and a press then reported success while writing nothing. A ramp's step is
 * the unit's (N64), so it is resolved here, once, and travels with the entry.
 *
 * Split out of the block, which is at the length this project allows.
 */
@Composable
fun warmUpActionFor(
    exercise: TemplateExercise,
    unit: WeightUnit,
    onAddWarmUpSets: ((Long) -> Unit)?,
): WarmUpAction? {
    val step = Weight.stepGramsFor(exercise.stepGrams, unit)
    if (onAddWarmUpSets == null || warmUpRampFor(exercise.sets, step).isEmpty()) return null
    return WarmUpAction(
        label = stringResource(R.string.template_add_warmups),
        tag = TestTags.templateAddWarmUps(exercise.id),
        onClick = { onAddWarmUpSets(step) },
    )
}

