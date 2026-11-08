package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.HALF_SETS_PER_SET
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.domain.model.TrainedWindow
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle

/**
 * How a program's planned work spreads across muscles, and what the log actually trained (ROADMAP N119, N289).
 *
 * The plan is a read of the *week being authored*: it answers "does this week train what I think it does"
 * while the week is still being written. The trained side is the same count over sets that were performed in
 * the last 7 or 28 days, so the two sit on one screen and one rule rather than two destinations that could
 * disagree about what a set is worth.
 *
 * **Both sides state counts, never a verdict.** A range a lifter should hit is a claim this app has no evidence
 * for, and the parked P2.8 is where a judgement over real history would live; here the plan and the log are
 * described, and the lifter decides. The trained side deliberately has no bar beside each count: a share of the
 * window is not a target either.
 */
@Composable
fun ProgramBalanceRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramBalanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The window ends at the screen's own midnight, so a screen left open across it re-reads the date (N286).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    ProgramBalanceScreen(
        state = state,
        onBack = onBack,
        onSelectView = viewModel::onSelectView,
        onSelectWindow = viewModel::onSelectWindow,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramBalanceScreen(
    state: ProgramBalanceUiState,
    onBack: () -> Unit,
    onSelectView: (BalanceView) -> Unit,
    onSelectWindow: (TrainedWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.PROGRAM_BALANCE),
        topBar = {
            TopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.program_balance)) },
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
        if (state.isLoading) {
            CenteredMessage(
                text = stringResource(R.string.programs_loading),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            // The toggle is inside the list, not outside it, so a program with no work can still be read
            // against the log: the empty state must not take away the way to the other side.
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.programName?.let { name ->
                    item(key = "program") {
                        Text(text = name, style = MaterialTheme.typography.titleMedium)
                    }
                }
                item(key = "view") {
                    BalanceViewToggle(selected = state.view, onSelect = onSelectView)
                }
                when (state.view) {
                    BalanceView.PLAN -> planItems(state)
                    BalanceView.TRAINED -> trainedItems(state, onSelectWindow)
                }
            }
        }
    }
}

/** The Plan/Trained choice: one rule, two questions about it (ROADMAP N289). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BalanceViewToggle(selected: BalanceView, onSelect: (BalanceView) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BalanceView.entries.forEach { view ->
            AppFilterChip(
                selected = view == selected,
                onClick = { onSelect(view) },
                label = stringResource(view.labelRes),
                testTag = TestTags.programBalanceView(view.name),
            )
        }
    }
}

/** The plan's own size, its rows, or the sentence a plan with no work deserves (ROADMAP N119, N122). */
private fun LazyListScope.planItems(state: ProgramBalanceUiState) {
    if (state.balance.isEmpty()) {
        item(key = "empty") {
            CenteredMessage(
                text = stringResource(R.string.program_balance_empty),
                modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_EMPTY),
            )
        }
        return
    }

    balanceSummary(state.plannedSets)
    // The bars divide by the credited work, not by the plan's set count: a set is worth half to each muscle
    // it assists, so the rows sum past the plan's size (N122).
    val creditedHalfSets = state.balance.sumOf { it.halfSets }
    items(items = state.balance, key = { it.muscle?.name ?: "unstated" }) { row ->
        BalanceRow(row = row, creditedHalfSets = creditedHalfSets)
    }
}

/**
 * What the log trained in the chosen window, as counts (ROADMAP N289).
 *
 * The window chips stay above the empty state for the same reason the view toggle does: widening the window
 * is exactly what a lifter would do after reading "nothing in the last seven days", and taking the control
 * away would leave the answer without its next question.
 */
