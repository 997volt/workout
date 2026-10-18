package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.DELOAD_LOAD_KEPT_CHOICES
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.longLabel
import com.example.androidapp.ui.components.shortLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Building a program's deload week (ROADMAP N147).
 *
 * A draft, not an editor: the days, the exercises and one share of the load are the controls, and
 * everything else is the plan's own numbers reduced by a rule the screen states. What is written is a
 * set of ordinary templates plus the week's plan, so a number this screen gets wrong is a template
 * edit away — which is why it does not offer a second editor for them.
 */
@Composable
fun ProgramDeloadRoute(
    onBack: () -> Unit,
    onOpenWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramDeloadViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgramDeloadScreen(
        state = state,
        onBack = onBack,
        onToggleExercise = viewModel::onToggleExercise,
        onDayCount = viewModel::onDayCountChange,
        onLoadPercentChange = viewModel::onLoadPercentChange,
        onWeekday = viewModel::onWeekdayChange,
        onCreate = viewModel::onCreate,
        onUseLastDeload = viewModel::onUseLastDeload,
        onOpenWorkouts = onOpenWorkouts,
        onDismissError = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDeloadScreen(
    state: DeloadCreatorUiState,
    onBack: () -> Unit,
    onToggleExercise: (Int, String) -> Unit,
    onDayCount: (Int) -> Unit,
    onLoadPercentChange: (Int) -> Unit,
    onWeekday: (String, DayOfWeek?) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    onUseLastDeload: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // A failed load has its own persistent message, so the generic storage wording is not shown twice (B164).
    val errorText = state.error?.takeUnless { state.loadFailed }?.let { dataErrorMessage(it) }
    MessageSnackbar(errorText, snackbarHostState, onDismissError)

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.Deload.SCREEN),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { DeloadTopBar(onBack = onBack) },
        bottomBar = { DeloadCreateBar(state = state, onCreate = onCreate) },
    ) { innerPadding ->
        DeloadStates(
            state = state,
            onBack = onBack,
            onToggleExercise = onToggleExercise,
            onDayCount = onDayCount,
            onLoadPercentChange = onLoadPercentChange,
            onWeekday = onWeekday,
            onUseLastDeload = onUseLastDeload,
            onOpenWorkouts = onOpenWorkouts,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeloadTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.deload_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
    )
}

/**
 * The way out of the draft, drawn only while there is still something to write (N147).
 *
 * It is a bar rather than a floating button because the day list can be longer than the screen and a
 * primary action that scrolls away is one the lifter has to find twice.
 */
@Composable
private fun DeloadCreateBar(
    state: DeloadCreatorUiState,
    onCreate: () -> Unit,
) {
    if (!state.showsCreateBar) return
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick = onCreate,
            enabled = state.canCreate,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .testTag(TestTags.Deload.CREATE),
        ) {
            Text(stringResource(R.string.deload_create))
        }
    }
}

