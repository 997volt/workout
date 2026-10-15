package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.androidapp.R
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.AppTextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.example.androidapp.domain.model.MAINTENANCE_CORRIDOR_GRAMS
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.model.kind
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.ui.components.AppFilterChip

/**
 * The target line, and the one place a target is set (ROADMAP N39).
 *
 * Beside the average and the trend because it is the same kind of thing — a line about the series rather than
 * a reading in it — and editable here because a target nobody can set is a decoration. The value is typed in
 * the metric's displayed unit and parsed into its stored one, so a target of 80 kilograms is 80000 grams like
 * every other weight in the app.
 */
@Composable
fun GoalRow(
    goal: Double?,
    metric: MetricEntry,
    onSetGoal: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // `editing` is the only state here: the field's text belongs to the dialog, which is the thing that has
    // to validate it. A copy on this row would be a second answer to "what was typed", and the one that is
    // never read is the one that drifts.
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Statistics.GOAL),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.statistics_goal),
            style = MaterialTheme.typography.bodyMedium,
        )
        AppTextButton(
            onClick = { editing = true },
            modifier = Modifier.testTag(TestTags.Statistics.GOAL_SET),
        ) {
            Text(
                text = goal?.let { metric.unit.format(it) }
                    ?: stringResource(R.string.statistics_goal_none),
            )
        }
    }

    if (editing) {
        GoalDialog(
            metric = metric,
            goal = goal,
            onSet = { value ->
                onSetGoal(value)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

/**
 * The **weight** goal, and the one place its kind is set (ROADMAP N135).
 *
 * The number alone used to be the whole goal, with the rate typed beside it — two answers to one question,
 * which could disagree. Here the lifter states what they want and the rate follows: a weight to reach, a
 * way to go with no weight invented for it, or a weight to hold within the app's corridor. So the row says
 * the kind as well as the number, because the kind is half of what was chosen.
 */
@Composable
fun WeightGoalRow(
    goal: WeightGoal?,
    metric: MetricEntry,
    onSetGoal: (WeightGoal?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Statistics.WEIGHT_GOAL),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.statistics_weight_goal),
            style = MaterialTheme.typography.bodyMedium,
        )
        AppTextButton(
            onClick = { editing = true },
            modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_SET),
        ) {
            Text(text = weightGoalText(goal, metric))
        }
    }

    if (editing) {
        WeightGoalDialog(
            goal = goal,
            metric = metric,
            onSet = { value ->
                onSetGoal(value)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

/** What the goal reads as: its kind, and the weight only where it states one. */
@Composable
private fun weightGoalText(goal: WeightGoal?, metric: MetricEntry): String = when (goal) {
    null -> stringResource(R.string.statistics_weight_goal_none)
    is WeightGoal.Target -> metric.unit.format(goal.weightGrams.toDouble())
    is WeightGoal.Direction -> stringResource(
        if (goal.gain) R.string.statistics_weight_goal_gaining else R.string.statistics_weight_goal_losing,
    )
    // A corridor, written as the weight and the corridor either way — never as a rate of zero, which the
    // first glass of water would break (N135).
    is WeightGoal.Maintain -> stringResource(
        R.string.statistics_weight_goal_corridor,
        metric.unit.format(goal.weightGrams.toDouble()),
        metric.unit.format(MAINTENANCE_CORRIDOR_GRAMS.toDouble()),
    )
}

/**
 * The dialog the weight goal is chosen in (ROADMAP N135).
 *
 * The kind first, because it is the question; the weight only where the kind asks for one. Confirming with
 * a value kind whose field cannot be read sets nothing rather than a zero — the rule the target dialog
 * follows, so the mistake stays visible in the field instead of becoming a goal.
 */
@Composable
private fun WeightGoalDialog(
    goal: WeightGoal?,
    metric: MetricEntry,
    onSet: (WeightGoal?) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(goal?.kind()) }
    var typed by rememberSaveable {
        mutableStateOf(goal?.statedWeightGrams()?.let { metric.unit.format(it.toDouble()) }.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.statistics_weight_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightGoalKind.entries.forEach { option ->
                    AppFilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = stringResource(weightGoalKindLabel(option)),
                        testTag = TestTags.Statistics.weightGoalKind(option),
                    )
                }
                if (kind == WeightGoalKind.TARGET || kind == WeightGoalKind.MAINTAIN) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        suffix = { Text(stringResource(metric.unit.labelRes)) },
                        modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_FIELD),
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { weightGoalFor(kind, typed, metric)?.let(onSet) },
                modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM),
            ) {
                Text(stringResource(R.string.statistics_goal_confirm))
            }
        },
        dismissButton = {
            if (goal != null) {
                AppTextButton(
                    onClick = { onSet(null) },
                    modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_CLEAR),
                ) {
                    Text(stringResource(R.string.statistics_goal_clear))
                }
            }
        },
    )
}

/**
 * The goal [kind] and [typed] state, or null when they state nothing usable.
 *
 * A direction kind ignores the field on purpose: it states no weight, and a value left in the box from an
 * earlier target must not become one.
 */
private fun weightGoalFor(kind: WeightGoalKind?, typed: String, metric: MetricEntry): WeightGoal? = when (kind) {
    null -> null
    WeightGoalKind.LOSE -> WeightGoal.Direction(gain = false)
    WeightGoalKind.GAIN -> WeightGoal.Direction(gain = true)
    WeightGoalKind.TARGET -> metric.unit.parse(typed)?.let { WeightGoal.Target(it.toLong()) }
    WeightGoalKind.MAINTAIN -> metric.unit.parse(typed)?.let { WeightGoal.Maintain(it.toLong()) }
}

private fun weightGoalKindLabel(kind: WeightGoalKind): Int = when (kind) {
    WeightGoalKind.TARGET -> R.string.statistics_weight_goal_target
    WeightGoalKind.LOSE -> R.string.statistics_weight_goal_lose
    WeightGoalKind.GAIN -> R.string.statistics_weight_goal_gain
    WeightGoalKind.MAINTAIN -> R.string.statistics_weight_goal_maintain
}

/**
 * The dialog a target is typed into (ROADMAP N39).
 *
 * Its own state, seeded from the current target: the text is about this editing session, and keeping it here
 * is what lets the row stay a row.
 */
@Composable
private fun GoalDialog(metric: MetricEntry, goal: Double?, onSet: (Double?) -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf(goal?.let { metric.unit.format(it) }.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.statistics_goal)) },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                singleLine = true,
                suffix = { Text(stringResource(metric.unit.labelRes)) },
                modifier = Modifier.testTag(TestTags.Statistics.GOAL_FIELD),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = {
                    // A number that cannot be read sets nothing rather than a zero: the field keeps what was
                    // typed so the mistake is visible instead of silently becoming a target.
                    metric.unit.parse(typed)?.let(onSet)
                },
                modifier = Modifier.testTag(TestTags.Statistics.GOAL_CONFIRM),
            ) {
                Text(stringResource(R.string.statistics_goal_confirm))
            }
        },
        dismissButton = {
            if (goal != null) {
                AppTextButton(
                    onClick = { onSet(null) },
                    modifier = Modifier.testTag(TestTags.Statistics.GOAL_CLEAR),
                ) {
                    Text(stringResource(R.string.statistics_goal_clear))
                }
            }
        },
    )
}