@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.trainedItems(
    state: ProgramBalanceUiState,
    onSelectWindow: (TrainedWindow) -> Unit,
) {
    item(key = "window") {
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrainedWindow.entries.forEach { window ->
                AppFilterChip(
                    selected = window == state.window,
                    onClick = { onSelectWindow(window) },
                    label = stringResource(window.labelRes),
                    testTag = TestTags.programBalanceWindow(window.name),
                )
            }
        }
    }

    if (state.trainedReadFailed) {
        item(key = "trained-failed") {
            CenteredMessage(
                text = stringResource(R.string.program_balance_trained_failed),
                modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_TRAINED_FAILED),
            )
        }
        return
    }

    if (state.trained.isEmpty()) {
        item(key = "trained-empty") {
            CenteredMessage(
                text = stringResource(R.string.program_balance_trained_empty),
                modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_TRAINED_EMPTY),
            )
        }
        return
    }

    item(key = "trained-total") {
        Text(
            text = pluralStringResource(
                R.plurals.program_balance_trained_total,
                state.trainedSets,
                state.trainedSets,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_TRAINED_TOTAL),
        )
    }
    item(key = "trained-weighting") {
        Text(
            text = stringResource(R.string.program_balance_trained_weighting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    items(items = state.trained, key = { "trained-${it.muscle?.name ?: "unstated"}" }) { row ->
        TrainedRow(row = row)
    }
}

/**
 * The plan's own size, and how a set is counted across muscles (ROADMAP N119, N122).
 *
 * The weighting is said in words because the rows can sum past the total above it, and a lifter who counts
 * the sets in the editor deserves to know why.
 */
private fun LazyListScope.balanceSummary(plannedSets: Int) {
    item(key = "total") {
        Text(
            text = pluralStringResource(R.plurals.program_balance_total, plannedSets, plannedSets),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_TOTAL),
        )
    }
    item(key = "weighting") {
        Text(
            text = stringResource(R.string.program_balance_weighting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One muscle's line in the plan: what it is, how much of the plan it is, and the bar that says so. */
@Composable
private fun BalanceRow(row: MuscleSets, creditedHalfSets: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.programBalanceRow(row.muscle?.name ?: "unstated")),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = muscleLabel(row.muscle), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = setsLabel(row.halfSets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The share as a bar rather than a second number: the bar is what makes a lopsided plan obvious at a
        // glance, which is the whole reason for looking.
        LinearProgressIndicator(
            progress = { if (creditedHalfSets == 0) 0f else row.halfSets.toFloat() / creditedHalfSets },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/**
 * One muscle's line in the log: its name and the count, on one row and with no bar (ROADMAP N289).
 *
 * Merged for a screen reader, so the two texts are announced as one fact ("Quads, 9 sets") rather than as a
 * label whose number is a separate stop.
 */
@Composable
private fun TrainedRow(row: MuscleSets) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.programBalanceTrainedRow(row.muscle?.name ?: "unstated"))
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = muscleLabel(row.muscle), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = setsLabel(row.halfSets),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun muscleLabel(muscle: MuscleGroup?): String =
    muscle?.label ?: stringResource(R.string.program_balance_unstated)

/**
 * A half-set count as the screen writes it: `6`, `1.5`, `0.5` (ROADMAP N122).
 *
 * The count is held in halves so it stays exact, and this is the one place it is read back as the sets a
 * lifter counts — a half is written rather than rounded away, because the half *is* the secondary's share.
 */
@Composable
private fun setsLabel(halfSets: Int): String {
    val count = halfSetsLabel(halfSets)
    // Exactly one set reads "1 set"; anything else — including the halves — reads as a plural, because
    // "1.5 set" is not a sentence (N122).
    return if (halfSets == HALF_SETS_PER_SET) {
        stringResource(R.string.program_balance_set_one, count)
    } else {
        stringResource(R.string.program_balance_sets_many, count)
    }
}

private fun halfSetsLabel(halfSets: Int): String {
    val whole = halfSets / HALF_SETS_PER_SET
    return if (halfSets % HALF_SETS_PER_SET == 0) whole.toString() else "$whole.5"
}

/** The toggle's own words: the plan is what was authored, the trained side is what happened. */
private val BalanceView.labelRes: Int
    get() = when (this) {
        BalanceView.PLAN -> R.string.program_balance_plan
        BalanceView.TRAINED -> R.string.program_balance_trained
    }

/** How each window is named; the count beside it is the answer, not the label. */
private val TrainedWindow.labelRes: Int
    get() = when (this) {
        TrainedWindow.LAST_7_DAYS -> R.string.program_balance_window_7
        TrainedWindow.LAST_28_DAYS -> R.string.program_balance_window_28
    }
