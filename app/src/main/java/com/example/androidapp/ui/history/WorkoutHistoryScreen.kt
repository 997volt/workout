package com.example.androidapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.androidapp.ui.components.CenteredMessage
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.workout.WorkoutFormat
import java.time.ZoneId
import java.time.Instant
import java.time.YearMonth

@Composable
fun WorkoutHistoryRoute(
    onOpenWorkout: (String) -> Unit,
    onRepeatWorkout: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The repeat is a start, so it goes through the program's missed-day question like every other
    // start (P3.3, N48): the prompt is asked at the point of starting, never at launch. A skip that
    // could not be recorded is shown on the screen's own host, because the workout still starts.
    var message by remember { mutableStateOf<String?>(null) }
    val requestStart = programStartGate(
        onStart = { intent -> intent.repeatSessionId?.let(onRepeatWorkout) },
        onError = { message = it },
    )

    WorkoutHistoryScreen(
        state = state,
        onOpenWorkout = onOpenWorkout,
        onRepeatWorkout = { sessionId -> requestStart(StartIntent(repeatSessionId = sessionId)) },
        onBack = onBack,
        message = message,
        onDismissMessage = { message = null },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    state: WorkoutHistoryUiState,
    onOpenWorkout: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRepeatWorkout: (String) -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = stringResource(R.string.history_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> HistoryMessage(
                text = stringResource(R.string.history_loading),
                modifier = Modifier.padding(innerPadding),
            )

            // Distinct from "nothing matched": the user has simply not trained yet.
            state.isEmpty -> HistoryMessage(
                text = stringResource(R.string.history_empty),
                hint = stringResource(R.string.history_empty_hint),
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "month-${group.month}") {
                        MonthHeader(month = group.month)
                    }
                    items(items = group.workouts, key = { it.id }) { workout ->
                        WorkoutRow(
                            workout = workout,
                            onClick = { onOpenWorkout(workout.id) },
                            // Offered only where a repeat would copy something: a workout whose
                            // exercises have all left the library is history, not a template
                            // (ROADMAP B43's tail, N48).
                            onRepeat = if (workout.isRepeatable) {
                                { onRepeatWorkout(workout.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, modifier: Modifier = Modifier) {
    SectionHeader(text = HistoryFormat.month(month), modifier = modifier)
}

@Composable
private fun WorkoutRow(
    workout: WorkoutSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Repeats this workout, or null when there is nothing to copy (ROADMAP N48).
     *
     * The row's **second** action: the first is the open, which is why repeat sits in the trailing
     * slot beside the chevron rather than becoming a second full-width target — a row is one thing
     * you tap, and an action on it is an icon.
     */
    onRepeat: (() -> Unit)? = null,
) {
    val setCount = pluralStringResource(R.plurals.history_sets, workout.setCount, workout.setCount)
    val duration = workout.duration?.let { WorkoutFormat.elapsed(it) }.orEmpty()
    val unit = LocalWeightUnit.current
    val volume = stringResource(
        R.string.history_volume,
        Weight.format(workout.volumeGrams, unit),
        unit.label(),
    )

    AppRow(
        headline = HistoryFormat.historyHeadline(
            workout.startedAt,
            // The zone it was performed in, not the one it is being read in (ROADMAP N25).
            zone = workout.zoneIdOrNull() ?: ZoneId.systemDefault(),
        ),
        // The template's name comes first, because it is the half of the row that says what the
        // session *was* (ROADMAP N58); a workout with no plan behind it contributes nothing.
        supporting = listOf(workout.templateName, duration, setCount, volume)
            .filter { !it.isNullOrEmpty() }
            .joinToString(" · "),
        leading = { IconTile(icon = Icons.Filled.History, accent = TileAccent.Sky) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onRepeat?.let { repeat ->
                    IconButton(
                        onClick = repeat,
                        modifier = Modifier.testTag(TestTags.historyRepeat(workout.id)),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            // Names the action, like every other icon-only control: the row's own
                            // label already says what the row's tap does.
                            contentDescription = stringResource(R.string.history_repeat),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
        // The headline is a date: it names the workout but not the tap, so the row would
        // otherwise announce "Monday 28 September, button".
        onClickLabel = stringResource(R.string.action_open_workout),
        modifier = modifier,
    )
}

@Composable
private fun HistoryMessage(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    CenteredMessage(text = text, hint = hint, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
// Sample data is the entire point of a preview, so the literals stay literals.
@Suppress("MagicNumber")
private fun WorkoutHistoryScreenPreview() {
    AndroidAppTheme {
        WorkoutHistoryScreen(
            state = WorkoutHistoryUiState(
                isLoading = false,
                groups = listOf(
                    HistoryGroup(
                        month = YearMonth.of(2026, 9),
                        workouts = listOf(
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
                ),
            ),
            onOpenWorkout = {},
            onBack = {},
        )
    }
}
