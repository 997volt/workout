package com.example.androidapp.ui.history

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.workout.activityLabel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

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
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
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
    /** N146's query and chip, owned by the ViewModel so a rotation does not lose them. */
    onQueryChange: (String) -> Unit = {},
    onFilterChange: (HistoryFilter) -> Unit = {},
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

            // Two different facts, told apart (N146): a log with nothing in it yet, and one where the query
            // or the chip removed everything. A filter that answers "you have not trained" is worse than none.
            state.isFilteredEmpty -> NoMatchMessage(
                modifier = Modifier.padding(innerPadding),
                onClear = {
                    onQueryChange("")
                    onFilterChange(HistoryFilter.ALL)
                },
            )

            // Distinct from "nothing matched": the user has simply not trained yet.
            state.isEmpty -> HistoryMessage(
                text = stringResource(R.string.history_empty),
                hint = stringResource(R.string.history_empty_hint),
                modifier = Modifier.padding(innerPadding),
            )

            else -> HistoryList(
                state = state,
                onOpenWorkout = onOpenWorkout,
                onRepeatWorkout = onRepeatWorkout,
                onQueryChange = onQueryChange,
                onFilterChange = onFilterChange,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/**
 * The list itself: the search bar, then a month at a time (ROADMAP P1.6, N146).
 *
 * Split out of the screen because the screen had reached the length this project enforces, and this is the half
 * that has no state of its own: it draws what the state says and reports what was typed or picked.
 */
@Composable
private fun HistoryList(
    state: WorkoutHistoryUiState,
    onOpenWorkout: (String) -> Unit,
    onRepeatWorkout: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The field and the chip scroll with the list rather than sitting above it (N146): the log is read by
        // scrolling, and a pinned header would take a row's height from every screenful.
        item(key = "search") {
            HistorySearchBar(
                query = state.query,
                filter = state.filter,
                onQueryChange = onQueryChange,
                onFilterChange = onFilterChange,
            )
        }
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

/**
 * "Nothing matched", with the one control that undoes it (ROADMAP N146).
 *
 * Split out of the screen for the reason the screen's other sections were: a `when` branch that has to explain
 * itself is a composable, and this one carries the distinction the whole filter turns on — an empty *log* and an
 * empty *result* say different things.
 */
@Composable
private fun NoMatchMessage(onClear: () -> Unit, modifier: Modifier = Modifier) {
    HistoryMessage(
        text = stringResource(R.string.history_no_match),
        hint = stringResource(R.string.history_no_match_hint),
        modifier = modifier.testTag(TestTags.HISTORY_NO_MATCH),
        action = {
            AppTextButton(
                onClick = onClear,
                modifier = Modifier.testTag(TestTags.HISTORY_FILTER_ALL),
            ) {
                Text(stringResource(R.string.history_clear_filters))
            }
        },
    )
}

/**
 * The query and the chip (ROADMAP N146).
 *
 * A `TextField` rather than a Material `SearchBar`: the field filters a list already in memory, so the search
 * *view* — which expands, suggests and submits — would be a screen of machinery for a filter. It keeps the
 * library's shape (filled, with a clear button once there is something to clear), so the app's two searches read
 * the same.
 *
 * The chips say what they filter rather than counting rows: `All` is the state the screen had before there was a
 * filter, and a count would be a number to keep true.
 */
@Composable
private fun HistorySearchBar(
    query: String,
    filter: HistoryFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.HISTORY_SEARCH_FIELD),
            singleLine = true,
            label = { Text(stringResource(R.string.history_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.history_search_clear),
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChipFor(
                HistoryFilter.ALL,
                R.string.history_filter_all,
                filter,
                onFilterChange,
                TestTags.HISTORY_FILTER_ALL,
            )
            FilterChipFor(
                HistoryFilter.ACTIVITIES,
                R.string.history_filter_activities,
                filter,
                onFilterChange,
                TestTags.HISTORY_FILTER_ACTIVITIES,
            )
            FilterChipFor(
                HistoryFilter.WORKOUTS,
                R.string.history_filter_workouts,
                filter,
                onFilterChange,
                TestTags.HISTORY_FILTER_WORKOUTS,
            )
        }
    }
}

/** One chip, so the three above are a list rather than three chances to disagree about the shape. */
@Composable
private fun FilterChipFor(
    value: HistoryFilter,
    @StringRes label: Int,
    selected: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    testTag: String,
) {
    AppFilterChip(
        selected = selected == value,
        onClick = { onFilterChange(value) },
        label = stringResource(label),
        testTag = testTag,
    )
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

    AppRow(
        headline = HistoryFormat.historyHeadline(
            workout.startedAt,
            // The zone it was performed in, not the one it is being read in (ROADMAP N25).
            zone = workout.zoneIdOrNull() ?: ZoneId.systemDefault(),
        ),
        // The template's name comes first, because it is the half of the row that says what the
        // session *was* (ROADMAP N58); a workout with no plan behind it contributes nothing.
        //
        // The duration and the volume are deliberately not here (N100): a row in a month-grouped list is
        // scanned for *which workout this was*, and those two answer a different question — they are the
        // workout detail's, which is what opening the row is for.
        // The activity's own name comes first where there is one (N146): a session with no sets and no
        // template would otherwise read as a workout somebody forgot to log, which is exactly what N141 built
        // the activity case to stop saying.
        supporting = listOf(
            workout.activity?.let { stringResource(activityLabel(it)) },
            workout.templateName,
            setCount,
        )
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
    action: (@Composable () -> Unit)? = null,
) {
    CenteredMessage(text = text, hint = hint, modifier = modifier, action = action)
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
