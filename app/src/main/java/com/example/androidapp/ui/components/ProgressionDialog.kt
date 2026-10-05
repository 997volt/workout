package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.ProgressionPrompt

/**
 * The next step a plan earned, as the lifter's decision (ROADMAP N50).
 *
 * *Done* opens this where a plan can answer it. The dialog states what the plan asked and what was
 * done, and — when the session earned one — offers the next step as a choice: **a load** (the smallest
 * loadable step) **or a rep**, with *Not now* equally available.
 *
 * The rating is not here (N8): *How did that feel?* belongs to the exercise's own row, opened when the
 * lifter reaches for it, so leaving the exercise never asks for one.
 *
 * It is deliberately **not** a gate. *Not now* and a tap outside it both mean "suggest nothing", and
 * doing neither is as reachable as doing either: the app states a step it read out of the plan and the
 * log, and the lifter decides whether to take it. Nothing here writes anything — accepting one
 * direction reaches the ViewModel, which writes the plan and then finishes the exercise.
 *
 * Every control is addressed by a test tag rather than by its English wording, and each label names
 * what the control does — the load it would set, or the rep target it would raise — so a screen reader
 * hears the decision rather than a bare "increase".
 */
@Composable
fun ProgressionDialog(
    exerciseName: String,
    prompt: ProgressionPrompt,
    onAccept: (ProgressionDirection) -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        // Dismissing declines rather than choosing: a question that picks for the lifter when they tap
        // away is not a question — the shape the skip prompt already uses.
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.progression_title, exerciseName)) },
        text = { ProgressionBody(prompt = prompt, onAccept = onAccept) },
        // Nothing to confirm: the two steps are offered in the body, beside what they would change, and
        // *Not now* is the only thing left for the button row to say.
        confirmButton = {},
        dismissButton = {
            AppTextButton(
                onClick = onNotNow,
                modifier = Modifier.testTag(TestTags.PROGRESSION_NOT_NOW),
            ) {
                Text(stringResource(R.string.progression_not_now))
            }
        },
    )
}

/**
 * What the prompt states, and the steps it offers: its own composable because the dialog around it is
 * at the length this project allows, and because the two together are the question.
 */
@Composable
private fun ProgressionBody(
    prompt: ProgressionPrompt,
    onAccept: (ProgressionDirection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = prompt.planned?.let { planned ->
                val line = setLine(
                    reps = planned.targetReps,
                    weightGrams = planned.targetWeightGrams,
                    assistanceGrams = planned.targetAssistanceGrams,
                    rpeHalves = planned.targetRpeHalves,
                )
                stringResource(R.string.progression_plan, line)
            } ?: stringResource(R.string.progression_no_plan),
            modifier = Modifier.testTag(TestTags.PROGRESSION_PLAN),
        )
        prompt.performed?.let { done ->
            Text(
                text = stringResource(
                    R.string.progression_done,
                    setLine(done.reps, done.weightGrams, done.assistanceGrams, done.rpeHalves),
                ),
                modifier = Modifier.testTag(TestTags.PROGRESSION_DONE),
            )
        }
        prompt.offer?.let { offer ->
            Text(stringResource(R.string.progression_offer))
            // The two directions are the same control in two shapes, so neither reads as the app's
            // preferred answer; a plan that names no load simply has nothing to raise.
            offer.load?.let { step ->
                FilledTonalButton(
                    onClick = { onAccept(ProgressionDirection.LOAD) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.PROGRESSION_LOAD),
                ) {
                    Text(stringResource(R.string.progression_increase_load, Weight.kilograms(step.to)))
                }
            }
            FilledTonalButton(
                onClick = { onAccept(ProgressionDirection.REPS) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.PROGRESSION_REPS),
            ) {
                val raised = offer.reps.to
                Text(pluralStringResource(R.plurals.progression_increase_reps, raised, raised))
            }
        }
    }
}

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
): String {
    val repsPart = reps?.let { pluralStringResource(R.plurals.progression_reps_value, it, it) }
    val weightPart = weightGrams?.let {
        stringResource(R.string.progression_weight_value, Weight.display(it, assistanceGrams ?: 0L))
    }
    val rpePart = rpeHalves?.let { rpeMarker(it) }
    return listOfNotNull(repsPart, weightPart, rpePart).joinToString(" · ")
}
