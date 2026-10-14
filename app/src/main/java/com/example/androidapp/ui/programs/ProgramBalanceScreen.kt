package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.HALF_SETS_PER_SET
import com.example.androidapp.domain.model.MuscleSets
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.TestTags

/**
 * How a program's planned work spreads across muscles (ROADMAP N119).
 *
 * A read-only view of the *plan*: it answers "does this week train what I think it does" while the week is
 * still being authored, which no screen could before — the statistics tab reads what was logged, and the
 * editor shows one slot at a time.
 *
 * It states counts and shares rather than a verdict. A range a lifter should hit is a claim this app has no
 * evidence for, and the parked P2.8 is where a judgement over real history would live; here the plan is
 * described, and the lifter decides.
 */
@Composable
fun ProgramBalanceRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramBalanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgramBalanceScreen(state = state, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramBalanceScreen(
    state: ProgramBalanceUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.PROGRAM_BALANCE),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_balance)) },
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
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.programs_loading),
                modifier = Modifier.padding(innerPadding),
            )

            state.balance.isEmpty() -> CenteredMessage(
                text = stringResource(R.string.program_balance_empty),
                modifier = Modifier.padding(innerPadding).testTag(TestTags.PROGRAM_BALANCE_EMPTY),
            )

            else -> {
                // The bars divide by the credited work, not by the plan's set count: a set is worth half to
                // each muscle it assists, so the rows sum past the plan's size (N122).
                val creditedHalfSets = state.balance.sumOf { it.halfSets }
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.programName?.let { name ->
                        item(key = "program") {
                            Text(text = name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    balanceSummary(state.plannedSets)
                    items(items = state.balance, key = { it.muscle?.name ?: "unstated" }) { row ->
                        BalanceRow(row = row, creditedHalfSets = creditedHalfSets)
                    }
                }
            }
        }
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

/** One muscle's line: what it is, how much of the plan it is, and the bar that says so at a glance. */
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
            Text(
                text = row.muscle?.label ?: stringResource(R.string.program_balance_unstated),
                style = MaterialTheme.typography.bodyLarge,
            )
            val count = halfSetsLabel(row.halfSets)
            Text(
                // Exactly one set reads "1 set"; anything else — including the halves — reads as a plural,
                // because "1.5 set" is not a sentence (N122).
                text = if (row.halfSets == HALF_SETS_PER_SET) {
                    stringResource(R.string.program_balance_set_one, count)
                } else {
                    stringResource(R.string.program_balance_sets_many, count)
                },
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
 * A half-set count as the screen writes it: `6`, `1.5`, `0.5` (ROADMAP N122).
 *
 * The count is held in halves so it stays exact, and this is the one place it is read back as the sets a
 * lifter counts — a half is written rather than rounded away, because the half *is* the secondary's share.
 */
private fun halfSetsLabel(halfSets: Int): String {
    val whole = halfSets / HALF_SETS_PER_SET
    return if (halfSets % HALF_SETS_PER_SET == 0) whole.toString() else "$whole.5"
}
