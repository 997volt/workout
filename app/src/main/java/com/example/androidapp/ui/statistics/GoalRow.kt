package com.example.androidapp.ui.statistics

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.MAINTENANCE_CORRIDOR_GRAMS
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.model.kind
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.domain.repository.GoalPeriod
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.DatePrompt
import com.example.androidapp.ui.components.TestTags
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
 * The **weight** goal, its periods, and the one editor both are written from (ROADMAP N135, N215).
 *
 * The number alone used to be the whole goal, with the rate typed beside it — two answers to one question,
 * which could disagree. Here the lifter states what they want and the rate follows: a weight to reach, a way to
 * go with no weight invented for it, or a weight to hold within the app's corridor.
 *
 * **The past is editable.** A period is a start, an optional end and a kind — including *No goal* — so a stretch
 * that began in March can be entered, corrected or removed, and the outcomes under the list re-read that stretch
 * because they are derived. One editor serves the card (a period from today), a history row (that period, or the
 * end that belongs to the one after it) and *Add a past goal* (a new one anywhere before today).
 */
@Composable
fun WeightGoalRow(
    goal: WeightGoal?,
    metric: MetricEntry,
    /** Every period of the goal, oldest first, with what became of each (ROADMAP N137). */
    history: List<GoalPeriodOutcome>,
    /** The day this screen calls today: a new period starts there, and no period may begin after it. */
    today: LocalDate,
    modifier: Modifier = Modifier,
    onSavePeriod: (id: String?, start: LocalDate?, end: LocalDate?, goal: WeightGoal?) -> Unit = { _, _, _, _ -> },
    onRemovePeriod: (String) -> Unit = {},
    /** The last save's result: the editor closes only when it succeeded (B234). */
    saveOutcome: Boolean? = null,
    onDismissSave: () -> Unit = {},
) {
    var showingHistory by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<GoalPeriodEditor?>(null) }
    var saving by remember { mutableStateOf(false) }

    GoalSaveEffect(saveOutcome) { success ->
        if (success) editor = null
        saving = false
        onDismissSave()
    }

    GoalHeader(
        goal = goal,
        metric = metric,
        history = history,
        onOpenHistory = { showingHistory = true },
        modifier = modifier,
        onSetGoal = {
            editor = newEditor(
                goal = goal,
                canHaveEnd = false,
                titleRes = R.string.statistics_weight_goal,
                today = today,
            )
        },
    )

    GoalEditorHost(
        metric = metric,
        editor = editor,
        today = today,
        saving = saving,
        onSavePeriod = onSavePeriod,
        onRemovePeriod = onRemovePeriod,
        onSaving = { saving = it },
        onClose = {
            editor = null
            saving = false
        },
    )

    GoalHistoryHost(
        showing = showingHistory,
        history = history,
        metric = metric,
        onEdit = { period, nextStart ->
            editor = periodEditor(period, nextStart, today)
            showingHistory = false
        },
        onAdd = {
            editor = newEditor(
                goal = null,
                canHaveEnd = true,
                titleRes = R.string.statistics_goal_period_add_title,
                today = today,
            )
            showingHistory = false
        },
        onDismiss = { showingHistory = false },
    )
}

/** A new period from [today]: either the card's goal or an empty stretch from the history list. */
private fun newEditor(
    goal: WeightGoal?,
    canHaveEnd: Boolean,
    @StringRes titleRes: Int,
    today: LocalDate,
): GoalPeriodEditor =
    GoalPeriodEditor(
        id = null,
        start = today,
        end = null,
        goal = goal,
        canHaveEnd = canHaveEnd,
        startKnown = true,
        titleRes = titleRes,
    )

/** Closes the editor on a successful save and leaves it open on a refusal (B234). */
@Composable
private fun GoalSaveEffect(saveOutcome: Boolean?, onResult: (Boolean) -> Unit) {
    val currentOnResult by rememberUpdatedState(onResult)
    LaunchedEffect(saveOutcome) {
        saveOutcome?.let(currentOnResult)
    }
}

/** The one period editor, drawn only while an editor is open. */
@Composable
private fun GoalEditorHost(
    metric: MetricEntry,
    editor: GoalPeriodEditor?,
    today: LocalDate,
    saving: Boolean,
    onSavePeriod: (String?, LocalDate?, LocalDate?, WeightGoal?) -> Unit,
    onRemovePeriod: (String) -> Unit,
    onSaving: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    if (editor == null) return
    GoalPeriodDialog(
        metric = metric,
        editor = editor,
        today = today,
        saving = saving,
        onSave = { id, start, end, value ->
            onSaving(true)
            onSavePeriod(id, start, end, value)
        },
        onRemove = { id ->
            onRemovePeriod(id)
            onClose()
        },
        onDismiss = onClose,
    )
}

