package com.example.androidapp.ui.templates

import androidx.annotation.StringRes
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
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.ProgressionRule
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.progressionRuleFor
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.ui.components.exerciseWeightUnit

/**
 * What this plan will do with the step it earns, in the block it belongs to (P3.17, B181).
 *
 * One line, in the movement's own unit, and never a claim the prompt does not keep: the words come from
 * [ProgressionRule], and the weight is [Weight.stepGramsFor] — the movement's step, not a second default.
 *
 * **The rule itself lives in the model now** (N227): the workout screen states the same fact for the same plan,
 * so [progressionRuleFor] moved to the model where both screens read one implementation rather than the
 * duplicate predicate this file used to carry. What is left here is the plan editor's wording, which is the
 * long form; the session's is a clause in the block's small print.
 */
@Composable
internal fun ProgressionStepNotice(
    exercise: TemplateExercise,
    /** The app's own answer to *Done*, which [exercise]'s movement may override (ROADMAP N235). */
    appProgressionMode: ProgressionMode,
    modifier: Modifier = Modifier,
) {
    val unit = exerciseWeightUnit(exercise.weightUnit)
    val step = Weight.format(Weight.stepGramsFor(exercise.stepGrams, unit), unit)
    val unitLabel = stringResource(
        if (unit == WeightUnit.KILOGRAMS) R.string.suggestion_unit_kg else R.string.suggestion_unit_lb,
    )
    val text = when (progressionRuleFor(exercise.targetRpeHalves, exercise.progressionSets())) {
        ProgressionRule.NO_TARGET_RPE -> stringResource(R.string.plan_progression_no_gate)
        ProgressionRule.NO_REPS -> stringResource(R.string.plan_progression_no_reps)
        ProgressionRule.REPS_THEN_LOAD ->
            stringResource(R.string.plan_progression_reps_then_load, step, unitLabel)

        ProgressionRule.REPS_ONLY -> stringResource(R.string.plan_progression_reps_only)
        ProgressionRule.LOAD_ONLY -> stringResource(R.string.plan_progression_step, step, unitLabel)
        ProgressionRule.NOTHING_TO_MOVE -> stringResource(R.string.plan_progression_nothing_to_move)
    }
    // **The movement's own answer wins, and the sentence has to say so** (N235): otherwise it promises a
    // question that Done will not ask. The clause appears only where it changes what happens — a movement
    // that agrees with the setting needs no second sentence.
    val own = exercise.progressionMode
    val clause = own?.takeIf { it != appProgressionMode }?.let {
        stringResource(R.string.plan_progression_movement_answer, stringResource(it.answerLabel()))
    }
    Text(
        text = if (clause == null) text else "$text $clause",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}

/** What one answer is called in the plan's own sentence (ROADMAP N235). */
@StringRes
private fun ProgressionMode.answerLabel(): Int = when (this) {
    ProgressionMode.SUGGEST -> R.string.settings_progression_suggest
    ProgressionMode.APPLY_AT_DONE -> R.string.settings_progression_apply
    ProgressionMode.OFF -> R.string.settings_progression_off
}
