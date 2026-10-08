package com.example.androidapp.ui.home

import com.example.androidapp.domain.DataError
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.FailureMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.PrimaryActionButton
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutClock
import java.time.Instant

@Composable
fun WorkoutsHomeRoute(
    onStartWorkout: () -> Unit,
    onOpenTemplates: () -> Unit,
    /**
     * The navigation target: the template to start, and the slot it was scheduled as, if any
     * (ROADMAP P3.3, P3.8).
     */
    onStartTemplate: (String, String?) -> Unit,
    onOpenWorkout: (String) -> Unit,
    onOpenPrograms: () -> Unit,
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
            // The slot travels with the template so its prescription seeds the workout (P3.8).
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
                    // slot is what carries the prescription (P3.8).
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
        // A next-up row's pick starts the session and writes nothing (N85): the run it stands for is
        // deliberately calendar-free, so there is no week to key a substitution to.
        onStartSubstituteTemplate = startSubstitute(requestStart = requestStart),
        onOpenWorkout = onOpenWorkout,
        onOpenPrograms = onOpenPrograms,
        onOpenPlannedWorkout = viewModel::onOpenPlannedWorkout,
        message = message,
        onDismissMessage = { message = null },
        modifier = modifier,
    )
}

/**
 * A substitute picker that has been opened (ROADMAP N85): which row asked, and whether the pick is recorded.
 *
 * The two rows that offer *Substitute* do different things with the answer — a scheduled occurrence is
 * written for its slot and week (P3.11), while a next-up row is not written at all — so the picker has to
 * remember which kind of row opened it. A record rather than two states because the two only travel
 * together.
 */
