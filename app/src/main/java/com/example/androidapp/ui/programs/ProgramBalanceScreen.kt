package com.example.androidapp.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.programName?.let { name ->
                    item(key = "program") {
                        Text(text = name, style = MaterialTheme.typography.titleMedium)
                    }
                }
                item(key = "total") {
                    Text(
                        text = pluralStringResource(
                            R.plurals.program_balance_total,
                            state.totalSets,
                            state.totalSets,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(TestTags.PROGRAM_BALANCE_TOTAL),
                    )
                }
                items(items = state.balance, key = { it.muscle?.name ?: "unstated" }) { row ->
                    BalanceRow(row = row, total = state.totalSets)
                }
            }
        }
    }
}

/** One muscle's line: what it is, how much of the plan it is, and the bar that says so at a glance. */
@Composable
private fun BalanceRow(row: MuscleSets, total: Int) {
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
            Text(
                text = pluralStringResource(R.plurals.program_balance_sets, row.sets, row.sets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The share as a bar rather than a second number: the bar is what makes a lopsided plan obvious at a
        // glance, which is the whole reason for looking.
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else row.sets.toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