/** The goal history list, drawn only while it is open. */
@Composable
private fun GoalHistoryHost(
    showing: Boolean,
    history: List<GoalPeriodOutcome>,
    metric: MetricEntry,
    onEdit: (GoalPeriod, Instant?) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!showing) return
    GoalHistoryDialog(
        history = history,
        metric = metric,
        onEdit = onEdit,
        onAdd = onAdd,
        onDismiss = onDismiss,
    )
}

/** The row itself: the goal in force, the way to its list, and the way to set one (N135, N215). */
@Composable
private fun GoalHeader(
    goal: WeightGoal?,
    metric: MetricEntry,
    history: List<GoalPeriodOutcome>,
    onOpenHistory: () -> Unit,
    onSetGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The list is the way to a period that already ended, and to adding one: it is offered as soon as
            // there is a period to look at, because a goal set today must still be able to gain a past.
            if (history.isNotEmpty()) {
                AppTextButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.testTag(TestTags.Statistics.GOAL_HISTORY),
                ) {
                    Text(stringResource(R.string.statistics_goal_history))
                }
            }
            AppTextButton(
                onClick = onSetGoal,
                modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_SET),
            ) {
                Text(text = weightGoalText(goal, metric))
            }
        }
    }
}

/** What the editor is writing: a period that exists, or a new one, and the end its neighbours allow. */
private data class GoalPeriodEditor(
    val id: String?,
    val start: LocalDate,
    val end: LocalDate?,
    val goal: WeightGoal?,
    /** True when a later period exists — the only case an end can move a boundary into. */
    val canHaveEnd: Boolean,
    /** False for the undated first period: its start was never recorded, so saving needs one chosen. */
    val startKnown: Boolean,
    /**
     * What the dialog is called (B230).
     *
     * The card sets today's goal, a history row corrects one and *Add a past goal* adds one; the same fields
     * are not the same question, so the title travels with the editor rather than being guessed from a null id.
     */
    @StringRes val titleRes: Int,
)

/** A period as the editor opens it: its own start, and the next boundary as the end it owns (N215). */
private fun periodEditor(period: GoalPeriod, nextStart: Instant?, today: LocalDate): GoalPeriodEditor =
    GoalPeriodEditor(
        id = period.id,
        start = period.startedAt?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: today,
        end = nextStart?.atZone(ZoneId.systemDefault())?.toLocalDate(),
        goal = period.weightGoal,
        canHaveEnd = nextStart != null,
        startKnown = period.startedAt != null,
        titleRes = R.string.statistics_goal_period_edit_title,
    )

/**
 * What each period of the goal asked for, and whether it was met (ROADMAP N137).
 *
 * Newest first, because the period a lifter is in is the one they are asking about — and each row says the
 * stretch it covered, which is its own start to the next one's. A row with no recorded start says so rather than
 * showing a date the app does not have. Tapping one edits it; *Add a past goal* opens the same editor empty.
 */