private data class SubstituteRequest(val plan: TodayPlan, val records: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsHomeScreen(
    state: WorkoutsHomeUiState,
    clock: State<WorkoutClock>,
    onStartWorkout: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenTemplates: () -> Unit = {},
    onStartTemplate: (TodayPlan) -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    /**
     * Opens what a program's next run has planned, without starting it (ROADMAP N55).
     *
     * Looking and starting stopped being the same gesture: the field opens the plan, and *Start* keeps
     * starting it.
     */
    onOpenPlannedWorkout: (NextUp) -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    /**
     * Records the workout that stands in for one occurrence, then starts it (ROADMAP P3.11).
     *
     * A null template restores the slot's own workout and starts nothing.
     */
    onSubstituteTemplate: (TodayPlan, String?) -> Unit = { _, _ -> },
    /**
     * Starts the workout a next-up row's pick chose, and records nothing (ROADMAP N85).
     *
     * Beside [onSubstituteTemplate] rather than folded into it, because the two rows' picks are not the same
     * act: one writes a substitution for an occurrence it belongs to, and the other has no occurrence to
     * write against.
     */
    onStartSubstituteTemplate: (TodayPlan, String?) -> Unit = { _, _ -> },
) {
    // The row whose substitute picker is open, and whether that row's pick is recorded, or null
    // (P3.11, N85). `substituting?.plan` is what the dialog draws; which callback answers it is decided by
    // *which kind of row* opened it, which is the whole of N85's decision.
    var substituting by remember { mutableStateOf<SubstituteRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    SubstitutePicker(
        request = substituting,
        templates = state.templates,
        onChoose = { request, templateId ->
            if (request.records) {
                onSubstituteTemplate(request.plan, templateId)
            } else {
                onStartSubstituteTemplate(request.plan, templateId)
            }
        },
        onDismiss = { substituting = null },
    )

    Scaffold(        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { HomeTopBar() },
        // A bottom bar rather than a floating button. The primary action on this screen is the
        // one thing the reference never floats: it is a full-width pill resting on the bar,
        // because the thing you came to do should not be a target you have to aim at.
        bottomBar = {
            StartActions(
                activeWorkout = state.activeWorkout,
                clock = clock,
                nextUp = state.nextUp,
                onStartWorkout = onStartWorkout,
                onOpenTemplates = onOpenTemplates,
                // A next-up row starts the workout the same way a scheduled one does, so the slot's
                // prescription travels with it (ROADMAP P3.8).
                onStartTemplate = onStartTemplate,
                onOpenPlannedWorkout = onOpenPlannedWorkout,
                // A next-up row's pick starts the session and records nothing (N85).
                onSubstitute = { plan -> substituting = SubstituteRequest(plan, records = false) },
                // Programs took the slot the repeat-last link gave up (ROADMAP N42): the screen
                // the whole scheduling half is edited from belongs in the action row rather than
                // behind the overflow it got lost in.
                onOpenPrograms = onOpenPrograms,
            )
        },
    ) { innerPadding ->
        HomeContent(
            state = state,
            onOpenWorkout = onOpenWorkout,
            onStartTemplate = onStartTemplate,
            onSubstitute = { plan -> substituting = SubstituteRequest(plan, records = true) },
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
     * is also why which rows record is the request's business rather than the screen's.
     */
    onChoose: (SubstituteRequest, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = request ?: return
    SubstituteDialog(
        templates = templates.filterNot { it.id == current.plan.templateId },
        onPick = { templateId ->
            onDismiss()
            onChoose(current, templateId)
        },
        onClear = {
            onDismiss()
            onChoose(current, null)
        },
        // A next-up row records no substitution (N85), so there is nothing for it to restore: the entry
        // would dismiss and do nothing at all. Offered only where the pick was written (N53, N67).
        showClear = current.records,
        onDismiss = onDismiss,
    )
}

/**
 * What stands in for one occurrence this week (ROADMAP P3.11).
 *
 * The scheduled workout is offered first and clears the pick, because a substitution is the
 * lifter's statement rather than the app's and a mis-pick would otherwise be permanent — but only where
 * the pick is recorded at all ([showClear]): a next-up row's substitute writes no event, so it has no
 * pick to clear and the entry is left out rather than offered as a control that cannot do anything.
 */
@Composable
private fun SubstituteDialog(
    templates: List<WorkoutTemplate>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    /** False where the row records nothing, so there is no pick for the clearing row to clear (N85). */
    showClear: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.HOME_SUBSTITUTE_DIALOG),
        title = { Text(stringResource(R.string.home_substitute_title)) },
        text = {
            LazyColumn {
                if (showClear) {
                    item(key = "scheduled") {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.home_substitute_clear)) },
                            modifier = Modifier
                                .testTag(TestTags.HOME_SUBSTITUTE_CLEAR)
                                .clickable(onClick = onClear),
                        )
                    }
                }
                items(items = templates, key = { it.id }) { template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        modifier = Modifier
                            .testTag(TestTags.homeSubstituteTemplate(template.id))
                            .clickable { onPick(template.id) },
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
 * Lives here rather than beside the rows it draws because it is a decision about *state* — which
 * of the four things the screen can be showing — and the rows are only one of the four.
 */
@Composable
private fun HomeContent(
    state: WorkoutsHomeUiState,
    onOpenWorkout: (String) -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
        when {
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.history_loading),
                modifier = modifier,
            )

            // First run: an empty list with no explanation tells the user nothing. A next-up row no
            // longer counts here — it lives in the bottom bar (ROADMAP N55), which is not this list.
            state.todaysPlan.isNotEmpty() -> TodayAndRecent(
                state = state,
                onOpenWorkout = onOpenWorkout,
                onStartTemplate = onStartTemplate,
                onSubstitute = onSubstitute,
                modifier = modifier,
            )

            state.isFirstRun -> CenteredMessage(
                text = stringResource(R.string.home_first_run),
                hint = stringResource(R.string.home_first_run_hint),
                modifier = modifier.testTag(TestTags.HOME_FIRST_RUN),
            )

            state.recent.isEmpty() -> CenteredMessage(
                text = stringResource(R.string.home_no_recent),
                modifier = modifier.testTag(TestTags.HOME_NO_RECENT),
            )

            else -> LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    // The heading and nothing else: "See all workouts" went (ROADMAP N42), because
                    // History is the tab beside this one — a section heading carrying a way out of
                    // its own section was a second path to somewhere the app already goes.
                    SectionHeader(text = stringResource(R.string.home_recent))
                }
                items(state.recent.size, key = { state.recent[it].id }) { index ->
                    val workout = state.recent[index]
                    RecentWorkoutRow(workout = workout, onClick = { onOpenWorkout(workout.id) })
                }
            }
        }
}

