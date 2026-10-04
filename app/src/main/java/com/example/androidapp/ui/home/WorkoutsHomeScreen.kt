package com.example.androidapp.ui.home

import com.example.androidapp.domain.DataError
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.transfer.ClearOutcome
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.androidapp.ui.components.ClearEverythingDialog
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.transfer.DataTransferViewModel
import com.example.androidapp.ui.transfer.rememberDataTransferActions
import com.example.androidapp.ui.workout.WorkoutClock
import java.time.Instant

@Composable
fun WorkoutsHomeRoute(
    onStartWorkout: () -> Unit,
    onStartFromTemplate: () -> Unit,
    onRepeatLast: () -> Unit,
    /**
     * The navigation target: the template to start, and the slot it was scheduled as, if any
     * (ROADMAP P3.3, P3.8).
     */
    onStartTemplate: (String, String?) -> Unit,
    onOpenWorkout: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsHomeViewModel = hiltViewModel(),
    transferViewModel: DataTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Not unwrapped with `by`: reading it here would rebuild the list every second.
    val clock = viewModel.clock.collectAsStateWithLifecycle()

    // Export and import live here now (ROADMAP B1): they are app-level data
    // management, and the library N1 demoted to a reference screen was the wrong
    // home for them — two overflows deep from where the user starts.
    var message by remember { mutableStateOf<String?>(null) }
    val transferActions = rememberDataTransferActions(transferViewModel) { message = it }
    // The clear is a suspending call whose result is a sentence, so it needs both.
    val scope = rememberCoroutineScope()
    // Resolved at composition, not read from a captured Context: a resource looked up
    // through LocalContext is not configuration-aware (lint's point, and it is right).
    val clearedText = stringResource(R.string.clear_done)
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
                intent.repeatLast -> onRepeatLast()
                // The slot travels with the template so its prescription seeds the workout (P3.8).
                intent.templateId != null -> onStartTemplate(intent.templateId, intent.slotId)
                else -> onStartWorkout()
            }
        },
        onError = { message = it },
    )

    WorkoutsHomeScreen(
        state = state,
        clock = clock,
        onStartWorkout = { requestStart(StartIntent()) },
        onStartFromTemplate = onStartFromTemplate,
        onRepeatLast = { requestStart(StartIntent(repeatLast = true)) },
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
        onOpenHistory = onOpenHistory,
        onOpenPrograms = onOpenPrograms,
        onExportData = transferActions.export,
        onImportData = transferActions.import,
        onClearData = {
            scope.launch {
                when (val outcome = transferViewModel.clearEverything()) {
                    is ClearOutcome.Cleared -> message = clearedText
                    is ClearOutcome.Failed -> failure = outcome.error
                }
            }
        },
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
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onStartFromTemplate: () -> Unit = {},
    onStartTemplate: (TodayPlan) -> Unit = {},
    onRepeatLast: () -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    onExportData: (() -> Unit)? = null,
    onImportData: (() -> Unit)? = null,
    onClearData: (() -> Unit)? = null,
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    /**
     * Records the workout that stands in for one occurrence, then starts it (ROADMAP P3.11).
     *
     * A null template restores the slot's own workout and starts nothing.
     */
    onSubstituteTemplate: (TodayPlan, String?) -> Unit = { _, _ -> },
) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    // The row whose substitute picker is open, or null (P3.11).
    var substituting by remember { mutableStateOf<TodayPlan?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    if (confirmingClear) {
        ClearEverythingDialog(
            onExport = onExportData,
            onConfirm = {
                confirmingClear = false
                onClearData?.invoke()
            },
            onDismiss = { confirmingClear = false },
        )
    }

    SubstitutePicker(
        plan = substituting,
        templates = state.templates,
        onChoose = onSubstituteTemplate,
        onDismiss = { substituting = null },
    )

    Scaffold(        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                onOpenPrograms = onOpenPrograms,
                onExport = onExportData,
                onImport = onImportData,
                onClear = onClearData?.let { { confirmingClear = true } },
            )
        },
        // A bottom bar rather than a floating button. The primary action on this screen is the
        // one thing the reference never floats: it is a full-width pill resting on the bar,
        // because the thing you came to do should not be a target you have to aim at.
        bottomBar = {
            StartActions(
                activeWorkout = state.activeWorkout,
                clock = clock,
                onStartWorkout = onStartWorkout,
                onStartFromTemplate = onStartFromTemplate,
                // No new state: the list the home screen already shows answers this.
                canRepeat = state.canRepeatLast,
                onRepeatLast = onRepeatLast,
            )
        },
    ) { innerPadding ->
        HomeContent(
            state = state,
            onOpenWorkout = onOpenWorkout,
            onOpenHistory = onOpenHistory,
            onStartTemplate = onStartTemplate,
            onSubstitute = { plan -> substituting = plan },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Turns a held write failure into the snackbar sentence once, then clears it (F7).
 *
 * Split out of the route, which is at the length this project allows: the sentence comes from a
 * composable resource, so it cannot be read inside the coroutine that produced the failure.
 */
@Composable
private fun FailureMessage(
    failure: DataError?,
    onMessage: (String) -> Unit,
    onClear: () -> Unit,
) {
    // Read through `rememberUpdatedState` so the effect cannot fire a stale callback after a
    // recomposition, which is what the lint rule below is about.
    val currentOnMessage by rememberUpdatedState(onMessage)
    val currentOnClear by rememberUpdatedState(onClear)
    val text = failure?.let { dataErrorMessage(it) }
    LaunchedEffect(text) {
        if (text != null) {
            currentOnMessage(text)
            currentOnClear()
        }
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
    onOpenHistory: () -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
        when {
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.history_loading),
                modifier = modifier,
            )

            // First run: an empty list with no explanation tells the user nothing.
            state.todaysPlan.isNotEmpty() || state.nextUp.isNotEmpty() -> TodayAndRecent(
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
                    // "See all" moved onto the heading it belongs to. It was a row of the list,
                    // which made it read as one more workout; it is a way out of the section, so
                    // it sits with the section's name.
                    SectionHeader(
                        text = stringResource(R.string.home_recent),
                        trailing = {
                            TextButton(
                                onClick = onOpenHistory,
                                modifier = Modifier.testTag(TestTags.HOME_SEE_ALL),
                            ) {
                                Text(stringResource(R.string.home_see_all))
                            }
                        },
                    )
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
    onStartWorkout: () -> Unit,
    onStartFromTemplate: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whether a finished workout exists to repeat (ROADMAP N29). */
    canRepeat: Boolean = false,
    onRepeatLast: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (activeWorkout == null) {
            // The pair is a row of links above the pill, not a second pill: with a workout
            // already open there is no choice to make, and while there is one, only the start
            // itself is the primary act.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canRepeat) {
                    TextButton(
                        onClick = onRepeatLast,
                        modifier = Modifier.testTag(TestTags.HOME_REPEAT_LAST),
                    ) {
                        Text(stringResource(R.string.home_repeat_last))
                    }
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
            onOpenHistory = {},
        )
    }
}

/**
 * Home's app bar: the title, and navigation to the two things that are *not* home.
 *
 * The library moved here from being the start destination, which is the point of N1
 * — it is somewhere you go, not somewhere you land.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onExport: (() -> Unit)?,
    onImport: (() -> Unit)?,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenPrograms: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }

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
        actions = {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag(TestTags.HOME_MENU),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.transfer_more),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                HomeMenuItems(
                    onOpenPrograms = onOpenPrograms,
                    onExport = onExport,
                    onImport = onImport,
                    onClear = onClear,
                    onDismiss = { menuOpen = false },
                )
            }
        },
    )
}

/**
 * The app's data actions, together and in the order that matters (ROADMAP B1, N18).
 *
 * Export first, then import, then the one that cannot be undone — and that one is last
 * and coloured, because it is the only entry here that can cost the user something.
 */
@Composable
private fun DataActions(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.transfer_export)) },
                    onClick = {
                        onDismiss()
                        onExport()
                    },
                    modifier = Modifier.testTag(TestTags.DATA_EXPORT),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.transfer_import)) },
                    onClick = {
                        onDismiss()
                        onImport()
                    },
                    modifier = Modifier.testTag(TestTags.DATA_IMPORT),
                )
                if (onClear != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.clear_menu),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            onDismiss()
                            onClear()
                        },
                        modifier = Modifier.testTag(TestTags.HOME_CLEAR_DATA),
                    )
                }
    }
}

/**
 * The menu's entries, split out so the app bar stays a title and a button.
 *
 * The data actions are the newest members (ROADMAP B1, N18) and are null on a preview or
 * a screen test that does not exercise them — so the menu is exactly as long as it has
 * something to offer.
 */
@Composable
private fun HomeMenuItems(
    onExport: (() -> Unit)?,
    onImport: (() -> Unit)?,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
    onOpenPrograms: () -> Unit = {},
) {
    Column {
        // The schedule the today's-plan section is read from (ROADMAP P3.3): one tap from
        // the plan it changes, and above the data actions because it is a destination
        // rather than something done to the data.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_programs)) },
            onClick = {
                onDismiss()
                onOpenPrograms()
            },
            modifier = Modifier.testTag(TestTags.HOME_PROGRAMS),
        )
        // Export and import, moved down from the library (ROADMAP B1).
        if (onExport != null && onImport != null) {
            DataActions(
                onExport = onExport,
                onImport = onImport,
                onClear = onClear,
                onDismiss = onDismiss,
            )
        }
    }
}