@Composable
private fun GoalHistoryDialog(
    history: List<GoalPeriodOutcome>,
    metric: MetricEntry,
    onEdit: (GoalPeriod, Instant?) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Statistics.GOAL_HISTORY_DIALOG),
        title = { Text(stringResource(R.string.statistics_goal_history_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                history.mapIndexed { index, outcome -> outcome to history.getOrNull(index + 1)?.period?.startedAt }
                    .reversed()
                    .forEachIndexed { index, (outcome, nextStart) ->
                        GoalHistoryRow(
                            outcome = outcome,
                            nextStart = nextStart,
                            metric = metric,
                            testTag = TestTags.Statistics.goalOutcome(index),
                            onClick = { onEdit(outcome.period, nextStart) },
                        )
                    }
                AppTextButton(
                    onClick = onAdd,
                    modifier = Modifier.testTag(TestTags.Statistics.GOAL_PERIOD_ADD),
                ) {
                    Text(stringResource(R.string.statistics_goal_period_add))
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.Statistics.GOAL_HISTORY_CLOSE),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * One period: what it asked for, whether it was met, and the stretch it covered (ROADMAP N137, N215).
 *
 * The whole row is the control that edits it, and its name says so — a row that only looked like a fact would
 * leave the correction this feature is for undiscoverable.
 */
@Composable
private fun GoalHistoryRow(
    outcome: GoalPeriodOutcome,
    nextStart: Instant?,
    metric: MetricEntry,
    testTag: String,
    onClick: () -> Unit,
) {
    val start = outcome.period.startedAt
    val range = when {
        start == null -> stringResource(R.string.statistics_goal_from_unknown)
        nextStart == null -> stringResource(
            R.string.statistics_goal_from,
            goalDayText(start.atZone(ZoneId.systemDefault()).toLocalDate()),
        )
        else -> stringResource(
            R.string.statistics_goal_range,
            goalDayText(start.atZone(ZoneId.systemDefault()).toLocalDate()),
            goalDayText(nextStart.atZone(ZoneId.systemDefault()).toLocalDate()),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClickLabel = stringResource(R.string.statistics_goal_period_edit), onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = weightGoalText(outcome.period.weightGoal, metric),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(goalOutcomeLabel(outcome.outcome)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = range,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The verdict in words: met, missed, a period of no goal, or one too thin to judge (N137). */
private fun goalOutcomeLabel(outcome: GoalOutcome): Int = when (outcome) {
    GoalOutcome.Met -> R.string.statistics_goal_met
    GoalOutcome.Missed -> R.string.statistics_goal_missed
    GoalOutcome.NoGoal -> R.string.statistics_goal_no_goal
    GoalOutcome.Unjudged -> R.string.statistics_goal_unjudged
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
 * The dialog a goal period is written in (ROADMAP N135, N215).
 *
 * The kind first, because it is the question; the weight only where the kind asks for one; the days last, because
 * an edit to an existing period is usually about the goal rather than its boundary. **No goal** is one of the
 * kinds, so the period a *Clear* used to write is stated like any other stretch.
 */
@Composable
private fun GoalPeriodDialog(
    metric: MetricEntry,
    editor: GoalPeriodEditor,
    today: LocalDate,
    onSave: (String?, LocalDate?, LocalDate?, WeightGoal?) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean = false,
) {
    val draft = rememberGoalPeriodDraft(editor, metric, today)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Statistics.GOAL_PERIOD_DIALOG),
        title = {
            Text(stringResource(editor.titleRes))
        },
        text = { GoalPeriodFields(metric = metric, draft = draft) },
        confirmButton = {
            AppTextButton(
                onClick = { onSave(editor.id, draft.start, draft.end, draft.goal) },
                enabled = draft.valid && !saving,
                modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_CONFIRM),
            ) {
                Text(stringResource(R.string.statistics_goal_confirm))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editor.id != null) {
                    AppTextButton(
                        onClick = { onRemove(editor.id) },
                        modifier = Modifier.testTag(TestTags.Statistics.GOAL_PERIOD_REMOVE),
                    ) {
                        Text(stringResource(R.string.statistics_goal_period_remove))
                    }
                }
                AppTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )

    if (draft.pickingStart) {
        DatePrompt(
            initial = draft.start ?: today,
            latestDay = today,
            onDismiss = { draft.pickingStart = false },
            onPick = draft::pickStart,
        )
    }
    if (draft.pickingEnd) {
        DatePrompt(
            initial = draft.end ?: draft.start?.plusMonths(1) ?: today,
            latestDay = today,
            onDismiss = { draft.pickingEnd = false },
            onPick = draft::pickEnd,
        )
    }
}

/** The kind chips, the weight where it is asked for, and the two days (N215). */
@Composable
private fun GoalPeriodFields(metric: MetricEntry, draft: GoalPeriodDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalPeriodKinds(draft.kind) { draft.kindName = it?.name }
        if (draft.needsWeight) {
            OutlinedTextField(
                value = draft.weight,
                onValueChange = { draft.weight = it },
                singleLine = true,
                isError = draft.weight.isNotBlank() && draft.goal == null,
                suffix = { Text(stringResource(metric.unit.labelRes)) },
                modifier = Modifier.testTag(TestTags.Statistics.WEIGHT_GOAL_FIELD),
            )
        }
        AppTextButton(
            onClick = { draft.pickingStart = true },
            modifier = Modifier.testTag(TestTags.Statistics.GOAL_PERIOD_START),
        ) {
            Text(
                stringResource(
                    R.string.statistics_goal_period_since,
                    draft.start?.let(::goalDayText) ?: stringResource(R.string.statistics_goal_from_unknown),
                ),
            )
        }
        if (draft.canHaveEnd) {
            AppTextButton(
                onClick = { draft.pickingEnd = true },
                modifier = Modifier.testTag(TestTags.Statistics.GOAL_PERIOD_END),
            ) {
                Text(
                    stringResource(
                        R.string.statistics_goal_period_until,
                        draft.end?.let(::goalDayText) ?: stringResource(R.string.statistics_goal_period_ongoing),
                    ),
                )
            }
        }
    }
}

/** The kinds, *No goal* included: a period is what the lifter asked for, and nothing is one answer (N215). */
@Composable
private fun GoalPeriodKinds(kind: WeightGoalKind?, onKind: (WeightGoalKind?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WeightGoalKind.entries.forEach { option ->
            AppFilterChip(
                selected = kind == option,
                onClick = { onKind(option) },
                label = stringResource(weightGoalKindLabel(option)),
                testTag = TestTags.Statistics.weightGoalKind(option),
            )
        }
        AppFilterChip(
            selected = kind == null,
            onClick = { onKind(null) },
            label = stringResource(R.string.statistics_goal_kind_none),
            testTag = TestTags.Statistics.GOAL_KIND_NONE,
        )
    }
}

/**
 * The editor's form, held as state (ROADMAP N215).
 *
 * A state holder rather than six `remember`s in the dialog, the shape Compose recommends for a form: the dialog
 * is the layout, and the rules that decide what can be saved live beside the fields they read.
 */
private class GoalPeriodDraft(
    private val metric: MetricEntry,
    private val today: LocalDate,
    editor: GoalPeriodEditor,
) {
    val canHaveEnd: Boolean = editor.canHaveEnd

    /** Only the undated row the N137 migration creates, and only when it already exists, may keep no start (B232). */
    private val startMayBeUnknown = !editor.startKnown && editor.id != null
    var kindName by mutableStateOf(editor.goal?.kind()?.name)
    var weight by mutableStateOf(
        editor.goal?.statedWeightGrams()?.let { metric.unit.format(it.toDouble()) }.orEmpty(),
    )
    private var startDay by mutableStateOf(editor.start.takeIf { editor.startKnown }?.toEpochDay())
    private var endDay by mutableStateOf(editor.end?.toEpochDay())
    var pickingStart by mutableStateOf(false)
    var pickingEnd by mutableStateOf(false)

    val kind: WeightGoalKind?
        get() = kindName?.let { name -> WeightGoalKind.entries.firstOrNull { it.name == name } }

    val start: LocalDate? get() = startDay?.let { LocalDate.ofEpochDay(it) }

    /** An end only exists for a period with a next one (N215): the goal in force is open by definition. */
    val end: LocalDate? get() = endDay?.takeIf { canHaveEnd }?.let { LocalDate.ofEpochDay(it) }

    val needsWeight: Boolean
        get() = kind == WeightGoalKind.TARGET || kind == WeightGoalKind.MAINTAIN

    val goal: WeightGoal? get() = weightGoalFor(kind, weight, metric)

    /** A day after today is not a record, and an end that is not after its start would write an empty stretch. */
    val valid: Boolean
        get() {
            val start = start
            val end = end
            val datesValid = if (start == null) {
                startMayBeUnknown && (end == null || !end.isAfter(today))
            } else {
                !start.isAfter(today) && (end == null || (end.isAfter(start) && !end.isAfter(today)))
            }
            return datesValid && (!needsWeight || goal != null) && (canHaveEnd || end == null)
        }

    /** A start picked past the end takes the end with it, rather than leaving an invisible invalid form. */
    fun pickStart(day: LocalDate) {
        startDay = day.toEpochDay()
        val end = end
        if (end != null && !end.isAfter(day)) endDay = null
    }

    fun pickEnd(day: LocalDate) {
        endDay = day.toEpochDay()
    }
}

@Composable
private fun rememberGoalPeriodDraft(
    editor: GoalPeriodEditor,
    metric: MetricEntry,
    today: LocalDate,
): GoalPeriodDraft = remember(editor) { GoalPeriodDraft(metric, today, editor) }

private fun weightGoalFor(kind: WeightGoalKind?, typed: String, metric: MetricEntry): WeightGoal? = when (kind) {
    null -> null
    WeightGoalKind.LOSE -> WeightGoal.Direction(gain = false)
    WeightGoalKind.GAIN -> WeightGoal.Direction(gain = true)
    WeightGoalKind.TARGET -> metric.unit.parse(typed)?.let { WeightGoal.Target(it.toLong()) }
    WeightGoalKind.MAINTAIN -> metric.unit.parse(typed)?.let { WeightGoal.Maintain(it.toLong()) }
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
