package com.example.androidapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.FailureMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.PrimaryActionButton
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutClock
import kotlinx.coroutines.CoroutineScope

@Composable
fun WorkoutsHomeRoute(
    onStartWorkout: () -> Unit,
    onOpenTemplates: () -> Unit,
    /**
     * The navigation target: the template to start, and the slot it was scheduled as, if any
     * (ROADMAP P3.3, P3.8).
     */
    onStartTemplate: (String, String?) -> Unit,
    onOpenPrograms: () -> Unit,
    /**
     * The way into body measurements (ROADMAP N102).
     *
     * Home is where the weight series the statistics screen reads gets written, so the way in belongs with
     * the other ways in rather than inside a chart tab.
     */
    onOpenMeasurements: () -> Unit,
    /**
     * The way into today's daily measurement (ROADMAP N114), which opens that form rather than the list.
     */
    onMeasureToday: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Not unwrapped with `by`: reading it here would rebuild the list every second.
    val clock = viewModel.clock.collectAsStateWithLifecycle()
    val plannedWorkout by viewModel.plannedWorkout.collectAsStateWithLifecycle()

    // The substitute pick is the one write this screen still makes, and a failure has to be said
    // rather than swallowed (F7). The data actions that used to share this host now live in
    // Settings, where the app's database is what the screen is about (N43).
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // `dataErrorMessage` is a composable, so the failure is held as a value and turned into a
    // sentence during composition rather than inside the coroutine.
    var failure by remember { mutableStateOf<DataError?>(null) }
    FailureMessage(failure = failure, onMessage = { message = it }, onClear = { failure = null })

    // The active-session question (ROADMAP N89) is composed *inside* the missed-day gate below, so it
    // intercepts a start only once that question is settled: asking the other way round could leave a
    // lifter with a discarded session and nothing started if they then dismissed the missed-day prompt.
    val requestStartFromHome = activeWorkoutGate(
        // Read when a start arrives rather than captured: the session can begin or end while this composes.
        isActive = { state.activeWorkout != null },
        onStart = { intent ->
            // The slot travels with the template so the session's prefill reads that slot's history (N73).
            if (intent.templateId != null) {
                onStartTemplate(intent.templateId, intent.slotId)
            } else {
                onStartWorkout()
            }
        },
        // *Continue workout* goes where home's own pill does.
        onContinueOngoing = onStartWorkout,
        discard = viewModel::discardActiveWorkout,
        onFailure = { failure = it },
    )

    // Every start goes through the program's missed-day question (ROADMAP P3.3), and this is
    // the only place the navigation happens: "do it now" and "continue" differ in intent
    // rather than in destination plumbing. A skip that could not be recorded is shown on the
    // same host, because the workout still starts and the question will come back (F7).
    val requestStart = programStartGate(
        onStart = requestStartFromHome,
        onError = { message = it },
    )

    PlannedWorkoutDialog(
        planned = plannedWorkout,
        onDismiss = viewModel::onDismissPlannedWorkout,
    )

    WorkoutsHomeScreen(
        state = state,
        clock = clock,
        onStartWorkout = { requestStart(StartIntent()) },
        onOpenTemplates = onOpenTemplates,
        onStartTemplate = { plan ->
            requestStart(
                StartIntent(
                    templateId = plan.templateId,
                    // The row being started names it, so "continue with Bench" reads true, and its
                    // slot travels for the prefill history (N73).
                    slotId = plan.slotId,
                    label = plan.name,
                ),
            )
        },
        // Choosing a substitute records it for this slot and this week — no other week changes —
        // and then starts it, because the pick is made at the point of starting (P3.11).
        onSubstituteTemplate = substituteOccurrence(
            scope = scope,
            viewModel = viewModel,
            requestStart = requestStart,
            onFailure = { failure = it },
        ),
        // A block's own skip, which is a write and reports both ways (B195).
        onSkipBlock = rememberSkipBlock(viewModel, scope, { message = it }, { failure = it }),
        onOpenPrograms = onOpenPrograms,
        onOpenMeasurements = onOpenMeasurements,
        onMeasureToday = onMeasureToday,
        onOpenPlannedWorkout = viewModel::onOpenPlannedWorkout,
        message = message,
        onDismissMessage = { message = null },
        modifier = modifier,
    )
}

/**
 * The plan whose card opened the picker, or null (P3.11).
 *
 * One shape now, not two: *Substitute* is offered by a scheduled card alone (N125), so every pick is
 * written for the slot and week it belongs to. The next-up rows' unrecorded variant went with them (N85).
 */
private data class SubstituteRequest(val plan: TodayPlan)

@OptIn(ExperimentalMaterial3Api::class)
/**
 * A block's skip action, with the sentence it reports (ROADMAP B195).
 *
 * Its own composable because the route is at the length this project allows, and because the action
 * and the words it leaves behind are one thing: letting a session go is a write, and a failure has
 * to be said rather than swallowed.
 */
