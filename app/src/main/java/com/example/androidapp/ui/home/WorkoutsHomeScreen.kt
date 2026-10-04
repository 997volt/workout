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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.workout.WorkoutClock
import java.time.Instant

@Composable
fun WorkoutsHomeRoute(
    onStartWorkout: () -> Unit,
    onStartFromTemplate: () -> Unit,
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

    // Every start goes through the program's missed-day question (ROADMAP P3.3), and this is
    // the only place the navigation happens: "do it now" and "continue" differ in intent
    // rather than in destination plumbing. A skip that could not be recorded is shown on the
    // same host, because the workout still starts and the question will come back (F7).
    val requestStart = programStartGate(
        onStart = { intent ->
            when {
                // The slot travels with the template so its prescription seeds the workout (P3.8).
                intent.templateId != null -> onStartTemplate(intent.templateId, intent.slotId)
                else -> onStartWorkout()
            }
        },
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
        onStartFromTemplate = onStartFromTemplate,
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
        onOpenWorkout = onOpenWorkout,
        onOpenPrograms = onOpenPrograms,
        onOpenPlannedWorkout = viewModel::onOpenPlannedWorkout,
        message = message,
        onDismissMessage = { message = null },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsHomeScreen(
    state: WorkoutsHomeUiState,
    clock: State<WorkoutClock>,
    onStartWorkout: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartFromTemplate: () -> Unit = {},
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
) {
    // The row whose substitute picker is open, or null (P3.11).
    var substituting by remember { mutableStateOf<TodayPlan?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    SubstitutePicker(
        plan = substituting,
        templates = state.templates,
        onChoose = onSubstituteTemplate,
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
                onStartFromTemplate = onStartFromTemplate,
                // A next-up row starts the workout the same way a scheduled one does, so the slot's
                // prescription travels with it (ROADMAP P3.8).
                onStartTemplate = onStartTemplate,
                onOpenPlannedWorkout = onOpenPlannedWorkout,
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
            onSubstitute = { plan -> substituting = plan },
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
    plan: TodayPlan?,
    templates: List<WorkoutTemplate>,
    onChoose: (TodayPlan, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = plan ?: return
    SubstituteDialog(
        templates = templates.filterNot { it.id == current.templateId },
        onPick = { templateId ->
            onDismiss()
            onChoose(current, templateId)
        },
        onClear = {
            onDismiss()
            onChoose(current, null)
        },
        onDismiss = onDismiss,
    )
}

/**
 * What stands in for one occurrence this week (ROADMAP P3.11).
 *
 * The scheduled workout is offered first and clears the pick, because a substitution is the
 * lifter's statement rather than the app's and a mis-pick would otherwise be permanent.
 */
@Composable
private fun SubstituteDialog(
    templates: List<WorkoutTemplate>,
    onPick: (String) -> Unit,
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
                            .clickable { onPick(template.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
 * The home start action: **Start workout** for an empty session, and — while no
 * workout is open — **Start from template** beneath it (ROADMAP N3).
 *
 * Resuming offers no such choice: there is exactly one workout in progress, so the
 * button means one thing. The pair only appears when the user is actually choosing.
 */
@Composable
private fun StartActions(
    activeWorkout: ActiveWorkoutInfo?,
    clock: State<WorkoutClock>,
    nextUp: List<NextUp>,
    onStartWorkout: () -> Unit,
    onStartFromTemplate: () -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onOpenPlannedWorkout: (NextUp) -> Unit,
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Where each active program's run is, at the edge of the screen the thumb is already at
        // (ROADMAP N55). It moved out of the scrolling list, where a program with nothing scheduled
        // today was something to scroll to, and off the card it used to be: one program's next run
        // shown twice was two answers to one question, and more than one active program (P3.12) means
        // this row can repeat.
        nextUp.forEach { nextUpRow ->
            NextUpRow(
                nextUp = nextUpRow,
                onOpen = { onOpenPlannedWorkout(nextUpRow) },
                onStart = { onStartTemplate(nextUpRow.plan) },
            )
        }
        if (activeWorkout == null) {
            // The pair is a row of links above the pill, not a second pill: with a workout
            // already open there is no choice to make, and while there is one, only the start
            // itself is the primary act. Repeat-last gave this slot to Programs (ROADMAP N42):
            // the entry point it gave up is an action on a finished workout, in History.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onOpenPrograms,
                    modifier = Modifier.testTag(TestTags.HOME_PROGRAMS),
                ) {
                    Text(stringResource(R.string.home_programs))
                }
                TextButton(
                    onClick = onStartFromTemplate,
                    modifier = Modifier.testTag(TestTags.HOME_START_FROM_TEMPLATE),
                ) {
                    Text(stringResource(R.string.home_start_from_template))
                }
            }
        }
        StartOrResumeButton(
            activeWorkout = activeWorkout,
            clock = clock,
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The home screen's primary action (P1.16, moved here by N1).
 *
 * Reads the clock — and is the only composable here that does, so the one-second
 * tick stops at this button instead of rebuilding the list beneath it.
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

    // A filled pill rather than the extended floating button this used to be, and full width in
    // its bar: the reference's own call to action is a wide violet pill resting on the bottom of
    // the screen, and it is the one shape a user reads as "this is the thing to do here".
    Button(
        // Tagged by state, not caption: which of the two shows is the behaviour
        // under test, and the captions are user-visible text a translation changes.
        modifier = modifier
            .heightIn(min = BUTTON_HEIGHT)
            .testTag(if (resuming) TestTags.HOME_RESUME else TestTags.HOME_START),
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (resuming) Icons.Filled.PlayArrow else Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = if (resuming) {
                listOf(
                    stringResource(R.string.library_resume_workout),
                    elapsed,
                    exercises,
                ).filter { it.isNotEmpty() }.joinToString(" · ")
            } else {
                stringResource(R.string.library_start_workout)
            },
            // A resumed session's caption carries an elapsed time and an exercise count, so it
            // is the one that can outgrow the bar; the label must not push the icon out.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Comfortably over the 48dp minimum target, and the height the reference's pill reads at. */
private val BUTTON_HEIGHT = 52.dp

/**
 * One program's next run, in the bottom bar (ROADMAP N55).
 *
 * Compact rather than a card, because the bar may hold several rows — more than one program can be
 * active (P3.12) — and because it now sits beside the start pill rather than in a scrolling list.
 * Where [NextUpRow]'s old card put the name first and the button under it, this puts the *field* on
 * the row and the start beside it: tapping the field opens what is planned, and tapping *Start* starts
 * it, so looking and starting stopped being the same gesture.
 */
@Composable
private fun NextUpRow(
    nextUp: NextUp,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = pluralStringResource(
        R.plurals.home_plan_exercises,
        nextUp.plan.exerciseCount,
        nextUp.plan.exerciseCount,
    )
    val openLabel = stringResource(R.string.home_next_up_open, nextUp.plan.name)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The field is the whole left-hand half of the row, so the target is the row rather than a
        // caption inside it. `clickable` carries an `onClickLabel` because the headline names the
        // workout and never the action (the rule the restyle's rows follow).
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag(TestTags.Home.nextUp(nextUp.plan.id))
                .clickable(onClickLabel = openLabel, onClick = onOpen),
        ) {
            Text(
                text = stringResource(R.string.home_next_up),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = nextUp.plan.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // The program's name is here because more than one program may be active (P3.12), so
                // two rows have to be tellable apart.
                text = listOf(nextUp.programName, exercises).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onStart,
            modifier = Modifier.testTag(TestTags.Home.nextUpStart(nextUp.plan.id)),
        ) {
            Text(stringResource(R.string.home_plan_start))
        }
    }
}

/**
 * What a next-up row has planned, read on the tap that opened it (ROADMAP N55).
 *
 * A dialog rather than the template's editor, which is the only destination a template had: the
 * question the field answers is "what is in this workout", and opening the editor to answer it would
 * put every target in the plan one mis-tap from being rewritten on the way to reading it. It is also
 * a dialog rather than a screen because the answer is short — the workout's ordered exercises — and a
 * destination for a list of names would be a screen with a back button and nothing to do on it.
 */
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
            onStartFromTemplate = {},
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