@Composable
private fun DeloadStates(
    state: DeloadCreatorUiState,
    onBack: () -> Unit,
    onToggleExercise: (Int, String) -> Unit,
    onDayCount: (Int) -> Unit,
    onLoadPercentChange: (Int) -> Unit,
    onWeekday: (String, DayOfWeek?) -> Unit,
    onUseLastDeload: () -> Unit,
    onOpenWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredMessage(
            text = stringResource(R.string.programs_loading),
            modifier = modifier,
        )

        state.loadFailed -> CenteredMessage(
            text = stringResource(R.string.deload_load_failed),
            hint = stringResource(R.string.deload_load_failed_hint),
            modifier = modifier,
        )

        state.hasNothingToBuild -> CenteredMessage(
            text = stringResource(R.string.deload_no_slots),
            hint = stringResource(R.string.deload_no_slots_hint),
            modifier = modifier,
        )

        state.created != null -> DeloadDone(
            state = state,
            onClose = onBack,
            onOpenWorkouts = onOpenWorkouts,
            modifier = modifier,
        )

        else -> DeloadBody(
            state = state,
            onToggleExercise = onToggleExercise,
            onDayCount = onDayCount,
            onLoadPercentChange = onLoadPercentChange,
            onWeekday = onWeekday,
            onUseLastDeload = onUseLastDeload,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeloadBody(
    state: DeloadCreatorUiState,
    onToggleExercise: (Int, String) -> Unit,
    onDayCount: (Int) -> Unit,
    onLoadPercentChange: (Int) -> Unit,
    onWeekday: (String, DayOfWeek?) -> Unit,
    onUseLastDeload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.Deload.LIST),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { DeloadHeader(state) }
        if (state.lastDeloadWeek != null) {
            item(key = "last") {
                Column {
                    AppTextButton(
                        onClick = onUseLastDeload,
                        modifier = Modifier.testTag(TestTags.Deload.USE_LAST),
                    ) {
                        Text(stringResource(R.string.deload_use_last))
                    }
                    Text(
                        text = stringResource(
                            R.string.deload_use_last_hint,
                            state.lastDeloadWeek?.let { dateLabel(it) }.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "days") { DeloadDaysStepper(state, onDayCount) }
        item(key = "load") {
            DeloadLoadChoices(
                loadKeptPercent = state.loadKeptPercent,
                onLoadPercentChange = onLoadPercentChange,
            )
        }
        if (state.hasExistingPlan) {
            item(key = "existing") {
                Text(
                    text = stringResource(R.string.deload_existing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(items = state.days, key = { _, day -> day.sourceSlotId }) { index, day ->
            DeloadDayCard(
                day = day,
                number = index + 1,
                onToggleExercise = { exerciseId -> onToggleExercise(index, exerciseId) },
                onWeekday = onWeekday,
            )
        }
    }
}

@Composable
private fun DeloadHeader(state: DeloadCreatorUiState) {
    AppCard {
        state.programName?.let { name ->
            Text(text = name, style = MaterialTheme.typography.titleMedium)
        }
        state.weekStart?.let { week ->
            Text(
                text = stringResource(R.string.deload_week, dateLabel(week)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.deload_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DeloadDaysStepper(
    state: DeloadCreatorUiState,
    onDayCount: (Int) -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deload_days),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.deload_days_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StepButton(
                glyph = "\u2212",
                description = stringResource(R.string.deload_days_decrease),
                testTag = TestTags.Deload.DAYS_DECREASE,
                enabled = state.dayCount > 1,
                onClick = { onDayCount(state.dayCount - 1) },
            )
            Text(
                text = stringResource(R.string.deload_days_count, state.dayCount, state.maxDays),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp).testTag(TestTags.Deload.DAYS_VALUE),
            )
            StepButton(
                glyph = "+",
                description = stringResource(R.string.deload_days_increase),
                testTag = TestTags.Deload.DAYS_INCREASE,
                enabled = state.dayCount < state.maxDays,
                onClick = { onDayCount(state.dayCount + 1) },
            )
        }
    }
}

@Composable
private fun StepButton(
    glyph: String,
    description: String,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeloadLoadChoices(
    loadKeptPercent: Int,
    onLoadPercentChange: (Int) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.deload_load),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DELOAD_LOAD_KEPT_CHOICES.forEach { percent ->
                FilterChip(
                    selected = loadKeptPercent == percent,
                    onClick = { onLoadPercentChange(percent) },
                    label = { Text(stringResource(R.string.deload_load_option, percent)) },
                    modifier = Modifier.testTag(TestTags.Deload.load(percent)),
                )
            }
        }
    }
}

@Composable
private fun DeloadDayCard(
    day: DeloadDayUi,
    number: Int,
    onToggleExercise: (String) -> Unit,
    onWeekday: (String, DayOfWeek?) -> Unit,
) {
    AppCard(modifier = Modifier.testTag(TestTags.Deload.day(day.sourceSlotId))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deload_day, number),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = day.sourceName, style = MaterialTheme.typography.titleMedium)
            }
            WeekdayPicker(day = day, onWeekday = onWeekday)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        day.exercises.forEach { exercise ->
            ExerciseChoice(
                slotId = day.sourceSlotId,
                exercise = exercise,
                onToggle = onToggleExercise,
            )
        }
    }
}

@Composable
private fun WeekdayPicker(
    day: DeloadDayUi,
    onWeekday: (String, DayOfWeek?) -> Unit,
) {
    var open by rememberSaveable(day.sourceSlotId) { mutableStateOf(false) }
    val chooseLabel = stringResource(R.string.deload_weekday, day.sourceName)
    Box {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier
                .testTag(TestTags.Deload.weekday(day.sourceSlotId))
                .semantics { contentDescription = chooseLabel },
        ) {
            Text(day.weekday?.shortLabel() ?: stringResource(R.string.deload_no_weekday))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DayOfWeek.entries.forEach { weekday ->
                DropdownMenuItem(
                    text = { Text(weekday.longLabel()) },
                    onClick = {
                        open = false
                        onWeekday(day.sourceSlotId, weekday)
                    },
                    modifier = Modifier.testTag(
                        TestTags.Deload.weekdayOption(day.sourceSlotId, weekday.name),
                    ),
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.deload_no_weekday)) },
                onClick = {
                    open = false
                    onWeekday(day.sourceSlotId, null)
                },
                modifier = Modifier.testTag(
                    TestTags.Deload.weekdayOption(day.sourceSlotId, NO_WEEKDAY),
                ),
            )
        }
    }
}

@Composable
private fun ExerciseChoice(
    slotId: String,
    exercise: DeloadExerciseUi,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        FilterChip(
            selected = exercise.selected,
            onClick = { onToggle(exercise.exerciseId) },
            label = { Text(exercise.name) },
            modifier = Modifier.testTag(
                TestTags.Deload.exercise(slotId, exercise.exerciseId),
            ),
        )
        Text(
            text = prescriptionOf(exercise),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
        // The pool is program-wide, so a row says which program day it came from (N152).
        if (exercise.sourceName.isNotEmpty()) {
            Text(
                text = stringResource(R.string.deload_from, exercise.sourceName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/**
 * One exercise's draft, as the screen states it: `3 × 5 · 60 kg`.
 *
 * The set count is the rule's own (the plan's, up to three), the reps are its highest target floored
 * at five, and the load is what the chosen share derives — with the unit the exercise is shown in
 * (N64). Nothing is added for a value the rule could not derive: no reps reads as a set count, no load
 * as the sets and reps alone.
 */
@Composable
private fun prescriptionOf(exercise: DeloadExerciseUi): String {
    val first = exercise.sets.firstOrNull() ?: return ""
    val setCount = exercise.sets.size
    val base = first.reps?.let { reps ->
        stringResource(R.string.deload_sets_reps, setCount, reps)
    } ?: pluralStringResource(R.plurals.deload_sets, setCount, setCount)

    val assistance = first.assistanceGrams?.takeIf { it > 0L }
    val added = first.weightGrams?.takeIf { it > 0L }
    val load = when {
        assistance != null -> "${Weight.display(0L, assistance, exercise.weightUnit)} ${exercise.weightUnit.label()}"
        added != null -> "${Weight.format(added, exercise.weightUnit)} ${exercise.weightUnit.label()}"
        else -> null
    }
    return load?.let { stringResource(R.string.deload_with_load, base, it) } ?: base
}

@Composable
private fun DeloadDone(
    state: DeloadCreatorUiState,
    onClose: () -> Unit,
    onOpenWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val week = state.weekStart
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.deload_created_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(TestTags.Deload.CREATED),
        )
        state.created?.let { created ->
            Text(
                text = pluralStringResource(
                    R.plurals.deload_created,
                    created.dayCount,
                    created.dayCount,
                    week?.let { dateLabel(it) }.orEmpty(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Button(
            onClick = onClose,
            modifier = Modifier.padding(top = 8.dp).testTag(TestTags.Deload.DONE),
        ) {
            Text(stringResource(R.string.deload_done))
        }
        // The result is a set of ordinary workouts in the library; the card is one tap short of them
        // otherwise, which is N148's gap (it offered only Done).
        AppTextButton(
            onClick = onOpenWorkouts,
            modifier = Modifier.padding(top = 4.dp).testTag(TestTags.Deload.VIEW_WORKOUTS),
        ) {
            Text(stringResource(R.string.deload_view_workouts))
        }
    }
}

/** A stable tag for the weekday picker's "no day" entry. */
private const val NO_WEEKDAY = "NONE"

/** The week's date as the device's locale writes it. */
private fun dateLabel(date: LocalDate): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .format(date)