@Composable
private fun rememberSkipBlock(
    viewModel: WorkoutsHomeViewModel,
    scope: CoroutineScope,
    onMessage: (String) -> Unit,
    onFailure: (DataError) -> Unit,
): (TodayPlan) -> Unit {
    val skipped = stringResource(R.string.home_block_skipped)
    return skipBlockSession(scope, viewModel, onDone = { onMessage(skipped) }, onFailure = onFailure)
}

@Composable
fun WorkoutsHomeScreen(
    state: WorkoutsHomeUiState,
    clock: State<WorkoutClock>,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTemplates: () -> Unit = {},
    onStartTemplate: (TodayPlan) -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    onOpenMeasurements: () -> Unit = {},
    /**
     * Opens the measurements screen with today's daily form already up (ROADMAP N114).
     *
     * A second entry rather than a flag on [onOpenMeasurements]: the two rows reach one destination for
     * different reasons — reading the record, and answering today — and the row that answers is drawn only
     * while today has no entry to read.
     */
    onMeasureToday: () -> Unit = {},
    /**
     * Opens what a card has planned, without starting it (ROADMAP N55, N127).
     *
     * Looking and starting stopped being the same gesture: the card's own name opens the plan, and *Start*
     * keeps starting it.
     */
    onOpenPlannedWorkout: (TodayPlan) -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    /**
     * Records the workout that stands in for one occurrence, then starts it (ROADMAP P3.11).
     *
     * A null template restores the slot's own workout and starts nothing. The third argument is the picked
     * template's **name**, so the start's own question can name the workout it is about to begin rather than
     * the one that was scheduled (B93).
     */
    onSubstituteTemplate: (TodayPlan, String?, String?) -> Unit = { _, _, _ -> },
    /** Lets a block's next session go, which is the only way past one that cannot be trained (B195). */
    onSkipBlock: (TodayPlan) -> Unit = {},
) {
    // The card whose substitute picker is open, or null (P3.11). `substituting?.plan` is what the dialog draws.
    var substituting by remember { mutableStateOf<SubstituteRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    SubstitutePicker(
        request = substituting,
        templates = state.templates,
        onChoose = { request, templateId, templateName ->
            onSubstituteTemplate(request.plan, templateId, templateName)
        },
        onDismiss = { substituting = null },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { HomeTopBar() },
        // A bottom bar rather than a floating button. The primary action on this screen is the
        // one thing the reference never floats: it is a full-width pill resting on the bar,
        // because the thing you came to do should not be a target you have to aim at.
        //
        // The *Resume* pill is all it carries now (N124): where the next workout is lives in the body with
        // the day's plan, so with nothing running the bar has nothing to say and is not drawn at all.
        bottomBar = {
            state.activeWorkout?.let { running ->
                StartActions(
                    activeWorkout = running,
                    clock = clock,
                    onStartWorkout = onStartWorkout,
                )
            }
        },
    ) { innerPadding ->
        HomeContent(
            state = state,
            onStartWorkout = onStartWorkout,
            onOpenPrograms = onOpenPrograms,
            onOpenTemplates = onOpenTemplates,
            onOpenMeasurements = onOpenMeasurements,
            onMeasureToday = onMeasureToday,
            onStartTemplate = onStartTemplate,
            onSubstitute = { plan -> substituting = SubstituteRequest(plan) },
            onSkipBlock = onSkipBlock,
            onOpenPlannedWorkout = onOpenPlannedWorkout,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The open substitute picker, or nothing (ROADMAP P3.11).
 *
 * Split out of the screen, which is at the length this project allows, and because the dialog's
 * three exits are one shape: pick a stand-in, restore the scheduled workout, or back out.
 *
 * The slot's own template is left out: the dialog asks what stands in for it, so offering the thing
 * being replaced as a choice was a second, contradictory way to say "the scheduled workout", and the
 * first row already says that. Starting the scheduled workout stays the row's own Start.
 */
@Composable
private fun SubstitutePicker(
    request: SubstituteRequest?,
    templates: List<WorkoutTemplate>,
    /**
     * The pick, with the request it answers.
     *
     * The request travels with it because the picker **dismisses before it reports** — the dialog's exits
     * call `onDismiss` first, so reading the screen's own state back here would read it already cleared. That
     * is also why which rows record is the request's business rather than the screen's. The third argument is
     * the picked template's name, which the dialog already draws and the start's question needs (B93).
     */
    onChoose: (SubstituteRequest, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = request ?: return
    SubstituteDialog(
        templates = templates.filterNot { it.id == current.plan.templateId },
        onPick = { template ->
            onDismiss()
            onChoose(current, template.id, template.name)
        },
        onClear = {
            onDismiss()
            onChoose(current, null, null)
        },
        onDismiss = onDismiss,
    )
}

/**
 * What stands in for one occurrence this week (ROADMAP P3.11).
 *
 * The scheduled workout is offered first and clears the pick, because a substitution is the
 * lifter's statement rather than the app's and a mis-pick would otherwise be permanent. The next-up rows
 * that wrote no event — so had no pick to clear — went with the rows themselves (N85, N125), which is why
 * the entry is unconditional now.
 */
@Composable
private fun SubstituteDialog(
    templates: List<WorkoutTemplate>,
    onPick: (WorkoutTemplate) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.HOME_SUBSTITUTE_DIALOG),
        title = { Text(stringResource(R.string.home_substitute_title)) },
        text = {
            LazyColumn {
                item(key = "scheduled") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.home_substitute_clear)) },
                        modifier = Modifier
                            .testTag(TestTags.HOME_SUBSTITUTE_CLEAR)
                            .clickable(onClick = onClear),
                    )
                }
                items(items = templates, key = { it.id }) { template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        modifier = Modifier
                            .testTag(TestTags.homeSubstituteTemplate(template.id))
                            .clickable { onPick(template) },
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * The list body, split out so the screen itself stays a scaffold and a state.
 *
 * The body is the ways in, always, with the one surface for where the next workout is below them: today's
 * plan and each active program's next run (N115, N124). There is no empty state left to draw — a body of
 * four ways in is never blank — which is why the first-run message went with the list it was explaining.
 */
@Composable
private fun HomeContent(
    state: WorkoutsHomeUiState,
    onStartWorkout: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onMeasureToday: () -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    onSkipBlock: (TodayPlan) -> Unit,
    onOpenPlannedWorkout: (TodayPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        CenteredMessage(
            text = stringResource(R.string.history_loading),
            modifier = modifier,
        )
    } else {
        HomeBody(
            state = state,
            onStartWorkout = onStartWorkout,
            onOpenPrograms = onOpenPrograms,
            onOpenTemplates = onOpenTemplates,
            onOpenMeasurements = onOpenMeasurements,
            onMeasureToday = onMeasureToday,
            onStartTemplate = onStartTemplate,
            onSubstitute = onSubstitute,
            onSkipBlock = onSkipBlock,
            onOpenPlannedWorkout = onOpenPlannedWorkout,
            modifier = modifier,
        )
    }
}

/**
 * The home start bar: the *Resume* pill, for the workout already running (ROADMAP P1.16, N102, N124).
 *
 * So the bar is drawn only while a workout is open, and resuming is the only thing it offers. It stays a
 * bottom bar rather than a floating button because the primary action is the one thing you should not have
 * to aim at.
 *
 * Starting empty is one of the body's rows (N102), and this bar does not offer it: the same action twice on
 * one screen is what N42 removed the last time it happened. The *Programs* and *Templates* links left with
 * it, for that reason and to that place — the body's ways in, where a destination can say what it is instead
 * of being one word in a row of words.
 */
@Composable
private fun StartActions(
    activeWorkout: ActiveWorkoutInfo,
    clock: State<WorkoutClock>,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Resuming offers no choice: there is exactly one workout in progress, so the button means one
        // thing. The caller draws the bar only when there is one (N102, N124).
        ResumeButton(
            activeWorkout = activeWorkout,
            clock = clock,
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The bar's one pill: the workout already running (P1.16, moved here by N1, narrowed by N102).
 *
 * Reads the clock — and is the only composable here that does, so the one-second tick stops at this button
 * instead of rebuilding the list beneath it — and hands the shape to [PrimaryActionButton], which the next-up
 * pill shares.
 *
 * There is exactly one workout in progress, so the button means one thing and takes no null: the empty start
 * it used to fall back to is a row in the body now, which is why this no longer branches.
 */
@Composable
private fun ResumeButton(
    activeWorkout: ActiveWorkoutInfo,
    clock: State<WorkoutClock>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = pluralStringResource(
        R.plurals.library_exercises,
        activeWorkout.exerciseCount,
        activeWorkout.exerciseCount,
    )

    PrimaryActionButton(
        text = listOf(
            stringResource(R.string.library_resume_workout),
            clock.value.elapsed,
            exercises,
        ).filter { it.isNotEmpty() }.joinToString(" · "),
        icon = Icons.Filled.PlayArrow,
        onClick = onClick,
        // Resuming is the only thing the bar offers, so it keeps the accent and stays the loud one — the
        // pairing N61 settled when the empty start receded to the container below it.
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        // Tagged by state, not caption: the caption is user-visible text a translation changes.
        modifier = modifier.testTag(TestTags.HOME_RESUME),
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkoutsHomeScreenPreview() {
    AndroidAppTheme {
        WorkoutsHomeScreen(
            state = WorkoutsHomeUiState(isLoading = false),
            clock = remember { mutableStateOf(WorkoutClock()) },
            onStartWorkout = {},
            onOpenTemplates = {},
            onOpenPrograms = {},
            onOpenMeasurements = {},
        )
    }
}

/**
 * Home's app bar: the title, and nothing else.
 *
 * Programs and the app's data actions used to hang off the overflow here; both have moved to the
 * places they belong (ROADMAP N42, N43) — Programs into the action row, the data actions into
 * Settings — so the bar is the screen's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    // Centred, and the title is an eyebrow rather than a headline: the reference puts the
    // screen's name in small tracked capitals and lets the content be the loudest thing on the
    // screen. `CenterAlignedTopAppBar` and not `TopAppBar`, because a small tracked title
    // hanging off the left edge under a back arrow is the one arrangement that looks like a
    // mistake rather than a decision.
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            TopBarTitle(
                text = stringResource(R.string.home_title),
                testTag = TestTags.HOME_TITLE,
            )
        },
    )
}