/**
 * The home start bar: the ways to begin, then where a program's run is up to (ROADMAP N3, P3.9, N55).
 *
 * Read top to bottom, it is the two ways into a plan — *Programs* and *Templates* — the screen's primary
 * action, and last the next-up block, so the thing the app is telling you to do next sits at the very
 * edge the thumb is already at. The empty start names itself **Start empty workout** so the bar's two
 * full-width pills do not read as the same action.
 *
 * Resuming offers no choice: there is exactly one workout in progress, so the button means one thing.
 * The pair of links **stays while a workout is open** (ROADMAP N78): hiding them was how the app said
 * "you are in a workout", and a lifter checking what is next should not have to finish one to look — the
 * pill below already says what the primary act is. Repeat-last gave its slot to Programs (ROADMAP N42);
 * the entry point it gave up is an action on a finished workout, in History.
 */
@Composable
private fun StartActions(
    activeWorkout: ActiveWorkoutInfo?,
    clock: State<WorkoutClock>,
    nextUp: List<NextUp>,
    onStartWorkout: () -> Unit,
    onOpenTemplates: () -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onOpenPlannedWorkout: (NextUp) -> Unit,
    /** Opens the substitute picker for one next-up row (ROADMAP N85). */
    onSubstitute: (TodayPlan) -> Unit,
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A row of links above the pill, not a second pill. They stay while a workout is open
        // (ROADMAP N78): hiding them was how the app said "you are in a workout", and a lifter
        // checking what is next should not have to finish one to look. The pill below already says
        // what the primary act is.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextButton(
                onClick = onOpenPrograms,
                modifier = Modifier.testTag(TestTags.HOME_PROGRAMS),
            ) {
                Text(stringResource(R.string.home_programs))
            }
            AppTextButton(
                onClick = onOpenTemplates,
                modifier = Modifier.testTag(TestTags.HOME_TEMPLATES),
            ) {
                Text(stringResource(R.string.home_templates))
            }
        }
        StartOrResumeButton(
            activeWorkout = activeWorkout,
            clock = clock,
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth(),
        )
        // Where each active program's run is, at the bottom edge of the screen the thumb is already
        // at (ROADMAP P3.9, N55). It sits under the start pill so the thing the app says is next is
        // the last thing the thumb reaches, and the field stays compact above its own full-width pill
        // because more than one active program (P3.12) means the bar can carry several.
        nextUp.forEach { nextUpRow ->
            NextUpRow(
                nextUp = nextUpRow,
                onOpen = { onOpenPlannedWorkout(nextUpRow) },
                onStart = { onStartTemplate(nextUpRow.plan) },
                onSubstitute = { onSubstitute(nextUpRow.plan) },
            )
        }
    }
}

/**
 * The home screen's primary action (P1.16, moved here by N1).
 *
 * Reads the clock — and is the only composable here that does, so the one-second tick stops at this
 * button instead of rebuilding the list beneath it — and hands the shape to [PrimaryActionButton],
 * which the next-up pill shares.
 */
@Composable
private fun StartOrResumeButton(
    activeWorkout: ActiveWorkoutInfo?,
    clock: State<WorkoutClock>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resuming = activeWorkout != null
    val elapsed = if (resuming) clock.value.elapsed else ""
    val exercises = if (resuming) {
        pluralStringResource(
            R.plurals.library_exercises,
            activeWorkout.exerciseCount,
            activeWorkout.exerciseCount,
        )
    } else {
        ""
    }

    PrimaryActionButton(
        text = if (resuming) {
            listOf(
                stringResource(R.string.library_resume_workout),
                elapsed,
                exercises,
            ).filter { it.isNotEmpty() }.joinToString(" · ")
        } else {
            stringResource(R.string.home_start_empty_workout)
        },
        icon = if (resuming) Icons.Filled.PlayArrow else Icons.Filled.Add,
        onClick = onClick,
        // The empty start recedes to the palette's deep indigo (N61), one step below the planned pills,
        // which draw the tonal container now (N83). Resuming is the only thing to do, so it keeps the
        // accent and stays the loud one.
        containerColor = if (resuming) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        // The pair travels together: the lesser pill draws on the container, so its label takes the
        // container's own "on" colour. The default is the accent surface's `onPrimary`, which the
        // contrast check would then be asserting about a pair nothing draws (N61).
        contentColor = if (resuming) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        // Tagged by state, not caption: which of the two shows is the behaviour under test, and the
        // captions are user-visible text a translation changes.
        modifier = modifier.testTag(if (resuming) TestTags.HOME_RESUME else TestTags.HOME_START),
    )
}

/**
 * One program's next run, at the bottom of the bar (ROADMAP P3.9, N55).
 *
 * The field opens what is planned and the full-width pill under it starts it, so looking and starting
 * stay two gestures (N55). The pill keeps the empty start's shape but not its colour: it draws the tonal
 * container *Log set* draws (N83), which is what the two planned-workout starts have in common and what
 * tells them apart from the empty start's deep indigo. The field above it stays compact, because more than
 * one program can be active (P3.12) and the bar may carry several.
 */
@Composable
private fun NextUpRow(
    nextUp: NextUp,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    /** Opens the substitute picker for this row (ROADMAP N85). */
    onSubstitute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(R.string.home_next_up_open, nextUp.plan.name)
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        // The whole line is the field, so the target is the row rather than a caption inside it.
        // `clickable` carries an `onClickLabel` because the headline names the workout and never the
        // action (the rule the restyle's rows follow).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.Home.nextUp(nextUp.plan.id))
                .clickable(onClickLabel = openLabel, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NextUpLabel(nextUp = nextUp, modifier = Modifier.weight(1f))
            // The rare action beside the field rather than under the pill (N85): the pill is the screen's
            // full-width start and stays that way (N61), so a second action goes on the row above it —
            // which is where today's card offers the same one (P3.11). Leaving it unrecorded is N85's
            // decision, and `startSubstitute` is where the argument for it lives.
            AppTextButton(
                onClick = onSubstitute,
                modifier = Modifier.testTag(TestTags.Home.nextUpSubstitute(nextUp.plan.id)),
            ) {
                Text(stringResource(R.string.home_substitute))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryActionButton(
            text = stringResource(R.string.home_start_planned_workout),
            icon = Icons.Filled.PlayArrow,
            onClick = onStart,
            // Log set's container (N83). A planned-workout start is the same action as the workout's own
            // next set — the plan, stated and then committed — so it wears the same colour, and
            // `PrimaryActionButton` takes the pair rather than assuming the accent.
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                // Room between the row's own text and the action it offers (N86). Of the three gaps the
                // entry named, a device showed this one to be the tight one: the supporting line's box
                // ended about 4 dp above the pill, while the space between two rows measured ~24 dp and
                // the block's own top ~14 dp — both of which read fine. 8 dp makes this one their equal.
                // N88 gave the text a fourth line, and the device re-measure with it still read 8 dp.
                .padding(top = 8.dp)
                .testTag(TestTags.Home.nextUpStart(nextUp.plan.id)),
        )
    }
}

/**
 * A next-up row's text: what is next, the workout, the program, and how many exercises (ROADMAP N88).
 *
 * The program and the count are two lines rather than one joined sentence, and **always**: the point is the
 * shape of the row rather than a wrap that happens once the text is long, so a one-word program name must
 * not pull the count back up beside it. Split out of [NextUpRow] because the row is at the length this
 * project allows, and the count's plural is what this half owns.
 */
@Composable
private fun NextUpLabel(nextUp: NextUp, modifier: Modifier = Modifier) {
    val exercises = pluralStringResource(
        R.plurals.home_plan_exercises,
        nextUp.plan.exerciseCount,
        nextUp.plan.exerciseCount,
    )
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_next_up),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = nextUp.plan.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // The program's name is here because more than one program may be active
            // (P3.12), so two rows have to be tellable apart.
            text = nextUp.programName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = exercises,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutsHomeScreenPreview() {
    AndroidAppTheme {
        WorkoutsHomeScreen(
            state = WorkoutsHomeUiState(
                isLoading = false,
                recent = listOf(
                    WorkoutSummary(
                        id = "a",
                        startedAt = Instant.parse("2026-09-28T07:00:00Z"),
                        finishedAt = Instant.parse("2026-09-28T08:05:00Z"),
                        exerciseCount = 4,
                        setCount = 16,
                        volumeGrams = 12_500_000L,
                    ),
                ),
            ),
            clock = remember { mutableStateOf(WorkoutClock()) },
            onStartWorkout = {},
            onOpenTemplates = {},
            onOpenWorkout = {},
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
